package dk.ku.di.dms.vms.tpcc.proxy.workload;

import dk.ku.di.dms.vms.modb.common.constraint.ConstraintReference;
import dk.ku.di.dms.vms.modb.common.type.DataType;
import dk.ku.di.dms.vms.modb.common.type.DataTypeUtils;
import dk.ku.di.dms.vms.modb.definition.Schema;
import dk.ku.di.dms.vms.modb.storage.record.AppendOnlyBuffer;
import dk.ku.di.dms.vms.sdk.embed.metadata.EmbedMetadataLoader;
import dk.ku.di.dms.vms.tpcc.common.events.NewOrderWareIn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static dk.ku.di.dms.vms.tpcc.proxy.datagen.DataGenUtils.nuRand;
import static dk.ku.di.dms.vms.tpcc.proxy.datagen.DataGenUtils.randomNumber;
import static dk.ku.di.dms.vms.tpcc.proxy.infra.TPCcConstants.*;
import static java.lang.System.Logger.Level.*;

public final class WorkloadUtils {

    private static final System.Logger LOGGER = System.getLogger(WorkloadUtils.class.getName());

    private static final String BASE_WORKLOAD_FILE_NAME = "new_order_input_";

    private static final Schema SCHEMA = new Schema(
            new String[]{ "w_id", "d_id", "c_id", "itemIds", "supWares", "qty", "allLocal" },
            new DataType[]{
                    DataType.INT, DataType.INT, DataType.INT, DataType.INT_ARRAY,
                    DataType.INT_ARRAY, DataType.INT_ARRAY, DataType.BOOL
            },
            new int[]{},
            new ConstraintReference[]{},
            false
    );

    private static void write(long pos, Object[] record) {
        long currAddress = pos;
        for (int index = 0; index < SCHEMA.columnOffset().length; index++) {
            DataType dt = SCHEMA.columnDataType(index);
            DataTypeUtils.callWriteFunction(currAddress, dt, record[index]);
            currAddress += dt.value;
        }
    }

    private static Object[] read(long address){
        Object[] record = new Object[SCHEMA.columnOffset().length];
        long currAddress = address;
        for(int i = 0; i < SCHEMA.columnOffset().length; i++) {
            DataType dt = SCHEMA.columnDataType(i);
            record[i] = DataTypeUtils.getValue(dt, currAddress);
            currAddress += dt.value;
        }
        return record;
    }

    /**
     * @param initTs Necessary to discard batches that complete after the end of the experiment
     * @param submitted Necessary to calculate the latency, throughput, and percentiles
     */
    public record WorkloadStats(long initTs, Map<Long, List<Long>>[] submitted){}

    @SuppressWarnings("unchecked")
    public static WorkloadStats submitWorkload(List<Iterator<NewOrderWareIn>> input, int runTime, Function<NewOrderWareIn, Long> func) {
        int numWorkers = input.size();
        LOGGER.log(INFO, "Submitting transactions through "+numWorkers+" worker(s)");
        CountDownLatch allThreadsStart = new CountDownLatch(numWorkers+1);
        CountDownLatch allThreadsAreDone = new CountDownLatch(numWorkers);
        Map<Long, List<Long>>[] submittedArray = new Map[numWorkers];

        for(int i = 0; i < numWorkers; i++) {
            final Iterator<NewOrderWareIn> workerInput = input.get(i);
            int finalI = i;
            Thread thread = new Thread(()-> submittedArray[finalI] =
                            Worker.run(allThreadsStart, allThreadsAreDone, workerInput, runTime, func));
            thread.start();
        }

        allThreadsStart.countDown();
        long initTs;
        try {
            allThreadsStart.await();
            initTs = System.currentTimeMillis();
            LOGGER.log(INFO,"Experiment main going to wait for the workers to finish.");
            if (!allThreadsAreDone.await(runTime * 2L, TimeUnit.MILLISECONDS)) {
                LOGGER.log(ERROR,"Latch has not reached zero. Something wrong with the worker(s)");
            } else {
                LOGGER.log(INFO,"Experiment main woke up!");
            }
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }

        return new WorkloadStats(initTs, submittedArray);
    }

