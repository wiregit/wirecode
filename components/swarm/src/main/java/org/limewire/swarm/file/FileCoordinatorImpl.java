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
import org.limewire.swarm.AbstractSwarmCoordinator;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;
import org.limewire.swarm.SwarmWriteJobImpl;

/**
 * A {@link FileCoordinator} that verifies files using the given
 * {@link SwarmFileVerifier} and reads/writes the files using the given
 * {@link SwarmFileSystem}.
 * 
 * This implementation expects the writeService to use either in-place execution
 * or a single thread. If multiple threads are used, verifying may incorrectly
 * reverify multiple times.
 */
public class FileCoordinatorImpl extends AbstractSwarmCoordinator {

    private static final Log LOG = LogFactory.getLog(FileCoordinatorImpl.class);

    /** The minimum blocksize to lease. */
    private static final long DEFAULT_MIN_BLOCK_SIZE = 16 * 1024;

    /** The minimum block size to use. */
    private final long minBlockSize;

    /** All ranges that are out on lease. */
    private final IntervalSet leasedBlocks;

    /** The blocks that were written to disk. */
    private final IntervalSet writtenBlocks;

    /** The blocks that were verified after being written to disk. */
    private final IntervalSet verifiedBlocks;

    /** Blocks that are pending to be written to disk. */
    private final IntervalSet pendingBlocks;

    /** The strategy for selecting new leased ranges. */
    private final SwarmBlockSelector blockSelector;

    /** The file writer. */
    private final SwarmFileSystem fileSystem;

    /** The ExecutorService to use for writing. */
    private final ExecutorService writeService;

    /** The file verifier. */
    private final SwarmBlockVerifier swarmBlockVerifier;

    /** The amount of data that was lost to corruption. */
    private long amountLost;

    /** A simple lock. */
    private final Object LOCK = new Object();

    public FileCoordinatorImpl(SwarmFileSystem fileSystem, SwarmBlockVerifier swarmFileVerifier,
            ExecutorService writeService, SwarmBlockSelector selectionStrategy) {
        this(fileSystem, swarmFileVerifier, writeService, selectionStrategy, DEFAULT_MIN_BLOCK_SIZE);
    }

    public FileCoordinatorImpl(SwarmFileSystem fileSystem, SwarmBlockVerifier swarmFileVerifier,
            ExecutorService writeService, SwarmBlockSelector selectionStrategy, long minBlockSize) {
        this.leasedBlocks = new IntervalSet();
        this.writtenBlocks = new IntervalSet();
        this.pendingBlocks = new IntervalSet();
        this.verifiedBlocks = new IntervalSet();
        this.blockSelector = selectionStrategy;
        this.fileSystem = fileSystem;
        this.writeService = writeService;
        this.swarmBlockVerifier = swarmFileVerifier;
        this.minBlockSize = minBlockSize;
    }

    public Range lease() {
        return lease(null, fileSystem.getCompleteSize(), blockSelector);
    }

    public Range leasePortion(IntervalSet availableRanges) {
        // Lease modifies, so clone.
        if (availableRanges != null)
            availableRanges = availableRanges.clone();
        return lease(availableRanges, Math.max(minBlockSize, swarmBlockVerifier.getBlockSize()),
                blockSelector);
    }

    public Range leasePortion(IntervalSet availableRanges, SwarmBlockSelector swarmSelector) {
        // Lease modifies, so clone.
        if (availableRanges != null)
            availableRanges = availableRanges.clone();
        return lease(availableRanges, Math.max(minBlockSize, swarmBlockVerifier.getBlockSize()),
                blockSelector);
    }

    protected Range lease(IntervalSet availableRanges, long blockSize,
            SwarmBlockSelector swarmSelector) {
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
            // assert hasLease(range); there are valid times where unless will
            // be called and we don't have the lease
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
        listeners().blockLeased(chosen);
    }

    private boolean hasLease(Range range) {
        return leasedBlocks.contains(range);
    }

    private void addPending(Range range) {
        pendingBlocks.add(range);
        listeners().blockPending(range);
    }

    private void deleteLease(Range range) {
        leasedBlocks.delete(range);
        listeners().blockUnleased(range);
    }

    private void deletePending(Range range) {
        pendingBlocks.delete(range);
        listeners().blockUnpending(range);
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
            verifiableRanges = swarmBlockVerifier.scanForVerifiableRanges(writtenBlocks, fileSystem
                    .getCompleteSize());
            complete = verifiableRanges.isEmpty() && isComplete();
        }

        listeners().blockWritten(writtenRange);

