package finsim.portfolio.infra;

import finsim.portfolio.entities.Position;

import java.util.List;
import java.util.stream.Collectors;

public final class PositionUtils {
    private PositionUtils() {}

    public static finsim.common.entities.Position convertPosition(Position position) {
        return new finsim.common.entities.Position(
            String.valueOf(position.id), // Convert int to String for common entity
            String.valueOf(position.account_id),
            String.valueOf(position.instrument_id),
            position.amount,
            position.open_price
        );
    }

    public static List<finsim.common.entities.Position> convertPositions(List<Position> positions) {
        return positions.stream()
                .map(PositionUtils::convertPosition)
                .collect(Collectors.toList());
    }
}