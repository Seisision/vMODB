package dk.ku.di.dms.vms.modb.query;

import dk.ku.di.dms.vms.modb.ExecutorTest;
import dk.ku.di.dms.vms.modb.common.type.DataType;
import dk.ku.di.dms.vms.modb.definition.Schema;
import dk.ku.di.dms.vms.modb.definition.Table;
import dk.ku.di.dms.vms.modb.index.unique.UniqueHashBufferIndex;
import dk.ku.di.dms.vms.modb.storage.record.RecordBufferContext;
import dk.ku.di.dms.vms.modb.transaction.multiversion.index.PrimaryIndex;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Common set of methods for two or more tests (e.g., {@link PlannerTest} and {@link ExecutorTest}
 */
public final class TestCommon {

    public static Map<String, Table> getDefaultCatalog(){

        // item
        String[] itemColumns = { "i_id", "i_price", "i_name", "i_data" };
        DataType[] itemDataTypes = { DataType.INT, DataType.FLOAT, DataType.STRING, DataType.STRING };
        Schema itemSchema = new Schema(itemColumns, itemDataTypes, new int[]{0}, null, false );

        ResourceScope scope = ResourceScope.newSharedScope();
        MemorySegment segment = MemorySegment.allocateNative(itemSchema.getRecordSize() * 10L, scope);
        RecordBufferContext rbc = new RecordBufferContext(segment, 10);
        UniqueHashBufferIndex index = new UniqueHashBufferIndex(rbc, itemSchema, itemSchema.getPrimaryKeyColumns());
        PrimaryIndex consistentIndex = new PrimaryIndex(index);
        Table itemTable = new Table("item", itemSchema, consistentIndex);

        // customer
//        String[] customerColumns = { "c_id", "c_d_id", "c_w_id", "c_discount", "c_last", "c_credit", "c_balance", "c_ytd_payment" };
//        DataType[] customerDataTypes = { DataType.LONG, DataType.INT, DataType.INT,
//                DataType.FLOAT, DataType.STRING, DataType.STRING, DataType.FLOAT, DataType.FLOAT };
//        Schema customerSchema = new Schema(customerColumns, customerDataTypes, new int[]{0,1,2}, null );
//        Table customerTable = new Table("customer", customerSchema);
//
        Map<String, Table> map = new HashMap<>();
        map.put("item", itemTable);

        return map;

    }

//    public static QueryTree getSimpleQueryTree() throws AnalyzerException {
//
//        Map<String, Table> map = new HashMap<>();
//        String[] columnNames = { "col1", "col2" };
//        DataType[] columnDataTypes = { DataType.INT, DataType.INT };
//        Schema schema = new Schema( columnNames, columnDataTypes, new int[]{0}, null );
//        ResourceScope scope = ResourceScope.newSharedScope();
//        MemorySegment segment = MemorySegment.allocateNative(itemSchema.getRecordSize() * 10L, scope);
//        RecordBufferContext rbc = new RecordBufferContext(segment, 10, itemSchema.getRecordSize());
//        UniqueHashIndex index = new UniqueHashIndex(rbc, itemSchema, itemSchema.getPrimaryKeyColumns());
//        ConsistentIndex consistentIndex = new ConsistentIndex(index);
//        catalog.insertTable( new Table( "tb1", schema,  (UniqueHashIndex) null ));
//        catalog.insertTable( new Table( "tb2", schema,  (UniqueHashIndex) null ));
//
//        SelectStatementBuilder builder = QueryBuilderFactory.select();
//        IStatement sql = builder.select("col1, col2")
//                .from("tb1")
//                .where("col1", EQUALS, 1)
//                .and("col2", EQUALS, 2)
//                .build();
//
//        Analyzer analyzer = new Analyzer(catalog);
//        QueryTree queryTree = analyzer.analyze( sql );
//        return queryTree;
//    }

//    public static QueryTree getJoinQueryTree() throws AnalyzerException {
//
//        final Catalog catalog = new Catalog();
//        String[] columnNames = { "col1", "col2" };
//        DataType[] columnDataTypes = { DataType.INT, DataType.INT };
//        final Schema schema = new Schema( columnNames, columnDataTypes, new int[]{0}, null );
//        catalog.insertTable( new Table( "tb1", schema, (UniqueHashIndex) null ));
//        catalog.insertTable( new Table( "tb2", schema,  (UniqueHashIndex) null ));
//        catalog.insertTable( new Table( "tb3", schema,  (UniqueHashIndex) null ));
//
//        SelectStatementBuilder builder = QueryBuilderFactory.select();
//        IStatement sql = builder.select("tb1.col1, tb2.col2")
//                .from("tb1")
//                .join("tb2","col1").on(EQUALS, "tb1", "col1")
//                .join("tb3","col1").on(EQUALS, "tb1", "col1")
//                .where("tb1.col2", EQUALS, 2)
//                .and("tb2.col2",EQUALS,1)
//                .build();
//
//        Analyzer analyzer = new Analyzer(catalog);
//        QueryTree queryTree = analyzer.analyze( sql );
//
//        return queryTree;
//    }

}
