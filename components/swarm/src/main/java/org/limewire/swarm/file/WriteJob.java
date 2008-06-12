package org.limewire.swarm.file;

import java.io.IOException;

import org.apache.http.nio.ContentDecoder;

/**
 * A job for writing data.  The job is expected to asynchronously write data
 * to disk.
 */
public interface WriteJob {

    /**
     * Notification that content is available in the decoder.
     * This returns the amount of data that was read.
     */
    long consumeContent(ContentDecoder decoder) throws IOException;

    /**
     * Cancels this job.
     */
    void cancel();

}
