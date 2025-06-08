package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Transfer;
import finsim.common.inputs.CreateTransfer;

import java.util.Date;

@Event
public final class TransferCreated {
    public Date timestamp;
    public CreateTransfer request;
    public Transfer transfer;
    public String instanceId;

    public TransferCreated() {}

    public TransferCreated(Date timestamp, CreateTransfer request, Transfer transfer, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.transfer = transfer;
        this.instanceId = instanceId;
    }
}
