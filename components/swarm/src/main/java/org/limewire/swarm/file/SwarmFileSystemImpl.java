package org.limewire.swarm.file;

import java.util.concurrent.ExecutorService;

import org.limewire.collection.IntervalSet;
import org.limewire.swarm.SwarmDownload;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmListenerList;
import org.limewire.swarm.SwarmSelector;
import org.limewire.swarm.SwarmVerifier;

public class SwarmFileSystemImpl {
    /** The minimum block size to use. */
    private long minBlockSize;

    /** The complete size of the file. */
    private long completeSize;

    /** All ranges that are out on lease. */
    private IntervalSet leasedBlocks;

    /** The blocks that were written to disk. */
    private IntervalSet writtenBlocks;

    /** The blocks that were verified after being written to disk. */
    private IntervalSet verifiedBlocks;

    /** Blocks that are pending to be written to disk. */
    private IntervalSet pendingBlocks;

    /** The strategy for selecting new leased ranges. */
    private SwarmSelector blockChooser;

    /** The file writer. */
    private SwarmDownload swarmFile;

    /** The ExecutorService to use for writing. */
    private ExecutorService writeService;

    /** The file verifier. */
    private SwarmVerifier swarmFileVerifier;

    /** The amount of data that was lost to corruption. */
    private long amountLost;

    /** A simple lock. */
    private Object LOCK;

    private SwarmListenerList listeners;

    private SwarmFileSystem fileSystem;

    public SwarmFileSystemImpl(Object lock, SwarmListenerList listeners, SwarmFileSystem fileSystem) {
        LOCK = lock;
        this.listeners = listeners;
        this.fileSystem = fileSystem;
    }

    public long getMinBlockSize() {
        return minBlockSize;
    }

    public void setMinBlockSize(long minBlockSize) {
        this.minBlockSize = minBlockSize;
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
    }

    public IntervalSet getLeasedBlocks() {
        return leasedBlocks;
    }

    public void setLeasedBlocks(IntervalSet leasedBlocks) {
        this.leasedBlocks = leasedBlocks;
    }

    public IntervalSet getWrittenBlocks() {
        return writtenBlocks;
    }

    public void setWrittenBlocks(IntervalSet writtenBlocks) {
        this.writtenBlocks = writtenBlocks;
    }

    public IntervalSet getVerifiedBlocks() {
        return verifiedBlocks;
    }

    public void setVerifiedBlocks(IntervalSet verifiedBlocks) {
        this.verifiedBlocks = verifiedBlocks;
    }

    public IntervalSet getPendingBlocks() {
        return pendingBlocks;
    }

    public void setPendingBlocks(IntervalSet pendingBlocks) {
        this.pendingBlocks = pendingBlocks;
    }

    public SwarmSelector getBlockChooser() {
        return blockChooser;
    }

    public void setBlockChooser(SwarmSelector blockChooser) {
        this.blockChooser = blockChooser;
    }

    public SwarmDownload getSwarmFile() {
        return swarmFile;
    }

    public void setSwarmFile(SwarmDownload swarmFile) {
        this.swarmFile = swarmFile;
    }

    public ExecutorService getWriteService() {
        return writeService;
    }

    public void setWriteService(ExecutorService writeService) {
        this.writeService = writeService;
    }

    public SwarmVerifier getSwarmFileVerifier() {
        return swarmFileVerifier;
    }

    public void setSwarmFileVerifier(SwarmVerifier swarmFileVerifier) {
        this.swarmFileVerifier = swarmFileVerifier;
    }

    public long getAmountLost() {
        return amountLost;
    }

    public void setAmountLost(long amountLost) {
        this.amountLost = amountLost;
    }

    public Object getLOCK() {
        return LOCK;
    }

    public void setLOCK(Object lock) {
        LOCK = lock;
    }

    public SwarmListenerList getListeners() {
        return listeners;
    }

    public void setListeners(SwarmListenerList listeners) {
        this.listeners = listeners;
    }

    public SwarmFileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(SwarmFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
}