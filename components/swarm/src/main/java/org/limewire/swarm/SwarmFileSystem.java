package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public interface SwarmFileSystem {

    /**
     * Writes data from the decoder, starting at 'start'. Writes as much as the
     * decoder has available, and returns the amount written. See
     * {@link FileChannel#write(ByteBuffer, long)}.
     */
    long write(ByteBuffer byteBuffer, long start) throws IOException;
    
    long read(ByteBuffer byteBuffer, long start) throws IOException;

    /** Closes this writer. */
    void close() throws IOException;

    /** Initializes this writer for writing. */
    void initialize() throws IOException;
    
    public long getCompleteSize();

}
