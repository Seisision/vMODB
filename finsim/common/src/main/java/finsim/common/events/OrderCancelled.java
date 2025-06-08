package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Order;
import finsim.common.inputs.CancelOrder;

import java.util.Date;

@Event
public final class OrderCancelled {
    public Date timestamp;
    public CancelOrder request;
    public Order order;
    public String instanceId;

    public OrderCancelled() {}

    public OrderCancelled(Date timestamp, CancelOrder request, Order order, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.order = order;
        this.instanceId = instanceId;
    }
}