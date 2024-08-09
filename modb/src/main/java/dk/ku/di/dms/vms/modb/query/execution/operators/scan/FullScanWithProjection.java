package dk.ku.di.dms.vms.modb.query.execution.operators.scan;

import dk.ku.di.dms.vms.modb.query.execution.filter.FilterContext;
import dk.ku.di.dms.vms.modb.transaction.TransactionContext;
import dk.ku.di.dms.vms.modb.transaction.multiversion.index.IMultiVersionIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FullScanWithProjection extends AbstractScan {

    public FullScanWithProjection(IMultiVersionIndex index,
                                  int[] projectionColumns,
                                  int entrySize) {
        super(entrySize, index, projectionColumns);
    }

//    public MemoryRefNode run(FilterContext filterContext){
//        Iterator<IKey> iterator = index.iterator();
//        while(iterator.hasNext()){
//            if(index.checkCondition(iterator, filterContext)){
//                append(iterator, projectionColumns);
//            }
//            iterator.next();
//        }
//        return memoryRefNode;
//    }

    public List<Object[]> runAsEmbedded(TransactionContext txCtx){
        List<Object[]> res = new ArrayList<>();
        Iterator<Object[]> iterator = this.index.iterator(txCtx);
        while(iterator.hasNext()){
            res.add(iterator.next());
        }
        return res;
    }

    public List<Object[]> runAsEmbedded(TransactionContext txCtx, FilterContext filterContext){
        List<Object[]> res = new ArrayList<>();
        Iterator<Object[]> iterator = this.index.iterator(txCtx);
        while(iterator.hasNext()){
            Object[] obj = iterator.next();
            if(this.index.checkCondition(filterContext, obj))
                res.add(obj);
        }
        return res;
    }

    @Override
    public boolean isFullScan() {
        return true;
    }

    @Override
    public FullScanWithProjection asFullScan() {
        return this;
    }

}
