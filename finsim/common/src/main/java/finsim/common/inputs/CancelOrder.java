package finsim.common.inputs;
import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public final class CancelOrder {
    public String id;
    public String instanceId;

    public CancelOrder() {}

    public CancelOrder(String id, String instanceId) {
        this.id = id;
        this.instanceId = instanceId;
    }
}