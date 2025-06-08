package finsim.client.infra;

import finsim.client.entities.Client;

import java.util.List;
import java.util.stream.Collectors;

public final class ClientUtils {
    private ClientUtils() {}

    public static finsim.common.entities.Client convertClient(Client client) {
        return new finsim.common.entities.Client(
            String.valueOf(client.id), // Convert int to String for common entity
            client.name,
            client.active == 1
        );
    }

    public static List<finsim.common.entities.Client> convertClients(List<Client> clients) {
        return clients.stream()
                .map(ClientUtils::convertClient)
                .collect(Collectors.toList());
    }
}