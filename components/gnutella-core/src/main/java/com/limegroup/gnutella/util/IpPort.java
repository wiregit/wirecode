package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.util.Comparator;

/**
 * Utility interface that allows class containing host information to be 
 * used generically.
 */
public interface IpPort {

    /**
     * Accessor for the <tt>InetAddress</tt> for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */
    InetAddress getInetAddress();
    
    /**
     * Accessor for the port this host is listening on.
     * 
     * @return the port this host is listening on
     */
    int getPort();

    /**
     * Accessor for the address string.
     * 
     * @return the address of this host as a string
     */
    String getAddress();
    
    /**
     * The sole comparator to use for IpPort objects.
     */
    public static final Comparator COMPARATOR = new IpPortComparator();
    
    /**
     * A comparator to compare IpPort objects.
     *
     * This is useful for when a variety of objects that implement IpPort
     * want to be placed in a Set.  Since it is difficult (near impossible)
     * to enforce that they all maintain a valid contract with regards
     * to hashCode & equals, the only valid way to enforce Set equality
     * is to use a Comparator that is based on the IpPortness.
     */
    public static class IpPortComparator implements Comparator {
        public int compare(Object a, Object b) {
            if(a == b)
                return 0;
            IpPort ip1 = (IpPort)a;
            IpPort ip2 = (IpPort)b;
            int diff = ip1.getPort() - ip2.getPort();
            if(diff == 0) {
                byte[] neta = ip1.getInetAddress().getAddress();
                byte[] netb = ip2.getInetAddress().getAddress();
                if(neta[0] == netb[0]) {
                    if(neta[1] == netb[1]) {
                        if(neta[2] == netb[2]) {
                            if(neta[3] == netb[3]) {
                                return 0;
                            } else {
                                return neta[3] - netb[3];
                            }
                        } else {
                            return neta[2] - netb[2];
                        }
                    } else {
                        return neta[1] - netb[1];
                    }
                } else {
                    return neta[0] - netb[0];
                }
            } else
                return diff;
        }
    }    
        
}
