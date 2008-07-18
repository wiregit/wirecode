package org.limewire.swarm;

import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

public interface SwarmVerifier {
    /**
     * Returns a list of ranges that can be verified using
     * {@link #verify(Range, SwarmFileSystem)}.
     * 
     * @param completeSize
     */
    List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize);

    /**
     * Returns true if a range is verified, false otherwise.
     */
    boolean verify(Range range, SwarmFileSystem swarmFile);

    /** Returns a suggested block size, for easier verification. */
    long getBlockSize();
}
