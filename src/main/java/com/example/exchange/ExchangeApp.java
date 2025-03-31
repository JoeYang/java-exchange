package com.example.exchange;

import com.example.orderbook.OrderBook;
import com.example.orderbook.SimpleOrderBook;
import com.example.orderentry.fix.FixOrderEntryAdapter;
import com.example.exchange.fix.FixServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ExchangeApp {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeApp.class);
    private static Map<String, OrderBook> orderBooks = new HashMap<>();
    private static FixServerManager fixServerManager;
    
    public static void main(String[] args) {
        try {
            logger.info("Starting Exchange Application");
            
            // Initialize order books
            initializeOrderBooks();
            
            // Initialize FIX adapter with the first order book (for simplicity)
            String defaultSymbol = "AAPL";
            FixOrderEntryAdapter fixOrderEntryAdapter = new FixOrderEntryAdapter(orderBooks.get(defaultSymbol));
            
            // Start FIX server
            fixServerManager = new FixServerManager(fixOrderEntryAdapter);
            fixServerManager.start();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Exchange Application");
                if (fixServerManager != null) {
                    fixServerManager.stop();
                }
            }));
            
            // Keep the application running
            logger.info("Exchange Application started successfully. Press Ctrl+C to exit.");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Error starting Exchange Application", e);
            System.exit(1);
        }
    }
    
    private static void initializeOrderBooks() {
        // Create order books for different symbols
        String[] symbols = {"AAPL", "META", "MSFT", "GOOGL"};
        
        for (String symbol : symbols) {
            OrderBook orderBook = new SimpleOrderBook(symbol);
            orderBooks.put(symbol, orderBook);
            logger.info("Initialized order book for {}", symbol);
        }
    }
}