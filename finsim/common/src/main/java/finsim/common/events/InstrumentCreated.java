package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Instrument;
import finsim.common.inputs.CreateInstrument;

import java.util.Date;

@Event
public final class InstrumentCreated {
    public Date timestamp;
    public CreateInstrument request;
    public Instrument instrument;
    public String instanceId;

    public InstrumentCreated() {}

    public InstrumentCreated(Date timestamp, CreateInstrument request, Instrument instrument, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.instrument = instrument;
        this.instanceId = instanceId;
    }
}