package com.example.orderentry;

import com.example.models.Order;
import com.example.models.Trade;
import java.util.UUID;

/**
 * Callback interface for order entry events.
 * Implementations will receive notifications about order lifecycle events.
 */
public interface OrderEntryCallback {
    
    /**
     * Called when an order is accepted by the system.
     * 
     * @param orderId The ID of the accepted order
     * @param clientOrderId The client-assigned order ID (if provided)
     */
    void onOrderAccepted(UUID orderId, String clientOrderId);
    
    /**
     * Called when an order is rejected by the system.
     * 
     * @param clientOrderId The client-assigned order ID (if provided)
     * @param reason The reason for rejection
     */
    void onOrderRejected(String clientOrderId, String reason);
    
    /**
     * Called when an order is filled (partially or completely).
     * 
     * @param orderId The ID of the filled order
     * @param trade The trade that resulted from the fill
     */
    void onOrderFilled(UUID orderId, Trade trade);
    
    /**
     * Called when an order is canceled.
     * 
     * @param orderId The ID of the canceled order
     */
    void onOrderCanceled(UUID orderId);
    
    /**
     * Called when an order modification is accepted.
     * 
     * @param orderId The ID of the modified order
     */
    void onOrderModified(UUID orderId);
    
    /**
     * Called when an order modification is rejected.
     * 
     * @param orderId The ID of the order
     * @param reason The reason for rejection
     */
    void onOrderModificationRejected(UUID orderId, String reason);
}