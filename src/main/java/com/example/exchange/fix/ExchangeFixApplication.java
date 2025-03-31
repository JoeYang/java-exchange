package com.example.exchange.fix;

import com.example.models.Order;
import com.example.models.Trade;
import com.example.orderentry.OrderEntryCallback;
import com.example.orderentry.fix.FixOrderEntryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExchangeFixApplication extends MessageCracker implements Application {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeFixApplication.class);
    private final FixOrderEntryAdapter fixOrderEntryAdapter;
    private final Map<String, SessionID> clientSessionMap = new HashMap<>();
    
    public ExchangeFixApplication(FixOrderEntryAdapter fixOrderEntryAdapter) {
        this.fixOrderEntryAdapter = fixOrderEntryAdapter;
        this.fixOrderEntryAdapter.registerCallback(new FixOrderCallback());
    }
    
    @Override
    public void onCreate(SessionID sessionId) {
        logger.info("Session created: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        logger.info("Logon from session: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        logger.info("Logout from session: {}", sessionId);
        // Remove any client mappings for this session
        clientSessionMap.entrySet().removeIf(entry -> entry.getValue().equals(sessionId));
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // Called before admin message is sent out
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // Called when admin message is received
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // Called before app message is sent out
        logger.debug("Sending message: {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // Called when app message is received
        logger.debug("Received message: {}", message);
        crack(message, sessionId);
    }
    
@quickfix.MessageCracker.Handler
    public void onNewOrderSingle(NewOrderSingle message, SessionID sessionId) throws FieldNotFound {
        logger.info("Received NewOrderSingle: {}", message);
        
        // Extract fields from FIX message
        String clientOrderId = message.getClOrdID().getValue();
        String symbol = message.getSymbol().getValue();
        char side = message.getSide().getValue();
        char orderType = message.getOrdType().getValue();
        double price = 0.0;
        if (orderType == OrdType.LIMIT) {
            price = message.getPrice().getValue();
        }
        double quantity = message.getOrderQty().getValue();
        char timeInForce = TimeInForce.DAY; // Default
        if (message.isSetTimeInForce()) {
            timeInForce = message.getTimeInForce().getValue();
        }
        
        // Store client session mapping
        clientSessionMap.put(clientOrderId, sessionId);
        
        // Convert to map for processing
        Map<Integer, String> fixMap = new HashMap<>();
        fixMap.put(11, clientOrderId); // ClOrdID
        fixMap.put(55, symbol); // Symbol
        fixMap.put(54, String.valueOf(side)); // Side
        fixMap.put(40, String.valueOf(orderType)); // OrdType
        fixMap.put(44, String.valueOf(price)); // Price
        fixMap.put(38, String.valueOf(quantity)); // OrderQty
        fixMap.put(59, String.valueOf(timeInForce)); // TimeInForce
        
        // Process the order
        UUID orderId = fixOrderEntryAdapter.processNewOrderSingle(fixMap);
        
        // If order was rejected, send rejection message
        if (orderId == null) {
            sendOrderReject(clientOrderId, "Order rejected", sessionId);
        }
    }
    
@quickfix.MessageCracker.Handler
    public void onOrderCancelRequest(OrderCancelRequest message, SessionID sessionId) throws FieldNotFound {
        logger.info("Received OrderCancelRequest: {}", message);
        
        // Extract fields
        String clientOrderId = message.getClOrdID().getValue();
        String origClientOrderId = message.getOrigClOrdID().getValue();
        
        // Store client session mapping
        clientSessionMap.put(clientOrderId, sessionId);
        
        // Cancel the order
        boolean success = fixOrderEntryAdapter.cancelOrder(null, origClientOrderId);
        
        // If cancellation failed, send rejection
        if (!success) {
            sendCancelReject(clientOrderId, origClientOrderId, "Cancel rejected", sessionId);
        }
    }
    
    private void sendOrderReject(String clientOrderId, String reason, SessionID sessionId) {
        try {
            ExecutionReport report = new ExecutionReport(
                    new OrderID("NONE"),
                    new ExecID(UUID.randomUUID().toString()),
                    new ExecType(ExecType.REJECTED),
                    new OrdStatus(OrdStatus.REJECTED),
                    new Side(Side.BUY), // Placeholder
                    new LeavesQty(0),
                    new CumQty(0),
                    new AvgPx(0));
            
            report.set(new ClOrdID(clientOrderId));
            report.set(new Text(reason));
            
            Session.sendToTarget(report, sessionId);
        } catch (Exception e) {
            logger.error("Error sending order reject", e);
        }
    }
    
    private void sendCancelReject(String clientOrderId, String origClientOrderId, String reason, SessionID sessionId) {
        try {
            quickfix.fix44.OrderCancelReject reject = new quickfix.fix44.OrderCancelReject(
                    new OrderID("NONE"),
                    new ClOrdID(clientOrderId),
                    new OrigClOrdID(origClientOrderId),
                    new OrdStatus(OrdStatus.REJECTED),
                    new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST));
            
            reject.set(new Text(reason));
            
            Session.sendToTarget(reject, sessionId);
        } catch (Exception e) {
            logger.error("Error sending cancel reject", e);
        }
    }
    
    /**
     * Callback implementation for order events.
     */
    private class FixOrderCallback implements OrderEntryCallback {
        
        @Override
        public void onOrderAccepted(UUID orderId, String clientOrderId) {
            logger.info("Order accepted: {}, clientOrderId: {}", orderId, clientOrderId);
            
            // Find the session for this client order ID
            SessionID sessionId = clientSessionMap.get(clientOrderId);
            if (sessionId != null) {
                try {
                    sendExecutionReport(orderId, clientOrderId, ExecType.NEW, OrdStatus.NEW, 
                            BigDecimal.ZERO, BigInteger.ZERO, sessionId);
                } catch (Exception e) {
                    logger.error("Error sending execution report for accepted order", e);
                }
            } else {
                logger.warn("No session found for clientOrderId: {}", clientOrderId);
            }
        }

        @Override
        public void onOrderRejected(String clientOrderId, String reason) {
            logger.info("Order rejected: {}, reason: {}", clientOrderId, reason);
            
            // Find the session for this client order ID
            SessionID sessionId = clientSessionMap.get(clientOrderId);
            if (sessionId != null) {
                sendOrderReject(clientOrderId, reason, sessionId);
            } else {
                logger.warn("No session found for clientOrderId: {}", clientOrderId);
            }
        }

        @Override
        public void onOrderFilled(UUID orderId, Trade trade) {
            logger.info("Order filled: {}, trade: {}", orderId, trade);
            
            // Get the order
            Order order = fixOrderEntryAdapter.getOrder(orderId);
            if (order == null) {
                logger.error("Order not found for fill notification: {}", orderId);
                return;
            }
            
            String clientOrderId = order.getClientOrderId();
            if (clientOrderId == null) {
                logger.error("No client order ID for order: {}", orderId);
                return;
            }
            
            // Find the session for this client order ID
            SessionID sessionId = clientSessionMap.get(clientOrderId);
            if (sessionId != null) {
                try {
                    // Determine execution type and status
                    char execType = ExecType.TRADE;
                    char ordStatus = order.getFilledQuantity().equals(order.getQuantity()) ? 
                            OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
                    
                    sendExecutionReport(orderId, clientOrderId, execType, ordStatus, 
                            trade.getPrice(), trade.getQuantity(), sessionId);
                } catch (Exception e) {
                    logger.error("Error sending execution report for filled order", e);
                }
            } else {
                logger.warn("No session found for clientOrderId: {}", clientOrderId);
            }
        }

        @Override
        public void onOrderCanceled(UUID orderId) {
            logger.info("Order canceled: {}", orderId);
            
            // Get the order
            Order order = fixOrderEntryAdapter.getOrder(orderId);
            if (order == null) {
                logger.error("Order not found for cancel notification: {}", orderId);
                return;
            }
            
            String clientOrderId = order.getClientOrderId();
            if (clientOrderId == null) {
                logger.error("No client order ID for order: {}", orderId);
                return;
            }
            
            // Find the session for this client order ID
            SessionID sessionId = clientSessionMap.get(clientOrderId);
            if (sessionId != null) {
                try {
                    sendExecutionReport(orderId, clientOrderId, ExecType.CANCELED, OrdStatus.CANCELED, 
                            BigDecimal.ZERO, BigInteger.ZERO, sessionId);
                } catch (Exception e) {
                    logger.error("Error sending execution report for canceled order", e);
                }
            } else {
                logger.warn("No session found for clientOrderId: {}", clientOrderId);
            }
        }

        @Override
        public void onOrderModified(UUID orderId) {
            logger.info("Order modified: {}", orderId);
            
            // Get the order
            Order order = fixOrderEntryAdapter.getOrder(orderId);
            if (order == null) {
                logger.error("Order not found for modification notification: {}", orderId);
                return;
            }
            
            String clientOrderId = order.getClientOrderId();
            if (clientOrderId == null) {
                logger.error("No client order ID for order: {}", orderId);
                return;
            }
            
            // Find the session for this client order ID
            SessionID sessionId = clientSessionMap.get(clientOrderId);
            if (sessionId != null) {
                try {
                    char ordStatus = order.getFilledQuantity().equals(BigInteger.ZERO) ? OrdStatus.NEW : 
                        order.getFilledQuantity().equals(order.getQuantity()) ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
                    
                    sendExecutionReport(orderId, clientOrderId, ExecType.REPLACED, ordStatus, 
                            BigDecimal.ZERO, BigInteger.ZERO, sessionId);
                } catch (Exception e) {
                    logger.error("Error sending execution report for modified order", e);
                }
            } else {
                logger.warn("No session found for clientOrderId: {}", clientOrderId);
            }
        }

        @Override
        public void onOrderModificationRejected(UUID orderId, String reason) {
            logger.info("Order modification rejected: {}, reason: {}", orderId, reason);
            
            // Get the order
            Order order = fixOrderEntryAdapter.getOrder(orderId);
            if (order == null) {
                logger.error("Order not found for modification rejection: {}", orderId);
                return;
            }
            
            String clientOrderId = order.getClientOrderId();
            if (clientOrderId == null) {
                logger.error("No client order ID for order: {}", orderId);
                return;
            }
            
            // Find the session for this client order ID
            SessionID sessionId = clientSessionMap.get(clientOrderId);
            if (sessionId != null) {
                try {
                    ExecutionReport report = new ExecutionReport(
                            new OrderID(orderId.toString()),
                            new ExecID(UUID.randomUUID().toString()),
                            new ExecType(ExecType.REJECTED),
                            new OrdStatus(order.getFilledQuantity().equals(BigInteger.ZERO) ? OrdStatus.NEW : 
                                order.getFilledQuantity().equals(order.getQuantity()) ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED),
                            new Side(order.getSide() == com.example.models.OrderSide.BUY ? Side.BUY : Side.SELL),
                            new LeavesQty(order.getQuantity().subtract(order.getFilledQuantity()).doubleValue()),
                            new CumQty(order.getFilledQuantity().doubleValue()),
                            new AvgPx(0));
                    
                    report.set(new ClOrdID(clientOrderId));
                    report.set(new Text(reason));
                    
                    Session.sendToTarget(report, sessionId);
                } catch (Exception e) {
                    logger.error("Error sending execution report for modification rejection", e);
                }
            } else {
                logger.warn("No session found for clientOrderId: {}", clientOrderId);
            }
        }
        
        private void sendExecutionReport(UUID orderId, String clientOrderId, char execType, char ordStatus, 
                                        BigDecimal lastPrice, BigInteger lastQty, SessionID sessionId) throws SessionNotFound {
            // Get the order
            Order order = fixOrderEntryAdapter.getOrder(orderId);
            if (order == null) {
                logger.error("Order not found for execution report: {}", orderId);
                return;
            }
            
            ExecutionReport report = new ExecutionReport(
                    new OrderID(orderId.toString()),
                    new ExecID(UUID.randomUUID().toString()),
                    new ExecType(execType),
                    new OrdStatus(ordStatus),
                    new Side(order.getSide() == com.example.models.OrderSide.BUY ? Side.BUY : Side.SELL),
                    new LeavesQty(order.getQuantity().subtract(order.getFilledQuantity()).doubleValue()),
                    new CumQty(order.getFilledQuantity().doubleValue()),
                    new AvgPx(0));
            
            report.set(new ClOrdID(clientOrderId));
            report.set(new Symbol(order.getSymbol()));
            report.set(new OrderQty(order.getQuantity().doubleValue()));
            
            if (order.getPrice() != null) {
                report.set(new Price(order.getPrice().doubleValue()));
            }
            
            if (lastQty.compareTo(BigInteger.ZERO) > 0) {
                report.set(new LastQty(lastQty.doubleValue()));
                report.set(new LastPx(lastPrice.doubleValue()));
            }
            
            Session.sendToTarget(report, sessionId);
        }
    }
}