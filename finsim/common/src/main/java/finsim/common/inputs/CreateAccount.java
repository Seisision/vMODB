package finsim.common.inputs;

import dk.ku.di.dms.vms.modb.api.annotations.Event;

import java.util.Date;

@Event
public final class CreateAccount {
    public String id;
    public String name;
    public String clientId;
    public String instanceId;

    public CreateAccount() {}

    public CreateAccount(String id, String name, String clientId, String instanceId) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.instanceId = instanceId;
    }
}
