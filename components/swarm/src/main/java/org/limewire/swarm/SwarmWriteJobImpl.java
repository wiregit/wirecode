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

    private final SwarmCoordinator swarmCoordinator;

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
     * @param range - The total range we know we want to write with this job.
     * @param swarmCoordinator - The coordinator controlling disk access.
     * @param jobScheduler - The scheduler to use for asynchronous write tasks.
     * @param callback - A callback to the source controlling network data.
     */
    public SwarmWriteJobImpl(Range range, SwarmCoordinator swarmCoordinator,
            ExecutorService jobScheduler, SwarmWriteJobControl callback) {
        this.jobScheduler = jobScheduler;
        this.swarmCoordinator = swarmCoordinator;
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
            swarmCoordinator.unpending(range);
        }
    }

    /**
     * Schedules a write job to write the data to disk. It buffers first pulls
     * data out of the network buffer, then creates a write job to write the
     * data.
     * 
     * @param content - the content to write to disk.
     * @return the number of bytes to be written
     * 
     */
    public long write(final SwarmContent content) throws IOException {
        long written = 0;
        synchronized (scheduleLock) {

            final ByteBuffer networkUnblockingBuffer = byteBufferCache.getHeap(BUFFER_SIZE);
            written = content.read(networkUnblockingBuffer);
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
     * @param range - the range of data pending to be written.
     * @param buffer - buffer containing data to be written.
     */
    private void writeData(Range range, ByteBuffer buffer) throws IOException {
        //TODO cleanup the concept of the write range.
        synchronized (scheduleLock) {
            ByteBuffer networkUnblockingBuffer = byteBufferCache.getHeap(BUFFER_SIZE);
            try {

                networkUnblockingBuffer.put(buffer);
                byteBufferCache.release(buffer);
                networkUnblockingBuffer.flip();

                long bytesWritten = swarmCoordinator.write(writeRange, networkUnblockingBuffer);
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