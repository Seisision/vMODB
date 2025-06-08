package finsim.instrument.infra;

import finsim.instrument.entities.Instrument;

import java.util.List;
import java.util.stream.Collectors;

public final class InstrumentUtils {
    private InstrumentUtils() {}

    public static finsim.common.entities.Instrument convertInstrument(Instrument instrument) {
        if (instrument == null) {
            return null; // Return null if instrument is null
        }
        return new finsim.common.entities.Instrument(
            String.valueOf(instrument.instrument_id), // Convert int to String for common entity
            instrument.name,
            instrument.isin,
            instrument.asset_class,
            instrument.lot_size,
            instrument.currency,
            instrument.tradeable == 1 // Convert int to boolean
        );
    }

    public static List<finsim.common.entities.Instrument> convertInstruments(List<Instrument> instruments) {
        return instruments.stream()
                .map(InstrumentUtils::convertInstrument)
                .collect(Collectors.toList());
    }
}