package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import java.util.Enumeration;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.*;

/**
 * A Gnutella messaging connection.  Provides handshaking functionality and
 * routines for reading and writing of Gnutella messages.  A connection is
 * either incoming (created from a Socket) or outgoing (created from an
 * address).  This class does not provide sophisticated buffering or routing
 * logic; use ManagedConnection for that. <p>
 *
 * You will note that the constructors don't actually involve the network and
 * hence never throw exceptions or block. <b>To actual initialize a connection,
 * you must call initialize().</b> While this is somewhat awkward, it is
 * intentional.  It makes it easier, for example, for the GUI to show
 * uninitialized connections.<p>
 *
 * <tt>Connection</tt> supports only 0.6 handshakes.  Gnutella 0.6 connections
 * have a list of properties read and written during the handshake sequence.
 * Typical property/value pairs might be "Query-Routing: 0.3" or "User-Agent:
 * LimeWire".  
 *
 * This class augments the basic 0.6 handshaking mechanism to allow
 * authentication via "401" messages.  Authentication interactions can take
 * multiple rounds.  
 */
public class Connection {
    /** 
     * The underlying socket, its address, and input and output streams.  sock,
     * in, and out are null iff this is in the unconnected state.  For thread
     * synchronization reasons, it is important that this only be modified by
     * the send(m) and receive() methods.
     */
    private final String _host;
    private int _port;
    private Socket _socket;
    private InputStream _in;
    private OutputStream _out;
    private final boolean OUTGOING;

    /** The possibly non-null VendorMessagePayload which describes what
     *  VendorMessages the guy on the other side of this connection supports.
     */
    protected MessagesSupportedVendorMessage _messagesSupported = null;
    
    /**
     * Trigger an opening connection to close after it opens.  This
     * flag is set in shutdown() and then checked in initialize()
     * to insure the _socket.close() happens if shutdown is called
     * asynchronously before initialize() completes.  Note that the 
     * connection may have been remotely closed even if _closed==true.  
     * Protected (instead of private) for testing purposes only.
     */
    protected volatile boolean _closed=false;

    /** 
	 * The headers read from the connection.
	 */
    private final Properties HEADERS_READ = new Properties();

    /**
     * The <tt>HandshakeResponse</tt> wrapper for the connection headers.
     */
	private HandshakeResponse _headers = 
        HandshakeResponse.createEmptyResponse();

    /** For outgoing Gnutella 0.6 connections, the properties written
     *  after "GNUTELLA CONNECT".  Null otherwise. */
    private final Properties REQUEST_HEADERS;

    /** For outgoing Gnutella 0.6 connections, a function calculating the
     *  properties written after the server's "GNUTELLA OK".  For incoming
     *  Gnutella 0.6 connections, the properties written after the client's
     *  "GNUTELLA CONNECT". */
    private final HandshakeResponder RESPONSE_HEADERS;

    /** The list of all properties written during the handshake sequence,
     *  analogous to HEADERS_READ.  This is needed because
     *  RESPONSE_HEADERS lazily calculates properties according to what it
     *  read. */
    private final Properties HEADERS_WRITTEN = new Properties();

	/**
	 * Gnutella 0.6 connect string.
	 */
    private String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";

	/**
	 * Gnutella 0.6 accept connection string.
	 */
    public static final String GNUTELLA_OK_06 = "GNUTELLA/0.6 200 OK";
    public static final String GNUTELLA_06 = "GNUTELLA/0.6";
    public static final String _200_OK     = " 200 OK";
    public static final String GNUTELLA_06_200 = "GNUTELLA/0.6 200";
    public static final String CONNECT="CONNECT/";
    /** End of line for Gnutella 0.6 */
    public static final String CRLF="\r\n";
    
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
     * The time in milliseconds since 1970 that this connection was
     * established.
     */
    private long _connectionTime = Long.MAX_VALUE;


    /** if I am a Ultrapeer shielding the given connection */
    private Boolean _isLeaf=null;
    /** if I am a leaf connected to a supernode  */
    private Boolean _isUltrapeer=null;

    /**
     * The "soft max" ttl to use for this connection.
     */
    private byte _softMax;

