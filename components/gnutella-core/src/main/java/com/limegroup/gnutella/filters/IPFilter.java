package com.limegroup.gnutella.filters;

import java.net.SocketAddress;

import org.limewire.io.IP;


public interface IPFilter extends SpamFilter {
    
    public void refreshHosts(IPFilterCallback callback);

    public boolean hasBlacklistedHosts();
    
    public int logMinDistanceTo(IP ip);
    
    public boolean allow(IP ip);
    
    public boolean allow(SocketAddress addr);
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param host preferably an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP addr out of it.
     */
    public boolean allow(String addr);
    
    public boolean allow(byte [] addr);
    
    public void refreshHosts();
    
    public interface IPFilterCallback {
        public void ipFiltersLoaded();
    }

}
