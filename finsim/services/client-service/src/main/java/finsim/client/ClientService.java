package finsim.client;
import finsim.common.inputs.CreateAccount;
import finsim.common.events.AccountCreated;
import finsim.common.inputs.UpdateAccount;
import finsim.common.events.AccountChanged;
import finsim.client.infra.AccountUtils;
import finsim.client.repositories.IAccountRepository;
import finsim.client.entities.Account;

import finsim.common.inputs.CreateClient;
import finsim.common.events.ClientCreated;
import finsim.common.inputs.UpdateClient;
import finsim.common.events.ClientChanged;
import finsim.client.infra.ClientUtils;
import finsim.client.repositories.IClientRepository;
import finsim.client.entities.Client;

import dk.ku.di.dms.vms.modb.api.annotations.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.List;

import static finsim.common.Constants.*;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.RW;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.W;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Microservice("client")
public final class ClientService {

    private static final System.Logger LOGGER = System.getLogger(ClientService.class.getName());

    private final IAccountRepository accountRepository;
    private final IClientRepository clientRepository;

    public ClientService(IAccountRepository accountRepository, IClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Inbound(values = {CREATE_ACCOUNT})
    @Outbound(ACCOUNT_CREATED)
    @Transactional(type=W)
    public AccountCreated createAccount(CreateAccount createAccount) {
        LOGGER.log(INFO, "APP: Client-service received a create account request for ID " + createAccount.id);
        System.out.println("APP: Client-service received a create account request for ID " + createAccount.id);
        
        // Validate account creation parameters
        if (createAccount.name == null || createAccount.name.trim().isEmpty()) {
            LOGGER.log(ERROR, "APP: Invalid account creation - name cannot be empty");
            return new AccountCreated(new Date(), createAccount, null, createAccount.instanceId);
        }

        // Validate client ID
        if (createAccount.clientId == null || createAccount.clientId.trim().isEmpty()) {
            LOGGER.log(ERROR, "APP: Invalid account creation - clientId cannot be empty");
            return new AccountCreated(new Date(), createAccount, null, createAccount.instanceId);
        }
        // Check if the client exists
        Client existingClient = this.clientRepository.lookupByKey(Integer.parseInt(createAccount.clientId));
        if (existingClient == null) {
            LOGGER.log(ERROR, "APP: Client with ID " + createAccount.clientId + " does not exist");
            return new AccountCreated(new Date(), createAccount, null, createAccount.instanceId);
        }        
        
        // Create the account entity
        Account account = new Account(
            Integer.parseInt(createAccount.id),
            createAccount.name,
            true,  // Active by default
            Integer.parseInt(createAccount.clientId)  // Assuming clientId is provided as a string
        );
        
        this.accountRepository.insert(account);
        Account savedAccount = this.accountRepository.lookupByKey(account.getId());
        
        if (savedAccount != null) {
            LOGGER.log(INFO, "APP: Account created successfully with ID " + savedAccount.getId());
            return new AccountCreated(new Date(), createAccount, AccountUtils.convertAccount(savedAccount), createAccount.instanceId);
        } else {
            LOGGER.log(ERROR, "APP: Failed to create account with ID " + createAccount.id);
            return new AccountCreated(new Date(), createAccount, null, createAccount.instanceId);
        }
    }

    @Inbound(values = {UPDATE_ACCOUNT})
    @Outbound(ACCOUNT_CHANGED)
    @Transactional(type=RW)
    public AccountChanged updateAccount(UpdateAccount updateAccount) {
        LOGGER.log(INFO, "APP: Client-service received an update account request for ID " + updateAccount.id);
        System.out.println("APP: Client-service received an update account request for ID " + updateAccount.id);
        
        // Validate account update parameters
        if (updateAccount.name == null || updateAccount.name.trim().isEmpty()) {
            LOGGER.log(ERROR, "APP: Invalid account update - name cannot be empty");
            return new AccountChanged(new Date(), updateAccount, null, updateAccount.instanceId);
        }
        
        // Lookup the existing account
        Account existingAccount = this.accountRepository.lookupByKey(Integer.parseInt(updateAccount.id));
        
        if (existingAccount != null) {
            // Validate client ID has not changed
            if (existingAccount.client_id != Integer.parseInt(updateAccount.clientId)) {
                LOGGER.log(ERROR, "APP: Client ID cannot be changed for account with ID " + updateAccount.id);
                return new AccountChanged(new Date(), updateAccount, null, updateAccount.instanceId);
            }

            existingAccount.name = updateAccount.name;
            existingAccount.active = updateAccount.isActive ? 1 : 0;  // Update active status if provided
            existingAccount.updated_at = new Date();  // Update timestamp
            
            this.accountRepository.update(existingAccount);
            Account updatedAccount = this.accountRepository.lookupByKey(existingAccount.getId());
            
            if (updatedAccount != null) {
                LOGGER.log(INFO, "APP: Account updated successfully with ID " + updatedAccount.getId());
                return new AccountChanged(new Date(), updateAccount, AccountUtils.convertAccount(updatedAccount), updateAccount.instanceId);
            } else {
                LOGGER.log(ERROR, "APP: Failed to update account with ID " + updateAccount.id);
                return new AccountChanged(new Date(), updateAccount, null, updateAccount.instanceId);
            }
        } else {
            LOGGER.log(ERROR, "APP: Account with ID " + updateAccount.id + " not found");
            return new AccountChanged(new Date(), updateAccount, null, updateAccount.instanceId);
        }
    }

    @Inbound(values = {CREATE_CLIENT})
    @Outbound(CLIENT_CREATED)
    @Transactional(type=W)
    public ClientCreated createClient(CreateClient createClient) {
        LOGGER.log(INFO, "APP: Client-service received a create client request for ID " + createClient.id);
        System.out.println("APP: Client-service received a create client request for ID " + createClient.id);
        
        // Validate client creation parameters
        if (createClient.name == null || createClient.name.trim().isEmpty()) {
            LOGGER.log(ERROR, "APP: Invalid client creation - name cannot be empty");
            return new ClientCreated(new Date(), createClient, null, createClient.instanceId);
        }
        
        // Create the client entity
        Client client = new Client(
            Integer.parseInt(createClient.id),
            createClient.name,
            true  // Active by default
        );
        
        this.clientRepository.insert(client);
        Client savedClient = this.clientRepository.lookupByKey(client.getId());
        
        if (savedClient != null) {
            LOGGER.log(INFO, "APP: Client created successfully with ID " + savedClient.getId());
            return new ClientCreated(new Date(), createClient, ClientUtils.convertClient(savedClient), createClient.instanceId);
        } else {
            LOGGER.log(ERROR, "APP: Failed to create client with ID " + createClient.id);
            return new ClientCreated(new Date(), createClient, null, createClient.instanceId);
        }
    }

    @Inbound(values = {UPDATE_CLIENT})
    @Outbound(CLIENT_CHANGED)
    @Transactional(type=RW)
    public ClientChanged updateClient(UpdateClient updateClient) {
        LOGGER.log(INFO, "APP: Client-service received an update client request for ID " + updateClient.id);
        System.out.println("APP: Client-service received an update client request for ID " + updateClient.id);
        
        // Validate client update parameters
        if (updateClient.name == null || updateClient.name.trim().isEmpty()) {
            LOGGER.log(ERROR, "APP: Invalid client update - name cannot be empty");
            return new ClientChanged(new Date(), updateClient, null, updateClient.instanceId);
        }
        
        // Lookup the existing client
        Client existingClient = this.clientRepository.lookupByKey(Integer.parseInt(updateClient.id));
        
        if (existingClient != null) {
            existingClient.name = updateClient.name;
            existingClient.active = updateClient.isActive ? 1 : 0;  // Update active status if provided
            existingClient.updated_at = new Date();  // Update timestamp
            
            this.clientRepository.update(existingClient);
            Client updatedClient = this.clientRepository.lookupByKey(existingClient.getId());
            
            if (updatedClient != null) {
                LOGGER.log(INFO, "APP: Client updated successfully with ID " + updatedClient.getId());
                return new ClientChanged(new Date(), updateClient, ClientUtils.convertClient(updatedClient), updateClient.instanceId);
            } else {
                LOGGER.log(ERROR, "APP: Failed to update client with ID " + updateClient.id);
                return new ClientChanged(new Date(), updateClient, null, updateClient.instanceId);
            }
        } else {
            LOGGER.log(ERROR, "APP: Client with ID " + updateClient.id + " not found");
            return new ClientChanged(new Date(), updateClient, null, updateClient.instanceId);
        }
    }

    // getAllClients
    public List<finsim.common.entities.Client> getAllClients() {
        LOGGER.log(INFO, "APP: Client-service received a request to get all clients");
        System.out.println("APP: Client-service received a request to get all clients");
        
        List<Client> clients = this.clientRepository.getAll();
        
        if (clients != null && !clients.isEmpty()) {
            LOGGER.log(INFO, "APP: Found " + clients.size() + " clients");
            return ClientUtils.convertClients(clients);
        } else {
            LOGGER.log(INFO, "APP: No clients found");
            return List.of();
        }
    }

    // getAllAccounts
    public List<finsim.common.entities.Account> getAllAccounts() {
        LOGGER.log(INFO, "APP: Client-service received a request to get all accounts");
        System.out.println("APP: Client-service received a request to get all accounts");
        
        List<Account> accounts = this.accountRepository.getAll();
        
        if (accounts != null && !accounts.isEmpty()) {
            LOGGER.log(INFO, "APP: Found " + accounts.size() + " accounts");
            return AccountUtils.convertAccounts(accounts);
        } else {
            LOGGER.log(INFO, "APP: No accounts found");
            return List.of();
        }
    }

    // getClientById
    public finsim.common.entities.Client getClientById(int clientId) {
        LOGGER.log(INFO, "APP: Client-service received a request to get client by ID " + clientId);
        System.out.println("APP: Client-service received a request to get client by ID " + clientId);
        
        Client client = this.clientRepository.lookupByKey(clientId);
        
        if (client != null) {
            LOGGER.log(INFO, "APP: Found client with ID " + clientId);
            return ClientUtils.convertClient(client);
        } else {
            LOGGER.log(ERROR, "APP: Client with ID " + clientId + " not found");
            return null;
        }
    }

    // getAccountById
    public finsim.common.entities.Account getAccountById(int accountId) {
        LOGGER.log(INFO, "APP: Client-service received a request to get account by ID " + accountId);
        System.out.println("APP: Client-service received a request to get account by ID " + accountId);
        
        Account account = this.accountRepository.lookupByKey(accountId);
        
        if (account != null) {
            LOGGER.log(INFO, "APP: Found account with ID " + accountId);
            return AccountUtils.convertAccount(account);
        } else {
            LOGGER.log(ERROR, "APP: Account with ID " + accountId + " not found");
            return null;
        }
    }

}