package com.limegroup.gnutella.filters;

import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import com.limegroup.gnutella.Assert;

/**
 * An IP address.  More precisely, a set of IP addresses specified by a regular
 * expression (e.g., "18.239.*.*") or a submask (e.g., "18.239.0.0/255.255.0.0".
 * Immutable, though the equals(..) and hashCode(..) methods have not been
 * overridden.  Used to implement IPFilter; the generic usefulness of this
 * class is questionable.
 *
 * @author Gregorio Roper 
 */
class IP {
    private long addr;
    private long mask;

    /**
     * Creates an IP object
     * 
     * @param ip_str a String of the format "0.0.0.0", "0.0.0.0/0.0.0.0", 
     *               or "0.0.0.0/0" as an argument.
     */
    IP (String ip_str) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer (ip_str, "/");
        if ( tokenizer.countTokens() == 1) { //assume a simple IP "0.0.*.*"
            this.addr = stringToLong(ip_str.replace('*', '0'));
            this.mask = createNetmaskFromWildChars(ip_str);
        } else if (tokenizer.countTokens() == 2) {
            // maybe an IP of the form "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
            this.addr = stringToLong(tokenizer.nextToken());
            this.mask = parseNetmask(tokenizer.nextToken());
        } else 
            throw new IllegalArgumentException("Could not parse address: " + 
                                               ip_str);
    }

    /**
     * Convert String containing an netmask to long variable containing
     * a bitmask
     * @param mask String containing a netmask of the format "0.0.0.0" or
     *          containing an unsigned integer < 32 if this netmask uses the
     *          simplified bsd syntax.
     * @return long containing the subnetmask
     */
    private static long parseNetmask (String mask) 
        throws IllegalArgumentException {
        final String exceptionString = "Could not parse netmask: ";
        StringTokenizer tokenizer = new StringTokenizer(mask, ".");
        // assume simple syntax, an integer <= 32
        if (tokenizer.countTokens() == 1) {
            try {
                // if the String contains the number k, we should return a
                // mask of k ones followed by (32 - k) zeroes
                short k = Short.parseShort(mask);
                if (k > 32 || k < 0) 
                    throw new IllegalArgumentException (exceptionString + mask);
                else {
                    long netmask = 0;
                    for (int i = 0; i < k; i++) {
                        // move the bit first because we want
                        // the last 1 at position 0 of the long
                        // variable
                        netmask = netmask << 1;
                        netmask++;
                    }
                    netmask = netmask << (32 - k);
                    return netmask;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException (exceptionString + mask);
            }
        } else if (tokenizer.countTokens() == 4) {// assume format: "0.0.0.0"
            return stringToLong(mask);
        } else 
            throw new IllegalArgumentException (exceptionString + mask);
    }        
    
    /**
     * Convert String containing an ip_address or subnetmask to long 
     * containing a bitmask.
     * @param String of the format "0.0.0..." presenting ip_address or 
     * subnetmask
     * @return long containing a bit representation of the ip address
     */
    private static long stringToLong (String ip_str) 
        throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(ip_str, ".");
        
        long ip = 0;
        // convert String to a Long variable
        for (int i = 0; i < 4; i++) {
            try {
                ip = ip << 8;
                // if tokenizer.countTokens() < 4, simply add zeros
                if (tokenizer.hasMoreTokens()) 
            	    ip += Short.parseShort(tokenizer.nextToken());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse address or mask: "
                                                   + ip_str);
            }
        }
        return ip;
    }
    
    /**
     * Create new subnet mask from IP-address of the format 0.*.*.0
     * @param String of the format W.X.Y.Z, W, X, Y and Z can be numbers or '*'
     * @return long with a subnet mask
     */
    private static long createNetmaskFromWildChars (String s) 
        throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(s, ".");
        long _mask = 0;
        
        for (int i = 0; i < 4; i++) {
            _mask = _mask << 8;
            if (tokenizer.hasMoreTokens() && 
                !tokenizer.nextToken().equalsIgnoreCase("*"))
                _mask += 0xff;
        }
        return _mask;
    }
    
    /**
     * Returns if ip is contained in this.
     * @param ip a singleton IP set, e.g., one representing a single address
     */
    public boolean contains(IP ip) {
        //Example: let this=1.1.1.*=1.1.1.0/255.255.255.0
        //         let ip  =1.1.1.2=1.1.1.2/255.255.255.255  
        //      => ip.addr&this.mask=1.1.1.0
        //      => this.addr&this.mask=1.1.1.0
        return ((ip.addr & this.mask) == (this.addr & this.mask));
    }
    
    /**
     * Returns true if other is an IP with the same address and mask.  Note that
     * "1.1.1.1/255.255.255.255" does not equal "2.2.2.2/255.255.255.255", even
     * though they denote the same sets of addresses.
     */
    public boolean equals(Object other) {
        if (other instanceof IP) {
            IP ip=(IP)other;
            return this.addr==ip.addr && this.mask==ip.mask;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return (int)(addr^mask);
    }
}
