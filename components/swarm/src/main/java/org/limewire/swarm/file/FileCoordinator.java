package org.limewire.swarm.file;

import java.io.IOException;

import org.apache.http.nio.ContentDecoder;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

public interface FileCoordinator {

    /** Leases all available data. */
    Range lease();

    /** Leases just a portion of data, only allowing ranges in availableRanges. */
    Range leasePortion(IntervalSet availableRanges);

    /** Returns the size of the expected file. */
    long getSize();

    /** Returns a previously leased range. */
    void release(Range createRange);

    /**
     * Transfers data from the decoder, starting at start. Returns the amount
     * transferred.
     * 
     * @throws IOException
     */
    long transferFrom(ContentDecoder decoder, long start) throws IOException;

    /** Returns true if any range is available for leasing. */
    boolean isRangeAvailableForLease();
    
    /** Returns true if any ranges within available ranges are available for leasing. */
    boolean isRangeAvailableForLease(IntervalSet availableRanges);

}
