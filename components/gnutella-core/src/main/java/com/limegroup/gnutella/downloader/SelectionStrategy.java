package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.util.IntervalSet;

public interface SelectionStrategy {
    /**
     * Encapsulates an algorithm for deciding which block of a file to download next.  
     * 
     * For efficiency reasons attempts will be made to align the start and end of intervals
     * to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @param availableIntervals a representation of the set of 
     *      bytes available for download from a given server, minus
     *      the set of bytes that we already have (or have assigned)
     * @param lowerBound the first byte that has not been assigned yet
     * @param upperBound the last byte that has not been assigned yet
     * @param fileSize the total length of the file being downloaded
     * @param blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignared.  An attempt will be made to make the high end of the interval one less
     *      than a multiple of blockSize.  Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Interval that should be assigned next, with a size of at most blockSize bytes
     * @throws NoSuchElementException if passed an empty IntervalSet
     */
    public Interval pickAssignment(IntervalSet availableIntervals, 
            long lowerBound,
            long upperBound,
            long blockSize) throws java.util.NoSuchElementException;
}
