package com.limegroup.gnutella.connection;

import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import com.sun.java.util.collections.*;

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

    private boolean _registered;

    /**
     * List of headers that we were not able to send out -- buffer 
     * them for later sending.
     */
    //private final LinkedList BUFFERED_HEADERS = new LinkedList();

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

            // if we successfully wrote everything, try writing more
            //return writeBuffered();
        //} else {
           // return writeBuffered();
        //}
    }

    /**
     * Writes as many headers to the buffer as possible before not being able
     * to send any more data.
     *
     * @throws IOException if there is an IO error writing to the channel
     * @return <tt>true</tt> if all buffered headers have been successfully
     *  have been written to the network, meaning that this channel no 
     *  longer needs to be registered for writes, otherwise returns 
     *  <tt>false</tt>, indicating that the selector should keep this
     *  channel registered for write events
     */
//    private boolean writeBuffered() throws IOException {
//        Iterator iter = BUFFERED_HEADERS.iterator();
//        while(iter.hasNext() && !_headerBuffer.hasRemaining()) {
//            rawWriteHeader((String)iter.next()); 
//        }
//        if(_headerBuffer.hasRemaining() || !BUFFERED_HEADERS.isEmpty()) {
//            // keep this channel registered for write events
//            return false;
//        } else {
//            // deregister this channel for write events
//            return true;
//        }
//    }

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

        // If we've already started to buffer, keep buffering.  We'll be
        // notified by the selector whenever we can send
        //if(BUFFERED_HEADERS.size() > 0) {
          //  BUFFERED_HEADERS.add(header);
            //return false;
        //}
        
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
                if(!_registered) {
                    NIODispatcher.instance().addWriter(CONNECTION);
                    _registered = true;                    
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

    // inherit doc comment
    public synchronized void setWriteRegistered(boolean registered) {
        _registered = registered;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderWriter#hasBufferedData()
     */
    public boolean hasBufferedData() {
        return _headerBuffer.hasRemaining();
    }
}
