package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.altlocs.AltLocDigest;
import com.limegroup.gnutella.util.BloomFilter;

/**
 * A stub for altloc digests.
 */
public class AltLocDigestStub extends AltLocDigest {

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#XOR(com.limegroup.gnutella.util.BloomFilter)
     */
    public void xor(BloomFilter other) {
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#OR(com.limegroup.gnutella.util.BloomFilter)
     */
    public void or(BloomFilter other) {
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#AND(com.limegroup.gnutella.util.BloomFilter)
     */
    public void and(BloomFilter other) {
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#invert()
     */
    public void invert() {
    }

}
