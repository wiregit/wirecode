package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.Trie;
import com.limegroup.gnutella.util.PatriciaTrie.KeyCreator;
import com.limegroup.gnutella.util.Trie.Cursor;

/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized. 
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
    
    /** Gets the number of addresses loaded. */
    public int size() {
        return ips.size();
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
        // then don't include.  Also remove any IPs we encountered
        // that are contained by this new IP.
        // These two properties are necessary to allow the optimization
        // in Lookup to exit when the distance is greater than 1.
        AddFilter filter = new AddFilter(ip);
        Map.Entry<IP, IP> entry = ips.select(ip, filter);
        if(entry != null) {
            if(!entry.getKey().contains(ip)) {
                // TODO: remove during select instead of afterwards.
                for(IP obsolete : filter.getContained()) {
                    ips.remove(obsolete);
                }
                ips.put(ip, ip);
            }
        } else {
            ips.put(ip, ip);
        }
    }

    /**
     * @param String equal to an IP
     * @returns true if ip_address is contained somewhere in the list of IPs
     */
    public boolean contains(final IP ip) {
        long start, end;
        
        start = System.nanoTime();
        Map.Entry<IP,IP> e = ips.select(ip, new LookupFilter(ip));
        end = System.nanoTime();
        System.out.println("Lookup for: " + ip + " took: " + (end-start));
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
     * 
     * TODO: Optimize this to use the values of Patricia.
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
    
    /** A filter for looking up IPs. */
    private static class LookupFilter implements Trie.Cursor<IP, IP> {
        private final IP lookup;
        
        LookupFilter(IP lookup) {
            this.lookup = lookup;
        }
        
        public Cursor.SelectStatus select(Map.Entry<? extends IP, ? extends IP> entry) {
            IP compare = entry.getKey();
            if (compare.contains(lookup)) {
                return Cursor.SelectStatus.EXIT; // Terminate
            }
           
            // We traverse a PATRICIA Trie from the nearest to
            // the furthest item to the lookup key. So if the 
            // nearest item didn't contain the lookup IP and the
            // xor distance is more than one bit then terminate
            // as well as the lookup is hopeless and we'd otherwise
            // traverse the entire Trie.
            int distance = compare.getDistanceTo(lookup);
            return distance > 1 ? SelectStatus.EXIT : SelectStatus.CONTINUE;
        }
    };
    
    /**
     * A filter for adding IPs -- stores IPs we encountered that
     * are contained by the to-be-added IP, so they can later
     * be removed.
     */
    private static class AddFilter implements Trie.Cursor<IP, IP> {
        private final IP lookup;
        private List<IP> contained;
        
        AddFilter(IP lookup) {
            this.lookup = lookup;
        }
        
        /**
         * Returns all the IPs we encountered while selecting
         * that were contained by the IP being added.
         */
        public List<IP> getContained() {
            if(contained == null)
                return Collections.emptyList();
            else
                return contained;
        }
        
        public Cursor.SelectStatus select(Map.Entry<? extends IP, ? extends IP> entry) {
            IP compare = entry.getKey();
            if (compare.contains(lookup)) {
                return Cursor.SelectStatus.EXIT; // Terminate
            }
            
            // TODO: remove here when it starts working.
            if(lookup.contains(compare)) {
                if(contained == null)
                    contained = new ArrayList<IP>();
                contained.add(compare);
            }
            
            // See AddFilter for explanation.
            int distance = compare.getDistanceTo(lookup);
            return distance > 1 ? SelectStatus.EXIT : SelectStatus.CONTINUE;
        }
    };
    
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
