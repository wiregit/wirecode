package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.IpPort;
import com.sun.java.util.collections.Collection;
import com.sun.java.util.collections.Iterator;

/**
 * Sends Gnutella pings via UPD to a set of hosts and calls back to a listener
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
    public static UDPHostRanker rank(Collection hosts, HostListener hl){
        if(hosts == null) {
            throw new NullPointerException("null hosts not allowed");
        }
        if(hl == null) {
            throw new NullPointerException("null listener not allowed");
        }
        return new UDPHostRanker(hosts, hl);
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
        PingRequest ping = new PingRequest((byte)1);
        Iterator iter = hosts.iterator();
        while(iter.hasNext()) {
            IpPort host = (IpPort)iter.next();
            System.out.println("UDPHostRanker::sending ping");
            UDPService.instance().send(ping, host, LISTENER);
        }
    }
}
