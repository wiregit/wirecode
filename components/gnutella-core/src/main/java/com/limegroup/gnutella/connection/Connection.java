package com.limegroup.gnutella.connection;

import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.Properties;
import java.util.Enumeration;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushProxyInterface;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.updates.UpdateManager;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.*;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Set;

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
public class Connection implements ReplyHandler, PushProxyInterface {
    
    /**
     * Lock for maintaining accurate data for when to allow ping forwarding.
     */
    private final Object PING_LOCK = new Object();

    /**
     * Lock for maintaining accurate data for when to allow pong forwarding.
     */
    private final Object PONG_LOCK = new Object();
    
    /** 
     * The maximum number of times ManagedConnection instances should send UDP
     * ConnectBack requests.
     */
    private static final int MAX_UDP_CONNECT_BACK_ATTEMPTS = 15;

    /** 
     * The maximum number of times ManagedConnection instances should send TCP
     * ConnectBack requests.
     */
    private static final int MAX_TCP_CONNECT_BACK_ATTEMPTS = 10;
    
    /** 
     * The underlying socket, its address, and input and output streams.  sock,
     * in, and out are null iff this is in the unconnected state.  For thread
     * synchronization reasons, it is important that this only be modified by
     * the send(m) and receive() methods.
     */
    private final String _host;
    private int _port;
    private Socket _socket;
    private final boolean OUTGOING;
    
    private InputStream _in;
    private OutputStream _out;
    
    /**
     * The Inflater to use for inflating read streams, initialized
     * in initialize() if the connection told us it's sending with
     * a Content-Encoding of deflate.
     * Definitions:
     *   Inflater.getTotalOut -- The number of UNCOMPRESSED bytes
     *   Inflater.getTotalIn  -- The number of COMPRESSED bytes
     */
    private Inflater _inflater;
    
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
    private Deflater _deflater;

    /** 
     * The possibly non-null VendorMessagePayload which describes what
     * VendorMessages the guy on the other side of this connection supports.
     */
    private MessagesSupportedVendorMessage _messagesSupported;
    
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
    private volatile boolean _closed = false;

    /** 
     * The headers read from the connection.
     */
    private final Properties HEADERS_READ = new Properties();

    /**
     * The <tt>HandshakeResponse</tt> wrapper for the connection headers.
     */
    private HandshakeResponse _headers = 
        HandshakeResponse.createEmptyResponse();
        
    /**
     * The <tt>HandshakeResponse</tt> wrapper for written connection headers.
     */
    private HandshakeResponse _headersWritten = 
        HandshakeResponse.createEmptyResponse();        

    /** 
     * For outgoing Gnutella 0.6 connections, the properties written
     * after "GNUTELLA CONNECT".  Null otherwise. 
     */
    private final Properties REQUEST_HEADERS;

    /** 
     * For outgoing Gnutella 0.6 connections, a function calculating the
     * properties written after the server's "GNUTELLA OK".  For incoming
     * Gnutella 0.6 connections, the properties written after the client's
     * "GNUTELLA CONNECT".
     * Non-final so that the responder can be garbage collected after we've
     * concluded the responding (by setting to null).
     */
    private HandshakeResponder RESPONSE_HEADERS;

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
    /** if I am an Ultrapeer peering to another Ultrapeer */
    private Boolean _isUltrapeerToUltrapeer=null;

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
    private static final IOException CONNECTION_CLOSED =
        new IOException("connection closed");
    
    /** 
     * True iff this should not be policed by the ConnectionWatchdog, e.g.,
     * because this is a connection to a Clip2 reflector. 
     */
    private boolean _isKillable = true;
    
    /** 
     * Use this if a PushProxyAck is received for this MC meaning the remote
     * Ultrapeer can serve as a PushProxy.
     */
    private InetAddress _pushProxyAddr;

    /** 
     * Use this if a PushProxyAck is received for this MC meaning the remote
     * Ultrapeer can serve as a PushProxy.
     */
    private int _pushProxyPort = -1;
    
    /** Use this if a HopsFlowVM instructs us to stop sending queries below
     *  this certain hops value....
     */
    private int _softMaxHops = -1;


