package com.example.orderentry.binary;

import com.example.models.OrderStatus;
import com.example.orderentry.OrderStatusResponse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Binary-specific implementation of OrderStatusResponse.
 * Provides methods to convert the response to binary format.
 */
public class BinaryOrderStatusResponse extends OrderStatusResponse {

    public BinaryOrderStatusResponse(UUID orderId, String clientOrderId, OrderStatus status,
                                    BigDecimal price, BigInteger quantity, BigInteger filledQuantity,
                                    String symbol, String message) {
        super(orderId, clientOrderId, status, price, quantity, filledQuantity, symbol, message);
    }
    
    /**
     * Factory method to create from a base OrderStatusResponse.
     * 
     * @param response The base response
     * @return A Binary-specific response
     */
    public static BinaryOrderStatusResponse fromOrderStatusResponse(OrderStatusResponse response) {
        return new BinaryOrderStatusResponse(
            response.getOrderId(),
            response.getClientOrderId(),
            response.getStatus(),
            response.getPrice(),
            response.getQuantity(),
            response.getFilledQuantity(),
            response.getSymbol(),
            response.getMessage()
        );
    }
    
    /**
     * Converts the response to binary format.
     * 
     * @return A ByteBuffer containing the binary representation
     */
    public ByteBuffer toBinary() {
        // Example binary response format:
        // Bytes 0-1: Message type (101 = order status)
        // Bytes 2-17: Order ID (UUID as bytes)
        // Bytes 18-33: Client order ID (as string)
        // Byte 34: Status (1 = new, 2 = partially filled, 3 = filled, 4 = canceled, 5 = rejected)
        // Bytes 35-42: Price (as long, scaled by 10^8)
        // Bytes 43-50: Quantity (as long)
        // Bytes 51-58: Filled quantity (as long)
        // Bytes 59-74: Symbol (as string)
        // Bytes 75-138: Message (as string, up to 64 chars)
        
        ByteBuffer buffer = ByteBuffer.allocate(139);
        
        // Message type
        buffer.putShort((short) 101);
        
        // Order ID
        buffer.putLong(getOrderId().getMostSignificantBits());
        buffer.putLong(getOrderId().getLeastSignificantBits());
        
        // Client order ID
        byte[] clientOrderIdBytes = new byte[16];
        if (getClientOrderId() != null) {
            byte[] sourceBytes = getClientOrderId().getBytes();
            System.arraycopy(sourceBytes, 0, clientOrderIdBytes, 0, 
                    Math.min(sourceBytes.length, clientOrderIdBytes.length));
        }
        buffer.put(clientOrderIdBytes);
        
        // Status
        buffer.put(convertStatusToBinary(getStatus()));
        
        // Price
        long priceRaw = getPrice() != null ? 
                getPrice().scaleByPowerOfTen(8).longValue() : 0L;
        buffer.putLong(priceRaw);
        
        // Quantity
        buffer.putLong(getQuantity().longValue());
        
        // Filled quantity
        buffer.putLong(getFilledQuantity().longValue());
        
        // Symbol
        byte[] symbolBytes = new byte[16];
        if (getSymbol() != null) {
            byte[] sourceBytes = getSymbol().getBytes();
            System.arraycopy(sourceBytes, 0, symbolBytes, 0, 
                    Math.min(sourceBytes.length, symbolBytes.length));
        }
        buffer.put(symbolBytes);
        
        // Message
        byte[] messageBytes = new byte[64];
        if (getMessage() != null) {
            byte[] sourceBytes = getMessage().getBytes();
            System.arraycopy(sourceBytes, 0, messageBytes, 0, 
                    Math.min(sourceBytes.length, messageBytes.length));
        }
        buffer.put(messageBytes);
        
        buffer.flip();
        return buffer;
    }
    
    /**
     * Converts internal order status to binary status code.
     * 
     * @param status The internal order status
     * @return The binary status code
     */
    private byte convertStatusToBinary(OrderStatus status) {
        switch (status) {
            case NEW: return 1;
            case PARTIALLY_FILLED: return 2;
            case FILLED: return 3;
            case CANCELED: return 4;
            case REJECTED: return 5;
            default: return 0;
        }
    }
}