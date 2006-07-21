package com.limegroup.gnutella.filters;

import java.util.LinkedList;
import java.util.List;

/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized.
 *
 * @author Gregorio Roper 
 */
public class IPList {
    /** The list of IP's. */
    private List<IP> ips = new LinkedList<IP>();

    public IPList () {}
    
    /**
     * Determines if any hosts exist in this list.
     */
    public boolean isEmpty() {
        return ips.isEmpty();
    }

    /** 
     * Adds a certain IP to the IPList.
     * @param ip_str a String containing the IP, see IP.java for formatting
     */
    public void add(String ip_str) {
	    IP ip;
        try {
            ip = new IP(ip_str);
        } catch (IllegalArgumentException e) {
            return;
        }
        
        if (!ips.contains(ip)) {// don't add the same IP more than once
            ips.add(ip);
        }
    }

    /**
     * @param String equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains (IP ip) {
        for(IP pattern : ips) {
            if (pattern.contains(ip))
                return true;
        }
        return false;
    }
    
    /**
     * Calculates the first set bit in the distance between an IPv4 address and
     * the ranges represented by this list.
     * 
     * This is equivalent to floor(log2(distance)) + 1.
     *  
     * @param ip an IPv4 address, represented as an IP object with a /32 netmask.
     * @return an int on the interval [0,31].
     */
    public int logMinDistanceTo(IP ip) {
        int distance = minDistanceTo(ip);
        int logDistance = 0;
        int testMask = -1; // All bits set
        // Guaranteed to terminate since testMask will reach zero
        while ((distance & testMask) != 0) {
            testMask <<= 1;
            ++logDistance;
        }
        return logDistance;
    }
    
    
    /**
     * Calculates the minimum distance between an IPv4 address this list of IPv4 address ranges.
     * 
     * @param ip an IPv4 address, represented as an IP object with a /32 netmask.
     * @return 32-bit unsigned distance (using xor metric), represented as Java int
     */
    public int minDistanceTo(IP ip) {
        if (ip.getMask() != -1) {
            throw new IllegalArgumentException("Expected single IP, not an IP range.");
        }
        // Note that this function uses xor with Integer.MIN_VALUE
        // to reverse the sense of the most significant bit.  This
        // causes the "<" and ">" operators to work properly even
        // though we're representing 32-bit unsinged values as
        // Java ints.
       int min_distance = Integer.MAX_VALUE;
       for(IP ipRange : ips) {
           int distance = Integer.MIN_VALUE ^ ipRange.getDistanceTo(ip);
           if (min_distance > distance) {
               min_distance = distance;
           }
       }
        
       // Change the most significant bit back to its normal sense.
       return Integer.MIN_VALUE ^ min_distance;
    }
}
