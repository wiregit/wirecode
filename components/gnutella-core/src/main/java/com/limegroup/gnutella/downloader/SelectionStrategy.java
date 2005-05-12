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
     *      bytes available for download from a given server
     * @param previewLength the number of contiguous bytes from the 
     *      beginning of the file that have already been assigned 
     *      (and will presumably soon be available for preview)
     * @param lastNeededByte all bytes after lastNeededByte have been assigned for
     *      download.  Note that this information may not be availbable
     *      from availableIntervals, since availableIntervals may contain server-specific
     *      information.
     * @param fileSize the total length of the file being downloaded
     * @param blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignared.  An attempt will be made to make the high end of the interval one less
     *      than a multiple of blockSize.  Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Interval that should be assigned next, with a size of at most blockSize bytes
     * @throws NoSuchElementException if passed an empty IntervalSet
     */
    public Interval pickAssignment(IntervalSet availableIntervals, 
            long previewLength,
            long lastNeededBlock,
            long blockSize) throws java.util.NoSuchElementException;
}
