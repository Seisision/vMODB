package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Instrument;
import finsim.common.inputs.UpdateInstrument;

import java.util.Date;

@Event
public final class InstrumentChanged {
    public Date timestamp;
    public UpdateInstrument request;
    public Instrument instrument;
    public String instanceId;

    public InstrumentChanged() {}

    public InstrumentChanged(Date timestamp, UpdateInstrument request, Instrument instrument, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.instrument = instrument;
        this.instanceId = instanceId;
    }
}