package dk.ku.di.dms.vms.modb.query.execution.operators.scan;

import dk.ku.di.dms.vms.modb.definition.key.IKey;
import dk.ku.di.dms.vms.modb.transaction.TransactionContext;
import dk.ku.di.dms.vms.modb.transaction.multiversion.index.IMultiVersionIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * On-flight scanning, filtering, and projection in a single operator.
 * The caller knows the output format.
 * But called does not know how many records are.
 * Header:
 * int - number of rows returned
 * This must be performed by a proper method at the end of the procedure
 * We can have a method called seal or close() in abstract operator
 * It will put the information on header.
 */
public final class IndexScanWithProjection extends AbstractScan {

    public IndexScanWithProjection(
                     IMultiVersionIndex index,
                     int[] projectionColumns,
                     int entrySize) {
        super(entrySize, index, projectionColumns);
    }

    @Override
    public boolean isIndexScan() {
        return true;
    }

    @Override
    public IndexScanWithProjection asIndexScan() {
        return this;
    }

    public List<Object[]> runAsEmbedded(TransactionContext txCtx){
        List<Object[]> res = new ArrayList<>();
        Iterator<Object[]> iterator = this.index.iterator(txCtx);
        while(iterator.hasNext()){
            res.add( iterator.next() );
        }
        return res;
    }

    public List<Object[]> runAsEmbedded(TransactionContext txCtx, IKey... keys) {
        List<Object[]> res = new ArrayList<>();
        Iterator<Object[]> iterator;
        if (keys.length > 1) {
            iterator = this.index.iterator(txCtx, keys);
        } else {
            iterator = this.index.iterator(txCtx, keys[0]);
        }
        while(iterator.hasNext()){
            res.add( iterator.next() );
        }
        return res;
    }

}