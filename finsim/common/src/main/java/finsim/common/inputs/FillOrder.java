package finsim.common.inputs;

import java.util.Date;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public final class FillOrder {
    public String id;
    public int executedQuantity;
    public double executedPrice;
    public Date executionDateTime;
    public String instanceId;

    public FillOrder() {}

    public FillOrder(String id, int executedQuantity, double executedPrice, 
                   Date executionDateTime, String instanceId) {
        this.id = id;
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.executionDateTime = executionDateTime;
        this.instanceId = instanceId;
    }
}