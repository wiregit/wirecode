package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Properties;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.http.HTTPHeader;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.List;

/**
 * This class performs non-blocking handshaking, responding to read and write
 * events from the NIO selector.
 */
public class NIOHandshaker extends AbstractHandshaker {

    private ByteBuffer _responseBuffer;

    private ByteBuffer _requestBuffer;

    private boolean _registered;


    private boolean _handshakeComplete;

    

    public static Handshaker createHandshaker(Connection conn,
        Properties requestHeaders, HandshakeResponder responseHeaders)  {
        return new NIOHandshaker(conn, requestHeaders, responseHeaders);
    }
    
    private NIOHandshaker(Connection conn, 
        Properties requestHeaders, HandshakeResponder responseHeaders)  {
        super(conn, requestHeaders, responseHeaders);
        if(CONNECTION.isOutgoing())  {
            _requestBuffer = createRequestBuffer(REQUEST_HEADERS);
        }
    }
    
    
    private static ByteBuffer createRequestBuffer(Properties headers)  {
        StringBuffer sb = new StringBuffer();
        Enumeration headerNames = headers.keys();
        Enumeration headerValues = headers.elements();
        while(headerNames.hasMoreElements())  {
            sb.append(headerNames.nextElement());
            sb.append(": ");
            sb.append(headerValues.nextElement());
            sb.append(CRLF);
        }
        sb.append(CRLF);
        
        byte[] connectBytes = (GNUTELLA_CONNECT_06+CRLF).getBytes();
        byte[] headerBytes = sb.toString().getBytes();
        ByteBuffer buffer = 
            ByteBuffer.allocate(connectBytes.length+headerBytes.length);
        buffer.put(connectBytes);
        buffer.put(headerBytes);
        return buffer;
    }