    /**
     * Creates an uninitialized outgoing Gnutella 0.6 connection with the
     * desired outgoing properties, possibly reverting to Gnutella 0.4 if
     * needed.
     * 
     * If properties1 and properties2 are null, forces connection at the 0.4
     * level.  This is a bit of a hack to make implementation in this and
     * subclasses easier; outside classes are discouraged from using it.
     *
     * @param host the name of the host to connect to
     * @param port the port of the remote host
     * @param requestHeaders the headers to be sent after "GNUTELLA CONNECT"
     * @param responseHeaders a function returning the headers to be sent
     *  after the server's "GNUTELLA OK".  Typically this returns only
     *  vendor-specific properties.
	 * @throws <tt>NullPointerException</tt> if any of the arguments are
	 *  <tt>null</tt>
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public Connection(String host, int port,
                      Properties requestHeaders,
                      HandshakeResponder responseHeaders) {

		if(host == null) {
			throw new NullPointerException("null host");
		}
		if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentException("illegal port: "+port);
		}
		if(requestHeaders == null) {
			throw new NullPointerException("null request headers");
		}
		if(responseHeaders == null) {
			throw new NullPointerException("null response headers");
		}		

        _host = host;
        _port = port;
        OUTGOING = true;
        REQUEST_HEADERS = requestHeaders;
        RESPONSE_HEADERS = responseHeaders;            
		if(!CommonUtils.isJava118()) {
			ConnectionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
		}
    }

    /**
     * Creates an uninitialized incoming 0.6 Gnutella connection. If the
	 * client is attempting to connect using an 0.4 handshake, it is
	 * rejected.
     * 
     * @param socket the socket accepted by a ServerSocket.  The word
     *  "GNUTELLA " and nothing else must have been read from the socket.
     * @param responseHeaders the headers to be sent in response to the client's 
	 *  "GNUTELLA CONNECT".  
	 * @throws <tt>NullPointerException</tt> if any of the arguments are
	 *  <tt>null</tt>
     */
    public Connection(Socket socket, HandshakeResponder responseHeaders) {
		if(socket == null) {
			throw new NullPointerException("null socket");
		}
		if(responseHeaders == null) {
			throw new NullPointerException("null response headers");
		}
        //Get the address in dotted-quad format.  It's important not to do a
        //reverse DNS lookup here, as that can block.  And on the Mac, it blocks
        //your entire system!
        _host = socket.getInetAddress().getHostAddress();
        _port = socket.getPort();
        _socket = socket;
        OUTGOING = false;
        RESPONSE_HEADERS = responseHeaders;	
		REQUEST_HEADERS = null;
		if(!CommonUtils.isJava118()) {
			ConnectionStat.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
		}
    }


    /** Call this method when the Connection has been initialized and accepted
     *  as 'long-lived'.
     */
    protected void postInit() {
        try { // TASK 1 - Send a MessagesSupportedVendorMessage if necessary....
			if(_headers.supportsVendorMessages()) {
                send(MessagesSupportedVendorMessage.instance());
			}
        }
        catch (IOException ioe) {
        }
        catch (BadPacketException bpe) {
            bpe.printStackTrace();  // we don't really expect this....
        }
    }

    /**
     * Call this method when you want to handle us to handle a VM.  We may....
     */
    protected void handleVendorMessage(VendorMessage vm) {
        if (vm instanceof MessagesSupportedVendorMessage)
            _messagesSupported = (MessagesSupportedVendorMessage) vm;
    }


