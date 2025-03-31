package com.example.orderbook;

import com.example.models.Order;
import com.example.models.OrderSide;
import com.example.models.OrderStatus;
import com.example.models.Trade;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple implementation of the OrderBook interface.
 */
public class SimpleOrderBook implements OrderBook {
    private final String symbol;
    private final Map<UUID, Order> orders;
    private final NavigableMap<BigDecimal, List<Order>> bidOrders;
    private final NavigableMap<BigDecimal, List<Order>> askOrders;
    private final List<Trade> recentTrades;
    private final List<OrderBookEventListener> listeners;
    
    public SimpleOrderBook(String symbol) {
        this.symbol = symbol;
        this.orders = new ConcurrentHashMap<>();
        this.bidOrders = new TreeMap<>(Collections.reverseOrder()); // Highest price first
        this.askOrders = new TreeMap<>(); // Lowest price first
        this.recentTrades = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public boolean addOrder(Order order) {
        if (!order.getSymbol().equals(symbol)) {
            return false;
        }
        
        orders.put(order.getId(), order);
        
        NavigableMap<BigDecimal, List<Order>> orderMap = order.getSide() == OrderSide.BUY ? bidOrders : askOrders;
        orderMap.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        
        // Notify listeners
        for (OrderBookEventListener listener : listeners) {
            listener.onOrderAdded(order);
        }
        
        // Match orders
        List<Trade> trades = matchOrders();
        
        return true;
    }
    
    @Override
    public boolean cancelOrder(UUID orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            return false;
        }
        
        NavigableMap<BigDecimal, List<Order>> orderMap = order.getSide() == OrderSide.BUY ? bidOrders : askOrders;
        List<Order> ordersAtPrice = orderMap.get(order.getPrice());
        if (ordersAtPrice != null) {
            ordersAtPrice.remove(order);
            if (ordersAtPrice.isEmpty()) {
                orderMap.remove(order.getPrice());
            }
        }
        
        orders.remove(orderId);
        order.setStatus(OrderStatus.CANCELED);
        
        // Notify listeners
        for (OrderBookEventListener listener : listeners) {
            listener.onOrderCanceled(orderId, order);
        }
        
        return true;
    }
    
    @Override
    public boolean modifyOrder(UUID orderId, BigDecimal newPrice, BigInteger newQuantity) {
        Order order = orders.get(orderId);
        if (order == null) {
            return false;
        }
        
        BigDecimal oldPrice = order.getPrice();
        BigInteger oldQuantity = order.getQuantity();
        
        // Remove from old price level
        NavigableMap<BigDecimal, List<Order>> orderMap = order.getSide() == OrderSide.BUY ? bidOrders : askOrders;
        List<Order> ordersAtPrice = orderMap.get(oldPrice);
        if (ordersAtPrice != null) {
            ordersAtPrice.remove(order);
            if (ordersAtPrice.isEmpty()) {
                orderMap.remove(oldPrice);
            }
        }
        
        // Update order
        if (newPrice != null) {
            order.setPrice(newPrice);
        }
        if (newQuantity != null) {
            order.setQuantity(newQuantity);
        }
        
        // Add to new price level
        orderMap.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        
        // Notify listeners
        for (OrderBookEventListener listener : listeners) {
            listener.onOrderModified(order, oldPrice, oldQuantity);
        }
        
        // Match orders
        List<Trade> trades = matchOrders();
        
        return true;
    }
    
    @Override
    public BigDecimal getBestBidPrice() {
        return bidOrders.isEmpty() ? null : bidOrders.firstKey();
    }
    
    @Override
    public BigDecimal getBestAskPrice() {
        return askOrders.isEmpty() ? null : askOrders.firstKey();
    }
    
    @Override
    public BigInteger getQuantityAtPriceLevel(BigDecimal price, boolean isBid) {
        NavigableMap<BigDecimal, List<Order>> orderMap = isBid ? bidOrders : askOrders;
        List<Order> ordersAtPrice = orderMap.get(price);
        
        if (ordersAtPrice == null || ordersAtPrice.isEmpty()) {
            return BigInteger.ZERO;
        }
        
        BigInteger totalQuantity = BigInteger.ZERO;
        for (Order order : ordersAtPrice) {
            totalQuantity = totalQuantity.add(order.getQuantity().subtract(order.getFilledQuantity()));
        }
        
        return totalQuantity;
    }
    
    @Override
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    @Override
    public Order getOrder(UUID orderId) {
        return orders.get(orderId);
    }
    
    @Override
    public String getSymbol() {
        return symbol;
    }
    
    @Override
    public List<Trade> getRecentTrades(int limit) {
        int size = Math.min(limit, recentTrades.size());
        return new ArrayList<>(recentTrades.subList(recentTrades.size() - size, recentTrades.size()));
    }
    
