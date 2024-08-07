package dk.ku.di.dms.vms.marketplace;

import dk.ku.di.dms.vms.coordinator.Coordinator;
import dk.ku.di.dms.vms.coordinator.options.CoordinatorOptions;
import dk.ku.di.dms.vms.coordinator.transaction.TransactionBootstrap;
import dk.ku.di.dms.vms.coordinator.transaction.TransactionDAG;
import dk.ku.di.dms.vms.coordinator.transaction.TransactionInput;
import dk.ku.di.dms.vms.marketplace.common.entities.CartItem;
import dk.ku.di.dms.vms.marketplace.common.events.ReserveStock;
import dk.ku.di.dms.vms.marketplace.common.inputs.CustomerCheckout;
import dk.ku.di.dms.vms.marketplace.common.inputs.UpdateProduct;
import dk.ku.di.dms.vms.modb.common.schema.network.node.IdentifiableNode;
import dk.ku.di.dms.vms.modb.common.schema.network.node.ServerNode;
import dk.ku.di.dms.vms.modb.common.serdes.IVmsSerdesProxy;
import dk.ku.di.dms.vms.modb.common.serdes.VmsSerdesProxyBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.Thread.sleep;

/**
 * Different transactions in stock VMS interleave
 * Some come from product (update product), others come from the coordinator (reserve stock)
 */
public final class ProductStockOrderWorkflowTest extends AbstractWorkflowTest {

    private static final CustomerCheckout customerCheckout = new CustomerCheckout(
         1, "test", "test", "test", "test","test",
            "test", "test","test","test","test",
            "test", "test", "test", 1,"1"
            );

    @Test
    @SuppressWarnings("BusyWait")
    public void testComplexTopologyWithThreeVMSs() throws Exception {
        dk.ku.di.dms.vms.marketplace.product.Main.main(null);
        dk.ku.di.dms.vms.marketplace.stock.Main.main(null);
        dk.ku.di.dms.vms.marketplace.order.Main.main(null);

        ingestDataIntoProductVms();
        insertItemsInStockVms();

        Coordinator coordinator = loadCoordinator();
        Thread coordinatorThread = new Thread(coordinator);
        coordinatorThread.start();

        do{
            sleep(5000);
        } while (coordinator.getConnectedVMSs().size() < 3);

        Thread thread = new Thread(new ProductUpdateAndCheckoutProducer(coordinator));
        thread.start();

        sleep(BATCH_WINDOW_INTERVAL * 3);

        Assert.assertEquals(2, coordinator.getBatchOffsetPendingCommit());
        Assert.assertEquals(20, coordinator.getLastTidOfLastCompletedBatch());
    }

    private static class ProductUpdateAndCheckoutProducer implements Runnable {

        private final Coordinator coordinator;

        public ProductUpdateAndCheckoutProducer(Coordinator coordinator) {
            this.coordinator = coordinator;
        }

        @Override
        public void run() {
            IVmsSerdesProxy serdes = VmsSerdesProxyBuilder.build( );
            int val = 1;
            while(val <= 10) {
                // update product
                UpdateProduct updateProduct = new UpdateProduct(
                        1,1,"test","test","test","test",10.0F,10.0F,"test", String.valueOf(val)
                );
                String payload = serdes.serialize(updateProduct, UpdateProduct.class);
                TransactionInput.Event eventPayload = new TransactionInput.Event("update_product", payload);
                TransactionInput txInput = new TransactionInput("update_product", eventPayload);
                LOGGER.log(INFO, "[InputProducer] New product version: "+val);
                this.coordinator.queueTransactionInput(txInput);

                // reserve stock
                ReserveStock reserveStockEvent = new ReserveStock(
                        new Date(), customerCheckout,
                        List.of(new CartItem(1,1,"test",1.0f, 1.0f, 1, 1.0f, String.valueOf(val))),
                        String.valueOf(val)
                );
                String payload_ = serdes.serialize(reserveStockEvent, ReserveStock.class);
                TransactionInput.Event eventPayload_ = new TransactionInput.Event("reserve_stock", payload_);
                TransactionInput txInput_ = new TransactionInput("customer_checkout", eventPayload_);
                LOGGER.log(INFO, "[CheckoutProducer] New reserve stock event with version: "+val);
                this.coordinator.queueTransactionInput(txInput_);

                val++;
            }
            LOGGER.log(INFO, "InputProducer going to bed definitely... ");
        }
    }

    private Coordinator loadCoordinator() throws IOException {
        ServerNode serverIdentifier = new ServerNode( "localhost", 8080 );

        Map<Integer, ServerNode> serverMap = new HashMap<>(2);
        serverMap.put(serverIdentifier.hashCode(), serverIdentifier);

        TransactionDAG updatePriceDag =  TransactionBootstrap.name("update_price")
                .input( "a", "product", "update_price" )
                .terminal("b", "product", "a")
                .build();

        TransactionDAG updateProductDag =  TransactionBootstrap.name("update_product")
                .input( "a", "product", "update_product" )
                .terminal("b", "stock", "a")
                .build();

        TransactionDAG checkoutDag =  TransactionBootstrap.name("customer_checkout")
                .input( "a", "stock", "reserve_stock" )
                .terminal("b", "order", "a")
                .build();

        Map<String, TransactionDAG> transactionMap = new HashMap<>();
        transactionMap.put(updatePriceDag.name, updatePriceDag);
        transactionMap.put(updateProductDag.name, updateProductDag);
        transactionMap.put(checkoutDag.name, checkoutDag);

        IVmsSerdesProxy serdes = VmsSerdesProxyBuilder.build( );

        Map<Integer, IdentifiableNode> VMSs = getIdentifiableNodeMap();

        return Coordinator.build(
                serverMap,
                VMSs,
                transactionMap,
                serverIdentifier,
                new CoordinatorOptions().withBatchWindow(3000),
                1,
                1, 
                serdes
        );
    }

    private static Map<Integer, IdentifiableNode> getIdentifiableNodeMap() {
        Map<Integer, IdentifiableNode> VMSs = new HashMap<>(3);
        IdentifiableNode productAddress = new IdentifiableNode("product", "localhost", 8081);
        VMSs.put(productAddress.hashCode(), productAddress);
        IdentifiableNode stockAddress = new IdentifiableNode("stock", "localhost", 8082);
        VMSs.put(stockAddress.hashCode(), stockAddress);
        IdentifiableNode orderAddress = new IdentifiableNode("order", "localhost", 8083);
        VMSs.put(orderAddress.hashCode(), orderAddress);
        return VMSs;
    }

}
