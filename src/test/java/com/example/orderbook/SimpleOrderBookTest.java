package com.example.orderbook;

import com.example.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleOrderBookTest {
    
    private SimpleOrderBook orderBook;
    private static final String SYMBOL = "BTC/USD";
    
    @BeforeEach
    public void setUp() {
        orderBook = new SimpleOrderBook(SYMBOL);
    }
    
    @Test
    public void testOrderBookCreation() {
        assertEquals(SYMBOL, orderBook.getSymbol());
        assertNull(orderBook.getBestBidPrice());
        assertNull(orderBook.getBestAskPrice());
        assertTrue(orderBook.getAllOrders().isEmpty());
    }
    
    @Test
    public void testAddOrder() {
        Order buyOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00"),
            new BigInteger("15"),
            TimeInForce.GTC,
            null
        );
        
        boolean result = orderBook.addOrder(buyOrder);
        
        assertTrue(result);
        assertEquals(1, orderBook.getAllOrders().size());
        assertEquals(new BigDecimal("10000.00"), orderBook.getBestBidPrice());
        assertNull(orderBook.getBestAskPrice());
    }
    
    @Test
    public void testCancelOrder() {
        Order buyOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00"),
            new BigInteger("15"),
            TimeInForce.GTC,
            null
        );
        
        orderBook.addOrder(buyOrder);
        boolean result = orderBook.cancelOrder(buyOrder.getId());
        
        assertTrue(result);
        assertEquals(0, orderBook.getAllOrders().size());
        assertNull(orderBook.getBestBidPrice());
    }
    
    @Test
    public void testModifyOrder() {
        Order buyOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00"),
            new BigInteger("15"),
            TimeInForce.GTC,
            null
        );
        
        orderBook.addOrder(buyOrder);
        boolean result = orderBook.modifyOrder(
            buyOrder.getId(),
            new BigDecimal("10500.00"),
            new BigInteger("20")
        );
        
        assertTrue(result);
        assertEquals(new BigDecimal("10500.00"), orderBook.getBestBidPrice());
        Order modifiedOrder = orderBook.getOrder(buyOrder.getId());
        assertEquals(new BigInteger("20"), modifiedOrder.getQuantity());
    }
    
    @Test
    public void testBasicOrderMatching() {
        // Add buy order
        Order buyOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00"),
            new BigInteger("15"),
            TimeInForce.DAY,
            null
        );
        orderBook.addOrder(buyOrder);
        
        // Add sell order at lower price (should match)
        Order sellOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.SELL,
            new BigDecimal("9900.00"),
            new BigInteger("10"),
            TimeInForce.DAY,
            null
        );
        orderBook.addOrder(sellOrder);
        
        // Check that orders matched
        List<Trade> recentTrades = orderBook.getRecentTrades(10);
        assertEquals(1, recentTrades.size());
        
        Trade trade = recentTrades.get(0);
        assertEquals(buyOrder.getId(), trade.getBuyOrderId());
        assertEquals(sellOrder.getId(), trade.getSellOrderId());
        assertEquals(new BigDecimal("9900.00"), trade.getPrice()); // Matched at ask price
        assertEquals(new BigInteger("10"), trade.getQuantity());
        
        // Check order status
        assertEquals(OrderStatus.PARTIALLY_FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
        
        // Check remaining quantity
        assertEquals(new BigInteger("5"), buyOrder.getQuantity().subtract(buyOrder.getFilledQuantity()));
        assertEquals(BigInteger.ZERO, sellOrder.getQuantity().subtract(sellOrder.getFilledQuantity()));
    }
    
    @Test
    public void testMultipleOrderMatching() {
        // Add buy orders
        Order buyOrder1 = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00"),
            new BigInteger("10"),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(buyOrder1);
        
        Order buyOrder2 = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("9900.00"),
            new BigInteger("20"),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(buyOrder2);
        
        // Add sell order that matches both buy orders
        Order sellOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.SELL,
            new BigDecimal("9800.00"),
            new BigInteger("30"),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(sellOrder);
        
        // Check that orders matched
        List<Trade> recentTrades = orderBook.getRecentTrades(10);
        assertEquals(2, recentTrades.size());
        
        // First trade should be with the highest bid
        Trade trade1 = recentTrades.get(0);
        assertEquals(buyOrder1.getId(), trade1.getBuyOrderId());
        assertEquals(sellOrder.getId(), trade1.getSellOrderId());
        assertEquals(new BigDecimal("9800.00"), trade1.getPrice());
        assertEquals(new BigInteger("10"), trade1.getQuantity());
        
        // Second trade should be with the second highest bid
        Trade trade2 = recentTrades.get(1);
        assertEquals(buyOrder2.getId(), trade2.getBuyOrderId());
        assertEquals(sellOrder.getId(), trade2.getSellOrderId());
        assertEquals(new BigDecimal("9800.00"), trade2.getPrice());
        assertEquals(new BigInteger("20"), trade2.getQuantity());
        
        // Check order status
        assertEquals(OrderStatus.FILLED, buyOrder1.getStatus());
        assertEquals(OrderStatus.FILLED, buyOrder2.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
        
        // Check that order book is empty
        assertNull(orderBook.getBestBidPrice());
        assertNull(orderBook.getBestAskPrice());
    }
    
    @Test
    public void testMarketDepth() {
        // Add multiple buy orders at different prices
        orderBook.addOrder(new Order(SYMBOL, OrderType.LIMIT, OrderSide.BUY, new BigDecimal("10000.00"), new BigInteger("10")));
        orderBook.addOrder(new Order(SYMBOL, OrderType.LIMIT, OrderSide.BUY, new BigDecimal("9900.00"), new BigInteger("20")));
        orderBook.addOrder(new Order(SYMBOL, OrderType.LIMIT, OrderSide.BUY, new BigDecimal("9800.00"), new BigInteger("30")));
        
        // Add multiple sell orders at different prices
        orderBook.addOrder(new Order(SYMBOL, OrderType.LIMIT, OrderSide.SELL, new BigDecimal("10100.00"), new BigInteger("15")));
        orderBook.addOrder(new Order(SYMBOL, OrderType.LIMIT, OrderSide.SELL, new BigDecimal("10200.00"), new BigInteger("25")));
        orderBook.addOrder(new Order(SYMBOL, OrderType.LIMIT, OrderSide.SELL, new BigDecimal("10300.00"), new BigInteger("35")));
        
        // Get market depth with 2 levels
        MarketDepth depth = orderBook.getMarketDepth(2);
        
        // Check bid side
        List<MarketDepth.PriceLevel> bids = depth.getBids();
        assertEquals(2, bids.size());
        assertEquals(new BigDecimal("10000.00"), bids.get(0).getPrice());
        assertEquals(new BigInteger("10"), bids.get(0).getQuantity());
        assertEquals(new BigDecimal("9900.00"), bids.get(1).getPrice());
        assertEquals(new BigInteger("20"), bids.get(1).getQuantity());
        
        // Check ask side
        List<MarketDepth.PriceLevel> asks = depth.getAsks();
        assertEquals(2, asks.size());
        assertEquals(new BigDecimal("10100.00"), asks.get(0).getPrice());
        assertEquals(new BigInteger("15"), asks.get(0).getQuantity());
        assertEquals(new BigDecimal("10200.00"), asks.get(1).getPrice());
        assertEquals(new BigInteger("25"), asks.get(1).getQuantity());
    }
    
    @Test
    public void testPartiallyFilledOrderStatus() {
        // Add buy order
        Order buyOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00"),
            new BigInteger("20"),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(buyOrder);
        
        // Add sell order with smaller quantity (should partially fill buy order)
        Order sellOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.SELL,
            new BigDecimal("9900.00"),
            new BigInteger("10"),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(sellOrder);
        
        // Check order statuses
        assertEquals(OrderStatus.PARTIALLY_FILLED, buyOrder.getStatus());
        assertEquals(OrderStatus.FILLED, sellOrder.getStatus());
        
        // Check filled quantities
        assertEquals(new BigInteger("10"), buyOrder.getFilledQuantity());
        assertEquals(new BigInteger("10"), sellOrder.getFilledQuantity());
        
        // Check remaining quantity
        assertEquals(new BigInteger("10"), buyOrder.getQuantity().subtract(buyOrder.getFilledQuantity()));
        assertEquals(BigInteger.ZERO, sellOrder.getQuantity().subtract(sellOrder.getFilledQuantity()));
    }
}