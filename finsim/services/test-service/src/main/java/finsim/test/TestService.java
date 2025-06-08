package finsim.test;

import finsim.test.entities.TestItem;
import finsim.test.infra.TestUtils;
import finsim.test.repositories.ITestItemRepository;
import finsim.common.inputs.UpdateTestItem;
import finsim.common.events.TestItemUpdated;
import dk.ku.di.dms.vms.modb.api.annotations.*;

import java.util.Date;
import java.util.List;

import static finsim.common.Constants.*;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.RW;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.W;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

@Microservice("test")
public final class TestService {

    private static final System.Logger LOGGER = System.getLogger(TestService.class.getName());

    private final ITestItemRepository testItemRepository;

    public TestService(ITestItemRepository testItemRepository) {
        this.testItemRepository = testItemRepository;
    }

    @Inbound(values = {UPDATE_TEST_ITEM})
    @Outbound(TEST_ITEM_UPDATED)
    @Transactional(type=RW)
    public TestItemUpdated update(UpdateTestItem updateTestItem) {
        LOGGER.log(DEBUG, "APP: Test-service received an update request for primary ID "+ updateTestItem.PrimaryId +" with TID: "+updateTestItem.instanceId);
        // get test items from the given customer
        List<TestItem> testItems = 
                this.testItemRepository.getTestItemsByPrimaryId(updateTestItem.PrimaryId);
        if(testItems == null || testItems.isEmpty()) {
            LOGGER.log(ERROR, "APP: No test items found for primary ID "+updateTestItem.PrimaryId+" TID: "+updateTestItem.instanceId);
            return new TestItemUpdated(new Date(), updateTestItem, List.of(), updateTestItem.instanceId);
        }
        // update each test item
        for (TestItem testItem : testItems) {
            testItem.test_name = updateTestItem.TestName;
            testItem.test_number = updateTestItem.TestNumber;
            this.testItemRepository.update(testItem);
            LOGGER.log(DEBUG, "APP: Test item updated with TID: "+updateTestItem.instanceId);
        }

        // return the updated test items
        return new TestItemUpdated(new Date(), updateTestItem, TestUtils.convertTestItems(testItems), updateTestItem.instanceId);
    }   

}