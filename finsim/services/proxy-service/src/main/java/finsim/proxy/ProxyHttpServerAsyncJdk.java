package finsim.proxy;

import dk.ku.di.dms.vms.coordinator.Coordinator;
import dk.ku.di.dms.vms.coordinator.infra.AbstractHttpHandler;
import dk.ku.di.dms.vms.coordinator.transaction.TransactionInput;
import dk.ku.di.dms.vms.web_common.IHttpHandler;

import static finsim.common.Constants.*;

/**
 * HTTP server implementation for the Finsim proxy service.
 * Handles incoming HTTP requests and forwards them to the coordinator.
 */
public final class ProxyHttpServerAsyncJdk extends AbstractHttpHandler implements IHttpHandler {

    public ProxyHttpServerAsyncJdk(Coordinator coordinator) {
        super(coordinator);
    }

    /**
     * Handles POST requests - for creating resources
     * In finsim context: create orders
     */
    public void post(String uri, String body) {
        System.out.println("Received POST request: " + uri);
        if (uri.contains("/create_order")) {
            System.out.println("Processing create order request");
            this.submitCreateOrder(body);
            return;
        }
        if(uri.contains("/create_instrument")) {
            System.out.println("Processing create instrument request");
            this.submitCreateInstrument(body);
            return;
        }
        if(uri.contains("/cancel_order")) {
            System.out.println("Processing cancel order request");
            this.submitCancelOrder(body);
            return;
        }
        if (uri.contains("/create_transfer")) {
            System.out.println("Processing create transfer request");
            this.submitCreateTransfer(body);
            return;
        }
        if (uri.contains("/order_position_created")) {
            System.out.println("Processing order position created event");
            // This is an event, not a transaction
            TransactionInput.Event eventPayload = new TransactionInput.Event(ORDER_POSITION_CREATED, body);
            TransactionInput txInput = new TransactionInput(ORDER_POSITION_CREATED, eventPayload);
            this.coordinator.queueTransactionInput(txInput);
            return;
        }

        System.out.println("Unsupported POST request, no action taken");
    }

    /**
     * Handles PATCH requests - for partial updates
     * In finsim context: update orders
     */
    public void patch(String uri, String body) {
        System.out.println("Received PATCH request: " + uri);
        if (uri.contains("/order")) {
            System.out.println("Processing update order request");
            this.submitUpdateOrder(body);
        }

        System.out.println("Unsupported PATCH request, no action taken");
    }

    /**
     * Handles DELETE requests - for deleting resources
     * In finsim context: cancel orders
     */
    public void delete(String uri, String body) {
        System.out.println("Received DELETE request: " + uri);
        
        System.out.println("Unsupported DELETE request, no action taken");
    }

    // Transaction submission methods
    
    private void submitCreateOrder(String payload) {
        System.out.println("Submitting create order transaction to coordinator");
        TransactionInput.Event eventPayload = new TransactionInput.Event(CREATE_ORDER, payload);
        TransactionInput txInput = new TransactionInput(CREATE_ORDER, eventPayload);
        this.coordinator.queueTransactionInput(txInput);
    }

    private void submitUpdateOrder(String payload) {
        System.out.println("Submitting update order transaction to coordinator");
        TransactionInput.Event eventPayload = new TransactionInput.Event(UPDATE_ORDER, payload);
        TransactionInput txInput = new TransactionInput(UPDATE_ORDER, eventPayload);
        this.coordinator.queueTransactionInput(txInput);
    }

    private void submitCancelOrder(String payload) {
        System.out.println("Submitting cancel order transaction to coordinator");
        TransactionInput.Event eventPayload = new TransactionInput.Event(CANCEL_ORDER, payload);
        TransactionInput txInput = new TransactionInput(CANCEL_ORDER, eventPayload);
        this.coordinator.queueTransactionInput(txInput);
    }

    private void submitCreateInstrument(String payload) {
    System.out.println("Submitting create instrument transaction to coordinator");
    TransactionInput.Event eventPayload = new TransactionInput.Event(CREATE_INSTRUMENT, payload);
    TransactionInput txInput = new TransactionInput(CREATE_INSTRUMENT, eventPayload);
    this.coordinator.queueTransactionInput(txInput);
    }

    private void submitUpdateInstrument(String payload) {
        System.out.println("Submitting update instrument transaction to coordinator");
        TransactionInput.Event eventPayload = new TransactionInput.Event(UPDATE_INSTRUMENT, payload);
        TransactionInput txInput = new TransactionInput(UPDATE_INSTRUMENT, eventPayload);
        this.coordinator.queueTransactionInput(txInput);
    }

    private void submitDelistInstrument(String payload) {
        System.out.println("Submitting delist instrument transaction to coordinator");
        TransactionInput.Event eventPayload = new TransactionInput.Event(DELIST_INSTRUMENT, payload);
        TransactionInput txInput = new TransactionInput(DELIST_INSTRUMENT, eventPayload);
        this.coordinator.queueTransactionInput(txInput);
    }

    private void submitCreateTransfer(String payload) {
        System.out.println("Submitting create transfer transaction to coordinator");
        TransactionInput.Event eventPayload = new TransactionInput.Event(CREATE_TRANSFER, payload);
        TransactionInput txInput = new TransactionInput(CREATE_TRANSFER, eventPayload);
        this.coordinator.queueTransactionInput(txInput);
    }
}