package com.limegroup.gnutella;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.messages.Message;

/**
 * Dispatches messages to the MessageRouter.
 */
@Singleton
public class MessageDispatcher {
    
    private final Executor DISPATCH;

    private final MessageRouter messageRouter;
    
    @Inject
    public MessageDispatcher(MessageRouter messageRouter, @Named("messageExecutor") Executor dispatch) {
        this.messageRouter = messageRouter;
        this.DISPATCH = dispatch;
    }
    
    /** Dispatches a runnable, to allow arbitrary runnables to be processed on the message thread. */
    public void dispatch(Runnable r) {
        DISPATCH.execute(r);
    }
    
    /**
     * Dispatches a UDP message.
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new UDPDispatch(messageRouter, m, addr));
    }
    
    /**
     * Dispatches a Multicast message.
     */
    public void dispatchMulticast(Message m, InetSocketAddress addr) {
        DISPATCH.execute(new MulticastDispatch(messageRouter, m, addr));
    }
    
    /**
     * Dispatches a TCP message.
     */
    public void dispatchTCP(Message m, ManagedConnection conn) {
        DISPATCH.execute(new TCPDispatch(messageRouter, m, conn));
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