package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Account;
import finsim.common.inputs.UpdateAccount;

import java.util.Date;

@Event
public final class AccountChanged {
    public Date timestamp;
    public UpdateAccount request;
    public Account account;
    public String instanceId;

    public AccountChanged() {}

    public AccountChanged(Date timestamp, UpdateAccount request, Account account, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.account = account;
        this.instanceId = instanceId;
    }
}
