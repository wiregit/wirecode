package com.limegroup.gnutella.connection;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/**
 * Performs non-blocking writing of headers.
 *
 */
public final class NIOHeaderWriter implements HeaderWriter {

    private final SocketChannel CHANNEL;

    private ByteBuffer _headerBuffer = ByteBuffer.allocate(1024);

    /**
     * Cached byte array for the CRLF that ends every header.
     */
    private static final byte[] CRLF = "\r\n".getBytes();

    /**
     * The <tt>Connection</tt> instance for this writer.
     */
    private final Connection CONNECTION;


    /**
     * Creates a new <tt>NIOHeaderWriter</tt> for the specified connection
     * and returns it.
     *
     * @param conn the <tt>Connection</tt> this writer will write headers 
     *  for
     * @return a new <tt>NIOHeaderWriter</tt> instance
     */
    public static NIOHeaderWriter createWriter(Connection conn) {
        return new NIOHeaderWriter(conn);        
    }

    /**
     * Creates a new <tt>NIOHeaderWriter</tt> for the specified connection.
     * @param conn the <tt>Connection</tt> this writer will write headers 
     *  for     
     */
    private NIOHeaderWriter(Connection conn) {
        CONNECTION = conn;
        System.out.println("NIOHeaderWriter::NIOHeaderWriter::about to get socket");
        CHANNEL = conn.getSocket().getChannel();        
    }

    /**
     * Writes any partially-written headers to the network.
     *
     * @throws IOException if there is an IO error writing to the channel
     */
    public boolean write() throws IOException {

        // write anything left over in the buffer
        if(_headerBuffer.hasRemaining()) {
            CHANNEL.write(_headerBuffer);
        }
        
        return !_headerBuffer.hasRemaining();
    }


    /**
     * Writes the specified header to the network using non-blocking IO.  If 
     * all headers have been written successfully, this returns <tt>true</tt>,
     * indicating that this connection no longer needs to be registered for
     * write events.  Otherwise, this writer still has headers that need to
     * be written to the network, and this will return <tt>false</tt>.
     *
     * @param header the header to write
     * @throws IOException if there is an IO error writing to the channel
     * @throws IllegalArgumentException if the header parameter is null
     *  or the empty string
     * @return <tt>true</tt> if this writer has written all requested headers
     *  to the network, and can therefore be deregistered for write events,
     *  otherwise returns <tt>false</tt> to indicate that this connection 
     *  should remain registered so that it can complete writing all of its
     *  headers
     */
    public boolean writeHeader(String header) throws IOException {
        if(header == null || header.equals("")) {
            throw new IllegalArgumentException("null or empty string: "+header);
        }

        
        return rawWriteHeader(header);
    }

    /**
     * Header writing method that simply tries to write the specified header
     * to the network without performing any buffering.
     *
     * @param header the header to write
     */
    private boolean rawWriteHeader(String header) throws IOException {
        _headerBuffer.put(header.getBytes());

        CHANNEL.write(_headerBuffer);
        if(_headerBuffer.hasRemaining()) {
            synchronized(this) {
                if(!CONNECTION.writeRegistered()) {
                    NIODispatcher.instance().addWriter(CONNECTION);                 
                }
            }
            return false;
        } else {
            _headerBuffer.clear();
            return true;
        }        
    }

    // inherit doc comment
    public boolean closeHeaderWriting() throws IOException {
        _headerBuffer.put(CRLF);
        CHANNEL.write(_headerBuffer);
        if(_headerBuffer.hasRemaining())  {
            return false;
        } else  {
            _headerBuffer = null;
            return true;
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderWriter#hasBufferedData()
     */
    public boolean hasBufferedData() {
        return _headerBuffer.hasRemaining();
    }
}
