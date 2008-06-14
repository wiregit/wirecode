package org.limewire.swarm.file;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

/**
 * A coordinator for multiple sources writing a single file.
 * In order to cooperate about which ranges are required, a source
 * must first lease a range with either {@link #lease()} or {@link #leasePortion(IntervalSet)}.
 * Once leased, the source can mark the range as retrieved and pending a write
 * with {@link #pending(Range)}.  Once pending, a source can write the data
 * with {@link #write(ContentDecoder, long)}.
 * 
 * At any step, if the rules are not followed, an assertion error will
 * be thrown.
 */
public interface FileCoordinator {

    /**
     * Leases all available data.
     * If no data is available for leasing, this returns null.
     */
    Range lease();

    /**
     * Leases just a portion of data, only allowing ranges in availableRanges.
     * If no data is available for leasing, this returns null.
     */
    Range leasePortion(IntervalSet availableRanges);

    /** Returns the size of the expected file. */
    long getSize();

    /** 
     * Returns a previously leased range.
     * This will allow other sources to lease that range.
     */
    void unlease(Range range);

    /** Returns true if any range is available for leasing. */
    boolean isRangeAvailableForLease();
    
    /** Returns true if any ranges within available ranges are available for leasing. */
    boolean isRangeAvailableForLease(IntervalSet availableRanges);

    /**
     * Constructs a new WriteJob that will be used to write data.
     * As data becomes available, {@link WriteJob#consumeContent(ContentDecoder)}
     * can be called, informing the job that data is available.
     * If the job cannot consume data, it will suspend I/O.  When the job
     * can begin consuming data again, it will resume I/O.
     * 
     * It is expected that the job will call {@link #pending(Range)} when
     * it consumed content, and will eventually call {@link #write(ContentDecoder, long)}.
     */
    WriteJob newWriteJob(long position, IOControl ioctrl);

    /**
     * Marks a range as pending a write.
     * The range is still not leasable, but now cannot be unleased.
     * To release a pending range (without writing it), use {@link #unpending(Range)}.
     */
    void pending(Range range);

    /**
     * Marks a range as no longer pending a write.
     * The range is re-available for leasing.
     */
    void unpending(Range range);

    /**
     * Signals a range as being written.
     * It is removed from pending and unavailable for future leases.
     */
    void wrote(Range range);
}
