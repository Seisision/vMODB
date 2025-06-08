package finsim.client.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.annotations.VmsForeignKey;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@VmsTable(name="accounts")
public final class Account implements IEntity<Integer> {
    
    @Id
    @VmsIndex(name = "account_idx")
    public int id;
    
    @Column
    public Date created_at;
    
    @Column
    public Date updated_at;

    @Column
    public String name;

    @Column
    public int active;

    @Id
    @VmsForeignKey(table=Client.class,column = "id")
    public int client_id;

    public Account() {}

    public Account(int id, String name, Boolean active, int client_id) {
        this.id = id;
        this.name = name;
        this.active = active ? 1 : 0;
        this.created_at = new Date();
        this.updated_at = new Date();
        this.client_id = client_id;
    }
        
    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", client_id=" + client_id +
                ", created_at=" + created_at +
                ", updated_at=" + updated_at +
                '}';
    }
}