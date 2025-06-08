package finsim.common.inputs;

import finsim.common.enums.BuySellType;
import finsim.common.enums.OrderDuration;
import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;

@Event
public final class MarketFill {
    public String order_id;
    public int quantity;
    public double price;
    public String instanceId;

    public MarketFill() {}

    public MarketFill(String order_id, int quantity, double price, String instanceId) {
        this.order_id = order_id;
        this.quantity = quantity;
        this.price = price;
        this.instanceId = instanceId;
    }
}