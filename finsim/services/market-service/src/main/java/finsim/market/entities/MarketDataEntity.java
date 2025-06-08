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

@VmsTable(name="market_data")
@Entity
@IdClass(MarketDataEntity.MarketDataId.class)
public final class MarketDataEntity implements IEntity<MarketDataEntity.MarketDataId> {
    
    public static class MarketDataId implements Serializable {
        public String instrument_id;
        
        public MarketDataId() {}
        
        public MarketDataId(String instrument_id) {
            this.instrument_id = instrument_id;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MarketDataId that = (MarketDataId) o;
            return instrument_id != null ? instrument_id.equals(that.instrument_id) : that.instrument_id == null;
        }
        
        @Override
        public int hashCode() {
            return instrument_id != null ? instrument_id.hashCode() : 0;
        }
    }
    
    @Id
    @VmsIndex(name = "market_data_idx")
    public String instrument_id;
    
    @Column
    public double last_price;
    
    @Column
    public Date last_updated;
    
    public MarketDataEntity() {}
    
    public MarketDataEntity(String instrument_id, double last_price, Date last_updated) {
        this.instrument_id = instrument_id;
        this.last_price = last_price;
        this.last_updated = last_updated;
    }
    
    @Override
    public String toString() {
        return "MarketDataEntity{" +
                "instrument_id='" + instrument_id + '\'' +
                ", last_price=" + last_price +
                ", last_updated=" + last_updated +
                '}';
    }
}