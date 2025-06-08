package finsim.common.events;
import dk.ku.di.dms.vms.modb.api.annotations.Event;
import finsim.common.entities.Account;
import finsim.common.inputs.CreateAccount;

import java.util.Date;

@Event
public final class AccountCreated {
    public Date timestamp;
    public CreateAccount request;
    public Account account;
    public String instanceId;

    public AccountCreated() {}

    public AccountCreated(Date timestamp, CreateAccount request, Account account, String instanceId) {
        this.timestamp = timestamp;
        this.request = request;
        this.account = account;
        this.instanceId = instanceId;
    }
}
