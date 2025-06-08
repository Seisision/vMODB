package finsim.test.infra;

import finsim.test.entities.TestItem;
import finsim.test.repositories.ITestItemRepository;
import dk.ku.di.dms.vms.modb.common.serdes.IVmsSerdesProxy;
import dk.ku.di.dms.vms.modb.common.serdes.VmsSerdesProxyBuilder;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.sdk.embed.client.DefaultHttpHandler;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

import java.util.List;

public final class TestHttpHandler extends DefaultHttpHandler {

    private static final System.Logger LOGGER = System.getLogger(TestHttpHandler.class.getName());

    private final ITestItemRepository repository;
    private static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();

    public TestHttpHandler(ITransactionManager transactionManager,
                           ITestItemRepository repository){
        super(transactionManager);
        this.repository = repository;
    }

    @Override
    public void patch(String uri, String body) {
        LOGGER.log(DEBUG, "Received patch request: {0}", uri);
        String[] split = uri.split("/");
        String op = split[split.length - 1];
        if(op.contentEquals("add")) {
            LOGGER.log(DEBUG, "Received add request: {0}", uri);
            finsim.common.entities.TestItem testItemAPI =
                    SERDES.deserialize(body, finsim.common.entities.TestItem.class);
            this.transactionManager.beginTransaction(0, 0, 0, false);
            this.repository.insert(TestUtils.convertTestItemAPI(testItemAPI));

            LOGGER.log(DEBUG, "Added test item: {0}", testItemAPI);
            return;
        }
        else
        {
            LOGGER.log(ERROR, "Received unknown request: {0}", uri);
        }
        this.transactionManager.reset();
    }

    @Override
    public String getAsJson(String uri) {
        String[] split = uri.split("/");
        int secondaryId = Integer.parseInt(split[split.length - 1]);
        this.transactionManager.beginTransaction( 0, 0, 0,true );
        List<TestItem> testItems = this.repository.getTestItemsByPrimaryId(secondaryId);
        return SERDES.serializeList(testItems);
    }
}