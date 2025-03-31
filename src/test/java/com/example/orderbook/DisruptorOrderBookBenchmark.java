package com.example.orderbook;

import com.example.models.Order;
import com.example.models.OrderSide;
import com.example.models.OrderType;
import com.example.models.TimeInForce;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
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
public class DisruptorOrderBookBenchmark {

    private SimpleOrderBook orderBook;
    private Random random;
    private static final String SYMBOL = "BTC/USD";
    
    // Disruptor components
    private Disruptor<OrderEvent> disruptor;
    private RingBuffer<OrderEvent> ringBuffer;
    
    @Setup
    public void setup() {
        orderBook = new SimpleOrderBook(SYMBOL);
        random = new Random(42); // Fixed seed for reproducibility
        
        // Initialize the Disruptor
        int bufferSize = 1024; // Must be power of 2
        disruptor = new Disruptor<>(
                OrderEvent::new, 
                bufferSize, 
                DaemonThreadFactory.INSTANCE);
        
        // Set up the event handler that processes orders
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            orderBook.addOrder(event.getOrder());
        });
        
        // Start the Disruptor
        disruptor.start();
        
        // Get the ring buffer for publishing events
        ringBuffer = disruptor.getRingBuffer();
    }
    
    @TearDown
    public void tearDown() {
        disruptor.shutdown();
    }
    
    @Benchmark
    public void addOrderWithDisruptor(Blackhole blackhole) {
        Order order = createRandomOrder();
        
        // Publish the order to the ring buffer
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrder(order);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        blackhole.consume(order);
    }
    
    @Benchmark
    public void addOrderDirect(Blackhole blackhole) {
        Order order = createRandomOrder();
        boolean result = orderBook.addOrder(order);
        blackhole.consume(result);
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
                .include(DisruptorOrderBookBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
    
    // Event class for the Disruptor
    public static class OrderEvent {
        private Order order;
        
        public Order getOrder() {
            return order;
        }
        
        public void setOrder(Order order) {
            this.order = order;
        }
    }
}