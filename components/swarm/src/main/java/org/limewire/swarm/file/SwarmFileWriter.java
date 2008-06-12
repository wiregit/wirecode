package org.limewire.swarm.file;

import java.io.IOException;

import org.apache.http.nio.ContentDecoder;

public interface SwarmFileWriter {

    /**
     * Writes data from the decoder, starting at 'start'.
     * Writes as much as the decoder has available, and returns
     * the amount written.
     */
    long transferFrom(ContentDecoder decoder, long start) throws IOException;
    
    /** Closes this writer. */
    void finish();
    
    /** Initializes this writer for writing. */
    void initialize() throws IOException;

}
