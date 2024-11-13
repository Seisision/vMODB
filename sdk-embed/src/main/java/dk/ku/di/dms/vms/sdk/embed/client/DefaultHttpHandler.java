package dk.ku.di.dms.vms.sdk.embed.client;

import dk.ku.di.dms.vms.modb.common.serdes.IVmsSerdesProxy;
import dk.ku.di.dms.vms.modb.common.serdes.VmsSerdesProxyBuilder;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.web_common.IHttpHandler;

public class DefaultHttpHandler implements IHttpHandler {

    protected static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();

    protected final ITransactionManager transactionManager;

    public DefaultHttpHandler(ITransactionManager transactionManager){
        this.transactionManager = transactionManager;
    }

    @Override
    public void patch(String uri, String body) {
        this.transactionManager.reset();
    }

}
