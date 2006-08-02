package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.Trie;
import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;
import com.limegroup.gnutella.util.Trie.Cursor;

/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized. 
 * 
 * This class is optimized by the use of a PATRICIA Trie to store the ranges.
 * Many of the optimizations work because of two key properties that we use
 * when inserting items into the Trie.
 * 
 *   1) If the item to be inserted is within a range already in the Trie,
 *      the item is not inserted.
 *   2) If the item to be inserted contains any items that are within the
 *      Trie, those items are removed.
 *      
 * Maintaining these properties allows certain necessary optimizations, such as
 * looking at only the closest node when performing a lookup.  If these
 * optimizations were not done, then certain items would appear closer within
 * the Trie, despite there being a range further away that encompassed a given IP.
 * 
 * Using a PATRICIA allows an intelligent traversal to be done, so that at most
 * 32 comparisons (the number of bits in an address) are performed regardless 
 * of the number of items inserted into the Trie.  It also allows very efficient
 * means of calculating the minimum distance (using an xor metric), because the
 * Trie orders the IPs by distance.  
 */
public class IPList {
    
    /** The list of IPs. */
    private Trie<IP, IP> ips = new PatriciaTrie<IP, IP>(new IPKeyAnalyzer());

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
    public boolean contains(IP lookup) {
        IP ip = ips.select(lookup);        
        return ip != null && ip.contains(lookup);
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
     * Calculates the minimum distance between an IPv4 address and this list of IPv4 address ranges.
     * 
     * @param lookup an IPv4 address, represented as an IP object with a /32 netmask.
     * @return 32-bit unsigned distance (using xor metric), represented as Java int
     */
    public int minDistanceTo(IP lookup) {
        if (lookup.mask != -1) {
            throw new IllegalArgumentException("Expected single IP, not an IP range.");
        }
        
        // The nature of the PATRICIA Trie & the distance (using an xor metric)
        // work well in that the closest item within the trie is also the shortest
        // distance.
        IP ip = ips.select(lookup);
        return ip.getDistanceTo(lookup);
    }
    
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
    
    private static class IPKeyAnalyzer implements KeyAnalyzer<IP> {

        private static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for(int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }
        
        private static final int[] BITS = createIntBitMask(32);
        
        public int length(IP key) {
            return 32;
        }

        public boolean isBitSet(IP key, int keyLength, int bitIndex) {
            int maddr = key.addr & key.mask;
            return (maddr & BITS[bitIndex]) != 0;
        }
        
        public int bitIndex(IP key,   int keyOff, int keyLength,
                            IP found, int foundOff, int foundKeyLength) {
            int maddr1 = key.addr & key.mask;
            int maddr2 = (found != null) ? found.addr & found.mask : 0;
            
            if(keyOff != 0 || foundOff != 0)
                throw new IllegalArgumentException("offsets must be 0 for fixed-size keys");
            
            int length = Math.max(keyLength, foundKeyLength);
            
            boolean allNull = true;
            for (int i = 0; i < length; i++) {
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
                return KeyAnalyzer.NULL_BIT_KEY;
            }
            
            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare(IP o1, IP o2) {
            int addr1 = o1.addr & o1.mask;
            int addr2 = o2.addr & o2.mask;
            if(addr1 > addr2)
                return 1;
            else if(addr1 < addr2)
                return -1;
            else
                return 0;
                
        }

        public int bitsPerElement() {
            return 1;
        }

        public boolean isPrefix(IP prefix, int offset, int length, IP key) {
            int addr1 = prefix.addr & prefix.mask;
            int addr2 = key.addr & key.mask;
            addr1 = addr1 << offset;
            
            int mask = 0;
            for(int i = 0; i < length; i++) {
                mask |= (0x1 << i);
            }
            
            addr1 &= mask;
            addr2 &= mask;
            
            return addr1 == addr2;
        }
    }
}
