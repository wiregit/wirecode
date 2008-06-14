package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentDecoder;

public interface SwarmFile {

    /**
     * Writes data from the decoder, starting at 'start'.
     * Writes as much as the decoder has available, and returns
     * the amount written.
     * See {@link FileChannel#write(ByteBuffer, long).
     */
    long transferFrom(ContentDecoder decoder, long start) throws IOException;
    
    /**
     * Transfers data from the file to the given buffer.
     * See {@link FileChannel#read(ByteBuffer, long).
     */
    long transferTo(ByteBuffer buffer, long start) throws IOException;
    
    /** Closes this writer. */
    void finish();
    
    /** Initializes this writer for writing. */
    void initialize() throws IOException;

}
