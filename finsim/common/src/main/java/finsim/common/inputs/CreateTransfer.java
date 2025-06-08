package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;

@Event
public final class CreateTransfer {
    public String id;
    public String instrumentId;
    public int quantity;
    public String sourceAccountId;
    public String targetAccountId;
    public String externalReference;
    public String instanceId;

    public CreateTransfer() {}
    public CreateTransfer(String id, String instrumentId, int quantity, String sourceAccountId, 
                          String targetAccountId, String externalReference, String instanceId) {
        this.id = id;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.externalReference = externalReference;
        this.instanceId = instanceId;
    }
}