package finsim.proxy;

import dk.ku.di.dms.vms.coordinator.Coordinator;
import dk.ku.di.dms.vms.coordinator.transaction.TransactionBootstrap;
import dk.ku.di.dms.vms.coordinator.transaction.TransactionDAG;
import dk.ku.di.dms.vms.modb.common.schema.network.node.IdentifiableNode;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;

import java.util.*;

import static finsim.common.Constants.*;

/**
 * The proxy builds on top of the coordinator module.
 * It configures the transaction DAGs between the order and market services.
 */
public final class Main {

    public static void main(String[] ignoredArgs) {
        Properties properties = ConfigUtils.loadProperties();
        loadCoordinator(properties);
    }

    private static Map<String, TransactionDAG> buildTransactionDAGs(String[] transactionList) {
        Map<String, TransactionDAG> transactionMap = new HashMap<>();
        Set<String> transactions = Set.of(transactionList);

        // DEBUG: Log raw transaction list contents
        System.out.println("DEBUG: Transaction list from properties contains " + transactionList.length + " items:");
        for (int i = 0; i < transactionList.length; i++) {
            System.out.println("  " + i + ": '" + transactionList[i] + "' (length: " + transactionList[i].length() + ")");
        }
        
        // DEBUG: Log CREATE_INSTRUMENT constant details
        System.out.println("DEBUG: CREATE_INSTRUMENT constant value: '" + CREATE_INSTRUMENT + "'");
        System.out.println("DEBUG: CREATE_INSTRUMENT length: " + CREATE_INSTRUMENT.length());
        
        // DEBUG: Log contains check for each transaction
        System.out.println("DEBUG: Does transactions contain CREATE_ORDER? " + transactions.contains(CREATE_ORDER));
        System.out.println("DEBUG: Does transactions contain CREATE_INSTRUMENT? " + transactions.contains(CREATE_INSTRUMENT));
        System.out.println("DEBUG: Does transactions contain UPDATE_ORDER? " + transactions.contains(UPDATE_ORDER));
        
        // CREATE_ORDER transaction: Order service creates the order, then Market service processes it
        if (transactions.contains(CREATE_ORDER)) {
            System.out.println("Creating transaction DAG for CREATE_ORDER");
            TransactionDAG createOrderDag = TransactionBootstrap.name(CREATE_ORDER)
                    .input("a", "order", CREATE_ORDER)
                    //.terminal("b", "market", "a")
                    .internal("b", "market", ORDER_CREATED, "a") // internal node for order creation
                    .terminal("c", "portfolio", "b") 
                    // we used to have orderhandled in order_service internal node for market processing (ORDER_HANDLED)
                    .build();
            transactionMap.put(createOrderDag.name, createOrderDag);
            System.out.println("DEBUG: Added CREATE_ORDER DAG with name: '" + createOrderDag.name + "'");
        }

        // CANCEL_ORDER transaction: Order service cancels an order, then Market service processes it
        System.out.println("DEBUG: Checking CANCEL_ORDER condition: " + transactions.contains(CANCEL_ORDER));
        if (transactions.contains(CANCEL_ORDER)) {
            System.out.println("Creating transaction DAG for CANCEL_ORDER");
            TransactionDAG cancelOrderDag = TransactionBootstrap.name(CANCEL_ORDER)
                    .input("a", "order", CANCEL_ORDER)
                    .terminal("b", "market", "a")
                    //TODO
                    //.internal("b", "market", ORDER_CANCEL_ATTEMPTED, "a")
                    //.terminal("c", "order", "b") // changes the order status to CANCELLED if the market accepts the cancellation
                    .build();
            transactionMap.put(cancelOrderDag.name, cancelOrderDag);
            System.out.println("DEBUG: Added CANCEL_ORDER DAG with name: '" + cancelOrderDag.name + "'");
        }

        // ORDER_POSITION_CREATED transaction: Order service creates an order position
        System.out.println("DEBUG: Checking ORDER_POSITION_CREATED condition: " + transactions.contains(ORDER_POSITION_CREATED));
        if (transactions.contains(ORDER_POSITION_CREATED)) {
            System.out.println("Creating transaction DAG for ORDER_POSITION_CREATED");
            TransactionDAG orderPositionCreatedDag = TransactionBootstrap.name(ORDER_POSITION_CREATED)
                    .input("a", "order", ORDER_POSITION_CREATED)
                    .terminal("b", "order", "a")
                    .build();
            transactionMap.put(orderPositionCreatedDag.name, orderPositionCreatedDag);
            System.out.println("DEBUG: Added ORDER_POSITION_CREATED DAG with name: '" + orderPositionCreatedDag.name + "'");
        }

        // CREATE_INSTRUMENT transaction: Instrument service creates an instrument
        System.out.println("DEBUG: Checking CREATE_INSTRUMENT condition: " + transactions.contains(CREATE_INSTRUMENT));
        if (transactions.contains(CREATE_INSTRUMENT)) {
            System.out.println("Creating transaction DAG for CREATE_INSTRUMENT");
            TransactionDAG createInstrumentDag = TransactionBootstrap.name(CREATE_INSTRUMENT)
                    .input("a", "instrument", CREATE_INSTRUMENT)
                    .terminal("b", "order", "a")
                    .build();
            System.out.println("DEBUG: Built CREATE_INSTRUMENT DAG with name: '" + createInstrumentDag.name + "'");
            transactionMap.put(createInstrumentDag.name, createInstrumentDag);
            System.out.println("DEBUG: Added CREATE_INSTRUMENT DAG to map with key: '" + createInstrumentDag.name + "'");
        } 

        // else {
        //     System.out.println("DEBUG: Not creating CREATE_INSTRUMENT DAG - not in transaction list");
            
        //     // FORCE CREATE the DAG regardless of property settings - for testing
        //     System.out.println("DEBUG: FORCE CREATING the CREATE_INSTRUMENT DAG for testing");
        //     TransactionDAG forcedDag = TransactionBootstrap.name(CREATE_INSTRUMENT)
        //             .input("a", "instrument", CREATE_INSTRUMENT)
        //             .build();
        //     transactionMap.put(forcedDag.name, forcedDag);
        //     System.out.println("DEBUG: Force-added CREATE_INSTRUMENT DAG with name: '" + forcedDag.name + "'");
        // }

        // UPDATE_ORDER transaction: Order service updates an existing order
         if (transactions.contains(UPDATE_ORDER)) {
             TransactionDAG updateOrderDag = TransactionBootstrap.name(UPDATE_ORDER)
                     .input("a", "order", UPDATE_ORDER)
                     .terminal("b", "market", "a")
                     .build();
             transactionMap.put(updateOrderDag.name, updateOrderDag);
         }

        // CANCEL_ORDER transaction: Order service cancels an existing order
         if (transactions.contains(CANCEL_ORDER)) {
             TransactionDAG cancelOrderDag = TransactionBootstrap.name(CANCEL_ORDER)
                     .input("a", "order", CANCEL_ORDER)
                     .terminal("b", "market", "a")
                     .build();
             transactionMap.put(cancelOrderDag.name, cancelOrderDag);
         }

        return transactionMap;
    }

