package finsim.market.repositories;

import finsim.market.entities.Fill;
import dk.ku.di.dms.vms.modb.api.annotations.Query;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;

import java.util.List;

public interface IFillRepository extends IRepository<Fill.FillId, Fill> {
    @Query("select * from fills where fill_transaction_id = :transactionid")
    List<Fill> getFillsByTransactionId(String transactionid);
}