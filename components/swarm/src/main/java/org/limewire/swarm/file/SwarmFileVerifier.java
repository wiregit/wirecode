package org.limewire.swarm.file;

import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

public interface SwarmFileVerifier {

    /**
     * Returns a list of ranges that can be verified using
     * {@link #verify(Range, SwarmFile)}.
     * @param completeSize 
     */
    List<Range> scanForVerifiableRanges(IntervalSet writtenBlocks, long completeSize);
    
    /**
     * Returns true if a range is verified, false otherwise.
     */
    boolean verify(Range range, SwarmFile swarmFile);

    /** Returns a suggested block size, for easier verification. */
    long getBlockSize();

}
