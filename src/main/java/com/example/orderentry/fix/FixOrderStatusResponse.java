package com.example.orderentry.fix;

import com.example.models.OrderStatus;
import com.example.orderentry.OrderStatusResponse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FIX-specific implementation of OrderStatusResponse.
 * Provides methods to convert the response to FIX message format.
 */
public class FixOrderStatusResponse extends OrderStatusResponse {

    public FixOrderStatusResponse(UUID orderId, String clientOrderId, OrderStatus status,
                                 BigDecimal price, BigInteger quantity, BigInteger filledQuantity,
                                 String symbol, String message) {
        super(orderId, clientOrderId, status, price, quantity, filledQuantity, symbol, message);
    }
    
    /**
     * Factory method to create from a base OrderStatusResponse.
     * 
     * @param response The base response
     * @return A FIX-specific response
     */
    public static FixOrderStatusResponse fromOrderStatusResponse(OrderStatusResponse response) {
        return new FixOrderStatusResponse(
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
     * Converts the response to a FIX message format.
     * 
     * @return A map of FIX tag-value pairs
     */
    public Map<Integer, String> toFixMessage() {
        Map<Integer, String> fixMessage = new HashMap<>();
        
        // Message type (35) - Order Status (8)
        fixMessage.put(35, "8");
        
        // Order ID (37)
        fixMessage.put(37, getOrderId().toString());
        
        // Client Order ID (11)
        if (getClientOrderId() != null) {
            fixMessage.put(11, getClientOrderId());
        }
        
        // Order status (39)
        fixMessage.put(39, convertStatusToFix(getStatus()));
        
        // Symbol (55)
        fixMessage.put(55, getSymbol());
        
        // Price (44)
        if (getPrice() != null) {
            fixMessage.put(44, getPrice().toPlainString());
        }
        
        // Order quantity (38)
        fixMessage.put(38, getQuantity().toString());
        
        // Filled quantity (14)
        fixMessage.put(14, getFilledQuantity().toString());
        
        // Remaining quantity (151)
        BigInteger remainingQty = getQuantity().subtract(getFilledQuantity());
        fixMessage.put(151, remainingQty.toString());
        
        // Text message (58)
        if (getMessage() != null) {
            fixMessage.put(58, getMessage());
        }
        
        return fixMessage;
    }
    
    /**
     * Converts internal order status to FIX status code.
     * 
     * @param status The internal order status
     * @return The FIX status code
     */
    private String convertStatusToFix(OrderStatus status) {
        switch (status) {
            case NEW: return "0";
            case PARTIALLY_FILLED: return "1";
            case FILLED: return "2";
            case CANCELED: return "4";
            case REJECTED: return "8";
            default: return "0";
        }
    }
}