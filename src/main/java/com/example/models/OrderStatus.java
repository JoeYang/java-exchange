package com.example.models;

/**
 * Represents the status of an order.
 */
public enum OrderStatus {
    NEW,        // Order has been accepted but not yet processed
    PARTIALLY_FILLED,  // Order has been partially filled
    FILLED,     // Order has been completely filled
    CANCELED,   // Order has been canceled
    REJECTED    // Order has been rejected
}