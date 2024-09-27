package dk.ku.di.dms.vms.modb.persistence;

import dk.ku.di.dms.vms.modb.common.memory.MemoryUtils;
import dk.ku.di.dms.vms.modb.common.type.DataType;
import dk.ku.di.dms.vms.modb.definition.Schema;
import dk.ku.di.dms.vms.modb.definition.key.IntKey;
import dk.ku.di.dms.vms.modb.index.unique.UniqueHashBufferIndex;
import dk.ku.di.dms.vms.modb.storage.record.RecordBufferContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public final class PersistenceTest {

    private static jdk.internal.misc.Unsafe UNSAFE;

    @BeforeClass
    public static void setUp(){
        UNSAFE = MemoryUtils.UNSAFE;
        assert UNSAFE != null;
    }

    // private static final 10737418240
    private static final long ONE_GB = Integer.MAX_VALUE / 2;
    private static final long TWO_GB = Integer.MAX_VALUE;
    private static final long TEN_GB = ONE_GB * 10;

    @Test
    public void testCollision() throws IOException {
        Schema schema = new Schema(new String[]{"id", "test"}, new DataType[]{ DataType.INT, DataType.STRING },
                new int[]{ 0 }, null, false );
        var fileName = "mapped_file_test.data";
        Path path = Paths.get(fileName);
        var fc = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.READ,
                StandardOpenOption.SPARSE,
                StandardOpenOption.WRITE
        );
        MemorySegment memorySegment = fc.map(FileChannel.MapMode.READ_WRITE, 0,
                7L * schema.getRecordSize(), Arena.ofShared());
        var bufCtx = new RecordBufferContext( memorySegment);
        var index = new UniqueHashBufferIndex(bufCtx, schema, schema.getPrimaryKeyColumns(),7);

        index.insert(IntKey.of(10), new Object[] { 10, "test" } );
        index.insert(IntKey.of(14), new Object[] { 14, "test" } );
        index.insert(IntKey.of(24), new Object[] { 24, "test" } );

        Assert.assertEquals(3, index.size());

        index.reset();

    }

    @Test
    public void testMemoryMapping() throws IOException {
        Schema schema = new Schema(new String[]{"id", "test"}, new DataType[]{ DataType.INT, DataType.STRING },
                new int[]{ 0 }, null, false );
        var fileName = "mapped_file_test.data";
        Path path = Paths.get(fileName);
        var fc = FileChannel.open(path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.READ,
                    StandardOpenOption.SPARSE,
                    StandardOpenOption.WRITE
                );
        MemorySegment memorySegment = fc.map(FileChannel.MapMode.READ_WRITE, 0,
                10L * schema.getRecordSize(), Arena.ofShared());

        var bufCtx = new RecordBufferContext(memorySegment);
        var index = new UniqueHashBufferIndex(bufCtx, schema, schema.getPrimaryKeyColumns(), 10);
        for(int i = 1; i <= 10; i = i + 2){
            index.insert(IntKey.of(i), new Object[] { i, "test" } );
        }

        Object[] recordRes = index.record(IntKey.of(3));
        assert recordRes != null && ((String)recordRes[1]).contentEquals("test");

        // test force now
        bufCtx.force();
        fc.close();
        fc = FileChannel.open(path, StandardOpenOption.READ);

        memorySegment = fc.map(FileChannel.MapMode.READ_ONLY, 0,
                10L * schema.getRecordSize(), Arena.ofShared());
        bufCtx = new RecordBufferContext(memorySegment);
        index = new UniqueHashBufferIndex(bufCtx, schema, schema.getPrimaryKeyColumns(), 10);

        var bb = ByteBuffer.allocate( 10 * schema.getRecordSize() );
        int res = fc.read(bb);
        Assert.assertEquals(730, res);

        var record = index.lookupByKey( IntKey.of(3) );
        Assert.assertNotNull(record);
        Assert.assertTrue(record[1].toString().contentEquals("test"));
    }

    @Test
    public void testFileMapping() throws IOException {

        String userHome = System.getProperty("user.home");

        String filePath = userHome + "/" + "testFile";

        File file = new File(filePath);
        boolean res = false;
        if(!file.exists()){
            res = file.createNewFile();
        }

        assert res;

        long divFactor = TWO_GB - 8;

        // every byte buffer "loses" 8 bytes per mapping

        int numberBuckets = (int) (TEN_GB / divFactor);

        long lostBytes = numberBuckets * 8;

        MemorySegment segment;
        try (Arena arena = Arena.ofShared()) {
            segment = arena.allocate(TEN_GB - lostBytes);
        }

        long nextOffset = 0;
        // long offsetEnd = divFactor - 1;

        ByteBuffer mappedBuffer;

        long[] initOffsetPerBuffer = new long[numberBuckets];

        Map<Integer, ByteBuffer> buffers = new HashMap<>( numberBuckets);
        for(int i = 0; i < numberBuckets; i++){

            initOffsetPerBuffer[i] = nextOffset;

            MemorySegment seg0 = segment.asSlice(nextOffset, divFactor);
            mappedBuffer = seg0.asByteBuffer();

            buffers.put(i, mappedBuffer);

            nextOffset += divFactor;
            nextOffset++;
        }

        mappedBuffer = buffers.get(0);

        ByteBuffer appBuffer = ByteBuffer.allocate(Integer.BYTES);
        appBuffer.putInt(1);
        appBuffer.clear();

        // writing an integer value to mapped buffer
        mappedBuffer.put( appBuffer );

        // flush
        segment.force();

        // reset position
        mappedBuffer.clear();

        assert(mappedBuffer.getInt() == appBuffer.getInt());

        // next check is regarding the last buffer
        long offsetToTest = initOffsetPerBuffer[2];

        mappedBuffer.position(0);
        mappedBuffer.putInt(10);

        // https://github.com/apache/flink/blob/master/flink-core/src/main/java/org/apache/flink/core/memory/MemoryUtils.java
        UNSAFE.copyMemory( segment.address(), segment.address() + offsetToTest, Integer.BYTES );

        assert (buffers.get(2).getInt() == 10);

    }

}
