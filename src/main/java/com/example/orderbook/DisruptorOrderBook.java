package com.example.orderbook;

import com.example.models.Order;
import com.example.models.OrderSide;
import com.example.models.Trade;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class DisruptorOrderBook implements OrderBook {
    
    private final SimpleOrderBook delegateOrderBook;
    private final Disruptor<OrderCommand> disruptor;
    private final RingBuffer<OrderCommand> ringBuffer;
    private final ConcurrentHashMap<UUID, CountDownLatch> commandLatches;
    
    public DisruptorOrderBook(String symbol) {
        this.delegateOrderBook = new SimpleOrderBook(symbol);
        this.commandLatches = new ConcurrentHashMap<>();
        
        // Initialize the Disruptor
        int bufferSize = 1024; // Must be power of 2
        this.disruptor = new Disruptor<>(
                OrderCommand::new,
                bufferSize,
                DaemonThreadFactory.INSTANCE);
        
        // Set up the event handler
        this.disruptor.handleEventsWith(new OrderCommandHandler());
        
        // Start the Disruptor
        this.disruptor.start();
        
        // Get the ring buffer for publishing events
        this.ringBuffer = disruptor.getRingBuffer();
    }
    
    @Override
    public boolean addOrder(Order order) {
        return publishCommand(CommandType.ADD_ORDER, order, null, null, null);
    }
    
    @Override
    public boolean cancelOrder(UUID orderId) {
        return publishCommand(CommandType.CANCEL_ORDER, null, orderId, null, null);
    }
    
    @Override
    public boolean modifyOrder(UUID orderId, BigDecimal newPrice, BigInteger newQuantity) {
        return publishCommand(CommandType.MODIFY_ORDER, null, orderId, newPrice, newQuantity);
    }
    
    private boolean publishCommand(CommandType type, Order order, UUID orderId, 
                                  BigDecimal price, BigInteger quantity) {
        // For high-throughput scenarios, consider making this asynchronous
        // by removing the latch and returning immediately
        CountDownLatch latch = new CountDownLatch(1);
        UUID commandId = UUID.randomUUID();
        commandLatches.put(commandId, latch);
        
        long sequence = ringBuffer.next();
        try {
            OrderCommand command = ringBuffer.get(sequence);
            command.setCommandId(commandId);
            command.setType(type);
            command.setOrder(order);
            command.setOrderId(orderId);
            command.setPrice(price);
            command.setQuantity(quantity);
            command.setResult(false);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        try {
            // Consider adding a timeout here to prevent blocking indefinitely
            latch.await(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            CountDownLatch resultLatch = commandLatches.remove(commandId);
            return resultLatch != null && resultLatch.getCount() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    // Delegate methods to the underlying order book
    @Override
    public BigDecimal getBestBidPrice() {
        return delegateOrderBook.getBestBidPrice();
    }
    
    @Override
    public BigDecimal getBestAskPrice() {
        return delegateOrderBook.getBestAskPrice();
    }
    
    @Override
    public BigInteger getQuantityAtPriceLevel(BigDecimal price, boolean isBid) {
        return delegateOrderBook.getQuantityAtPriceLevel(price, isBid);
    }
    
    @Override
    public List<Order> getAllOrders() {
        return delegateOrderBook.getAllOrders();
    }
    
    @Override
    public Order getOrder(UUID orderId) {
        return delegateOrderBook.getOrder(orderId);
    }
    
    @Override
    public String getSymbol() {
        return delegateOrderBook.getSymbol();
    }
    
    @Override
    public List<Trade> getRecentTrades(int limit) {
        return delegateOrderBook.getRecentTrades(limit);
    }
    
    @Override
    public List<Trade> matchOrders() {
        return delegateOrderBook.matchOrders();
    }
    
    @Override
    public MarketDepth getMarketDepth(int levels) {
        return delegateOrderBook.getMarketDepth(levels);
    }
    
    @Override
    public boolean registerListener(OrderBookEventListener listener) {
        return delegateOrderBook.registerListener(listener);
    }
    
    @Override
    public boolean unregisterListener(OrderBookEventListener listener) {
        return delegateOrderBook.unregisterListener(listener);
    }
    
    public void shutdown() {
        disruptor.shutdown();
    }
    
    // Command types for the Disruptor
    private enum CommandType {
        ADD_ORDER,
        CANCEL_ORDER,
        MODIFY_ORDER
    }
    
    // Event class for the Disruptor
    public static class OrderCommand {
        private UUID commandId;
        private CommandType type;
        private Order order;
        private UUID orderId;
        private BigDecimal price;
        private BigInteger quantity;
        private boolean result;
        
        public UUID getCommandId() {
            return commandId;
        }
        
        public void setCommandId(UUID commandId) {
            this.commandId = commandId;
        }
        
        public CommandType getType() {
            return type;
        }
        
        public void setType(CommandType type) {
            this.type = type;
        }
        
        public Order getOrder() {
            return order;
        }
        
        public void setOrder(Order order) {
            this.order = order;
        }
        
        public UUID getOrderId() {
            return orderId;
        }
        
        public void setOrderId(UUID orderId) {
            this.orderId = orderId;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        public BigInteger getQuantity() {
            return quantity;
        }
        
        public void setQuantity(BigInteger quantity) {
            this.quantity = quantity;
        }
        
        public boolean isResult() {
            return result;
        }
        
        public void setResult(boolean result) {
            this.result = result;
        }
    }
    
    // Event handler for processing commands
    private class OrderCommandHandler implements EventHandler<OrderCommand> {
        @Override
        public void onEvent(OrderCommand command, long sequence, boolean endOfBatch) {
            boolean result = false;
            
            switch (command.getType()) {
                case ADD_ORDER:
                    result = delegateOrderBook.addOrder(command.getOrder());
                    break;
                case CANCEL_ORDER:
                    result = delegateOrderBook.cancelOrder(command.getOrderId());
                    break;
                case MODIFY_ORDER:
                    result = delegateOrderBook.modifyOrder(
                        command.getOrderId(), 
                        command.getPrice(), 
                        command.getQuantity()
                    );
                    break;
            }
            
            command.setResult(result);
            CountDownLatch latch = commandLatches.get(command.getCommandId());
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}