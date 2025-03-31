package com.example.orderbook;

import com.example.models.Order;
import com.example.models.Trade;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

/**
 * Interface defining the essential operations for an order book.
 * An order book maintains buy and sell orders for a specific trading symbol
 * and handles the matching of orders to create trades.
 */
public interface OrderBook {
    
    /**
     * Adds a new order to the order book.
     * 
     * @param order The order to be added
     * @return True if the order was successfully added, false otherwise
     */
    boolean addOrder(Order order);
    
    /**
     * Cancels an existing order in the order book.
     * 
     * @param orderId The ID of the order to cancel
     * @return True if the order was successfully canceled, false if the order doesn't exist or can't be canceled
     */
    boolean cancelOrder(UUID orderId);
    
    /**
     * Modifies an existing order in the order book.
     * 
     * @param orderId The ID of the order to modify
     * @param newPrice The new price for the order (null if unchanged)
     * @param newQuantity The new quantity for the order (null if unchanged)
     * @return True if the order was successfully modified, false otherwise
     */
    boolean modifyOrder(UUID orderId, BigDecimal newPrice, BigInteger newQuantity);
    
    /**
     * Gets the best bid price (highest buy price) in the order book.
     * 
     * @return The best bid price, or null if no buy orders exist
     */
    BigDecimal getBestBidPrice();
    
    /**
     * Gets the best ask price (lowest sell price) in the order book.
     * 
     * @return The best ask price, or null if no sell orders exist
     */
    BigDecimal getBestAskPrice();
    
    /**
     * Gets the total quantity available at a specific price level.
     * 
     * @param price The price level to check
     * @param isBid True for bid (buy) side, false for ask (sell) side
     * @return The total quantity available at the specified price level
     */
    BigInteger getQuantityAtPriceLevel(BigDecimal price, boolean isBid);
    
    /**
     * Gets all orders in the order book.
     * 
     * @return A list of all orders in the order book
     */
    List<Order> getAllOrders();
    
    /**
     * Gets a specific order by its ID.
     * 
     * @param orderId The ID of the order to retrieve
     * @return The order if found, null otherwise
     */
    Order getOrder(UUID orderId);
    
    /**
     * Gets the trading symbol this order book is for.
     * 
     * @return The trading symbol
     */
    String getSymbol();
    
    /**
     * Gets the recent trades that have occurred in this order book.
     * 
     * @param limit The maximum number of trades to return
     * @return A list of recent trades, ordered from most to least recent
     */
    List<Trade> getRecentTrades(int limit);
    
    /**
     * Matches orders in the order book and creates trades.
     * This is typically called after adding or modifying an order.
     * 
     * @return A list of trades that were created during the matching process
     */
    List<Trade> matchOrders();
    
    /**
     * Gets the current market depth up to a specified number of price levels.
     * 
     * @param levels The number of price levels to include
     * @return A representation of the market depth
     */
    MarketDepth getMarketDepth(int levels);
    
    /**
     * Registers an event listener to receive notifications about order book events.
     * 
     * @param listener The listener to register
     * @return True if the listener was successfully registered, false otherwise
     */
    boolean registerListener(OrderBookEventListener listener);
    
    /**
     * Unregisters an event listener to stop receiving notifications about order book events.
     * 
     * @param listener The listener to unregister
     * @return True if the listener was successfully unregistered, false otherwise
     */
    boolean unregisterListener(OrderBookEventListener listener);
}