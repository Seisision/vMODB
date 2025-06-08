package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;

@Event
public final class CreateClient {
    public String id;
    public String name;
    public String instanceId;

    public CreateClient() {}

    public CreateClient(String id, String name, String instanceId) {
        this.id = id;
        this.name = name;
        this.instanceId = instanceId;
    }
}
