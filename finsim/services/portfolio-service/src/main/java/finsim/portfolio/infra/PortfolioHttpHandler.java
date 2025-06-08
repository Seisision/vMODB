package finsim.portfolio.infra;

import finsim.common.inputs.CreateTransfer;
import finsim.common.events.OrderHandled;
import finsim.common.events.TransferCreated;
import finsim.portfolio.PortfolioService;
import finsim.portfolio.repositories.IPositionRepository;
import finsim.portfolio.repositories.ITransferRepository;
import dk.ku.di.dms.vms.modb.common.serdes.IVmsSerdesProxy;
import dk.ku.di.dms.vms.modb.common.serdes.VmsSerdesProxyBuilder;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.sdk.embed.client.DefaultHttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static finsim.common.Constants.INSTRUMENT_VMS_PORT;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

public final class PortfolioHttpHandler extends DefaultHttpHandler {

    private static final System.Logger LOGGER = System.getLogger(PortfolioHttpHandler.class.getName());

    private final PortfolioService portfolioService;
    private static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();

    public PortfolioHttpHandler(ITransactionManager transactionManager,
                           IPositionRepository positionRepository,
                           ITransferRepository transferRepository) {
        super(transactionManager);
        this.portfolioService = new PortfolioService(positionRepository, transferRepository);
    }

    @Override
    public String getAsJson(String uri) {
        LOGGER.log(DEBUG, "Received GET request: {0}", uri);
        System.out.println("GET request received: " + uri);
        
        try {
            if (uri.matches("/positions/account/\\d+")) {
                int accountId = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
                this.transactionManager.beginTransaction(0, 0, 0, true);
                String positionsJson = SERDES.serialize(this.portfolioService.getPositionsByAccountId(accountId), finsim.common.entities.Position[].class);
                this.transactionManager.commit();
                return positionsJson;
            }             
            else if (uri.matches("/positions/account/\\d+/cash")) {
                String[] parts = uri.split("/");
                int accountId = Integer.parseInt(parts[3]);
                int instrumentId = -1; // Cash is represented by -1 in the instrument ID
                this.transactionManager.beginTransaction(0, 0, 0, true);
                String positionJson = SERDES.serialize(this.portfolioService.getPositionByAccountAndInstrument(accountId, instrumentId), finsim.common.entities.Position.class);
                this.transactionManager.commit();
                return positionJson;
            } 
            else if (uri.matches("/positions/account/\\d+/instrument/\\d+")) {
                String[] parts = uri.split("/");
                int accountId = Integer.parseInt(parts[3]);
                int instrumentId = Integer.parseInt(parts[5]);
                this.transactionManager.beginTransaction(0, 0, 0, true);
                String positionJson = SERDES.serialize(this.portfolioService.getPositionByAccountAndInstrument(accountId, instrumentId), finsim.common.entities.Position.class);
                this.transactionManager.commit();
                return positionJson;
            }
            else if (uri.matches("/positions/\\d+")) {
                int positionId = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
                this.transactionManager.beginTransaction(0, 0, 0, true);
                String positionJson = SERDES.serialize(this.portfolioService.getPositionById(positionId), finsim.common.entities.Position.class);
                this.transactionManager.commit();
                return positionJson;
            }
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error processing GET request: {0}", e.getMessage());
            System.out.println("ERROR processing GET request: " + e.getMessage());
            e.printStackTrace();
            this.transactionManager.reset();
        }
        return "{}";
    }
}