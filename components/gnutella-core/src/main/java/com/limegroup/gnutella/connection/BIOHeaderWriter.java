package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.statistics.*;

import java.io.*;

/**
 * This class handles blocking Gnutella header writing.
 */
public final class BIOHeaderWriter implements HeaderWriter {

    /**
     * Cached byte array for the CRLF that ends every header.
     */
    private static final byte[] CRLF = "\r\n".getBytes();

    /**
     * Constant for the <tt>OutputStream</tt> to write to.
     */
    private final OutputStream OUTPUT_STREAM;

    /**
     * Creates a new <tt>BIOHeaderWriter</tt> instance for the specified
     * connection.
     *
     * @param conn the <tt>Connection</tt> to create a header writer for
     * @return a new <tt>BIOHeaderWriter</tt> instance
     */
    public static BIOHeaderWriter createWriter(Connection conn) {
        return new BIOHeaderWriter(conn);        
    }

    /**
     * Constructs a new <tt>BIOHeaderWriter</tt> instance.
     *
     * @param conn the <tt>Connection</tt> to create the header for
     */
    private BIOHeaderWriter(Connection conn) {
        OUTPUT_STREAM = conn.getOutputStream();
    }

    public boolean write() throws IOException {
        throw new IllegalStateException("no left over data in blocking writer");
    }

    /**
     * Implements <tt>HeaderWriter</tt> interface.  Performs a blocking 
     * write of the specified header.
     *
     * @throws IOException if there's an IO error writing the header to 
     *  the socket 
     */
    public boolean writeHeader(String header) throws IOException {
        if(header == null || header.equals("")) {
            throw new IllegalArgumentException("null or empty string: "+header);
        }

        byte[] bytes = header.getBytes();

        if(!CommonUtils.isJava118()) {
            BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(
                bytes.length);
        }        
        OUTPUT_STREAM.write(bytes);
        OUTPUT_STREAM.flush();
        return true;              
    }

    /**
     * Implements <tt>HeaderWriter</tt> interface.  Closes the header writing
     * with a CRLF using the blocking output stream.
     *
     * @throws IOException if there's an IO error writing to the socket 
     *  to close the header
     */
    public boolean closeHeaderWriting() throws IOException {
        if(!CommonUtils.isJava118()) {
            BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(CRLF.length);
        }        
        OUTPUT_STREAM.write(CRLF);
        OUTPUT_STREAM.flush(); 
        return true;                      
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderWriter#hasBufferedData()
     */
    public boolean hasBufferedData() {
        // TODO Auto-generated method stub
        return false;
    }
}
