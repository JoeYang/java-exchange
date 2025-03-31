package com.example.orderentry;

import com.example.orderbook.OrderBook;
import com.example.orderentry.binary.BinaryOrderEntryAdapter;
import com.example.orderentry.fix.FixOrderEntryAdapter;

/**
 * Factory for creating order entry adapters.
 */
public class OrderEntryAdapterFactory {
    
    /**
     * Protocol types supported by the factory.
     */
    public enum ProtocolType {
        FIX,
        BINARY
    }
    
    /**
     * Creates an order entry adapter for the specified protocol.
     * 
     * @param type The protocol type
     * @param orderBook The order book to use
     * @return The order entry adapter
     */
    public static OrderEntryHandler createAdapter(ProtocolType type, OrderBook orderBook) {
        switch (type) {
            case FIX:
                return new FixOrderEntryAdapter(orderBook);
            case BINARY:
                return new BinaryOrderEntryAdapter(orderBook);
            default:
                throw new IllegalArgumentException("Unsupported protocol type: " + type);
        }
    }
}