package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Properties;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.http.HTTPHeader;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class performs non-blocking handshaking, responding to read and write
 * events from the NIO selector.
 * 
 * TODO: make sure writers are added in all necessary cases.
 */
public class NIOHandshaker extends AbstractHandshaker {

    /**
     * Wrapper for the <tt>Buffer</tt> containing response data.
     */
    private WriteHeaderWrapper _responseBuffer;

    /**
     * Wrapper for the <tt>Buffer</tt> containing request data.
     */    
    private WriteHeaderWrapper _requestBuffer;

    /**
     * <tt>HandshakeResponse</tt> containing the handshake response headers 
     * and status line we return to the remote host.
     */
    private HandshakeResponse _ourResponse;

    /**
     * The current state of handshake writing, following the state pattern.
     */
    private HandshakeWriteState _writeState;

    /**
     * The current state of handshake reading, following the state pattern.
     */
    private HandshakeReadState _readState;

    /**
     * Variable for the number of handshake attempts for determining when to
     * give up on authenticated handshakes.
     */
    private int _handshakeAttempts;
    
    /**
     * Flag for whether or not an incoming connection is coming from a crawler.
     */
    private boolean _isCrawler;
    
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
                new WriteHeaderWrapper(GNUTELLA_CONNECT_06, REQUEST_HEADERS);
            _writeState = new OutgoingRequestWriteState();
            _readState = new OutgoingResponseReadState();
        } else  {
            _writeState = new IncomingResponseWriteState();
            _readState = new IncomingRequestReadState();
        }
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
        // Check to see if this is an attempt to connect to ourselves
        InetAddress localAddress = CONNECTION.getSocket().getLocalAddress();
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() &&
            CONNECTION.getSocket().getInetAddress().equals(localAddress) &&
            CONNECTION.getPort() == ConnectionSettings.PORT.getValue()) {
            throw new IOException("Connection to self");
        }      

        RouterService.getAcceptor().setAddress( localAddress );  
        
        if(_headerReader != null)  {
            throw new IllegalStateException("two calls to handshake!");
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
        // lazily initialize the writer to make sure the sockets
        // and IO streams are initialized        
        if(_headerReader == null)  {
            _headerReader = NIOHeaderReader.createReader(CONNECTION); 
        }
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
                
                _headers = HandshakeResponse.createResponse(HEADERS_READ);
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
     * Reads headers with a timeout.
     * 
     * @param timeout the timeout to allow while reading headers
     * @return <tt>true</tt> if all of the headers were successfully read,
     *  otherwise <tt>false</tt>
     * @throws IOException if an IO error occurs while reading the headers
     */
    private boolean readHeaders(int timeout) throws IOException  {
        // TODO: make this work with timeouts??
        return readHeaders();
    }

    /**
     * Writes handshaking data to the network depending on the current state
     * of the handshake.  This may, for example, write the connection
     * request on an outgoing handshake, or it may write the handshake 
     * response.
     * 
     * @throws IOException if there is an IO error writing data to the network
     * @return <tt>true</tt> if all of the data for this write state was 
     *  successfully written, otherwise <tt>false</tt>
     */
    public boolean write() throws IOException  {
        if(_writeState == null)  {
            throw new IllegalStateException("_writeState should not be null");
        }
        _writeState = _writeState.write();
        return !_writeState.hasRemaining();  
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
    
    // inherit doc comment
    public ByteBuffer getRemainingData() {
        return _headerReader.getRemainingData();
    } 
    
    private static void printBuffer(ByteBuffer buffer)  {
        buffer.flip();
        for(int i=0; i<buffer.limit(); i++)  {
            System.out.print((char)buffer.get(i));
        }
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
            if(_requestBuffer.writeBuffer())  {
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
            if(!_responseBuffer.writeBuffer()) {
                _hasRemaining = true;
                return this;
            }
           
            _hasRemaining = false;
            _handshakeAttempts++;
            int code = _ourResponse.getStatusCode();
            //Consider termination...
            if(code == HandshakeResponse.OK) {
                if(HandshakeResponse.OK_MESSAGE.equals(
                    _ourResponse.getStatusMessage())){
                    //a) Terminate normally if we wrote "200 OK".
                    // We're all done writing in this case.
                    _writeComplete = true;
                    CONNECTION.handshakeComplete();
                    
                    // note that hasRemaining is false
                    return this;
                } else {
                    //b) Continue loop if we wrote "200 AUTHENTICATING".
                    
                    // set the state to read the response again
                    _readState = new OutgoingResponseReadState();
                              
                    // should stay in this write state
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

        
        /**
         * Reads the response status to an outgoing hanshake attempt, 
         * progressing to the header reading state if the status line was read
         * correctly.
         * 
         * @return <tt>this</tt> if the expected response status line was not 
         *  completely read, meaning that we cannot progress to the next state, 
         *  and the return value form a read call to a new 
         *  <tt>OutgoingResponseReadHeaderState</tt> if the response line was 
         *  successfully read
         */
        public HandshakeReadState read() throws IOException {
            // read the response status line
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
     * outgoing hanshake attempt.  This is the second step in an outgoing
     * handshake.  First we write, then we read (this step), then we write.
     */
    private final class OutgoingResponseReadHeaderState 
        implements HandshakeReadState  {
        
        /**
         * Constant for the Gnutella connection response line sent back from
         * the remote host.
         */
        private final String RESPONSE_LINE;

        /**
         * Creates a new <tt>OutgoingResponseReadHeaderState</tt> with the 
         * specified response line returned from the remote host.
         * 
         * @param responseLine
         * @throws IOException
         */
        OutgoingResponseReadHeaderState(String responseLine) 
            throws IOException {
            RESPONSE_LINE = responseLine;        
        }
    
        /**
         * Reads the response headers to an outgoing connection handshake 
         * attempt.  This is the final read state for outgoing connections.
         * 
         * @return <tt>this</tt> if the headers were not completely read,
         *  otherwise <tt>null</tt> to indicate that the handshake reading
         *  is complete
         */
        public HandshakeReadState read() throws IOException  {
            // First make sure we read all of the headers.
            if(readHeaders())  {
                _readComplete = true;
            } else {
                return this;
            }

            // Terminate abnormally if we read something other than 200 or 401.
            HandshakeResponse theirResponse = 
                HandshakeResponse.createResponse(
                    RESPONSE_LINE.substring(GNUTELLA_06.length()).trim(), 
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
                new WriteHeaderWrapper(
                    GNUTELLA_06 + " " + _ourResponse.getStatusLine(), 
                        _ourResponse.props());
            
            // Proceed to write our response if we're not already registered
            // for write events. If we are registered for write events, we'll
            // be notified as soon as TCP buffer space becomes available, and
            // we'll write then        
            if(!CONNECTION.writeRegistered())  {
                write();
            }
    
            // we're all done with reading
            return null;
        }
    }
    
    /**
     * This class handles reading an incoming handshake request.
     */
    private final class IncomingRequestReadState 
        implements HandshakeReadState  {
            
        private String _requestLine;    

        /**
         * Reads an incoming handshake request and writes the appropriate 
         * response if possible.
         * 
         * @return <tt>this</tt> if the read did not successfully complete,
         *  otherwise <tt>null</tt>, indicating that all handshake data has 
         *  been read
         * @throws IOException if an IO error occurs during the read
         */
        public HandshakeReadState read() throws IOException {
            // read the response status line

            if(_requestLine == null) {    
                _requestLine = NIOHandshaker.this._headerReader.readConnect();
                
                // stay in this state if we were unable to read the whole line
                if(_requestLine == null) {
                    return this;
                }
                // if they didn't give an expected response format, abort
                if (!notLessThan06(_requestLine))  {
                    throw new IOException("Unexpected connect: "+_requestLine);
                } 
            }    
            
            // First make sure we read all of the headers.
            if(!readHeaders())  {
               return this;
            }
                
            // Proceed to write our response if we're not already registered
            // for write events. If we are registered for write events, we'll
            // be notified as soon as TCP buffer space becomes available, and
            // we'll write then        
            if(!CONNECTION.writeRegistered())  {
                write();
            }
            
            // we're all done with reading the request -- next we need to read
            // their response
            return new IncomingResponseReadStatusState();
            
        }
        
    }
    
    /**
     * Write state for writing the response to an incoming connection request.
     * The response consists of a status line, such as:
     * 
     * GNUTELLA/0.6 503 I am a shielded leaf node 
     * 
     * or:
     * 
     * GNUTELLA/0.6 200 OK
     * 
     * followed by Gnutella connection headers.
     */
    private final class IncomingResponseWriteState 
        extends AbstractHandshakeWriteState  {


        /**
         * Writes the response to an incoming connection request.  Our response
         * depends upon whether or not we have free connection slots available
         * and what features the connecting client supports.
         */
        public HandshakeWriteState write() throws IOException {

            // If we've gone beyond the maximum number of handshake attempts,
            // throw an exception
            if(_handshakeAttempts > MAX_HANDSHAKE_ATTEMPTS) {
                //If we didn't successfully return out of the method, throw an 
                //exception to indicate that handshaking didn't reach any 
                //conclusion.  The values here are kind of a hack.
                throw NoGnutellaOkException.UNRESOLVED_SERVER;
            }
            
            // Is this a connection from the crawler?  We store this data to
            // know whether or not to disconnect later after reading
            _isCrawler = _headers.isCrawler(); 
            
            // If we're not finishing an incomplete write call, 
            if(!_hasRemaining)   {       
                
                //Note: in the following code, it appears that we're ignoring
                //the response code written by the initiator of the connection.
                //However, you can prove that the last code was always 200 OK.
                //See initializeIncoming and the code at the bottom of this
                //loop.
                _ourResponse = 
                    RESPONSE_HEADERS.respond(_headers, false);  
                _responseBuffer = 
                    new WriteHeaderWrapper(
                        GNUTELLA_06 + " " + _ourResponse.getStatusLine(), 
                        _ourResponse.props());
            }
                    
            if(!_responseBuffer.writeBuffer()) {
                _hasRemaining = true;
                return this;
            }

            _hasRemaining = false;
            _writeComplete = true;
            _handshakeAttempts++;
            
            // Our response should be either OK or UNAUTHORIZED for the 
            // handshake to proceed.
            int code = _ourResponse.getStatusCode();
            if((code != HandshakeResponse.OK) && 
               (code != HandshakeResponse.UNAUTHORIZED_CODE)) {
                if(code == HandshakeResponse.SLOTS_FULL) {
                    throw NoGnutellaOkException.CLIENT_REJECT;
                } else {
                    throw NoGnutellaOkException.createClientUnknown(code);
                }
            }
            
            // Otherwise, we're accepting the connection with 
            // GNUTELLA/0.6 200 OK.  There is no next write state for incoming
            // connections, so return this with hasRemaining == false.
            return this;
        }
            
    }

    /**
     * This class handles reading the final response to an incoming handshake
     * request -- the final state of incoming handshakes.
     */
    private final class IncomingResponseReadStatusState 
        implements HandshakeReadState  {

        /**
         * Variable for the response read from the remote host attempting to
         * handshake. 
         */
        private String _responseStatus;
        
        /**
         * Reads the incoming response status line.  If this line is read 
         * completely, we move to reading the headers.  Otherwise, we stay in
         * this state until the response status line is successfully read.
         */
        public HandshakeReadState read() throws IOException {
            if(_ourResponse.getStatusCode() 
               == HandshakeResponse.UNAUTHORIZED_CODE){
      
                // TODO: allow the timeout here
                _responseStatus = 
                    _headerReader.readConnect(USER_INPUT_WAIT_TIME);  
            } else {
                _responseStatus = _headerReader.readConnect(); 
            }
            
            if(_responseStatus == null) {
                return this;
            }

            if (!_responseStatus.startsWith(GNUTELLA_06)) {
                throw new IOException("Bad connect string");
            }
            return new IncomingResponseReadHeaderState(_responseStatus);
        }
            
    }
    
    /**
     * This class handles reading the final response to an incoming handshake
     * request -- the final state of incoming handshakes.
     */
    private final class IncomingResponseReadHeaderState 
        implements HandshakeReadState  {

        /**
         * Constant for the response status  read from the remote host 
         * attempting to handshake. 
         */
        private final String RESPONSE_STATUS;


        IncomingResponseReadHeaderState(String responseStatus) {
            RESPONSE_STATUS = responseStatus;
        }
        
        /**
         * Reads an incoming handshake response -- the final state for incoming
         * connection handshakes.
         * 
         * @return <tt>this</tt> if the read did not successfully complete,
         *  otherwise <tt>null</tt>, indicating that all handshake data has 
         *  been read
         * @throws IOException if an IO error occurs during the read
         */
        public HandshakeReadState read() throws IOException {
            // Read the response from the other side.  If we asked the other
            // side to authenticate, give more time so as to receive user input
            if(_ourResponse.getStatusCode() 
               == HandshakeResponse.UNAUTHORIZED_CODE){
                   
                if(!readHeaders(USER_INPUT_WAIT_TIME)) {
                    return this;
                } 
            } else {
                if(!readHeaders()) {
                    return this;
                }
            }

            HandshakeResponse theirResponse = 
                HandshakeResponse.createResponse(
                    RESPONSE_STATUS.substring(GNUTELLA_06.length()).trim(),
                        HEADERS_READ);


            // Decide whether to proceed.
            int code = _ourResponse.getStatusCode();
            if(code == HandshakeResponse.OK) {
                if(theirResponse.getStatusCode() == HandshakeResponse.OK) {
                    // if it's the crawler, we throw an exception to make  
                    // sure we correctly disconnect
                    if(_isCrawler) {
                        throw new IOException("crawler -- disconnect");
                    }
                      
                    _readComplete = true;
                    
                    // Any leftover message data will be handled by the message
                    // reader
                    CONNECTION.handshakeComplete();
                      
                    //a) If we wrote 200 and they wrote 200 OK, stop normally.
                    return this;
                }
            } else {
                Assert.that(code==HandshakeResponse.UNAUTHORIZED_CODE,
                            "Response code: "+code);
                if(theirResponse.getStatusCode()==HandshakeResponse.OK)  {
                    //b) If we wrote 401 and they wrote "200...", keep looping.
                      
                    return this;
                }
            }
            //c) Terminate abnormally
            throw NoGnutellaOkException.
                createServerUnknown(theirResponse.getStatusCode());            
        }
    }
        
    /**
     * Helper class that takes care of recording written message headers and
     * creating <tt>ByteBuffer</tt>s for writing.
     */
    private final class WriteHeaderWrapper {
        
        /**
         * Variable specifying whether or not the <tt>ByteBuffer</tt> has been
         * flipped.
         */
        private boolean _flipped;
    
        /**
         * Constant for the headers to write.  This is stored to enable the 
         * recording of headers once they are actually written to the network.
         */
        private final Properties HEADERS;

        /**
         * Constant for the <tt>ByteBuffer</tt> instance containing all of the
         * bytes to be written, both from the message request or response line
         * and from the headers.
         */    
        private final ByteBuffer BUFFER;
         
        /**
         * @param statusLine the Gnutella request or response message to write
         * @param headers the Gnutella 0.6 (HTTP style) message headers to 
         *  write
         */
        public WriteHeaderWrapper(String statusLine, Properties headers) {
            HEADERS = headers;
            BUFFER = createBuffer(statusLine, headers);
        }
        
        /**
         * Make sure we only flip the write buffer once.
         */
        public void flip() {
            if(!_flipped) {
                BUFFER.flip();
            }
        }

        /**
         * Records the fact that the headers for this wrapper have been written.
         * This MUST only be called once all headers have actually been written
         * to the network.
         */
        void recordHeaderWrites() {
            Enumeration headers = HEADERS.keys();
            while(headers.hasMoreElements()) {
                String key = (String)headers.nextElement();
                String value = (String)HEADERS.getProperty(key);
            
                HEADERS_WRITTEN.put(key, value);
            }
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
        private boolean writeBuffer() throws IOException  {

            // Make sure we flip the buffer for writing if we haven't already
            flip();
        
            //printBuffer(buffer);
        
            CONNECTION.getSocket().getChannel().write(BUFFER);
            if(BUFFER.hasRemaining())  {
            
                if(!CONNECTION.writeRegistered())  {
                    NIODispatcher.instance().addWriter(CONNECTION);
                } 
                
                // we need to stay registered and keep writing
                return false;
            }

            
            // Make sure all written headers are recorded
            recordHeaderWrites();

            return true;       
        }
    }
}
