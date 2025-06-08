package finsim.order;

import finsim.common.Constants;
import finsim.order.repositories.IOrderRepository;
import finsim.order.repositories.IInstrumentReplicaRepository;
import finsim.order.infra.OrderHttpHandler;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplication;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplicationOptions;

import java.util.Properties;

public final class Main {
    
    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());
    
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Order Service...");
        
        Properties properties = ConfigUtils.loadProperties();
        VmsApplication vms = buildVms(properties);
        vms.start();
        
        System.out.println("Order Service started on port " + Constants.ORDER_VMS_PORT);
    }
    
    private static VmsApplication buildVms(Properties properties) throws Exception {
        VmsApplicationOptions options = VmsApplicationOptions.build(
                properties,
                "0.0.0.0",
                Constants.ORDER_VMS_PORT, new String[]{
                "finsim.order",
                "finsim.common"
        });
        
        return VmsApplication.build(options, (transactionManager, repositoryProvider) -> {
            // Get the repository from MODB
            IOrderRepository orderRepository = (IOrderRepository) repositoryProvider.apply("orders");
            IInstrumentReplicaRepository instrumentReplicaRepository = (IInstrumentReplicaRepository) repositoryProvider.apply("instrumentReplicas");
            
            // Create and return the HTTP handler that will use our service
            return new OrderHttpHandler(transactionManager, orderRepository, instrumentReplicaRepository);
        });
    }
}