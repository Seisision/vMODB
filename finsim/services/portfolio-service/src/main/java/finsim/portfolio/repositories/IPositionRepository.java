package finsim.portfolio.repositories;

import finsim.portfolio.entities.Position;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;
import dk.ku.di.dms.vms.modb.api.annotations.Query;
import java.util.List;

public interface IPositionRepository extends IRepository<Integer, Position> {
    @Query("SELECT * FROM positions WHERE account_id = :accountId AND instrument_id = :instrumentId")
    List<Position> lookupByAccountAndInstrument(int accountId, int instrumentId);

    @Query("SELECT * FROM positions WHERE account_id = :accountId")
    List<Position> lookupByAccount(int accountId);
}