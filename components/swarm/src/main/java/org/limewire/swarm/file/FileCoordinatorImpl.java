package org.limewire.swarm.file;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.IOControl;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCacheImpl;


public class FileCoordinatorImpl implements FileCoordinator {
    
    private static final Log LOG = LogFactory.getLog(FileCoordinatorImpl.class);
    
    /** The minimum blocksize to lease. */
    private static final long MIN_BLOCK_SIZE = 16 * 1024;
    
    /** The complete size of the file. */
    private final long completeSize;
    
    /** All ranges that are out on lease. */
    private final IntervalSet leasedBlocks;
    
    /** The blocks that were written to disk. */
    private final IntervalSet writtenBlocks;
    
    /** The blocks that were verified after being written to disk. */
    private final IntervalSet verifiedBlocks;
    
    /** Blocks that are pending to be written to disk. */
    private final IntervalSet pendingBlocks;
    
    /** The strategy for selecting new leased ranges. */
    private final SelectionStrategy blockChooser;
    
    /** The file writer. */
    private final SwarmFile swarmFile;
    
    /** The ExecutorService to use for writing. */
    private final ExecutorService writeService;
    
    /** The file verifier. */
    private final SwarmFileVerifier swarmFileVerifier;
    
    /** The amount of data that was lost to corruption. */
    private long amountLost;
    
    /** A simple lock. */
    private final Object LOCK = new Object();


    public FileCoordinatorImpl(long size, SwarmFile swarmFile, SwarmFileVerifier swarmFileVerifier,
            ExecutorService writeService) {
        this.completeSize = size;
        this.leasedBlocks = new IntervalSet();
        this.writtenBlocks = new IntervalSet();
        this.pendingBlocks = new IntervalSet();
        this.verifiedBlocks = new IntervalSet();
        this.blockChooser = new ContiguousSelectionStrategy(size);
        this.swarmFile = swarmFile;
        this.writeService = writeService;
        this.swarmFileVerifier = swarmFileVerifier;
    }

    public long getSize() {
        return completeSize;
    }

    public Range lease() {
        return lease(null, completeSize);
    }

    public Range leasePortion(IntervalSet availableRanges) {
        return lease(availableRanges, Math.max(MIN_BLOCK_SIZE, swarmFileVerifier.getBlockSize()));
    }

    public void unlease(Range range) {
        synchronized(LOCK) {
            assert leasedBlocks.contains(range);
            leasedBlocks.delete(range);
        }
    }
    
    public void pending(Range range) {
        synchronized(LOCK) {
            assert leasedBlocks.contains(range);
            leasedBlocks.delete(range);
            pendingBlocks.add(range);
        }
    }
    
    public void unpending(Range range) {
        synchronized(LOCK) {
            assert pendingBlocks.contains(range);
            pendingBlocks.delete(range);
        }
    }
    
    public void wrote(Range writtenRange) {
        List<Range> verifiableRanges = Collections.emptyList();
        synchronized(LOCK) {
            assert pendingBlocks.contains(writtenRange);
            pendingBlocks.delete(writtenRange);
            writtenBlocks.add(writtenRange);
            verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks, completeSize);
        }
        
        for(Range rangeToVerify : verifiableRanges) {
            boolean verified = swarmFileVerifier.verify(rangeToVerify, swarmFile);
            synchronized(LOCK) {
                assert writtenBlocks.contains(rangeToVerify);
                writtenBlocks.delete(rangeToVerify);
                if(verified) {
                    verifiedBlocks.add(rangeToVerify);
                } else {
                    // TODO: Add a toggle for keeping lost ranges.
                    amountLost += rangeToVerify.getHigh() - rangeToVerify.getLow() + 1;
                }
            }
        }
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
        
        synchronized(LOCK) {
            leasedBlocks.add(chosen);
        }
            
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
        
        synchronized(LOCK) {
            neededBytes.delete(leasedBlocks);
            neededBytes.delete(writtenBlocks);
            neededBytes.delete(pendingBlocks);
            neededBytes.delete(verifiedBlocks);
        }    
        
        // Calculate the intersection of neededBytes and availableBytes
        availableRanges.delete(neededBytes.invert(completeSize));        
        return availableRanges;
    }
    
    public WriteJob newWriteJob(long position, IOControl ioctrl) {
        return new FileCoordinatorWriteJobImpl(position, ioctrl, writeService, this, new ByteBufferCacheImpl(), swarmFile);
    }

}
