package com.example.orderentry;

import com.example.models.Order;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

/**
 * Interface for order entry handlers.
 */
public interface OrderEntryHandler {
    
    /**
     * Submits a new order.
     * 
     * @param order The order to submit
     * @return The order ID if successful, null otherwise
     */
    UUID submitOrder(Order order);
    
    /**
     * Cancels an existing order.
     * 
     * @param orderId The order ID
     * @param clientOrderId The client order ID (optional)
     * @return true if successful, false otherwise
     */
    boolean cancelOrder(UUID orderId, String clientOrderId);
    
    /**
     * Modifies an existing order.
     * 
     * @param orderId The order ID
     * @param clientOrderId The client order ID (optional)
     * @param newPrice The new price
     * @param newQuantity The new quantity
     * @return true if successful, false otherwise
     */
    boolean modifyOrder(UUID orderId, String clientOrderId, BigDecimal newPrice, BigInteger newQuantity);
    
    /**
     * Gets the status of an order.
     * 
     * @param orderId The order ID
     * @return The order status response
     */
    OrderStatusResponse getOrderStatus(UUID orderId);
    
    /**
     * Gets an order by its ID.
     * 
     * @param orderId The order ID
     * @return The order if found, null otherwise
     */
    Order getOrder(UUID orderId);
    
    /**
     * Registers a callback for order events.
     * 
     * @param callback The callback to register
     * @return true if successful, false otherwise
     */
    boolean registerCallback(OrderEntryCallback callback);
    
    /**
     * Unregisters a callback for order events.
     * 
     * @param callback The callback to unregister
     * @return true if successful, false otherwise
     */
    boolean unregisterCallback(OrderEntryCallback callback);
}