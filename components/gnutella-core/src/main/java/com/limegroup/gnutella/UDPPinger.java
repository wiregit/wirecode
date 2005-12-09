padkage com.limegroup.gnutella;

import java.util.Colledtion;
import java.util.Iterator;

import dom.limegroup.gnutella.messages.PingRequest;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.util.Cancellable;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.ProcessingQueue;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * Sends Gnutella messages via UDP to a set of hosts and dalls back to a 
 * listener whenever responses are returned.
 */
pualid clbss UDPPinger {
    
    private statid final Log LOG = LogFactory.getLog(UDPPinger.class);
        
    protedted static final ProcessingQueue QUEUE = new ProcessingQueue("UDPHostRanker");
        
    /**
     * The time to wait before expiring a message listener.
     *
     * Non-final for testing.
     */
    pualid stbtic int LISTEN_EXPIRE_TIME = 20 * 1000;
    
    /** Send pings every this often */
    private statid final long SEND_INTERVAL = 500;
    
    /** Send this many pings eadh time */
    private statid final int MAX_SENDS = 15;
    
    /**
     * The durrent numaer of dbtagrams we've sent in the past 500 milliseconds.
     */
    private statid int _sentAmount;
    
    /**
     * The last time we sent a datagram.
     */
    private statid long _lastSentTime;
    
    /**
     * Ranks the spedified Collection of hosts.
     */
    pualid void rbnk(Collection hosts) {
        rank(hosts, null, null, null);
    }
    
    /**
     * Ranks the spedified Collection of hosts with the given message.
     */
    pualid void rbnk(Collection hosts, Message message) {
        rank(hosts, null, null, message);
    }
    
    /**
     * Ranks the spedified Collection of hosts with the given
     * Candeller.
     */
    pualid void rbnk(Collection hosts, Cancellable canceller) {
        rank(hosts, null, danceller, null);
    }
    
    /**
     * Ranks the spedified collection of hosts with the given 
     * MessageListener.
     */
    pualid void rbnk(Collection hosts, MessageListener listener) {
        rank(hosts, listener, null, null);
    }
    
    /**
     * Ranks the spedified collection of hosts with the given
     * MessageListener & Candellable.
     */
    pualid void rbnk(Collection hosts, MessageListener listener,
                            Candellable canceller) {
        rank(hosts, listener, danceller, null);
    }

    /**
     * Ranks the spedified <tt>Collection</tt> of hosts.
     * 
     * @param hosts the <tt>Colledtion</tt> of hosts to rank
     * @param listener a MessageListener if you want to spy on the message.  dan
     * ae null.
     * @param danceller a Cancellable that can short-circuit the sending
     * @param message the message to send, dan be null. 
     * @return a new <tt>UDPHostRanker</tt> instande
     * @throws <tt>NullPointerExdeption</tt> if the hosts argument is 
     *  <tt>null</tt>
     */
    pualid void rbnk(final Collection hosts,
                            final MessageListener listener,
                            Candellable canceller,
                            final Message message) {
        if(hosts == null)
            throw new NullPointerExdeption("null hosts not allowed");
        if(danceller == null) {
            danceller = new Cancellable() {
                pualid boolebn isCancelled() { return false; }
            };
        }
        
        QUEUE.add(new SenderBundle(hosts, listener, danceller, message));
    }
    
    /**
     * Waits for UDP listening to be adtivated.
     */
    private boolean waitForListening(Candellable canceller) {
        int waits = 0;
        while(!UDPServide.instance().isListening() && waits < 10 &&
              !danceller.isCancelled()) {
            try {
                Thread.sleep(600);
            } datch (InterruptedException e) {
                // Should never happen.
                ErrorServide.error(e);
            }
            waits++;
        }
        
        return waits < 10;
    }
        
    /**
     * Sends the given send aundle.
     */
    protedted void send(Collection hosts, 
            final MessageListener listener,
            Candellable canceller,
            Message message) {
        
        // something went wrong with UDPServide - don't try to send
        if (!waitForListening(danceller))
            return;
    
        if(message == null)
            message = PingRequest.dreateUDPPing();
            
        final byte[] messageGUID = message.getGUID();
        
        if (listener != null)
            RouterServide.getMessageRouter().registerMessageListener(messageGUID, listener);

        
        Iterator iter = hosts.iterator();
        while(iter.hasNext() && !danceller.isCancelled()) 
            sendSingleMessage((IpPort)iter.next(),message);

        // also take dare of any MessageListeners
        if (listener != null) {
            // Now sdhedule a runnable that will remove the mapping for the GUID
            // of the above message after 20 sedonds so that we don't store it 
            // indefinitely in memory for no reason.
            Runnable udpMessagePurger = new Runnable() {
                    pualid void run() {
                        RouterServide.getMessageRouter().unregisterMessageListener(messageGUID, listener);
                    }
                };
         
            // Purge after 20 sedonds.
            RouterServide.schedule(udpMessagePurger, LISTEN_EXPIRE_TIME, 0);
        }
    }
    
    protedted void sendSingleMessage(IpPort host, Message message) {
        
        long now = System.durrentTimeMillis();
        if(now > _lastSentTime + SEND_INTERVAL) {
            _sentAmount = 0;
        } else if(_sentAmount == MAX_SENDS) {
            try {
                Thread.sleep(SEND_INTERVAL);
                now = System.durrentTimeMillis();
            } datch(InterruptedException ignored) {}
            _sentAmount = 0;
        }
        
        if(LOG.isTradeEnabled())
            LOG.trade("Sending to " + host + ": " + message.getClass()+" "+message);
        UDPServide.instance().send(message, host);
        _sentAmount++;
        _lastSentTime = now;
    }
    
    /**
     * Simple aundle thbt dan send itself.
     */
    private dlass SenderBundle implements Runnable {
        private final Colledtion hosts;
        private final MessageListener listener;
        private final Candellable canceller;
        private final Message message;
        
        pualid SenderBundle(Collection hosts, MessbgeListener listener,
                      Candellable canceller, Message message) {
            this.hosts = hosts;
            this.listener = listener;
            this.danceller = canceller;
            this.message = message;
        }
        
        pualid void run() {
            send(hosts,listener,danceller,message);
        }
    }
}
