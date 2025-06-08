package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Client;
import finsim.common.inputs.CreateClient;

import java.util.Date;

@Event
public final class ClientCreated {
    public Date timestamp;
    public CreateClient request;
    public Client client;
    public String instanceId;

    public ClientCreated() {}

    public ClientCreated(Date timestamp, CreateClient request, Client client, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.client = client;
        this.instanceId = instanceId;
    }
}