    private static final class Worker {

        public static Map<Long, List<Long>> run(CountDownLatch allThreadsStart, CountDownLatch allThreadsAreDone,
                                                Iterator<NewOrderWareIn> input, int runTime, Function<NewOrderWareIn, Long> func) {
            Map<Long,List<Long>> startTsMap = new HashMap<>();
            long threadId = Thread.currentThread().threadId();
            LOGGER.log(INFO,"Thread ID " + threadId + " started");
            allThreadsStart.countDown();
            try {
                allThreadsStart.await();
            } catch (InterruptedException e) {
                LOGGER.log(ERROR, "Thread ID "+threadId+" failed to await start");
                throw new RuntimeException(e);
            }
            long currentTs = System.currentTimeMillis();
            long endTs = System.currentTimeMillis() + runTime;
            do {
                try {
                    long batchId = func.apply(input.next());
                    if(!startTsMap.containsKey(batchId)){
                        startTsMap.put(batchId, new ArrayList<>());
                    }
                    startTsMap.get(batchId).add(currentTs);
                } catch (Exception e) {
                    if(!input.hasNext()){
                        LOGGER.log(WARNING, "Number of input events are not enough for runtime " + runTime + " ms");
                        allThreadsAreDone.countDown();
                        break;
                    } else {
                        LOGGER.log(ERROR,"Exception in Thread ID: " + (e.getMessage() == null ? "No message" : e.getMessage()));
                        throw new RuntimeException(e);
                    }
                }
                currentTs = System.currentTimeMillis();
            } while (currentTs < endTs);

            allThreadsAreDone.countDown();
            return startTsMap;
        }
    }

    public static List<Iterator<NewOrderWareIn>> mapWorkloadInputFiles(int numWare){
        LOGGER.log(INFO, "Mapping "+numWare+" warehouse files from disk...");
        long initTs = System.currentTimeMillis();
        List<Iterator<NewOrderWareIn>> input = new ArrayList<>(numWare);
        for(int i = 0; i < numWare; i++){
            AppendOnlyBuffer buffer = EmbedMetadataLoader.loadAppendOnlyBufferUnknownSize(BASE_WORKLOAD_FILE_NAME+"1");
            // calculate number of entries (i.e., transactions)
            int numTransactions = (int) buffer.size() / SCHEMA.getRecordSize();
            input.add( createWorkloadInputIterator(buffer, numTransactions) );
        }
        long endTs = System.currentTimeMillis();
        LOGGER.log(INFO, "Mapped "+numWare+" warehouse files from disk in "+(endTs-initTs)+" ms");
        return input;
    }

    private static Iterator<NewOrderWareIn> createWorkloadInputIterator(AppendOnlyBuffer buffer, int numTransactions){
        return new Iterator<>() {
            int txIdx = 1;
            @Override
            public boolean hasNext() {
                return this.txIdx <= numTransactions;
            }
            @Override
            public NewOrderWareIn next() {
                Object[] newOrderInput = read(buffer.nextOffset());
                buffer.forwardOffset(SCHEMA.getRecordSize());
                this.txIdx++;
                return parseRecordIntoEntity(newOrderInput);
            }
        };
    }

    public static List<Iterator<NewOrderWareIn>> createWorkload(int numWare, int numTransactions){
        LOGGER.log(INFO, "Generating of "+(numTransactions * numWare)+"...");
        long initTs = System.currentTimeMillis();
        List<Iterator<NewOrderWareIn>> input = new ArrayList<>(numWare);
        for(int ware = 1; ware <= numWare; ware++) {
            LOGGER.log(INFO, "Generating "+numTransactions+" transactions for warehouse "+ware);
            String fileName = BASE_WORKLOAD_FILE_NAME+ware;
            AppendOnlyBuffer buffer = EmbedMetadataLoader.loadAppendOnlyBuffer(numTransactions, SCHEMA.getRecordSize(), fileName, true);
            for (int txIdx = 1; txIdx <= numTransactions; txIdx++) {
                Object[] newOrderInput = generateNewOrder(ware, numWare);
                write(buffer.nextOffset(), newOrderInput);
                buffer.forwardOffset(SCHEMA.getRecordSize());
            }
            buffer.force();
        }
        long endTs = System.currentTimeMillis();
        LOGGER.log(INFO, "Generated "+(numTransactions * numWare)+" in "+(endTs-initTs)+" ms");
        return input;
    }

