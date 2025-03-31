package com.example.orderentry;

import com.example.models.Order;
import com.example.models.OrderStatus;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

/**
 * Response object for order status requests.
 */
public class OrderStatusResponse {
    private final UUID orderId;
    private final String clientOrderId;
    private final OrderStatus status;
    private final BigDecimal price;
    private final BigInteger quantity;
    private final BigInteger filledQuantity;
    private final String symbol;
    private final String message;
    
    public OrderStatusResponse(UUID orderId, String clientOrderId, OrderStatus status, 
                              BigDecimal price, BigInteger quantity, BigInteger filledQuantity,
                              String symbol, String message) {
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
        this.status = status;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = filledQuantity;
        this.symbol = symbol;
        this.message = message;
    }
    
    // Factory method to create from an Order object
    public static OrderStatusResponse fromOrder(Order order, String message) {
        return new OrderStatusResponse(
            order.getId(),
            order.getClientOrderId(),
            order.getStatus(),
            order.getPrice(),
            order.getQuantity(),
            order.getFilledQuantity(),
            order.getSymbol(),
            message
        );
    }
    
    // Getters
    public UUID getOrderId() { return orderId; }
    public String getClientOrderId() { return clientOrderId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getPrice() { return price; }
    public BigInteger getQuantity() { return quantity; }
    public BigInteger getFilledQuantity() { return filledQuantity; }
    public String getSymbol() { return symbol; }
    public String getMessage() { return message; }
}