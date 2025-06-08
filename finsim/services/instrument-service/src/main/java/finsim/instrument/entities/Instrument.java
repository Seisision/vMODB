package finsim.instrument.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Entity;
import java.util.Date;
import java.util.Objects;

@Entity
@VmsTable(name="instruments")
public final class Instrument implements IEntity<Integer> {
    
    @Id
    @VmsIndex(name = "instrument_idx")
    public int instrument_id;
    
    @Column
    public String name;
    
    @Column
    @VmsIndex(name = "isin_idx")
    public String isin;
    
    @Column
    public String asset_class;
    
    @Column
    public int lot_size;
    
    @Column
    public String currency;
    
    @Column
    @VmsIndex(name = "tradeable_idx")
    public int tradeable; // Changed from boolean to int (0 = false, 1 = true)
    
    @Column
    public Date created_at;
    
    @Column
    public Date updated_at;

    public Instrument() {}

    public Instrument(int id, String name, String isin, String assetClass, int lotSize, String currency, boolean tradeable) {
        this.instrument_id = id;
        this.name = name;
        this.isin = isin;
        this.asset_class = assetClass;
        this.lot_size = lotSize;
        this.currency = currency;
        this.tradeable = tradeable ? 1 : 0; // Convert boolean to int
        this.created_at = new Date();
        this.updated_at = new Date();
    }
    
    // Helper method to check if instrument is tradeable
    public boolean isTradeable() {
        return this.tradeable == 1;
    }
    
    // Helper method to set tradeable status
    public void setTradeable(boolean tradeable) {
        this.tradeable = tradeable ? 1 : 0;
    }
    
    // @Override
    // public Integer getId() {
    //     return instrument_id;
    // }


    @Override
    public String toString() {
        return "Instrument{" +
                "id='" + instrument_id + '\'' +
                ", name='" + name + '\'' +
                ", isin='" + isin + '\'' +
                ", asset_class='" + asset_class + '\'' +
                ", lot_size=" + lot_size +
                ", currency='" + currency + '\'' +
                ", tradeable=" + (tradeable == 1) +
                '}';
    }
}