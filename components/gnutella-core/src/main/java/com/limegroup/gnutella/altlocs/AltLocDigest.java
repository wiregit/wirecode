
package com.limegroup.gnutella.altlocs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.BloomFilter;

/**
 * A bloom filter factory that can create filters for direct locs and push locs
 * 
 * It assumes that each altloc hashes to a 12-bit digit, which allows us to have up to 
 * 4096 altlocs per mesh - which is plenty.
 * 
 * In memory, those 4096 bits are stored as a BitSet, but on the network they are 
 * represented as list of values - i.e. each 3 bytes carry the hashes of two altlocs.
 */
public abstract class AltLocDigest implements BloomFilter {

    /**
     * A BitSet storage for the values of the filter.
     * When we have many entries, or need to do boolean algebra we use
     * this representation.
     */
    protected BitSet _values;
    
    /**
     * the actual hashing function.  Acts differently on altlocs than pushlocs.
     * range is 0 < hash < 2^12
     */
    protected abstract int hash(AlternateLocation altloc);
    
    
    public void add(Object o) {
        
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#addAll(java.util.Collection)
     */
    public void addAll(Collection c) {
        for (Iterator iter = c.iterator();iter.hasNext();)
            add(iter.next());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        if (! (o instanceof AlternateLocation))
            return false;
        AlternateLocation loc = (AlternateLocation)o;
        
        return _values.get(hash(loc));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c) {
        for (Iterator iter = c.iterator();iter.hasNext();) {
            if (!contains(iter.next()))
                return false;
        }
        return true;
    }
    
    /**
     * @return a packed representation of hashes, where every 3 bytes represent
     * two altlocs.
     */
    public byte [] toBytes() {
        int size = _values.cardinality() / 3;
        if (_values.cardinality() % 3 != 0)
            size++;
        
        byte []ret = new byte[size];
        int index = 0;
        boolean first = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        for(int i=_values.nextSetBit(0); i>=0;i=_values.nextSetBit(i+1)){
            if (first) {
                ret[index++] = (byte)((i & 0xFF0 ) >> 4);
                ret[index] = (byte)((i & 0xF) << 4);
                first = false;
            } else {
                ret [index++] |= (byte)((i & 0xF00) >> 8);
                ret [index++] = (byte)(i & 0xFF);
                first = true;
            }
        }
        return ret;
    }
    
    public void write(OutputStream out) throws IOException {
        out.write(toBytes());
    }
    
    /**
     * @param push whether the location contains pushlocs
     * @return digest of the given location.
     */
    public static AltLocDigest getDigest(AlternateLocationCollection alc, boolean push) {
        return null;
    }
    
    public static AltLocDigest parseDigest(byte []data, int offset, int length) 
    throws IOException {
        return null;
    }
    
    public static AltLocDigest parseDigest(InputStream in) throws IOException {
        return null;
    }
    
}
