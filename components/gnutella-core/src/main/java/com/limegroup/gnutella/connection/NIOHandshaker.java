package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Properties;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.http.HTTPHeader;

/**
 * This class performs non-blocking handshaking, responding to read and write
 * events from the NIO selector.
 */
public class NIOHandshaker extends AbstractHandshaker {

    private ByteBuffer _responseBuffer;

    private ByteBuffer _requestBuffer;

    private HandshakeResponse _ourResponse;

    private HandshakeWriteState _writeState;

    private HandshakeReadState _readState;

    private int _handshakeAttempts;
    
    /**
     * Creates a new <tt>NIOHandshaker</tt> for the specified connection.
     * 
     * @param conn the <tt>Connection</tt> to handshake for
     * @param requestHeaders the request headers to send
     * @param responseHeaders the responder for determining our response to 
     *  the remote host
     * @return a new <tt>NIOHandshaker</tt>
     */
    public static Handshaker createHandshaker(Connection conn,
        Properties requestHeaders, HandshakeResponder responseHeaders)  {
        return new NIOHandshaker(conn, requestHeaders, responseHeaders);
    }
    
    /**
     * Creates a new <tt>NIOHandshaker</tt> instance for the specified 
     * connection.
     * 
     * @param conn the <tt>Connection</tt> to handshake for
     * @param requestHeaders the request headers to send
     * @param responseHeaders the responder for determining our response to 
     *  the remote host
     */
    private NIOHandshaker(Connection conn, 
        Properties requestHeaders, HandshakeResponder responseHeaders)  {
        super(conn, requestHeaders, responseHeaders);
        if(CONNECTION.isOutgoing())  {
            _requestBuffer = 
                createBuffer(GNUTELLA_CONNECT_06, REQUEST_HEADERS);
            _writeState = new OutgoingRequestWriteState();
            _readState = new OutgoingResponseReadState();
        } else  {
            //_writeState = new IncomingResponseWriteState();
            
        }
    }
    
    /**
     * Creates a new byte buffer for the outgoing connection request.
     * 
     * @param headers the headers to include in the request
     * @return a new byte buffer with the connect string and headers for the
     *  outgoing connection request
     */
    private static ByteBuffer createBuffer(String statusLine, 
        Properties headers)  {
        StringBuffer sb = new StringBuffer();
        Enumeration headerNames = headers.propertyNames();
        while(headerNames.hasMoreElements())  {
            String key = (String)headerNames.nextElement();
            sb.append(key);
            sb.append(": ");
            sb.append(headers.getProperty(key));
            //sb.append(headerValues.nextElement());
            sb.append(CRLF);
        }
        sb.append(CRLF);
        // TODO: deal with Remote-IP header -- see old Connection sendHeaders
        
        byte[] connectBytes = (statusLine + CRLF).getBytes();
        byte[] headerBytes = sb.toString().getBytes();
        ByteBuffer buffer = 
            ByteBuffer.allocate(connectBytes.length+headerBytes.length);
        buffer.put(connectBytes);
        buffer.put(headerBytes);
        return buffer;
    }
    
    
    /**
     * Performs a non-blocking handshake.  In the case of an outgoing
     * handshake, this writes the outgoing connection request.  In the case of
     * an incoming request, this does nothing since we have to wait for any 
     * read events to come in.  The handshake does not complete after this call,
     * with the return value simply indicating whether or not this connectio
     * should be registered for write events (there was an incomplete write on
     * the channel do to the upstream TCP buffers being filled).
     * 
     * @return <tt>true</tt> if any writes successfully completed, meaning that
     *  this connection SHOULD NOT be registered for write events, otherwise
     *  a <tt>false</tt> value indicates that this connection has more data
     *  to write, and should be notified when space becomes available in the
     *  TCP buffers
     */
    public boolean handshake() throws IOException, NoGnutellaOkException {
        if(_headerReader != null)  {
            throw new IllegalStateException("two calls to handshake!");
        }
        // lazily initialize the writer to make sure the sockets
        // and IO streams are initialized        
        if(_headerReader == null)  {
            _headerReader = NIOHeaderReader.createReader(CONNECTION); 
        }
        
        // write our request headers if it's an outgoing handshake
        if(CONNECTION.isOutgoing()) {
            return write();
        }
        return true;
    }
    

    /**
     * Progresses through the handshake reading states.  The state remains
     * the same if an incomplete read occurs.
     * 
     * @throws IOException if there is an IO error during the read, or if
     *  the connection is not accepted by either party
     */
    public void read() throws IOException  {
        _readState = _readState.read();
    }
    
