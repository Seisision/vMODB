package finsim.instrument.infra;

import finsim.common.inputs.CreateInstrument;
import finsim.common.inputs.UpdateInstrument;
import finsim.common.inputs.DelistInstrument;
import finsim.common.events.InstrumentCreated;
import finsim.common.events.InstrumentChanged;
import finsim.common.events.InstrumentDelisted;
import finsim.instrument.InstrumentService;
import finsim.instrument.repositories.IInstrumentRepository;
import dk.ku.di.dms.vms.modb.common.serdes.IVmsSerdesProxy;
import dk.ku.di.dms.vms.modb.common.serdes.VmsSerdesProxyBuilder;
import dk.ku.di.dms.vms.modb.common.transaction.ITransactionManager;
import dk.ku.di.dms.vms.sdk.embed.client.DefaultHttpHandler;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static finsim.common.Constants.*;

import java.util.List;

public final class InstrumentHttpHandler extends DefaultHttpHandler {

    private static final System.Logger LOGGER = System.getLogger(InstrumentHttpHandler.class.getName());

    private final InstrumentService instrumentService;
    private static final IVmsSerdesProxy SERDES = VmsSerdesProxyBuilder.build();

    public InstrumentHttpHandler(ITransactionManager transactionManager,
                           IInstrumentRepository repository) {
        super(transactionManager);
        this.instrumentService = new InstrumentService(repository);
    }

    @Override
    public void post(String uri, String body) {
        LOGGER.log(DEBUG, "Received POST request: {0}", uri);
        System.out.println("POST request received: " + uri);
        System.out.println("Request body: " + body);
        
        try {
            if (uri.endsWith("/create_instrument")) {
                // Deserialize CreateInstrument from JSON
                CreateInstrument createInstrument = SERDES.deserialize(body, CreateInstrument.class);
                System.out.println("Deserialized CreateInstrument: id=" + createInstrument.id);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                InstrumentCreated result = instrumentService.createInstrument(createInstrument);
                System.out.println("Instrument created successfully: " + (result.instrument != null));
                // Explicitly commit the transaction
                //this.transactionManager.commit();
                this.transactionManager.reset();
                return;
            }
            else if (uri.endsWith("/update_instrument")) {
                // Deserialize UpdateInstrument from JSON
                UpdateInstrument updateInstrument = SERDES.deserialize(body, UpdateInstrument.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                InstrumentChanged result = instrumentService.updateInstrument(updateInstrument);
                System.out.println("Instrument updated successfully: " + (result.instrument != null));
                // Explicitly commit the transaction
                this.transactionManager.commit();
                return;
            }
            else if (uri.endsWith("/delist_instrument")) {
                // Deserialize DelistInstrument from JSON
                DelistInstrument delistInstrument = SERDES.deserialize(body, DelistInstrument.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                InstrumentDelisted result = instrumentService.delistInstrument(delistInstrument);
                // Make sure to explicitly commit the transaction
                this.transactionManager.commit();
                System.out.println("Instrument delisted successfully: " + (result.instrument != null));
                return;
            }
            
            // If we get here, no matching endpoint was found
            LOGGER.log(ERROR, "Unknown POST endpoint: {0}", uri);
            System.out.println("ERROR: Unknown POST endpoint: " + uri);
            this.transactionManager.reset();
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error processing POST request: {0}", e.getMessage());
            System.out.println("ERROR processing POST request: " + e.getMessage());
            e.printStackTrace();
            // Reset the transaction on error
            this.transactionManager.reset();
        }
    }

    @Override
    public void patch(String uri, String body) {
        LOGGER.log(DEBUG, "Received PATCH request: {0}", uri);
        System.out.println("PATCH request received: " + uri);
        System.out.println("Request body: " + body);
        
        try {
            if (uri.endsWith("/update_instrument")) {
                // Deserialize UpdateInstrument from JSON
                UpdateInstrument updateInstrument = SERDES.deserialize(body, UpdateInstrument.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                InstrumentChanged result = instrumentService.updateInstrument(updateInstrument);
                // Make sure to explicitly commit the transaction
                this.transactionManager.commit();
                System.out.println("Instrument updated successfully: " + (result.instrument != null));
                return;
            }
            else if (uri.endsWith("/delist_instrument")) {
                // Deserialize DelistInstrument from JSON
                DelistInstrument delistInstrument = SERDES.deserialize(body, DelistInstrument.class);
                
                // Begin transaction explicitly
                this.transactionManager.beginTransaction(0, 0, 0, false);
                InstrumentDelisted result = instrumentService.delistInstrument(delistInstrument);
                // Make sure to explicitly commit the transaction
                this.transactionManager.commit();
                System.out.println("Instrument delisted successfully: " + (result.instrument != null));
                return;
            }
            
            // If we get here, no matching endpoint was found
            LOGGER.log(ERROR, "Unknown PATCH endpoint: {0}", uri);
            System.out.println("ERROR: Unknown PATCH endpoint: " + uri);
            this.transactionManager.reset();
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error processing PATCH request: {0}", e.getMessage());
            System.out.println("ERROR processing PATCH request: " + e.getMessage());
            e.printStackTrace();
            this.transactionManager.reset();
        }
    }

    @Override
    public String getAsJson(String uri) {
        LOGGER.log(DEBUG, "Received GET request: {0}", uri);
        System.out.println("GET request received: " + uri);
        
        try {
            this.transactionManager.beginTransaction(0, 0, 0, true); // Read-only transaction
            
            // Handle GET requests for instruments
            if (uri.matches("/instruments/\\d+")) {
                String instrumentId = uri.substring(uri.lastIndexOf('/') + 1);
                System.out.println("Looking up instrument with ID: " + instrumentId);
                
                // List<finsim.common.entities.Instrument> allInstruments = instrumentService.getAllInstruments();
                // System.out.println("All instruments in repository: " + (allInstruments.isEmpty() ? "NONE" : allInstruments.size() + " instruments"));
                // for (finsim.common.entities.Instrument ins : allInstruments) {
                //     System.out.println("- Instrument: ID=" + ins.id + ", name=" + ins.name + ", tradeable=" + ins.tradeable);
                // }
                
                finsim.common.entities.Instrument instrument = instrumentService.getInstrument(Integer.parseInt(instrumentId));
                
                // End the transaction before returning
                //this.transactionManager.commit();
                
                if (instrument != null) {
                    String jsonResult = SERDES.serialize(instrument, finsim.common.entities.Instrument.class);
                    System.out.println("Returning instrument data: " + jsonResult);
                    return jsonResult;
                } else {
                    LOGGER.log(ERROR, "Instrument with ID {0} not found", instrumentId);
                    System.out.println("ERROR: Instrument with ID " + instrumentId + " not found, returning empty JSON");
                    return "{}"; // Return empty JSON if not found
                }
            }
            else if (uri.equals("/instruments")) {
                // To list all instruments
                List<finsim.common.entities.Instrument> instruments = instrumentService.getAllInstruments();
                
                // End the transaction before returning
                //this.transactionManager.commit();
                
                String result = SERDES.serializeList(instruments);
                System.out.println("Returning all instruments: " + result);
                return result;
            }
        } catch (Exception e) {
            LOGGER.log(ERROR, "Error processing GET request: {0}", e.getMessage());
            System.out.println("ERROR processing GET request: " + e.getMessage());
            e.printStackTrace();
            // Reset transaction on error
            //this.transactionManager.reset();
        }
        return "{}";
    }
}