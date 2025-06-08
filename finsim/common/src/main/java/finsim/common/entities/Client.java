package finsim.common.entities;

public final class Client {
    public String id;
    public String name;
    public Boolean active;

    public Client() { }
    public Client(String id, String name, Boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\""
                + ",\"name\":\"" + name + "\""
                + ",\"active\":" + active
                + "}";
    }
}