    /**
     * Reads Gnutella headers from this connection.
     * 
     * @return <tt>true</tt> if this set of headers was entirely read, i.e. 
     *  we read a double '\r\n' sequence, otherwise <tt>false</tt>
     * @throws IOException if an IO error occurs while reading the headers
     */
    private boolean readHeaders() throws IOException  {
        int headersRead = 0;
        while(headersRead < MAX_HEADERS_TO_READ)  {
            HTTPHeader header = _headerReader.readHeader();
            if(_headerReader.headersComplete()) {
                // we're all done
                return true;
            } 
            if(header == null)  {
                // we need to finish reading on the next pass
                return false;
            } 
            
            // TODO: handle authentication -- see old Connection readHeaders
            // method
            handleRemoteIP(header);
            HEADERS_READ.put(header.getHeaderNameString(),
                header.getHeaderValueString());
            headersRead++;
        }
        throw new IOException("too many headers read");
    }

    /**
     * Writes handshaking data to the network depending on the current state
     * of the handshake.  This may, for example, write the connection
     * request on an outgoing handshake, or it may write the handshake 
     * response.
     * 
     * @throws IOException if there is an IO error writing data to the network
     */
    public boolean write() throws IOException  {
        _writeState = _writeState.write();
        
        return !_writeState.hasRemaining();  
    }
    
    /**
     * Helper method that writes the specified buffer to this connection's
     * channel and adds the connection as a writer if the reading doesn't
     * complete and it's not already added.
     * 
     * @param buffer the <tt>ByteBuffer</tt> containing headers to write
     * @return <tt>true</tt> if the entire buffer was successfully written,
     *  otherwise <tt>false</tt>
     * @throws IOException if there was an IO error writing to the buffer
     */
    private boolean writeBuffer(ByteBuffer buffer) throws IOException  {
        CONNECTION.getSocket().getChannel().write(buffer);
        if(buffer.hasRemaining())  {
            if(!CONNECTION.writeRegistered())  {
                NIODispatcher.instance().addWriter(CONNECTION);
            } 
                
            // we need to stay registered and keep writing
            return false;
        }
        return true;       
    }

    /**
     * Accessor for whether or not the connection handshaking process is 
     * complete.
     * 
     * @return <tt>true</tt> if the connection handshaking process if complete,
     *  otherwise <tt>false</tt>
     */
    public boolean handshakeComplete() {
        return _handshakeComplete;
    }    


    /**
     * Interface for executing the current handshake state for writing data
     * to the remote host, following the state pattern.
     */
    private static interface HandshakeWriteState  {
        
        /**
         * Performs as much writing as possible for the current handshake
         * state.  This may write a connection request and headers, a response
         * status and header, etc.
         * 
         * @return the <tt>HandshakeWriteState</tt> after this call to write
         * @throws IOException if an IO error occurs during writing
         */
        public HandshakeWriteState write() throws IOException;
        
        /**
         * Determines whether or not this writing state has more to write.
         * 
         * @return <tt>true</tt> if this handshaking state has more to write,
         *  otherwise <tt>false</tt>
         */
        public boolean hasRemaining();
    }

    /**
     * Abstract handshake write class that adds functionality common to all
     * handshake writing states, such as whether or not writing is complete.
     */
    private abstract class AbstractHandshakeWriteState  
        implements HandshakeWriteState {
        
        /**
         * Flag for weather or not this write state has more data to write.
         */
        protected boolean _hasRemaining;
        
        // inherit doc comment
        public boolean hasRemaining()   {
            return _hasRemaining;
        }     
    }
    
    /**
     * Specialized class that handles the initial writing of outgoing connection
     * handshake request and headers.
     */
    private final class OutgoingRequestWriteState 
        extends AbstractHandshakeWriteState {

        /**
         * Writes the outgoing Gnutella connection request and headers.
         * 
         * @throws IOException if an IO error occurs during writing
         */
        public HandshakeWriteState write() throws IOException {
            if(writeBuffer(_requestBuffer))  {
                return new OutgoingResponseWriteState();
            }
            
            // we have more to write in this state, so set the flag
            _hasRemaining = true;
            
            // We will be notified of the read by the selector.
            return this;
        }
    }