    @Override
    public List<Trade> matchOrders() {
        List<Trade> newTrades = new ArrayList<>();
        
        while (!bidOrders.isEmpty() && !askOrders.isEmpty()) {
            BigDecimal bestBidPrice = bidOrders.firstKey();
            BigDecimal bestAskPrice = askOrders.firstKey();
            
            // If best bid is less than best ask, no match is possible
            if (bestBidPrice.compareTo(bestAskPrice) < 0) {
                break;
            }
            
            List<Order> bestBidOrders = bidOrders.get(bestBidPrice);
            List<Order> bestAskOrders = askOrders.get(bestAskPrice);
            
            Order bidOrder = bestBidOrders.get(0);
            Order askOrder = bestAskOrders.get(0);
            
            // Calculate match quantity
            BigInteger bidRemaining = bidOrder.getQuantity().subtract(bidOrder.getFilledQuantity());
            BigInteger askRemaining = askOrder.getQuantity().subtract(askOrder.getFilledQuantity());
            BigInteger matchQuantity = bidRemaining.min(askRemaining);
            
            // Create trade at ask price (taker pays)
            Trade trade = new Trade(
                bidOrder.getId(),
                askOrder.getId(),
                symbol,
                bestAskPrice,
                matchQuantity,
                BigDecimal.ZERO, // TODO: Calculate fee
                symbol
            );
            
            // Update filled quantities
            bidOrder.setFilledQuantity(bidOrder.getFilledQuantity().add(matchQuantity));
            askOrder.setFilledQuantity(askOrder.getFilledQuantity().add(matchQuantity));
            
            // Update order status
            if (bidOrder.getFilledQuantity().compareTo(bidOrder.getQuantity()) >= 0) {
                bidOrder.setStatus(OrderStatus.FILLED);
                bestBidOrders.remove(bidOrder);
                if (bestBidOrders.isEmpty()) {
                    bidOrders.remove(bestBidPrice);
                }
            } else if (bidOrder.getFilledQuantity().compareTo(BigInteger.ZERO) > 0) {
                // Order is partially filled
                bidOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }
            
            if (askOrder.getFilledQuantity().compareTo(askOrder.getQuantity()) >= 0) {
                askOrder.setStatus(OrderStatus.FILLED);
                bestAskOrders.remove(askOrder);
                if (bestAskOrders.isEmpty()) {
                    askOrders.remove(bestAskPrice);
                }
            } else if (askOrder.getFilledQuantity().compareTo(BigInteger.ZERO) > 0) {
                // Order is partially filled
                askOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }
            
            // Add trade to recent trades
            recentTrades.add(trade);
            newTrades.add(trade);
            
            // Notify listeners
            for (OrderBookEventListener listener : listeners) {
                listener.onTradeExecuted(trade);
            }
        }
        
        return newTrades;
    }
    
    @Override
    public MarketDepth getMarketDepth(int levels) {
        List<MarketDepth.PriceLevel> bids = new ArrayList<>();
        List<MarketDepth.PriceLevel> asks = new ArrayList<>();
        
        int bidCount = 0;
        for (Map.Entry<BigDecimal, List<Order>> entry : bidOrders.entrySet()) {
            if (bidCount >= levels) break;
            
            BigDecimal price = entry.getKey();
            List<Order> ordersAtPrice = entry.getValue();
            BigInteger totalQuantity = BigInteger.ZERO;
            
            for (Order order : ordersAtPrice) {
                totalQuantity = totalQuantity.add(order.getQuantity().subtract(order.getFilledQuantity()));
            }
            
            bids.add(new MarketDepth.PriceLevel(price, totalQuantity, ordersAtPrice.size()));
            bidCount++;
        }
        
        int askCount = 0;
        for (Map.Entry<BigDecimal, List<Order>> entry : askOrders.entrySet()) {
            if (askCount >= levels) break;
            
            BigDecimal price = entry.getKey();
            List<Order> ordersAtPrice = entry.getValue();
            BigInteger totalQuantity = BigInteger.ZERO;
            
            for (Order order : ordersAtPrice) {
                totalQuantity = totalQuantity.add(order.getQuantity().subtract(order.getFilledQuantity()));
            }
            
            asks.add(new MarketDepth.PriceLevel(price, totalQuantity, ordersAtPrice.size()));
            askCount++;
        }
        
        return new MarketDepth(symbol, bids, asks);
    }
    
    @Override
    public boolean registerListener(OrderBookEventListener listener) {
        return listeners.add(listener);
    }
    
    @Override
    public boolean unregisterListener(OrderBookEventListener listener) {
        return listeners.remove(listener);
    }
}