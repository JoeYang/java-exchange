package com.example.orderbook;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Represents the market depth of an order book, showing the quantity
 * available at different price levels for both bid and ask sides.
 */
public class MarketDepth {
    private final String symbol;
    private final List<PriceLevel> bids; // Sorted by price descending (highest first)
    private final List<PriceLevel> asks; // Sorted by price ascending (lowest first)
    
    public MarketDepth(String symbol, List<PriceLevel> bids, List<PriceLevel> asks) {
        this.symbol = symbol;
        this.bids = bids;
        this.asks = asks;
        
        // Ensure bids are sorted in descending order (highest price first)
        bids.sort((a, b) -> b.getPrice().compareTo(a.getPrice()));
        
        // Ensure asks are sorted in ascending order (lowest price first)
        asks.sort(Comparator.comparing(PriceLevel::getPrice));
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public List<PriceLevel> getBids() {
        return Collections.unmodifiableList(bids);
    }
    
    public List<PriceLevel> getAsks() {
        return Collections.unmodifiableList(asks);
    }
    
    /**
     * Gets the best bid price level (highest buy price).
     * @return The best bid price level, or null if no bids exist
     */
    public PriceLevel getBestBid() {
        return bids.isEmpty() ? null : bids.get(0);
    }
    
    /**
     * Gets the best ask price level (lowest sell price).
     * @return The best ask price level, or null if no asks exist
     */
    public PriceLevel getBestAsk() {
        return asks.isEmpty() ? null : asks.get(0);
    }
    
    /**
     * Represents a single price level in the order book with its total quantity.
     */
    public static class PriceLevel {
        private final BigDecimal price;
        private final BigInteger quantity;
        private final int orderCount;
        
        public PriceLevel(BigDecimal price, BigInteger quantity, int orderCount) {
            this.price = price;
            this.quantity = quantity;
            this.orderCount = orderCount;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public BigInteger getQuantity() {
            return quantity;
        }
        
        public int getOrderCount() {
            return orderCount;
        }
        
        @Override
        public String toString() {
            return String.format("[Price: %s, Quantity: %s, Orders: %d]", 
                    price.toPlainString(), quantity.toString(), orderCount);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Market Depth for ").append(symbol).append(":\n");
        
        sb.append("Asks (Sell Orders):\n");
        for (PriceLevel level : asks) {
            sb.append(level).append("\n");
        }
        
        sb.append("Bids (Buy Orders):\n");
        for (PriceLevel level : bids) {
            sb.append(level).append("\n");
        }
        
        return sb.toString();
    }
}