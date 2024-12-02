package dk.ku.di.dms.vms.tpcc.warehouse.infra;

import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.sdk.embed.client.DefaultHttpHandler;
import dk.ku.di.dms.vms.tpcc.warehouse.entities.Customer;
import dk.ku.di.dms.vms.tpcc.warehouse.entities.District;
import dk.ku.di.dms.vms.tpcc.warehouse.entities.Warehouse;
import dk.ku.di.dms.vms.tpcc.warehouse.repositories.ICustomerRepository;
import dk.ku.di.dms.vms.tpcc.warehouse.repositories.IDistrictRepository;
import dk.ku.di.dms.vms.tpcc.warehouse.repositories.IWarehouseRepository;

public final class WarehouseHttpHandler extends DefaultHttpHandler {

    private final IWarehouseRepository warehouseRepository;

    private final IDistrictRepository districtRepository;

    private final ICustomerRepository customerRepository;

    public WarehouseHttpHandler(ITransactionManager transactionManager,
                                IWarehouseRepository warehouseRepository,
                                IDistrictRepository districtRepository,
                                ICustomerRepository customerRepository) {
        super(transactionManager);
        this.warehouseRepository = warehouseRepository;
        this.districtRepository = districtRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public String getAsJson(String uri) throws RuntimeException {
        String[] uriSplit = uri.split("/");
        String table;
        switch (uriSplit.length){
            case 3 -> table = uriSplit[uriSplit.length - 2];
            case 4 -> table = uriSplit[uriSplit.length - 3];
            case 5 -> table = uriSplit[uriSplit.length - 4];
            default -> table = "";
        }
        switch (table){
            case "warehouse" -> {
                int wareId = Integer.parseInt(uriSplit[uriSplit.length - 1]);
                this.transactionManager.beginTransaction(0, 0, 0, true);
                Warehouse warehouse = this.warehouseRepository.lookupByKey(wareId);
                return warehouse.toString();
            }
            case "district" -> {
                int distId = Integer.parseInt(uriSplit[uriSplit.length - 2]);
                int wareId = Integer.parseInt(uriSplit[uriSplit.length - 1]);
                this.transactionManager.beginTransaction(Long.MAX_VALUE, 0, Long.MAX_VALUE, true);
                District district = this.districtRepository.lookupByKey(new District.DistrictId( distId, wareId ));
                return district.toString();
            }
            case "customer" -> {
                int cId = Integer.parseInt(uriSplit[uriSplit.length - 3]);
                int distId = Integer.parseInt(uriSplit[uriSplit.length - 2]);
                int wareId = Integer.parseInt(uriSplit[uriSplit.length - 1]);
                this.transactionManager.beginTransaction(0, 0, 0, true);
                Customer customer = this.customerRepository.lookupByKey(new Customer.CustomerId( cId, distId, wareId ));
                return customer.toString();
            }
            case null, default -> {
                LOGGER.log(System.Logger.Level.WARNING, "URI not recognized: "+uri);
                return "{ \"message\":\" URI not recognized = "+uri+"\" }";
            }
        }
    }

    @Override
    public void post(String uri, String payload) {
        String[] uriSplit = uri.split("/");
        String table = uriSplit[uriSplit.length - 1];
        switch (table){
            case "warehouse" -> {
                Warehouse warehouse = SERDES.deserialize(payload, Warehouse.class);
                this.transactionManager.beginTransaction(0, 0, 0, false);
                this.warehouseRepository.upsert(warehouse);
            }
            case "district" -> {
                District district = SERDES.deserialize(payload, District.class);
                this.transactionManager.beginTransaction(0, 0, 0, false);
                this.districtRepository.upsert(district);
            }
            case "customer" -> {
                Customer customer = SERDES.deserialize(payload, Customer.class);
                this.transactionManager.beginTransaction(0, 0, 0, false);
                this.customerRepository.upsert(customer);
            }
        }
    }

}
