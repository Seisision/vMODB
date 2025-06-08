package finsim.order.entities;

import finsim.common.enums.BuySellType;
import finsim.common.enums.OrderStatus;
import finsim.common.enums.OrderDuration;
import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@VmsTable(name="orders")
public final class Order implements IEntity<Integer> {
    
    @Id
    @VmsIndex(name = "order_idx")
    public int id;
    
    @Column
    public String account_id;
    
    @Column
    public String instrument_id;
    
    @Column
    public int quantity;
    
    @Column
    public double price;
    
    @Column
    public BuySellType buy_sell;
    
    @Column
    public Date date_time;
    
    @Column
    @VmsIndex(name = "status_idx")
    public OrderStatus status;
    
    @Column
    public OrderDuration duration;
    
    @Column
    public Date expiry_date_time;
    
    @Column
    public Date created_at;
    
    @Column
    public Date updated_at;

    @Column
    public Date execution_date_time;

    public Order() {}

    public Order(int id, String accountId, String instrumentId, int quantity, double price, 
                BuySellType buySell, Date dateTime, OrderStatus status, OrderDuration duration, 
                Date expiryDateTime) {
        this.id = id;
        this.account_id = accountId;
        this.instrument_id = instrumentId;
        this.quantity = quantity;
        this.price = price;
        this.buy_sell = buySell;
        this.date_time = dateTime;
        this.status = status;
        this.duration = duration;
        this.expiry_date_time = expiryDateTime;
        this.created_at = new Date();
        this.updated_at = new Date();
    }
    
    @Override
    public Integer getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", account_id='" + account_id + '\'' +
                ", instrument_id='" + instrument_id + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", buy_sell=" + buy_sell +
                ", date_time=" + date_time +
                ", status=" + status +
                ", duration=" + duration +
                ", expiry_date_time=" + expiry_date_time +
                ", execution_date_time=" + execution_date_time +
                '}';
    }
}