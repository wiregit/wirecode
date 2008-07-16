package org.limewire.swarm;

import java.io.IOException;


/**
 * A job for writing data.  The job is expected to asynchronously write data
 * to disk.
 */
public interface SwarmWriteJob {

    /**
     * Notification that content is available in the decoder.
     * This returns the amount of data that was read.
     */
    long consumeContent(SwarmContent content) throws IOException;

    /**
     * Cancels this job.
     */
    void cancel();

}
