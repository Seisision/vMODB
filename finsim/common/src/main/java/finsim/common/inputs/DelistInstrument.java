package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public final class DelistInstrument {
    public String id;
    public String instanceId;

    public DelistInstrument() {}

    public DelistInstrument(String id, String instanceId) {
        this.id = id;
        this.instanceId = instanceId;
    }
}