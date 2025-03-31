package com.example.exchange.fix;

import com.example.orderentry.fix.FixOrderEntryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FixServerManager {
    private static final Logger logger = LoggerFactory.getLogger(FixServerManager.class);
    
    private final FixOrderEntryAdapter fixOrderEntryAdapter;
    private Acceptor acceptor;
    
    public FixServerManager(FixOrderEntryAdapter fixOrderEntryAdapter) {
        this.fixOrderEntryAdapter = fixOrderEntryAdapter;
    }
    
    public void start() throws ConfigError, FileNotFoundException {
        // Load FIX configuration
        SessionSettings settings = new SessionSettings(
                new FileInputStream("/Users/joeyang/Personal/github/java-exchange/config/quickfix-server.properties"));
        
        // Create FIX application
        Application fixApplication = new ExchangeFixApplication(fixOrderEntryAdapter);
        
        // Create message store factory
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        
        // Create log factory
        LogFactory logFactory = new FileLogFactory(settings);
        
        // Create message factory
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        // Create and start acceptor
        acceptor = new SocketAcceptor(
                fixApplication, storeFactory, settings, logFactory, messageFactory);
        
        acceptor.start();
        logger.info("FIX acceptor started");
    }
    
    public void stop() {
        if (acceptor != null) {
            acceptor.stop();
            logger.info("FIX acceptor stopped");
        }
    }
}