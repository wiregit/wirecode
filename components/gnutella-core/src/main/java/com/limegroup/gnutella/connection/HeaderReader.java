package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.http.HTTPHeader;

/**
 * Interface for Gnutella header reading implementations.
 */
public interface HeaderReader {
   
    /**
     * Reads the next header in the sequence with no timeout.
     * 
     * @return a new <tt>HTTPHeader</tt> istance, or <tt>null</tt> if we could
     *  not read a complete header and should continue reading when there's 
     *  more data in the TCP buffer for this channel
     */    
    HTTPHeader readHeader() throws IOException;

    /**
     * Reads the next header in the sequence with the specified timeout.
     * 
     * @param timeout the timeout for receiving data on the socket -- the 
     *  socket will be closed if we don't receive data within this number of
     *  milliseconds
     * @return a new <tt>HTTPHeader</tt> istance, or <tt>null</tt> if we could
     *  not read a complete header and should continue reading when there's 
     *  more data in the TCP buffer for this channel
     */
    HTTPHeader readHeader(int timeout) throws IOException;

    /**
     * Determines whether or not all headers have been read, i.e. if we have
     * read the final sequence of two "\r\n"s.
     * 
     * @return <tt>true</tt> if we have read all of the headers, otherwise
     *  <tt>false</tt>
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
     * Specialized method for reading the Gnutella connect string.  The connect
     * string is different from other headers in that it ends in \n\n instead
     * of \r\n.  Like readConnect() with no arguments, except that this method
     * will abort reading if no data is read within the specified number of 
     * milliseconds.
     *
     * @param timeout the number of milliseconds to wait for data before
     *  closing the connection
     * @return the connect string read from the remote host
     */
    String readConnect(int timeout) throws IOException;
}
