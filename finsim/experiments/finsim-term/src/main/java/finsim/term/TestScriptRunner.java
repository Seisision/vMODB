package finsim.term;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Script test runner
// 
// Valid commands:
// - REPEAT (N) ... END REPEAT: repeats commands N times (each on new line)
// - CREATE_INSTRUMENT (ID): creates an instrument with specified ID
// - CREATE_BUY_ORDER (ACCOUNT_ID, INSTRUMENT_ID, AMOUNT): creates a buy order (price 100)
// - CREATE_SELL_ORDER (ACCOUNT_ID, INSTRUMENT_ID, AMOUNT): creates a sell order (price 100)
// - SLEEP (N): sleeps for N milliseconds
// 
// NOTE: Nested REPEAT blocks have not been tested but should work
// Future Improvement: REPEAT PARALLEL (N,P) to run N commands in parallel (P threads)
// 
// Use # for comments
//
// Examples:
// see high-volume-test.script, high-volume-test-2.script, sample-test.script
public class TestScriptRunner {
    
    private static final String BASE_URL = "http://localhost:8091"; // proxy base url

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // use atomic integer to support future multithreading, could be counter now
    private static final AtomicInteger orderIdGenerator = new AtomicInteger(1);
    
    // ANSI Colors (for niceness as in terminal)
    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[1;33m";
    private static final String BLUE = "\033[0;34m";
    private static final String NC = "\033[0m"; // No Color, to reset
    
    // Test counter
    private int totalInstrumentsCreated = 0;
    private int totalOrdersCreated = 0;
    private int totalErrors = 0;
    
    // Execute the script with the file (can use relative)
    public boolean executeScript(Path scriptPath) {
        if (!Files.exists(scriptPath)) {
            printError("ERROR! Script file not found: " + scriptPath);
            return false;
        }
        
        // use getFileName to have full path if entering relative
        printInfo("Starting script: " + scriptPath.getFileName());
        resetCounters();
        
        try (BufferedReader reader = Files.newBufferedReader(scriptPath)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { // Skip empty lines and comments
                    lines.add(line);
                }
            }
            
            boolean success = executeCommands(lines);
            printExecutionSummary();
            return success;
            
        } catch (IOException e) {
            printError("Error reading script file: " + e.getMessage());
            return false;
        }
    }
    
    // Execute a list of commands after having loaded the script file
    private boolean executeCommands(List<String> commands) {
        int i = 0;
        while (i < commands.size()) {
            String command = commands.get(i);
            
            // REPEAT must have an END REPEAT
            if (command.startsWith("REPEAT")) {
                // Handle REPEAT block
                int repeatCount = parseRepeatCount(command);
                if (repeatCount <= 0) {
                    printError("Invalid REPEAT count: " + command);
                    totalErrors++;
                    return false;
                }
                
                // Find the matching END REPEAT
                int endIndex = findEndRepeat(commands, i);
                if (endIndex == -1) {
                    printError("No matching END REPEAT found for line: " + command);
                    totalErrors++;
                    return false;
                }
                
                // Extract commands within the REPEAT block
                List<String> repeatCommands = commands.subList(i + 1, endIndex);
                
                printInfo("Executing REPEAT block " + repeatCount + " times (" + repeatCommands.size() + " commands)");
                
                // Execute the block repeatCount times
                for (int rep = 0; rep < repeatCount; rep++) {
                    printInfo("Repeat iteration " + (rep + 1) + "/" + repeatCount);
                    if (!executeCommands(repeatCommands)) {
                        return false;
                    }
                }
                
                // Skip to after the END REPEAT
                i = endIndex + 1;
                
            } else {
                // Execute single command
                if (!executeCommand(command)) {
                    totalErrors++;
                    // Continue execution instead of stopping on single command failure
                }
                i++;
            }
        }
        
        return totalErrors == 0;
    }
    
    // Run a single command
    private boolean executeCommand(String command) {
        try {
            if (command.startsWith("CREATE_INSTRUMENT")) {
                return executeCreateInstrument(command);
            } else if (command.startsWith("CREATE_BUY_ORDER")) {
                return executeCreateOrder(command, "BUY");
            } else if (command.startsWith("CREATE_SELL_ORDER")) {
                return executeCreateOrder(command, "SELL");
            } else if (command.startsWith("SLEEP")) {
                return executeSleep(command);
            } else if (command.equals("END REPEAT")) {
                // This should not be reached if file is valid but return anyways
                return true;
            } else {
                // Warn about unknown command, probably a typo
                printWarning("Unknown command: " + command);
                return true; 
            }
        } catch (Exception e) {
            printError("Error executing command '" + command + "': " + e.getMessage());
            return false;
        }
    }
        
    private boolean executeCreateInstrument(String command) {
        String instrumentId = parseParameter(command, "CREATE_INSTRUMENT");
        if (instrumentId == null) {
            printError("Invalid CREATE_INSTRUMENT command: " + command);
            return false;
        }
        
        printInfo("Creating instrument with ID: " + instrumentId);
        
        String instrumentName = "Test Instrument " + instrumentId;
        String jsonPayload = String.format("""
            {
                "id": "%s",
                "name": "%s",
                "isin": "US%s000001",
                "assetClass": "EQUITY",
                "lotSize": 100,
                "currency": "USD",
                "tradeable": true,
                "instanceId": "test-instrument-%s-%d"
            }
            """, instrumentId, instrumentName, instrumentId, instrumentId, System.currentTimeMillis());
        
        boolean success = sendPostRequest("/create_instrument", jsonPayload, "Instrument");
        if (success) {
            totalInstrumentsCreated++;
        }
        return success;
    }
    
