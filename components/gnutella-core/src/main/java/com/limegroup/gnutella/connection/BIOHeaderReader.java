package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.http.HTTPHeader;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * This class handles reading Gnutella message headers using blocking sockets.
 */
public final class BIOHeaderReader implements HeaderReader {

    /**
     * Flag for whether or not we're done reading headers from the connection.
     */
    private boolean _headersComplete;

    /**
     * Constant for the connection this does reading for.
     */
    private final Connection CONNECTION;
    
    /**
     * Cache the 'connection closed' exception, so we have to allocate
     * one for every closed connection.
     */
    private static final IOException CONNECTION_CLOSED =
        new IOException("connection closed");

    /**
     * Factory method for creating new <tt>BIOHeaderReader</tt> instances for
     * reading headers from the specified connection.
     * 
     * @param conn the <tt>Connection</tt> instance this reader will read 
     *  headers for
     * @return a new <tt>BIOHeaderReader</tt> ready to read headers for the 
     *  specified connection
     */
    public static HeaderReader createReader(Connection conn) {
        return new BIOHeaderReader(conn);
    }

    /**
     * Creates a new <tt>BIOHeaderReader</tt> instance for performing blocking
     * header reads on behalf of the specified connection.
     * 
     * @param conn the <tt>Connection</tt> this header reader will read for
     */
    private BIOHeaderReader(Connection conn) {
        CONNECTION = conn;
    }

            
    // inherit doc comment
    public HTTPHeader readHeader() throws IOException {
        return readHeader(Constants.TIMEOUT);
    }
    
    /**
     * Reads the next header from this connection's socket.
     * 
     * @param timeout The time in milliseconds to wait for data on the socket.
     * @return a new <tt>HTTPHeader</tt> instance for the Gnutella connection
     *  header (which are in HTTP form), or <tt>null</tt> if we've reached the
     *  end of the handshake -- the final \r\n
     */
    public HTTPHeader readHeader(int timeout) throws IOException {
        String header = readLine(timeout);
        if(header == null)  {
            _headersComplete = true;
            return null;
        }
        
        _headersComplete = false;
        // wrap the header string in an HTTPHeader instance
        return HTTPHeader.createHeader(header);
    }

    // inherit doc comment
    public boolean headersComplete() {
        return _headersComplete;
    }

    /**
     * Helper method for reading one line of a connection request/response line
     * or headers. 
     * 
     * @param timeout the time in milliseconds to wait for data before closing
     *  the connection -- if no data is read within this timeframe, the 
     *  connection will be closed
     * @return the read line of characters, or <tt>null</tt> if we read the 
     *  final "\r\n" indicating the end of a header sequence
     * @throws IOException if there is an IO error reading data from the socket
     */
    private String readLine(int timeout) throws IOException  {
        int oldTimeout = CONNECTION.getSocket().getSoTimeout();
        // InputStream.read can throw an NPE if we closed the connection,
        // so we must catch NPE and throw the CONNECTION_CLOSED.
        try {
            CONNECTION.getSocket().setSoTimeout(timeout);
            String line = 
                (new ByteReader(CONNECTION.getInputStream())).readLine();
            if (line==null)
                throw new IOException("unexpected end of file");
            if(!CommonUtils.isJava118()) {
                BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(
                    line.length());
            }
            if(line.length() == 0)  {
                // found the final \r\n -- header finished
                return null;
            }
            return line;
        } catch(NullPointerException e) {
            throw CONNECTION_CLOSED;
        } finally {
            // Restore socket timeout.
            CONNECTION.getSocket().setSoTimeout(oldTimeout);
        }
    }

    // inherit doc comment
    public String readConnect() throws IOException {
        return readConnect(Constants.TIMEOUT);
    }
    
    // inherit doc comment
    public String readConnect(int timeout) throws IOException {
        return readLine(timeout);
    }

    /**
     * In the case of blocking reads, the reader never reads more data than it
     * needs to beyond the end of the headers, so this always returns 
     * <tt>false</tt>.
     * 
     * @return <tt>false</tt> because blocking reads look at one character at a
     *  time, and therefore don't even have to read beyond the end of the
     *  headers
     */
    public boolean hasRemainingData() {
        return false;
    }
}
