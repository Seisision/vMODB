package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.inputs.CreateOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

@Event
public final class OrderHandled {
    public Date timestamp;
    public CreateOrder request;
    public int amount_filled;
    public String instanceId;
    public boolean is_aborted;
    public String aborted_reason;
    // For more realism store the fill price as it could be better than the order request price

    public OrderHandled() {}

    public OrderHandled(Date timestamp, CreateOrder request, int amount_filled, boolean is_aborted, String aborted_reason, 
                        String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.amount_filled = amount_filled;
        this.is_aborted = is_aborted;
        this.aborted_reason = aborted_reason;
        this.instanceId = instanceId;
    }

    @JsonIgnore
    public boolean isFilled() {
        return amount_filled > 0;
    }

    @JsonIgnore
    public boolean isRouted() {
        return amount_filled < request.quantity;
    }
}