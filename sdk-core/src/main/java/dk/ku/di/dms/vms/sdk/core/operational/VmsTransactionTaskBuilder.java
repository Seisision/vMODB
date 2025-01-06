package dk.ku.di.dms.vms.sdk.core.operational;

import dk.ku.di.dms.vms.modb.api.enums.ExecutionModeEnum;
import dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionContext;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static java.lang.System.Logger.Level.ERROR;

/**
 * Given the transactional handler and callback are shared among all instances
 * of transaction tasks, the builder offers these to avoid
 * repetitive creation of these attributes in task instances
 */
public final class VmsTransactionTaskBuilder {

    private static final System.Logger LOGGER = System.getLogger(VmsTransactionTaskBuilder.class.getSimpleName());

    private static final int NEW = 0;
    private static final int READY = 1;
    private static final int RUNNING = 2;
    private static final int FINISHED = 3;
    private static final int FAILED = 4;

    private final ITransactionManager transactionManager;

    private final ISchedulerCallback schedulerCallback;

    public VmsTransactionTaskBuilder(ITransactionManager transactionManager,
                                     ISchedulerCallback schedulerCallback) {
        this.transactionManager = transactionManager;
        this.schedulerCallback = schedulerCallback;
    }

    public final class VmsTransactionTask implements Runnable {

        private final long tid;

        private final long lastTid;

        private final long batch;

        // the information necessary to run the method
        private final VmsTransactionSignature signature;

        private final Object inputEvent;

        private volatile int status;

        private final List<Object> partitionKeys;

        private VmsTransactionTask(long tid, long lastTid, long batch,
                                   VmsTransactionSignature signature,
                                   Object inputEvent) {
            this.tid = tid;
            this.lastTid = lastTid;
            this.batch = batch;
            this.signature = signature;
            this.inputEvent = inputEvent;
            this.partitionKeys = getPartitionKeys(signature, inputEvent);
        }

        @SuppressWarnings("unchecked")
        private static List<Object> getPartitionKeys(VmsTransactionSignature signature, Object inputEvent) {
            List<Object> partitionIdAux;
            try {
                if (signature.executionMode() == ExecutionModeEnum.PARTITIONED) {
                    if (List.class.isAssignableFrom(signature.partitionByMethod().getReturnType())) {
                        partitionIdAux = (List<Object>) signature.partitionByMethod().invoke(inputEvent);
                    } else {
                        partitionIdAux = List.of(signature.partitionByMethod().invoke(inputEvent));
                    }
                } else {
                    partitionIdAux = List.of();
                }
            } catch (InvocationTargetException | IllegalAccessException e){
                LOGGER.log(ERROR, "Failed to obtain partition key(s) from method "+ signature.partitionByMethod().getName());
                partitionIdAux = List.of();
            }
            return partitionIdAux;
        }

        private void readOnlyRun(){
            try{
                transactionManager.beginTransaction(this.tid, -1, this.lastTid, true);
                Object output = this.signature.method().invoke(this.signature.vmsInstance(), this.inputEvent);
                OutboundEventResult eventOutput = new OutboundEventResult(this.tid, this.batch, this.signature.outputQueue(), output);
                schedulerCallback.success(this.signature.executionMode(), eventOutput);
            } catch (IllegalAccessException | InvocationTargetException e) {
                this.handleErrorOnTask(e);
            } catch (Exception e){
                this.handleGenericError(e);
            }
        }

        private void handleGenericError(Exception e) {
            LOGGER.log(ERROR, "Error not related to invoking task "+this.toString()+"\n"+ e);
            e.printStackTrace(System.out);
            schedulerCallback.error(signature.executionMode(), this.tid, e);
        }

        private void handleErrorOnTask(ReflectiveOperationException e) {
            LOGGER.log(ERROR, "Error during invoking task "+this.toString()+"\n"+ e);
            e.printStackTrace(System.out);
            schedulerCallback.error(signature.executionMode(), this.tid, e);
        }

        @Override
        public void run() {
            this.signalRunning();
            if(this.signature.transactionType() == TransactionTypeEnum.R){
                this.readOnlyRun();
                return;
            }
            ITransactionContext txCtx = transactionManager.beginTransaction(this.tid, -1, this.lastTid, false);
            try {
                Object output = this.signature.method().invoke(this.signature.vmsInstance(), this.inputEvent);
                OutboundEventResult eventOutput = new OutboundEventResult(this.tid, this.batch, this.signature.outputQueue(), output);
                transactionManager.commit();
                schedulerCallback.success(this.signature.executionMode(), eventOutput);
            } catch (IllegalAccessException | InvocationTargetException e) {
                this.handleErrorOnTask(e);
            } catch (Exception e){
                this.handleGenericError(e);
            }
            // avoid returning indexes to pool before committing
            txCtx.release();
        }

        public long tid() {
            return this.tid;
        }

        public VmsTransactionSignature signature(){
            return this.signature;
        }

        public Object input(){
            return this.inputEvent;
        }

        public int status(){
            return this.status;
        }

        // set as ready for execution. the thread pool is able to execute it
        public void signalReady(){
            this.status = READY;
        }

        public void signalRunning(){
            this.status = RUNNING;
        }

        public boolean isScheduled(){
            return this.status > NEW && this.status < FINISHED;
        }

        public boolean isFinished(){
            return this.status == FINISHED;
        }

        public void signalFinished(){
            this.status = FINISHED;
        }

        public void signalFailed(){
            this.status = FAILED;
        }

        public List<Object> partitionKeys() {
            return this.partitionKeys;
        }

        @Override
        public String toString() {
            return "{"
                    + "\"batch\":\"" + this.batch + "\""
                    + ",\"lastTid\":\"" + this.lastTid + "\""
                    + ",\"tid\":\"" + tid + "\""
                    + "}";
        }

    }

    public VmsTransactionTask build(long tid, long lastTid, long batch,
                                    VmsTransactionSignature signature,
                                    Object input){
        return new VmsTransactionTask(tid, lastTid, batch, signature, input);
    }

    public VmsTransactionTask buildFinished(long tid){
        var sig = new VmsTransactionSignature(null, null, null, ExecutionModeEnum.SINGLE_THREADED, null, null);
        var deadTask = new VmsTransactionTask(tid, 0, 0, sig, null);
        deadTask.status = FINISHED;
        return deadTask;
    }

}