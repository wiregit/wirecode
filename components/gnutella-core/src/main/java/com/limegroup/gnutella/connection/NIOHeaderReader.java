package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.http.HTTPHeader;

/**
 * Specialized class for non-blocking reading of Gnutella message headers.
 */
public final class NIOHeaderReader implements HeaderReader {


    /**
     * Flag for whether or not we've read a complete set of headers.  A 
     * complete set of headers is terminated by \r\n.
     */
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
     * Creates a new <tt>NIOHeaderReader</tt> for the specified connection.
     * 
     * @param conn the <tt>Connection</tt> this header reader will read for
     * @return a new <tt>NIOHeaderReader</tt> for the specified connection
     */
    public static NIOHeaderReader createReader(Connection conn) {
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
        String line = readLine();
        if(line == null) {
            return null;
        }
        if(headersComplete()) {
            return null;
        }
        return HTTPHeader.createHeader(line);     
    }
    
    // inherit doc comment
    public HTTPHeader readHeader(int timeout) throws IOException {
        // TODO re-enable timeouts
        return readHeader();
    }

    // inherit doc comment
    public String readConnect() throws IOException {
        return readLine();
    }

    // inherit doc comment
    public String readConnect(int timeout) throws IOException {
        return readConnect();
        // TODO enable timeouts
    }
    
    // inherit doc comment
    public boolean hasRemainingData() {
        return _headerByteBuffer.hasRemaining();
    }
    
    /**
     * Helper method for reading a line of handshaking -- a line terminated
     * by \r\n.  This line can either be a request line, a response status 
     * line, or a HTTP-style Gnutella connection header.  In particular, this
     * method takes care of the various header reading states for non-blocking
     * reads, such as the case where a partial line was read on the previous 
     * call.
     * 
     * @return the line read from the remote host, or <tt>null</tt> if we 
     *  could only read a partial header
     * @throws IOException if there's an IO error reading the header
     */    
    private String readLine() throws IOException {
        _headersComplete = false;
        // If there are more headers to read in the buffer, keep reading
        if(_headerByteBuffer.position() != 0 && 
           _headerByteBuffer.hasRemaining())  {
            String header = read(_headerByteBuffer);
        
            // If we get a complete header, return it.  Otherwise, we'll 
            // read more
            if(header != null)  {
                // If we read the final \r\n to end the headers, return null.
                if(header.length() == 0) {
                    return null;
                }
                return header;
            }
        } 

    
        // We can safely clear the buffer here because all data in the buffer
        // has been read at this point -- hasRemaining is false
        // prepare the buffer for reading again
        _headerByteBuffer.clear();
    
        // Read into the buffer.  After this read, the buffer can contain no
        // data, it can contain a partial header, multiple headers, headers
        // and partial Gnutella messages, etc.
        int bytesRead = CHANNEL.read(_headerByteBuffer);

        if(bytesRead == -1)  {
            // Remove the buffer memory as fast as possible
            _headerByteBuffer = null;
            throw new IOException("socket closed by remote host");
        }
    
        _headerByteBuffer.flip();
        String header = read(_headerByteBuffer);
    
        if(header == null)  {  
            // continue reading on the next pass
            return null;
        }
    
        return header;         
    }
    
    
    /**
     * Helper method that stores header characters.  If a header is only 
     * partially read, this class stores the read characters in a string 
     * buffer for later reading.
     */
    private String read(ByteBuffer buf) throws IOException   {
        while(buf.hasRemaining() && _buffer.length() < 1024)  {
            char curChar = (char)buf.get();
            if(curChar == '\r') {
                char nextChar = (char)buf.get();
                if(nextChar == '\n') {
                    // we've reached the end of a header
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


    // inherit doc comment
    public boolean headersComplete() {
        return _headersComplete;
    }

    // inherit doc comment
    public ByteBuffer getRemainingData() {
        return _headerByteBuffer;
    }
}