    private static void loadCoordinator(Properties properties) {
        // Get list of transactions from properties
        String transactionsRaw = properties.getProperty("transactions");
        if (transactionsRaw == null) {
            throw new RuntimeException("Make sure the app.properties contain a 'transactions' entry");
        }
        
        String[] transactions = transactionsRaw.split(",");
        Map<String, TransactionDAG> transactionMap = buildTransactionDAGs(transactions);

        // Set up service nodes
        Map<String, IdentifiableNode> serviceNodes = buildServiceNodes(properties);
        
        // Debug: Print DAG information
        System.out.println("transactionDAG count: " + transactionMap.size());
        System.out.println("Transaction DAGs configured:");
        for (String key : transactionMap.keySet()) {
            System.out.println("  DAG: " + key);
            TransactionDAG dag = transactionMap.get(key);
            System.out.println("    Input events: " + dag.inputEvents.keySet());
            System.out.println("    Internal nodes: " + Arrays.toString(dag.internalNodes.toArray()));
            System.out.println("    Terminal nodes: " + Arrays.toString(dag.terminalNodes.toArray()));
        }
        
        // Build coordinator with configured DAGs and services
        Coordinator coordinator = Coordinator.build(properties, serviceNodes, transactionMap, ProxyHttpServerAsyncJdk::new);

        // Start the coordinator in a separate thread
        Thread coordinatorThread = new Thread(coordinator);
        coordinatorThread.start();
        
        System.out.println("Finsim proxy started successfully. Services configured: " + 
                           String.join(", ", serviceNodes.keySet()));
        System.out.println("Transactions configured: " + String.join(", ", transactionMap.keySet()));
    }

    private static Map<String, IdentifiableNode> buildServiceNodes(Properties properties) {
        Map<String, IdentifiableNode> serviceNodes = new HashMap<>();
    
        // Get market service configuration
        String marketHost = properties.getProperty("market_host");
        if (marketHost == null) throw new RuntimeException("Market host is null");
        
        String marketPortStr = properties.getProperty("market_port");
        if (marketPortStr == null) throw new RuntimeException("Market port is null");
        
        int marketPort = Integer.parseInt(marketPortStr);
        IdentifiableNode marketNode = new IdentifiableNode("market", marketHost, marketPort);
        serviceNodes.put(marketNode.identifier, marketNode);

        // Get order service configuration
        String orderHost = properties.getProperty("order_host");
        if (orderHost == null) throw new RuntimeException("Order host is null");
        
        String orderPortStr = properties.getProperty("order_port");
        if (orderPortStr == null) throw new RuntimeException("Order port is null");
        
        int orderPort = Integer.parseInt(orderPortStr);
        IdentifiableNode orderNode = new IdentifiableNode("order", orderHost, orderPort);
        serviceNodes.put(orderNode.identifier, orderNode);

        // get instrument service configuration
        String instrumentHost = properties.getProperty("instrument_host");
        if (instrumentHost == null) throw new RuntimeException("Instrument host is null");

        String instrumentPortStr = properties.getProperty("instrument_port");
        if (instrumentPortStr == null) throw new RuntimeException("Instrument port is null");

        int instrumentPort = Integer.parseInt(instrumentPortStr);
        IdentifiableNode instrumentNode = new IdentifiableNode("instrument", instrumentHost, instrumentPort);
        serviceNodes.put(instrumentNode.identifier, instrumentNode);

        // get portfolio service configuration
        String portfolioHost = properties.getProperty("portfolio_host");
        if (portfolioHost == null) throw new RuntimeException("Portfolio host is null");

        String portfolioPortStr = properties.getProperty("portfolio_port");
        if (portfolioPortStr == null) throw new RuntimeException("Portfolio port is null");

        int portfolioPort = Integer.parseInt(portfolioPortStr);
        IdentifiableNode portfolioNode = new IdentifiableNode("portfolio", portfolioHost, portfolioPort);
        serviceNodes.put(portfolioNode.identifier, portfolioNode);
    
        return serviceNodes;
    }
}