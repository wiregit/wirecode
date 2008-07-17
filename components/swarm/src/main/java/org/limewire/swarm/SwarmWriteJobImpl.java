package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;

public class SwarmWriteJobImpl implements SwarmWriteJob {

    private static final Log LOG = LogFactory.getLog(SwarmWriteJobImpl.class);

    private final ExecutorService jobScheduler;

    private final SwarmCoordinator fileCoordinator;

    private final Object scheduleLock = new Object();

    private Future<Void> scheduledJob;

    private final Range range;

    private final SwarmWriteJobCallBack callback;

    public SwarmWriteJobImpl(Range range, SwarmCoordinator fileCoordinator,
            ExecutorService jobScheduler, SwarmWriteJobCallBack callback) {
        this.jobScheduler = jobScheduler;
        this.fileCoordinator = fileCoordinator;
        this.range = range;
        this.callback = callback;
    }

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

    public long write(final SwarmContent content) throws IOException {
        synchronized (scheduleLock) {
           
            scheduledJob = jobScheduler.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    writeData(range, content);
                    return null;
                }
            });

            
            return 0;
        }
    }

    /**
     * Writes data from the buffer into a temporary buffer, and then to disk.
     * This roundabout way of writing is required to ensure that no shared lock
     * is used both during disk & network I/O. (Disk I/O is inherently blocking,
     * and can stall network I/O otherwise.)
     * @param range 
     * 
     * @param content
     */
    private void writeData(Range range, SwarmContent content) throws IOException {
        fileCoordinator.write(range, content);
    }
}