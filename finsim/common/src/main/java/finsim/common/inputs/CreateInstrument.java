package finsim.common.inputs;
import dk.ku.di.dms.vms.modb.api.annotations.Event;

@Event
public final class CreateInstrument {
    public String id;
    public String name;
    public String isin;
    public String assetClass;
    public int lotSize;
    public String currency;
    public boolean tradeable;
    public String instanceId;

    public CreateInstrument() {}

    public CreateInstrument(String id, String name, String isin, String assetClass, int lotSize, String currency, boolean tradeable, String instanceId) {
        this.id = id;
        this.name = name;
        this.isin = isin;
        this.assetClass = assetClass;
        this.lotSize = lotSize;
        this.currency = currency;
        this.tradeable = tradeable;
        this.instanceId = instanceId;
    }
}