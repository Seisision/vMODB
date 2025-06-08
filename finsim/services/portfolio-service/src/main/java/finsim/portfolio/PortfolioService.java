package finsim.portfolio;
import finsim.common.inputs.CreateTransfer;
import finsim.common.events.OrderHandled;
import finsim.common.events.TransferCreated;
import finsim.common.enums.BuySellType;
import finsim.portfolio.infra.TransferUtils;
import finsim.portfolio.repositories.ITransferRepository;
import finsim.portfolio.entities.Transfer;

import finsim.portfolio.infra.PositionUtils;
import finsim.portfolio.repositories.IPositionRepository;
import finsim.portfolio.entities.Position;

import dk.ku.di.dms.vms.modb.api.annotations.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.List;

import static finsim.common.Constants.*;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.RW;
import static dk.ku.di.dms.vms.modb.api.enums.TransactionTypeEnum.W;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

@Microservice("portfolio")
public final class PortfolioService {
    private int nextPositionId = 1000;

    private static final System.Logger LOGGER = System.getLogger(PortfolioService.class.getName());

    private final IPositionRepository positionRepository;
    private final ITransferRepository transferRepository;

    public PortfolioService(IPositionRepository positionRepository, ITransferRepository transferRepository) {
        this.positionRepository = positionRepository;
        this.transferRepository = transferRepository;
    }   

