package com.limegroup.gnutella.connection;

import java.io.*;

/**
 * Interface for writing Gnutella connection headers.  Implementations may 
 * write headers using any mode they like, including blocking and non-blocking
 * network IO.
 */
public interface HeaderWriter {
    
    /**
     * Writes the specified header out to the socket.
     * 
     * @param header the header to write to the remote host
     * @return <tt>true</tt> if the header was successfully written, 
     *  otherwise <tt>false</tt>
     * @throws IOException if there was an IO error writing the header
     */
    boolean writeHeader(String header) throws IOException;

    /**
     * Closes the writing of headers by sending the trailing "\r\n".  This
     * denotes the end of a set of Gnutella connection headers.
     * 
     * @return <tt>true</tt> if the closing "\r\n" was successfully sent, 
     *  otherwise <tt>false</tt>
     * @throws IOException if there was an IO error closing the header
     *  sequence
     */
    boolean closeHeaderWriting() throws IOException;
}
