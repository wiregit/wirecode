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
    private static final short IP_V = 4; 

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
     * @param String of the format "0.0.0.0" or "0.0.0.0/0.0.0.0" 
     *  representing ip_address/subnetmask
     * @return IP containing ip_address and subnetmask converted to long-variables
     */
    private static IP stringToIP (String ip_str) {
        StringTokenizer tokenizer = new StringTokenizer (ip_str, "/");
        String[] _ips = new String [tokenizer.countTokens()];
        for (int i = 0; i < _ips.length; i++) 
            _ips[i] = tokenizer.nextToken();
        
        if (_ips.length != 2 && _ips.length != 1)
            throw new IllegalArgumentException();

        if (_ips.length == 1) 
            return new IP ( stringToLong(_ips[0].replace('*', '0')),
                            parseForWildChars(_ips[0]));
        
        else 
            return new IP ( stringToLong(_ips[0].replace('*', '0')), 
                            stringToLong(_ips[1]) & parseForWildChars(_ips[0]));
        
    }

    /**
     * Convert String[] containing an ip_address to IP-object
     * @param String[] of the format {"0.0.0.0", "0.0.0.0"} representing
     *  {ip_address, subnetmask}
     * @return IP containing ip_address and subnetmask converted to long-variables
     */
    private static IP stringToIP (String[] ip_str) {
        if (ip_str.length != 2)
            throw new IllegalArgumentException();
        else 
            return new IP(stringToLong (ip_str[0]), stringToLong (ip_str[1]));
    }        

    /**
     * Convert String containing an ip_address or subnetmask to long
     * @param String of the format "0.0.0.0" presenting ip_address or subnetmask
     * @return long 
     */
    private static long stringToLong (String ip_str) {
        StringTokenizer tokenizer = new StringTokenizer(ip_str, ".");
        if (tokenizer.countTokens() != IP_V)
            throw new IllegalArgumentException();

        long ip = 0;
        // convert String to a Long variable
        for (int i = 0; i < IP_V; i++) {
            try {
                ip = ip << 8;
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
        if (tokenizer.countTokens() != IP_V)
            throw new IllegalArgumentException();
        
        long _mask = 0;
        
        for (int i = 0; i < IP_V; i++) {
            _mask = _mask << 8;
            if (!tokenizer.nextToken().equalsIgnoreCase("*"))
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
    
    /*
    public static void main(String args[]) {
        //More tests in IPList and IPFilter!

        IP a=new IP("1.1.1.*");
        IP b=new IP("1.1.1.2");
        IP c=new IP("1.1.2.1");
        Assert.that(a.contains(b));
        Assert.that(! b.contains(a));
        Assert.that(! a.contains(c));
        Assert.that(! c.contains(a));

        Assert.that(! a.equals(c));
        Assert.that(! a.equals(b));
        Assert.that(! b.equals(a));
        Assert.that(! b.equals(c));
        Assert.that(a.equals(a));
        Assert.that(b.equals(b));
        Assert.that(! a.equals("asdf"));
        IP d=new IP("1.1.1.0/255.255.255.0");
        Assert.that(a.equals(d));
        Assert.that(d.equals(a));
        Assert.that(a.hashCode()==d.hashCode());
    } 
    */
}
