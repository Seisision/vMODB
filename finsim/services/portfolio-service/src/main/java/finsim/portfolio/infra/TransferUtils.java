package finsim.portfolio.infra;

import finsim.portfolio.entities.Transfer;

import java.util.List;
import java.util.stream.Collectors;

public final class TransferUtils {
    private TransferUtils() {}

    public static finsim.common.entities.Transfer convertTransfer(Transfer transfer) {
        return new finsim.common.entities.Transfer(
            String.valueOf(transfer.id), // Convert int to String for common entity
            String.valueOf(transfer.source_account_id),
            String.valueOf(transfer.target_account_id),
            transfer.amount,
            String.valueOf(transfer.instrument_id),
            transfer.external_reference,
            transfer.created_at
        );
    }

    public static List<finsim.common.entities.Transfer> convertTransfers(List<Transfer> transfers) {
        return transfers.stream()
                .map(TransferUtils::convertTransfer)
                .collect(Collectors.toList());
    }
}