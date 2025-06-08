package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public final class UpdateTestItem {

    public int PrimaryId;

    public float TestNumber;

    public String TestName;

    public String instanceId;

    public UpdateTestItem(){}

    public UpdateTestItem(int primaryId, float testNumber, String testName, String instanceId) {
        this.PrimaryId = primaryId;
        this.TestNumber = testNumber;
        this.TestName = testName;        
        this.instanceId = instanceId;
    }

    public int getId(){
        return this.PrimaryId;
    }

}
