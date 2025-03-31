package com.example.models;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final String symbol;
    private final OrderType type;
    private final OrderSide side;
    private BigDecimal price;
    private BigInteger quantity;
    private BigInteger filledQuantity;
    private OrderStatus status;
    private final TimeInForce timeInForce;
    private final Instant createdAt;
    private Instant updatedAt;
    private final String clientOrderId; // Added clientOrderId field
    
    public Order(String symbol, OrderType type, OrderSide side, BigDecimal price, 
                BigInteger quantity, TimeInForce timeInForce, String clientOrderId) {
        this.id = UUID.randomUUID();
        this.symbol = symbol;
        this.type = type;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = BigInteger.ZERO;
        this.status = OrderStatus.NEW;
        this.timeInForce = timeInForce != null ? timeInForce : TimeInForce.GTC;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.clientOrderId = clientOrderId; // Initialize clientOrderId
    }
    
    // Convenience constructor without clientOrderId
    public Order(String symbol, OrderType type, OrderSide side, BigDecimal price, 
                BigInteger quantity) {
        this(symbol, type, side, price, quantity, TimeInForce.GTC, null);
    }
    
    // Convenience constructor without timeInForce and clientOrderId
    public Order(String symbol, OrderType type, OrderSide side, BigDecimal price, 
                BigInteger quantity, TimeInForce timeInForce) {
        this(symbol, type, side, price, quantity, timeInForce, null);
    }
    
    // Getters
    public UUID getId() {
        return id;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public OrderType getType() {
        return type;
    }
    
    public OrderSide getSide() {
        return side;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public BigInteger getQuantity() {
        return quantity;
    }
    
    public BigInteger getFilledQuantity() {
        return filledQuantity;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public TimeInForce getTimeInForce() {
        return timeInForce;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public String getClientOrderId() {
        return clientOrderId;
    }
    
    // Setters for mutable fields
    public void setPrice(BigDecimal price) {
        this.price = price;
        this.updatedAt = Instant.now();
    }
    
    public void setQuantity(BigInteger quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }
    
    public void setFilledQuantity(BigInteger filledQuantity) {
        this.filledQuantity = filledQuantity;
        this.updatedAt = Instant.now();
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", clientOrderId='" + clientOrderId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", type=" + type +
                ", side=" + side +
                ", price=" + price +
                ", quantity=" + quantity +
                ", filledQuantity=" + filledQuantity +
                ", status=" + status +
                ", timeInForce=" + timeInForce +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}