    public static int getNumWorkloadInputFiles(){
        String basePathStr = EmbedMetadataLoader.getBasePath();
        Path basePath = Paths.get(basePathStr);
        try(var paths = Files
                // retrieve all files in the folder
                .walk(basePath)
                // find the log files
                .filter(path -> path.toString().contains(BASE_WORKLOAD_FILE_NAME))) {
            return paths.toList().size();
        } catch (IOException ignored){
            return 0;
        }
    }

    private static NewOrderWareIn parseRecordIntoEntity(Object[] newOrderInput) {
        return new NewOrderWareIn(
                (int) newOrderInput[0],
                (int) newOrderInput[1],
                (int) newOrderInput[2],
                (int[]) newOrderInput[3],
                (int[]) newOrderInput[4],
                (int[]) newOrderInput[5],
                (boolean) newOrderInput[6]
        );
    }

    private static Object[] generateNewOrder(int w_id, int num_ware){
        int d_id;
        int c_id;
        int ol_cnt;
        int all_local = 1;
        int not_found = NUM_ITEMS + 1;
        int rbk;

        int max_num_items_per_order_ = Math.min(MAX_NUM_ITEMS_PER_ORDER, NUM_ITEMS);
        int min_num_items_per_order_ = Math.min(5, max_num_items_per_order_);

        d_id = randomNumber(1, NUM_DIST_PER_WARE);
        c_id = nuRand(1023, 1, NUM_CUST_PER_DIST);

        ol_cnt = randomNumber(min_num_items_per_order_, max_num_items_per_order_);
        rbk = randomNumber(1, 100);

        int[] itemIds = new int[ol_cnt];
        int[] supWares = new int[ol_cnt];
        int[] qty = new int[ol_cnt];

        for (int i = 0; i < ol_cnt; i++) {
            int item_ = nuRand(8191, 1, NUM_ITEMS);

            // avoid duplicate items
            while(foundItem(itemIds, i, item_)){
                item_ = nuRand(8191, 1, NUM_ITEMS);
            }
            itemIds[i] = item_;

            if(FORCE_ABORTS) {
                if ((i == ol_cnt - 1) && (rbk == 1)) {
                    // this can lead to exception and then abort in app code
                    itemIds[i] = not_found;
                }
            }

            if (ALLOW_MULTI_WAREHOUSE_TX) {
                if (randomNumber(1, 100) != 1) {
                    supWares[i] = w_id;
                } else {
                    supWares[i] = otherWare(num_ware, w_id);
                    all_local = 0;
                }
            } else {
                supWares[i] = w_id;
            }
            qty[i] = randomNumber(1, 10);
        }

        return new Object[]{ w_id, d_id, c_id, itemIds, supWares, qty, all_local == 1 };
    }

    private static boolean foundItem(int[] itemIds, int length, int value){
        if(length == 0) return false;
        for(int i = 0; i < length; i++){
            if(itemIds[i] == value) return true;
        }
        return false;
    }

    /**
     * Based on <a href="https://github.com/AgilData/tpcc/blob/master/src/main/java/com/codefutures/tpcc/Driver.java#L310">AgilData</a>
     */
    private static int otherWare(int num_ware, int home_ware) {
        int tmp;
        if (num_ware == 1) return home_ware;
        do {
            tmp = randomNumber(1, num_ware);
        } while (tmp == home_ware);
        return tmp;
    }

}
