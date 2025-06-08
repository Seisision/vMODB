package finsim.portfolio;

import finsim.common.Constants;
import finsim.portfolio.repositories.IPositionRepository;
import finsim.portfolio.repositories.ITransferRepository;
import finsim.portfolio.infra.PortfolioHttpHandler;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplication;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplicationOptions;

import java.util.Properties;

public final class Main {
    
    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());
    
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Portfolio Service...");
        
        Properties properties = ConfigUtils.loadProperties();
        VmsApplication vms = buildVms(properties);
        vms.start();
        
        System.out.println("Portfolio Service started on port " + Constants.PORTFOLIO_VMS_PORT);
    }
    
    private static VmsApplication buildVms(Properties properties) throws Exception {
        VmsApplicationOptions options = VmsApplicationOptions.build(
                properties,
                "0.0.0.0",
                Constants.PORTFOLIO_VMS_PORT, new String[]{
                "finsim.portfolio",
                "finsim.common"
        });
        
        return VmsApplication.build(options, (transactionManager, repositoryProvider) -> {
            // Get the repository from MODB
            IPositionRepository positionRepository = (IPositionRepository) repositoryProvider.apply("positions");
            ITransferRepository transferRepository = (ITransferRepository) repositoryProvider.apply("transfers");
            
            // Create and return the HTTP handler that will use our service
            return new PortfolioHttpHandler(transactionManager, positionRepository, transferRepository);
        });
    }
}