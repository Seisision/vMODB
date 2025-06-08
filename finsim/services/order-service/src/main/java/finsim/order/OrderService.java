package finsim.order;

import finsim.common.enums.OrderStatus;
import finsim.common.events.OrderCreated;
import finsim.common.events.OrderChanged;
import finsim.common.events.OrderCancelled;
import finsim.common.events.OrderHandled;
import finsim.common.events.InstrumentCreated;
import finsim.common.inputs.CreateOrder;
import finsim.common.inputs.UpdateOrder;
import finsim.common.inputs.CancelOrder;
import finsim.common.inputs.FillOrder;
import finsim.common.inputs.OrderPositionCreated;
import finsim.order.entities.Order;
import finsim.order.entities.InstrumentReplica;
import finsim.order.infra.OrderUtils;
import finsim.order.repositories.IInstrumentReplicaRepository;
import finsim.order.repositories.IOrderRepository;

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
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Microservice("order")
public final class OrderService {

    private static final System.Logger LOGGER = System.getLogger(OrderService.class.getName());

    private final IOrderRepository orderRepository;
    private final IInstrumentReplicaRepository instrumentReplicaRepository;

    public OrderService(IOrderRepository orderRepository, IInstrumentReplicaRepository instrumentReplicaRepository) {
        this.orderRepository = orderRepository;
        this.instrumentReplicaRepository = instrumentReplicaRepository;
    }
        
    private boolean validateOrderBasics(CreateOrder createOrder) {
        // Validate basic order parameters
        if (createOrder.price <= 0) {
            LOGGER.log(ERROR, "APP: Invalid order creation - price must be positive");
            return false;
        }
        
        if (createOrder.quantity <= 0) {
            LOGGER.log(ERROR, "APP: Invalid order creation - quantity must be positive");
            return false;
        }
        
        // Validate that the instrument ID is not empty
        if (createOrder.instrumentId == null || createOrder.instrumentId.trim().isEmpty()) {
            LOGGER.log(ERROR, "APP: Invalid order creation - missing instrument ID");
            return false;
        }
        
        return true;
    }
    
    private boolean validateOrderUpdate(UpdateOrder updateOrder) {
        // Validate basic order parameters
        if (updateOrder.price <= 0) {
            LOGGER.log(ERROR, "APP: Invalid order update - price must be positive");
            return false;
        }
        
        if (updateOrder.quantity <= 0) {
            LOGGER.log(ERROR, "APP: Invalid order update - quantity must be positive");
            return false;
        }
        
        return true;
    }

