package finsim.common.inputs;

import java.util.Date;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public class OrderRouted {
    public String orderId;
    public String instrumentId;
    public double price;
    public double quantity;
    public boolean isBuyOrder;
    public Date timestamp;
    public String instanceId;

    public OrderRouted() {}

    public OrderRouted(String orderId, String instrumentId, double price, 
                      double quantity, boolean isBuyOrder, Date timestamp, String instanceId) {
        this.orderId = orderId;
        this.instrumentId = instrumentId;
        this.price = price;
        this.quantity = quantity;
        this.isBuyOrder = isBuyOrder;
        this.timestamp = timestamp;
        this.instanceId = instanceId;
    }
}