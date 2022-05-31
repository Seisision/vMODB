package dk.ku.di.dms.vms.coordinator.server.http;

import java.util.List;

/**
 * HTTP request entity.
 */
public record Request(
        String method,
        String uri,
        String version,
        List<Header> headers,
        byte[] body) {

    public String header(String name) {
        for (Header header : headers) {
            if (header.name().equalsIgnoreCase(name)) {
                return header.value();
            }
        }
        return null;
    }

    public boolean hasHeader(String name, String value) {
        for (Header header : headers) {
            if (header.name().equalsIgnoreCase(name) && header.value().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

}
