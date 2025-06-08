package finsim.order.repositories;

import finsim.order.entities.Order;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;
import dk.ku.di.dms.vms.modb.api.annotations.Query;
import java.util.List;

public interface IOrderRepository extends IRepository<Integer, Order> {
    @Query("SELECT * FROM orders WHERE account_id = :accountId")
    List<Order> lookupByAccount(String accountId);
}