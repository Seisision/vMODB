package finsim.portfolio.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.annotations.VmsForeignKey;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@VmsTable(name="positions")
public final class Position implements IEntity<Integer> {
    
    @Id
    public int id;
    
    @Column
    public Date created_at;
    
    @Column
    public Date updated_at;

    @Column
    public int instrument_id;

    @Column
    public int amount;

    @Column
    public double open_price;

    @Column
    public int account_id;

    public Position() {}

    public Position(int id, int instrument_id, int amount, double open_price, int account_id) {
        this.id = id;
        this.instrument_id = instrument_id;
        this.amount = amount;
        this.open_price = open_price;
        this.account_id = account_id;
        this.created_at = new Date();
        this.updated_at = new Date();
    }
    
    @Override
    public String toString() {
        return "Position{" +
                "id=" + id +
                ", instrument_id=" + instrument_id +
                ", amount=" + amount +
                ", open_price=" + open_price +
                ", account_id=" + account_id +
                ", created_at=" + created_at +
                ", updated_at=" + updated_at +
                '}';
    }
}