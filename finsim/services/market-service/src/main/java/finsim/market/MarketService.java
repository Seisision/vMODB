package finsim.market;

import finsim.common.entities.Order;
import finsim.common.enums.BuySellType;
import finsim.common.enums.OrderStatus;
import finsim.common.enums.OrderDuration;
import finsim.common.events.InstrumentCreated;
import finsim.common.events.OrderCancelled;
import finsim.common.events.OrderChanged;
import finsim.common.events.OrderCreated;
import finsim.common.events.OrderHandled;
import finsim.common.inputs.FillOrder;
import finsim.common.inputs.MarketFill;
import finsim.common.inputs.OrderRouted;
import finsim.market.entities.Fill;
import finsim.market.entities.MarketDataEntity;
import finsim.market.entities.MarketState;
import finsim.market.infra.MarketUtils;
import finsim.market.repositories.IFillRepository;
import finsim.market.repositories.IMarketDataRepository;
import finsim.market.repositories.IMarketStateRepository;
import dk.ku.di.dms.vms.modb.api.annotations.*;
import dk.ku.di.dms.vms.modb.common.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static finsim.common.Constants.*;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.RW;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.R;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Microservice("market")
public final class MarketService {

    private static final System.Logger LOGGER = System.getLogger(MarketService.class.getName());
    private final Properties properties;

    private final IFillRepository fillRepository;
    private final IMarketStateRepository marketStateRepository;
    private final IMarketDataRepository marketDataRepository;

    public MarketService(
            IFillRepository fillRepository,
            IMarketStateRepository marketStateRepository,
            IMarketDataRepository marketDataRepository) {
        
        this.properties = ConfigUtils.loadProperties();

        this.fillRepository = fillRepository;
        this.marketStateRepository = marketStateRepository;
        this.marketDataRepository = marketDataRepository;
        
        LOGGER.log(INFO, "MarketService initialized with repositories: FillRepository only for testing");
    }

    @Inbound(values = {ORDER_CREATED})
    @Outbound(ORDER_HANDLED)
    @Transactional(type = RW)
    public OrderHandled handleOrderCreated(OrderCreated orderCreated) {
        LOGGER.log(DEBUG, "APP: Market-service received an order created event for order ID " + 
                  orderCreated.order.id + " with TID: " + orderCreated.instanceId);
        
        System.out.println("========================================================");
        System.out.println("ORDER CREATED EVENT RECEIVED IN MARKET SERVICE: ID=" + orderCreated.order.id);
        System.out.println("Routing order to market for execution");
        System.out.println("========================================================");
        
        // If the order is null, we cannot process it (validation failure)
        if (orderCreated.order == null) {
            LOGGER.log(ERROR, "OrderCreated event has no order details. Cannot process.");
            return new OrderHandled(new Date(), orderCreated.request, 0, true, "Order validation failed", orderCreated.instanceId);
        }

        // Check if auto-fill is enabled (if false we depend on fill order input from proxy)
        boolean doAutoFill = doAutoFill();
        System.out.println("AUTO FILL IS " + (doAutoFill ? "ENABLED" : "DISABLED"));

        // TODO: If not doAutoFill save routedOrder in repository

        OrderHandled orderHandled = new OrderHandled(
            new Date(),
            orderCreated.request,
            doAutoFill ? orderCreated.order.quantity : 0, // if auto-fill is enabled, we fill the entire order
            false, // is_aborted is false as we are processing the order
            "", // no aborted reason as the order is not aborted
            orderCreated.instanceId
        );

        return orderHandled;
    }

    @Inbound(values = {ORDER_CANCELLED})
    @Transactional(type = RW)
    public void handleOrderCancelled(OrderCancelled orderCancelled) {
        LOGGER.log(DEBUG, "APP: Market-service received an order cancelled event for order ID " + 
                  orderCancelled.order.id + " with TID: " + orderCancelled.instanceId);
        
        System.out.println("========================================================");
        System.out.println("ORDER CANCELLED EVENT RECEIVED IN MARKET SERVICE: ID=" + orderCancelled.order.id);
        System.out.println("Processing order cancellation in market");
        System.out.println("========================================================");

        // TODO: If routedOrder exists in repository, delete it
        
    }

    // @Inbound(values = {ORDER_CHANGED})
    // @Transactional(type = RW)
    // public OrderFilled handleOrderChanged(OrderChanged orderChanged) {
    //     LOGGER.log(DEBUG, "APP: Market-service received an order changed event for order ID " + 
    //               orderChanged.order.id + " with TID: " + orderChanged.instanceId);
        
    //     System.out.println("========================================================");
    //     System.out.println("ORDER CHANGED EVENT RECEIVED IN MARKET SERVICE: ID=" + orderChanged.order.id);
    //     System.out.println("Processing order change in market");
    //     System.out.println("========================================================");
        
