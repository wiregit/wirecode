package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.http.HTTPHeader;

/**
 * Specialized class for non-blocking reading of Gnutella message headers.
 */
public final class NIOHeaderReader implements HeaderReader {


    //private HttpHeader _header;

    private boolean _headersComplete;

    /**
     * Constant for the <tt>SocketChannel</tt> for this connection.
     */
    private SocketChannel CHANNEL;

    /**
     * Constant for the connection that this reader reads headers for.
     */
    private final Connection CONNECTION;
    

    private ByteBuffer _headerByteBuffer = ByteBuffer.allocate(1024);
    
    private StringBuffer _buffer = new StringBuffer();
    
    /**
     * @param connection
     * @return
     */
    public static NIOHeaderReader createReader(Connection conn) {
        // TODO Auto-generated method stub
        return new NIOHeaderReader(conn);
    }
        
    /**
     * Creates a new <tt>NIOHeaderReader</tt> for the specified connection.
     * 
     * @param conn the <tt>Connection</tt> this header reader will read for
     */
    private NIOHeaderReader(Connection conn) {
        CONNECTION = conn;
        CHANNEL = conn.getSocket().getChannel();
        //_header = new HttpHeader();  
    }

    /**
     * Reads the next header from the socket channel.  If the entire header
     * is not read, returns <tt>null</tt>.  Throws an exception if there is a
     * problem reading the header.
     * 
     * @return the next header from the socket channel
     * @throws IOException if there is an IO error reading the header from the
     *  network, including a syntax error
     */    
    public HTTPHeader readHeader() throws IOException  { 
        // if there are more headers to read in the buffer, keep reading
        if(_headerByteBuffer.position() != 0 && 
           _headerByteBuffer.hasRemaining())  {
            String header = read(_headerByteBuffer);
        
            // If we get a complete header, return it.  Otherwise, we'll 
            // read more
            if(header != null)  {
                System.out.println(header);
                return HTTPHeader.createHeader(header);
            }
        } 

    
        // we can safely clear the buffer here because all data in the buffer
        // has been read at this point -- hasRemaining is false
        // prepare the buffer for reading again
        _headerByteBuffer.clear();
    
        // Read into the buffer.  After this read, the buffer can contain no
        // data, it can contain a partial header, multiple headers, headers
        // and partial Gnutella messages, etc.
        int bytesRead = CHANNEL.read(_headerByteBuffer);

        if(bytesRead == -1)  {
            // remove the buffer memory as fast as possible
            _headerByteBuffer = null;
            throw new IOException("socket closed by remote host");
        }
    
        _headerByteBuffer.flip();
        //_headerBuffer = _headerByteBuffer.asCharBuffer();
      
        String header = read(_headerByteBuffer);
    
        if(header == null)  {  
            // continue reading on the next pass
            return null;
        }
    
    
        // set the buffer's read position back to the beginning
        //_headerByteBuffer.clear();
    
        //_header = new HttpHeader();
        return HTTPHeader.createHeader(header);        
    }
    
    
    /**
     * Helper method that stores header characters.  If a header is only 
     * partially read, this class stores the read characters in a string 
     * buffer for later reading.
     */
    private String read(ByteBuffer buf) throws IOException   {
        while(buf.hasRemaining() && _buffer.length() < 1024)  {
            char curChar = (char)buf.get();
            if(curChar == '\r' || curChar == '\n') {
                char nextChar = (char)buf.get();
                if(nextChar == '\n') {
                    // we've reached the end of a header
                    // note that the CharBuffer likely still has more 
                    // characters
                    String header = _buffer.toString().trim();
                    if(header.length() == 0)  {
                        _headersComplete = true;
                    } else  {
                        _headersComplete = false;
                    }
                    _buffer = new StringBuffer();
                    return header;
                } else {
                    // we encountered only a '\r' or a '\n' but no final 
                    // '\n', so keep appending
                    _buffer.append(curChar);
                    _buffer.append(nextChar);
                }
            } else  {
                _buffer.append(curChar);
            }
        }
        
        if(_buffer.length() >= 1024)   {
            _buffer = new StringBuffer();
            throw new IOException("HTTP header too large");
        }
        
        // keep reading in this case without worrying about clearing the
        // ByteBuffer -- we've only read a partial header
        return null;         
    }


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#readHeader(int)
     */
    public HTTPHeader readHeader(int timeout) throws IOException {
        // TODO re-enable timeouts
        return readHeader();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#headersComplete()
     */
    public boolean headersComplete() {
        return _headersComplete;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#readConnect()
     */
    public String readConnect() throws IOException {
        return read(_headerByteBuffer);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.HeaderReader#readConnect(int)
     */
    public String readConnect(int timeout) throws IOException {
        return null;
        // TODO enable timeouts
    }
}
