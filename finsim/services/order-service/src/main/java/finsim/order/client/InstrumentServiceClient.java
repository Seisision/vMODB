package finsim.order.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static finsim.common.Constants.INSTRUMENT_VMS_PORT;

/**
 * Client for communicating with the Instrument Service.
 * Used to validate instruments exist before creating orders.
 */
public class InstrumentServiceClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    
    private static final System.Logger LOGGER = System.getLogger(InstrumentServiceClient.class.getName());

    public InstrumentServiceClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.baseUrl = "http://localhost:" + INSTRUMENT_VMS_PORT;
    }
    
    /**
     * Checks if an instrument exists by ID.
     * 
     * @param instrumentId the ID of the instrument to validate
     * @return true if the instrument exists, false otherwise
     */
    public boolean instrumentExists(String instrumentId) {
        try {
            URI uri = URI.create(baseUrl + "/instruments/" + instrumentId);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Check if we received a valid response (not empty) and status code 200
            if (response.statusCode() == 200 && !response.body().equals("{}")) {
                LOGGER.log(System.Logger.Level.DEBUG, "Instrument with ID " + instrumentId + " exists");
                return true;
            } else {
                LOGGER.log(System.Logger.Level.DEBUG, "Instrument with ID " + instrumentId + " does not exist");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Error checking if instrument exists: " + e.getMessage());
            System.out.println("Error checking if instrument exists: " + e.getMessage());
            return false; // Assume instrument doesn't exist in case of error
        }
    }
}