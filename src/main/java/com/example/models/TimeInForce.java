package com.example.models;

/**
 * Represents the Time in Force for an order.
 */
public enum TimeInForce {
    DAY,            // Valid for the day until market close
    GTC,            // Good Till Canceled - valid until explicitly canceled
    GTD,            // Good Till Date - valid until a specified date
    IOC,            // Immediate or Cancel - execute immediately or cancel
    FOK,            // Fill or Kill - execute completely immediately or cancel
    AT_THE_OPENING, // Valid only during the market opening
    AT_THE_CLOSE    // Valid only during the market closing
}