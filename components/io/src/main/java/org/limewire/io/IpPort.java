package org.limewire.io;


import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
    public static final Comparator<IpPort> COMPARATOR = new IpPortComparator();
    
    /** An empty list, casted to an IpPort. */
    public static final List<IpPort> EMPTY_LIST = Collections.emptyList();
    /** An empty set, casted to an IpPort. */
    public static final Set<IpPort> EMPTY_SET = Collections.emptySet();
    
    /**
     * A comparator to compare IpPort objects.
     *
     * This is useful for when a variety of objects that implement IpPort
     * want to be placed in a Set.  Since it is difficult (near impossible)
     * to enforce that they all maintain a valid contract with regards
     * to hashCode & equals, the only valid way to enforce Set equality
     * is to use a Comparator that is based on the IpPortness.
     */
    public static class IpPortComparator implements Comparator<IpPort> {
        public int compare(IpPort ip1, IpPort ip2) {
            if(ip1 == ip2)
                return 0;
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
