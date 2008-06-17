package org.limewire.swarm.file;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.IOControl;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCacheImpl;

/**
 * A {@link FileCoordinator} that verifies files using the given
 * {@link SwarmFileVerifier} and reads/writes the files using the
 * given {@link SwarmFile}.
 * 
 * This implementation expects the writeService to use either in-place
 * execution or a single thread.  If multiple threads are used,
 * verifying may incorrectly reverify multiple times.
 */
public class FileCoordinatorImpl implements FileCoordinator {
    
    private static final Log LOG = LogFactory.getLog(FileCoordinatorImpl.class);
    
    /** The minimum blocksize to lease. */
    private static final long DEFAULT_MIN_BLOCK_SIZE = 16 * 1024;
    
    /** The minimum block size to use. */
    private final long minBlockSize;
    
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
    
    /** List of listeners. */
    private final CopyOnWriteArrayList<SwarmFileCompletionListener> listeners =
        new CopyOnWriteArrayList<SwarmFileCompletionListener>();

    public FileCoordinatorImpl(long size, SwarmFile swarmFile, SwarmFileVerifier swarmFileVerifier,
            ExecutorService writeService, SelectionStrategy selectionStrategy) {
        this(size, swarmFile, swarmFileVerifier, writeService, selectionStrategy, DEFAULT_MIN_BLOCK_SIZE);
    }
    

    public FileCoordinatorImpl(long size, SwarmFile swarmFile, SwarmFileVerifier swarmFileVerifier,
            ExecutorService writeService, SelectionStrategy selectionStrategy, long minBlockSize) {
        this.completeSize = size;
        this.leasedBlocks = new IntervalSet();
        this.writtenBlocks = new IntervalSet();
        this.pendingBlocks = new IntervalSet();
        this.verifiedBlocks = new IntervalSet();
        this.blockChooser = selectionStrategy;
        this.swarmFile = swarmFile;
        this.writeService = writeService;
        this.swarmFileVerifier = swarmFileVerifier;
        this.minBlockSize = minBlockSize;
    }
    
    public void addCompletionListener(SwarmFileCompletionListener swarmFileCompletionListener) {
        listeners.add(swarmFileCompletionListener);
    }

    public long getCompleteFileSize() {
        return completeSize;
    }

    public Range lease() {
        return lease(null, completeSize);
    }

    public Range leasePortion(IntervalSet availableRanges) {
        return lease(availableRanges, Math.max(minBlockSize, swarmFileVerifier.getBlockSize()));
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
        List<Range> verifiableRanges;
        boolean complete;
        synchronized(LOCK) {
            assert pendingBlocks.contains(writtenRange);
            pendingBlocks.delete(writtenRange);
            writtenBlocks.add(writtenRange);
            verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks, completeSize);
            complete = verifiableRanges.isEmpty() && verifiedBlocks.isEmpty() && writtenBlocks.getNumberOfIntervals() == 1 && writtenBlocks.getSize() == completeSize;
        }
        
        if(complete) {
            for(SwarmFileCompletionListener listener : listeners) {
                listener.fileCompleted(this, swarmFile);
            }
        }
        verifyRanges(verifiableRanges);
    }
    
    private void verifyRanges(List<Range> verifiableRanges) {
        if(LOG.isDebugEnabled() && !verifiableRanges.isEmpty()) {
            LOG.debug("Verifying ranges: " + verifiableRanges);
        }
        
        for(Range rangeToVerify : verifiableRanges) {
            boolean verified = swarmFileVerifier.verify(rangeToVerify, swarmFile);
            synchronized(LOCK) {
                assert writtenBlocks.contains(rangeToVerify);
                writtenBlocks.delete(rangeToVerify);
                if(verified) {
                    verifiedBlocks.add(rangeToVerify);
                } else {
                    // TODO: Add a toggle for keeping lost ranges, and do not
                    //       count if doing a 'full scan'.
                    amountLost += rangeToVerify.getHigh() - rangeToVerify.getLow() + 1;
                    
                    if(LOG.isDebugEnabled())
                        LOG.debug("Lost range: " + rangeToVerify + ", total lost: " + amountLost);
                }
            }
        }
    }
    
    public long getAmountVerified() {
        synchronized(LOCK) {
            return verifiedBlocks.getSize();
        }
    }
    
    // TODO: enable support for scanning the existing ranges on disk
    public void reverify() {
        final List<Range> verifiableRanges;
        synchronized(LOCK) {
            writtenBlocks.add(verifiedBlocks);
            verifiedBlocks.clear();
            // As an optimization, only scan for ranges if we have no pending blocks.
            // (This works because a pending range implies wrote will be called,
            //  and wrote will trigger a verification.)
            if(pendingBlocks.getNumberOfIntervals() == 0) {
                verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks, completeSize);
            } else {
                verifiableRanges = Collections.emptyList();
            }
        }
        
        if(!verifiableRanges.isEmpty()) {
            writeService.execute(new Runnable() {
                public void run() {
                    verifyRanges(verifiableRanges);
                }
            });
        }
    }
    
    // TODO: enable support for scanning the existing ranges on disk
    public void verify() {
        final List<Range> verifiableRanges;
        synchronized(LOCK) {
            // As an optimization, only scan for ranges if we have no pending blocks.
            // (This works because a pending range implies wrote will be called,
            //  and wrote will trigger a verification.)
            if(pendingBlocks.getNumberOfIntervals() == 0) {
                verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks, completeSize);
            } else {
                verifiableRanges = Collections.emptyList();
            }
        }
        
        if(!verifiableRanges.isEmpty()) {
            writeService.execute(new Runnable() {
                public void run() {
                    verifyRanges(verifiableRanges);
                }
            });
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
