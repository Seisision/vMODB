package dk.ku.di.dms.vms.coordinator.batch;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data structure to keep data about a batch commit
 * The batch in progress (the first or the one waiting for being committed)
 * must be shared with vms workers
 */
public final class BatchContext {

    // no need non volatile. immutable
    public final long batchOffset;

    // set of terminal VMSs that has not voted yet
    public final Set<String> missingVotes;

    public final Set<String> terminalVMSs;

    public Map<String, Long> previousBatchPerVms;

    public Map<String,Integer> numberOfTIDsPerVms;

    public long tidAborted;

    public long lastTid;

    public BatchContext(long batchOffset) {
        this.batchOffset = batchOffset;
        this.terminalVMSs = new HashSet<>();
        this.missingVotes = new HashSet<>();
    }

    // called when the batch is over
    public void seal(long lastTidOverall,
                     Map<String, Long> previousBatchPerVms, Map<String,Integer> numberOfTIDsPerVms){
        this.lastTid = lastTidOverall;
        // immutable
        this.previousBatchPerVms = previousBatchPerVms;
        this.numberOfTIDsPerVms = numberOfTIDsPerVms;
        // must be a modifiable hash set because the set will be modified upon BATCH_COMPLETE messages received
        this.missingVotes.addAll(this.terminalVMSs);
    }

}