package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import java.util.Collection;
import java.util.Iterator;

/**
 * Sends Gnutella pings via UDP to a set of hosts and calls back to a listener
 * whenever responses are returned.
 */
public class UDPHostRanker {

    private static final MessageRouter ROUTER = 
        RouterService.getMessageRouter();

    /**
     * Ranks the specified <tt>Collection</tt> of hosts.  It does this simply
     * by sending UDP Gnutella "pings" to each host in the specified 
     * <tt>Collection</tt>.  The hosts are then "ranked" by the order in which
     * they return pongs.  This gives some idea of network latency to that host,
     * allowing hosts that are closer on the network and/or that are less busy 
     * to be preferenced over hosts that are further away and/or more busy.
     * Returns the new <tt>UDPHostRanker</tt> instance.
     * 
     * @param hosts the <tt>Collection</tt> of hosts to rank
     * @param listener a MessageListener if you want to spy on the pongs.  can
     * be null.
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt> or if the listener argument is <tt>null</tt>
     */
    public static void rank(final Collection hosts,
                            final MessageListener listener) {
        if(hosts == null) {
            throw new NullPointerException("null hosts not allowed");
        }
        Thread ranker = new ManagedThread(new Runnable() {
            public void run() {
                new UDPHostRanker(hosts, listener);
            }
        }, "UDPHostRanker");
        ranker.setDaemon(true);
        ranker.start();
    }
    
    /**
     * Creates a new <tt>UDPHostRanker</tt> for the specified hosts.  This
     * constructor blocks sending pings to these hosts and waits for 
     * <tt>UDPService</tt> to open its socket.
     * 
     * @param hosts the hosts to rank
     */
    private UDPHostRanker(Collection hosts, MessageListener listener) {
        int waits = 0;
        while(!UDPService.instance().isListening() && waits < 10) {
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
        final PingRequest ping = new PingRequest((byte)1);
        final GUID pingGUID = new GUID(ping.getGUID());
        
        if (listener != null) ROUTER.registerMessageListener(pingGUID, 
                                                             listener);

        final int MAX_SENDS = 15;
        Iterator iter = hosts.iterator();
        for(int i = 0; iter.hasNext(); i++) {
            if(i == MAX_SENDS) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ignored) {}
                i = 0;
            }
            IpPort host = (IpPort)iter.next();
            UDPService.instance().send(ping, host);
        }

        // now that we've pinged all these bad boys, any replies will get
        // funneled back to the HostCatcher via MessageRouter.handleUDPMessage

        // also take care of any MessageListeners
        if (listener != null) {

            // Now schedule a runnable that will remove the mapping for the GUID
            // of the above ping after 20 seconds so that we don't store it 
            // indefinitely in memory for no reason.
            Runnable udpPingPurger = new Runnable() {
                    public void run() {
                        ROUTER.unregisterMessageListener(pingGUID);
                    }
                };
         
            // Purge after 20 seconds.
            RouterService.schedule(udpPingPurger, (long)(20*1000), 0);
        }
    }
}
