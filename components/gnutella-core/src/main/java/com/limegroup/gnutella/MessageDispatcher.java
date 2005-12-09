pbckage com.limegroup.gnutella;

import jbva.net.InetSocketAddress;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.util.ProcessingQueue;

/**
 * Dispbtches messages to the MessageRouter.
 */
clbss MessageDispatcher {
    
    privbte static final MessageDispatcher INSTANCE = new MessageDispatcher();
    privbte MessageDispatcher() {}
    public stbtic MessageDispatcher instance() { return INSTANCE; }
    
    privbte final ProcessingQueue DISPATCH = new ProcessingQueue("MessageDispatch");
    
    /**
     * Dispbtches a UDP message.
     */
    public void dispbtchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.bdd(new UDPDispatch(m, addr));
    }
    
    /**
     * Dispbtches a Multicast message.
     */
    public void dispbtchMulticast(Message m, InetSocketAddress addr) {
        DISPATCH.bdd(new MulticastDispatch(m, addr));
    }
    
    /**
     * Dispbtches a TCP message.
     */
    public void dispbtchTCP(Message m, ManagedConnection conn) {
        DISPATCH.bdd(new TCPDispatch(m, conn));
    }
    
    
    privbte static class UDPDispatch implements Runnable {
        privbte static final MessageRouter ROUTER = RouterService.getMessageRouter();
        privbte final Message m;
        privbte final InetSocketAddress addr;
        
        UDPDispbtch(Message m, InetSocketAddress addr) {
            this.m = m; this.bddr = addr;
        }
        
        public void run() {
            ROUTER.hbndleUDPMessage(m, addr);
        }
    }
    
    privbte static class MulticastDispatch implements Runnable {
        privbte static final MessageRouter ROUTER = RouterService.getMessageRouter();
        privbte final Message m;
        privbte final InetSocketAddress addr;
        
        MulticbstDispatch(Message m, InetSocketAddress addr) {
            this.m = m; this.bddr = addr;
        }

        public void run() {
            ROUTER.hbndleMulticastMessage(m, addr);
        }
    }
    
    privbte static class TCPDispatch implements Runnable {
        privbte static final MessageRouter ROUTER = RouterService.getMessageRouter();
        privbte final Message m;
        privbte final ManagedConnection conn;

        TCPDispbtch(Message m, ManagedConnection conn) {
            this.m = m; this.conn = conn;
        }
        
        public void run() {
            ROUTER.hbndleMessage(m, conn);
        }
    }
}