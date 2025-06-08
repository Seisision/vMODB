package finsim.test.repositories;

import finsim.test.entities.TestItem;
import dk.ku.di.dms.vms.modb.api.annotations.Query;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;

import java.util.List;

public interface ITestItemRepository extends IRepository<TestItem.TestItemId, TestItem> {
    @Query("select * from test_items where primary_id = :primaryId")
    List<TestItem> getTestItemsByPrimaryId(int primaryId);
}