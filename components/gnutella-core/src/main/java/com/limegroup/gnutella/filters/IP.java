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
    private static final long DEFAULT_MASK = 0xffffffffL; //255.255.255.255

    // Constructors
    IP (String ip_str) {
        this(stringToIP(ip_str));
    }

    IP (String[] ip_str) {
        this(stringToIP(ip_str));
    }

    IP (long _addr, long _mask) {
        this.addr = _addr & _mask;
        this.mask = _mask;
    }

    IP (long _addr) {
        this.addr = _addr;
        this.mask = DEFAULT_MASK;
    }

    IP (IP ip) {
        this.addr = ip.addr & ip.mask;
        this.mask = ip.mask;
    }

    /**
     * Convert String containing an ip_address to IP-object
     * @param String of the format "0.0.0.0" or "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
     *  representing ip_address/subnetmask
     * @return IP containing ip_address and subnetmask converted to 
     * long-variables
     */
    private static IP stringToIP (String ip_str) {
        StringTokenizer tokenizer = new StringTokenizer (ip_str, "/");
        if ( tokenizer.countTokens() == 1)  //assume a simple IP "0.0.0.0"
            return new IP ( stringToLong(ip_str.replace('*', '0')),
                            parseForWildChars(ip_str));
        // maybe an IP of the form "0.0.0.0/0.0.0.0" or "0.0.0.0/0"
        else if (tokenizer.countTokens() == 2) { 
            String[] ip = { tokenizer.nextToken(), tokenizer.nextToken() };
            return stringToIP ( ip );
        } else 
            throw new IllegalArgumentException();
    }

    /**
     * Convert String[] containing an ip_address to IP-object
     * @param String[] of the format {"0.0.0.0", "0.0.0.0"} representing
     *  {ip_address, subnetmask}
     * @return IP containing ip_address and subnetmask converted to 
     * long-variables
     */
    private static IP stringToIP (String[] ip_str) {
        if (ip_str.length != 2)
            throw new IllegalArgumentException();
        else {
            // probably an IP with subnet mask "0.0.0.0/0.0.0.0"
            if (ip_str[1].indexOf('.') != -1) 
                return new IP ( stringToLong(ip_str[0].replace('*', '0')), 
                                stringToLong(ip_str[1]) & 
                                parseForWildChars(ip_str[0]));
            else {// try an IP with simplified subnetmask "0.0.0.0/0" 
                long mask = 0;
                try {
                    int i = 0;
                    for (; i < Integer.parseInt(ip_str[1]); i++ ) {
                        mask ++;
                        mask = mask << 1;
                    }
                    mask = mask << (31 - i);
                } catch (NumberFormatException e) {
                    //drop exception, simply ignore the mask.
                } 
                return new IP ( stringToLong(ip_str[0].replace('*', '0')), 
                                mask );
            }    
        }
    }        
    
    /**
     * Convert String containing an ip_address or subnetmask to long
     * @param String of the format "0.0.0..." presenting ip_address or 
     * subnetmask
     * @return long 
     */
    private static long stringToLong (String ip_str) {
        StringTokenizer tokenizer = new StringTokenizer(ip_str, ".");
        
        long ip = 0;
        // convert String to a Long variable
        for (int i = 0; i < 4; i++) {
            try {
                ip = ip << 8;
                // if number of tokens !=4, either use
                if (tokenizer.hasMoreTokens()) 
                    // the first four tokens or add "0"-tokens
            	    ip += Short.parseShort(tokenizer.nextToken());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }
        return ip;
    }
    
    /**
     * Create new subnet mask from IP-address of the format 0.*.*.0
     * @param String of the format W.X.Y.Z, W, X, Y and Z can be numbers or '*'
     * @return long with a subnet mask
     */
    private static long parseForWildChars (String s) {
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
     * @return ip address
     */
    long getAddr () {
        return this.addr;
    }
    
    /**
     * @return subnet mask
     */
    long getMask () {
        return this.mask;
    }
    
    /**
     * @return long variable containing the ip_address with the applied mask
     */
    public long getMaskedAddr () {
        return (mask & addr);
    }
    
    /**
     * @return long variable containing the inverted ip_address with the applied
     *  mask 
     */
    public long getIMaskedAddr () {
        return (mask & ~addr & DEFAULT_MASK);
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
