package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;

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
     * Specialized method for reading the Gnutella connect string.  This 
     * differs from the raw readHeader method in that it does not provide the 
     * convenience of the HTTPHeader typing, since this is not an HTTP header 
     * with a key and a value.
     * 
     * @return the connect string read from the remote host
     */
    String readConnect() throws IOException;

    /**
     * Specialized method for reading the Gnutella connect string.  Like 
     * readConnect() with no arguments, except that this method will abort 
     * reading if no data is read within the specified number of milliseconds.
     *
     * @param timeout the number of milliseconds to wait for data before
     *  closing the connection
     * @return the connect string read from the remote host
     */
    String readConnect(int timeout) throws IOException;

    /**
     * Accessor for whether or not there's more data that was read off of the
     * channel that has not been read.  This is typically called after the 
     * header reading is complete to see whether or not we've read some 
     * Gnutella message data as well.  This is particularly important for 
     * non-blocking reads where reads happen in chunks instead of one 
     * character at a time.
     * 
     * @return <tt>true</tt> if there's more data that we've read in from the
     *  channel but have not processed, otherwise <tt>false</tt>
     */
    boolean hasRemainingData();

    /**
     * Accessor for any data remaining in the header reader.  This is 
     * particularly useful for NIO header reading where we will frequently
     * read into the first message or so sent from the remote host at the end
     * of the handshake.  This allows that message data to be retrieved and 
     * processed.
     * 
     * @return a <tt>ByteBuffer</tt> containing any left over data read from 
     *  the remote host but not yet processed
     */
    ByteBuffer getRemainingData();
}
