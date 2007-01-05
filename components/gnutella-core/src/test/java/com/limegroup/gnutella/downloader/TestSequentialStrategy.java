package com.limegroup.gnutella.downloader;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.limegroup.gnutella.util.IntervalSet;

/** 
 * Selection strategy that selects the next available block.  
 * 
 * This is not well tested; it's just here as a testing stub. 
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class TestSequentialStrategy implements SelectionStrategy {
    public Interval pickAssignment(IntervalSet availableBytes,
            IntervalSet neededBytes,
            long blockSize) throws NoSuchElementException {
        
        // Construct test cases such that this won't result in getting the first element of an empty list
        Iterator intervalIterator = availableBytes.getAllIntervals();
        
        // If we don't need alignment, we're done
        if (blockSize < 1 && intervalIterator.hasNext())
            return (Interval) intervalIterator.next();
        
        // Loop over intervals, looking for a suitable candidate
        while (intervalIterator.hasNext()) {
            Interval candidate = (Interval) intervalIterator.next();
            // align the high point of the interval
            long alignedHigh = candidate.low;
            // step ahead one block size
            alignedHigh += blockSize;
            // cut back to the block boundary
            alignedHigh -= (alignedHigh % blockSize);
            // step back one more byte
            alignedHigh -= 1;
            
            if (alignedHigh <= candidate.high) {
                return new Interval((int)candidate.low, (int) alignedHigh);
            } else {
                return candidate;
            }
        }
        
        // All Intervals have been tested and found to be lacking
        throw new NoSuchElementException("blockSize:"+blockSize+" Intervals:"+availableBytes); 
    }
}
