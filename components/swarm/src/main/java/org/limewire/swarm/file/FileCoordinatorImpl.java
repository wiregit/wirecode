package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCache;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmDownload;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmIOControl;
import org.limewire.swarm.SwarmListener;
import org.limewire.swarm.SwarmListenerList;
import org.limewire.swarm.SwarmSelector;
import org.limewire.swarm.SwarmVerifier;
import org.limewire.swarm.SwarmWriteJob;

/**
 * A {@link FileCoordinator} that verifies files using the given
 * {@link SwarmFileVerifier} and reads/writes the files using the given
 * {@link SwarmDownload}.
 * 
 * This implementation expects the writeService to use either in-place execution
 * or a single thread. If multiple threads are used, verifying may incorrectly
 * reverify multiple times.
 */
public class FileCoordinatorImpl implements SwarmCoordinator {

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
    private final SwarmSelector blockChooser;

    /** The file writer. */
    private final SwarmDownload swarmFile;

    /** The ExecutorService to use for writing. */
    private final ExecutorService writeService;

    /** The file verifier. */
    private final SwarmVerifier swarmFileVerifier;

    /** The amount of data that was lost to corruption. */
    private long amountLost;

    /** A simple lock. */
    private final Object LOCK = new Object();

    private final SwarmListenerList listeners = new SwarmListenerList(this);

    private SwarmFileSystem fileSystem = null;

    /** List of listeners. */

    public FileCoordinatorImpl(long size, SwarmDownload swarmFile, SwarmVerifier swarmFileVerifier,
            ExecutorService writeService, SwarmSelector selectionStrategy) {
        this(size, swarmFile, swarmFileVerifier, writeService, selectionStrategy,
                DEFAULT_MIN_BLOCK_SIZE);
    }

    public FileCoordinatorImpl(long size, SwarmDownload swarmFile, SwarmVerifier swarmFileVerifier,
            ExecutorService writeService, SwarmSelector selectionStrategy, long minBlockSize) {
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

    public void addListener(SwarmListener swarmListener) {
        listeners.add(swarmListener);
    }

    public long getCompleteFileSize() {
        return completeSize;
    }

    public Range lease() {
        return lease(null, completeSize, blockChooser);
    }

    public Range leasePortion(IntervalSet availableRanges) {
        // Lease modifies, so clone.
        if (availableRanges != null)
            availableRanges = availableRanges.clone();
        return lease(availableRanges, Math.max(minBlockSize, swarmFileVerifier.getBlockSize()),
                blockChooser);
    }

    public Range leasePortion(IntervalSet availableRanges, SwarmSelector swarmSelector) {
        // Lease modifies, so clone.
        if (availableRanges != null)
            availableRanges = availableRanges.clone();
        return lease(availableRanges, Math.max(minBlockSize, swarmFileVerifier.getBlockSize()),
                blockChooser);
    }

    protected Range lease(IntervalSet availableRanges, long blockSize, SwarmSelector swarmSelector) {
        IntervalSet neededBytes = new IntervalSet();
        availableRanges = getAvailableRangesForLease(availableRanges, neededBytes);

        if (availableRanges.isEmpty()) {
            return null;
        }

        // Pick a range, add it to leased, and exit.
        Range chosen;
        try {
            chosen = swarmSelector.pickAssignment(availableRanges, neededBytes, blockSize);
        } catch (NoSuchElementException nsee) {
            return null;
        }

        synchronized (LOCK) {
            addLease(chosen);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Leasing: " + chosen + ", from available: " + availableRanges + ", needed: "
                    + neededBytes);
        return chosen;
    }

    public void unlease(Range range) {
        synchronized (LOCK) {
            assert hasLease(range);
            deleteLease(range);
        }
    }

    public void pending(Range range) {
        synchronized (LOCK) {
            assert hasLease(range);
            deleteLease(range);
            addPending(range);
        }
    }

    private void addLease(Range chosen) {
        leasedBlocks.add(chosen);
    }

    private boolean hasLease(Range range) {
        return leasedBlocks.contains(range);
    }

    private void addPending(Range range) {
        pendingBlocks.add(range);
    }

    private void deleteLease(Range range) {
        leasedBlocks.delete(range);
    }

    private void deletePending(Range range) {
        pendingBlocks.delete(range);
    }

    private boolean hasPending(Range range) {
        return pendingBlocks.contains(range);
    }

    private void addWritten(Range writtenRange) {
        writtenBlocks.add(writtenRange);
    }

    public void unpending(Range range) {
        synchronized (LOCK) {
            assert hasPending(range);
            deletePending(range);
        }
    }

    public void wrote(Range writtenRange) {
        List<Range> verifiableRanges;
        boolean complete;
        synchronized (LOCK) {
            assert hasPending(writtenRange);
            deletePending(writtenRange);
            addWritten(writtenRange);
            verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks,
                    completeSize);
            complete = verifiableRanges.isEmpty() && isComplete();
        }

        if (complete) {
            listeners.downloadCompleted(swarmFile);
        }

        verifyRanges(verifiableRanges);
    }

