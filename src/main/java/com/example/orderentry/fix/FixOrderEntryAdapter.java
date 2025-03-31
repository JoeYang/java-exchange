package com.example.orderentry.fix;

import com.example.models.Order;
import com.example.models.OrderSide;
import com.example.models.OrderType;
import com.example.models.TimeInForce;
import com.example.orderbook.OrderBook;
import com.example.orderentry.OrderEntryCallback;
import com.example.orderentry.OrderEntryHandler;
import com.example.orderentry.OrderStatusResponse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * FIX protocol adapter for order entry.
 * This class would integrate with a FIX engine like QuickFIX/J.
 */
public class FixOrderEntryAdapter implements OrderEntryHandler {
    
    private final OrderBook orderBook;
    private final List<OrderEntryCallback> callbacks;
    private final Map<String, UUID> clientOrderIdMap;
    
    public FixOrderEntryAdapter(OrderBook orderBook) {
        this.orderBook = orderBook;
        this.callbacks = new CopyOnWriteArrayList<>();
        this.clientOrderIdMap = new ConcurrentHashMap<>();
    }
    
    @Override
    public UUID submitOrder(Order order) {
        // Store client order ID mapping if provided
        if (order.getClientOrderId() != null) {
            clientOrderIdMap.put(order.getClientOrderId(), order.getId());
        }
        
        boolean success = orderBook.addOrder(order);
        
        if (success) {
            // Notify callbacks
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderAccepted(order.getId(), order.getClientOrderId());
            }
            return order.getId();
        } else {
            // Notify callbacks of rejection
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderRejected(order.getClientOrderId(), "Order rejected by order book");
            }
            return null;
        }
    }
    
    @Override
    public boolean cancelOrder(UUID orderId, String clientOrderId) {
        // Resolve order ID from client order ID if needed
        if (orderId == null && clientOrderId != null) {
            orderId = clientOrderIdMap.get(clientOrderId);
            if (orderId == null) {
                return false;
            }
        }
        
        boolean success = orderBook.cancelOrder(orderId);
        
        if (success) {
            // Notify callbacks
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderCanceled(orderId);
            }
        }
        
        return success;
    }
    
    @Override
    public boolean modifyOrder(UUID orderId, String clientOrderId, BigDecimal newPrice, BigInteger newQuantity) {
        // Resolve order ID from client order ID if needed
        if (orderId == null && clientOrderId != null) {
            orderId = clientOrderIdMap.get(clientOrderId);
            if (orderId == null) {
                return false;
            }
        }
        
        boolean success = orderBook.modifyOrder(orderId, newPrice, newQuantity);
        
        if (success) {
            // Notify callbacks
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderModified(orderId);
            }
        } else {
            // Notify callbacks of rejection
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderModificationRejected(orderId, "Order modification rejected");
            }
        }
        
        return success;
    }
    
    @Override
    public OrderStatusResponse getOrderStatus(UUID orderId) {
        Order order = orderBook.getOrder(orderId);
        if (order == null) {
            return null;
        }
        
        return OrderStatusResponse.fromOrder(order, "Order found");
    }
    
    @Override
    public Order getOrder(UUID orderId) {
        return orderBook.getOrder(orderId);
    }
    
    @Override
    public boolean registerCallback(OrderEntryCallback callback) {
        return callbacks.add(callback);
    }
    
    @Override
    public boolean unregisterCallback(OrderEntryCallback callback) {
        return callbacks.remove(callback);
    }
    
    // FIX-specific methods would go here
    // These would integrate with your FIX engine
    
    /**
     * Processes a FIX new order single message.
     * 
     * @param fixMessage The FIX message as a map of tag-value pairs
     * @return The order ID if successful, null otherwise
     */
    public UUID processNewOrderSingle(Map<Integer, String> fixMessage) {
        // Extract fields from FIX message
        String clientOrderId = fixMessage.get(11); // ClOrdID
        String symbol = fixMessage.get(55); // Symbol
        String sideStr = fixMessage.get(54); // Side
        String typeStr = fixMessage.get(40); // OrdType
        String priceStr = fixMessage.get(44); // Price
        String quantityStr = fixMessage.get(38); // OrderQty
        String timeInForceStr = fixMessage.get(59); // TimeInForce
        
        // Convert to internal types
        OrderSide side = convertFixSide(sideStr);
        OrderType type = convertFixOrderType(typeStr);
        BigDecimal price = new BigDecimal(priceStr);
        BigInteger quantity = new BigInteger(quantityStr);
        TimeInForce timeInForce = convertFixTimeInForce(timeInForceStr);
        
        // Create and submit order
        Order order = new Order(symbol, type, side, price, quantity, timeInForce, clientOrderId);
        return submitOrder(order);
    }
    
    // Helper methods to convert FIX values to internal enums
    private OrderSide convertFixSide(String side) {
        if ("1".equals(side)) return OrderSide.BUY;
        if ("2".equals(side)) return OrderSide.SELL;
        throw new IllegalArgumentException("Invalid FIX side: " + side);
    }
    
    private OrderType convertFixOrderType(String type) {
        if ("1".equals(type)) return OrderType.MARKET;
        if ("2".equals(type)) return OrderType.LIMIT;
        throw new IllegalArgumentException("Invalid FIX order type: " + type);
    }
    
    private TimeInForce convertFixTimeInForce(String tif) {
        if ("0".equals(tif)) return TimeInForce.DAY;
        if ("1".equals(tif)) return TimeInForce.GTC;
        if ("3".equals(tif)) return TimeInForce.IOC;
        if ("4".equals(tif)) return TimeInForce.FOK;
        throw new IllegalArgumentException("Invalid FIX time in force: " + tif);
    }
}