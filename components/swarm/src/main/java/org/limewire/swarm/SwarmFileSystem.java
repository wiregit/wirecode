package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.swarm.file.SwarmFile;

public interface SwarmFileSystem {

    /**
     * Writes data from the byteBuffer to the fileSystem, starting at 'start'.
     * Writes as much as the has available, and returns the amount written.
     * 
     * @param byteBuffer
     * @param start
     * @return
     * @throws IOException
     */
    long write(ByteBuffer byteBuffer, long start) throws IOException;

    /**
     * Reads data from the fileSystem starting at 'start' into the given buffer.
     * Reads as much as is available or will fit into the buffer, and returns
     * the amount read.
     * 
     * @param byteBuffer
     * @param start
     * @return
     * @throws IOException
     */
    long read(ByteBuffer byteBuffer, long start) throws IOException;

    /**
     * Closes this filesystem, cleaning up resources used for reading/writing.
     * 
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Initializes this filesystem for use.
     * 
     * @throws IOException
     */
    void initialize() throws IOException;

    /**
     * Retunrs the complete size of this filesystem. All files contained within.
     * 
     * @return
     */
    long getCompleteSize();

    /**
     * Retrieves the swarm file located at the given position.
     * 
     * @param position
     * @return
     */
    SwarmFile getSwarmFile(long position);

}
