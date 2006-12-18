package com.limegroup.gnutella;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.ExecutorsHelper;

/**
 * Dispatches messages to the MessageRouter.
 */
class MessageDispatcher {
    
    private static final MessageDispatcher INSTANCE = new MessageDispatcher();
    private MessageDispatcher() {}
    public static MessageDispatcher instance() { return INSTANCE; }
    
    private final ExecutorService DISPATCH = ExecutorsHelper.newProcessingQueue("MessageDispatch");
    
    /**
     * Dispatches a UDP message.
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new UDPDispatch(m, addr));
    }
    
    /**
     * Dispatches a Multicast message.
     */
    public void dispatchMulticast(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new MulticastDispatch(m, addr));
    }
    
    /**
     * Dispatches a TCP message.
     */
    public void dispatchTCP(Message m, ManagedConnection conn) {
        DISPATCH.execute(new TCPDispatch(m, conn));
    }
    
    
    private static class UDPDispatch implements Runnable {
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();
        private final Message m;
        private final InetSocketAddress addr;
        
        UDPDispatch(Message m, InetSocketAddress addr) {
            this.m = m; this.addr = addr;
        }
        
        public void run() {
            ROUTER.handleUDPMessage(m, addr);
        }
    }
    
    private static class MulticastDispatch implements Runnable {
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();
        private final Message m;
        private final InetSocketAddress addr;
        
        MulticastDispatch(Message m, InetSocketAddress addr) {
            this.m = m; this.addr = addr;
        }

        public void run() {
            ROUTER.handleMulticastMessage(m, addr);
        }
    }
    
    private static class TCPDispatch implements Runnable {
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();
        private final Message m;
        private final ManagedConnection conn;

        TCPDispatch(Message m, ManagedConnection conn) {
            this.m = m; this.conn = conn;
        }
        
        public void run() {
            ROUTER.handleMessage(m, conn);
        }
    }
}