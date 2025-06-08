package finsim.common.entities;

public final class Account {
    public String id;
    public String name;
    public Boolean active;
    public String clientId; // The ID of the client this account belongs to

    public Account() { }
    public Account(String id, String name, Boolean active, String clientId) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\""
                + ",\"name\":\"" + name + "\""
                + ",\"active\":" + active
                + ",\"clientId\":\"" + clientId + "\""
                + "}";
    }
}