    private ByteBuffer createResponseBuffer(Properties headers) {
        if(CONNECTION.isOutgoing())  {
            //return ByteBuffer.allocate(1024); 
        } 
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#handshake()
     */
    public boolean handshake() throws IOException, NoGnutellaOkException {
        // lazily initialize the readers and writers to make sure the sockets
        // and IO streams are initialized        
        if(_headerWriter == null) {
            _headerWriter = NIOHeaderWriter.createWriter(CONNECTION); 
        }
        if(_headerReader == null)  {
            _headerReader = NIOHeaderReader.createReader(CONNECTION); 
        }
        
        // write our request headers if it's an outgoing handshake
        if(CONNECTION.isOutgoing()) {
            return write();
        }
        return true;
    }
    
    public void read() throws IOException  {
        while(true)  {
            HTTPHeader header = _headerReader.readHeader();
            if(_headerReader.headersComplete()) {
                // we're all done
                break;
            } 
            if(header == null)  {
                // we need to finish reading on the next pass
                return;
            } 
            
            handleRemoteIP(header);
            HEADERS_READ.put(header.getHeaderNameString(),
                header.getHeaderValueString());
        }
        // create the response for the headers read
        createResponseBuffer(HEADERS_READ);
        write();
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
        if(CONNECTION.isOutgoing() && _requestBuffer.hasRemaining())  {
            // if this returns true, this will no longer be registered for
            // write events, and will only get registered again if there
            // are incomplete writes
            return writeBuffer(_requestBuffer);
        } else if(_responseBuffer.hasRemaining()) {
            return writeBuffer(_responseBuffer);
        }
        return true ;      
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
            if(!_registered)  {
                NIODispatcher.instance().addWriter(CONNECTION);
            } 
                
            // we need to stay registered and keep writing
            return false;
        }
        return true;       
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#handshakeComplete()
     */
    public boolean handshakeComplete() {
        return _handshakeComplete;
    }    


    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#setWriteRegistered(boolean)
     */
    public synchronized void setWriteRegistered(boolean registered) {
        _registered = registered;
    }
    
    /**
     * Interface for executing the current handshake state, following the state
     * pattern.
     */
    private static interface HandshakeState  {
        public HandshakeState handshake() throws IOException;
    }
    
    
    private final class OutgoingConnect implements HandshakeState  {
        
        // inherit doc comment
        public HandshakeState handshake() throws IOException {
            // Send "GNUTELLA CONNECT/0.6" and headers
            if(writeBuffer(_requestBuffer)) {
                   
            }
            
            if(_requestBuffer.hasRemaining()) {
                return this;
            } else {
                return null;
            }      
        }
    }
    
    /**
     * State that sends Gnutella connection headers for the initial phase of
     * an outgoing connection.
     */
    private final class OutgoingHeaderState implements HandshakeState  {
        public HandshakeState handshake() throws IOException {
            
            // if we already have buffered data to write, try to write it
            if(_headerWriter.hasBufferedData())  {
                
                // if we're still unable to write the data, return and try on
                // the next pass
                if(!_headerWriter.write())  {
                    return this;
                }
            }
            
            // otherwise, keep trying to write
            boolean allHeadersWritten = true;
            Enumeration enum = REQUEST_HEADERS.propertyNames();
            List headersToRemove = new LinkedList();
            while (enum.hasMoreElements()) {
                String key = (String)enum.nextElement();
                String value = REQUEST_HEADERS.getProperty(key);
                // Overwrite any domain name with true IP address
                if ( HeaderNames.REMOTE_IP.equals(key) )
                    value = 
                        CONNECTION.getSocket().getInetAddress().getHostAddress();
                if (value==null)
                    value = "";
                boolean headerWritten = 
                    _headerWriter.writeHeader(key+": "+value+CRLF);   
                headersToRemove.add(key);
                if(headerWritten) {
                    HEADERS_WRITTEN.put(key, value);
                } else  {
                    allHeadersWritten = false;
                    break;
                }
            }
        

            if(allHeadersWritten) {
                //send the trailer
                if(_headerWriter.closeHeaderWriting())  {
                    // TODO: this is wrong!! switch it back!!
                    //return new ReadOutgoingResponseState();
                    return this;
                } else  {
                    return this;
                }
            
            } else {
                // remove any headers that we've written
                Iterator iter = headersToRemove.iterator();
                while(iter.hasNext())  {
                    String key = (String)iter.next();
                    REQUEST_HEADERS.remove(key);
                }
                headersToRemove.clear();
                return this;           
            }                       
        }
    }
    
    /*
    private final class ReadOutgoingResponseState implements HandshakeState  {
        
        private String _connectLine;
        
        public HandshakeState handshake() throws IOException  {
            //This step may involve handshaking multiple times so as
            //to support challenge/response kind of behaviour
            for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++) {

                //2. Read "GNUTELLA/0.6 200 OK"  
                _connectLine = _headerReader.readHeader();
                String connectLine = readLine();
                Assert.that(connectLine != null, "null connectLine");
                if (! connectLine.startsWith(GNUTELLA_06))
                    throw new IOException("Bad connect string");
            
                //3. Read the Gnutella headers. 
                readHeaders();

                //Terminate abnormally if we read something other than 200 or 401.
                HandshakeResponse theirResponse = 
                    HandshakeResponse.createResponse(
                        connectLine.substring(GNUTELLA_06.length()).trim(), 
                        HEADERS_READ);
                Assert.that(theirResponse != null, "null theirResponse");

                int code = theirResponse.getStatusCode();
                if (code != HandshakeResponse.OK &&  
                    code != HandshakeResponse.UNAUTHORIZED_CODE) {
                    if(code == HandshakeResponse.SLOTS_FULL) {
                        throw NoGnutellaOkException.SERVER_REJECT;
                    } else {
                        throw NoGnutellaOkException.createServerUnknown(code);
                    }
                }

                //4. Write "GNUTELLA/0.6" plus response code, such as "200 OK", 
                //   and headers.
                Assert.that(RESPONSE_HEADERS != null, "null RESPONSE_HEADERS");
                HandshakeResponse ourResponse = 
                    RESPONSE_HEADERS.respond(theirResponse, true);

                Assert.that(ourResponse != null, "null ourResponse");
                writeLine(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF);
                sendHeaders(ourResponse.props());

                code = ourResponse.getStatusCode();
                //Consider termination...
                if(code == HandshakeResponse.OK) {
                    if(HandshakeResponse.OK_MESSAGE.equals(
                        ourResponse.getStatusMessage())){
                        //a) Terminate normally if we wrote "200 OK".
                        return null;
                    } else {
                        //b) Continue loop if we wrote "200 AUTHENTICATING".
                        continue;
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
            
            //If we didn't successfully return out of the method, throw an exception
            //to indicate that handshaking didn't reach any conclusion.  The values
            //here are kind of a hack.
            throw NoGnutellaOkException.UNRESOLVED_SERVER;
        }
    }
    
    private final class WriteOutgoingResponseState implements HandshakeState  {
        public HandshakeState handshake() throws IOException  {
        
        }
    }
    
    
    private final class IncomingConnect implements HandshakeState  {
        
        // inherit doc comment
        public HandshakeState handshake() throws IOException {
            //1. Send "GNUTELLA CONNECT/0.6" and headers
            if(_headerWriter.hasBufferedData())  {
                if(_headerWriter.write())  {
                    return new OutgoingHeaderPhase();
                }
            }
            boolean wroteOutgoingConnect = 
                _headerWriter.writeHeader(GNUTELLA_CONNECT_06+CRLF);
            if(wroteOutgoingConnect)  {
                return new OutgoingHeaderPhase();
            } else if(!_registered) {
                NIODispatcher.instance().addWriter(CONNECTION);
            }   
            return this;                        
        }
    }
    */
}
