package finsim.market.repositories;

import finsim.market.entities.MarketDataEntity;
import dk.ku.di.dms.vms.modb.api.annotations.Query;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;

import java.util.List;

public interface IMarketDataRepository extends IRepository<MarketDataEntity.MarketDataId, MarketDataEntity> {
    @Query("select * from market_data where instrument_id = :instrument_id")
    MarketDataEntity getMarketDataByInstrumentId(String instrument_id);
    
    @Query("select * from market_data")
    List<MarketDataEntity> getAllMarketData();
}