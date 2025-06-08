package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Order;
import finsim.common.inputs.CreateOrder;

import java.util.Date;

@Event
public final class OrderCreated {
    public Date timestamp;
    public CreateOrder request;
    public Order order;
    public String instanceId;

    public OrderCreated() {}

    public OrderCreated(Date timestamp, CreateOrder request, Order order, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.order = order;
        this.instanceId = instanceId;
    }
}