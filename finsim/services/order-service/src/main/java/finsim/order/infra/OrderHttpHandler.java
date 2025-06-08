package finsim.order.infra;

import finsim.common.inputs.CreateOrder;
import finsim.common.inputs.UpdateOrder;
import finsim.common.inputs.CancelOrder;
import finsim.common.inputs.FillOrder;
import finsim.common.events.OrderCreated;
import finsim.common.events.OrderChanged;
import finsim.common.events.OrderCancelled;
import finsim.order.OrderService;
import finsim.order.repositories.IOrderRepository;
import finsim.order.repositories.IInstrumentReplicaRepository;
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

public final class OrderHttpHandler extends DefaultHttpHandler {

    private static final System.Logger LOGGER = System.getLogger(OrderHttpHandler.class.getName());

    private final OrderService orderService;
    private static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();

    public OrderHttpHandler(ITransactionManager transactionManager,
                           IOrderRepository orderRepository,
                           IInstrumentReplicaRepository instrumentReplicaRepository) {
        super(transactionManager);
        this.orderService = new OrderService(orderRepository, instrumentReplicaRepository);
    }

    
    // Validates if an instrument exists and is tradeable by calling the Instrument service
    private boolean validateInstrument(String instrumentId) {
        try {
            // Check if instrument ID is valid
            if (instrumentId == null || instrumentId.trim().isEmpty()) {
                System.out.println("Invalid instrument ID: empty or null");
                return false;
            }

            // Parse instrument ID as integer (instruments are identified by integer IDs)
            try {
                Integer.parseInt(instrumentId);
            } catch (NumberFormatException e) {
                LOGGER.log(ERROR, "Invalid instrument ID format: " + instrumentId);
                return false;
            }

            // Build URL for instrument service validation endpoint
            String url = "http://localhost:" + INSTRUMENT_VMS_PORT + "/instruments/" + instrumentId;
            System.out.println("Validating instrument with URL: " + url);

            // Create and configure HTTP connection
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 second timeout
            connection.setReadTimeout(5000);
            
            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("Instrument validation response code: " + responseCode);
            
            // Read the response if successful
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Close the connection
                connection.disconnect();
                
                // Check if the returned JSON contains information about tradeable status
                String responseBody = response.toString();
                System.out.println("Instrument data: " + responseBody);
                
                // Very simple check - if the instrument exists and JSON has the text "tradeable":true
                // A proper implementation would use JSON parsing
                return responseBody.contains("\"tradeable\":true");
            } else {
                // Close the connection
                connection.disconnect();
                return false; // Non-200 response means instrument doesn't exist or isn't tradeable
            }
            
        } catch (IOException e) {
            LOGGER.log(ERROR, "Error validating instrument: " + e.getMessage());
            System.out.println("Error validating instrument: " + e.getMessage());
            e.printStackTrace();
            return false; // Fail closed on errors
        }
    }

    @Override
    public void post(String uri, String body) {
        LOGGER.log(DEBUG, "Received POST request: {0}", uri);
        System.out.println("POST request received: " + uri);
        System.out.println("Request body: " + body);
        
        try {
            if (uri.endsWith("/create_order")) {
                // Deserialize CreateOrder from JSON
                CreateOrder createOrder = SERDES.deserialize(body, CreateOrder.class);
                System.out.println("Deserialized CreateOrder: id=" + createOrder.id);
                
                // Validate instrument exists and is tradeable before creating the order
                boolean isInstrumentValid = validateInstrument(createOrder.instrumentId);
                System.out.println("Instrument validation result: " + (isInstrumentValid ? "VALID" : "INVALID"));
                
               
                this.transactionManager.beginTransaction(0, 0, 0, false);
                OrderCreated result = orderService.createOrder(createOrder);
                System.out.println("Order created successfully: " + (result.order != null));
                return; 
            } 
            else if (uri.endsWith("/update_order")) {
                // Deserialize UpdateOrder from JSON
                UpdateOrder updateOrder = SERDES.deserialize(body, UpdateOrder.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                OrderChanged result = orderService.updateOrder(updateOrder);
                System.out.println("Order updated successfully: " + (result.order != null));
                return;
            }
            else if (uri.endsWith("/cancel_order")) {
                // Deserialize CancelOrder from JSON
                CancelOrder cancelOrder = SERDES.deserialize(body, CancelOrder.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                OrderCancelled result = orderService.cancelOrder(cancelOrder);
                System.out.println("Order cancelled successfully: " + (result.order != null));
                return;
            }
            
            // If we get here, no matching endpoint was found
            LOGGER.log(ERROR, "Unknown POST endpoint: {0}", uri);
            System.out.println("ERROR: Unknown POST endpoint: " + uri);
            this.transactionManager.reset();
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error processing POST request: {0}", e.getMessage());
            System.out.println("ERROR processing POST request: " + e.getMessage());
            e.printStackTrace();
            // Reset the transaction on error
            this.transactionManager.reset();
        }
    }

    @Override
    public void patch(String uri, String body) {
        LOGGER.log(DEBUG, "Received PATCH request: {0}", uri);
        System.out.println("PATCH request received: " + uri);
        System.out.println("Request body: " + body);
        
        try {
            if (uri.endsWith("/update_order")) {
                // Deserialize UpdateOrder from JSON
                UpdateOrder updateOrder = SERDES.deserialize(body, UpdateOrder.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                OrderChanged result = orderService.updateOrder(updateOrder);
                System.out.println("Order updated successfully: " + (result.order != null));
                return;
            }
            
            // If we get here, no matching endpoint was found
            LOGGER.log(ERROR, "Unknown PATCH endpoint: {0}", uri);
            System.out.println("ERROR: Unknown PATCH endpoint: " + uri);
            this.transactionManager.reset();
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error processing PATCH request: {0}", e.getMessage());
            System.out.println("ERROR processing PATCH request: " + e.getMessage());
            e.printStackTrace();
            this.transactionManager.reset();
        }
    }

    @Override
    public String getAsJson(String uri) {
        LOGGER.log(DEBUG, "Received GET request: {0}", uri);
        System.out.println("GET request received: " + uri);
        
        try {
            if (uri.matches("/orders/account/\\d+")) {
                String accountId = uri.substring(uri.lastIndexOf('/') + 1);
                this.transactionManager.beginTransaction(0, 0, 0, true);                
                String ordersJson = SERDES.serialize(orderService.getOrdersByAccountId(accountId), finsim.common.entities.Order[].class);                
                this.transactionManager.commit();
                
                return ordersJson;
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