    @Inbound(values = {CREATE_ORDER})
    @Outbound(ORDER_CREATED)
    @Transactional(type=RW)
    public OrderCreated createOrder(CreateOrder createOrder) {
        LOGGER.log(INFO, "APP: Order-service received a create request for order ID "+ createOrder.id +" with TID: "+createOrder.instanceId);
        System.out.println("APP: Order-service received a create request for order ID "+ createOrder.id +" with TID: "+createOrder.instanceId);

        boolean isInstrumentValid = validateInstrument(createOrder.instrumentId);
        if(!isInstrumentValid) {
            LOGGER.log(ERROR, "APP: Invalid order creation - instrument not valid: " + createOrder.instrumentId);
            System.out.println("APP: Invalid order creation - instrument not valid: " + createOrder.instrumentId);
            return new OrderCreated(
                new Date(), 
                createOrder, 
                null, // validation failure
                createOrder.instanceId
            );
        }
        try {
            LOGGER.log(INFO, "========================================================");
            LOGGER.log(INFO, "CREATE ORDER REQUEST RECEIVED: ID=" + createOrder.id);
            LOGGER.log(INFO, "AccountID: " + createOrder.accountId);
            LOGGER.log(INFO, "InstrumentID: " + createOrder.instrumentId);
            LOGGER.log(INFO, "Quantity: " + createOrder.quantity);
            LOGGER.log(INFO, "Price: " + createOrder.price);
            LOGGER.log(INFO, "BuySell: " + createOrder.buySell);
            LOGGER.log(INFO, "Instrument validation: " + (isInstrumentValid ? "VALID" : "INVALID"));
            LOGGER.log(INFO, "InstanceID: " + createOrder.instanceId);
            LOGGER.log(INFO, "========================================================");   

            System.out.println("========================================================");
            System.out.println("CREATE ORDER REQUEST RECEIVED: ID=" + createOrder.id);
            System.out.println("AccountID: " + createOrder.accountId);
            System.out.println("InstrumentID: " + createOrder.instrumentId);
            System.out.println("Quantity: " + createOrder.quantity);
            System.out.println("Price: " + createOrder.price);
            System.out.println("BuySell: " + createOrder.buySell);
            System.out.println("Instrument validation: " + (isInstrumentValid ? "VALID" : "INVALID"));
            System.out.println("InstanceID: " + createOrder.instanceId);
            System.out.println("========================================================");
            System.out.flush();
            System.out.println("APP: Order-service received a create request for order ID "+ createOrder.id +" with TID: "+createOrder.instanceId);
                         
            // Validate basic order parameters
            if (!validateOrderBasics(createOrder)) {
                LOGGER.log(ERROR, "APP: Order validation failed for ID " + createOrder.id);
                System.out.println("APP: Order validation failed for ID " + createOrder.id);
                return new OrderCreated(
                    new Date(), 
                    createOrder, 
                    null,  // validation failure
                    createOrder.instanceId
                );
            }
            

            LOGGER.log(INFO, "APP: Creating order with ID " + createOrder.id + " TID: " + createOrder.instanceId);
            System.out.println("APP: Creating order with ID " + createOrder.id + " TID: " + createOrder.instanceId);

            Order order = new Order(
                Integer.parseInt(createOrder.id),
                createOrder.accountId,
                createOrder.instrumentId,
                createOrder.quantity,
                createOrder.price,
                createOrder.buySell,
                new Date(),  // current time
                OrderStatus.PENDING,  // default status for new orders
                createOrder.duration,
                createOrder.expiryDateTime
            );
            
            this.orderRepository.insert(order);
            Order savedOrder = this.orderRepository.lookupByKey(order.getId());
            
            if (savedOrder != null) {
                LOGGER.log(INFO, "APP: Order created successfully with TID: " + createOrder.instanceId);
                System.out.println("APP: Order created successfully with TID: "+createOrder.instanceId);
                return new OrderCreated(
                    new Date(), 
                    createOrder, 
                    OrderUtils.convertOrder(savedOrder), 
                    createOrder.instanceId
                );
            } else {
                LOGGER.log(ERROR, "APP: Failed to create order with ID "+createOrder.id+" TID: "+createOrder.instanceId);
                System.out.println("APP: Failed to create order with ID "+createOrder.id+" TID: "+createOrder.instanceId);
                return new OrderCreated(
                    new Date(), 
                    createOrder, 
                    null,
                    createOrder.instanceId
                );
            }
        } catch (Exception e) {
            LOGGER.log(ERROR, "EXCEPTION IN CREATE ORDER: " + e.getMessage());
            System.out.println("EXCEPTION IN CREATE ORDER: " + e.getMessage());
            e.printStackTrace();
            System.out.flush();
            throw e; // Re-throw so MODB can handle it
        }
    }

