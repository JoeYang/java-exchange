package com.example.orderbook;

import com.example.models.Order;
import com.example.models.Trade;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

/**
 * Interface for listening to events that occur in an order book.
 * Components can implement this interface to be notified of order book events.
 */
public interface OrderBookEventListener {
    
    /**
     * Called when a new order is added to the order book.
     * 
     * @param order The order that was added
     */
    void onOrderAdded(Order order);
    
    /**
     * Called when an order is canceled from the order book.
     * 
     * @param orderId The ID of the order that was canceled
     * @param order The order that was canceled
     */
    void onOrderCanceled(UUID orderId, Order order);
    
    /**
     * Called when an order is modified in the order book.
     * 
     * @param order The order after modification
     * @param oldPrice The price before modification
     * @param oldQuantity The quantity before modification
     */
    void onOrderModified(Order order, BigDecimal oldPrice, BigInteger oldQuantity);
    
    /**
     * Called when a trade is executed in the order book.
     * 
     * @param trade The trade that was executed
     */
    void onTradeExecuted(Trade trade);
    
    /**
     * Called when the best bid price changes.
     * 
     * @param newBestBid The new best bid price
     * @param oldBestBid The old best bid price
     */
    void onBestBidChanged(BigDecimal newBestBid, BigDecimal oldBestBid);
    
    /**
     * Called when the best ask price changes.
     * 
     * @param newBestAsk The new best ask price
     * @param oldBestAsk The old best ask price
     */
    void onBestAskChanged(BigDecimal newBestAsk, BigDecimal oldBestAsk);
}