package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.http.HTTPHeader;

/**
 * 
 */
public final class BIOHandshaker extends AbstractHandshaker {


    private boolean _handshakeComplete;
    
    /**
     * Time to wait for inut from user at the remote end. (in milliseconds)
     */
    public static final int USER_INPUT_WAIT_TIME = 2 * 60 * 1000; //2 min
    
    /**
     * The number of times we will respond to a given challenge 
     * from the other side, or otherwise, during connection handshaking
     */
    public static final int MAX_HANDSHAKE_ATTEMPTS = 5;  
    
    /**
     * The <tt>HandshakeResponse</tt> wrapper for the connection headers.
     */
    private HandshakeResponse _headers = 
        HandshakeResponse.createEmptyResponse();       

    public static Handshaker createHandshaker(Connection conn, 
        Properties requestHeaders, HandshakeResponder responseHeaders)  {
        return new BIOHandshaker(conn, requestHeaders, responseHeaders);
    }
    
    /**
     * Creates a new <tt>BIOHandshaker</tt> for the specified connection.
     * 
     * @param conn the <tt>Connection</tt> that this will handshake for
     * @param requestHeaders the headers for incoming request headers
     * @param responseHeaders the headers we respond with
     */
    private BIOHandshaker(Connection conn, Properties requestHeaders, 
        HandshakeResponder responseHeaders)  {
        super(conn, requestHeaders, responseHeaders); 
    }
    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#handshake()
     */
    public boolean handshake() throws IOException, NoGnutellaOkException {  
        
        _headerWriter = BIOHeaderWriter.createWriter(CONNECTION); 
        
        _headerReader = BIOHeaderReader.createReader(CONNECTION);     
        //In all the line reading code below, we are somewhat lax in
        //distinguishing between '\r' and '\n'.  Who cares?
        if(CONNECTION.isOutgoing())
            initializeOutgoing();
        else
            initializeIncoming();      
            
        _handshakeComplete = true ; 
        return true;       
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#handshakeComplete()
     */
    public boolean handshakeComplete() {
        return _handshakeComplete;
    }
    
    /** 
     * Sends and receives handshake strings for outgoing connections,
     * throwing exception if any problems. 
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  
     */
    private void initializeOutgoing() throws IOException {
        //1. Send "GNUTELLA CONNECT/0.6" and headers
        _headerWriter.writeHeader(GNUTELLA_CONNECT_06+CRLF);
        
        sendHeaders(REQUEST_HEADERS);   
        
        //conclude the handshake (This may involve exchange of 
        //information multiple times with the host at the other end).
        concludeOutgoingHandshake();
    }
    
    /**
     * Responds to the responses/challenges from the host on the other
     * end of the connection, till a conclusion reaches. Handshaking may
     * involve multiple steps.  
     *
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  
     */
    protected void concludeOutgoingHandshake() throws IOException {
        //This step may involve handshaking multiple times so as
        //to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++) {

            //2. Read "GNUTELLA/0.6 200 OK"  
            String connectLine = _headerReader.readConnect();
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
            _headerWriter.writeHeader(GNUTELLA_06 + " " + 
                ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.props());

            code = ourResponse.getStatusCode();
            //Consider termination...
            if(code == HandshakeResponse.OK) {
                if(HandshakeResponse.OK_MESSAGE.equals(
                    ourResponse.getStatusMessage())){
                    //a) Terminate normally if we wrote "200 OK".
                    return;
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
    
    /** 
     * Sends and receives handshake strings for incoming connections,
     * throwing exception if any problems. 
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException if there's an unexpected connect string or
     *  any other problem
     */
    private void initializeIncoming() throws IOException {
        //Dispatch based on first line read.  Remember that "GNUTELLA " has
        //already been read by Acceptor.  Hence we are looking for "CONNECT/0.6"
        //String connectString = readLine();
        String connectString = _headerReader.readConnect();
        if (notLessThan06(connectString)) {
            //1. Read headers (connect line has already been read)
            readHeaders();
            //Conclude the handshake (This may involve exchange of information
            //multiple times with the host at the other end).
            concludeIncomingHandshake();
        } else {
            throw new IOException("Unexpected connect string: "+connectString);
        }
    }

    /** 
     * Returns true iff line ends with "CONNECT/N", where N
     * is a number greater than or equal "0.6". 
     */
    private static boolean notLessThan06(String line) {
        int i = line.indexOf(CONNECT);
        if (i<0)
            return false;
        try {
            Float F = new Float(line.substring(i+CONNECT.length()));
            float f = F.floatValue();
            return f >= 0.6f;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Responds to the handshake from the host on the other
     * end of the connection, till a conclusion reaches. Handshaking may
     * involve multiple steps.
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  May wish to retry at 0.4
     */
    protected void concludeIncomingHandshake() throws IOException {
        //Respond to the handshake.  This step may involve handshaking multiple
        //times so as to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++){
            //2. Send our response and headers.

            // is this an incoming connection from the crawler??
            boolean isCrawler = _headers.isCrawler();
            
            //Note: in the following code, it appears that we're ignoring
            //the response code written by the initiator of the connection.
            //However, you can prove that the last code was always 200 OK.
            //See initializeIncoming and the code at the bottom of this
            //loop.
            HandshakeResponse ourResponse = 
                RESPONSE_HEADERS.respond(_headers, false);

            _headerWriter.writeHeader(GNUTELLA_06 + " " + 
                ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.props());                   
            //Our response should be either OK or UNAUTHORIZED for the handshake
            //to proceed.
            int code = ourResponse.getStatusCode();
            if((code != HandshakeResponse.OK) && 
               (code != HandshakeResponse.UNAUTHORIZED_CODE)) {
                if(code == HandshakeResponse.SLOTS_FULL) {
                    throw NoGnutellaOkException.CLIENT_REJECT;
                } else {
                    throw NoGnutellaOkException.createClientUnknown(code);
                }
            }
                    
            //3. read the response from the other side.  If we asked the other
            //side to authenticate, give more time so as to receive user input
            String connectLine;
            if(ourResponse.getStatusCode() == 
               HandshakeResponse.UNAUTHORIZED_CODE){ 
                connectLine = _headerReader.readConnect(USER_INPUT_WAIT_TIME);
                readHeaders(USER_INPUT_WAIT_TIME); 
                _headers = HandshakeResponse.createResponse(HEADERS_READ);
            } else{
                connectLine = _headerReader.readConnect();  
                readHeaders();
            }
            
            if (! connectLine.startsWith(GNUTELLA_06))
                throw new IOException("Bad connect string");

            HandshakeResponse theirResponse = 
                HandshakeResponse.createResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(),
                    HEADERS_READ);


            //Decide whether to proceed.
            code = ourResponse.getStatusCode();
            if(code == HandshakeResponse.OK) {
                if(theirResponse.getStatusCode() == HandshakeResponse.OK) {
                    // if it's the crawler, we throw an exception to make sure 
                    // we correctly disconnect
                    if(isCrawler) {
                        throw new IOException("crawler connection-disconnect");
                    }
                    //a) If we wrote 200 and they wrote 200 OK, stop normally.
                    return;
                } 
            } else {
                Assert.that(code==HandshakeResponse.UNAUTHORIZED_CODE,
                            "Response code: "+code);
                if(theirResponse.getStatusCode()==HandshakeResponse.OK)
                    //b) If we wrote 401 and they wrote "200...", keep looping.
                    continue;
            }
            //c) Terminate abnormally
            throw NoGnutellaOkException.
                createClientUnknown(theirResponse.getStatusCode());
        }        
        
        //If we didn't successfully return out of the method, throw an exception
        //to indicate that handshaking didn't reach any conclusion.  The values
        //here are kind of a hack.
        throw NoGnutellaOkException.UNRESOLVED_CLIENT;
    }
    
    /**
     * Reads the properties from the network into HEADERS_READ, throwing
     * IOException if there are any problems. 
     *     @modifies network 
     */
    private void readHeaders() throws IOException {
        readHeaders(Constants.TIMEOUT);
        _headers = HandshakeResponse.createResponse(HEADERS_READ);
    }
    
    /**
     * Reads the properties from the network into HEADERS_READ, throwing
     * IOException if there are any problems. 
     * @param timeout The time to wait on the socket to read data before 
     * IOException is thrown
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private void readHeaders(int timeout) throws IOException {
        int headersRead = 0;
        while (headersRead < 30) {
            //This doesn't distinguish between \r and \n.  That's fine.
            HTTPHeader header = _headerReader.readHeader(timeout);
            
            if (header==null)  {
                return;                    //blank line ==> done 
            }
                
            //TODO: authentication broken

            handleRemoteIP(header);
            HEADERS_READ.put(header.getHeaderNameString(), 
                header.getHeaderValueString());
            headersRead++;
        }
    }
    
    /**
     * Writes the properties in props to network, including the blank line at
     * the end.  Throws IOException if there are any problems.
     * @param props The headers to be sent. Note: null argument is 
     * acceptable, if no headers need to be sent (still the trailer will
     * be sent
     * @modifies network 
     */
    private void sendHeaders(Properties props) throws IOException {
        if(props != null) {
            Enumeration enum=props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String)enum.nextElement();
                String value = props.getProperty(key);
                // Overwrite any domainname with true IP address
                if ( HeaderNames.REMOTE_IP.equals(key) )
                    value = 
                        CONNECTION.getSocket().getInetAddress().getHostAddress();
                if (value==null)
                    value = "";
                _headerWriter.writeHeader(key+": "+value+CRLF);   
                HEADERS_WRITTEN.put(key, value);
            }
        }
        //send the trailer
        _headerWriter.closeHeaderWriting();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#setWriteRegistered(boolean)
     */
    public void setWriteRegistered(boolean registered) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#write()
     */
    public boolean write() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.Handshaker#read()
     */
    public void read() {
        // TODO Auto-generated method stub
        
    }
}
