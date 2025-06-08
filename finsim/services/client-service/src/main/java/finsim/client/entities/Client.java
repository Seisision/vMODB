package finsim.client.entities;

import dk.ku.di.dms.vms.modb.api.annotations.VmsIndex;
import dk.ku.di.dms.vms.modb.api.annotations.VmsTable;
import dk.ku.di.dms.vms.modb.api.interfaces.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@VmsTable(name="clients")
public final class Client implements IEntity<Integer> {
    
    @Id
    @VmsIndex(name = "client_idx")
    public int id;
    
    @Column
    public Date created_at;
    
    @Column
    public Date updated_at;

    @Column
    public String name;

    @Column
    public int active;

    public Client() {}

    public Client(int id, String name, Boolean active ) {
        this.id = id;
        this.name = name;
        this.active = active ? 1 : 0;
        this.created_at = new Date();
        this.updated_at = new Date();
    }
        
    @Override
    public String toString() {
        return "Client{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", created_at=" + created_at +
                ", updated_at=" + updated_at +
                '}';
    }
}