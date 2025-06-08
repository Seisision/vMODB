package finsim.instrument;

import finsim.common.Constants;
import finsim.instrument.repositories.IInstrumentRepository;
import finsim.instrument.infra.InstrumentHttpHandler;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplication;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplicationOptions;

import java.util.Properties;

public final class Main {
    
    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());
    
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Instrument Service...");
        
        Properties properties = ConfigUtils.loadProperties();
        VmsApplication vms = buildVms(properties);
        vms.start();
        
        System.out.println("Instrument Service started on port " + Constants.INSTRUMENT_VMS_PORT);
    }
    
    private static VmsApplication buildVms(Properties properties) throws Exception {
        VmsApplicationOptions options = VmsApplicationOptions.build(
                properties,
                "0.0.0.0",
                Constants.INSTRUMENT_VMS_PORT, new String[]{
                "finsim.instrument",
                "finsim.common"
        });
        
        return VmsApplication.build(options, (transactionManager, repositoryProvider) -> {
            // Get the repository from MODB
            IInstrumentRepository instrumentRepository = (IInstrumentRepository) repositoryProvider.apply("instruments");
            
            System.out.println("Instrument repository initialized.");

            // Create and return the HTTP handler that will use our service
            return new InstrumentHttpHandler(transactionManager, instrumentRepository);
        });
    }
}