    @Inbound(values = {UPDATE_ORDER})
    @Outbound(ORDER_CHANGED)
    @Transactional(type=RW)
    public OrderChanged updateOrder(UpdateOrder updateOrder) {
        LOGGER.log(DEBUG, "APP: Order-service received an update request for ID "+ updateOrder.id +" with TID: "+updateOrder.instanceId);
        System.out.println("APP: Order-service received an update request for ID "+ updateOrder.id +" with TID: "+updateOrder.instanceId);
        
        Integer orderId = Integer.parseInt(updateOrder.id);
        Order existingOrder = this.orderRepository.lookupByKey(orderId);
        
        if (existingOrder == null) {
            LOGGER.log(ERROR, "APP: No order found for ID "+updateOrder.id+" TID: "+updateOrder.instanceId);
            System.out.println("APP: No order found for ID "+updateOrder.id+" TID: "+updateOrder.instanceId);
            return new OrderChanged(new Date(), updateOrder, null, updateOrder.instanceId);
        }
        
        // Only allow updates to pending orders
        if (existingOrder.status != OrderStatus.PENDING) {
            LOGGER.log(ERROR, "APP: Cannot update non-pending order with ID "+updateOrder.id+" TID: "+updateOrder.instanceId);
            System.out.println("APP: Cannot update non-pending order with ID "+updateOrder.id+" TID: "+updateOrder.instanceId);
            return new OrderChanged(new Date(), updateOrder, OrderUtils.convertOrder(existingOrder), updateOrder.instanceId);
        }
        
        // Validate order update
        if (!validateOrderUpdate(updateOrder)) {
            LOGGER.log(ERROR, "APP: Order update validation failed for ID " + updateOrder.id);
            System.out.println("APP: Order update validation failed for ID " + updateOrder.id);
            return new OrderChanged(
                new Date(),
                updateOrder,
                OrderUtils.convertOrder(existingOrder),
                updateOrder.instanceId
            );
        }
        
        existingOrder.quantity = updateOrder.quantity;
        existingOrder.price = updateOrder.price;
        existingOrder.duration = updateOrder.duration;
        existingOrder.expiry_date_time = updateOrder.expiryDateTime;
        existingOrder.updated_at = new Date();

        System.out.println("APP: Order being updated in repository with TID: "+updateOrder.instanceId);

        this.orderRepository.update(existingOrder);


        Order updatedOrder = this.orderRepository.lookupByKey(existingOrder.getId());

        if (updatedOrder == null) {
            LOGGER.log(ERROR, "APP: Failed to retrieve updated order with ID "+updateOrder.id+" TID: "+updateOrder.instanceId);
            System.out.println("APP: Failed to retrieve updated order with ID "+updateOrder.id+" TID: "+updateOrder.instanceId);
            return new OrderChanged(new Date(), updateOrder, null, updateOrder.instanceId);
        }
        
        LOGGER.log(DEBUG, "APP: Order updated with TID: "+updateOrder.instanceId);
        System.out.println("APP: Order updated with TID: "+updateOrder.instanceId);
        
        return new OrderChanged(
            new Date(), 
            updateOrder, 
            OrderUtils.convertOrder(updatedOrder), 
            updateOrder.instanceId
        );
    }

    @Inbound(values = {CANCEL_ORDER})
    @Outbound(ORDER_CANCELLED)
    @Transactional(type=RW)
    public OrderCancelled cancelOrder(CancelOrder cancelOrder) {
        LOGGER.log(DEBUG, "APP: Order-service received a cancel request for ID "+ cancelOrder.id +" with TID: "+cancelOrder.instanceId);
        
        Integer orderId = Integer.parseInt(cancelOrder.id);
        Order existingOrder = this.orderRepository.lookupByKey(orderId);                
        
        if (existingOrder == null) {
            LOGGER.log(ERROR, "APP: No order found for ID "+cancelOrder.id+" TID: "+cancelOrder.instanceId);
            System.out.println("APP: No order found for ID "+cancelOrder.id+" TID: "+cancelOrder.instanceId);
            return new OrderCancelled(new Date(), cancelOrder, null, cancelOrder.instanceId);
        }
        
        // Only allow cancellation of pending orders
        if (existingOrder.status != OrderStatus.PENDING) {
            LOGGER.log(ERROR, "APP: Cannot cancel non-pending order with ID "+cancelOrder.id+" TID: "+cancelOrder.instanceId);
            System.out.println("APP: Cannot cancel non-pending order with ID "+cancelOrder.id+" TID: "+cancelOrder.instanceId);
            return new OrderCancelled(new Date(), cancelOrder, OrderUtils.convertOrder(existingOrder), cancelOrder.instanceId);
        }
        
        // Update order status to cancelled
        existingOrder.status = OrderStatus.CANCELLED;
        existingOrder.updated_at = new Date();
        this.orderRepository.update(existingOrder);
        Order updatedOrder = this.orderRepository.lookupByKey(existingOrder.getId());
        
        LOGGER.log(DEBUG, "APP: Order cancelled with TID: "+cancelOrder.instanceId);
        System.out.println("APP: Order cancelled with TID: "+cancelOrder.instanceId);
        
        return new OrderCancelled(
            new Date(), 
            cancelOrder, 
            OrderUtils.convertOrder(updatedOrder), 
            cancelOrder.instanceId
        );
    }

