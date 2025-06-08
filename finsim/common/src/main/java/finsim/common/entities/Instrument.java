package finsim.common.entities;

public final class Instrument {
    public String id;
    public String name;
    public String isin; // International Securities Identification Number
    public String assetClass; // describes the type of instrument (stock, bond, etc)
    public int lotSize;
    public String currency;
    public boolean tradeable;

    public Instrument() { }

    public Instrument(String id, String name, String isin, String assetClass, int lotSize, String currency, boolean tradeable) {
        this.id = id;
        this.name = name;
        this.isin = isin;
        this.assetClass = assetClass;
        this.lotSize = lotSize;
        this.currency = currency;
        this.tradeable = tradeable;
    }

    @Override
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\""
                + ",\"name\":\"" + name + "\""
                + ",\"isin\":\"" + isin + "\""
                + ",\"assetClass\":\"" + assetClass + "\""
                + ",\"lotSize\":\"" + lotSize + "\""
                + ",\"currency\":\"" + currency + "\""
                + ",\"tradeable\":\"" + tradeable + "\""
                + "}";
    }
}