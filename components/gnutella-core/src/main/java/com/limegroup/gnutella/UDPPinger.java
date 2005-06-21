package com.limegroup.gnutella;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ProcessingQueue;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Sends Gnutella messages via UDP to a set of hosts and calls back to a 
 * listener whenever responses are returned.
 */
public class UDPPinger {
    
    private static final Log LOG = LogFactory.getLog(UDPPinger.class);

    protected static final MessageRouter ROUTER = 
        RouterService.getMessageRouter();
        
    protected static final ProcessingQueue QUEUE = 
         new ProcessingQueue("UDPHostRanker");
        
    /**
     * The time to wait before expiring a message listener.
     *
     * Non-final for testing.
     */
    public static int LISTEN_EXPIRE_TIME = 20 * 1000;
    
    /** Send pings every this often */
    private static final long SEND_INTERVAL = 500;
    
    /** Send this many pings each time */
    private static final int MAX_SENDS = 15;
    
    /**
     * The current number of datagrams we've sent in the past 500 milliseconds.
     */
    private static int _sentAmount;
    
    /**
     * The last time we sent a datagram.
     */
    private static long _lastSentTime;
    
    /**
     * Ranks the specified Collection of hosts.
     */
    public void rank(Collection hosts) {
        rank(hosts, null, null, null);
    }
    
    /**
     * Ranks the specified Collection of hosts with the given message.
     */
    public void rank(Collection hosts, Message message) {
        rank(hosts, null, null, message);
    }
    
    /**
     * Ranks the specified Collection of hosts with the given
     * Canceller.
     */
    public void rank(Collection hosts, Cancellable canceller) {
        rank(hosts, null, canceller, null);
    }
    
    /**
     * Ranks the specified collection of hosts with the given 
     * MessageListener.
     */
    public void rank(Collection hosts, MessageListener listener) {
        rank(hosts, listener, null, null);
    }
    
    /**
     * Ranks the specified collection of hosts with the given
     * MessageListener & Cancellable.
     */
    public void rank(Collection hosts, MessageListener listener,
                            Cancellable canceller) {
        rank(hosts, listener, canceller, null);
    }

    /**
     * Ranks the specified <tt>Collection</tt> of hosts.
     * 
     * @param hosts the <tt>Collection</tt> of hosts to rank
     * @param listener a MessageListener if you want to spy on the message.  can
     * be null.
     * @param canceller a Cancellable that can short-circuit the sending
     * @param message the message to send, can be null. 
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt>
     */
    public void rank(final Collection hosts,
                            final MessageListener listener,
                            Cancellable canceller,
                            final Message message) {
        if(hosts == null)
            throw new NullPointerException("null hosts not allowed");
        if(canceller == null) {
            canceller = new Cancellable() {
                public boolean isCancelled() { return false; }
            };
        }
        
        QUEUE.add(new SenderBundle(hosts, listener, canceller, message));
    }
    
    /**
     * Waits for UDP listening to be activated.
     */
    private boolean waitForListening(Cancellable canceller) {
        int waits = 0;
        while(!UDPService.instance().isListening() && waits < 10 &&
              !canceller.isCancelled()) {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                // Should never happen.
                ErrorService.error(e);
            }
            waits++;
        }
        
        return waits < 10;
    }
        
    /**
     * Sends the given send bundle.
     */
    protected void send(Collection hosts, 
            final MessageListener listener,
            Cancellable canceller,
            Message message) {
        
        // something went wrong with UDPService - don't try to send
        if (!waitForListening(canceller))
            return;
    
        if(message == null)
            message = PingRequest.createUDPPing();
            
        final byte[] messageGUID = message.getGUID();
        
        if (listener != null)
            ROUTER.registerMessageListener(messageGUID, listener);

        
        Iterator iter = hosts.iterator();
        while(iter.hasNext() && !canceller.isCancelled()) 
            sendSingleMessage((IpPort)iter.next(),message);

        // also take care of any MessageListeners
        if (listener != null) {
            // Now schedule a runnable that will remove the mapping for the GUID
            // of the above message after 20 seconds so that we don't store it 
            // indefinitely in memory for no reason.
            Runnable udpMessagePurger = new Runnable() {
                    public void run() {
                        ROUTER.unregisterMessageListener(messageGUID, listener);
                    }
                };
         
            // Purge after 20 seconds.
            RouterService.schedule(udpMessagePurger, LISTEN_EXPIRE_TIME, 0);
        }
    }
    
    protected void sendSingleMessage(IpPort host, Message message) {
        
        long now = System.currentTimeMillis();
        if(now > _lastSentTime + SEND_INTERVAL) {
            _sentAmount = 0;
        } else if(_sentAmount == MAX_SENDS) {
            try {
                Thread.sleep(SEND_INTERVAL);
                now = System.currentTimeMillis();
            } catch(InterruptedException ignored) {}
            _sentAmount = 0;
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace("Sending to " + host + ": " + message.getClass()+" "+message);
        UDPService.instance().send(message, host);
        _sentAmount++;
        _lastSentTime = now;
    }
    
    /**
     * Simple bundle that can send itself.
     */
    private class SenderBundle implements Runnable {
        private final Collection hosts;
        private final MessageListener listener;
        private final Cancellable canceller;
        private final Message message;
        
        public SenderBundle(Collection hosts, MessageListener listener,
                      Cancellable canceller, Message message) {
            this.hosts = hosts;
            this.listener = listener;
            this.canceller = canceller;
            this.message = message;
        }
        
        public void run() {
            send(hosts,listener,canceller,message);
        }
    }
}
