package finsim.common.inputs;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Order;

import java.util.Date;

@Event
public final class OrderPositionCreated {
    public String timestamp;
    public String orderId;
    public int amountFilled;
    public String instanceId;

    public OrderPositionCreated() {}

    public OrderPositionCreated(String timestamp, String orderId, int amountFilled, String instanceId) {
        this.timestamp = timestamp;
        this.orderId = orderId;
        this.amountFilled = amountFilled;
        this.instanceId = instanceId;
    }
}