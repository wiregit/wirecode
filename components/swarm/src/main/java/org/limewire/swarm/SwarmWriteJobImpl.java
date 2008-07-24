package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCache;

/**
 * 
 * 
 *
 */
public class SwarmWriteJobImpl implements SwarmWriteJob {

    private static final Log LOG = LogFactory.getLog(SwarmWriteJobImpl.class);

    private final ExecutorService jobScheduler;

    private final SwarmCoordinator fileCoordinator;

    private final Object scheduleLock = new Object();

    private Future<Void> scheduledJob;

    private final Range range;

    private Range writeRange = null;

    private final SwarmWriteJobControl callback;

    private final ByteBufferCache byteBufferCache;

    private static final int BUFFER_SIZE = 8192;

    /**
     * Constructor for a SwarmWriteJobImpl, it will write the data for a given
     * range of bytes, by making subsequent calls to this jobs write method.
     * 
     * @param range
     * @param fileCoordinator
     * @param jobScheduler
     * @param callback
     */
    public SwarmWriteJobImpl(Range range, SwarmCoordinator fileCoordinator,
            ExecutorService jobScheduler, SwarmWriteJobControl callback) {
        this.jobScheduler = jobScheduler;
        this.fileCoordinator = fileCoordinator;
        this.range = range;
        this.writeRange = range;
        this.callback = callback;
        this.byteBufferCache = new ByteBufferCache();
    }

    /**
     * Cancels this write job and any pending scheduled tasks.
     */
    public void cancel() {
        synchronized (scheduleLock) {
            LOG.debug("Cancelling Write Job");
            if (scheduledJob != null) {
                scheduledJob.cancel(false);
                scheduledJob = null;
            }
            fileCoordinator.unpending(range);
        }
    }

    /**
     * Schedules a write job to write the data to disk.
     * 
     * @param content
     * @return the number of bytes to be written
     * 
     */
    public long write(final SwarmContent content) throws IOException {
        long written = 0;
        // TODO do something with the written variable.

        synchronized (scheduleLock) {

            final ByteBuffer networkUnblockingBuffer = byteBufferCache.getHeap(BUFFER_SIZE);
            content.read(networkUnblockingBuffer);
            networkUnblockingBuffer.flip();

            scheduledJob = jobScheduler.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    writeData(range, networkUnblockingBuffer);
                    return null;
                }
            });
        }
        return written;
    }

    /**
     * Writes data from the buffer into a temporary buffer, and then to disk.
     * This roundabout way of writing is required to ensure that no shared lock
     * is used both during disk & network I/O. (Disk I/O is inherently blocking,
     * and can stall network I/O otherwise.)
     * 
     * @param range
     * 
     * @param buffer
     */
    private void writeData(Range range, ByteBuffer buffer) throws IOException {
        synchronized (scheduleLock) {
            ByteBuffer networkUnblockingBuffer = byteBufferCache.getHeap(BUFFER_SIZE);
            try {

                networkUnblockingBuffer.put(buffer);
                byteBufferCache.release(buffer);
                networkUnblockingBuffer.flip();

                long bytesWritten = fileCoordinator.write(writeRange, networkUnblockingBuffer);
                long newLow = writeRange.getLow() + bytesWritten;
                long newHigh = writeRange.getHigh();

                LOG.trace("Re-requesting I/O");
                callback.resume();

                if (newLow <= newHigh) {
                    Range newRange = Range.createRange(newLow, newHigh);
                    this.writeRange = newRange;
                }
            } finally {
                byteBufferCache.release(networkUnblockingBuffer);
                callback.resume();
            }
        }

    }
}