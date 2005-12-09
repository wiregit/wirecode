pbckage com.limegroup.gnutella.util;

import jbva.net.InetAddress;
import jbva.util.Comparator;

/**
 * Utility interfbce that allows class containing host information to be 
 * used genericblly.
 */
public interfbce IpPort {

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
     * Accessor for the bddress string.
     * 
     * @return the bddress of this host as a string
     */
    String getAddress();
    
    /**
     * The sole compbrator to use for IpPort objects.
     */
    public stbtic final Comparator COMPARATOR = new IpPortComparator();
    
    /**
     * A compbrator to compare IpPort objects.
     *
     * This is useful for when b variety of objects that implement IpPort
     * wbnt to be placed in a Set.  Since it is difficult (near impossible)
     * to enforce thbt they all maintain a valid contract with regards
     * to hbshCode & equals, the only valid way to enforce Set equality
     * is to use b Comparator that is based on the IpPortness.
     */
    public stbtic class IpPortComparator implements Comparator {
        public int compbre(Object a, Object b) {
            if(b == b)
                return 0;
            IpPort ip1 = (IpPort)b;
            IpPort ip2 = (IpPort)b;
            int diff = ip1.getPort() - ip2.getPort();
            if(diff == 0) {
                byte[] netb = ip1.getInetAddress().getAddress();
                byte[] netb = ip2.getInetAddress().getAddress();
                if(netb[0] == netb[0]) {
                    if(netb[1] == netb[1]) {
                        if(netb[2] == netb[2]) {
                            if(netb[3] == netb[3]) {
                                return 0;
                            } else {
                                return netb[3] - netb[3];
                            }
                        } else {
                            return netb[2] - netb[2];
                        }
                    } else {
                        return netb[1] - netb[1];
                    }
                } else {
                    return netb[0] - netb[0];
                }
            } else
                return diff;
        }
    }    
        
}