    @Inbound(values = {ORDER_HANDLED})
    @Transactional(type=RW)
    public void handleOrderHandled(OrderHandled orderHandled) {
        LOGGER.log(INFO, "APP: Portfolio-service received an order handled event for Order ID " + orderHandled.request.id);
        System.out.println("APP: Portfolio-service received an order handled event for Order ID " + orderHandled.request.id);
        
        // Check if the order was filled
        if (orderHandled.isFilled()) {
            // Determine positions to update or create
            double fillPrice = orderHandled.request.price; // Assuming the fill price is the same as the request price
            if (orderHandled.request.buySell == BuySellType.SELL)
            {
                // Deduct the filled quantity from the position
                List<Position> existingPositions = this.positionRepository.lookupByAccountAndInstrument(Integer.parseInt(orderHandled.request.accountId), Integer.parseInt(orderHandled.request.instrumentId));
                if (existingPositions.isEmpty()) {
                    LOGGER.log(ERROR, "APP: No existing position found for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                    return; // No position to update, cannot handle sell order
                } else {
                    // Assuming we only handle one position per account and instrument for simplicity
                    Position existingPosition = existingPositions.get(0);
                    if (existingPosition.amount < orderHandled.amount_filled) {
                        LOGGER.log(ERROR, "APP: Insufficient position amount for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId + ". Cannot fill sell order.");
                        return; // Not enough position to cover the sell order
                    }
                    existingPosition.amount -= orderHandled.amount_filled; // Adjust amount
                    if (existingPosition.amount <= 0) {
                        // If amount becomes zero or negative, we can consider this position closed
                        this.positionRepository.delete(existingPosition);
                        LOGGER.log(INFO, "APP: Position closed for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                    } else {
                        this.positionRepository.update(existingPosition);
                        LOGGER.log(INFO, "APP: Position updated for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                    }
                }

                // Add cash to instrumentId -1 position
                List<Position> cashPositions = this.positionRepository.lookupByAccountAndInstrument(Integer.parseInt(orderHandled.request.accountId), -1);
                if (cashPositions.isEmpty()) {
                    // Create a new cash position if none exists
                    Position cashPosition = new Position(
                        GeneratePositionId(), // Generate a unique position ID
                        -1, // Cash position
                        (int)(orderHandled.amount_filled * fillPrice), // Cash amount added
                        1, // Cash position does not have a price                        
                        Integer.parseInt(orderHandled.request.accountId)
                    );
                    this.positionRepository.insert(cashPosition);
                    LOGGER.log(INFO, "APP: New cash position created for Account ID " + orderHandled.request.accountId);
                } else {
                    // Update existing cash position
                    Position cashPosition = cashPositions.get(0);
                    cashPosition.amount += orderHandled.amount_filled * fillPrice; // Adjust cash amount
                    this.positionRepository.update(cashPosition);
                    LOGGER.log(INFO, "APP: Cash position updated for Account ID " + orderHandled.request.accountId);
                }
            }
            else {
                // This is a buy order, we need to create or update the position
                List<Position> existingPositions = this.positionRepository.lookupByAccountAndInstrument(Integer.parseInt(orderHandled.request.accountId), Integer.parseInt(orderHandled.request.instrumentId));
                if (existingPositions.isEmpty()) {
                    // Create a new position if none exists
                    Position newPosition = new Position(
                        GeneratePositionId(), // Generate a unique position ID
                        Integer.parseInt(orderHandled.request.instrumentId),
                        orderHandled.amount_filled, // Filled quantity
                        fillPrice, // Assuming the fill price is the same as the request price
                        Integer.parseInt(orderHandled.request.accountId)
                    );
                    this.positionRepository.insert(newPosition);
                    System.out.println("APP: New position created for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                    LOGGER.log(INFO, "APP: New position created for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                } else {
                    // Update existing position
                    Position existingPosition = existingPositions.get(0);
                    existingPosition.amount += orderHandled.amount_filled; // Adjust quantity
                    existingPosition.open_price = ((existingPosition.amount * existingPosition.open_price) + (orderHandled.amount_filled * fillPrice)) / (existingPosition.amount + orderHandled.amount_filled); // Weighted average price
                    this.positionRepository.update(existingPosition);
                    System.out.println("APP: Position updated for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                    LOGGER.log(INFO, "APP: Position updated for Account ID " + orderHandled.request.accountId + " and Instrument ID " + orderHandled.request.instrumentId);
                }
                // Deduct cash from instrumentId -1 position
                List<Position> cashPositions = this.positionRepository.lookupByAccountAndInstrument(Integer.parseInt(orderHandled.request.accountId), -1);
                if (cashPositions.isEmpty()) {
                    // Create a new cash position if none exists
                    Position cashPosition = new Position(
                        GeneratePositionId(), // Generate a unique position ID
                        -1, // Cash position
                        -(int)(orderHandled.amount_filled * fillPrice), // Cash amount deducted
                        1, // Cash position does not have a price                        
                        Integer.parseInt(orderHandled.request.accountId)
                    );
                    this.positionRepository.insert(cashPosition);
                    System.out.println("APP: New cash position created for Account ID " + orderHandled.request.accountId);
                    LOGGER.log(INFO, "APP: New cash position created for Account ID " + orderHandled.request.accountId);
                } else {
                    // Assuming we only handle one cash position per account
                    Position cashPosition = cashPositions.get(0);
                    cashPosition.amount -= orderHandled.amount_filled * fillPrice; // Adjust cash amount
                    if (cashPosition.amount < 0) {
                        LOGGER.log(ERROR, "APP: Insufficient cash for Account ID " + orderHandled.request.accountId + " to cover the buy order.");  
                    }
                    this.positionRepository.update(cashPosition);
                }
            }
            // Create a OrderPositionCreated event by calling the proxy service
            PostOrderPositionCreated(orderHandled);
        } else if (orderHandled.isRouted()) {
            // Do nothing for routed orders, as they are not filled yet
            LOGGER.log(INFO, "APP: Order with ID " + orderHandled.request.id + " was routed but not filled.");
        } else {
            // Do nothing for orders that were not filled or routed
            LOGGER.log(INFO, "APP: Order with ID " + orderHandled.request.id + " was aborted. Reason: " + orderHandled.aborted_reason);
        }
    }

    private int GeneratePositionId() {
        // Generate a unique position ID
        return nextPositionId++;
    }   

    private void PostOrderPositionCreated(OrderHandled orderHandled)
    {
        try {
            // Prepare the URL for the POST request
            String url = "http://localhost:8091/order_position_created"; // Adjust the URL as needed
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            // Create the JSON payload
            String jsonPayload = String.format(
                "{\"timestamp\":\"%s\", \"orderId\":%s, \"amountFilled\":%d, \"instanceId\":\"%s\"}",
                new Date().toString(),
                orderHandled.request.id,
                orderHandled.amount_filled,
                // Create new instance ID using order id and millisecond timestamp
                "orderPositionCreated-" + orderHandled.request.id + "-" + System.currentTimeMillis()
            );
            

            // Send the request
            con.getOutputStream().write(jsonPayload.getBytes());

            // Get the response code
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                LOGGER.log(INFO, "APP: OrderPositionCreated event posted successfully for Order ID " + orderHandled.request.id);
            } else {
                LOGGER.log(ERROR, "APP: Failed to post OrderPositionCreated event for Order ID " + orderHandled.request.id + ". Response Code: " + responseCode);
            }
        } catch (IOException e) {
            LOGGER.log(ERROR, "APP: Error posting OrderPositionCreated event for Order ID " + orderHandled.request.id, e);
        }
    }

    @Inbound(values = {CREATE_TRANSFER})
    @Outbound(TRANSFER_CREATED)
    @Transactional(type=W)
    public TransferCreated createTransfer(CreateTransfer createTransfer) {
        LOGGER.log(INFO, "APP: Portfolio-service received a create transfer request for ID " + createTransfer.id);
        System.out.println("APP: Portfolio-service received a create transfer request for ID " + createTransfer.id);
        
        // Validate transfer creation parameters
        if (createTransfer.quantity <= 0) {
            LOGGER.log(ERROR, "APP: Invalid transfer creation - amount must be greater than zero");
            return new TransferCreated(new Date(), createTransfer, null, createTransfer.instanceId);
        }
        
        // Update positions for the transfer
        if (createTransfer.sourceAccountId != "-1") {
            // Deduct from the fromAccount position
            List<Position> fromPositions = this.positionRepository.lookupByAccountAndInstrument(Integer.parseInt(createTransfer.sourceAccountId), Integer.parseInt(createTransfer.instrumentId));
            if (fromPositions.isEmpty()) {
                LOGGER.log(ERROR, "APP: No existing position found for fromAccount ID " + createTransfer.sourceAccountId + " and Instrument ID " + createTransfer.instrumentId);
                return new TransferCreated(new Date(), createTransfer, null, createTransfer.instanceId);
            } else {
                Position fromPosition = fromPositions.get(0);
                fromPosition.amount -= createTransfer.quantity; // Adjust quantity
                if (fromPosition.amount < 0) {
                    LOGGER.log(ERROR, "APP: Insufficient quantity in fromAccount ID " + createTransfer.sourceAccountId + " for transfer.");
                    return new TransferCreated(new Date(), createTransfer, null, createTransfer.instanceId);
                } else {
                    this.positionRepository.update(fromPosition);
                    LOGGER.log(INFO, "APP: Position updated for fromAccount ID " + createTransfer.sourceAccountId + " and Instrument ID " + createTransfer.instrumentId);
                }
            }
        }
        if (createTransfer.targetAccountId != "-1") {
            // Add to the toAccount position
            List<Position> toPositions = this.positionRepository.lookupByAccountAndInstrument(Integer.parseInt(createTransfer.targetAccountId), Integer.parseInt(createTransfer.instrumentId));
            if (toPositions.isEmpty()) {
                // Create a new position if none exists
                Position newPosition = new Position(
                    GeneratePositionId(), // Generate a unique position ID
                    Integer.parseInt(createTransfer.instrumentId),
                    createTransfer.quantity, // Filled quantity
                    0, // Price is not relevant for transfers (it could be, but for simplicity we set it to 0),
                    Integer.parseInt(createTransfer.targetAccountId)
                );
                this.positionRepository.insert(newPosition);
                LOGGER.log(INFO, "APP: New position created for toAccount ID " + createTransfer.targetAccountId + " and Instrument ID " + createTransfer.instrumentId);
            } else {
                // Update existing position
                Position toPosition = toPositions.get(0);
                toPosition.amount += createTransfer.quantity; // Adjust quantity
                this.positionRepository.update(toPosition);
                LOGGER.log(INFO, "APP: Position updated for toAccount ID " + createTransfer.targetAccountId + " and Instrument ID " + createTransfer.instrumentId);
            }
        }
        
        // Create the transfer entity
        Transfer transfer = new Transfer(
            Integer.parseInt(createTransfer.id),
            Integer.parseInt(createTransfer.instrumentId),
            createTransfer.quantity,
            Integer.parseInt(createTransfer.sourceAccountId),
            Integer.parseInt(createTransfer.targetAccountId),
            createTransfer.externalReference
        );
        
        this.transferRepository.insert(transfer);
        Transfer savedTransfer = this.transferRepository.lookupByKey(transfer.getId());
        
        if (savedTransfer != null) {
            LOGGER.log(INFO, "APP: Transfer created successfully with ID " + savedTransfer.getId());
            return new TransferCreated(new Date(), createTransfer, TransferUtils.convertTransfer(savedTransfer), createTransfer.instanceId);
        } else {
            LOGGER.log(ERROR, "APP: Failed to create transfer with ID " + createTransfer.id);
            return new TransferCreated(new Date(), createTransfer, null, createTransfer.instanceId);
        }
    }

    // getTransferById
    public finsim.common.entities.Transfer getTransferById(int transferId) {
        LOGGER.log(INFO, "APP: Portfolio-service received a request to get transfer by ID " + transferId);
        System.out.println("APP: Portfolio-service received a request to get transfer by ID " + transferId);
        
        Transfer transfer = this.transferRepository.lookupByKey(transferId);
        
        if (transfer != null) {
            LOGGER.log(INFO, "APP: Found transfer with ID " + transferId);
            return TransferUtils.convertTransfer(transfer);
        } else {
            LOGGER.log(ERROR, "APP: Transfer with ID " + transferId + " not found");
            return null;
        }
    }

    // getPositionById
    public finsim.common.entities.Position getPositionById(int positionId) {
        LOGGER.log(INFO, "APP: Portfolio-service received a request to get position by ID " + positionId);
        System.out.println("APP: Portfolio-service received a request to get position by ID " + positionId);
        
        Position position = this.positionRepository.lookupByKey(positionId);
        
        if (position != null) {
            LOGGER.log(INFO, "APP: Found position with ID " + positionId);
            return PositionUtils.convertPosition(position);
        } else {
            LOGGER.log(ERROR, "APP: Position with ID " + positionId + " not found");
            return null;
        }
    }

    // getPositionsByAccountId
    public List<finsim.common.entities.Position> getPositionsByAccountId(int accountId) {
        LOGGER.log(INFO, "APP: Portfolio-service received a request to get positions by Account ID " + accountId);
        System.out.println("APP: Portfolio-service received a request to get positions by Account ID " + accountId);
        
        List<Position> positions = this.positionRepository.lookupByAccount(accountId);
        
        if (positions != null && !positions.isEmpty()) {
            LOGGER.log(INFO, "APP: Found " + positions.size() + " positions for Account ID " + accountId);
            return PositionUtils.convertPositions(positions);
        } else {
            LOGGER.log(ERROR, "APP: No positions found for Account ID " + accountId);
            return List.of(); // Return an empty list if no positions found
        }
    }

    // getPositionByAccountAndInstrument
    public finsim.common.entities.Position getPositionByAccountAndInstrument(int accountId, int instrumentId) {
        LOGGER.log(INFO, "APP: Portfolio-service received a request to get position by Account ID " + accountId + " and Instrument ID " + instrumentId);
        System.out.println("APP: Portfolio-service received a request to get position by Account ID " + accountId + " and Instrument ID " + instrumentId);
        
        List<Position> positions = this.positionRepository.lookupByAccountAndInstrument(accountId, instrumentId);
        
        if (positions != null && !positions.isEmpty()) {
            LOGGER.log(INFO, "APP: Found position for Account ID " + accountId + " and Instrument ID " + instrumentId);
            return PositionUtils.convertPosition(positions.get(0)); // Assuming we only handle one position per account and instrument
        } else {
            LOGGER.log(ERROR, "APP: Position for Account ID " + accountId + " and Instrument ID " + instrumentId + " not found");
            return null;
        }
    }


}