    //     // TODO: if routedOrder has less quantity than orderChanged.order.quantity, only fill the difference
    //     // TODO: delete routedOrder if routedOrder.quantity - orderChanged.order.quantity <= 0
    //     // otherwise update routedOrder with routedOrder.quantity -= orderChanged.order.quantity
    //     // TODO: then remove everything below

    //     // Check if the order was filled
    //     if (orderChanged.order.status == OrderStatus.COMPLETED) {
    //         LOGGER.log(INFO, "Order ID " + orderChanged.order.id + " is already filled. No action taken.");
    //         return null; // No action needed if the order is already filled
    //     }
        
    //     if (orderChanged.order.status == OrderStatus.CANCELLED) {
    //         LOGGER.log(INFO, "Order ID " + orderChanged.order.id + " is cancelled. No action taken.");
    //         return null; // No action needed if the order is cancelled
    //     }

    //     if (orderChanged.order.status != OrderStatus.PENDING) {
    //         LOGGER.log(INFO, "Order ID " + orderChanged.order.id + " is not pending. No action taken.");
    //         return null; // No action needed if the order is not pending
    //     }

    //     // TODO: Implement logic to check if the order needs to be filled
    //     // For now, we will assume the order needs to be filled and create an OrderFilled event

    //     if (orderChanged.order.quantity <= 0) {
    //         LOGGER.log(INFO, "Order ID " + orderChanged.order.id + " has non-positive quantity. No action taken.");
    //         return null; // No action needed if the order quantity is non-positive
    //     }
    //     // This is similar to the routeOrder method, as it also creates an OrderFilled event
    //     // Generate a unique fill ID
    //     try
    //     {
    //         String fillId = MarketUtils.generateFillId();
            
    //         // Create a fill record with the renamed field
    //         Fill fill = new Fill(
    //             fillId,
    //             orderChanged.order.id, // This will be stored in transaction_id field now
    //             orderChanged.order.instrumentId,
    //             orderChanged.order.quantity,
    //             orderChanged.order.price,
    //             new Date()
    //         );
            
    //         // ONLY USING FILL REPOSITORY FOR TESTING
    //         LOGGER.log(INFO, "REPOSITORY-TEST: About to call fillRepository.insert()");
    //         fillRepository.insert(fill);
    //         LOGGER.log(INFO, "REPOSITORY-TEST: Successfully inserted fill record");
            
    //         // Skip market data update for testing
            
    //         // Rest of the method remains unchanged
    //         Date executionTime = new Date();
    //         FillOrder fillOrderInput = new FillOrder(
    //             orderChanged.order.id,
    //             (int)orderChanged.order.quantity,
    //             orderChanged.order.price,
    //             executionTime,
    //             orderChanged.instanceId
    //         );
            
    //         Order order = new Order();
    //         order.id = orderChanged.order.id;
    //         order.instrumentId = orderChanged.order.instrumentId;
    //         order.price = orderChanged.order.price;
    //         order.quantity = (int)orderChanged.order.quantity;
    //         order.buySell = orderChanged.order.buySell;
    //         order.dateTime = new Date();
    //         order.status = OrderStatus.COMPLETED;
    //         order.duration = OrderDuration.DAY_ORDER;
    //         order.expiryDateTime = null;
            
    //         return new OrderFilled(new Date(), fillOrderInput, order, orderChanged.instanceId);
    //     } catch (Exception e) {
    //         LOGGER.log(ERROR, "REPOSITORY-ERROR: Error in routeOrder: " + e.getMessage());
    //         e.printStackTrace();
    //         throw e;
    //     }
    // }

    // @Inbound(values = {MARKET_FILL})
    // @Outbound(ORDER_FILLED)
    // @Transactional(type = RW)
    // public OrderFilled handleMarketFill(MarketFill marketFill) {
    //     LOGGER.log(DEBUG, "APP: Market-service received a market fill request for order ID " + 
    //               marketFill.order_id + " with TID: " + marketFill.instanceId);
        
    //     try {
    //         // Generate a unique fill ID
    //         String fillId = MarketUtils.generateFillId();
            
    //         // Create a fill record with the renamed field
    //         Fill fill = new Fill(
    //             fillId,
    //             marketFill.order_id, // This will be stored in transaction_id field now
    //             null, // Instrument ID is not provided in MarketFill
    //             marketFill.quantity,
    //             marketFill.price,
    //             new Date()
    //         );
            
    //         // TODO: Remove routedOrder from repository if it exists
            
    //         // TODO: change type of second property in this to MArketFill from FillOrder
    //         return new OrderFilled(new Date(), null, null, marketFill.instanceId);
    //     } catch (Exception e) {
    //         System.out.println("REPOSITORY-ERROR: Error in handleMarketFill: " + e.getMessage());
    //         e.printStackTrace();
    //         throw e;
    //     }
    // }

    // this should always be true, but for future improvements it exists
    private boolean doAutoFill() {
        return "true".equals(properties.getProperty("market.auto_fill"));
    }
}