        if (complete) {
            listeners().downloadCompleted(fileSystem);
        }
        verifyRanges(verifiableRanges);
    }

    /**
     * Returns true if this is complete either because all data is in
     * writtenBlocks, or all data is in verifiedBlocks.
     * 
     * LOCK must be held while calling this.
     */
    public boolean isComplete() {
        synchronized (LOCK) {
            IntervalSet blocksToCheck = null;
            if (verifiedBlocks.isEmpty()) {
                blocksToCheck = writtenBlocks;
            } else if (writtenBlocks.isEmpty()) {
                blocksToCheck = verifiedBlocks;
            }

            return blocksToCheck != null && blocksToCheck.getNumberOfIntervals() == 1
                    && blocksToCheck.getSize() == fileSystem.getCompleteSize();
        }
    }

    private void verifyRanges(List<Range> verifiableRanges) {
        if (LOG.isDebugEnabled() && !verifiableRanges.isEmpty()) {
            LOG.debug("Verifying ranges: " + verifiableRanges);
        }

        boolean complete = false;
        for (Range rangeToVerify : verifiableRanges) {
            boolean verified = swarmBlockVerifier.verify(rangeToVerify, fileSystem);
            synchronized (LOCK) {
                assert writtenBlocks.contains(rangeToVerify);
                writtenBlocks.delete(rangeToVerify);
                if (verified) {
                    verifiedBlocks.add(rangeToVerify);
                    complete = isComplete();
                    listeners().blockVerified(rangeToVerify);
                    handleVerifiedPieces();
                } else {
                    listeners().blockVerificationFailed(rangeToVerify);
                    // TODO: Add a toggle for keeping lost ranges, and do not
                    // count if doing a 'full scan'.
                    amountLost += rangeToVerify.getHigh() - rangeToVerify.getLow() + 1;

                    if (LOG.isDebugEnabled())
                        LOG.debug("Lost range: " + rangeToVerify + ", total lost: " + amountLost);
                }
            }
        }

        if (complete) {
            listeners().downloadCompleted(fileSystem);
        }
    }

    private void handleVerifiedPieces() {
        List<SwarmFile> swarmFiles = fileSystem.getSwarmFiles();
        for (SwarmFile swarmFile : swarmFiles) {
            Range fileRange = Range.createRange(swarmFile.getStartByte(), swarmFile.getEndByte());
            if (verifiedBlocks.contains(fileRange)) {
                try {
                    fileSystem.closeSwarmFile(swarmFile);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    // TODO need to figure out what we will do in this case,
                    // most likely jsut log the message
                    e.printStackTrace();
                }
            }
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
                verifiableRanges = swarmBlockVerifier.scanForVerifiableRanges(writtenBlocks,
                        fileSystem.getCompleteSize());
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
                verifiableRanges = swarmBlockVerifier.scanForVerifiableRanges(writtenBlocks,
                        fileSystem.getCompleteSize());
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
            availableRanges = IntervalSet.createSingletonSet(0, fileSystem.getCompleteSize() - 1);

        // Figure out which blocks we still need to assign
        if (neededBytes == null) {
            neededBytes = IntervalSet.createSingletonSet(0, fileSystem.getCompleteSize() - 1);
        } else {
            neededBytes.add(Range.createRange(0, fileSystem.getCompleteSize() - 1));
        }

        synchronized (LOCK) {
            neededBytes.delete(leasedBlocks);
            neededBytes.delete(writtenBlocks);
            neededBytes.delete(pendingBlocks);
            neededBytes.delete(verifiedBlocks);
        }
        // Calculate the intersection of neededBytes and availableBytes
        availableRanges.delete(neededBytes.invert(fileSystem.getCompleteSize()));
        return availableRanges;
    }

    public SwarmWriteJob createWriteJob(Range range, SwarmWriteJobControl callback) {
        return new SwarmWriteJobImpl(range, this, writeService, callback);
    }

    public long write(Range range, ByteBuffer swarmContent) throws IOException {
        // TODO unlock certain portions allow multiple writes at teh same time
        synchronized (LOCK) {
            long position = range.getLow();
            long startRange = range.getLow();
            long endRange = startRange - swarmContent.position() + swarmContent.limit() - 1;
            Range pendingRange = Range.createRange(startRange, endRange);
            pending(pendingRange);
            long bytesWritten = fileSystem.write(swarmContent, position);
            wrote(pendingRange);
            return bytesWritten;
        }
    }

    public void finish() throws IOException {
        synchronized (LOCK) {
            // TODO handle possible running write jobs.
            // TODO we can cancel the running write jobs through the scheduler
            fileSystem.close();
        }
    }

    public Range renewLease(Range oldLease, Range newLease) {
        synchronized (LOCK) {
            assert hasLease(oldLease);
            assert newLease.isSubrange(oldLease);
            deleteLease(oldLease);
            addLease(newLease);
            return newLease;
        }

    }

    public SwarmFile getSwarmFile(Range range) {
        return fileSystem.getSwarmFile(range.getLow());
    }

    public SwarmFileSystem getSwarmFileSystem() {
        return fileSystem;
    }

    @Override
    public String toString() {
        StringBuffer toString = new StringBuffer();
        toString.append("expected: ").append("0-").append(fileSystem.getCompleteSize()-1).append("\n");
        toString.append("leased: " + leasedBlocks).append("\n");
        toString.append("pending: " + pendingBlocks).append("\n");
        toString.append("written: " + writtenBlocks).append("\n");
        toString.append("verified: " + verifiedBlocks).append("\n");
        return toString.toString();
    }

}