    // Executes CREATE_BUY_ORDER or CREATE_SELL_ORDER command.
    // Format: CREATE_BUY_ORDER (ACCOUNT_ID, INSTRUMENT_ID, AMOUNT)
    // Format: CREATE_SELL_ORDER (ACCOUNT_ID, INSTRUMENT_ID, AMOUNT)
    private boolean executeCreateOrder(String command, String buySell) {
        String[] params = parseMultipleParameters(command, buySell.equals("BUY") ? "CREATE_BUY_ORDER" : "CREATE_SELL_ORDER");
        if (params == null || params.length != 3) {
            printError("Invalid " + (buySell.equals("BUY") ? "CREATE_BUY_ORDER" : "CREATE_SELL_ORDER") + " command: " + command);
            return false;
        }
        
        String accountId = params[0];
        String instrumentId = params[1];
        String quantity = params[2];
        String orderId = String.valueOf(orderIdGenerator.getAndIncrement());
        
        printInfo("Creating " + buySell + " order: account=" + accountId + ", instrument=" + instrumentId + ", quantity=" + quantity);
        
        // hardcoded price for now, could be fourth parameter in future
        String price = "100.00";
        
        // Create yyyy-MM-dd format for expiry date (today + 1 day)
        String simplifiedDate = LocalDateTime.now().plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String jsonPayload = String.format("""
            {
                "id": "%s",
                "accountId": "%s",
                "instrumentId": "%s",
                "quantity": %s,
                "price": %s,
                "buySell": "%s",
                "duration": "DAY_ORDER",
                "expiryDateTime": "%s",
                "instanceId": "test-order-%s-%d"
            }
            """, orderId, accountId, instrumentId, quantity, price, buySell, simplifiedDate, orderId, System.currentTimeMillis());
        
        boolean success = sendPostRequest("/create_order", jsonPayload, "Order");
        if (success) {
            totalOrdersCreated++;
        }
        return success;
    }
        
    private boolean executeSleep(String command) {
        String millisStr = parseParameter(command, "SLEEP");
        if (millisStr == null) {
            printError("Invalid SLEEP command: " + command);
            return false;
        }
        
        try {
            int millis = Integer.parseInt(millisStr);
            printInfo("Sleeping for " + millis + " milliseconds");
            Thread.sleep(millis);
            return true;
        } catch (NumberFormatException e) {
            printError("Invalid sleep duration: " + millisStr);
            return false;
        } catch (InterruptedException e) {
            printWarning("Sleep interrupted");
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private int parseRepeatCount(String command) {
        String countStr = parseParameter(command, "REPEAT");
        if (countStr == null) {
            return -1;
        }
        
        try {
            return Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private int findEndRepeat(List<String> commands, int repeatIndex) {
        int nestLevel = 1;
        for (int i = repeatIndex + 1; i < commands.size(); i++) {
            String cmd = commands.get(i);
            if (cmd.startsWith("REPEAT")) {
                nestLevel++;
            } else if (cmd.equals("END REPEAT")) {
                nestLevel--;
                if (nestLevel == 0) {
                    return i;
                }
            }
        }
        return -1; // No matching END REPEAT found
    }
    
    private String parseParameter(String command, String commandName) {
        int start = command.indexOf('(');
        int end = command.lastIndexOf(')');
        
        if (start == -1 || end == -1 || start >= end) {
            return null;
        }
        
        return command.substring(start + 1, end).trim();
    }
    
    private String[] parseMultipleParameters(String command, String commandName) {
        String paramStr = parseParameter(command, commandName);
        if (paramStr == null) {
            return null;
        }
        
        String[] params = paramStr.split(",");
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }
        
        return params;
    }
    
    private boolean sendPostRequest(String endpoint, String jsonPayload, String resourceType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                printSuccess(resourceType + " created successfully");
                return true;
            } else {
                printError("Failed to create " + resourceType.toLowerCase() + ". HTTP status: " + response.statusCode());
                printError("Response: " + response.body());
                return false;
            }
            
        } catch (Exception e) {
            printError("Error sending request: " + e.getMessage());
            return false;
        }
    }
    
    private void resetCounters() {
        totalInstrumentsCreated = 0;
        totalOrdersCreated = 0;
        totalErrors = 0;
        orderIdGenerator.set(1);
    }   

    private void printExecutionSummary() {
        System.out.println("=========================================================");
        printInfo("Script Execution Summary:");
        printSuccess("* Instruments created: " + totalInstrumentsCreated);
        printSuccess("* Orders created: " + totalOrdersCreated);
        if (totalErrors > 0) {
            printError("! Errors encountered: " + totalErrors);
        } else {
            printSuccess("* No errors encountered");
        }
        System.out.println("=========================================================");
    }
    
    // Utility methods for colored output (color and then reset with NC)
    private static void printInfo(String message) {
        System.out.println(BLUE + "[INFO]" + NC + " " + message);
    }

    private static void printSuccess(String message) {
        System.out.println(GREEN + "[SUCCESS]" + NC + " " + message);
    }

    private static void printWarning(String message) {
        System.out.println(YELLOW + "[WARNING]" + NC + " " + message);
    }

    private static void printError(String message) {
        System.out.println(RED + "[ERROR]" + NC + " " + message);
    }
}
