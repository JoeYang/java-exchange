package com.example.models;

/**
 * Represents the type of an order.
 */
public enum OrderType {
    MARKET,     // Market order - executed at the current market price
    LIMIT,      // Limit order - executed at a specified price or better
    IOC,        // Immediate-or-Cancel - must be filled immediately (at least partially) or canceled
    FOK,        // Fill-or-Kill - must be filled completely immediately or canceled entirely
    STOP_LIMIT, // Stop-Limit - becomes a limit order when the stop price is reached
    STOP_LOSS   // Stop-Loss - becomes a market order when the stop price is reached
}