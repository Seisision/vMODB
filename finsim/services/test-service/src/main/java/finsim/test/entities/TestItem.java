package finsim.test.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;

@VmsTable(name="test_items")
@IdClass(TestItem.TestItemId.class)
public final class TestItem implements IEntity<TestItem.TestItemId> {

    public static class TestItemId implements Serializable {
        public int primary_id;
        public int secondary_id;
        public TestItemId(){}
        public TestItemId(int primary_id, int secondary_id) {
            this.primary_id = primary_id;
            this.secondary_id = secondary_id;
        }
    }

    @Id
    @VmsIndex(name = "test_idx")
    public int primary_id;

    @Id
    @VmsIndex(name = "test_idx")
    public int secondary_id;

    @Column
    public String test_name;

    @Column
    public float test_number;

    @Column
    @VmsIndex(name = "test_idx")
    public String version;

    public TestItem() {}

    public TestItem(int primary_id, int secondary_id, String test_name, float test_number, String version) {
        this.primary_id = primary_id;
        this.secondary_id = secondary_id;
        this.test_name = test_name;
        this.test_number = test_number;
        this.version = version;
    }

    @Override
    public String toString() {
        return "TestItem{"
                + "primary_id=" + primary_id
                + ", secondary_id=" + secondary_id
                + ", test_name='" + test_name + '\''
                + ", test_number=" + test_number
                + ", version='" + version + '\''
                + '}';
    }
}