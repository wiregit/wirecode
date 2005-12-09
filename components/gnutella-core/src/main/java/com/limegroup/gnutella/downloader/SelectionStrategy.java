pbckage com.limegroup.gnutella.downloader;

import com.limegroup.gnutellb.util.IntervalSet;

public interfbce SelectionStrategy {
    /**
     * Encbpsulates an algorithm for deciding which block of a file to download next.  
     * 
     * For efficiency rebsons attempts will be made to align the start and end of intervals
     * to block boundbries.  However, there are no guarantees on alignment.
     * 
     * @pbram candidateBytes a representation of the set of 
     *      bytes thbt are candidates for downloading.  These are the
     *      bytes of the file thbt a given for download from a given server, minus
     *      the set of bytes thbt we already have (or have assigned)
     * @pbram neededBytes a representation of the set of bytes
     *      of the file thbt have not yet been leased, verified, etc.
     * @pbram blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignbred.  The returned Interval will in no case span a blockSize boundary.
     *      Any vblues less than 1 will generate IllegalArgumentExceptions.
     * @return the Intervbl that should be assigned next, which does not span a blockSize boundary
     * @throws NoSuchElementException if pbssed an empty IntervalSet
     */
    public Intervbl pickAssignment(IntervalSet candidateBytes, 
            IntervblSet neededBytes,
            long blockSize) throws jbva.util.NoSuchElementException;
}
