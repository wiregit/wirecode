pbckage com.limegroup.gnutella;

import jbva.util.Collection;
import jbva.util.Iterator;

import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.util.Cancellable;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.ProcessingQueue;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * Sends Gnutellb messages via UDP to a set of hosts and calls back to a 
 * listener whenever responses bre returned.
 */
public clbss UDPPinger {
    
    privbte static final Log LOG = LogFactory.getLog(UDPPinger.class);
        
    protected stbtic final ProcessingQueue QUEUE = new ProcessingQueue("UDPHostRanker");
        
    /**
     * The time to wbit before expiring a message listener.
     *
     * Non-finbl for testing.
     */
    public stbtic int LISTEN_EXPIRE_TIME = 20 * 1000;
    
    /** Send pings every this often */
    privbte static final long SEND_INTERVAL = 500;
    
    /** Send this mbny pings each time */
    privbte static final int MAX_SENDS = 15;
    
    /**
     * The current number of dbtagrams we've sent in the past 500 milliseconds.
     */
    privbte static int _sentAmount;
    
    /**
     * The lbst time we sent a datagram.
     */
    privbte static long _lastSentTime;
    
    /**
     * Rbnks the specified Collection of hosts.
     */
    public void rbnk(Collection hosts) {
        rbnk(hosts, null, null, null);
    }
    
    /**
     * Rbnks the specified Collection of hosts with the given message.
     */
    public void rbnk(Collection hosts, Message message) {
        rbnk(hosts, null, null, message);
    }
    
    /**
     * Rbnks the specified Collection of hosts with the given
     * Cbnceller.
     */
    public void rbnk(Collection hosts, Cancellable canceller) {
        rbnk(hosts, null, canceller, null);
    }
    
    /**
     * Rbnks the specified collection of hosts with the given 
     * MessbgeListener.
     */
    public void rbnk(Collection hosts, MessageListener listener) {
        rbnk(hosts, listener, null, null);
    }
    
    /**
     * Rbnks the specified collection of hosts with the given
     * MessbgeListener & Cancellable.
     */
    public void rbnk(Collection hosts, MessageListener listener,
                            Cbncellable canceller) {
        rbnk(hosts, listener, canceller, null);
    }

    /**
     * Rbnks the specified <tt>Collection</tt> of hosts.
     * 
     * @pbram hosts the <tt>Collection</tt> of hosts to rank
     * @pbram listener a MessageListener if you want to spy on the message.  can
     * be null.
     * @pbram canceller a Cancellable that can short-circuit the sending
     * @pbram message the message to send, can be null. 
     * @return b new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts brgument is 
     *  <tt>null</tt>
     */
    public void rbnk(final Collection hosts,
                            finbl MessageListener listener,
                            Cbncellable canceller,
                            finbl Message message) {
        if(hosts == null)
            throw new NullPointerException("null hosts not bllowed");
        if(cbnceller == null) {
            cbnceller = new Cancellable() {
                public boolebn isCancelled() { return false; }
            };
        }
        
        QUEUE.bdd(new SenderBundle(hosts, listener, canceller, message));
    }
    
    /**
     * Wbits for UDP listening to be activated.
     */
    privbte boolean waitForListening(Cancellable canceller) {
        int wbits = 0;
        while(!UDPService.instbnce().isListening() && waits < 10 &&
              !cbnceller.isCancelled()) {
            try {
                Threbd.sleep(600);
            } cbtch (InterruptedException e) {
                // Should never hbppen.
                ErrorService.error(e);
            }
            wbits++;
        }
        
        return wbits < 10;
    }
        
    /**
     * Sends the given send bundle.
     */
    protected void send(Collection hosts, 
            finbl MessageListener listener,
            Cbncellable canceller,
            Messbge message) {
        
        // something went wrong with UDPService - don't try to send
        if (!wbitForListening(canceller))
            return;
    
        if(messbge == null)
            messbge = PingRequest.createUDPPing();
            
        finbl byte[] messageGUID = message.getGUID();
        
        if (listener != null)
            RouterService.getMessbgeRouter().registerMessageListener(messageGUID, listener);

        
        Iterbtor iter = hosts.iterator();
        while(iter.hbsNext() && !canceller.isCancelled()) 
            sendSingleMessbge((IpPort)iter.next(),message);

        // blso take care of any MessageListeners
        if (listener != null) {
            // Now schedule b runnable that will remove the mapping for the GUID
            // of the bbove message after 20 seconds so that we don't store it 
            // indefinitely in memory for no rebson.
            Runnbble udpMessagePurger = new Runnable() {
                    public void run() {
                        RouterService.getMessbgeRouter().unregisterMessageListener(messageGUID, listener);
                    }
                };
         
            // Purge bfter 20 seconds.
            RouterService.schedule(udpMessbgePurger, LISTEN_EXPIRE_TIME, 0);
        }
    }
    
    protected void sendSingleMessbge(IpPort host, Message message) {
        
        long now = System.currentTimeMillis();
        if(now > _lbstSentTime + SEND_INTERVAL) {
            _sentAmount = 0;
        } else if(_sentAmount == MAX_SENDS) {
            try {
                Threbd.sleep(SEND_INTERVAL);
                now = System.currentTimeMillis();
            } cbtch(InterruptedException ignored) {}
            _sentAmount = 0;
        }
        
        if(LOG.isTrbceEnabled())
            LOG.trbce("Sending to " + host + ": " + message.getClass()+" "+message);
        UDPService.instbnce().send(message, host);
        _sentAmount++;
        _lbstSentTime = now;
    }
    
    /**
     * Simple bundle thbt can send itself.
     */
    privbte class SenderBundle implements Runnable {
        privbte final Collection hosts;
        privbte final MessageListener listener;
        privbte final Cancellable canceller;
        privbte final Message message;
        
        public SenderBundle(Collection hosts, MessbgeListener listener,
                      Cbncellable canceller, Message message) {
            this.hosts = hosts;
            this.listener = listener;
            this.cbnceller = canceller;
            this.messbge = message;
        }
        
        public void run() {
            send(hosts,listener,cbnceller,message);
        }
    }
}
