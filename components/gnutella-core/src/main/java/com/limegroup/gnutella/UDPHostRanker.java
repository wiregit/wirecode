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
     * Ranks the specified <tt>Collection</tt> of hosts.  It does this simply
     * by sending UDP Gnutella "pings" to each host in the specified 
     * <tt>Collection</tt>.  The hosts are then "ranked" by the order in which
     * they return pongs.  This gives some idea of network latency to that host,
     * allowing hosts that are closer on the network and/or that are less busy 
     * to be preferenced over hosts that are further away and/or more busy.
     * Returns the new <tt>UDPHostRanker</tt> instance.
     * 
     * @param hosts the <tt>Collection</tt> of hosts to rank
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt> or if the listener argument is <tt>null</tt>
     */
    public static UDPHostRanker rank(Collection hosts){
        if(hosts == null) {
            throw new NullPointerException("null hosts not allowed");
        }
        return new UDPHostRanker(hosts);
    }
    
    /**
     * Creates a new <tt>UDPHostRanker</tt> for the specified hosts.  This
     * constructor blocks sending pings to these hosts and waits for 
     * <tt>UDPService</tt> to open its socket.
     * 
     * @param hosts the hosts to rank
     */
    private UDPHostRanker(Collection hosts) {
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
        
        Iterator iter = hosts.iterator();
        while(iter.hasNext()) {
            IpPort host = (IpPort)iter.next();
            UDPService.instance().send(ping, host);
        }

        // now that we've pinged all these bad boys, any replies will get
        // funneled back to the HostCatcher via MessageRouter.handleUDPMessage
    }
}