    /** The class wide static counter for the number of udp connect back 
     *  request sent.
     */
    private static int _numUDPConnectBackRequests = 0;

    /** The class wide static counter for the number of tcp connect back 
     *  request sent.
     */
    private static int _numTCPConnectBackRequests = 0;

    /**
     * Variable for the <tt>QueryRouteTable</tt> sent for this 
     * connection.
     */
    private QueryRouteTable _lastQRPTableSent;
    
    /**
     * Handle to the message writer for this connection.
     */
    private MessageWriter _messageWriter;
    
    /**
     * Handle to the message reader for this connection.
     */
    private MessageReader _messageReader;
    
    /**
     * Handle to the statistics recording class for this connection.
     */
    private final ConnectionStats STATS = new ConnectionStats(this);
    
    /**
     * Handle to the class that wraps all calls to the query routing tables
     * for this connection.
     */
    private QRPHandler QRP_HANDLER = QRPHandler.createHandler(this);
    
    /** 
     * Filter for filtering out messages that are considered spam.
     */
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();
        
    /**
     * The domain to which this connection is authenticated
     */
    private Set _domains;
    
    /** 
     * The total amount of upstream messaging bandwidth for ALL connections
     * in BYTES (not bits) per second. 
     */
    private static final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH=8000;
                                                            
