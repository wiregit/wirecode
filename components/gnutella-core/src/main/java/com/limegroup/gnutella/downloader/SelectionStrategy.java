package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.util.IntervalSet;

public interface SelectionStrategy {
    /**
     * Encapsulates an algorithm for deciding which block of a file to download next.  
     * 
     * For efficiency reasons attempts will be made to align the start and end of intervals
     * to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @param availableIntervals a representation of the set of bytes available for download
     * @param blockSize the size of block that shoud be returned, any values less than one will be ignored
     * @return the Interval that should be assigned next, with a size of at most blockSize bytes
     */
    public Interval pickAssignment(IntervalSet availableIntervals, long fileLength, long blockSize) throws java.util.NoSuchElementException;
}
