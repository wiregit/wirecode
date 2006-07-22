package com.limegroup.gnutella.filters;

import java.util.Map;

import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.Trie;
import com.limegroup.gnutella.util.PatriciaTrie.KeyCreator;

/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized.
 *
 * @author Gregorio Roper 
 */
public class IPList {
    
    
    /** The list of IPs. */
    private Trie<IP, IP> ips = new PatriciaTrie<IP, IP>(new IPKeyCreator());

    public IPList () {}
    
    /**
     * Determines if any hosts exist in this list.
     */
    public boolean isEmpty() {
        return ips.isEmpty();
    }

    /** 
     * Adds a certain IP to the IPList.
     * @param ipStr a String containing the IP, see IP.java for formatting
     */
    public void add(String ipStr) {
	    IP ip;
        try {
            ip = new IP(ipStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        
        // If we already had it (or an address that contained it),
        // then don't include.
        // TODO: Only add this check if we complete the full properties
        //       of the TODO in the contains method.
     //   if(!contains(ip))  
            ips.put(ip, ip);
    }

    /**
     * @param String equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains(final IP ip) {
        System.out.println("Looking up: " + ip);
        Trie.Cursor<IP, IP> cursor = new Trie.Cursor<IP, IP>() {
            public boolean select(Map.Entry<? extends IP, ? extends IP> entry) {
                IP compare = entry.getKey();
                if (compare.contains(ip)) {
                    System.out.println("Found a match on: " + compare);
                    return true; // Terminate
                }
                
                int distance = compare.getDistanceTo(ip);
                System.out.println("Comparing to: " + compare + ", distance: " + distance);

                // TODO: If we can ensure that when we add items,
                //       other items they contain are removed,
                //       OR if we can ensure that spacially larger
                //       IPs (those with larger netmasks) are traversed
                //       before smaller IPs, then we can the below shortcut.
                return false;
                
                // We traverse a PATRICIA Trie from the nearest to
                // the furthest item to the lookup key. So if the 
                // nearest item didn't contain the lookup IP and the
                // xor distance is more than one bit then terminate
                // as well as the lookup is hopeless and we'd otherwise
                // traverse the entire Trie.
                //return distance > 1;
            }
        };
        
        Map.Entry<IP,IP> e = ips.select(ip, cursor);
        return e != null && e.getKey().contains(ip);
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
        if (ip.mask != -1) {
            throw new IllegalArgumentException("Expected single IP, not an IP range.");
        }
        // Note that this function uses xor with Integer.MIN_VALUE
        // to reverse the sense of the most significant bit.  This
        // causes the "<" and ">" operators to work properly even
        // though we're representing 32-bit unsinged values as
        // Java ints.
       int min_distance = Integer.MAX_VALUE;
       for(IP ipRange : ips.keySet()) {
           int distance = Integer.MIN_VALUE ^ ipRange.getDistanceTo(ip);
           if (min_distance > distance) {
               min_distance = distance;
           }
       }
        
       // Change the most significant bit back to its normal sense.
       return Integer.MIN_VALUE ^ min_distance;
    }
    
    private static class IPKeyCreator implements KeyCreator<IP> {

        private static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for(int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }
        
        private static final int[] BITS = createIntBitMask(32);
        
        public int length() {
            return 32;
        }

        public boolean isBitSet(IP key, int bitIndex) {
            int maddr = key.addr & key.mask;
            return (maddr & BITS[bitIndex]) != 0;
        }
        
        public int bitIndex(IP key, IP found) {
            int maddr1 = key.addr & key.mask;
            int maddr2 = (found != null) ? found.addr & found.mask : 0;
            
            boolean allNull = true;
            for (int i = 0; i < 32; i++) {
                int a = maddr1 & BITS[i];
                int b = maddr2 & BITS[i];
                
                if (allNull && a != 0) {
                    allNull = false;
                }
                
                if (a != b) {
                    return i;
                }
            }
            
            if (allNull) {
                return KeyCreator.NULL_BIT_KEY;
            }
            
            return KeyCreator.EQUAL_BIT_KEY;
        }
    }
}
