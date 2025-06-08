package finsim.instrument.repositories;

import finsim.instrument.entities.Instrument;
import dk.ku.di.dms.vms.modb.api.annotations.Repository;
import dk.ku.di.dms.vms.modb.api.interfaces.IRepository;
import dk.ku.di.dms.vms.modb.api.annotations.Query;

import java.util.List;

@Repository
public interface IInstrumentRepository extends IRepository<Integer, Instrument> {

@Query("SELECT * FROM instruments WHERE instrument_id = :id AND tradeable = :tradeable")
    Instrument getTradableInstrumentById(int id, int tradeable);

@Query("SELECT * FROM instruments WHERE tradeable = :tradeable")
    List<Instrument> getTradableInstruments(int tradeable);

}