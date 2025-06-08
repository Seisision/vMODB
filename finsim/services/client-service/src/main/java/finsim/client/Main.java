package finsim.client;

import finsim.common.Constants;
import finsim.client.repositories.IAccountRepository;
import finsim.client.repositories.IClientRepository;
import finsim.client.infra.ClientHttpHandler;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplication;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplicationOptions;

import java.util.Properties;

public final class Main {
    
    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());
    
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Client Service...");
        
        Properties properties = ConfigUtils.loadProperties();
        VmsApplication vms = buildVms(properties);
        vms.start();
        
        System.out.println("Client Service started on port " + Constants.ORDER_VMS_PORT);
    }
    
    private static VmsApplication buildVms(Properties properties) throws Exception {
        VmsApplicationOptions options = VmsApplicationOptions.build(
                properties,
                "0.0.0.0",
                Constants.CLIENT_VMS_PORT, new String[]{
                "finsim.client",
                "finsim.common"
        });
        
        return VmsApplication.build(options, (transactionManager, repositoryProvider) -> {
            // Get the repository from MODB
            IClientRepository clientRepository = (IClientRepository) repositoryProvider.apply("clients");
            IAccountRepository accountRepository = (IAccountRepository) repositoryProvider.apply("accounts");
            
            // Create and return the HTTP handler that will use our service
            return new ClientHttpHandler(transactionManager, accountRepository, clientRepository);
        });
    }
}