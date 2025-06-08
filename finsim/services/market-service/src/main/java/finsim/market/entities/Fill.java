package finsim.market.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;
import java.util.Date;

@Entity
@VmsTable(name="fills")
@IdClass(Fill.FillId.class)
public class Fill implements IEntity<Fill.FillId> {
    
    public static class FillId implements Serializable {
        public String fill_id;
        
        public FillId() {}
        
        public FillId(String fill_id) {
            this.fill_id = fill_id;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FillId that = (FillId) o;
            return fill_id != null ? fill_id.equals(that.fill_id) : that.fill_id == null;
        }
        
        @Override
        public int hashCode() {
            return fill_id != null ? fill_id.hashCode() : 0;
        }
    }

    @Id
    @VmsIndex(name = "fills_idx")
    public String fill_id;
    
    @Column
    public String transaction_id;
    
    @Column
    @VmsIndex(name = "instrument_idx")
    public String instrument_id;
    
    @Column
    public double quantity;
    
    @Column
    public double price;
    
    @Column
    public Date timestamp;

    public Fill() {}

    public Fill(String fill_id, String transaction_id, String instrument_id, 
                double quantity, double price, Date timestamp) {
        this.fill_id = fill_id;
        this.transaction_id = transaction_id;
        this.instrument_id = instrument_id;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }
}