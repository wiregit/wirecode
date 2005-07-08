package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.StringSetting;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.CompressionStat;
import com.limegroup.gnutella.statistics.ConnectionStat;
import com.limegroup.gnutella.statistics.HandshakingStat;
import com.limegroup.gnutella.util.CompressingOutputStream;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.UncompressingInputStream;

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
 * LimeWire".<p>
 *
 * This class augments the basic 0.6 handshaking mechanism to allow
 * authentication via "401" messages.  Authentication interactions can take
 * multiple rounds.<p>
 *
 * This class supports reading and writing streams using 'deflate' compression.
 * The HandshakeResponser is what actually determines whether or not
 * deflate will be used.  This class merely looks at what the responses are in
 * order to set up the appropriate streams.  Compression is implemented by
 * chaining the input and output streams, meaning that even if an extending
 * class implements getInputStream() and getOutputStream(), the actual input
 * and output stream used may not be an instance of the expected class.
 * However, the information is still chained through the appropriate stream.<p>
 *
 * The amount of bytes written and received are maintained by this class.  This
 * is necessary because of compression and decompression are considered
 * implementation details in this class.<p>
 * 
 * Finally, <tt>Connection</tt> also handles setting the SOFT_MAX_TTL on a
 * per-connection basis.  The SOFT_MAX TTL is the limit for hops+TTL on all
 * incoming traffic, with the exception of query hits.  If an incoming 
 * message has hops+TTL greater than SOFT_MAX, we set the TTL to 
 * SOFT_MAX-hops.  We do this on a per-connection basis because on newer
 * connections that understand X-Max-TTL, we can regulate the TTLs they 
 * send us.  This helps prevent malicious hosts from using headers like 
 * X-Max-TTL to simply get connections.  This way, they also have to abide
 * by the contract of the X-Max-TTL header, illustrated by sending lower
 * TTL traffic generally.
 */
public class Connection implements IpPort {
    
    private static final Log LOG = LogFactory.getLog(Connection.class);
	
	/**
	 * Lock for maintaining accurate data for when to allow ping forwarding.
     */
	private final Object PING_LOCK = new Object();

    /**
	 * Lock for maintaining accurate data for when to allow pong forwarding.
	 */
    private final Object PONG_LOCK = new Object();
    
    /** 
     * The underlying socket, its address, and input and output streams.  sock,
     * in, and out are null iff this is in the unconnected state.  For thread
     * synchronization reasons, it is important that this only be modified by
     * the send(m) and receive() methods.
     */
    private final String _host;
    private int _port;
    protected Socket _socket;
    private InputStream _in;
    private OutputStream _out;
    private final boolean OUTGOING;
    
    /**
     * The Inflater to use for inflating read streams, initialized
     * in initialize() if the connection told us it's sending with
     * a Content-Encoding of deflate.
     * Definitions:
     *   Inflater.getTotalOut -- The number of UNCOMPRESSED bytes
     *   Inflater.getTotalIn  -- The number of COMPRESSED bytes
     */
    protected Inflater _inflater;
    
    /**
     * The Deflater to use for deflating written streams, initialized
     * in initialize() if we told the connection we're sending with
     * a Content-Encoding of deflate.
     * Note that this is the same as '_out', but is assigned here
     * as the appropriate type so we don't have to cast when we
     * want to measure the compression savings.
     * Definitions:
     *   Deflater.getTotalOut -- The number of COMPRESSED bytes
     *   Deflater.getTotalIn  -- The number of UNCOMPRESSED bytes
     */
    protected Deflater _deflater;
    
    /**
     * The number of bytes sent to the output stream.
     */
    private volatile long _bytesSent;
    
    /**
     * The number of bytes recieved from the input stream.
     */
    private volatile long _bytesReceived;
    
    /**
     * The number of compressed bytes sent to the stream.
     * This is effectively the same as _deflater.getTotalOut(),
     * but must be cached because Deflater's behaviour is undefined
     * after end() has been called on it, which is done when this
     * connection is closed.
     */
    private volatile long _compressedBytesSent;
    
    /**
     * The number of compressed bytes read from the stream.
     * This is effectively the same as _inflater.getTotalIn(),
     * but must be cached because Inflater's behaviour is undefined
     * after end() has been called on it, which is done when this
     * connection is closed.
     */
    private volatile long _compressedBytesReceived;

    /** The possibly non-null VendorMessagePayload which describes what
     *  VendorMessages the guy on the other side of this connection supports.
     */
    protected MessagesSupportedVendorMessage _messagesSupported = null;
    
    /** The possibly non-null VendorMessagePayload which describes what
     *  Capabilities the guy on the other side of this connection supports.
     */
    protected CapabilitiesVM _capabilities = null;
    
