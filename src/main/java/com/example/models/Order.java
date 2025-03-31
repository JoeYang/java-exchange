package com.example.models;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an order in the exchange system.
 */
public class Order {
    private final UUID id;
    private final String symbol;
    private final OrderType type;
    private final OrderSide side;
    private BigDecimal price;
    private BigInteger quantity;
    private BigInteger filledQuantity;
    private OrderStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private TimeInForce timeInForce;
    private Instant expiryTime;

    public Order(String symbol, OrderType type, OrderSide side, BigDecimal price, BigInteger quantity) {
        this(symbol, type, side, price, quantity, TimeInForce.DAY, null);
    }

    public Order(String symbol, OrderType type, OrderSide side, BigDecimal price, BigInteger quantity, 
                 TimeInForce timeInForce, Instant expiryTime) {
        this.id = UUID.randomUUID();
        this.symbol = symbol;
        this.type = type;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = BigInteger.ZERO;
        this.status = OrderStatus.NEW;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.timeInForce = timeInForce;
        this.expiryTime = expiryTime;
    }

    // Getters and setters
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

    public void setPrice(BigDecimal price) {
        this.price = price;
        this.updatedAt = Instant.now();
    }

    public BigInteger getQuantity() {
        return quantity;
    }

    public void setQuantity(BigInteger quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public BigInteger getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(BigInteger filledQuantity) {
        this.filledQuantity = filledQuantity;
        this.updatedAt = Instant.now();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
        this.updatedAt = Instant.now();
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Instant expiryTime) {
        this.expiryTime = expiryTime;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", type=" + type +
                ", side=" + side +
                ", price=" + price +
                ", quantity=" + quantity +
                ", filledQuantity=" + filledQuantity +
                ", status=" + status +
                ", timeInForce=" + timeInForce +
                ", expiryTime=" + expiryTime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}