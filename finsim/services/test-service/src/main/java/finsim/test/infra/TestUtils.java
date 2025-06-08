package finsim.test.infra;

import finsim.common.entities.TestItem;

import java.util.List;

public final class TestUtils {

    public static List<TestItem> convertTestItems(List<finsim.test.entities.TestItem> testItems){
        return testItems.stream().map(TestUtils::convertTestItemEntity).toList();
    }

    public static TestItem convertTestItemEntity(finsim.test.entities.TestItem testItem){
        return new finsim.common.entities.TestItem( testItem.primary_id, testItem.secondary_id, testItem.test_name, testItem.test_number, testItem.version);
    }

    public static finsim.test.entities.TestItem convertTestItemAPI(TestItem testItem){
        return new finsim.test.entities.TestItem( testItem.PrimaryId, testItem.SecondaryId, testItem.TestName, testItem.TestNumber, testItem.Version);
    }

}