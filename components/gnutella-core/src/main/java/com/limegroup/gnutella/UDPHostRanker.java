package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.IpPort;
import com.sun.java.util.collections.Collection;
import com.sun.java.util.collections.Iterator;

/**
 * Sends Gnutella pings via UDP to a set of hosts and calls back to a listener
 * whenever responses are returned.
 */
public class UDPHostRanker {

    /**
     * Constant <tt>HostListener</tt> that should be notified about new hosts.
     */
    private final HostListener LISTENER;

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
     * @param hl the listener that should be notified whenever hosts are 
     *  received
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt> or if the listener argument is <tt>null</tt>
     */
    public static void rank(final Collection hosts, final HostListener hl) {
        if(hosts == null) {
            throw new NullPointerException("null hosts not allowed");
        }
        if(hl == null) {
            throw new NullPointerException("null listener not allowed");
        }
        Thread ranker = new Thread(new Runnable() {
                public void run() {
                    new UDPHostRanker(hosts, hl);
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
    private UDPHostRanker(Collection hosts, HostListener hl) {
        LISTENER = hl;
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
        
        // Add the mapping for the new GUID.
        UDPService.instance().addListener(pingGUID, LISTENER);
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
        
        // Now schedule a runnable that will remove the mapping for the GUID
        // of the above ping after 20 seconds so that we don't store it 
        // indefinitely in memory for no reason.
        Runnable udpPingPurger = new Runnable() {
            public void run() {
                UDPService.instance().removeListener(pingGUID);
            }
        };
        
        // Purge after 20 seconds.
        RouterService.schedule(udpPingPurger, (long)(20*1000), 0);
    }
}
