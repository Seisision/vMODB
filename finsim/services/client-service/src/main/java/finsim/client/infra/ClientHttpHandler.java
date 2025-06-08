package finsim.client.infra;

import finsim.common.inputs.CreateAccount;
import finsim.common.inputs.UpdateAccount;
import finsim.common.inputs.CreateClient;
import finsim.common.inputs.UpdateClient;
import finsim.common.events.AccountCreated;
import finsim.common.events.AccountChanged;
import finsim.common.events.ClientCreated;
import finsim.common.events.ClientChanged;
import finsim.client.ClientService;
import finsim.client.repositories.IAccountRepository;
import finsim.client.repositories.IClientRepository;
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

public final class ClientHttpHandler extends DefaultHttpHandler {

    private static final System.Logger LOGGER = System.getLogger(ClientHttpHandler.class.getName());

    private final ClientService clientService;
    private static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();

    public ClientHttpHandler(ITransactionManager transactionManager,
                           IAccountRepository accountRepository,
                           IClientRepository clientRepository) {
        super(transactionManager);
        this.clientService = new ClientService(accountRepository, clientRepository);
    }
    
    @Override
    public String getAsJson(String uri) {
        LOGGER.log(DEBUG, "Received GET request: {0}", uri);
        System.out.println("GET request received: " + uri);
        
        try {
            if (uri.equals("/clients")) {
                this.transactionManager.beginTransaction(0, 0, 0, true);
                String clientsJson = SERDES.serializeList(this.clientService.getAllClients());
                this.transactionManager.commit();
                return clientsJson;
            } else if (uri.matches("/clients/\\d+")) {
                int clientId = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
                this.transactionManager.beginTransaction(0, 0, 0, true);
                finsim.common.entities.Client client = this.clientService.getClientById(clientId);
                String clientJson = SERDES.serialize(client, finsim.common.entities.Client.class);
                this.transactionManager.commit();
                return clientJson;
            } else if (uri.equals("/accounts")) {
                this.transactionManager.beginTransaction(0, 0, 0, true);
                String accountsJson = SERDES.serializeList(this.clientService.getAllAccounts());
                this.transactionManager.commit();
                return accountsJson;
            } else if (uri.matches("/accounts/\\d+")) {
                int accountId = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
                this.transactionManager.beginTransaction(0, 0, 0, true);
                finsim.common.entities.Account account = this.clientService.getAccountById(accountId);
                String accountJson = SERDES.serialize(account, finsim.common.entities.Account.class);
                this.transactionManager.commit();
                return accountJson;
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