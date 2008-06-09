package org.limewire.swarm.file;

import java.util.NoSuchElementException;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.SelectionStrategy;

public class ContiguousSelectionStrategy implements SelectionStrategy {
    
    private final long completedSize;
    
    public ContiguousSelectionStrategy(long completedSize) {
        this.completedSize = completedSize;
    }

    public Range pickAssignment(IntervalSet candidateBytes, IntervalSet neededBytes, long blockSize)
            throws NoSuchElementException {
        
        long lowerBound = neededBytes.getFirst().getLow();
        long upperBound = neededBytes.getLast().getHigh();
        
        // Input validation
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegalArgumentException("First needed byte must be >= 0, "+lowerBound+"<0");
        if (upperBound >= completedSize)
            throw new IllegalArgumentException("neededBytes contains bytes beyond the end of the file."+
                    upperBound + " >= " + completedSize);
        if (candidateBytes.isEmpty())
            throw new NoSuchElementException();
        
        Range candidate = candidateBytes.getFirst();

        // Calculate what the high byte offset should be.
        // This will be at most blockSize-1 bytes greater than the low.
        long alignedHigh = alignHigh(candidate.getLow(), blockSize);

        // alignedHigh >= candidate.low, and therefore we
        // only have to check if alignedHigh > candidate.high.
        if (alignedHigh > candidate.getHigh())
            alignedHigh = candidate.getHigh();

        // Our ideal interval is [candidate.low, alignedHigh]
        
        // Optimize away creation of new objects, if possible
        Range ret = candidate;
        if (ret.getHigh() != alignedHigh)
            ret = Range.createRange(candidate.getLow(), alignedHigh);

        return ret;
    }
    
    /** Aligns location to one byte before the next highest block boundary */
    protected long alignHigh(long location, long blockSize) {
        location += blockSize;
        location -= location % blockSize;
        return location - 1;
    }

}
