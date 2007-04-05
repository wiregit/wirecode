package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.PatriciaTrie.Cursor;
import com.limegroup.gnutella.util.PatriciaTrie.KeyAnalyzer;

/**
 * A mutable list of IP addresses.  More specifically, a list of sets of
 * addresses, like "18.239.0.*".  Provides fast operations to find if an address
 * is in the list.  Used to implement IPFilter.  Not synchronized.
 *
 * @author Gregorio Roper 
 */
public class IPList {
    
    /** A null IP, to use as a comparison when adding. */
    private static final IP NULL_IP = new IP("*.*.*.*");
    
    /** The list of IP's. */
    private PatriciaTrie ips = new PatriciaTrie(new IPKeyAnalyzer());

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
     * @param ip_str a String containing the IP, see IP.java for formatting
     */
    public void add(String ip_str) {
	    IP ip;
        try {
            ip = new IP(ip_str);
        } catch (IllegalArgumentException e) {
            return;
        }
        
        add(ip);
    }
    
    /** 
     * Adds a certain IP to the IPList.
     * @param ipStr a String containing the IP, see IP.java for formatting
     */
    public void add(IP ip) {
        // SPECIAL-CASE:
        // If the IP we're adding is the 'null' key (0.0.0.0/0.0.0.0)
        // then we must clear the trie.  The AddFilter trick will not
        // work in this case.
        if(ip.equals(NULL_IP)) {
            ips.clear();
            ips.put(ip, ip);
            return;
        }
        
        if(!NetworkUtils.isValidAddress(ip)) {
            return;
        }
                
        // If we already had it (or an address that contained it),
        // then don't include.  Also remove any IPs we encountered
        // that are contained by this new IP.
        // These two properties are necessary to allow the optimization
        // in Lookup to exit when the distance is greater than 1.
        AddFilter filter = new AddFilter(ip);
        Map.Entry entry = ips.select(ip, filter);
        if(entry != null) {
            if(!((IP)entry.getKey()).contains(ip)) {
                for(Iterator iter =  filter.getContained().iterator(); iter.hasNext();) {
                    ips.remove(iter.next());
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
        IP ip = (IP) ips.select(lookup);        
        return ip != null && ip.contains(lookup);
    }
    
    public boolean isValidFilter(boolean allowPrivateIPs) {
        ValidFilter filter = new ValidFilter(allowPrivateIPs);
        ips.traverse(filter);
        return filter.isValid();
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
    public int minDistanceTo(IP lookup) {
        if (lookup.mask != -1) {
            throw new IllegalArgumentException("Expected single IP, not an IP range.");
        }
        // The nature of the PATRICIA Trie & the distance (using an xor metric)
        // work well in that the closest item within the trie is also the shortest
        // distance.
        IP ip = (IP) ips.select(lookup);
        if(ip == null)
            return Integer.MAX_VALUE;
        else
            return ip.getDistanceTo(lookup);
    }
      
    
    /**
     *  A trie cursor that determines if the IP list contained in the
     *  trie is valid or not. 
     *  A list is considered invalid if :
     *  1) It contains a private IP
     *  2) It contains an invalid IP
     *  3) It spans a range of hosts larger than the MAX_LIST_SPACE constant
     *
     */
    private static class ValidFilter implements Cursor {
        
        /** The space covered by private addresses */
        private static final int INVALID_SPACE = 60882944;
        
        /** The total IP space available */
        private static final long TOTAL_SPACE = (long)Math.pow(2,32) - INVALID_SPACE;
        
        /** The maximum IP space (in percent) for this IPList to be valid */
        private static final float MAX_LIST_SPACE = 0.05f;
        
        private boolean isInvalid;
        private long counter;
        
        private final boolean allowPrivateIPs;
        
        public boolean isValid() {
            return !isInvalid && ((counter/(float)TOTAL_SPACE) < MAX_LIST_SPACE) ;
        }
        
        public ValidFilter(boolean allowPrivateIPs) {
            this.allowPrivateIPs = allowPrivateIPs;
        }
        
        public int select(Entry entry) {
            IP key = (IP) entry.getKey();
            byte[] buf = new byte[4];
            ByteOrder.int2beb(key.addr,buf,0);
            
            if(!allowPrivateIPs && NetworkUtils.isPrivateAddress(buf)) {
                isInvalid = true;
                return Cursor.EXIT;
            }
            
            counter += Math.pow(2,countBits(~key.mask));
            return Cursor.CONTINUE;
        }
        
        /**
         * Counts number of 1 bits in a 32 bit unsigned number.
         *
         * @param x unsigned 32 bit number whose bits you wish to count.
         *
         * @return number of 1 bits in x.
         * @author Roedy Green
         */
        private int countBits( int x ) {
           // collapsing partial parallel sums method
           // collapse 32x1 bit counts to 16x2 bit counts, mask 01010101
           x = (x >>> 1 & 0x55555555) + (x & 0x55555555);
           // collapse 16x2 bit counts to 8x4 bit counts, mask 00110011
           x = (x >>> 2 & 0x33333333) + (x & 0x33333333);
           // collapse 8x4 bit counts to 4x8 bit counts, mask 00001111
           x = (x >>> 4 & 0x0f0f0f0f) + (x & 0x0f0f0f0f);
           // collapse 4x8 bit counts to 2x16 bit counts
           x = (x >>> 8 & 0x00ff00ff) + (x & 0x00ff00ff);
           // collapse 2x16 bit counts to 1x32 bit count
           return(x >>> 16) + (x & 0x0000ffff);
       }
        
    }
    
    /**
     * A filter for adding IPs -- stores IPs we encountered that
     * are contained by the to-be-added IP, so they can later
     * be removed.
     */
    private static class AddFilter implements Cursor {
        private final IP lookup;
        private List contained;
        
        AddFilter(IP lookup) {
            this.lookup = lookup;
        }
        
        /**
         * Returns all the IPs we encountered while selecting
         * that were contained by the IP being added.
         */
        public List getContained() {
            if(contained == null)
                return Collections.EMPTY_LIST;
            else
                return contained;
        }
        
        public int select(Map.Entry entry) {
            IP compare = (IP) entry.getKey();
            if (compare.contains(lookup)) {
                return Cursor.EXIT; // Terminate
            }
            
            if(lookup.contains(compare)) {
                if(contained == null)
                    contained = new ArrayList();
                contained.add(compare);
                return Cursor.CONTINUE;
            } else {
                // Because select traverses in XOR closeness,
                // the first time we encounter an item that's
                // not contained, we know we've exhausted all
                // possible containing values.
                return Cursor.EXIT;
            }
        }
    };
    
    private static class IPKeyAnalyzer implements KeyAnalyzer {

        private static final int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for(int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }
        
        private static final int[] BITS = createIntBitMask(32);
        
        public int length(Object key) {
            return 32;
        }

        public boolean isBitSet(Object key, int keyLength, int bitIndex) {
            IP ip = (IP) key;
            int maddr = ip.addr & ip.mask;
            return (maddr & BITS[bitIndex]) != 0;
        }
        
        public int bitIndex(Object key,   int keyOff, int keyLength,
                            Object found, int foundOff, int foundKeyLength) {
            int maddr1 = ((IP)key).addr & ((IP)key).mask;
            int maddr2 = (found != null) ? ((IP)found).addr & ((IP)found).mask : 0;
            
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

        public int compare(Object o1, Object o2) {
            int addr1 = ((IP)o1).addr & ((IP)o1).mask;
            int addr2 = ((IP)o2).addr & ((IP)o2).mask;
            if(addr1 > addr2)
                return 1;
            else if(addr1 < addr2)
                return -1;
            else
                return 0;
                
        }

        // This method is generally intended for variable length keys.
        // Fixed-length keys, such as an IP address (32 bits) tend to
        // look at each element as a bit, thus 1 element == 1 bit.
        public int bitsPerElement() {
            return 1;
        }

        public boolean isPrefix(Object prefix, int offset, int length, Object key) {
            int addr1 = ((IP)prefix).addr & ((IP)prefix).mask;
            int addr2 = ((IP)key).addr & ((IP)key).mask;
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
