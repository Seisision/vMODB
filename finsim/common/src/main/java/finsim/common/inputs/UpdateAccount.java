package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public final class UpdateAccount {
    public String id;
    public String name;
    public String clientId;
    public Boolean isActive;
    public String instanceId;

    public UpdateAccount() {}

    public UpdateAccount(String id, String name, String clientId, Boolean isActive, String instanceId) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.isActive = isActive;
        this.instanceId = instanceId;
    }
}