    @Inbound(values = {ORDER_POSITION_CREATED})
    @Transactional(type=RW)
    public void handleOrderPositionCreated(OrderPositionCreated orderPositionCreated) {
        LOGGER.log(DEBUG, "APP: Order-service received an order position created notification for ID "+ orderPositionCreated.orderId +" with TID: "+orderPositionCreated.instanceId);
        System.out.println("APP: Order-service received an order position created notification for ID "+ orderPositionCreated.orderId +" with TID: "+orderPositionCreated.instanceId);

        // Create a new order replica entity
        Order existingOrder = this.orderRepository.lookupByKey(Integer.parseInt(orderPositionCreated.orderId));
        
        if (existingOrder == null) {
            LOGGER.log(ERROR, "APP: No order found for ID "+orderPositionCreated.orderId+" TID: "+orderPositionCreated.instanceId);
            System.out.println("APP: No order found for ID "+orderPositionCreated.orderId+" TID: "+orderPositionCreated.instanceId);
            return; // Nothing to do if no order exists
        }

        // Update the existing order with the filled quantity and status
        if (existingOrder.quantity != orderPositionCreated.amountFilled) {
            LOGGER.log(WARNING, "APP: Order quantity mismatch for ID "+orderPositionCreated.orderId+" TID: "+orderPositionCreated.instanceId);
            System.out.println("APP: Order quantity mismatch for ID "+orderPositionCreated.orderId+" TID: "+orderPositionCreated.instanceId);
        }
        existingOrder.status = OrderStatus.COMPLETED; // Mark as completed
        existingOrder.execution_date_time = new Date(); // Set execution time to now
        existingOrder.updated_at = new Date();
        
        this.orderRepository.update(existingOrder);
        
        LOGGER.log(DEBUG, "APP: Order position created and updated with TID: "+orderPositionCreated.instanceId);
    }

    // Currently not used as it would create a circular DAG (order->market->portfolio->order)
    @Inbound(values = {ORDER_HANDLED})
    @Transactional(type=RW)
    public void handleOrderHandled(OrderHandled orderHandled) {
        LOGGER.log(DEBUG, "APP: Order-service received an order handled notification for ID "+ orderHandled.request.id +" with TID: "+orderHandled.instanceId);
        System.out.println("APP: Order handled notification received for ID "+ orderHandled.request.id +" with TID: "+orderHandled.instanceId);

        // Update the order status based on the handling result
        CreateOrder request = orderHandled.request;
        int amount_filled = orderHandled.amount_filled;

        if (orderHandled.isFilled()) {
            // If the order was filled, update status to COMPLETED
            Order existingOrder = this.orderRepository.lookupByKey(Integer.parseInt(orderHandled.request.id));
            if (existingOrder != null) {
                existingOrder.status = OrderStatus.COMPLETED;
                existingOrder.price = request.price; // Use the price from the request
                existingOrder.execution_date_time = new Date(); // Set execution time to now
                existingOrder.updated_at = new Date();
                this.orderRepository.update(existingOrder);
                LOGGER.log(DEBUG, "APP: Order filled and updated with TID: "+orderHandled.instanceId);
            } else {
                LOGGER.log(ERROR, "APP: No order found for ID "+orderHandled.request.id+" TID: "+orderHandled.instanceId);
            }
        } else {
            // If the order was not filled, update status to ROUTED
            Order existingOrder = this.orderRepository.lookupByKey(Integer.parseInt(orderHandled.request.id));
            if (existingOrder != null) {
                existingOrder.status = OrderStatus.ROUTED;
                existingOrder.updated_at = new Date();
                this.orderRepository.update(existingOrder);
                LOGGER.log(DEBUG, "APP: Order routed and updated with TID: "+orderHandled.instanceId);
            } else {
                LOGGER.log(ERROR, "APP: No order found for ID "+orderHandled.request.id+" TID: "+orderHandled.instanceId);
            }
        }
    }

