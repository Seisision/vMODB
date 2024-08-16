package dk.ku.di.dms.vms.modb.transaction.multiversion.index;

import dk.ku.di.dms.vms.modb.common.data_structure.Tuple;
import dk.ku.di.dms.vms.modb.definition.key.IKey;
import dk.ku.di.dms.vms.modb.definition.key.KeyUtils;
import dk.ku.di.dms.vms.modb.index.interfaces.ReadWriteIndex;
import dk.ku.di.dms.vms.modb.transaction.TransactionContext;
import dk.ku.di.dms.vms.modb.transaction.multiversion.WriteType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Wrapper of a non unique index for multi versioning concurrency control
 */
public final class NonUniqueSecondaryIndex implements IMultiVersionIndex {

    private static final Deque<Map<IKey, Tuple<Object[], WriteType>>> WRITE_SET_BUFFER = new ConcurrentLinkedDeque<>();

    private final Map<Long,Map<IKey, Tuple<Object[], WriteType>>> writeSet;

    // pointer to primary index
    // necessary because of concurrency control
    // secondary index point to records held in primary index
    private final PrimaryIndex primaryIndex;

    // a non-unique hash index
    private final ReadWriteIndex<IKey> underlyingIndex;

    // key: formed by secondary indexed columns
    // value: the corresponding pks
    private final Map<IKey, Set<IKey>> keyMap;

    public NonUniqueSecondaryIndex(PrimaryIndex primaryIndex, ReadWriteIndex<IKey> underlyingIndex) {
        this.writeSet = new ConcurrentHashMap<>();
        this.primaryIndex = primaryIndex;
        this.underlyingIndex = underlyingIndex;
        this.keyMap = new ConcurrentHashMap<>();
    }

    public ReadWriteIndex<IKey> getUnderlyingIndex(){
        return this.underlyingIndex;
    }

    /**
     * Called by the primary key index
     * In this method, the secondary key is formed
     * and then cached for later retrieval.
     * A secondary key point to several primary keys in the primary index
     * @param primaryKey may have many secIdxKey associated
     */
    @Override
    public boolean insert(TransactionContext txCtx, IKey primaryKey, Object[] record){
        IKey secKey = KeyUtils.buildRecordKey( this.underlyingIndex.columns(), record );
        Set<IKey> set = this.keyMap.computeIfAbsent(secKey, (ignored)-> new HashSet<>());
        if(!set.contains(primaryKey)) {
            var txWriteSet = this.writeSet.computeIfAbsent(txCtx.tid, k ->
                    Objects.requireNonNullElseGet(WRITE_SET_BUFFER.poll(), HashMap::new));
            txWriteSet.put(primaryKey, new Tuple<>(record, WriteType.INSERT));
            set.add(primaryKey);
        }
        return true;
    }

    @Override
    public void undoTransactionWrites(TransactionContext txCtx){
        var txWriteSet = this.writeSet.get(txCtx.tid);
        // var writes = WRITE_SET.get().entrySet().stream().filter(p->p.getValue().t2()==WriteType.INSERT).toList();
        for(Map.Entry<IKey, Tuple<Object[], WriteType>> entry : txWriteSet.entrySet()){
            if(entry.getValue().t2() != WriteType.INSERT) continue;
            IKey secKey = KeyUtils.buildRecordKey( this.underlyingIndex.columns(), entry.getValue().t1() );
            Set<IKey> set = this.keyMap.get(secKey);
            set.remove(entry.getKey());
        }
        this.clearAndReturnMapToBuffer(txCtx);
    }

    @Override
    public boolean update(TransactionContext txCtx, IKey key, Object[] record) {
        // KEY_WRITES.get().put(key, new Tuple<>(record, WriteType.UPDATE));
        // key already there
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean remove(TransactionContext txCtx, IKey key) {
        // how to know the sec idx if we don't have the record?
        throw new RuntimeException("Not supported");
    }

    public boolean remove(TransactionContext txCtx, IKey key, Object[] record){
        IKey secKey = KeyUtils.buildRecordKey( this.underlyingIndex.columns(), record );
        var txWriteSet = this.writeSet.computeIfAbsent(txCtx.tid, k ->
                Objects.requireNonNullElseGet(WRITE_SET_BUFFER.poll(), HashMap::new));
        txWriteSet.put(key, new Tuple<>(record, WriteType.DELETE));
        return true;
    }

    @Override
    public Object[] lookupByKey(TransactionContext txCtx, IKey key) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void installWrites(TransactionContext txCtx) {
        // just remove the delete since the insert is already in the keyMap
        for(var entry : this.writeSet.get(txCtx.tid).entrySet()){
            if(entry.getValue().t2() != WriteType.DELETE) continue;
            IKey secKey = KeyUtils.buildRecordKey( this.underlyingIndex.columns(), entry.getValue().t1() );
            Set<IKey> set = this.keyMap.get(secKey);
            set.remove(entry.getKey());
        }
        this.clearAndReturnMapToBuffer(txCtx);
    }

    private void clearAndReturnMapToBuffer(TransactionContext txCtx) {
        var map = this.writeSet.get(txCtx.tid);
        map.clear();
        WRITE_SET_BUFFER.addLast(map);
    }

    @Override
    public Iterator<Object[]> iterator(TransactionContext txCtx, IKey[] keys) {
        return new SecondaryIndexIterator(txCtx, keys);
    }

    private class SecondaryIndexIterator implements Iterator<Object[]> {

        private final TransactionContext txCtx;
        private Iterator<IKey> currentIterator;
        private final IKey[] keys;
        private int idx;

        public SecondaryIndexIterator(TransactionContext txCtx, IKey[] keys){
            this.txCtx = txCtx;
            this.idx = 0;
            this.keys = keys;
            this.currentIterator = keyMap.computeIfAbsent(keys[this.idx], (ignored) -> new HashSet<>()).iterator();
        }

        @Override
        public boolean hasNext() {
            return this.currentIterator.hasNext() || this.idx < this.keys.length - 1;
        }

        @Override
        public Object[] next() {
            if(!this.currentIterator.hasNext()){
                this.idx++;
                this.currentIterator = keyMap.computeIfAbsent(this.keys[this.idx], (ignored) -> new HashSet<>()).iterator();
            }
            IKey key = this.currentIterator.next();
            // this operation would not be necessary if we did not leak the writes to the keyMap
            return primaryIndex.getRecord(this.txCtx, key);
        }
    }

    @Override
    public int[] indexColumns() {
        return this.underlyingIndex.columns();
    }

    @Override
    public boolean containsColumn(int columnPos) {
        return this.underlyingIndex.containsColumn(columnPos);
    }

}
