package finsim.market.repositories;

import finsim.market.entities.MarketState;
import dk.ku.di.dms.vms.modb.api.annotations.Query;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;

public interface IMarketStateRepository extends IRepository<MarketState.MarketStateId, MarketState> {
    @Query("select * from market_state limit 1")
    MarketState getCurrentMarketState();
}