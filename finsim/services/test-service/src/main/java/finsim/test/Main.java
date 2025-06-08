package finsim.test;

import finsim.test.infra.TestHttpHandler;
import finsim.test.repositories.ITestItemRepository;
import finsim.common.Constants;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplication;
import dk.ku.di.dms.vms.sdk.embed.client.VmsApplicationOptions;

import java.util.Properties;

public final class Main {

    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());

    public static void main(String[] ignoredArgs) throws Exception {
        Properties properties = ConfigUtils.loadProperties();
        VmsApplication vms = buildVms(properties);
        vms.start();
    }

    private static VmsApplication buildVms(Properties properties) throws Exception {
        VmsApplicationOptions options = VmsApplicationOptions.build(
                properties,
                "0.0.0.0",
                Constants.TEST_VMS_PORT, new String[]{
                "finsim.test",
                "finsim.common"
        });
        return VmsApplication.build(options, (x,z) -> new TestHttpHandler(x, (ITestItemRepository) z.apply("test_items")));
    }

}