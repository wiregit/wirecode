package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.limewire.collection.Range;


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
     * Closes the given swarmFile for writing/reading.
     * 
     * @param swarmFile
     * @throws IOException
     */
    void closeSwarmFile(SwarmFile swarmFile) throws IOException;

    /**
     * Initializes this filesystem for use.
     * 
     * @throws IOException
     */
    void initialize() throws IOException;

    /**
     * Returns the complete size of this filesystem. All files contained within.
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

    /**
     * Returns the SwarmFiles represented by this file system.
     * @return
     */
    List<SwarmFile> getSwarmFiles();

    /**
     * Returns the SwarmFiles which are a part of the given range.
     * @param range range to get the files from.
     * @return
     */
    List<SwarmFile> getSwarmFiles(Range range);

}
