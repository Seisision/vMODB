package dk.ku.di.dms.vms.modb.definition.key.composite;

import dk.ku.di.dms.vms.modb.definition.key.IKey;

public final class TripleCompositeKey extends BaseComposite implements IKey {

    private final Object value0;
    private final Object value1;
    private final Object value2;

    public static TripleCompositeKey of(Object value0, Object value1, Object value2){
        return new TripleCompositeKey(value0, value1, value2);
    }

    private TripleCompositeKey(Object value0, Object value1, Object value2) {
        super(hashCode(value0, value1, value2));
        this.value0 = value0;
        this.value1 = value1;
        this.value2 = value2;
    }

    private static int hashCode(Object value0, Object value1, Object value2) {
        int result = 31 * value0.hashCode() + value1.hashCode();
        return 31 * result + value2.hashCode();
    }

    @Override
    public boolean equals(Object key){
        return key instanceof TripleCompositeKey pairCompositeKey &&
                this.value0.equals(pairCompositeKey.value0) &&
                this.value1.equals(pairCompositeKey.value1) &&
                this.value2.equals(pairCompositeKey.value2);
    }

    @Override
    public String toString() {
        return "{"
                + "\"value0\":" + this.value0
                + ",\"value1\":" + this.value1
                + ",\"value2\":" + this.value2
                + "}";
    }

}
