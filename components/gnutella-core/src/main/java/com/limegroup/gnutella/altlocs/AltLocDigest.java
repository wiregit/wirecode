
package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.TreeSet;

import com.limegroup.gnutella.util.BloomFilter;

/**
 * A bloom filter factory that can create filters for direct locs and push locs
 */
public abstract class AltLocDigest implements BloomFilter {

    protected TreeSet _values;
    
    protected abstract int hash(AlternateLocation altloc);
    
    
    public void add(Object o) {
        
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#addAll(java.util.Collection)
     */
    public void addAll(Collection c) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c) {
        // TODO Auto-generated method stub
        return false;
    }
    
    public byte [] toBytes() {
        return null;
    }
    
    public void write(OutputStream out) throws IOException {
        
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
