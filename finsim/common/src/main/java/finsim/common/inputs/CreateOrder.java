package finsim.common.inputs;

import finsim.common.enums.BuySellType;
import finsim.common.enums.OrderDuration;
import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;

@Event
public final class CreateOrder {
    public String id;
    public String accountId;
    public String instrumentId;
    public int quantity;
    public double price;
    public BuySellType buySell;
    public OrderDuration duration;
    public Date expiryDateTime;
    public String instanceId;

    public CreateOrder() {}

    public CreateOrder(String id, String accountId, String instrumentId, int quantity, double price, 
                    BuySellType buySell, OrderDuration duration, Date expiryDateTime, String instanceId) {
        this.id = id;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.price = price;
        this.buySell = buySell;
        this.duration = duration;
        this.expiryDateTime = expiryDateTime;
        this.instanceId = instanceId;
    }
}