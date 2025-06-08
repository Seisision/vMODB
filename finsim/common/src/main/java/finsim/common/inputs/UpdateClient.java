package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;


@Event
public final class UpdateClient {
    public String id;
    public String name;
    public Boolean isActive;
    public String instanceId;

    public UpdateClient() {}

    public UpdateClient(String id, String name, Boolean isActive, String instanceId) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
        this.instanceId = instanceId;
    }
}