    /** 
     * Initializes this without timeout; exactly like initialize(0). 
     * @see initialize(int)
     */
    public void initialize() 
		throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(0);
    }

    /**
     * Initialize the connection by doing the handshake.  Throws IOException
     * if we were unable to establish a normal messaging connection for
     * any reason.  Do not call send or receive if this happens.
     *
     * @param timeout for outgoing connections, the timeout in milliseconds
     *  to use in establishing the socket, or 0 for no timeout.  If the 
     *  platform does not support native timeouts, it will be emulated with
     *  threads.
     * @exception IOException we were unable to connect to the host
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception BadHandshakeException some other problem establishing 
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during handshaking, etc.
     */
    public void initialize(int timeout) 
		throws IOException, NoGnutellaOkException, BadHandshakeException {
        String expectString;
 
        if(isOutgoing())
            _socket=Sockets.connect(_host, _port, timeout);

        // Check to see if close() was called while the socket was initializing
        if (_closed) {
            _socket.close();
            throw new IOException("socket is closed");
        } 
        
        // Check to see if this is an attempt to connect to ourselves
		InetAddress localAddress = _socket.getLocalAddress();
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() &&
            _socket.getInetAddress().equals(localAddress) &&
            _port == SettingsManager.instance().getPort()) {
            throw new IOException("Connection to self");
        }

        try {
            // Set the Acceptors IP address
            RouterService.getAcceptor().setAddress( localAddress );
            
            _in = getInputStream();
            _out = getOutputStream();
            if (_in == null) throw new IOException("null input stream");
			else if(_out == null) throw new IOException("null output stream");
        } catch (Exception e) {
            //Apparently Socket.getInput/OutputStream throws
            //NullPointerException if the socket is closed.  (See Sun bug
            //4091706.)  Unfortunately the socket may have been closed after the
            //the check above, e.g., if the user pressed disconnect.  So we
            //catch NullPointerException here--and any other weird possible
            //exceptions.  An alternative is to obtain a lock before doing these
            //calls, but we are afraid that getInput/OutputStream may be a
            //blocking operation.  Just to be safe, we also check that in/out
            //are not null.
            close();
            throw new IOException("could not establish connection");
        }

        try {
            //In all the line reading code below, we are somewhat lax in
            //distinguishing between '\r' and '\n'.  Who cares?
            if(isOutgoing())
                initializeOutgoing();
            else
                initializeIncoming();

            _headers = HandshakeResponse.createResponse(HEADERS_READ);            
            _connectionTime = System.currentTimeMillis();

            // Now set the soft max TTL that should be used on this connection.
            // The +1 on the soft max for "good" connections is because the message
            // may come from a leaf, and therefore can have an extra hop.
            // "Good" connections are connections with features such as 
            // intra-Ultrapeer QRP passing.
            if(isGoodUltrapeer()) {
                _softMax = (byte)(_headers.getMaxTTL()+(byte)1);
            } else {
                _softMax = ConnectionSettings.SOFT_MAX.getValue();
            }
						
        } catch (NoGnutellaOkException e) {
            close();
            throw e;
        } catch (IOException e) {
            close();
            throw new BadHandshakeException(e);
        }
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
        sendString(GNUTELLA_CONNECT_06+CRLF);
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
    private void concludeOutgoingHandshake() throws IOException {
        //This step may involve handshaking multiple times so as
        //to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++) {

			//2. Read "GNUTELLA/0.6 200 OK"  
			String connectLine = readLine();
			if (! connectLine.startsWith(GNUTELLA_06))
				throw new IOException("Bad connect string");
			
			//3. Read the Gnutella headers. 
			readHeaders();

            //Terminate abnormally if we read something other than 200 or 401.
            HandshakeResponse theirResponse = 
                HandshakeResponse.createResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(), 
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

            //4. Write "GNUTELLA/0.6" plus response code, such as "200 OK", 
			//   and headers.
            HandshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(theirResponse, true);

            sendString(GNUTELLA_06 + " " 
                + ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.props());

            code = ourResponse.getStatusCode();
            //Consider termination...
            if(code == HandshakeResponse.OK) {
                if(ourResponse.getStatusMessage().equals(
                    HandshakeResponse.OK_MESSAGE)){
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
		String connectString = readLine();
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
     * Responds to the handshake from the host on the other
     * end of the connection, till a conclusion reaches. Handshaking may
     * involve multiple steps.
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  May wish to retry at 0.4
     */
    private void concludeIncomingHandshake() throws IOException {
        //Respond to the handshake.  This step may involve handshaking multiple
        //times so as to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++){
            //2. Send our response and headers.

			//Note: in the following code, it appears that we're ignoring
			//the response code written by the initiator of the connection.
			//However, you can prove that the last code was always 200 OK.
			//See initializeIncoming and the code at the bottom of this
			//loop.
			HandshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(_headers, false);

            sendString(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF);
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
            if(ourResponse.getStatusCode() 
               == HandshakeResponse.UNAUTHORIZED_CODE){
                connectLine = readLine(USER_INPUT_WAIT_TIME);  
                readHeaders(USER_INPUT_WAIT_TIME); 
                _headers = HandshakeResponse.createResponse(HEADERS_READ);
            }else{
                connectLine = readLine();  
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
                if(theirResponse.getStatusCode()==HandshakeResponse.OK)
                    //a) If we wrote 200 and they wrote 200 OK, stop normally.
                    return;
            } else {
                Assert.that(code==HandshakeResponse.UNAUTHORIZED_CODE,
                            "Response code: "+code);
                if(theirResponse.getStatusCode()==HandshakeResponse.OK)
                    //b) If we wrote 401 and they wrote "200...", keep looping.
                    continue;
            }
            //c) Terminate abnormally
            throw NoGnutellaOkException.
                createServerUnknown(theirResponse.getStatusCode());
        }        
        
        //If we didn't successfully return out of the method, throw an exception
        //to indicate that handshaking didn't reach any conclusion.  The values
        //here are kind of a hack.
        throw NoGnutellaOkException.UNRESOLVED_CLIENT;
    }
    
    /** Returns true iff line ends with "CONNECT/N", where N
     *  is a number greater than or equal "0.6". */
    private static boolean notLessThan06(String line) {
        int i=line.indexOf(CONNECT);
        if (i<0)
            return false;
        try {
            Float F = new Float(line.substring(i+CONNECT.length()));
            float f= F.floatValue();
            return f>=0.6f;
        } catch (NumberFormatException e) {
            return false;
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
                String key=(String)enum.nextElement();
                String value=props.getProperty(key);
                // Overwrite any domainname with true IP address
                if ( HeaderNames.REMOTE_IP.equals(key) )
                    value=getInetAddress().getHostAddress();
                if (value==null)
                    value="";
                sendString(key+": "+value+CRLF);   
                HEADERS_WRITTEN.put(key, value);
            }
        }
        //send the trailer
        sendString(CRLF);
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
     * @return The line of characters read
     * @modifies network
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private void readHeaders(int timeout) throws IOException {
        //TODO: limit number of headers read
        while (true) {
            //This doesn't distinguish between \r and \n.  That's fine.
            String line=readLine(timeout);
            if (line==null)
                throw new IOException("unexpected end of file"); //unexpected EOF
            if (line.equals(""))
                return;                    //blank line ==> done
            int i=line.indexOf(':');
            if (i<0)
                continue;                  //ignore lines without ':'
            String key=line.substring(0, i);
            String value=line.substring(i+1).trim();
            HEADERS_READ.put(key, value);
        }
    }

    /**
     * Writes s to out, with no trailing linefeeds.  Called only from
     * initialize().  
     *    @requires _socket, _out are properly set up */
    private void sendString(String s) throws IOException {
        if(s == null || s.equals("")) {
            throw new NullPointerException("null or empty string: "+s);
        }

        //TODO: character encodings?
        byte[] bytes=s.getBytes();
        _out.write(bytes);
        _out.flush();
    }
    
    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within TIMEOUT milliseconds (as defined by the
     * property manager), throws IOException.  This includes EOF.
     * @return The line of characters read
     * @requires _socket is properly set up
     * @modifies network
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private String readLine() throws IOException {
        return readLine(Constants.TIMEOUT);
    }

    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within the specified timeout milliseconds,
     * throws IOException.  This includes EOF.
     * @param timeout The time to wait on the socket to read data before 
     * IOException is thrown
     * @return The line of characters read
     * @requires _socket is properly set up
     * @modifies network
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private String readLine(int timeout) throws IOException {
        int oldTimeout=_socket.getSoTimeout();
        try {
            _socket.setSoTimeout(timeout);
            String line=(new ByteReader(_in)).readLine();
            if (line==null)
                throw new IOException("read null line");
            return line;
        } finally {
            //Restore socket timeout.
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /** Returns the stream to use for writing to s.  By default this is a
     *  BufferedOutputStream.  Subclasses may override to decorate the
     *  stream. */
    protected OutputStream getOutputStream()  throws IOException {
        return new BufferedOutputStream(_socket.getOutputStream());
    }

    /** Returns the stream to use for reading from s.  By default this is a
     *  BufferedInputStream.  Subclasses may override to decorate the stream. */
    protected InputStream getInputStream() throws IOException {
        return new BufferedInputStream(_socket.getInputStream());
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * Used to determine whether the connection is incoming or outgoing.
     */
    public boolean isOutgoing() {
        return OUTGOING;
    }

    /** A tiny allocation optimization; see Message.read(InputStream,byte[]). */
    private byte[] HEADER_BUF=new byte[23];
    /**
     * Receives a message.  This method is NOT thread-safe.  Behavior is
     * undefined if two threads are in a receive call at the same time for a
     * given connection.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but blocks until a
     *  message is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    protected Message receive() throws IOException, BadPacketException {
        //On the Macintosh, sockets *appear* to return the same ping reply
        //repeatedly if the connection has been closed remotely.  This prevents
        //connections from dying.  The following works around the problem.  Note
        //that Message.read may still throw IOException below.
        if (_closed)
            throw new IOException("connection closed");

        Message m = null;
        while (m == null) {
            m = Message.read(_in, HEADER_BUF, _softMax);
        }
        return m;
    }

    /**
     * Receives a message with timeout.  This method is NOT thread-safe.
     * Behavior is undefined if two threads are in a receive call at the same
     * time for a given connection.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but throws InterruptedIOException
     *  if timeout!=0 and no message is read after "timeout" milliseconds.  In
     *  this case, you should terminate the connection, as half a message may
     *  have been read.
     */
    public Message receive(int timeout)
		throws IOException, BadPacketException, InterruptedIOException {
        //See note in receive().
        if (_closed)
            throw new IOException("connection closed");

        //temporarily change socket timeout.
        int oldTimeout=_socket.getSoTimeout();
        _socket.setSoTimeout(timeout);
        try {
            Message m = Message.read(_in, _softMax);
            if (m==null) {
                throw new InterruptedIOException("null message read");
            }
            return m;
        } finally {
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Sends a message.  The message may be buffered, so call flush() to
     * guarantee that the message is sent synchronously.  This method is NOT
     * thread-safe. Behavior is undefined if two threads are in a send call
     * at the same time for a given connection.
     *
     * @requires this is fully initialized
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.
     */
    public void send(Message m) throws IOException {
        m.write(_out);
    }

    /**
     * Flushes any buffered messages sent through the send method.
     */
    public void flush() throws IOException {
        _out.flush();
    }

    /** Returns the host set at construction */
    public String getOrigHost() {
        return _host;
    }

    /** Returns the port set at construction */
    public int getOrigPort() {
        return _port;
    }
    
    /** 
     * Sets the port where the conected node listens at, not the one
     * got from socket
     */
    void setOrigPort(int port){
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: "+port);
        this._port = port;
    }

    /**
     * Returns the port of the foreign host this is connected to.
     * @exception IllegalStateException this is not initialized
     */
    public int getPort() throws IllegalStateException {
		if(_socket == null) {
			throw new IllegalStateException("Not initialized");
		}
		return _socket.getPort();
    }

    /**
     * Returns the port this is connected to locally.
     * @exception IllegalStateException this is not initialized
     */
    public int getLocalPort() throws IllegalStateException {
		if(_socket == null) {
			throw new IllegalStateException("Not initialized");
		}
		return _socket.getLocalPort();
    }

    /**
     * Returns the address of the foreign host this is connected to.
     * @exception IllegalStateException this is not initialized
     */
    public InetAddress getInetAddress() throws IllegalStateException {
		if(_socket == null) {
			throw new IllegalStateException("Not initialized");
		}
		return _socket.getInetAddress();
    }

    /**
     * Returns the local address of this.
     * @exception IllegalStateException this is not initialized
     */
    public InetAddress getLocalAddress() throws IllegalStateException {
		if(_socket == null) {
			throw new IllegalStateException("Not initialized");
		}
		return _socket.getLocalAddress();
    }

    /**
     * Returns the time this connection was established, in milliseconds
     * since January 1, 1970.
     *
     * @return the time this connection was established
     */
    public long getConnectionTime() {
        return _connectionTime;
    }

    /**
     * Checks whether this connection is considered a stable connection,
     * meaning it has been up for enough time to be considered stable.
     *
     * @return <tt>true</tt> if the connection is considered stable,
     *  otherwise <tt>false</tt>
     */
    public boolean isStable() {
        return isStable(System.currentTimeMillis());
    }

    /**
     * Checks whether this connection is considered a stable connection,
     * by comparing the time it was established with the <tt>millis</tt>
     * argument.
     *
     * @return <tt>true</tt> if the connection is considered stable,
     *  otherwise <tt>false</tt>
     */
    public boolean isStable(long millis) {
        return (millis - getConnectionTime())/1000 > 5;
    }

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int supportsVendorMessage(byte[] vendorID, int selector) {
        if (_messagesSupported != null)
            return _messagesSupported.supportsMessage(vendorID, selector);
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsUDPConnectBack() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsUDPConnectBack();
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsTCPConnectBack() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsTCPConnectBack();
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsHopsFlow() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsHopsFlow();
        return -1;
    }

    /**
     * Returns whether or not this connection represents a local address.
     *
     * @return <tt>true</tt> if this connection is a local address,
     *  otherwise <tt>false</tt>
     */
    protected boolean isLocal() {
        return NetworkUtils.isLocalAddress(_socket.getInetAddress());
    }

    /**
     * Returns the value of the given outgoing (written) connection property, or
     * null if no such property.  For example, getProperty("X-Supernode") tells
     * whether I am a supernode or a leaf node.  If I wrote a property multiple
     * time during connection, returns the latest.
     */
    public String getPropertyWritten(String name) {
        return HEADERS_WRITTEN.getProperty(name);
    }

    /**
     * @return true until close() is called on this Connection
     */
    public boolean isOpen() {
        return !_closed;
    }

    /**
     *  Closes the Connection's socket and thus the connection itself.
     */
    public void close() {
        // Setting this flag insures that the socket is closed if this
        // method is called asynchronously before the socket is initialized.
        _closed = true;
        if(_socket != null) {
            try {				
                _socket.close();
            } catch(IOException e) {}
        }
        
       // closing _in (and possibly _out too) can cause NPE's
       // in Message.read (and possibly other places),
       // because BufferedInputStream can't handle
       // the case where one thread is reading from the stream and
       // another closes it.
       // See BugParade ID: 4505257
       
       // if (_in != null) {
       //     try {
       //         _in.close();
       //     } catch (IOException e) {}
       // }
       // if (_out != null) {
       //     try {
       //         _out.close();
       //     } catch (IOException e) {}
       // }
    }


    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
		return _headers.getUserAgent();
    }


    // inherit doc comment
    public boolean isGoodUltrapeer() {
        return _headers.isGoodUltrapeer();
    }

    // inherit doc comment
    public boolean isGoodLeaf() {
        return _headers.isGoodLeaf();
    }

	/**
	 * Returns the number of intra-Ultrapeer connections this node maintains.
	 * 
	 * @return the number of intra-Ultrapeer connections this node maintains
	 */
	public int getNumIntraUltrapeerConnections() {
		return _headers.getNumIntraUltrapeerConnections();
	}

	// implements ReplyHandler interface -- inherit doc comment
	public boolean isHighDegreeConnection() {
		return _headers.isHighDegreeConnection();
	}

	/**
	 * Returns whether or not this connection is to an Ultrapeer that 
	 * supports query routing between Ultrapeers at 1 hop.
	 *
	 * @return <tt>true</tt> if this is an Ultrapeer connection that
	 *  exchanges query routing tables with other Ultrapeers at 1 hop,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isUltrapeerQueryRoutingConnection() {
		return _headers.isUltrapeerQueryRoutingConnection();
    }

    /**
     * Returns whether or not this connections supports "probe" queries,
     * or queries sent at TTL=1 that should not block the send path
     * of subsequent, higher TTL queries.
     *
     * @return <tt>true</tt> if this connection supports probe queries,
     *  otherwise <tt>false</tt>
     */
    public boolean supportsProbeQueries() {
        return _headers.supportsProbeQueries();
    }

	/**
	 * Returns the authenticated domains listed in the connection headers
	 * for this connection.
	 *
	 * @return the string of authenticated domains for this connection
	 */
	public String getDomainsAuthenticated() {
		return _headers.getDomainsAuthenticated();
	}

    /**
     * Accessor for whether or not this connection has received any
     * headers.
     *
     * @return <tt>true</tt> if this connection has finished initializing
     *  and therefore has headers, otherwise <tt>false</tt>
     */
    public boolean receivedHeaders() {
        return _headers != null;
    }

	/**
	 * Accessor for the <tt>HandshakeResponse</tt> instance containing all
	 * of the Gnutella connection headers passed by this node.
	 *
	 * @return the <tt>HandshakeResponse</tt> instance containing all of
	 *  the Gnutella connection headers passed by this node
	 */
	public HandshakeResponse headers() {
		return _headers;
	}
	
	/**
	 * Accessor for the LimeWire version reported in the connection headers
	 * for this node.	 
	 */
	public String getVersion() {
		return _headers.getVersion();
	}

    /** Returns true iff this connection wrote "Ultrapeer: false".
     *  This does NOT necessarily mean the connection is shielded. */
    public boolean isLeafConnection() {
		return _headers.isLeaf();
    }

    /** Returns true iff this connection wrote "Supernode: true". */
    public boolean isSupernodeConnection() {
		return _headers.isUltrapeer();
    }

    /** 
	 * Returns true iff the connection is an Ultrapeer and I am a leaf, i.e., 
     * if I wrote "X-Ultrapeer: false", this connection wrote 
	 * "X-Ultrapeer: true" (not necessarily in that order).  <b>Does 
	 * NOT require that QRP is enabled</b> between the two; the Ultrapeer 
	 * could be using reflector indexing, for example. 
	 */
    public boolean isClientSupernodeConnection() {
        if(_isUltrapeer == null) {
            _isUltrapeer = 
                new Boolean(isClientSupernodeConnection2());
        }
        return _isUltrapeer.booleanValue();
    }

    private boolean isClientSupernodeConnection2() {
        //Is remote host a supernode...
        if (! isSupernodeConnection())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(
            HeaderNames.X_ULTRAPEER);
        if (value==null)
            return false;
        else 
            return !Boolean.valueOf(value).booleanValue();
			
    }


	/**
	 * Returns whether or not this connection is to a client supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolean isGUESSCapable() {
		return _headers.isGUESSCapable();
	}

	/**
	 * Returns whether or not this connection is to a ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrapeer connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolean isGUESSUltrapeer() {
		return _headers.isGUESSUltrapeer();
	}


	/**
	 * Returns the version of the GUESS search scheme supported by the node
	 * at the other end of the connection.  This returns the version in
	 * whole numbers.  So, if the supported GUESS version is 0.1, this 
	 * will return 1.  If the other client has not sent an X-Guess header
	 * this returns -1.
	 *
	 * @return the version of GUESS supported, reported as a whole number,
	 *  or -1 if GUESS is not supported
	 */
	public int getGUESSVersion() {
		return _headers.getGUESSVersion();
	}

    /** Returns true iff this connection is a temporary connection as per
     the headers. */
    public boolean isTempConnection() {
		return _headers.isTempConnection();
    }
    
    /** Returns true iff I am a supernode shielding the given connection, i.e.,
     *  if I wrote "X-Ultrapeer: true" and this connection wrote 
	 *  "X-Ultrapeer: false, and <b>both support query routing</b>. */
    public boolean isSupernodeClientConnection() {
        if(_isLeaf == null) {
            _isLeaf = 
                new Boolean(isSupernodeClientConnection2());
        }
        return _isLeaf.booleanValue();
    }
    
    /** Returns true iff I am a supernode shielding the given connection, i.e.,
     *  if I wrote "X-Ultrapeer: true" and this connection wrote 
	 *  "X-Ultrapeer: false, and <b>both support query routing</b>. */
    private boolean isSupernodeClientConnection2() {
        //Is remote host a supernode...
        if (! isLeafConnection())
            return false;

        //...and am I a supernode?
        String value=getPropertyWritten(
            HeaderNames.X_ULTRAPEER);
        if (value==null)
            return false;
        else if (!Boolean.valueOf(value).booleanValue())
            return false;

        //...and do both support QRP?
        return isQueryRoutingEnabled();
    }

    /** Returns true if this supports GGEP'ed messages.  GGEP'ed messages (e.g.,
     *  big pongs) should only be sent along connections for which
     *  supportsGGEP()==true. */
    public boolean supportsGGEP() {
		return _headers.supportsGGEP();
    }


    /** True if the remote host supports query routing (QRP).  This is only 
     *  meaningful in the context of leaf-ultrapeer relationships. */
    boolean isQueryRoutingEnabled() {
		return _headers.isQueryRoutingEnabled();
    }

    // overrides Object.toString
    public String toString() {
        return "CONNECTION: host=" + _host  + " port=" + _port; 
    }
    
    
    /////////////////////////// Unit Tests  ///////////////////////////////////
    
//      /** Unit test */
//      public static void main(String args[]) {
//          Assert.that(! notLessThan06("CONNECT"));
//          Assert.that(! notLessThan06("CONNECT/0.4"));
//          Assert.that(! notLessThan06("CONNECT/0.599"));
//          Assert.that(! notLessThan06("CONNECT/XP"));
//          Assert.that(notLessThan06("CONNECT/0.6"));
//          Assert.that(notLessThan06("CONNECT/0.7"));
//          Assert.that(notLessThan06("GNUTELLA CONNECT/1.0"));

//          final Properties props=new Properties();
//          props.setProperty("Query-Routing", "0.3");        
//          HandshakeResponder standardResponder=new HandshakeResponder() {
//              public HandshakeResponse respond(HandshakeResponse response,
//                                               boolean outgoing) {
//                  return new HandshakeResponse(props);
//              }
//          };        
//          HandshakeResponder secretResponder=new HandshakeResponder() {
//              public HandshakeResponse respond(HandshakeResponse response,
//                                               boolean outgoing) {
//                  Properties props2=new Properties();
//                  props2.setProperty("Secret", "abcdefg");
//                  return new HandshakeResponse(props2);
//              }
//          };
//          ConnectionPair p=null;

//          //1. 0.4 => 0.4
//          p=connect(null, null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //2. 0.6 => 0.6
//          p=connect(standardResponder, props, secretResponder);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Secret")==null);
//          Assert.that(p.in.getProperty("Secret").equals("abcdefg"));
//          disconnect(p);

//          //3. 0.4 => 0.6 (Incoming doesn't send properties)
//          p=connect(standardResponder, null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //4. 0.6 => 0.4 (If the receiving connection were Gnutella 0.4, this
//          //wouldn't work.  But the new guy will automatically upgrade to 0.6.)
//          p=connect(null, props, standardResponder);
//          Assert.that(p!=null);
//          //Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //5.
//          System.out.println("-Testing IOException reading from closed socket");
//          p=connect(null, null, null);
//          Assert.that(p!=null);
//          p.in.close();
//          try {
//              p.out.receive();
//              Assert.that(false);
//          } catch (BadPacketException failed) {
//              Assert.that(false);
//          } catch (IOException pass) {
//          }

//          //6.
//          System.out.println("-Testing IOException writing to closed socket");
//          p=connect(null, null, null);
//          Assert.that(p!=null);
//          p.in.close();
//          try { Thread.sleep(2000); } catch (InterruptedException e) { }
//          try {
//              //You'd think that only one write is needed to get IOException.
//              //That doesn't seem to be the case, and I'm not 100% sure why.  It
//              //has something to do with TCP half-close state.  Anyway, this
//              //slightly weaker test is good enough.
//              p.out.send(new QueryRequest((byte)3, 0, "las"));
//              p.out.flush();
//              p.out.send(new QueryRequest((byte)3, 0, "las"));
//              p.out.flush();
//              Assert.that(false);
//          } catch (IOException pass) {
//          }

//          //7.
//          System.out.println("-Testing connect with timeout");
//          Connection c=new Connection("this-host-does-not-exist.limewire.com", 6346);
//          int TIMEOUT=1000;
//          long start=System.currentTimeMillis();
//          try {
//              c.initialize(TIMEOUT);
//              Assert.that(false);
//          } catch (IOException e) {
//              //Check that exception happened quickly.  Note fudge factor below.
//              long elapsed=System.currentTimeMillis()-start;  
//              Assert.that(elapsed<(3*TIMEOUT)/2, "Took too long to connect: "+elapsed);
//          }
//      }   

//      private static class ConnectionPair {
//          Connection in;
//          Connection out;
//      }

//      private static ConnectionPair connect(HandshakeResponder inProperties,
//                                            Properties outRequestHeaders,
//                                            HandshakeResponder outProperties2) {
//          ConnectionPair ret=new ConnectionPair();
//          com.limegroup.gnutella.tests.MiniAcceptor acceptor=
//              new com.limegroup.gnutella.tests.MiniAcceptor(inProperties);
//          try {
//              ret.out=new Connection("localhost", 6346,
//                                     outRequestHeaders, outProperties2,
//                                     true);
//              ret.out.initialize();
//          } catch (IOException e) { }
//          ret.in=acceptor.accept();
//          if (ret.in==null || ret.out==null)
//              return null;
//          else
//              return ret;
//      }

//      private static void disconnect(ConnectionPair cp) {
//          if (cp.in!=null)
//              cp.in.close();
//          if (cp.out!=null)
//              cp.out.close();
//      }    
}
