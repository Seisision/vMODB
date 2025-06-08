package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Order;
import finsim.common.inputs.UpdateOrder;

import java.util.Date;

@Event
public final class OrderChanged {
    public Date timestamp;
    public UpdateOrder request;
    public Order order;
    public String instanceId;

    public OrderChanged() {}

    public OrderChanged(Date timestamp, UpdateOrder request, Order order, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.order = order;
        this.instanceId = instanceId;
    }
}