package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.http.HTTPHeader;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * This class handles the blocking
 */
public final class BIOHeaderReader implements HeaderReader {

    //private final InputStream INPUT_STREAM;

    //private final Socket SOCKET;

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
     * @param connection
     * @return
     */
    public static HeaderReader createReader(Connection conn) {
        return new BIOHeaderReader(conn);
    }

    /**
     * @param conn
     */
    private BIOHeaderReader(Connection conn) {
        CONNECTION = conn;
    }

            
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#readHeader()
     */
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

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#headersComplete()
     */
    public boolean headersComplete() {
        return _headersComplete;
    }

    
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
            //Restore socket timeout.
            CONNECTION.getSocket().setSoTimeout(oldTimeout);
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#readConnect()
     */
    public String readConnect() throws IOException {
        return readConnect(Constants.TIMEOUT);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#readConnect(int)
     */
    public String readConnect(int timeout) throws IOException {
        return readLine(timeout);
    }
}
