package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.altlocs.AltLocDigest;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.util.BloomFilter;

/**
 * A stub for altloc digests.
 */
public class AltLocDigestStub extends AltLocDigest {

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AltLocDigest#hash(com.limegroup.gnutella.altlocs.AlternateLocation)
     */
    protected int hash(AlternateLocation altloc) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#XOR(com.limegroup.gnutella.util.BloomFilter)
     */
    public BloomFilter XOR(BloomFilter other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#OR(com.limegroup.gnutella.util.BloomFilter)
     */
    public BloomFilter OR(BloomFilter other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#AND(com.limegroup.gnutella.util.BloomFilter)
     */
    public BloomFilter AND(BloomFilter other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#invert()
     */
    public BloomFilter invert() {
        // TODO Auto-generated method stub
        return null;
    }

}
