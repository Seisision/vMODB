package finsim.common.entities;

import java.util.Date;

public final class Transfer {
    public String id;
    public String accountIdFrom;
    public String accountIdTo;
    public int amount;
    public String instrumentId;
    public String externalReference;
    public Date createdAt; 

    public Transfer() { }
    public Transfer(String id, String accountIdFrom, String accountIdTo, int amount, String instrumentId, String externalReference, Date createdAt) {
        this.id = id;
        this.accountIdFrom = accountIdFrom;
        this.accountIdTo = accountIdTo;
        this.amount = amount;
        this.instrumentId = instrumentId;
        this.externalReference = externalReference;
        this.createdAt = createdAt;
    }
    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\""
                + ",\"accountIdFrom\":\"" + accountIdFrom + "\""
                + ",\"accountIdTo\":\"" + accountIdTo + "\""
                + ",\"amount\":" + amount
                + ",\"instrumentId\":\"" + instrumentId + "\""
                + ",\"externalReference\":\"" + externalReference + "\""
                + ",\"createdAt\":\"" + (createdAt != null ? createdAt.toString() : "null") + "\""
                + "}";
    }
}