    /**
     * Returns true if this is complete either because all data is in
     * writtenBlocks, or all data is in verifiedBlocks.
     * 
     * LOCK must be held while calling this.
     */
    private boolean isComplete() {
        IntervalSet blocksToCheck = null;
        if (verifiedBlocks.isEmpty()) {
            blocksToCheck = writtenBlocks;
        } else if (writtenBlocks.isEmpty()) {
            blocksToCheck = verifiedBlocks;
        }

        return blocksToCheck != null && blocksToCheck.getNumberOfIntervals() == 1
                && blocksToCheck.getSize() == completeSize;
    }

    private void verifyRanges(List<Range> verifiableRanges) {
        if (LOG.isDebugEnabled() && !verifiableRanges.isEmpty()) {
            LOG.debug("Verifying ranges: " + verifiableRanges);
        }

        boolean complete = false;
        for (Range rangeToVerify : verifiableRanges) {
            boolean verified = swarmFileVerifier.verify(rangeToVerify, swarmFile);
            synchronized (LOCK) {
                assert writtenBlocks.contains(rangeToVerify);
                writtenBlocks.delete(rangeToVerify);
                if (verified) {
                    verifiedBlocks.add(rangeToVerify);
                    complete = isComplete();
                } else {
                    // TODO: Add a toggle for keeping lost ranges, and do not
                    // count if doing a 'full scan'.
                    amountLost += rangeToVerify.getHigh() - rangeToVerify.getLow() + 1;

                    if (LOG.isDebugEnabled())
                        LOG.debug("Lost range: " + rangeToVerify + ", total lost: " + amountLost);
                }
            }
        }

        if (complete) {
            listeners.downloadCompleted(swarmFile);
        }
    }

    public long getAmountVerified() {
        synchronized (LOCK) {
            return verifiedBlocks.getSize();
        }
    }

    public long getAmountLost() {
        synchronized (LOCK) {
            return amountLost;
        }
    }

    // TODO: enable support for scanning the existing ranges on disk
    public void reverify() {
        final List<Range> verifiableRanges;
        synchronized (LOCK) {
            writtenBlocks.add(verifiedBlocks);
            verifiedBlocks.clear();
            // As an optimization, only scan for ranges if we have no pending
            // blocks.
            // (This works because a pending range implies wrote will be called,
            // and wrote will trigger a verification.)
            if (pendingBlocks.getNumberOfIntervals() == 0 && !writtenBlocks.isEmpty()) {
                verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks,
                        completeSize);
            } else {
                verifiableRanges = Collections.emptyList();
            }
        }

        if (!verifiableRanges.isEmpty()) {
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
        synchronized (LOCK) {
            // As an optimization, only scan for ranges if we have no pending
            // blocks.
            // (This works because a pending range implies wrote will be called,
            // and wrote will trigger a verification.)
            if (pendingBlocks.getNumberOfIntervals() == 0 && !writtenBlocks.isEmpty()) {
                verifiableRanges = swarmFileVerifier.scanForVerifiableRanges(writtenBlocks,
                        completeSize);
            } else {
                verifiableRanges = Collections.emptyList();
            }
        }

        if (!verifiableRanges.isEmpty()) {
            writeService.execute(new Runnable() {
                public void run() {
                    verifyRanges(verifiableRanges);
                }
            });
        }
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
    protected IntervalSet getAvailableRangesForLease(IntervalSet availableRanges,
            IntervalSet neededBytes) {
        if (availableRanges == null)
            availableRanges = IntervalSet.createSingletonSet(0, completeSize - 1);

        // Figure out which blocks we still need to assign
        if (neededBytes == null) {
            neededBytes = IntervalSet.createSingletonSet(0, completeSize - 1);
        } else {
            neededBytes.add(Range.createRange(0, completeSize - 1));
        }

        synchronized (LOCK) {
            neededBytes.delete(leasedBlocks);
            neededBytes.delete(writtenBlocks);
            neededBytes.delete(pendingBlocks);
            neededBytes.delete(verifiedBlocks);
        }

        // Calculate the intersection of neededBytes and availableBytes
        availableRanges.delete(neededBytes.invert(completeSize));
        return availableRanges;
    }

    // public SwarmWriteJob newWriteJob(long position, SwarmIOControl ioctrl) {
    // return new FileCoordinatorWriteJobImpl(position, ioctrl, writeService,
    // this,
    // new ByteBufferCache(), swarmFile);
    // }

    public SwarmWriteJob write(Range range, SwarmContent swarmContent) {
        synchronized (LOCK) {
            long position = range.getLow();

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 16);
            int numRead = 0;
            // TODO verify
            try {
                while ((numRead = swarmContent.read(byteBuffer)) != -1) {
                    int bufferPosition = byteBuffer.position();
                    Range pendingRange = Range.createRange(position, position + bufferPosition);

                    pending(pendingRange);
                    byteBuffer.flip();
                    swarmFile.transferFrom(byteBuffer, position);
                    wrote(pendingRange);
                    byteBuffer.clear();
                    position += bufferPosition;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    }

}
