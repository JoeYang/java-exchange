package com.example.orderbook;

import com.example.models.Order;
import com.example.models.OrderSide;
import com.example.models.OrderType;
import com.example.models.TimeInForce;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
public class SimpleOrderBookBenchmark {

    private SimpleOrderBook orderBook;
    private Random random;
    private static final String SYMBOL = "BTC/USD";
    
    @Setup
    public void setup() {
        orderBook = new SimpleOrderBook(SYMBOL);
        random = new Random(42); // Fixed seed for reproducibility
    }
    
    @Benchmark
    public void addOrder(Blackhole blackhole) {
        Order order = createRandomOrder();
        boolean result = orderBook.addOrder(order);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void addAndMatchOrders(Blackhole blackhole) {
        // Add a buy order
        Order buyOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.BUY,
            new BigDecimal("10000.00").add(new BigDecimal(random.nextInt(1000))),
            new BigInteger(String.valueOf(random.nextInt(100) + 1)),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(buyOrder);
        
        // Add a sell order that might match
        Order sellOrder = new Order(
            SYMBOL,
            OrderType.LIMIT,
            OrderSide.SELL,
            new BigDecimal("9900.00").add(new BigDecimal(random.nextInt(300))),
            new BigInteger(String.valueOf(random.nextInt(100) + 1)),
            TimeInForce.GTC,
            null
        );
        orderBook.addOrder(sellOrder);
        
        blackhole.consume(orderBook.getRecentTrades(10));
    }
    
    @Benchmark
    public void getMarketDepth(Blackhole blackhole) {
        MarketDepth depth = orderBook.getMarketDepth(10);
        blackhole.consume(depth);
    }
    
    private Order createRandomOrder() {
        OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
        BigDecimal basePrice = side == OrderSide.BUY ? new BigDecimal("10000.00") : new BigDecimal("10100.00");
        BigDecimal price = basePrice.add(new BigDecimal(random.nextInt(1000)));
        
        return new Order(
            SYMBOL,
            OrderType.LIMIT,
            side,
            price,
            new BigInteger(String.valueOf(random.nextInt(100) + 1)),
            TimeInForce.GTC,
            null
        );
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SimpleOrderBookBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}