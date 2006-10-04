package com.limegroup.gnutella;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * Dispatches messages to the MessageRouter.
 */
public class MessageDispatcher {
    
    private final ProcessingQueue DISPATCH = new ProcessingQueue("MessageDispatch");
    
    private final MessageRouter messageRouter;
    
    public MessageDispatcher(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }
    
    public ProcessingQueue getProcessingQueue() {
        return DISPATCH;
    }
    
    /**
     * Dispatches a UDP message.
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.add(new UDPDispatch(messageRouter, m, addr));
    }
    
    /**
     * Dispatches a Multicast message.
     */
    public void dispatchMulticast(Message m, InetSocketAddress addr) {
        DISPATCH.add(new MulticastDispatch(messageRouter, m, addr));
    }
    
    /**
     * Dispatches a TCP message.
     */
    public void dispatchTCP(Message m, ManagedConnection conn) {
        DISPATCH.add(new TCPDispatch(messageRouter, m, conn));
    }
    
    
    private static class UDPDispatch implements Runnable {
        private final MessageRouter messageRouter;
        private final Message m;
        private final InetSocketAddress addr;
        
        UDPDispatch(MessageRouter messageRouter, Message m, InetSocketAddress addr) {
            this.messageRouter = messageRouter;
            this.m = m; this.addr = addr;
        }
        
        public void run() {
            messageRouter.handleUDPMessage(m, addr);
        }
    }
    
    private static class MulticastDispatch implements Runnable {
        private final MessageRouter messageRouter;
        private final Message m;
        private final InetSocketAddress addr;
        
        MulticastDispatch(MessageRouter messageRouter, Message m, InetSocketAddress addr) {
            this.messageRouter = messageRouter;
            this.m = m; this.addr = addr;
        }

        public void run() {
            messageRouter.handleMulticastMessage(m, addr);
        }
    }
    
    private static class TCPDispatch implements Runnable {
        private final MessageRouter messageRouter;
        private final Message m;
        private final ManagedConnection conn;

        TCPDispatch(MessageRouter messageRouter, Message m, ManagedConnection conn) {
            this.messageRouter = messageRouter;
            this.m = m; this.conn = conn;
        }
        
        public void run() {
            messageRouter.handleMessage(m, conn);
        }
    }
}