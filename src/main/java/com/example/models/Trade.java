package com.example.models;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a trade execution in the exchange system.
 * A trade occurs when a buy order matches with a sell order.
 */
public class Trade {
    private final UUID id;
    private final UUID buyOrderId;
    private final UUID sellOrderId;
    private final String symbol;
    private final BigDecimal price;
    private final BigInteger quantity;
    private final BigDecimal fee;
    private final Instant executionTime;
    private final String feeSymbol;
    
    public Trade(UUID buyOrderId, UUID sellOrderId, String symbol, 
                BigDecimal price, BigInteger quantity, BigDecimal fee, String feeSymbol) {
        this.id = UUID.randomUUID();
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.executionTime = Instant.now();
        this.fee = fee;
        this.feeSymbol = feeSymbol;
    }
    
    // Getters - all fields are final so no setters needed
    public UUID getId() {
        return id;
    }
    
    public UUID getBuyOrderId() {
        return buyOrderId;
    }
    
    public UUID getSellOrderId() {
        return sellOrderId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public BigInteger getQuantity() {
        return quantity;
    }
    
    public Instant getExecutionTime() {
        return executionTime;
    }
    
    public BigDecimal getFee() {
        return fee;
    }
    
    public String getFeeSymbol() {
        return feeSymbol;
    }
    
    /**
     * Calculates the total value of this trade.
     * @return The total value (price * quantity)
     */
    public BigDecimal getTotalValue() {
        return price.multiply(new BigDecimal(quantity));
    }
    
    @Override
    public String toString() {
        return "Trade{" +
                "id=" + id +
                ", buyOrderId=" + buyOrderId +
                ", sellOrderId=" + sellOrderId +
                ", symbol='" + symbol + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", executionTime=" + executionTime +
                ", fee=" + fee +
                ", feeSymbol='" + feeSymbol + '\'' +
                '}';
    }
}