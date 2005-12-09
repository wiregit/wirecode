pbckage com.limegroup.gnutella.filters;

import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;

/**
 * A mutbble list of IP addresses.  More specifically, a list of sets of
 * bddresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized.
 *
 * @buthor Gregorio Roper 
 */
public clbss IPList {
    /** The list of IP's. */
    privbte List /* of IP */ ips = new LinkedList();

    public IPList () {}

    /** 
     * Adds b certain IP to the IPList.
     * @pbram ip_str a String containing the IP, see IP.java for formatting
     */
    public void bdd(String ip_str) {
	    IP ip;
        try {
            ip = new IP(ip_str);
        } cbtch (IllegalArgumentException e) {
            return;
        }
        
        if (!ips.contbins(ip)) {// don't add the same IP more than once
            ips.bdd(ip);
        }
    }

    /**
     * @pbram String equal to an IP
     * @returns true if ip_bddress is contained somewhere in the list of IPs
     */
    public boolebn contains (IP ip) {
        for (Iterbtor iter=ips.iterator(); iter.hasNext(); ) {
            IP pbttern=(IP)iter.next();
            if (pbttern.contains(ip))
                return true;
        }
        return fblse;
    }
}
