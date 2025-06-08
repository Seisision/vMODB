package finsim.instrument;

import finsim.common.events.InstrumentCreated;
import finsim.common.events.InstrumentChanged;
import finsim.common.events.InstrumentDelisted;
import finsim.common.inputs.CreateInstrument;
import finsim.common.inputs.UpdateInstrument;
import finsim.common.inputs.DelistInstrument;
import finsim.instrument.entities.Instrument;
import finsim.instrument.infra.InstrumentUtils;
import finsim.instrument.repositories.IInstrumentRepository;

import dk.ku.di.dms.vms.modb.api.annotations.*;

import java.util.Date;
import java.util.Optional;
import java.util.List;

import static finsim.common.Constants.*;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.RW;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.W;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

@Microservice("instrument")
public final class InstrumentService {

    private static final System.Logger LOGGER = System.getLogger(InstrumentService.class.getName());

    private final IInstrumentRepository instrumentRepository;

    public InstrumentService(IInstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Inbound(values = {CREATE_INSTRUMENT})
    @Outbound(INSTRUMENT_CREATED)
    @Transactional(type=RW)
    public InstrumentCreated createInstrument(CreateInstrument createInstrument) {
        System.out.println("APP: Instrument-service received a create request for ID "+ createInstrument.id +" with TID: "+createInstrument.instanceId);
        
        Instrument instrument = new Instrument(
            Integer.parseInt(createInstrument.id),
            createInstrument.name,
            createInstrument.isin,
            createInstrument.assetClass,
            createInstrument.lotSize,
            createInstrument.currency,
            createInstrument.tradeable
        );
        
        System.out.println("Service Build Id: 102");

        System.out.println("APP: Inserting instrument with ID: " + instrument.instrument_id + 
                          ", name: " + instrument.name + 
                          ", tradeable: " + instrument.isTradeable());
        
        this.instrumentRepository.insert(instrument);

        System.out.println("APP: Instrument inserted into repository with ID: " + instrument.instrument_id + 
                          ", name: " + instrument.name + 
                          ", tradeable: " + instrument.isTradeable());

        Instrument savedInstrument = this.instrumentRepository.lookupByKey(instrument.instrument_id);

        System.out.println("APP: Instrument is null: " + (savedInstrument == null));

        List<Instrument> allInstruments = this.instrumentRepository.getAll();
        System.out.println("APP: All instruments in repository: " + allInstruments.size());
        for (Instrument inst : allInstruments) {
            System.out.println("APP: Instrument ID: " + inst.instrument_id + 
                              ", name: " + inst.name + 
                              ", tradeable: " + inst.isTradeable());
        }
        
        
        if (savedInstrument != null) {
            System.out.println("APP: Instrument created successfully with ID: " + savedInstrument.instrument_id + 
                              ", name: " + savedInstrument.name + 
                              ", tradeable: " + savedInstrument.isTradeable() + 
                              ", TID: " + createInstrument.instanceId);
            
            finsim.common.entities.Instrument convertedInstrument = InstrumentUtils.convertInstrument(savedInstrument);
            System.out.println("APP: Converted instrument: " + convertedInstrument);
            
            return new InstrumentCreated(
                new Date(), 
                createInstrument, 
                convertedInstrument, 
                createInstrument.instanceId
            );
        } else {
            System.out.println("APP: Failed to create instrument with ID "+createInstrument.id+" TID: "+createInstrument.instanceId);
            return new InstrumentCreated(
                new Date(), 
                createInstrument, 
                null, 
                createInstrument.instanceId
            );
        }
    }

    // Update instrument TO BE TRARDEABLE
    @Inbound(values = {UPDATE_INSTRUMENT})
    @Outbound(INSTRUMENT_CHANGED)
    @Transactional(type=RW)
    public InstrumentChanged updateInstrument(UpdateInstrument updateInstrument) {
        LOGGER.log(DEBUG, "APP: Instrument-service received an update request for ID "+ updateInstrument.id +" with TID: "+updateInstrument.instanceId);
        
        Integer instrumentId = Integer.parseInt(updateInstrument.id);
        Instrument existingInstrument = this.instrumentRepository.lookupByKey(instrumentId);
        

        if (existingInstrument == null) {
            LOGGER.log(ERROR, "APP: No instrument found for ID "+updateInstrument.id+" TID: "+updateInstrument.instanceId);
            return new InstrumentChanged(new Date(), updateInstrument, null, updateInstrument.instanceId);
        }
        
        existingInstrument.name = updateInstrument.name;
        existingInstrument.asset_class = updateInstrument.assetClass;
        existingInstrument.lot_size = updateInstrument.lotSize;
        existingInstrument.currency = updateInstrument.currency;
        existingInstrument.setTradeable(updateInstrument.tradeable); 
        existingInstrument.updated_at = new Date();
        
        this.instrumentRepository.update(existingInstrument);
        Instrument updatedInstrument = this.instrumentRepository.lookupByKey(existingInstrument.instrument_id);
        
        LOGGER.log(DEBUG, "APP: Instrument updated with TID: "+updateInstrument.instanceId);
        
        return new InstrumentChanged(
            new Date(), 
            updateInstrument, 
            InstrumentUtils.convertInstrument(updatedInstrument), 
            updateInstrument.instanceId
        );
    }

    @Inbound(values = {DELIST_INSTRUMENT})
    @Outbound(INSTRUMENT_DELISTED)
    @Transactional(type=RW)
    public InstrumentDelisted delistInstrument(DelistInstrument delistInstrument) {
        LOGGER.log(DEBUG, "APP: Instrument-service received a delist request for ID "+ delistInstrument.id +" with TID: "+delistInstrument.instanceId);
        
        Integer instrumentId = Integer.parseInt(delistInstrument.id);
        Instrument existingInstrument = this.instrumentRepository.lookupByKey(instrumentId);
        
        if (existingInstrument == null) {
            LOGGER.log(ERROR, "APP: No instrument found for ID "+delistInstrument.id+" TID: "+delistInstrument.instanceId);
            return new InstrumentDelisted(new Date(), delistInstrument, null, delistInstrument.instanceId);
        }
        
        // Mark instrument as non-tradeable
        existingInstrument.setTradeable(false); // Use helper method
        existingInstrument.updated_at = new Date();
        
        this.instrumentRepository.update(existingInstrument);
        Instrument updatedInstrument = this.instrumentRepository.lookupByKey(existingInstrument.instrument_id);
        
        LOGGER.log(DEBUG, "APP: Instrument delisted with TID: "+delistInstrument.instanceId);
        
        return new InstrumentDelisted(
            new Date(), 
            delistInstrument, 
            InstrumentUtils.convertInstrument(updatedInstrument), 
            delistInstrument.instanceId
        );
    }

    public finsim.common.entities.Instrument getInstrument(int instrumentId) {
        //Instrument instrument = this.instrumentRepository.lookupByKey(instrumentId);
        Instrument instrument = this.instrumentRepository.getTradableInstrumentById(instrumentId, 1);
        System.out.println("Looking up instrument with ID: " + instrumentId);
        System.out.println("Found instrument: " + (instrument != null ? instrument.toString() : "null"));
        if (instrument == null) {
            LOGGER.log(DEBUG, "No instrument found with ID {0}", instrumentId);
            System.out.println("No instrument found with ID " + instrumentId + ". Trying to find in all instruments...");

            List<Instrument> allInstruments = this.instrumentRepository.getAll();
            // try to find the instrument in all instruments
            for (Instrument ins : allInstruments) {
                if (ins.instrument_id == instrumentId) {
                    System.out.println("Found instrument with ID " + instrumentId + ": " + ins);
                    return InstrumentUtils.convertInstrument(ins);
                }
            }

            return null;
        }
        System.out.println("Found instrument with ID " + instrumentId + ": " + instrument);
        return InstrumentUtils.convertInstrument(instrument);
    }
    
    public List<finsim.common.entities.Instrument> getAllInstruments() {
        System.out.println("Getting all instruments from repository...");
        List<Instrument> instruments = this.instrumentRepository.getAll();
        System.out.println("Repository returned instruments: " + (instruments != null ? instruments.size() : "null"));
        
        if (instruments == null || instruments.isEmpty()) {
            System.out.println("No instruments found in repository");
            
            // For debugging
            Instrument testInstrument = this.instrumentRepository.lookupByKey(1);
            System.out.println("Test lookup of instrument with ID 1: " + (testInstrument != null ? "FOUND" : "NOT FOUND"));
            
            return List.of();
        }
        
        System.out.println("Converting " + instruments.size() + " instruments from repository entities to API entities");
        return InstrumentUtils.convertInstruments(instruments);
    }
}