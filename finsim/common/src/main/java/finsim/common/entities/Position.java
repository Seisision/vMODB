package finsim.common.entities;

public final class Position {
    public String id;
    public String accountId;
    public String instrumentId;
    public int quantity;
    public Double openPrice; // the price at which the position was opened, if multiple orders are merged this is the weighted average price

    public Position() { }
    public Position(String id, String accountId, String instrumentId, int quantity, Double openPrice) {
        this.id = id;
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.quantity = quantity;
        this.openPrice = openPrice;
    }

    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\""
                + ",\"accountId\":\"" + accountId + "\""
                + ",\"instrumentId\":\"" + instrumentId + "\""
                + ",\"quantity\":" + quantity
                + ",\"openPrice\":" + openPrice
                + "}";
    }
}