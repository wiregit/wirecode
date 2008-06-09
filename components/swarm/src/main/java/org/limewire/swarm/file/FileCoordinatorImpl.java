package org.limewire.swarm.file;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.ContentDecoder;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.RandomDownloadStrategy;
import com.limegroup.gnutella.downloader.SelectionStrategy;

public class FileCoordinatorImpl implements FileCoordinator {
    
    private static final Log LOG = LogFactory.getLog(FileCoordinatorImpl.class);
    
    /** The complete size of the file. */
    private final long completeSize;
    
    /** All ranges that are out on lease. */
    private IntervalSet leasedBlocks;
    
    /** The blocks that were written to disk. */
    private IntervalSet writtenBlocks; 
    
    /** The strategy for selecting new leased ranges. */
    private SelectionStrategy blockChooser;
    
    /** The file writer. */
    private SwarmFileWriter fileWriter;

    public FileCoordinatorImpl(long size, SwarmFileWriter fileWriter) {
        this.completeSize = size;
        this.leasedBlocks = new IntervalSet();
        this.writtenBlocks = new IntervalSet();
        this.blockChooser = new RandomDownloadStrategy(size);
        this.fileWriter = fileWriter;
    }

    public long getSize() {
        return completeSize;
    }

    public Range lease() {
        return lease(null, completeSize);
    }

    public Range leasePortion(IntervalSet availableRanges) {
        return lease(availableRanges, getBlockSize());
    }

    public void release(Range range) {
        assert leasedBlocks.contains(range);
        leasedBlocks.delete(range);
    }

    public long transferFrom(ContentDecoder decoder, long start) throws IOException {
        long wrote = fileWriter.transferFrom(decoder, start);
        if(wrote > 0) {
            Range range = Range.createRange(start, start+wrote-1);
            writtenBlocks.add(range);
            leasedBlocks.delete(range);
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace("Wrote: " + wrote);
        
        return wrote;
    }
    
    protected long getBlockSize() {
        return 10240;
    }
    
    protected Range lease(IntervalSet availableRanges, long blockSize) {
        IntervalSet neededBytes = new IntervalSet();
        availableRanges = getAvailableRangesForLease(availableRanges, neededBytes);
        
        // Pick a range, add it to leased, and exit.
        Range chosen;
        try {
            chosen = blockChooser.pickAssignment(availableRanges, neededBytes, blockSize);
        } catch(NoSuchElementException nsee) {
            return null;
        }
        
        leasedBlocks.add(chosen);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Leasing: " + chosen + ", from available: " + availableRanges + ", needed: " + neededBytes);
        return chosen;
    }
    
    public boolean isRangeAvailableForLease() {
        return isRangeAvailableForLease(null);
    }
    
    public boolean isRangeAvailableForLease(IntervalSet availableRanges) {
        return !getAvailableRangesForLease(availableRanges, null).isEmpty();
    }
    
    /**
     * Mutates availableRanges to leave only the blocks left that can be leased.
     * If availableRanges is null, this assumes everything is available.
     * 
     * Also mutates neededBytes, to leave only what is needed.
     **/
    protected IntervalSet getAvailableRangesForLease(IntervalSet availableRanges, IntervalSet neededBytes) {
        if(availableRanges == null)
            availableRanges = IntervalSet.createSingletonSet(0, completeSize-1);
        
        // Figure out which blocks we still need to assign
        if(neededBytes == null) {
            neededBytes = IntervalSet.createSingletonSet(0, completeSize-1);
        } else {
            neededBytes.add(Range.createRange(0, completeSize-1));
        }
        
        neededBytes.delete(leasedBlocks);
        neededBytes.delete(writtenBlocks);
        
        // Calculate the intersection of neededBytes and availableBytes
        availableRanges.delete(neededBytes.invert(completeSize));
        return availableRanges;
    }

}
