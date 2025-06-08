package finsim.market;

import finsim.common.Constants;
import finsim.market.infra.MarketHttpHandler;
import finsim.market.repositories.IFillRepository;
import finsim.market.repositories.IMarketDataRepository;
import finsim.market.repositories.IMarketStateRepository;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplication;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplicationOptions;

import java.util.Properties;

public final class Main {

    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Market Service...");
        
        Properties properties = ConfigUtils.loadProperties();
        System.out.println("Properties: ");
        System.out.println(properties);
        
        VmsApplication vms = buildVms(properties);
        vms.start();
        
        System.out.println("Market Service started on port " + Constants.MARKET_VMS_PORT);
    }

    private static VmsApplication buildVms(Properties properties) throws Exception {
        VmsApplicationOptions options = VmsApplicationOptions.build(
                properties,
                "0.0.0.0",
                Constants.MARKET_VMS_PORT, new String[]{
                "finsim.market",
                "finsim.common"
        });
        
        return VmsApplication.build(options, (transactionManager, repositoryFactory) -> new MarketHttpHandler(
            transactionManager, 
            (IFillRepository) repositoryFactory.apply("fills"),
            (IMarketStateRepository) repositoryFactory.apply("marketState"),
            (IMarketDataRepository) repositoryFactory.apply("marketData")
        ));
    }
}