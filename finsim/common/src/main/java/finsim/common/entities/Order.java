package finsim.common.entities;

import finsim.common.enums.BuySellType;
import finsim.common.enums.OrderStatus;
import finsim.common.enums.OrderDuration;

import java.util.Date;

public final class Order {
    public String id;
    public String accountId;
    public String instrumentId;
    public int quantity;
    public double price;
    public BuySellType buySell;
    public Date dateTime;
    public OrderStatus status;
    public OrderDuration duration; // how long the order is valid
    public Date expiryDateTime; // for some order durations, when the order expires

    public Order() { }

    public Order(String id, String accountId, String instrumentId, int quantity, double price, 
                BuySellType buySell, Date dateTime, OrderStatus status, OrderDuration duration, 
                Date expiryDateTime) {
        this.id = id;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.price = price;
        this.buySell = buySell;
        this.dateTime = dateTime;
        this.status = status;
        this.duration = duration;
        this.expiryDateTime = expiryDateTime;
    }

    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\""
                + ",\"accountId\":\"" + accountId + "\""
                + ",\"instrumentId\":\"" + instrumentId + "\""
                + ",\"quantity\":\"" + quantity + "\""
                + ",\"price\":\"" + price + "\""
                + ",\"buySell\":\"" + buySell + "\""
                + ",\"dateTime\":\"" + dateTime + "\""
                + ",\"status\":\"" + status + "\""
                + ",\"duration\":\"" + duration + "\""
                + ",\"expiryDateTime\":\"" + expiryDateTime + "\""
                + "}";
    }
}