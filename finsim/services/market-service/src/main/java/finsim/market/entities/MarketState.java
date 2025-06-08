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

@VmsTable(name="market_state")
@Entity
@IdClass(MarketState.MarketStateId.class)
public class MarketState implements IEntity<MarketState.MarketStateId> {
    
    public static class MarketStateId implements Serializable {
        public Date trading_date;
        
        public MarketStateId() {}
        
        public MarketStateId(Date trading_date) {
            this.trading_date = trading_date;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MarketStateId that = (MarketStateId) o;
            return trading_date != null ? trading_date.equals(that.trading_date) : that.trading_date == null;
        }
        
        @Override
        public int hashCode() {
            return trading_date != null ? trading_date.hashCode() : 0;
        }
    }
    
    public enum Status {
        OPEN,
        CLOSED,
        HALTED
    }
    
    @Column
    public Status market_status;     // Overall market status
    
    @Id
    @VmsIndex(name = "market_state_idx")
    public Date trading_date;        // Current trading day
    
    @Column
    public Date market_open_time;     // When market opened/will open
    
    @Column
    public Date market_close_time;    // When market closed/will close
    
    public MarketState() {}
    
    public MarketState(Status marketStatus, Date tradingDate, 
                       Date marketOpenTime, Date marketCloseTime) {
        this.market_status = marketStatus;
        this.trading_date = tradingDate;
        this.market_open_time = marketOpenTime;
        this.market_close_time = marketCloseTime;
    }
}