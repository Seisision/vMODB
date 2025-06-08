package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Instrument;
import finsim.common.inputs.DelistInstrument;

import java.util.Date;

@Event
public final class InstrumentDelisted {
    public Date timestamp;
    public DelistInstrument request;
    public Instrument instrument;
    public String instanceId;

    public InstrumentDelisted() {}

    public InstrumentDelisted(Date timestamp, DelistInstrument request, Instrument instrument, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.instrument = instrument;
        this.instanceId = instanceId;
    }
}