    /**
     * Trigger an opening connection to close after it opens.  This
     * flag is set in shutdown() and then checked in initialize()
     * to insure the _socket.close() happens if shutdown is called
     * asynchronously before initialize() completes.  Note that the 
     * connection may have been remotely closed even if _closed==true.  
     * Protected (instead of private) for testing purposes only.
     * This also protects us from calling methods on the Inflater/Deflater
     * objects after end() has been called on them.
     */
    protected volatile boolean _closed=false;

    /** 
	 * The headers read from the connection.
	 */
    private final Properties HEADERS_READ = new Properties();

    /**
     * The <tt>HandshakeResponse</tt> wrapper for the connection headers.
     */
	private volatile HandshakeResponse _headers = 
        HandshakeResponse.createEmptyResponse();
        
    /**
     * The <tt>HandshakeResponse</tt> wrapper for written connection headers.
     */
	private HandshakeResponse _headersWritten = 
        HandshakeResponse.createEmptyResponse();        

    /** For outgoing Gnutella 0.6 connections, the properties written
     *  after "GNUTELLA CONNECT".  Null otherwise. */
    private final Properties REQUEST_HEADERS;

    /** 
     * For outgoing Gnutella 0.6 connections, a function calculating the
     *  properties written after the server's "GNUTELLA OK".  For incoming
     *  Gnutella 0.6 connections, the properties written after the client's
     *  "GNUTELLA CONNECT".
     * Non-final so that the responder can be garbage collected after we've
     * concluded the responding (by setting to null).
     */
    protected HandshakeResponder RESPONSE_HEADERS;

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

    /**
     * The "soft max" ttl to use for this connection.
     */
    private byte _softMax;

    /**
     * Variable for the next time to allow a ping.  Volatile to avoid
     * multiple threads caching old data for the ping time.
     */
    private volatile long _nextPingTime = Long.MIN_VALUE;

    /**
     * Variable for the next time to allow a pong.  Volatile to avoid
     * multiple threads caching old data for the pong time.
     */
    private volatile long _nextPongTime = Long.MIN_VALUE;
    
