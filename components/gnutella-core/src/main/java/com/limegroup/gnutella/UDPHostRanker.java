package com.limegroup.gnutella;


import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.sun.java.util.collections.Iterator;

/**
 * Sends Gnutella pings via UPD to a set of hosts and calls back to a listener
 * whenever responses are returned.
 */
public class UDPHostRanker implements HostListener {

    private final HostListener LISTENER;

    /**
     * Crates a new <tt>UDPHostRanker</tt> for the specified set of hosts.
     * 
     * @param hosts the <tt>Iterator</tt> of hosts to rank
     * @param hl the listener that should be notified whenever hosts are 
     *  received
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt> or if the listener argument is <tt>null</tt>
     */
    public static UDPHostRanker createRanker(Iterator hosts, HostListener hl) {
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
     * <tt>UDPService</tt> to be created.
     * 
     * @param hosts the hosts to rank
     */
    private UDPHostRanker(Iterator hosts, HostListener hl) {
        LISTENER = hl;
        int waits = 0;
        while(!UDPService.instance().isListening() && waits < 5) {
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
        while(hosts.hasNext()) {
            IpPort host = (IpPort)hosts.next();
            UDPService.instance().send(ping, host.getAddress(), host.getPort(),
                this);
        }
    }

    // inherit doc comment
    public void addHost(IpPort host) {
        if(NetworkUtils.isPrivateAddress(host.getAddress())) {
            return;
        }
        if(host instanceof PingReply) {
            PingReply pr = (PingReply)host;
            if(pr.hasFreeUltrapeerSlots()) {
                LISTENER.addHost(pr);
            }            
        }
        
    }

}
