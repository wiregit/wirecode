package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.http.HTTPHeader;

/**
 * Interface for Gnutella header reading implementations.
 */
public interface HeaderReader {
    
    HTTPHeader readHeader() throws IOException;

    /**
     * @param timeout
     * @return
     */
    HTTPHeader readHeader(int timeout) throws IOException;

    /**
     * @return
     */
    boolean headersComplete();

    /**
     * Specialized method for reading the Gnutella connect string.  The connect
     * string is different from other headers in that it ends in \n\n instead
     * of \r\n.
     *
     * @return the connect string read from the remote host
     */
    String readConnect() throws IOException;

    /**
     * @param user_input_wait_time
     * @return
     */
    String readConnect(int timeout) throws IOException;
}
