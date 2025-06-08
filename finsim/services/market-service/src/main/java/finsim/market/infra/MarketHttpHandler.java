package finsim.market.infra;

import finsim.common.inputs.OrderRouted;
import finsim.market.MarketService;
import finsim.market.entities.Fill;
import finsim.market.entities.MarketState;
import finsim.market.entities.MarketDataEntity;
import finsim.market.repositories.IFillRepository;
import finsim.market.repositories.IMarketDataRepository;
import finsim.market.repositories.IMarketStateRepository;
import dk.ku.di.dms.vms.modb.common.serdes.IVmsSerdesProxy;
import dk.ku.di.dms.vms.modb.common.serdes.VmsSerdesProxyBuilder;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.sdk.embed.client.DefaultHttpHandler;

import java.util.List;

import static finsim.common.Constants.*;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

public class MarketHttpHandler extends DefaultHttpHandler {
    private static final System.Logger LOGGER = System.getLogger(MarketHttpHandler.class.getName());
    private static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();
    
    private final MarketService marketService;
    private final IFillRepository fillRepository;
    private final IMarketStateRepository marketStateRepository;
    private final IMarketDataRepository marketDataRepository;
    
    public MarketHttpHandler(
            ITransactionManager transactionManager,
            IFillRepository fillRepository,
            IMarketStateRepository marketStateRepository,
            IMarketDataRepository marketDataRepository) {
        super(transactionManager);
        this.fillRepository = fillRepository;
        this.marketStateRepository = marketStateRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketService = new MarketService(fillRepository, marketStateRepository, marketDataRepository);
    }    
}