    /**
     * Specialized class for handling writing the outgoing connection handshake
     * response status line and headers.
     */
    private final class OutgoingResponseWriteState 
        extends AbstractHandshakeWriteState {


        /**
         * Writes the final connection response and any headers for completing
         * an outgoing connection handshake.  If we're using authentication,
         * we may revert back to the read response state and start progressing
         * through the handshake states again.  
         * 
         * Typically, however, this state will typically consist of simply
         * writing:
         * 
         * GNUTELLA/0.6 200 OK
         * 
         * If we connected as an Ultrapeer and the remote host has told us to 
         * become a leaf via leaf-guidance, then we'll also include a header 
         * indicating our Ultrapeer status after leaf guidance, as in:
         * 
         * GNUTELLA/0.6 200 OK
         * X-Ultrapeer: False
         */
        public HandshakeWriteState write() throws IOException {
            if(_handshakeAttempts > MAX_HANDSHAKE_ATTEMPTS) {
                //If we didn't successfully return out of the method, throw an 
                //exception to indicate that handshaking didn't reach any 
                //conclusion.  The values here are kind of a hack.
                throw NoGnutellaOkException.UNRESOLVED_SERVER;
            }
            if(!writeBuffer(_responseBuffer)) {
                _hasRemaining = true;
                return this;
            }
            _handshakeAttempts++;
            int code = _ourResponse.getStatusCode();
            //Consider termination...
            if(code == HandshakeResponse.OK) {
                if(HandshakeResponse.OK_MESSAGE.equals(
                    _ourResponse.getStatusMessage())){
                    //a) Terminate normally if we wrote "200 OK".
                    // We're all done writing in this case
                    _hasRemaining = false;
                    _handshakeComplete = true;
                    CONNECTION.handshakeComplete();
                    return null;
                } else {
                    //b) Continue loop if we wrote "200 AUTHENTICATING".
                    
                    // set the state to read the response again
                    _readState = new OutgoingResponseReadState();
                    
                    // We have nothing left to send.  Our _responseBuffer will
                    // be reset when we read their response in the processing
                    // of the next read.
                    _hasRemaining = false;
                    
                    // TODO: make sure we set the state correctly to keep 
                    // looping -- _hasRemaining???
                    return this;
                }
            } else {                
                //c) Terminate abnormally if we wrote anything else.
                if(code == HandshakeResponse.SLOTS_FULL) {
                    throw NoGnutellaOkException.CLIENT_REJECT;
                } else {
                    throw NoGnutellaOkException.createClientUnknown(code);
                }
            }
        }
    }

    /**
     * Interface for executing the current handshake state for reading data
     * from the remote host, following the state pattern.
     */
    private static interface HandshakeReadState  {
        public HandshakeReadState read() throws IOException;
    }
    
    /**
     * Specialized class for reading the response to an outgoing handshake 
     * attempt.  This reads only the response line.  It does not read the
     * response headers.
     */
    private final class OutgoingResponseReadState 
        implements HandshakeReadState  {

        
        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.NIOHandshaker.HandshakeReadState#read()
         */
        public HandshakeReadState read() throws IOException {
            String connectLine = NIOHandshaker.this._headerReader.readConnect();
            
            // if we only read a partial header, stay in this state and try to
            // read again
            if(connectLine == null)  {
                return this;
            }
            
            // if they didn't give an expected response format, abort
            if (!connectLine.startsWith(GNUTELLA_06))  {
                throw new IOException("Bad connect string");
            }

            HandshakeReadState newState = 
                new OutgoingResponseReadHeaderState(connectLine);
            return newState.read();
        }   
    }
    
    /**
     * Specialized class for reading the handshake response headers from an
     * outgoing hanshake attempt.
     */
    private final class OutgoingResponseReadHeaderState 
        implements HandshakeReadState  {
        

        private String CONNECT_LINE;

        OutgoingResponseReadHeaderState(String connectLine) throws IOException {
            CONNECT_LINE = connectLine;        
        }
    
        public HandshakeReadState read() throws IOException  {

            // First make sure we read all of the headers.
            if(readHeaders())  {
                _handshakeComplete = true;
            } else {
                return this;
            }

            // Terminate abnormally if we read something other than 200 or 401.
            HandshakeResponse theirResponse = 
                HandshakeResponse.createResponse(
                    CONNECT_LINE.substring(GNUTELLA_06.length()).trim(), 
                    HEADERS_READ);
            
            int code = theirResponse.getStatusCode();
            if (code != HandshakeResponse.OK &&  
                code != HandshakeResponse.UNAUTHORIZED_CODE) {
                if(code == HandshakeResponse.SLOTS_FULL) {
                    throw NoGnutellaOkException.SERVER_REJECT;
                } else {
                    throw NoGnutellaOkException.createServerUnknown(code);
                }
            } 

            // Write "GNUTELLA/0.6" plus response code, such as "200 OK", 
            // and headers.
            Assert.that(RESPONSE_HEADERS != null, "null RESPONSE_HEADERS");
            _ourResponse = 
                RESPONSE_HEADERS.respond(theirResponse, true);

            Assert.that(_ourResponse != null, "null ourResponse");
            _responseBuffer = 
                createBuffer(GNUTELLA_06 + " " + _ourResponse.getStatusLine(), 
                    _ourResponse.props());
            write();
    
            // we're all done with reading
            return null;
        }
    }
}
