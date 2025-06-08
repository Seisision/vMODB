package finsim.common.entities;

public final class TestItem {

    public int PrimaryId;

    public int SecondaryId;

    public String TestName;

    public float TestNumber;

    public String Version;

    public TestItem() { }

    public TestItem(int primaryId, int secondaryId, String testName, float testNumber, String version) {
        PrimaryId = primaryId;
        SecondaryId = secondaryId;
        TestName = testName;
        TestNumber = testNumber;
        Version = version;
    }

    @Override
    public String toString() {
        return "{"
                + "\"PrimaryId\":\"" + PrimaryId + "\""
                + ",\"SecondaryId\":\"" + SecondaryId + "\""
                + ",\"TestName\":\"" + TestName + "\""
                + ",\"TestNumber\":\"" + TestNumber + "\""
                + ",\"Version\":\"" + Version + "\""
                + "}";
    }
}