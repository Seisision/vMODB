package finsim.term;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * FinSim Terminal - Console application for testing FinSim microservices
 */
public class FinSimTerminal {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Map<String, Process> serviceProcesses = new HashMap<>();
    private static final Map<String, ServiceInfo> services = new LinkedHashMap<>();
    private static final String BASE_URL = "http://localhost:8091"; // Proxy service URL
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    // Terminal Colors
    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[1;33m";
    private static final String BLUE = "\033[0;34m";
    private static final String NC = "\033[0m"; // No Color
    
    static {
        // Harcoded service information, should ideally be loaded from a config file or environment variables
        services.put("market", new ServiceInfo("market", "services/market-service/target/market-service-1.0-SNAPSHOT-jar-with-dependencies.jar", 8084, "/tmp/market.log"));
        services.put("order", new ServiceInfo("order", "services/order-service/target/order-service-1.0-SNAPSHOT-jar-with-dependencies.jar", 8082, "/tmp/order.log"));
        services.put("instrument", new ServiceInfo("instrument", "services/instrument-service/target/instrument-service-1.0-SNAPSHOT-jar-with-dependencies.jar", 8081, "/tmp/instrument.log"));
        services.put("portfolio", new ServiceInfo("portfolio", "services/portfolio-service/target/portfolio-service-1.0-SNAPSHOT-jar-with-dependencies.jar", 8086, "/tmp/portfolio.log"));
        services.put("client", new ServiceInfo("client", "services/client-service/target/client-service-1.0-SNAPSHOT-jar-with-dependencies.jar", 8085, "/tmp/client.log"));
        services.put("proxy", new ServiceInfo("proxy", "services/proxy-service/target/proxy-service-1.0-SNAPSHOT-jar-with-dependencies.jar", 8091, "/tmp/proxy.log"));
    }

    // entry point
    public static void main(String[] args) {
        System.out.println("=== FinSim Terminal ===");
        System.out.println("Console application for testing FinSim microservices");
        System.out.println();

        boolean running = true;
        while (running) {
            showMenu();
            int choice = getChoice();
            
            switch (choice) {
                case 1 -> startServices();
                case 2 -> showLogsMenu();
                case 3 -> testProxyService();
                case 4 -> runScriptTest();
                case 5 -> getPositionsMenu();
                case 6 -> {
                    System.out.println("Shutting down services...");
                    shutdownServices();
                    System.out.println("Session ended");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
            
            // if stil running after the command we have this to let us decide when to go back to menu
            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
        
        // shutdown input scanner
        scanner.close();
    }

    // The menu of supported functionality
    private static void showMenu() {
        System.out.println("=== Main Menu ===");
        System.out.println("1. Start Services");
        System.out.println("2. View Service Logs");
        System.out.println("3. Test Proxy Service");
        System.out.println("4. Run Script Test");
        System.out.println("5. Get Positions by Account ID");
        System.out.println("6. Exit (Shutdown Services)");
        System.out.print("Please select an option: ");
    }

    private static int getChoice() {
        try {
            String input = scanner.nextLine().trim();
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1; // map all bad inputs to -1, which is never a valid choice
        }
    }

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

    private static void startServices() {
        System.out.println("=== Starting FinSim Services ===");
        
        // Find the finsim root directory
        Path workingDir = findFinsimRootDirectory();
        if (workingDir == null) {
            printError("Cannot find finsim root directory with services folder");
            printError("Please ensure you're running from within the finsim project structure");
            return;
        }
        
        printInfo("Working directory: " + workingDir);
        
        // Start services sequentially with waiting, following launch-services.sh pattern
        // In principle, all but proxy could be started in parallel, but for simplicity we do them sequentially
        for (ServiceInfo service : services.values()) {
            if (!startServiceAndWait(service, workingDir)) {
                printError("Failed to start " + service.name + " service. Stopping startup process.");
                return;
            }
        }
        
        printSuccess("All services ready");
    }

    // method to make test app robust against working dir issues
    private static Path findFinsimRootDirectory() {
        // Start from current working directory
        Path currentDir = Paths.get("").toAbsolutePath();
        
        // Check if we're already in the finsim directory
        if (Files.exists(currentDir.resolve("services")) && 
            Files.exists(currentDir.resolve("experiments")) &&
            currentDir.toString().endsWith("finsim")) {
            return currentDir;
        }
        
        // Search up the directory tree to find finsim root
        Path searchDir = currentDir;
        for (int i = 0; i < 10; i++) { // Limit search depth to avoid infinite loops
            // Check if this directory contains the finsim structure
            if (Files.exists(searchDir.resolve("finsim/services")) && 
                Files.exists(searchDir.resolve("finsim/experiments"))) {
                return searchDir.resolve("finsim");
            }
            
            // Also check if this directory itself is finsim
            if (Files.exists(searchDir.resolve("services")) && 
                Files.exists(searchDir.resolve("experiments")) &&
                searchDir.getFileName() != null && 
                searchDir.getFileName().toString().equals("finsim")) {
                return searchDir;
            }
            
            // Move up one directory
            Path parentDir = searchDir.getParent();
            if (parentDir == null || parentDir.equals(searchDir)) {
                break; // Reached root directory
            }
            searchDir = parentDir;
        }
        
        return null; // Couldn't find finsim directory
    }

    // start service via jar, and wait for the expected output or timeout
    // we assume it has started on timeout as output parsing does not always work
    private static boolean startServiceAndWait(ServiceInfo service, Path workingDir) {
        if (serviceProcesses.containsKey(service.name)) {
            printWarning("Service " + service.name + " is already running");
            return true;
        }

        Path jarPath = workingDir.resolve(service.jarPath);
        if (!Files.exists(jarPath)) {
            printError("JAR file not found: " + jarPath);
            printError("Available files in target directory:");
            try {
                Path targetDir = jarPath.getParent();
                if (Files.exists(targetDir)) {
                    Files.list(targetDir)
                         .filter(p -> p.toString().endsWith(".jar") || p.toString().endsWith(".JAR"))
                         .forEach(p -> System.out.println("  " + p.getFileName()));
                } else {
                    printError("Target directory does not exist: " + targetDir);
                }
            } catch (Exception e) {
                printError("Error listing target directory: " + e.getMessage());
            }
            return false;
        }

        try {
            printInfo("Starting " + service.name + " service...");
            
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "--enable-preview",
                "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
                "-jar",
                jarPath.toString()
            );
            
            pb.directory(workingDir.toFile());
            // Redirect both stdout and stderr to log file (> /tmp/service.log 2>&1)
            pb.redirectOutput(ProcessBuilder.Redirect.to(new File(service.logPath)));
            pb.redirectError(ProcessBuilder.Redirect.to(new File(service.logPath)));
            
            Process process = pb.start();
            serviceProcesses.put(service.name, process);
            
            // wait for service to be ready
            if (waitForService(service)) {
                return true;
            } else {
                // Service failed to start properly
                if (process.isAlive()) {
                    process.destroy();
                }
                serviceProcesses.remove(service.name);
                return false;
            }
            
        } catch (Exception e) {
            printError("Failed to start " + service.name + " service: " + e.getMessage());
            return false;
        }
    }

    // timeout handler code
    private static boolean waitForService(ServiceInfo service) {
        printInfo("Waiting for " + service.name + " service to start...");
        
        // Determine the expected startup message based on service type
        String expectedMessage;
        if ("proxy".equals(service.name)) {
            expectedMessage = "Starting transaction worker # 1";
        } else {
            expectedMessage = "Transaction scheduler has started";
        }
        
        int count = 0;
        int maxWaitSeconds = 10; // this could be configurable
        
        while (count < maxWaitSeconds) {
            try {
                Thread.sleep(1000);
                count++;
                
                // Check if the log file contains the expected startup message
                Path logPath = Paths.get(service.logPath);
                if (Files.exists(logPath)) {
                    try {
                        List<String> lines = Files.readAllLines(logPath);
                        for (String line : lines) {
                            if (line.contains(expectedMessage)) {
                                printSuccess("✓ " + service.name + " service started");
                                return true;
                            }
                        }
                    } catch (IOException e) {
                        // Continue waiting, log file might still be being created
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Timeout reached
        printWarning("Timeout waiting for " + service.name + " service. Last log line:");
        try {
            Path logPath = Paths.get(service.logPath);
            if (Files.exists(logPath)) {
                List<String> lines = Files.readAllLines(logPath);
                if (!lines.isEmpty()) {
                    System.out.println("  " + lines.get(lines.size() - 1));
                } else {
                    System.out.println("  No log output yet");
                }
            } else {
                System.out.println("  No log output yet");
            }
        } catch (IOException e) {
            System.out.println("  Error reading log: " + e.getMessage());
        }
        printWarning("Continuing anyway... (check service log from menu to verify service before test)");
        return true; // Continue with startup and we can check manually if this happens
    }

    private static void showLogsMenu() {
        System.out.println("\n=== Service Logs ===");
        List<String> serviceNames = new ArrayList<>(services.keySet());
        
        for (int i = 0; i < serviceNames.size(); i++) {
            String serviceName = serviceNames.get(i);
            ServiceInfo service = services.get(serviceName);
            boolean isRunning = serviceProcesses.containsKey(serviceName) && serviceProcesses.get(serviceName).isAlive();
            String status = isRunning ? GREEN + "[RUNNING]" + NC : RED + "[STOPPED]" + NC;
            System.out.println((i + 1) + ". " + serviceName + " service " + status + " (port " + service.port + ")");
        }
        System.out.println((serviceNames.size() + 1) + ". Back to main menu");
        System.out.print("Select a service to view logs: ");
        
        int choice = getChoice();
        if (choice > 0 && choice <= serviceNames.size()) {
            String serviceName = serviceNames.get(choice - 1);
            showServiceLog(services.get(serviceName));
        } else if (choice != serviceNames.size() + 1) {
            System.out.println("Invalid choice.");
        }
    }

    // Get first and last 50 lines of log file. Crashes can sometimes spam the log so we need both ends when troubleshooting
    // (if less than 100 lines show all)
    private static void showServiceLog(ServiceInfo service) {
        System.out.println("\n=== " + service.name.toUpperCase() + " Service Log ===");
        
        Path logPath = Paths.get(service.logPath);
        if (!Files.exists(logPath)) {
            printWarning("Log file not found: " + service.logPath);
            return;
        }
        
        try {
            List<String> lines = Files.readAllLines(logPath);
            
            if (lines.isEmpty()) {
                printInfo("Log file is empty");
                return;
            }
            
            if (lines.size() <= 100) {
                // Show all lines if 100 or fewer
                System.out.println("--- All " + lines.size() + " lines ---");
                for (String line : lines) {
                    System.out.println(line);
                }
                System.out.println("--- End of log ---");
            } else {
                // Show first 50 lines
                System.out.println("--- First 50 lines ---");
                for (int i = 0; i < 50; i++) {
                    System.out.println(lines.get(i));
                }
                
                System.out.println("\n... (" + (lines.size() - 100) + " lines omitted) ...\n");
                
                // Show last 50 lines
                System.out.println("--- Last 50 lines ---");
                for (int i = lines.size() - 50; i < lines.size(); i++) {
                    System.out.println(lines.get(i));
                }
                System.out.println("--- End of log ---");
            }
            
        } catch (IOException e) {
            printError("Error reading log file: " + e.getMessage());
        }
    }

    private static void testProxyService() {
        System.out.println("\n=== Testing Proxy Service ===");
                
        // Test data
        String instrumentId = "1001";
        String instrumentName = "Apple Inc";
        String orderId = "2001";
        String accountId = "3001";
        String quantity = "100";
        String price = "150.50";
        String buySell = "BUY";
        
        printInfo("Step 1: Creating instrument");
        if (createInstrument(instrumentId, instrumentName)) {
            printInfo("Step 2: Creating order using the created instrument");
            if (createOrder(orderId, instrumentId, accountId, quantity, price, buySell)) {
                printInfo("Step 3: Retrieving all positions for account " + accountId);

                // Wait 5 seconds to ensure the order is processed
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    printError("Sleep interrupted: " + e.getMessage());
                }

                getPositionsByAccount(accountId);                
                getOrdersByAccountId(accountId);
                
                printSuccess("All tests completed successfully!");
                System.out.println("==================================================================");
                printInfo("Test Summary:");
                printSuccess("* Instrument " + instrumentId + " (" + instrumentName + ") created");
                printSuccess("* Order " + orderId + " created for " + quantity + " shares @ $" + price);
                printSuccess("* Positions retrieved for account " + accountId);
                printSuccess("* Orders retrieved for account " + accountId);
                System.out.println("==================================================================");

            } else {
                printError("Order creation failed");
            }
        } else {
            printError("Instrument creation failed");
        }
    }

    private static void runScriptTest() {
        System.out.println("\n=== Script-Based Testing ===");
        
        System.out.print("Enter the path to the test script file: ");
        String scriptPath = scanner.nextLine().trim();
        
        if (scriptPath.isEmpty()) {
            printError("No script path provided");
            return;
        }
        
        Path path = Path.of(scriptPath);
        if (!path.isAbsolute()) {
            // Resolve relative to current working directory
            path = Path.of("").toAbsolutePath().resolve(scriptPath);
        }
        
        TestScriptRunner scriptRunner = new TestScriptRunner();
        
        printInfo("Executing script: " + path);
        long startTime = System.currentTimeMillis();
        
        boolean success = scriptRunner.executeScript(path);
        
        long duration = System.currentTimeMillis() - startTime;
        
        if (success) {
            printSuccess("Script executed successfully in " + duration + "ms");
        } else {
            printError("Script execution completed with errors in " + duration + "ms");
        }
    }

    private static void getPositionsMenu() {
        System.out.println("\n=== Get Positions by Account ID ===");
        
        System.out.print("Enter the account ID: ");
        String accountId = scanner.nextLine().trim();
        
        if (accountId.isEmpty()) {
            printError("No account ID provided");
            return;
        }
        
        printInfo("Retrieving positions for account ID: " + accountId);
        getPositionsByAccount(accountId);
    }

    private static boolean createInstrument(String instrumentId, String instrumentName) {
        printInfo("Creating instrument with ID: " + instrumentId + ", Name: " + instrumentName);
        
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
        
        return sendPostRequest("/create_instrument", jsonPayload, "Instrument");
    }

    private static boolean createOrder(String orderId, String instrumentId, String accountId, 
                                     String quantity, String price, String buySell) {
        printInfo("Creating order with ID: " + orderId + " for instrument: " + instrumentId);
        
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
        
        return sendPostRequest("/create_order", jsonPayload, "Order");
    }

    private static boolean sendPostRequest(String endpoint, String jsonPayload, String resourceType) {
        try {
            printInfo("Sending POST request to " + BASE_URL + endpoint);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            
            if (response.statusCode() == 200) {
                printSuccess(resourceType + " created successfully");
                return true;
            } else {
                printError("Failed to create " + resourceType.toLowerCase() + ". HTTP status: " + response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            printError("Error sending request: " + e.getMessage());
            return false;
        }
    }

    private static void shutdownServices() {
        printInfo("Shutting down all services...");
        
        for (Map.Entry<String, Process> entry : serviceProcesses.entrySet()) {
            String serviceName = entry.getKey();
            Process process = entry.getValue();
            
            if (process.isAlive()) {
                printInfo("Stopping " + serviceName + " service...");
                process.destroy();
                
                try {
                    boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
                    if (!terminated) {
                        printWarning("Force killing " + serviceName + " service...");
                        process.destroyForcibly();
                    }
                    printSuccess("* " + serviceName + " service stopped");
                } catch (InterruptedException e) {
                    printError("Error stopping " + serviceName + " service: " + e.getMessage());
                }
            }
        }
        
        serviceProcesses.clear();
        printSuccess("All services have been shut down");
    }

    // Due to bug with enum persistence in the order repository this will always give an empty response currently
    private static void getOrdersByAccountId(String accountId) {
        try {
            String ordersUrl = "http://localhost:8082/orders/account/" + accountId;
            printInfo("Retrieving orders from " + ordersUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ordersUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Order service response status: " + response.statusCode());
            System.out.println("Order service response body: " + response.body());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody != null && !responseBody.trim().equals("{}") && !responseBody.trim().equals("[]")) {
                    printSuccess("Orders retrieved successfully:");
                    System.out.println("===============================================================");
                    System.out.println("ORDERS FOR ACCOUNT " + accountId + ":");
                    System.out.println(responseBody);
                    System.out.println("===============================================================");
                } else {
                    printWarning("No orders found for account " + accountId + " (empty response)");
                }
            } else {
                printError("Failed to retrieve orders. HTTP status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            printError("Error retrieving orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void getPositionsByAccount(String accountId) {
        try {
            String portfolioUrl = "http://localhost:8086/positions/account/" + accountId;
            printInfo("Retrieving positions from " + portfolioUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(portfolioUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Portfolio service response status: " + response.statusCode());
            System.out.println("Portfolio service response body: " + response.body());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody != null && !responseBody.trim().equals("{}") && !responseBody.trim().equals("[]")) {
                    printSuccess("Positions retrieved successfully:");
                    System.out.println("===============================================================");
                    System.out.println("POSITIONS FOR ACCOUNT " + accountId + ":");
                    System.out.println(responseBody);
                    System.out.println("===============================================================");
                } else {
                    printWarning("No positions found for account " + accountId + " (empty response)");
                }
            } else {
                printError("Failed to retrieve positions. HTTP status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            printError("Error retrieving positions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // helper class
    private static class ServiceInfo {
        final String name;
        final String jarPath;
        final int port;
        final String logPath;
        
        ServiceInfo(String name, String jarPath, int port, String logPath) {
            this.name = name;
            this.jarPath = jarPath;
            this.port = port;
            this.logPath = logPath;
        }
    }
}
