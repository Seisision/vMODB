package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Client;
import finsim.common.inputs.UpdateClient;

import java.util.Date;

@Event
public final class ClientChanged {
    public Date timestamp;
    public UpdateClient request;
    public Client client;
    public String instanceId;

    public ClientChanged() {}

    public ClientChanged(Date timestamp, UpdateClient request, Client client, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.client = client;
        this.instanceId = instanceId;
    }
}
