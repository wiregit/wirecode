package com.limegroup.gnutella.connection;

import java.io.*;

/**
 * Interface for writing Gnutella connection headers.  Implementations may 
 * write headers using any mode they like, including blocking and non-blocking
 * network IO.
 */
public interface HeaderWriter {

    boolean write() throws IOException;

    boolean writeHeader(String header) throws IOException;

    boolean closeHeaderWriting() throws IOException;

    void setWriteRegistered(boolean registered);

    /**
     * @return
     */
    boolean hasBufferedData();
}
