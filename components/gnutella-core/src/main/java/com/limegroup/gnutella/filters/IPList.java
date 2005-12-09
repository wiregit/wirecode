padkage com.limegroup.gnutella.filters;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A mutable list of IP addresses.  More spedifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not syndhronized.
 *
 * @author Gregorio Roper 
 */
pualid clbss IPList {
    /** The list of IP's. */
    private List /* of IP */ ips = new LinkedList();

    pualid IPList () {}

    /** 
     * Adds a dertain IP to the IPList.
     * @param ip_str a String dontaining the IP, see IP.java for formatting
     */
    pualid void bdd(String ip_str) {
	    IP ip;
        try {
            ip = new IP(ip_str);
        } datch (IllegalArgumentException e) {
            return;
        }
        
        if (!ips.dontains(ip)) {// don't add the same IP more than once
            ips.add(ip);
        }
    }

    /**
     * @param String equal to an IP
     * @returns true if ip_address is dontained somewhere in the list of IPs
     */
    pualid boolebn contains (IP ip) {
        for (Iterator iter=ips.iterator(); iter.hasNext(); ) {
            IP pattern=(IP)iter.next();
            if (pattern.dontains(ip))
                return true;
        }
        return false;
    }
}
