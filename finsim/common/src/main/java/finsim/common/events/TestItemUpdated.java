package finsim.common.events;

import finsim.common.entities.TestItem;
import finsim.common.inputs.UpdateTestItem;
import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;
import java.util.List;

@Event
public final class TestItemUpdated {

    public Date timestamp;

    public UpdateTestItem updateTestItem;

    public List<TestItem> items;

    public String instanceId;

    public TestItemUpdated(){}

    public TestItemUpdated(Date timestamp, UpdateTestItem updateTestItem, List<TestItem> items, String instanceId) {
        this.timestamp = timestamp;
        this.updateTestItem = updateTestItem;
        this.items = items;
        this.instanceId = instanceId;
    }

}