    /** 
     * Limits outgoing bandwidth for ALL connections. 
     */
    private final static BandwidthThrottle _throttle=
        new BandwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);
        
    /** 
     * The timeout to use when connecting, in milliseconds.  This is NOT used
     * for bootstrap servers.  
     */
    private static final int CONNECT_TIMEOUT = 6000;  //6 seconds

    /**
     * Creates a new outgoing connection to the specified host on the
     * specified port.  
     *
     * @param host the address of the host we're connecting to
     * @param port the port the host is listening on
     */
    public Connection(String host, int port) {
        this(host, port, 
             (RouterService.isSupernode() ? 
              (Properties)(new UltrapeerHeaders(host)) : 
              (Properties)(new LeafHeaders(host))),
             (RouterService.isSupernode() ?
              (HandshakeResponder)new UltrapeerHandshakeResponder(host) :
              (HandshakeResponder)new LeafHandshakeResponder(host)));
    }

    /**
     * More customizable constructor used for testing.
     */
    public static Connection 
        createTestConnection(String host, int port, 
          Properties props, HandshakeResponder responder) { 
        return new Connection(host, port, props, responder);
    }

    /**
     * Creates an incoming connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the
     *  Gnutella handshake.
     */
    public Connection(Socket socket) {
        this(socket, 
              RouterService.isSupernode() ? 
              (HandshakeResponder)(new UltrapeerHandshakeResponder(
                  socket.getInetAddress().getHostAddress())) : 
              (HandshakeResponder)(new LeafHandshakeResponder(
                  socket.getInetAddress().getHostAddress())));
    }
        
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


    /** 
     * Initializes this without timeout; exactly like initialize(0). 
     * @see initialize(int)
     */
    public void initialize() 
        throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(CONNECT_TIMEOUT);
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
            throw CONNECTION_CLOSED;
        } 
        
        // Check to see if this is an attempt to connect to ourselves
        InetAddress localAddress = _socket.getLocalAddress();
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() &&
            _socket.getInetAddress().equals(localAddress) &&
            _port == ConnectionSettings.PORT.getValue()) {
            throw new IOException("Connection to self");
        }

        // Set the Acceptors IP address
        RouterService.getAcceptor().setAddress( localAddress );

        try {
            
            _in = new BufferedInputStream(_socket.getInputStream());
            _out = new ThrottledOutputStream(
                new BufferedOutputStream(_socket.getOutputStream()), 
                _throttle);
        } catch (NullPointerException e) {
            //Apparently Socket.getInput/OutputStream throws
            //NullPointerException if the socket is closed on JVMs prior to 
            //1.3.  (See Sun bug 4091706.)  Unfortunately the socket may 
            //have been closed after the the check above, e.g., if the 
            //user pressed disconnect.  So we catch NullPointerException 
            //here.  An alternative is to obtain a lock before doing these
            //calls, but we are afraid that getInput/OutputStream may be a
            //blocking operation.
            //close();
            throw new IOException("could not establish connection");
        }
        
        //In all the line reading code below, we are somewhat lax in
        //distinguishing between '\r' and '\n'.  Who cares?
        if(isOutgoing())
            initializeOutgoing();
        else
            initializeIncoming();

        _headers = HandshakeResponse.createResponse(HEADERS_READ);
        _headersWritten = HandshakeResponse.createResponse(HEADERS_WRITTEN);

        _connectionTime = System.currentTimeMillis();

        // Now set the soft max TTL that should be used on this connection.
        // The +1 on the soft max for "good" connections is because the 
        // message may come from a leaf, and therefore can have an extra
        // hop.  "Good" connections are connections with features such as 
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
            _out = new CompressingOutputStream(_out, _deflater);
        }            
        if(isReadDeflated()) {
            _inflater = new Inflater();
            _in = new UncompressingInputStream(_in, _inflater);
        }
                   
        // remove the reference to the RESPONSE_HEADERS, since we'll no
        // longer be responding.
        // This does not need to be in a finally clause, because if an
        // exception was thrown, the connection will be removed anyway.
        RESPONSE_HEADERS = null;
        
        // create the read/write classes for messages
        _messageWriter = new MessageWriterProxy(this); 
        _messageReader = new MessageReaderProxy(this);
        
        if(CommonUtils.isJava14OrLater() && 
           ConnectionSettings.USE_NIO.getValue()) {
            _socket.getChannel().configureBlocking(false);
            NIODispatcher.instance().addReader(this);     
        }
         
        // check for updates from this host  
        UpdateManager.instance().checkAndUpdate(this);          
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
                if(theirResponse.getStatusCode() == HandshakeResponse.OK) {
                    // if it's the crawler, we throw an exception to make sure we 
                    // correctly disconnect
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
                createServerUnknown(theirResponse.getStatusCode());
        }        
        
        //If we didn't successfully return out of the method, throw an exception
        //to indicate that handshaking didn't reach any conclusion.  The values
        //here are kind of a hack.
        throw NoGnutellaOkException.UNRESOLVED_CLIENT;
    }
    
    /** 
     * Call this method when the Connection has been initialized and accepted
     * as 'long-lived'.
     */
    public void postInit() {
        try { // TASK 1 - Send a MessagesSupportedVendorMessage if necessary....
            if(headers().supportsVendorMessages()) {
                send(MessagesSupportedVendorMessage.instance());
            }
        } catch (BadPacketException bpe) {
            // should never happen.
            ErrorService.error(bpe);
        }
    }
    
    
    /**
     * Call this method when you want to handle us to handle a VM.  We may....
     */
    public void handleVendorMessage(VendorMessage vm) {
        if (vm instanceof MessagesSupportedVendorMessage)
            _messagesSupported = (MessagesSupportedVendorMessage) vm;
            
        // now i can process
        if (vm instanceof HopsFlowVendorMessage) {
            // update the softMaxHops value so it can take effect....
            HopsFlowVendorMessage hops = (HopsFlowVendorMessage) vm;
            _softMaxHops = hops.getHopValue();
        }
        else if (vm instanceof PushProxyAcknowledgement) {
            // this connection can serve as a PushProxy, so note this....
            PushProxyAcknowledgement ack = (PushProxyAcknowledgement) vm;
            if (Arrays.equals(ack.getGUID(),
                              RouterService.getMessageRouter()._clientGUID)) {
                _pushProxyPort = ack.getListeningPort();
                _pushProxyAddr = ack.getListeningAddress();
            }
            // else mistake on the server side - the guid should be my client
            // guid - not really necessary but whatever
        }
        else if (vm instanceof MessagesSupportedVendorMessage) {

            // see if you need a PushProxy - the remoteHostSupportsPushProxy
            // test incorporates my leaf status in it.....
            if (remoteHostSupportsPushProxy() > -1) {
                // get the client GUID and send off a PushProxyRequest
                GUID clientGUID =
                    new GUID(RouterService.getMessageRouter()._clientGUID);
                try {
                    PushProxyRequest req = new PushProxyRequest(clientGUID);
                    send(req);
                }
                catch (BadPacketException never) {
                    ErrorService.error(never);
                }
            }

            // if we are ignoring local addresses and the connection is local
            // or the guy has a similar address then ignore
            if(ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && 
               (isLocal() || !isConnectBackCapable()))
                return;

            // do i need to send any ConnectBack messages????
            if (!UDPService.instance().canReceiveUnsolicited() &&
                (_numUDPConnectBackRequests < MAX_UDP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsUDPConnectBack() > -1)) {
                try {
                    GUID connectBackGUID =
                        RouterService.getUDPConnectBackGUID();
                    UDPConnectBackVendorMessage udp = 
                        new UDPConnectBackVendorMessage(RouterService.getPort(),
                                                        connectBackGUID);
                    send(udp);
                    _numUDPConnectBackRequests++;
                }
                catch (BadPacketException ignored) {
                    ErrorService.error(ignored);
                }
            }
            if (!RouterService.acceptedIncomingConnection() &&
                (_numTCPConnectBackRequests < MAX_TCP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsTCPConnectBack() > -1)) {
                try {
                    TCPConnectBackVendorMessage tcp =
                       new TCPConnectBackVendorMessage(RouterService.getPort());
                    send(tcp);
                    _numTCPConnectBackRequests++;
                }
                catch (BadPacketException ignored) {
                    ErrorService.error(ignored);
                }
            }
        }
    }
    
    /**
     * Accessor for the <tt>OutputStream</tt> for this connection.  The output
     * stream returned may or may not use compression.
     * 
     * @return the <tt>OutputStream</tt> for this connection
     */
    public OutputStream getOutputStream() {
        return _out;
    }
    
    /**
     * Accessor for the <tt>Inflater</tt> instance for this connection.  The
     * inflater decompresses incoming data if downstream compression is turned
     * on for this connection.
     * 
     * @return this connection's inflater
     */
    public Inflater getInflater() {
        return _inflater;
    }
    
    /**
     * Accessor for the <tt>Deflater</tt> instance for this connection.  The
     * deflater compresses outgoing data if upstream compression is turned
     * on for this connection.
     * 
     * @return this connection's deflater
     */
    public Deflater getDeflater() {
        return _deflater;
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
            if (HeaderNames.REMOTE_IP.equals(key) && ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {
                try {
                    ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(value);
                } catch (IllegalArgumentException ex) {
                }
            }
            HEADERS_READ.put(key, value);
        }
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
        if(!CommonUtils.isJava118()) {
            BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(
                bytes.length);
        }        
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
            //if(!_socket.getChannel().isOpen()) {
                
            //}
            String line=(new ByteReader(_in)).readLine();
            if (line==null)
                throw new IOException("read null line");
            if(!CommonUtils.isJava118()) {
                BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(
                    line.length());
            }
            return line;
        } catch(NullPointerException e) {
            throw CONNECTION_CLOSED;
        } finally {
            //Restore socket timeout.
            _socket.setSoTimeout(oldTimeout);
        }
    }
   
    

    /////////////////////////////////////////////////////////////////////////

    /**
     * Used to determine whether the connection is incoming or outgoing.
     */
    public boolean isOutgoing() {
        return OUTGOING;
    }


    /**
     * Sends a message.  This overrides does extra buffering so that Messages
     * are dropped if the socket gets backed up.  Will remove any extended
     * payloads if the receiving connection does not support GGGEP.   Also
     * updates MessageRouter stats.<p>
     *
     * This method IS thread safe.  Multiple threads can be in a send call
     * at the same time for a given connection.
     *
     * @requires this is fully constructed
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if the connection
     *  is already closed.  This is thread-safe and guaranteed not to block.
     */
    public void send(Message msg) {
        // if Hops Flow is in effect, and this is a QueryRequest, and the
        // hoppage is too biggage, discardage time....
        if ((_softMaxHops > -1) &&
            (msg instanceof QueryRequest) &&
            (msg.getHops() >= _softMaxHops)) {
                
            //TODO: record stats for this
            return;
        }

        if (! supportsGGEP())
            msg = msg.stripExtendedPayload();
            
        try {
            _messageWriter.write(msg);
        } catch (IOException e) {
            // should only happen in the case of NIO
            RouterService.removeConnection(this);
        }
    }

    /**
     * This method is called when a reply is received for a PingRequest
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePingReply(PingReply pingReply,
                                ReplyHandler rh) {
        send(pingReply);
    }

    /**
     * This method is called when a reply is received for a QueryRequest
     * originating on this Connection.  So, send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler rh) {
        send(queryReply);
    }

    /**
     * This method is called when a PushRequest is received for a QueryReply
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePushRequest(PushRequest pushRequest,
                                  ReplyHandler rh) {
        send(pushRequest);
    }  
    
    /**
     * Accessor for the <tt>MessageReader</tt> instance for this connection.
     * The reader handles reading all messages to the network.
     * 
     * @return the <tt>MessageReader</tt> for this connection
     */
    public MessageReader reader() {
        return _messageReader;    
    }
    
    /**
     * Accessor for the <tt>MessageWriter</tt> instance for this connection.
     * The writer handles writing all messages to the network.
     * 
     * @return the <tt>MessageWriter</tt> for this connection
     */
    public MessageWriter writer() {
        return _messageWriter;    
    }
    
    /**
     * Accessor for the <tt>QRPHandler</tt> for this connection.
     * 
     * @return the <tt>QRPHandler</tt> for this connection
     */
    public QRPHandler qrp() {
        return QRP_HANDLER;    
    }   

   /**
    * Returns the IP address of the remote host as a string.
    * 
    * @return the IP address of the remote host as a string
    */
    public String getIPString() {
        return _host;
    }

    /**
     * Accessor for the port number this connection is listening on.  Note that this 
     * is NOT the port of the socket itself.  For incoming connections, the getPort
     * method of the java.net.Socket class returns the ephemeral port that the
     * host connected with.  This port, however, is the port the remote host is
     * listening on for new connections, which we set using Gnutella connection
     * headers in the case of incoming connections.  For outgoing connections,
     * this is the port we used to connect to them -- their listening port.
     * 
     * @return the listening port for the remote host
     */
    public int getListeningPort() {
        return _port;
    }
    
    /** 
     * Sets the port where the conected node listens at, not the one
     * got from socket
     */
    public void setListeningPort(int port){
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
     * Accessor for the <tt>InputStream</tt> for this connection.  The stream
     * may be a buffered stream, a compressed stream, or any other type of
     * stream.
     * 
     * @return the <tt>InputStream</tt> for this connection
     */
    public InputStream getInputStream() {
        return _in;
    }

    /**
     * Returns true if the this connection is potentially on the 'same' network.
     */
    public boolean isConnectBackCapable() throws IllegalStateException {
        byte[] remote = getInetAddress().getAddress();
        byte[] local = _socket.getLocalAddress().getAddress();
        return !NetworkUtils.isCloseIP(local, remote);
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

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsPushProxy() {
        if ((_messagesSupported != null) && isClientSupernodeConnection())
            return _messagesSupported.supportsPushProxy();
        return -1;
    }

    /**
     * Returns whether or not this connection represents a local address.
     *
     * @return <tt>true</tt> if this connection is a local address,
     *  otherwise <tt>false</tt>
     */
    public boolean isLocal() {
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
     * Closes the Connection's socket and thus the connection itself.
     */
    void close() {
        
        // the writer can be null for testing
        if(_messageWriter != null) {
            // notify the message writing thread to finish sending and die
            _messageWriter.setClosed(true);
        }
        
        // Setting this flag insures that the socket is closed if this
        // method is called asynchronously before the socket is initialized.
        _closed = true;
        NetworkUtils.close(_socket);
        
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
    }

    
    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
        return _headers.getUserAgent();
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
                isClientSupernodeConnection2() ?
                    Boolean.TRUE : Boolean.FALSE;
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
     * Returns true iff the connection is an Ultrapeer and I am a Ultrapeer,
     * ie: if I wrote "X-Ultrapeer: true", this connection wrote 
     * "X-Ultrapeer: true" (not necessarily in that order).  <b>Does 
     * NOT require that QRP is enabled</b> between the two; the Ultrapeer 
     * could be using reflector indexing, for example. 
     */
    public boolean isSupernodeSupernodeConnection() {
        if(_isUltrapeerToUltrapeer == null) {
            _isUltrapeerToUltrapeer = 
                isSupernodeSupernodeConnection2() ?
                    Boolean.TRUE : Boolean.FALSE;
        }
        return _isUltrapeerToUltrapeer.booleanValue();
    }

    private boolean isSupernodeSupernodeConnection2() {
        //Is remote host a supernode...
        if (! isSupernodeConnection())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(
            HeaderNames.X_ULTRAPEER);
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
        if(_isLeaf == null) {
            _isLeaf =
                isSupernodeClientConnection2() ?
                    Boolean.TRUE : Boolean.FALSE;
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
    public boolean isQueryRoutingEnabled() {
        return _headers.isQueryRoutingEnabled();
    }


    /** 
     * Returns true if this should not be policed by the ConnectionWatchdog,
     * e.g., because this is a connection to a Clip2 reflector. Default value:
     * true.
     */
    public boolean isKillable() {
        return _isKillable;
    }
    
    /** 
     * Accessor for the query route table associated with this.  This is
     * guaranteed to be non-null, but it may not yet contain any data.
     *
     * @return the <tt>QueryRouteTable</tt> instance containing
     *  query route table data sent along this connection, or <tt>null</tt>
     *  if no data has yet been sent
     */
    public QueryRouteTable getQueryRouteTableSent() {
        return _lastQRPTableSent;
    }

    /**
     * Mutator for the last query route table that was sent along this
     * connection.
     *
     * @param qrt the last query route table that was sent along this
     *  connection
     */
    public void setQueryRouteTableSent(QueryRouteTable qrt) {
        _lastQRPTableSent = qrt;
    }

    
    /** @return a non-negative integer representing the proxy's port for HTTP
     *  communication, a negative number if PushProxy isn't supported.
     */
    public int getPushProxyPort() {
        return _pushProxyPort;
    }

    /** @return the InetAddress of the remote host - only meaningful if
     *  getPushProxyPort() > -1
     *  @see getPushProxyPort()
     */
    public InetAddress getPushProxyAddress() {
        return _pushProxyAddr;
    }
 
    /**
      * Utility method for checking whether or not this message is considered
      * spam.
      * 
      * @param m the <tt>Message</tt> to check
      * @return <tt>true</tt> if this is considered spam, otherwise 
      *  <tt>false</tt>
      */
     public boolean isSpam(Message m) {
         return !_routeFilter.allow(m);
     }

     //
     // Begin Message dropping and filtering calls
     //

     /**
      * A callback for Message Handler implementations to check to see if a
      * message is considered to be undesirable by the message's receiving
      * connection.
      * Messages ignored for this reason are not considered to be dropped, so
      * no statistics are incremented here.
      *
      * @return true if the message is spam, false if it's okay
      */
     public boolean isPersonalSpam(Message m) {
         return !_personalFilter.allow(m);
     }

     /**
      * @modifies this
      * @effects sets the underlying routing filter.   Note that
      *  most filters are not thread-safe, so they should not be shared
      *  among multiple connections.
      */
     public void setRouteFilter(SpamFilter filter) {
         _routeFilter = filter;
     }

     /**
      * @modifies this
      * @effects sets the underlying personal filter.   Note that
      *  most filters are not thread-safe, so they should not be shared
      *  among multiple connections.
      */
     public void setPersonalFilter(SpamFilter filter) {
         _personalFilter = filter;
     }
    
     /**
      * Returns the domain to which this connection is authenticated
      * @return the set (of String) of domains to which this connection 
      * is authenticated. Returns
      * null, in case of unauthenticated connection
      */
     public Set getDomains(){
         //Note that this method is not synchronized, and so _domains may 
         //get initialized multiple times (in case multiple threads invoke this
         //method, before domains is initialized). But thats not a problem as
         //all the instances will have same values, and all but 1 of them 
         //will get garbage collected
        
         if(_domains == null){
             //initialize domains
             _domains = createDomainSet();
         }
         //return the initialized domains
         return _domains;
//         return (String[])_domains.toArray(new String[0]);
     }

     /**
      * creates the set (of String) of domains from the properties sent/received
      * @return the set (of String) of domains
      */
     private Set createDomainSet(){
         Set domainSet;
         //get the domain property
         //In case of outgoing connection, we received the domains from the
         //remote host to whom we authenticated, viceversa for incoming
         //connection
         String domainsAuthenticated;
         if(this.isOutgoing())
             domainsAuthenticated = getDomainsAuthenticated();
         else
             domainsAuthenticated = getPropertyWritten(
                 HeaderNames.X_DOMAINS_AUTHENTICATED);

         //for unauthenticated connections
         if(domainsAuthenticated == null){
             //if no authentication done, initialize to a default domain set
             domainSet = User.createDefaultDomainSet();
         }else{
             domainSet = StringUtils.getSetofValues(domainsAuthenticated);
         }
        
         //return the domain set
         return domainSet;
     } 

   
    /**
     * Implements <tt>ReplyHandler</tt> interface.  Delegates to stats handler
     * for this connection.
     */
    public void countDroppedMessage() {
        stats().countDroppedMessage();    
    }
    
    /**
     * Implements <tt>ReplyHandler</tt> interface.  Delegates to stats handler
     * for this connection.
     * 
     * @return the number of messages received by this connection
     */
    public int getNumMessagesReceived() {
        return stats().getNumMessagesReceived();
    }
    
    /**
     * Accessor for the stats recording class for this connection.
     * 
     * @return the <tt>ConnectionStats</tt> instance for recording stats for
     *  this connection
     */
    public ConnectionStats stats() {
        return STATS;
    }
    

    /**
     * Handles core Gnutella request/reply protocol.  This call
     * will run until the connection is closed.  Note that this is called
     * from the run methods of several different thread implementations
     * that are inner classes of ConnectionManager.  This allows a single
     * thread to be used for initialization and for the request/reply loop.
     *
     * @requires this is initialized
     * @modifies the network underlying this, manager
     * @effects receives request and sends appropriate replies.
     *
     * @throws IOException passed on from the receive call; failures to forward
     *         or route messages are silently swallowed, allowing the message
     *         loop to continue.
     */
    /*
    public void loopForMessages() throws IOException {
        if(CommonUtils.isJava14OrLater() && 
           ConnectionSettings.USE_NIO.getValue()) {
           // anything to do here??
        } else {
            MessageRouter router = RouterService.getMessageRouter();
            while (true) {
                Message m=null;
                try {
                    m = receive();
                    if (m==null)
                        continue;
                } catch (BadPacketException e) {
                    // Don't increment any message counters here.  It's as if
                    // the packet never existed
                    continue;
                }
    
                // Run through the route spam filter and drop accordingly.
                if (isSpam(m)) {
                    if(!CommonUtils.isJava118()) {
                        ReceivedMessageStatHandler.TCP_FILTERED_MESSAGES.
                            addMessage(m);
                    }
                    stats().countDroppedMessage();
                    continue;
                }
    
                //call MessageRouter to handle and process the message
                router.handleMessage(m, this);            
            }
        }
    }
    */


    
    // overrides Object.toString
    public String toString() {
        return "CONNECTION: host=" + _host  + " port=" + _port; 
    }

    /** 
     * True if the OutputRunner died.  For testing only. 
     */
    private boolean _runnerDied = false;
    
    /**
     * Sets whether or not the sending thread has died -- USED ONLY
     * FOR TESTING!
     * 
     * @param died specifies whether or not the sender thread has died
     */
    public void setSenderDied(boolean died) {
        _runnerDied = died;    
    }
    
   
    
    /***************************************************************************
     * UNIT TESTS: tests/com/limegroup/gnutella/ManagedConnectionBufferTest
     **************************************************************************/


    /** FOR TESTING PURPOSES ONLY! */
    public boolean runnerDied() {
        return _runnerDied;
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
    
//  public boolean equals(Object o) {
//      return super.equals(o);
//  }
//  

//  public int hashCode() {
//      return super.hashCode();
//  }
    
    
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
