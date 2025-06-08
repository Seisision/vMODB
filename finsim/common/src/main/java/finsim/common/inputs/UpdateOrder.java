package finsim.common.inputs;

import finsim.common.enums.OrderDuration;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;

@Event
public final class UpdateOrder {
    public String id;
    public int quantity;
    public double price;
    public OrderDuration duration;
    public Date expiryDateTime;
    public String instanceId;

    public UpdateOrder() {}

    public UpdateOrder(String id, int quantity, double price, OrderDuration duration,
                    Date expiryDateTime, String instanceId) {
        this.id = id;
        this.quantity = quantity;
        this.price = price;
        this.duration = duration;
        this.expiryDateTime = expiryDateTime;
        this.instanceId = instanceId;
    }
}