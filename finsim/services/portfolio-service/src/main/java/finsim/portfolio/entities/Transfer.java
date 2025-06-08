package finsim.portfolio.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@VmsTable(name="transfers")
public final class Transfer implements IEntity<Integer> {
    
    @Id
    public int id;
    
    @Column
    public Date created_at;
    
    @Column
    public int instrument_id; // -1 for cash transfers

    @Column
    public int amount;

    @Column
    public int source_account_id; // -1 for external transfers

    @Column
    public int target_account_id; // -1 for external transfers

    @Column
    public String external_reference;

    public Transfer() {}

    public Transfer(int id, int instrument_id, int amount, int source_account_id, int target_account_id, String external_reference) {
        this.id = id;
        this.instrument_id = instrument_id;
        this.amount = amount;
        this.source_account_id = source_account_id;
        this.target_account_id = target_account_id;
        this.external_reference = external_reference;
        this.created_at = new Date();
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "id=" + id +
                ", instrument_id=" + instrument_id +
                ", amount=" + amount +
                ", source_account_id=" + source_account_id +
                ", target_account_id=" + target_account_id +
                ", external_reference='" + external_reference + '\'' +
                ", created_at=" + created_at +
                '}';
    }
}