package finsim.client.infra;

import finsim.client.entities.Account;

import java.util.List;
import java.util.stream.Collectors;

public final class AccountUtils {
    private AccountUtils() {}

    public static finsim.common.entities.Account convertAccount(Account account) {
        return new finsim.common.entities.Account(
            String.valueOf(account.id), // Convert int to String for common entity
            account.name,
            account.active == 1, // Convert int to Boolean
            String.valueOf(account.client_id) // Convert int to String for common entity
        );
    }

    public static List<finsim.common.entities.Account> convertAccounts(List<Account> accounts) {
        return accounts.stream()
                .map(AccountUtils::convertAccount)
                .collect(Collectors.toList());
    }
}