package com.example.orderentry.binary;

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
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Binary protocol adapter for order entry.
 * This class implements a custom binary protocol for high-performance order entry.
 */
public class BinaryOrderEntryAdapter implements OrderEntryHandler {
    
    private final OrderBook orderBook;
    private final List<OrderEntryCallback> callbacks;
    private final Map<String, UUID> clientOrderIdMap;
    
    public BinaryOrderEntryAdapter(OrderBook orderBook) {
        this.orderBook = orderBook;
        this.callbacks = new CopyOnWriteArrayList<>();
        this.clientOrderIdMap = new ConcurrentHashMap<>();
    }
    
    @Override
    public UUID submitOrder(Order order) {
        // Implementation similar to FIX adapter
        if (order.getClientOrderId() != null) {
            clientOrderIdMap.put(order.getClientOrderId(), order.getId());
        }
        
        boolean success = orderBook.addOrder(order);
        
        if (success) {
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderAccepted(order.getId(), order.getClientOrderId());
            }
            return order.getId();
        } else {
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderRejected(order.getClientOrderId(), "Order rejected by order book");
            }
            return null;
        }
    }
    
    // Other interface methods implemented similarly to FIX adapter
    
    @Override
    public boolean cancelOrder(UUID orderId, String clientOrderId) {
        if (orderId == null && clientOrderId != null) {
            orderId = clientOrderIdMap.get(clientOrderId);
            if (orderId == null) {
                return false;
            }
        }
        
        boolean success = orderBook.cancelOrder(orderId);
        
        if (success) {
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderCanceled(orderId);
            }
        }
        
        return success;
    }
    
    @Override
    public boolean modifyOrder(UUID orderId, String clientOrderId, BigDecimal newPrice, BigInteger newQuantity) {
        if (orderId == null && clientOrderId != null) {
            orderId = clientOrderIdMap.get(clientOrderId);
            if (orderId == null) {
                return false;
            }
        }
        
        boolean success = orderBook.modifyOrder(orderId, newPrice, newQuantity);
        
        if (success) {
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderModified(orderId);
            }
        } else {
            for (OrderEntryCallback callback : callbacks) {
                callback.onOrderModificationRejected(orderId, "Order modification rejected");
            }
        }
        
        return success;
    }

    @Override
    public Order getOrder(UUID orderId) {
        return orderBook.getOrder(orderId);
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
    public boolean registerCallback(OrderEntryCallback callback) {
        return callbacks.add(callback);
    }
    
    @Override
    public boolean unregisterCallback(OrderEntryCallback callback) {
        return callbacks.remove(callback);
    }
    
    /**
     * Processes a binary new order message.
     * 
     * @param buffer The binary message buffer
     * @return The order ID if successful, null otherwise
     */
    public UUID processNewOrderMessage(ByteBuffer buffer) {
        // Example binary protocol format:
        // Bytes 0-1: Message type (1 = new order)
        // Bytes 2-17: Client order ID (as string)
        // Bytes 18-33: Symbol (as string)
        // Byte 34: Side (1 = buy, 2 = sell)
        // Byte 35: Order type (1 = market, 2 = limit)
        // Bytes 36-43: Price (as long, scaled by 10^8)
        // Bytes 44-51: Quantity (as long)
        // Byte 52: Time in force (0 = day, 1 = GTC, 2 = IOC, 3 = FOK)
        
        byte[] clientOrderIdBytes = new byte[16];
        buffer.position(2);
        buffer.get(clientOrderIdBytes);
        String clientOrderId = new String(clientOrderIdBytes).trim();
        
        byte[] symbolBytes = new byte[16];
        buffer.get(symbolBytes);
        String symbol = new String(symbolBytes).trim();
        
        byte side = buffer.get();
        byte orderType = buffer.get();
        
        long priceRaw = buffer.getLong();
        BigDecimal price = BigDecimal.valueOf(priceRaw, 8); // Scale by 10^8
        
        long quantityRaw = buffer.getLong();
        BigInteger quantity = BigInteger.valueOf(quantityRaw);
        
        byte timeInForce = buffer.get();
        
        // Convert to internal types
        OrderSide orderSide = side == 1 ? OrderSide.BUY : OrderSide.SELL;
        OrderType type = orderType == 1 ? OrderType.MARKET : OrderType.LIMIT;
        TimeInForce tif = convertBinaryTimeInForce(timeInForce);
        
        // Create and submit order
        Order order = new Order(symbol, type, orderSide, price, quantity, tif, clientOrderId);
        return submitOrder(order);
    }
    
    private TimeInForce convertBinaryTimeInForce(byte tif) {
        switch (tif) {
            case 0: return TimeInForce.DAY;
            case 1: return TimeInForce.GTC;
            case 2: return TimeInForce.IOC;
            case 3: return TimeInForce.FOK;
            default: throw new IllegalArgumentException("Invalid binary time in force: " + tif);
        }
    }
    
    /**
     * Encodes an order status response into a binary message.
     * 
     * @param response The order status response
     * @return The binary encoded message
     */
    public ByteBuffer encodeOrderStatusResponse(OrderStatusResponse response) {
        // Example binary response format:
        // Bytes 0-1: Message type (101 = order status)
        // Bytes 2-17: Order ID (UUID as bytes)
        // Bytes 18-33: Client order ID (as string)
        // Byte 34: Status (1 = new, 2 = partially filled, 3 = filled, 4 = canceled, 5 = rejected)
        // Bytes 35-42: Price (as long, scaled by 10^8)
        // Bytes 43-50: Quantity (as long)
        // Bytes 51-58: Filled quantity (as long)
        // Bytes 59-74: Symbol (as string)
        // Bytes 75-138: Message (as string, up to 64 chars)
        
        ByteBuffer buffer = ByteBuffer.allocate(139);
        
        // Message type
        buffer.putShort((short) 101);
        
        // Order ID
        buffer.putLong(response.getOrderId().getMostSignificantBits());
        buffer.putLong(response.getOrderId().getLeastSignificantBits());
        
        // Client order ID
        byte[] clientOrderIdBytes = new byte[16];
        if (response.getClientOrderId() != null) {
            byte[] sourceBytes = response.getClientOrderId().getBytes();
            System.arraycopy(sourceBytes, 0, clientOrderIdBytes, 0, 
                    Math.min(sourceBytes.length, clientOrderIdBytes.length));
        }
        buffer.put(clientOrderIdBytes);
        
        // Status
        buffer.put(convertStatusToBinary(response.getStatus()));
        
        // Price
        long priceRaw = response.getPrice().scaleByPowerOfTen(8).longValue();
        buffer.putLong(priceRaw);
        
        // Quantity
        buffer.putLong(response.getQuantity().longValue());
        
        // Filled quantity
        buffer.putLong(response.getFilledQuantity().longValue());
        
        // Symbol
        byte[] symbolBytes = new byte[16];
        if (response.getSymbol() != null) {
            byte[] sourceBytes = response.getSymbol().getBytes();
            System.arraycopy(sourceBytes, 0, symbolBytes, 0, 
                    Math.min(sourceBytes.length, symbolBytes.length));
        }
        buffer.put(symbolBytes);
        
        // Message
        byte[] messageBytes = new byte[64];
        if (response.getMessage() != null) {
            byte[] sourceBytes = response.getMessage().getBytes();
            System.arraycopy(sourceBytes, 0, messageBytes, 0, 
                    Math.min(sourceBytes.length, messageBytes.length));
        }
        buffer.put(messageBytes);
        
        buffer.flip();
        return buffer;
    }
    
    private byte convertStatusToBinary(com.example.models.OrderStatus status) {
        switch (status) {
            case NEW: return 1;
            case PARTIALLY_FILLED: return 2;
            case FILLED: return 3;
            case CANCELED: return 4;
            case REJECTED: return 5;
            default: return 0;
        }
    }
}