    @Inbound(values = {INSTRUMENT_CREATED})
    @Transactional(type=RW)
    public void instrumentCreated(InstrumentCreated instrumentCreated) {
        LOGGER.log(DEBUG, "APP: Instrument-service received a create notification for ID "+ instrumentCreated.request.id +" with TID: "+instrumentCreated.instanceId);
        System.out.println("APP: Instrument-service received a create notification for ID "+ instrumentCreated.request.id +" with TID: "+instrumentCreated.instanceId);

        if (instrumentCreated.instrument == null) {
            LOGGER.log(ERROR, "APP: Instrument creation failed for ID "+instrumentCreated.request.id+" TID: "+instrumentCreated.instanceId);
            System.out.println("APP: Instrument creation failed for ID "+instrumentCreated.request.id+" TID: "+instrumentCreated.instanceId);
            return; // Nothing to do if instrument creation failed
        }

        // Create new instrument replica entity
        InstrumentReplica instrumentReplica = new InstrumentReplica(
            Integer.parseInt(instrumentCreated.request.id),
            instrumentCreated.instrument.name,
            instrumentCreated.instrument.isin,
            instrumentCreated.instrument.assetClass,
            instrumentCreated.instrument.lotSize,
            instrumentCreated.instrument.currency,
            instrumentCreated.instrument.tradeable
        );

        // Insert the instrument replica into the repository
        instrumentReplicaRepository.insert(instrumentReplica);
    }

    public List<finsim.common.entities.Order> getOrdersByAccountId(String accountId) {
        LOGGER.log(INFO, "APP: Order-service received a request to get orders by Account ID " + accountId);
        System.out.println("APP: Order-service received a request to get orders by Account ID " + accountId);
        
        try
        {
            // Bug in enum parsing this, so exception is thrown here
            List<Order> orders = this.orderRepository.lookupByAccount(accountId);
            
            if (orders != null && !orders.isEmpty()) {
                LOGGER.log(INFO, "APP: Found " + orders.size() + " orders for Account ID " + accountId);
                return OrderUtils.convertOrders(orders);
            } else {
                LOGGER.log(ERROR, "APP: No orders found for Account ID " + accountId);
                return List.of(); // Return an empty list if no orders found
            }
        } catch (Exception e) {
            LOGGER.log(ERROR, "APP: Error retrieving orders for Account ID " + accountId + ": " + e.getMessage());
            System.out.println("APP: Error retrieving orders for Account ID " + accountId + ": " + e.getMessage());
            return List.of(); // Return an empty list on error
        }
    }

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

            // Check if instrument exists in the database
            InstrumentReplica instrument = instrumentReplicaRepository.lookupByKey(Integer.parseInt(instrumentId));
            if (instrument == null) {
                System.out.println("Instrument not found in the database: " + instrumentId);
                return false;
            }
            // Check if the instrument is tradeable
            if (instrument.tradeable != 1) {
                System.out.println("Instrument is not tradeable: " + instrumentId);
                return false;
            }
            System.out.println("Instrument is valid and tradeable: " + instrumentId);
            return true;
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error validating instrument: " + e.getMessage());
            System.out.println("Error validating instrument: " + e.getMessage());
            e.printStackTrace();
            return false; // Fail closed on errors
        }            
    }
}