    /**
     * Cache the 'connection closed' exception, so we have to allocate
     * one for every closed connection.
     */
    protected static final IOException CONNECTION_CLOSED =
        new IOException("connection closed");

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
		ConnectionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
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
		ConnectionStat.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
    }


    /** Call this method when the Connection has been initialized and accepted
     *  as 'long-lived'.
     */
    protected void postInit() {
        try { // TASK 1 - Send a MessagesSupportedVendorMessage if necessary....
			if(_headers.supportsVendorMessages() > 0) {
                send(MessagesSupportedVendorMessage.instance());
                send(CapabilitiesVM.instance());
			}
        } catch (IOException ioe) {
        }
    }

    /**
     * Call this method if you want to send your neighbours a message with your
     * updated capabilities.
     */
    protected void sendUpdatedCapabilities() {
        try {
            if(_headers.supportsVendorMessages() > 0)
                send(CapabilitiesVM.instance());
        } catch (IOException iox) { }
    }

    /**
     * Call this method when you want to handle us to handle a VM.  We may....
     */
    protected void handleVendorMessage(VendorMessage vm) {
        if (vm instanceof MessagesSupportedVendorMessage)
            _messagesSupported = (MessagesSupportedVendorMessage) vm;
        if (vm instanceof CapabilitiesVM)
            _capabilities = (CapabilitiesVM) vm;
        if (vm instanceof HeaderUpdateVendorMessage) {
            HeaderUpdateVendorMessage huvm = (HeaderUpdateVendorMessage)vm;
            HEADERS_READ.putAll(huvm.getProperties());
            try {
                _headers = HandshakeResponse.createResponse(HEADERS_READ);
            }catch(IOException ignored){}
        }
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

        if(isOutgoing())
            _socket=Sockets.connect(_host, _port, timeout);

        // Check to see if close() was called while the socket was initializing
        if (_closed) {
            _socket.close();
            throw CONNECTION_CLOSED;
        } 
        
        // Check to see if this is an attempt to connect to ourselves
		InetAddress localAddress = _socket.getLocalAddress();
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() &&
            _socket.getInetAddress().equals(localAddress) &&
            _port == ConnectionSettings.PORT.getValue()) {
            throw new IOException("Connection to self");
        }
        
        // Notify the acceptor of our address.
        RouterService.getAcceptor().setAddress(localAddress);

        try {
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

            _headersWritten = HandshakeResponse.createResponse(HEADERS_WRITTEN);

            _connectionTime = System.currentTimeMillis();

            // Now set the soft max TTL that should be used on this connection.
            // The +1 on the soft max for "good" connections is because the message
            // may come from a leaf, and therefore can have an extra hop.
            // "Good" connections are connections with features such as 
            // intra-Ultrapeer QRP passing.
            _softMax = ConnectionSettings.SOFT_MAX.getValue();
            if(isGoodUltrapeer() || isGoodLeaf()) {
                // we give these an extra hop because they might be sending
                // us traffic from their leaves
                _softMax++;
            } 
            
            //wrap the streams with inflater/deflater
            // These calls must be delayed until absolutely necessary (here)
            // because the native construction for Deflater & Inflater 
            // allocate buffers outside of Java's memory heap, preventing 
            // Java from fully knowing when/how to GC.  The call to end()
            // (done explicitly in the close() method of this class, and
            //  implicitly in the finalization of the Deflater & Inflater)
            // releases these buffers.
            if(isWriteDeflated()) {
                _deflater = new Deflater();
                _out = createDeflatedOutputStream(_out);
            }            
            if(isReadDeflated()) {
                _inflater = new Inflater();
                _in = createInflatedInputStream(_in);
            }
            
            // remove the reference to the RESPONSE_HEADERS, since we'll no
            // longer be responding.
            // This does not need to be in a finally clause, because if an
            // exception was thrown, the connection will be removed anyway.
            RESPONSE_HEADERS = null;
						
        } catch (NoGnutellaOkException e) {
            close();
            throw e;
        } catch (IOException e) {
            close();
            throw new BadHandshakeException(e);
        }
    }
    
    /** Creates the output stream for deflating */
    protected OutputStream createDeflatedOutputStream(OutputStream out) {
        return new CompressingOutputStream(out, _deflater);
    }
    
    /** Creates the input stream for inflating */
    protected InputStream createInflatedInputStream(InputStream in) {
        return new UncompressingInputStream(in, _inflater);
    }
    
    /**
     * Determines whether this connection is capable of asynchronous messaging.
     */
    public boolean isAsynchronous() {
        return _socket.getChannel() != null;
    }

    /**
     * Accessor for whether or not this connection has been initialized.
     * Several methods of this class require that the connection is 
     * initialized, particularly that the socket is established.  These
     * methods should verify that the connection is initialized before
     * being called.
     *
     * @return <tt>true</tt> if the connection has been initialized and
     *  the socket established, otherwise <tt>false</tt>
     */
    public boolean isInitialized() {
        return _socket != null;
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
        writeLine(GNUTELLA_CONNECT_06+CRLF);
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
			Assert.that(connectLine != null, "null connectLine");
			if (! connectLine.startsWith(GNUTELLA_06)) {
                HandshakingStat.OUTGOING_BAD_CONNECT.incrementStat();
                throw new IOException("Bad connect string");
            }
				

			//3. Read the Gnutella headers. 
			readHeaders(Constants.TIMEOUT);

            //Terminate abnormally if we read something other than 200 or 401.
            HandshakeResponse theirResponse = 
                HandshakeResponse.createRemoteResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(), 
                    HEADERS_READ);
			_headers = theirResponse;
            Assert.that(theirResponse != null, "null theirResponse");

            int code = theirResponse.getStatusCode();
            if (code != HandshakeResponse.OK) {
                if(code == HandshakeResponse.SLOTS_FULL) {
                    if(theirResponse.isLimeWire()) {
                        if(theirResponse.isUltrapeer()) {
                            HandshakingStat.
                                OUTGOING_LIMEWIRE_ULTRAPEER_REJECT.
                                    incrementStat();
                        } else {
                            HandshakingStat.
                                OUTGOING_LIMEWIRE_LEAF_REJECT.
                                    incrementStat();
                        }
                    } else {
                        if(theirResponse.isUltrapeer()) {
                            HandshakingStat.
                                OUTGOING_OTHER_ULTRAPEER_REJECT.
                                    incrementStat();
                        } else {
                            HandshakingStat.
                                OUTGOING_OTHER_LEAF_REJECT.
                                    incrementStat();
                        }                            
                    } 
                    throw NoGnutellaOkException.SERVER_REJECT;
                } else {
                    HandshakingStat.OUTGOING_SERVER_UNKNOWN.incrementStat();
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
                    HandshakingStat.SUCCESSFUL_OUTGOING.incrementStat();
                    //a) Terminate normally if we wrote "200 OK".
                    return;
                } else {
                    //b) Continue loop if we wrote "200 AUTHENTICATING".
                    continue;
                }
            } else {                
                //c) Terminate abnormally if we wrote anything else.
                if(code == HandshakeResponse.SLOTS_FULL) {
                    HandshakingStat.OUTGOING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellaOkException.CLIENT_REJECT;
                } 
                else if(code == HandshakeResponse.LOCALE_NO_MATCH) {
                    //if responder's locale preferencing was set 
                    //and didn't match the locale this code is used.
                    //(currently in use by the dedicated connectionfetcher)
                    throw NoGnutellaOkException.CLIENT_REJECT_LOCALE;
                }
                else {
                    HandshakingStat.OUTGOING_CLIENT_UNKNOWN.incrementStat();
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

			// is this an incoming connection from the crawler??
			boolean isCrawler = _headers.isCrawler();
			
			//Note: in the following code, it appears that we're ignoring
			//the response code written by the initiator of the connection.
			//However, you can prove that the last code was always 200 OK.
			//See initializeIncoming and the code at the bottom of this
			//loop.
			HandshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(_headers, false);

            writeLine(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.props());
            
            // if it was the crawler, leave early.
            if(isCrawler) {
                // read one response, just to make sure they got ours.
                readLine();
                throw new IOException("crawler");
            }
                
            
            //Our response should be either OK or UNAUTHORIZED for the handshake
            //to proceed.
            int code = ourResponse.getStatusCode();
            if(code != HandshakeResponse.OK) {
                if(code == HandshakeResponse.SLOTS_FULL) {
                    HandshakingStat.INCOMING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellaOkException.CLIENT_REJECT;
                } else {
                    HandshakingStat.INCOMING_CLIENT_UNKNOWN.incrementStat();
                    throw NoGnutellaOkException.createClientUnknown(code);
                }
            }
                    
            //3. read the response from the other side.  If we asked the other
            //side to authenticate, give more time so as to receive user input
            String connectLine = readLine();  
            readHeaders();
			
            if (! connectLine.startsWith(GNUTELLA_06)) {
                HandshakingStat.INCOMING_BAD_CONNECT.incrementStat();
                throw new IOException("Bad connect string");
            }
                

            HandshakeResponse theirResponse = 
                HandshakeResponse.createRemoteResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(),
                    HEADERS_READ);           


            //Decide whether to proceed.
            code = ourResponse.getStatusCode();
            if(code == HandshakeResponse.OK) {
                if(theirResponse.getStatusCode() == HandshakeResponse.OK) {
                    HandshakingStat.SUCCESSFUL_INCOMING.incrementStat();
                    //a) If we wrote 200 and they wrote 200 OK, stop normally.
                    return;
                }
            }

			HandshakingStat.INCOMING_SERVER_UNKNOWN.incrementStat();
            //c) Terminate abnormally
            throw NoGnutellaOkException.
                createServerUnknown(theirResponse.getStatusCode());
        }        
        
        HandshakingStat.INCOMING_NO_CONCLUSION.incrementStat();
        //If we didn't successfully return out of the method, throw an exception
        //to indicate that handshaking didn't reach any conclusion.
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
            Enumeration names=props.propertyNames();
            while (names.hasMoreElements()) {
                String key=(String)names.nextElement();
                String value=props.getProperty(key);
                // Overwrite any domainname with true IP address
                if ( HeaderNames.REMOTE_IP.equals(key) )
                    value=getInetAddress().getHostAddress();
                if (value==null)
                    value="";
                writeLine(key+": "+value+CRLF);   
                HEADERS_WRITTEN.put(key, value);
            }
        }
        //send the trailer
        writeLine(CRLF);
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
            if (HeaderNames.REMOTE_IP.equals(key))
                changeAddress(value);
            HEADERS_READ.put(key, value);
        }
    }
    
    /**
     * Determines if the address should be changed and changes it if
     * necessary.
     */
    private void changeAddress(final String v) {
        InetAddress ia = null;
        try {
            ia = InetAddress.getByName(v);
        } catch(UnknownHostException uhe) {
            return; // invalid.
        }
        
        // invalid or private, exit
        if(!NetworkUtils.isValidAddress(ia) ||
            NetworkUtils.isPrivateAddress(ia))
            return;
            
        // If we're forcing, change that if necessary.
        if( ConnectionSettings.FORCE_IP_ADDRESS.getValue() ) {
            StringSetting addr = ConnectionSettings.FORCED_IP_ADDRESS_STRING;
            if(!v.equals(addr.getValue())) {
                addr.setValue(v);
                RouterService.addressChanged();
            }
        }
        // Otherwise, if our current address is invalid, change.
        else if(!NetworkUtils.isValidAddress(RouterService.getAddress())) {
            // will auto-call addressChanged.
            RouterService.getAcceptor().setAddress(ia);
        }
        
        RouterService.getAcceptor().setExternalAddress(ia);
    }
            

    /**
     * Writes s to out, with no trailing linefeeds.  Called only from
     * initialize().  
     *    @requires _socket, _out are properly set up */
    private void writeLine(String s) throws IOException {
        if(s == null || s.equals("")) {
            throw new NullPointerException("null or empty string: "+s);
        }

        //TODO: character encodings?
        byte[] bytes=s.getBytes();
		BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(bytes.length);
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
        // _in.read can throw an NPE if we closed the connection,
        // so we must catch NPE and throw the CONNECTION_CLOSED.
        try {
            _socket.setSoTimeout(timeout);
            String line=(new ByteReader(_in)).readLine();
            if (line==null)
                throw new IOException("read null line");
            BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(line.length());
            return line;
        } catch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        } finally {
            //Restore socket timeout.
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Returns the stream to use for writing to s.
     * If the message supports asynchronous messaging, we don't need
     * to buffer it, because it's already buffered internally.  Note, however,
     * that buffering it would not be wrong, because we can always flush
     * the buffered data.
     */
    protected OutputStream getOutputStream() throws IOException {
        if(isAsynchronous())
            return _socket.getOutputStream();
        else
            return new BufferedOutputStream(_socket.getOutputStream());
    }

    /**
     * Returns the stream to use for reading from s.
     * If this supports asynchronous messaging, the stream itself is returned,
     * because the underlying stream is already buffered.  This is also done
     * to ensure that when we switch to using asynch message processing, no
     * bytes are left within the BufferedInputStream's buffer.
     *
     * Otherwise (it isn't asynchronous-capable), we enforce a buffer around the stream.
     *
     * Subclasses may override to decorate the stream.
     */
    protected InputStream getInputStream() throws IOException {
        if(isAsynchronous())
            return _socket.getInputStream();
        else
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
    private final byte[] HEADER_BUF=new byte[23];

    /**
     * Receives a message.  This method is NOT thread-safe.  Behavior is
     * undefined if two threads are in a receive call at the same time for a
     * given connection.
     *
     * If this is an asynchronous read-deflated connection, this will set up
     * the UncompressingInputStream the first time this is called.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but blocks until a
     *  message is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    protected Message receive() throws IOException, BadPacketException {
        if(isAsynchronous() && isReadDeflated() && !(_in instanceof UncompressingInputStream))
            _in = new UncompressingInputStream(_in, _inflater);
        
        //On the Macintosh, sockets *appear* to return the same ping reply
        //repeatedly if the connection has been closed remotely.  This prevents
        //connections from dying.  The following works around the problem.  Note
        //that Message.read may still throw IOException below.
        //See note on _closed for more information.
        if (_closed)
            throw CONNECTION_CLOSED;

        Message m = null;
        while (m == null) {
            m = readAndUpdateStatistics();
        }
        return m;
    }

    /**
     * Receives a message with timeout.  This method is NOT thread-safe.
     * Behavior is undefined if two threads are in a receive call at the same
     * time for a given connection.
     *
     * If this is an asynchronous read-deflated connection, this will set up
     * the UncompressingInputStream the first time this is called.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but throws InterruptedIOException
     *  if timeout!=0 and no message is read after "timeout" milliseconds.  In
     *  this case, you should terminate the connection, as half a message may
     *  have been read.
     */
    public Message receive(int timeout)
		throws IOException, BadPacketException, InterruptedIOException {
        if(isAsynchronous() && isReadDeflated() && !(_in instanceof UncompressingInputStream))
            _in = new UncompressingInputStream(_in, _inflater);
		    
        //See note in receive().
        if (_closed)
            throw CONNECTION_CLOSED;

        //temporarily change socket timeout.
        int oldTimeout=_socket.getSoTimeout();
        _socket.setSoTimeout(timeout);
        try {
            Message m = readAndUpdateStatistics();
            if (m==null) {
                throw new InterruptedIOException("null message read");
            }
            return m;
        } finally {
            _socket.setSoTimeout(oldTimeout);
        }
    }
    
    /**
     * Reads a message from the network and updates the appropriate statistics.
     */
    private Message readAndUpdateStatistics()
      throws IOException, BadPacketException {
        
        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.
        Message msg = Message.read(_in, HEADER_BUF, Message.N_TCP, _softMax);
        updateReadStatistics(msg);
        return msg;
    }
    
    /**
     * Updates the read statistics.
     */
    protected void updateReadStatistics(Message msg) throws IOException {
            // _bytesReceived must be set differently
            // when compressed because the inflater will
            // read more input than a single message,
            // making it appear as if the deflated input
            // was actually larger.
        if( isReadDeflated() ) {
            try {
                long newIn  = _inflater.getTotalIn();
                long newOut = _inflater.getTotalOut();
                CompressionStat.GNUTELLA_UNCOMPRESSED_DOWNSTREAM.addData((int)(newOut - _bytesReceived));
                CompressionStat.GNUTELLA_COMPRESSED_DOWNSTREAM.addData((int)(newIn - _compressedBytesReceived));
                _compressedBytesReceived = newIn;
                _bytesReceived = newOut;
            } catch(NullPointerException npe) {
                // Inflater is broken and can throw an NPE if it was ended
                // at an odd time.
                throw CONNECTION_CLOSED;
            }
        } else if(msg != null) {
            _bytesReceived += msg.getTotalLength();
        }
    }
    
    /**
     * Optimization -- reuse the header buffer since sending will only be
     * done on one thread.
     */
    private final byte[] OUT_HEADER_BUF = new byte[23];

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
        if(LOG.isTraceEnabled())
            LOG.trace("Connection (" + toString() + 
                      ") is sending message: " + m);
        
        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.        
        try {
            m.write(_out, OUT_HEADER_BUF);
            updateWriteStatistics(m);
        } catch(NullPointerException e) {
            throw CONNECTION_CLOSED;
        }
    }

    /**
     * Flushes any buffered messages sent through the send method.
     */
    public void flush() throws IOException {
        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.
        try { 
            _out.flush();
            // we must update the write statistics again,
            // because flushing forces the deflater to deflate.
            updateWriteStatistics(null);
        } catch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        }
    }
    
    /**
     * Updates the write statistics.
     * @param m the possibly null message to add to the bytes sent
     */
    protected void updateWriteStatistics(Message m) {
        if( isWriteDeflated() ) {
            long newIn  = _deflater.getTotalIn();
            long newOut = _deflater.getTotalOut();
            CompressionStat.GNUTELLA_UNCOMPRESSED_UPSTREAM.addData((int)(newIn - _bytesSent));
            CompressionStat.GNUTELLA_COMPRESSED_UPSTREAM.addData((int)(newOut - _compressedBytesSent));
            _bytesSent = newIn;
            _compressedBytesSent = newOut;
        } else if( m != null) {
            _bytesSent += m.getTotalLength();
        }
    }    
    
    /**
     * Returns the number of bytes sent on this connection.
     * If the outgoing stream is compressed, the return value indicates
     * the compressed number of bytes sent.
     */
    public long getBytesSent() {
        if(isWriteDeflated())
            return _compressedBytesSent;
        else            
            return _bytesSent;
    }
    
    /**
     * Returns the number of uncompressed bytes sent on this connection.
     * If the outgoing stream is not compressed, this is effectively the same
     * as calling getBytesSent()
     */
    public long getUncompressedBytesSent() {
        return _bytesSent;
    }
    
    /** 
     * Returns the number of bytes received on this connection.
     * If the incoming stream is compressed, the return value indicates
     * the number of compressed bytes received.
     */
    public long getBytesReceived() {
        if(isReadDeflated())
            return _compressedBytesReceived;
        else
            return _bytesReceived;
    }
    
    /**
     * Returns the number of uncompressed bytes read on this connection.
     * If the incoming stream is not compressed, this is effectively the same
     * as calling getBytesReceived()
     */
    public long getUncompressedBytesReceived() {
        return _bytesReceived;
    }
    
    /**
     * Returns the percentage saved through compressing the outgoing data.
     * The value may be slightly off until the output stream is flushed,
     * because the value of the compressed bytes is not calculated until
     * then.
     */
    public float getSentSavedFromCompression() {
        if( !isWriteDeflated() || _bytesSent == 0 ) return 0;
        return 1-((float)_compressedBytesSent/(float)_bytesSent);
    }
    
    /**
     * Returns the percentage saved from having the incoming data compressed.
     */
    public float getReadSavedFromCompression() {
        if( !isReadDeflated() || _bytesReceived == 0 ) return 0;
        return 1-((float)_compressedBytesReceived/(float)_bytesReceived);
    }

   /**
    * Returns the IP address of the remote host as a string.
    * 
    * @return the IP address of the remote host as a string
    */
    public String getAddress() {
        return _host;
    }

    /**
     * Accessor for the port number this connection is listening on.  Note that 
     * this is NOT the port of the socket itself.  For incoming connections,  
     * the getPort method of the java.net.Socket class returns the ephemeral 
     * port that the host connected with.  This port, however, is the port the 
     * remote host is listening on for new connections, which we set using 
     * Gnutella connection headers in the case of incoming connections.  For 
     * outgoing connections, this is the port we used to connect to them -- 
     * their listening port.
     * 
     * @return the listening port for the remote host
     */
    public int getPort() {
        return _port;
    }
    
    /** 
     * Sets the port where the conected node listens at, not the one
     * got from socket
     */
    void setListeningPort(int port){
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: "+port);
        this._port = port;
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
     * Accessor for the <tt>Socket</tt> for this connection.
     * 
     * @return the <tt>Socket</tt> for this connection
     * @throws IllegalStateException if this connection is not yet
     *  initialized
     */
    public Socket getSocket() throws IllegalStateException {
        if(_socket == null) {
            throw new IllegalStateException("Not initialized");
        }
        return _socket;        
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
     * Accessor for the soft max TTL to use for this connection.
     * 
     * @return the soft max TTL for this connection
     */
    public byte getSoftMax() {
        return _softMax;
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
    
    /**
     * @return whether this connection supports routing of vendor messages
     * (i.e. will not drop a VM that has ttl <> 1 and hops > 0)
     */
    public boolean supportsVMRouting() {
        if (_headers != null)
            return _headers.supportsVendorMessages() >= 0.2;
        return false;
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
    public int remoteHostSupportsUDPRedirect() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsUDPConnectBackRedirect();
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsTCPRedirect() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsTCPConnectBackRedirect();
        return -1;
    }
    
    /** @return -1 if UDP crawling is supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsUDPCrawling() {
    	if (_messagesSupported != null)
    		return _messagesSupported.supportsUDPCrawling();
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

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsPushProxy() {
        if ((_messagesSupported != null) && isClientSupernodeConnection())
            return _messagesSupported.supportsPushProxy();
        return -1;
    }

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsLeafGuidance() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsLeafGuidance();
        return -1;
    }
    
    public int remoteHostSupportsHeaderUpdate() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsHeaderUpdate();
        return -1;
    }
    
    /**
     * Return whether or not the remote host supports feature queries.
     */
    public boolean getRemoteHostSupportsFeatureQueries() {
        if(_capabilities != null)
            return _capabilities.supportsFeatureQueries() > 0;
        return false;
    }

    /** @return the maximum selector of capability supported, else -1 if no
     *  support.
     */
    public int getRemoteHostFeatureQuerySelector() {
        if (_capabilities != null)
            return _capabilities.supportsFeatureQueries();
        return -1;
    }

    /** @return true if the capability is supported.
     */
    public boolean remoteHostSupportsWhatIsNew() {
        if (_capabilities != null)
            return _capabilities.supportsWhatIsNew();
        return false;
    }
    
    /**
     * Gets the remote host's 'update' version.
     */
    public int getRemoteHostUpdateVersion() {
        if(_capabilities != null)
            return _capabilities.supportsUpdate();
        else
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
        if(_closed)
            return;
        
        // Setting this flag insures that the socket is closed if this
        // method is called asynchronously before the socket is initialized.
        _closed = true;
        if(_socket != null) {
            try {				
                _socket.close();
            } catch(IOException e) {}
        }
        
        // tell the inflater & deflater that we're done with them.
        // These calls are dangerous, because we don't know that the
        // stream isn't currently deflating or inflating, and the access
        // to the deflater/inflater is not synchronized (it shouldn't be).
        // This can lead to NPE's popping up in unexpected places.
        // Fortunately, the calls aren't explicitly necessary because
        // when the deflater/inflaters are garbage-collected they will call
        // end for us.
        if( _deflater != null )
            _deflater.end();
        if( _inflater != null )
            _inflater.end();
        
       // closing _in (and possibly _out too) can cause NPE's
       // in Message.read (and possibly other places),
       // because BufferedInputStream can't handle
       // the case where one thread is reading from the stream and
       // another closes it.
       // See BugParade ID: 4505257
       
       if (_in != null) {
           try {
               _in.close();
           } catch (IOException e) {}
       }
       if (_out != null) {
           try {
               _out.close();
           } catch (IOException e) {}
       }
    }

    
    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
		return _headers.getUserAgent();
    }
    
    /**
     * Returns whether or not the remote host is a LimeWire (or derivative)
     */
    public boolean isLimeWire() {
        return _headers.isLimeWire();
    }
    
    public boolean isOldLimeWire() {
        return _headers.isOldLimeWire();
    }

    /**
     * Returns true if the outgoing stream is deflated.
     *
     * @return true if the outgoing stream is deflated.
     */
    public boolean isWriteDeflated() {
        return _headersWritten.isDeflateEnabled();
    }
    
    /**
     * Returns true if the incoming stream is deflated.
     *
     * @return true if the incoming stream is deflated.
     */
    public boolean isReadDeflated() {
        return _headers.isDeflateEnabled();
    }

    // inherit doc comment
    public boolean isGoodUltrapeer() {
        return _headers.isGoodUltrapeer();
    }

    // inherit doc comment
    public boolean isGoodLeaf() {
        return _headers.isGoodLeaf();
    }

    // inherit doc comment
    public boolean supportsPongCaching() {
        return _headers.supportsPongCaching();
    }

    /**
     * Returns whether or not we should allow new pings on this connection.  If
     * we have recently received a ping, we will likely not allow the second
     * ping to go through to avoid flooding the network with ping traffic.
     *
     * @return <tt>true</tt> if new pings are allowed along this connection,
     *  otherwise <tt>false</tt>
     */
    public boolean allowNewPings() {
    	synchronized(PING_LOCK) {
	        long curTime = System.currentTimeMillis();
			
			// don't allow new pings if the connection could drop any second
			if(!isStable(curTime)) return false;
	        if(curTime < _nextPingTime) {
	            return false;
	        } 
			_nextPingTime = System.currentTimeMillis() + 2500;
	        return true;
    	}
    }


    /**
     * Returns whether or not we should allow new pongs on this connection.  If
     * we have recently received a pong, we will likely not allow the second
     * pong to go through to avoid flooding the network with pong traffic.
     * In practice, this is only used to limit pongs sent to leaves.
     *
     * @return <tt>true</tt> if new pongs are allowed along this connection,
     *  otherwise <tt>false</tt>
     */
    public boolean allowNewPongs() {
    	synchronized(PONG_LOCK) {
		    long curTime = System.currentTimeMillis();

			// don't allow new pongs if the connection could drop any second
			if(!isStable(curTime)) return false;
		    if(curTime < _nextPongTime) {
		        return false;
		    } 
		    
			int interval;
		
			// if the connection is young, give it a lot of pongs, otherwise
			// be more conservative
			if(curTime - getConnectionTime() < 10000) {
				interval = 300;
			} else {
				interval = 12000;
			}
			_nextPongTime = curTime + interval;
					
		    return true;
    	}
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
        //Is remote host a supernode...
        if (! isSupernodeConnection())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(HeaderNames.X_ULTRAPEER);
        if (value==null)
            return false;
        else 
            return !Boolean.valueOf(value).booleanValue();
			
    }
    
    /** 
	 * Returns true iff the connection is an Ultrapeer and I am a Ultrapeer,
     * ie: if I wrote "X-Ultrapeer: true", this connection wrote 
	 * "X-Ultrapeer: true" (not necessarily in that order).  <b>Does 
	 * NOT require that QRP is enabled</b> between the two; the Ultrapeer 
	 * could be using reflector indexing, for example. 
	 */
    public boolean isSupernodeSupernodeConnection() {
        //Is remote host a supernode...
        if (! isSupernodeConnection())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(HeaderNames.X_ULTRAPEER);
        if (value==null)
            return false;
        else 
            return Boolean.valueOf(value).booleanValue();
			
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


    /** Returns true iff this connection is a temporary connection as per
     the headers. */
    public boolean isTempConnection() {
		return _headers.isTempConnection();
    }
    
    /** Returns true iff I am a supernode shielding the given connection, i.e.,
     *  if I wrote "X-Ultrapeer: true" and this connection wrote 
	 *  "X-Ultrapeer: false, and <b>both support query routing</b>. */
    public boolean isSupernodeClientConnection() {
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

    /**
     * Sends the StatisticVendorMessage down the connection
     */
    public void handleStatisticVM(StatisticVendorMessage svm) 
                                                            throws IOException {
        send(svm);
    }

    /**
     * Sends the SimppVM down the connection
     */
    public void handleSimppVM(SimppVM simppVM) throws IOException {
        send(simppVM);
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
    
    /**
     * access the locale pref. of the connected servent
     */
    public String getLocalePref() {
        return _headers.getLocalePref();
    }

    // Technically, a Connection object can be equal in various ways...
    // Connections can be said to be equal if the pipe the information is
    // travelling through is the same.
    // Or they can be equal if the remote host is the same, even if the
    // two connections are on different channels.
    // Ideally, our equals method would use the second option, however
    // this has problems with tests because of the setup of various
    // tests, connecting multiple connection objects to a central
    // testing Ultrapeer, uncorrectly labelling each connection
    // as equal.
    // Using pipe equality (by socket) also fails because
    // the socket doesn't exist for outgoing connections until
    // the connection is established, but the equals method is used
    // before then.
    // Until necessary, the equals & hashCode methods are therefore
    // commented out and sameness equality is being used.
    
//	public boolean equals(Object o) {
//      return super.equals(o);
//	}
//	

//	public int hashCode() {
//      return super.hashCode();
//	}
    
    
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
