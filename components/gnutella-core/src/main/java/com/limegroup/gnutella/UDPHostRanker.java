package com.limegroup.gnutella;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * Sends Gnutella messages via UDP to a set of hosts and calls back to a listener
 * whenever responses are returned.
 */
public class UDPHostRanker {

    private static final MessageRouter ROUTER = 
        RouterService.getMessageRouter();

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
    public static void rank(final Collection hosts,
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

        final Cancellable cancel = canceller;
        Thread ranker = new ManagedThread(new Runnable() {
            public void run() {
                new UDPHostRanker(hosts, listener, cancel, message);
            }
        }, "UDPHostRanker");
        ranker.setDaemon(true);
        ranker.start();
    }
    
    /**
     * Creates a new <tt>UDPHostRanker</tt> for the specified hosts.  This
     * constructor blocks sending messages to these hosts and waits for 
     * <tt>UDPService</tt> to open its socket.
     * 
     * @param hosts the hosts to rank
     */
    private UDPHostRanker(final Collection hosts,
                          final MessageListener listener,
                          final Cancellable canceller, 
                          Message message) {
        int waits = 0;
        while(!UDPService.instance().isListening() && waits < 10 &&
              !canceller.isCancelled()) {
            synchronized(this) {
                try {
                    wait(600);
                } catch (InterruptedException e) {
                    // Should never happen.
                    ErrorService.error(e);
                }
            }
            waits++;
        }
        
        if(message == null)
            message = PingRequest.createUDPPing();
            
        final byte[] messageGUID = message.getGUID();
        
        if (listener != null)
            ROUTER.registerMessageListener(messageGUID, listener);

        final int MAX_SENDS = 15;
        Iterator iter = hosts.iterator();
        for(int i = 0; iter.hasNext() && !canceller.isCancelled(); i++) {
            if(i == MAX_SENDS) {
                try {
                    Thread.sleep(500);
                } catch(InterruptedException ignored) {}
                i = 0;
            }
            IpPort host = (IpPort)iter.next();
            UDPService.instance().send(message, host);
        }

        // now that we've sent to these bad boys, any replies will get
        // funneled back to the HostCatcher via MessageRouter.handleUDPMessage

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
            RouterService.schedule(udpMessagePurger, (long)(20*1000), 0);
        }
    }
}
