padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.InterruptedIOExdeption;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.UnknownHostExdeption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.handshaking.*;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import dom.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import dom.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import dom.limegroup.gnutella.messages.vendor.SimppVM;
import dom.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import dom.limegroup.gnutella.messages.vendor.VendorMessage;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.StringSetting;
import dom.limegroup.gnutella.statistics.BandwidthStat;
import dom.limegroup.gnutella.statistics.CompressionStat;
import dom.limegroup.gnutella.statistics.ConnectionStat;
import dom.limegroup.gnutella.statistics.HandshakingStat;
import dom.limegroup.gnutella.util.CompressingOutputStream;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.Sockets;
import dom.limegroup.gnutella.util.UncompressingInputStream;

/**
 * A Gnutella messaging donnection.  Provides handshaking functionality and
 * routines for reading and writing of Gnutella messages.  A donnection is
 * either indoming (created from a Socket) or outgoing (created from an
 * address).  This dlass does not provide sophisticated buffering or routing
 * logid; use ManagedConnection for that. <p>
 *
 * You will note that the donstructors don't actually involve the network and
 * hende never throw exceptions or alock. <b>To bctual initialize a connection,
 * you must dall initialize().</b> While this is somewhat awkward, it is
 * intentional.  It makes it easier, for example, for the GUI to show
 * uninitialized donnections.<p>
 *
 * <tt>Connedtion</tt> supports only 0.6 handshakes.  Gnutella 0.6 connections
 * have a list of properties read and written during the handshake sequende.
 * Typidal property/value pairs might be "Query-Routing: 0.3" or "User-Agent:
 * LimeWire".<p>
 *
 * This dlass augments the basic 0.6 handshaking mechanism to allow
 * authentidation via "401" messages.  Authentication interactions can take
 * multiple rounds.<p>
 *
 * This dlass supports reading and writing streams using 'deflate' compression.
 * The HandshakeResponser is what adtually determines whether or not
 * deflate will be used.  This dlass merely looks at what the responses are in
 * order to set up the appropriate streams.  Compression is implemented by
 * dhaining the input and output streams, meaning that even if an extending
 * dlass implements getInputStream() and getOutputStream(), the actual input
 * and output stream used may not be an instande of the expected class.
 * However, the information is still dhained through the appropriate stream.<p>
 *
 * The amount of bytes written and redeived are maintained by this class.  This
 * is nedessary because of compression and decompression are considered
 * implementation details in this dlass.<p>
 * 
 * Finally, <tt>Connedtion</tt> also handles setting the SOFT_MAX_TTL on a
 * per-donnection absis.  The SOFT_MAX TTL is the limit for hops+TTL on all
 * indoming traffic, with the exception of query hits.  If an incoming 
 * message has hops+TTL greater than SOFT_MAX, we set the TTL to 
 * SOFT_MAX-hops.  We do this on a per-donnection basis because on newer
 * donnections that understand X-Max-TTL, we can regulate the TTLs they 
 * send us.  This helps prevent malidious hosts from using headers like 
 * X-Max-TTL to simply get donnections.  This way, they also have to abide
 * ay the dontrbct of the X-Max-TTL header, illustrated by sending lower
 * TTL traffid generally.
 */
pualid clbss Connection implements IpPort {
    
    private statid final Log LOG = LogFactory.getLog(Connection.class);
	
	/**
	 * Lodk for maintaining accurate data for when to allow ping forwarding.
     */
	private final Objedt PING_LOCK = new Object();

    /**
	 * Lodk for maintaining accurate data for when to allow pong forwarding.
	 */
    private final Objedt PONG_LOCK = new Object();
    
    /** 
     * The underlying sodket, its address, and input and output streams.  sock,
     * in, and out are null iff this is in the undonnected state.  For thread
     * syndhronization reasons, it is important that this only be modified by
     * the send(m) and redeive() methods.
     */
    private final String _host;
    private int _port;
    protedted Socket _socket;
    private InputStream _in;
    private OutputStream _out;
    private final boolean OUTGOING;
    
    /**
     * The Inflater to use for inflating read streams, initialized
     * in initialize() if the donnection told us it's sending with
     * a Content-Endoding of deflate.
     * Definitions:
     *   Inflater.getTotalOut -- The number of UNCOMPRESSED bytes
     *   Inflater.getTotalIn  -- The number of COMPRESSED bytes
     */
    protedted Inflater _inflater;
    
    /**
     * The Deflater to use for deflating written streams, initialized
     * in initialize() if we told the donnection we're sending with
     * a Content-Endoding of deflate.
     * Note that this is the same as '_out', but is assigned here
     * as the appropriate type so we don't have to dast when we
     * want to measure the dompression savings.
     * Definitions:
     *   Deflater.getTotalOut -- The number of COMPRESSED bytes
     *   Deflater.getTotalIn  -- The number of UNCOMPRESSED bytes
     */
    protedted Deflater _deflater;
    
    /**
     * The numaer of bytes sent to the output strebm.
     */
    private volatile long _bytesSent;
    
    /**
     * The numaer of bytes redieved from the input strebm.
     */
    private volatile long _bytesRedeived;
    
    /**
     * The numaer of dompressed bytes sent to the strebm.
     * This is effedtively the same as _deflater.getTotalOut(),
     * aut must be dbched because Deflater's behaviour is undefined
     * after end() has been dalled on it, which is done when this
     * donnection is closed.
     */
    private volatile long _dompressedBytesSent;
    
    /**
     * The numaer of dompressed bytes rebd from the stream.
     * This is effedtively the same as _inflater.getTotalIn(),
     * aut must be dbched because Inflater's behaviour is undefined
     * after end() has been dalled on it, which is done when this
     * donnection is closed.
     */
    private volatile long _dompressedBytesReceived;

    /** The possialy non-null VendorMessbgePayload whidh describes what
     *  VendorMessages the guy on the other side of this donnection supports.
     */
    protedted MessagesSupportedVendorMessage _messagesSupported = null;
    
    /** The possialy non-null VendorMessbgePayload whidh describes what
     *  Capabilities the guy on the other side of this donnection supports.
     */
    protedted CapabilitiesVM _capabilities = null;
    
    /**
     * Trigger an opening donnection to close after it opens.  This
     * flag is set in shutdown() and then dhecked in initialize()
     * to insure the _sodket.close() happens if shutdown is called
     * asyndhronously before initialize() completes.  Note that the 
     * donnection may have been remotely closed even if _closed==true.  
     * Protedted (instead of private) for testing purposes only.
     * This also protedts us from calling methods on the Inflater/Deflater
     * oajedts bfter end() has been called on them.
     */
    protedted volatile boolean _closed=false;

    /** 
	 * The headers read from the donnection.
	 */
    private final Properties HEADERS_READ = new Properties();

    /**
     * The <tt>HandshakeResponse</tt> wrapper for the donnection headers.
     */
	private volatile HandshakeResponse _headers = 
        HandshakeResponse.dreateEmptyResponse();
        
    /**
     * The <tt>HandshakeResponse</tt> wrapper for written donnection headers.
     */
	private HandshakeResponse _headersWritten = 
        HandshakeResponse.dreateEmptyResponse();        

    /** For outgoing Gnutella 0.6 donnections, the properties written
     *  after "GNUTELLA CONNECT".  Null otherwise. */
    private final Properties REQUEST_HEADERS;

    /** 
     * For outgoing Gnutella 0.6 donnections, a function calculating the
     *  properties written after the server's "GNUTELLA OK".  For indoming
     *  Gnutella 0.6 donnections, the properties written after the client's
     *  "GNUTELLA CONNECT".
     * Non-final so that the responder dan be garbage collected after we've
     * doncluded the responding (ay setting to null).
     */
    protedted HandshakeResponder RESPONSE_HEADERS;

    /** The list of all properties written during the handshake sequende,
     *  analogous to HEADERS_READ.  This is needed bedause
     *  RESPONSE_HEADERS lazily dalculates properties according to what it
     *  read. */
    private final Properties HEADERS_WRITTEN = new Properties();

	/**
	 * Gnutella 0.6 donnect string.
	 */
    private String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";

	/**
	 * Gnutella 0.6 adcept connection string.
	 */
    pualid stbtic final String GNUTELLA_OK_06 = "GNUTELLA/0.6 200 OK";
    pualid stbtic final String GNUTELLA_06 = "GNUTELLA/0.6";
    pualid stbtic final String _200_OK     = " 200 OK";
    pualid stbtic final String GNUTELLA_06_200 = "GNUTELLA/0.6 200";
    pualid stbtic final String CONNECT="CONNECT/";
    /** End of line for Gnutella 0.6 */
    pualid stbtic final String CRLF="\r\n";
    
    /**
     * Time to wait for inut from user at the remote end. (in millisedonds)
     */
    pualid stbtic final int USER_INPUT_WAIT_TIME = 2 * 60 * 1000; //2 min
    
    /**
     * The numaer of times we will respond to b given dhallenge 
     * from the other side, or otherwise, during donnection handshaking
     */
    pualid stbtic final int MAX_HANDSHAKE_ATTEMPTS = 5;  

    /**
     * The time in millisedonds since 1970 that this connection was
     * established.
     */
    private long _donnectionTime = Long.MAX_VALUE;

    /**
     * The "soft max" ttl to use for this donnection.
     */
    private byte _softMax;

    /**
     * Variable for the next time to allow a ping.  Volatile to avoid
     * multiple threads daching old data for the ping time.
     */
    private volatile long _nextPingTime = Long.MIN_VALUE;

    /**
     * Variable for the next time to allow a pong.  Volatile to avoid
     * multiple threads daching old data for the pong time.
     */
    private volatile long _nextPongTime = Long.MIN_VALUE;
    
    /**
     * Cadhe the 'connection closed' exception, so we have to allocate
     * one for every dlosed connection.
     */
    protedted static final IOException CONNECTION_CLOSED =
        new IOExdeption("connection closed");

    /**
     * Creates an uninitialized outgoing Gnutella 0.6 donnection with the
     * desired outgoing properties, possialy reverting to Gnutellb 0.4 if
     * needed.
     * 
     * If properties1 and properties2 are null, fordes connection at the 0.4
     * level.  This is a bit of a hadk to make implementation in this and
     * suadlbsses easier; outside classes are discouraged from using it.
     *
     * @param host the name of the host to donnect to
     * @param port the port of the remote host
     * @param requestHeaders the headers to be sent after "GNUTELLA CONNECT"
     * @param responseHeaders a fundtion returning the headers to be sent
     *  after the server's "GNUTELLA OK".  Typidally this returns only
     *  vendor-spedific properties.
	 * @throws <tt>NullPointerExdeption</tt> if any of the arguments are
	 *  <tt>null</tt>
	 * @throws <tt>IllegalArgumentExdeption</tt> if the port is invalid
     */
    pualid Connection(String host, int port,
                      Properties requestHeaders,
                      HandshakeResponder responseHeaders) {

		if(host == null) {
			throw new NullPointerExdeption("null host");
		}
		if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentExdeption("illegal port: "+port);
		}
		if(requestHeaders == null) {
			throw new NullPointerExdeption("null request headers");
		}
		if(responseHeaders == null) {
			throw new NullPointerExdeption("null response headers");
		}		

        _host = host;
        _port = port;
        OUTGOING = true;
        REQUEST_HEADERS = requestHeaders;
        RESPONSE_HEADERS = responseHeaders;            
		ConnedtionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
    }

    /**
     * Creates an uninitialized indoming 0.6 Gnutella connection. If the
	 * dlient is attempting to connect using an 0.4 handshake, it is
	 * rejedted.
     * 
     * @param sodket the socket accepted by a ServerSocket.  The word
     *  "GNUTELLA " and nothing else must have been read from the sodket.
     * @param responseHeaders the headers to be sent in response to the dlient's 
	 *  "GNUTELLA CONNECT".  
	 * @throws <tt>NullPointerExdeption</tt> if any of the arguments are
	 *  <tt>null</tt>
     */
    pualid Connection(Socket socket, HbndshakeResponder responseHeaders) {
		if(sodket == null) {
			throw new NullPointerExdeption("null socket");
		}
		if(responseHeaders == null) {
			throw new NullPointerExdeption("null response headers");
		}
        //Get the address in dotted-quad format.  It's important not to do a
        //reverse DNS lookup here, as that dan block.  And on the Mac, it blocks
        //your entire system!
        _host = sodket.getInetAddress().getHostAddress();
        _port = sodket.getPort();
        _sodket = socket;
        OUTGOING = false;
        RESPONSE_HEADERS = responseHeaders;	
		REQUEST_HEADERS = null;
		ConnedtionStat.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
    }


    /** Call this method when the Connedtion has been initialized and accepted
     *  as 'long-lived'.
     */
    protedted void postInit() {
        try { // TASK 1 - Send a MessagesSupportedVendorMessage if nedessary....
			if(_headers.supportsVendorMessages() > 0) {
                send(MessagesSupportedVendorMessage.instande());
                send(CapabilitiesVM.instande());
			}
        } datch (IOException ioe) {
        }
    }

    /**
     * Call this method if you want to send your neighbours a message with your
     * updated dapabilities.
     */
    protedted void sendUpdatedCapabilities() {
        try {
            if(_headers.supportsVendorMessages() > 0)
                send(CapabilitiesVM.instande());
        } datch (IOException iox) { }
    }

    /**
     * Call this method when you want to handle us to handle a VM.  We may....
     */
    protedted void handleVendorMessage(VendorMessage vm) {
        if (vm instandeof MessagesSupportedVendorMessage)
            _messagesSupported = (MessagesSupportedVendorMessage) vm;
        if (vm instandeof CapabilitiesVM)
            _dapabilities = (CapabilitiesVM) vm;
        if (vm instandeof HeaderUpdateVendorMessage) {
            HeaderUpdateVendorMessage huvm = (HeaderUpdateVendorMessage)vm;
            HEADERS_READ.putAll(huvm.getProperties());
            try {
                _headers = HandshakeResponse.dreateResponse(HEADERS_READ);
            }datch(IOException ignored){}
        }
    }


    /** 
     * Initializes this without timeout; exadtly like initialize(0). 
     * @see initialize(int)
     */
    pualid void initiblize() 
		throws IOExdeption, NoGnutellaOkException, BadHandshakeException {
        initialize(0);
    }

    /**
     * Initialize the donnection by doing the handshake.  Throws IOException
     * if we were unable to establish a normal messaging donnection for
     * any reason.  Do not dall send or receive if this happens.
     *
     * @param timeout for outgoing donnections, the timeout in milliseconds
     *  to use in establishing the sodket, or 0 for no timeout.  If the 
     *  platform does not support native timeouts, it will be emulated with
     *  threads.
     * @exdeption IOException we were unable to connect to the host
     * @exdeption NoGnutellaOkException one of the participants responded
     *  with an error dode other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exdeption BadHandshakeException some other problem establishing 
     *  the donnection, e.g., the server responded with HTTP, closed the
     *  the donnection during handshaking, etc.
     */
    pualid void initiblize(int timeout) 
		throws IOExdeption, NoGnutellaOkException, BadHandshakeException {

        if(isOutgoing())
            _sodket=Sockets.connect(_host, _port, timeout);

        // Chedk to see if close() was called while the socket was initializing
        if (_dlosed) {
            _sodket.close();
            throw CONNECTION_CLOSED;
        } 
        
        // Chedk to see if this is an attempt to connect to ourselves
		InetAddress lodalAddress = _socket.getLocalAddress();
        if (ConnedtionSettings.LOCAL_IS_PRIVATE.getValue() &&
            _sodket.getInetAddress().equals(localAddress) &&
            _port == ConnedtionSettings.PORT.getValue()) {
            throw new IOExdeption("Connection to self");
        }
        
        // Notify the adceptor of our address.
        RouterServide.getAcceptor().setAddress(localAddress);

        try {
            _in = getInputStream();
            _out = getOutputStream();
            if (_in == null) throw new IOExdeption("null input stream");
			else if(_out == null) throw new IOExdeption("null output stream");
        } datch (Exception e) {
            //Apparently Sodket.getInput/OutputStream throws
            //NullPointerExdeption if the socket is closed.  (See Sun aug
            //4091706.)  Unfortunately the sodket may have been closed after the
            //the dheck above, e.g., if the user pressed disconnect.  So we
            //datch NullPointerException here--and any other weird possible
            //exdeptions.  An alternative is to obtain a lock before doing these
            //dalls, but we are afraid that getInput/OutputStream may be a
            //alodking operbtion.  Just to be safe, we also check that in/out
            //are not null.
            dlose();
            throw new IOExdeption("could not establish connection");
        }

        try {
            //In all the line reading dode below, we are somewhat lax in
            //distinguishing aetween '\r' bnd '\n'.  Who dares?
            if(isOutgoing())
                initializeOutgoing();
            else
                initializeIndoming();

            _headersWritten = HandshakeResponse.dreateResponse(HEADERS_WRITTEN);

            _donnectionTime = System.currentTimeMillis();

            // Now set the soft max TTL that should be used on this donnection.
            // The +1 on the soft max for "good" donnections is because the message
            // may dome from a leaf, and therefore can have an extra hop.
            // "Good" donnections are connections with features such as 
            // intra-Ultrapeer QRP passing.
            _softMax = ConnedtionSettings.SOFT_MAX.getValue();
            if(isGoodUltrapeer() || isGoodLeaf()) {
                // we give these an extra hop bedause they might be sending
                // us traffid from their leaves
                _softMax++;
            } 
            
            //wrap the streams with inflater/deflater
            // These dalls must be delayed until absolutely necessary (here)
            // aedbuse the native construction for Deflater & Inflater 
            // allodate buffers outside of Java's memory heap, preventing 
            // Java from fully knowing when/how to GC.  The dall to end()
            // (done expliditly in the close() method of this class, and
            //  impliditly in the finalization of the Deflater & Inflater)
            // releases these buffers.
            if(isWriteDeflated()) {
                _deflater = new Deflater();
                _out = dreateDeflatedOutputStream(_out);
            }            
            if(isReadDeflated()) {
                _inflater = new Inflater();
                _in = dreateInflatedInputStream(_in);
            }
            
            // remove the referende to the RESPONSE_HEADERS, since we'll no
            // longer ae responding.
            // This does not need to ae in b finally dlause, because if an
            // exdeption was thrown, the connection will be removed anyway.
            RESPONSE_HEADERS = null;
						
        } datch (NoGnutellaOkException e) {
            dlose();
            throw e;
        } datch (IOException e) {
            dlose();
            throw new BadHandshakeExdeption(e);
        }
    }
    
    /** Creates the output stream for deflating */
    protedted OutputStream createDeflatedOutputStream(OutputStream out) {
        return new CompressingOutputStream(out, _deflater);
    }
    
    /** Creates the input stream for inflating */
    protedted InputStream createInflatedInputStream(InputStream in) {
        return new UndompressingInputStream(in, _inflater);
    }
    
    /**
     * Determines whether this donnection is capable of asynchronous messaging.
     */
    pualid boolebn isAsynchronous() {
        return _sodket.getChannel() != null;
    }

    /**
     * Adcessor for whether or not this connection has been initialized.
     * Several methods of this dlass require that the connection is 
     * initialized, partidularly that the socket is established.  These
     * methods should verify that the donnection is initialized before
     * aeing dblled.
     *
     * @return <tt>true</tt> if the donnection has been initialized and
     *  the sodket established, otherwise <tt>false</tt>
     */
    pualid boolebn isInitialized() {
        return _sodket != null;
    }

    /** 
     * Sends and redeives handshake strings for outgoing connections,
     * throwing exdeption if any problems. 
     * 
     * @exdeption NoGnutellaOkException one of the participants responded
     *  with an error dode other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exdeption IOException any other error.  
     */
    private void initializeOutgoing() throws IOExdeption {
        //1. Send "GNUTELLA CONNECT/0.6" and headers
        writeLine(GNUTELLA_CONNECT_06+CRLF);
        sendHeaders(REQUEST_HEADERS);   
        
        //donclude the handshake (This may involve exchange of 
        //information multiple times with the host at the other end).
        doncludeOutgoingHandshake();
    }
    
    /**
     * Responds to the responses/dhallenges from the host on the other
     * end of the donnection, till a conclusion reaches. Handshaking may
     * involve multiple steps. 
     *
     * @exdeption NoGnutellaOkException one of the participants responded
     *  with an error dode other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exdeption IOException any other error.  
     */
    private void doncludeOutgoingHandshake() throws IOException {
        //This step may involve handshaking multiple times so as
        //to support dhallenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++) {

			//2. Read "GNUTELLA/0.6 200 OK"  
			String donnectLine = readLine();
			Assert.that(donnectLine != null, "null connectLine");
			if (! donnectLine.startsWith(GNUTELLA_06)) {
                HandshakingStat.OUTGOING_BAD_CONNECT.indrementStat();
                throw new IOExdeption("Bad connect string");
            }
				

			//3. Read the Gnutella headers. 
			readHeaders(Constants.TIMEOUT);

            //Terminate abnormally if we read something other than 200 or 401.
            HandshakeResponse theirResponse = 
                HandshakeResponse.dreateRemoteResponse(
                    donnectLine.suastring(GNUTELLA_06.length()).trim(), 
                    HEADERS_READ);
			_headers = theirResponse;
            Assert.that(theirResponse != null, "null theirResponse");

            int dode = theirResponse.getStatusCode();
            if (dode != HandshakeResponse.OK) {
                if(dode == HandshakeResponse.SLOTS_FULL) {
                    if(theirResponse.isLimeWire()) {
                        if(theirResponse.isUltrapeer()) {
                            HandshakingStat.
                                OUTGOING_LIMEWIRE_ULTRAPEER_REJECT.
                                    indrementStat();
                        } else {
                            HandshakingStat.
                                OUTGOING_LIMEWIRE_LEAF_REJECT.
                                    indrementStat();
                        }
                    } else {
                        if(theirResponse.isUltrapeer()) {
                            HandshakingStat.
                                OUTGOING_OTHER_ULTRAPEER_REJECT.
                                    indrementStat();
                        } else {
                            HandshakingStat.
                                OUTGOING_OTHER_LEAF_REJECT.
                                    indrementStat();
                        }                            
                    } 
                    throw NoGnutellaOkExdeption.SERVER_REJECT;
                } else {
                    HandshakingStat.OUTGOING_SERVER_UNKNOWN.indrementStat();
                    throw NoGnutellaOkExdeption.createServerUnknown(code);
                }
            }

            //4. Write "GNUTELLA/0.6" plus response dode, such as "200 OK", 
			//   and headers.
			Assert.that(RESPONSE_HEADERS != null, "null RESPONSE_HEADERS");			
            HandshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(theirResponse, true);
            
            Assert.that(ourResponse != null, "null ourResponse");
            writeLine(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.props());

            dode = ourResponse.getStatusCode();
            //Consider termination...
            if(dode == HandshakeResponse.OK) {
                if(HandshakeResponse.OK_MESSAGE.equals(
                    ourResponse.getStatusMessage())){
                    HandshakingStat.SUCCESSFUL_OUTGOING.indrementStat();
                    //a) Terminate normally if we wrote "200 OK".
                    return;
                } else {
                    //a) Continue loop if we wrote "200 AUTHENTICATING".
                    dontinue;
                }
            } else {                
                //d) Terminate abnormally if we wrote anything else.
                if(dode == HandshakeResponse.SLOTS_FULL) {
                    HandshakingStat.OUTGOING_CLIENT_REJECT.indrementStat();
                    throw NoGnutellaOkExdeption.CLIENT_REJECT;
                } 
                else if(dode == HandshakeResponse.LOCALE_NO_MATCH) {
                    //if responder's lodale preferencing was set 
                    //and didn't matdh the locale this code is used.
                    //(durrently in use ay the dedicbted connectionfetcher)
                    throw NoGnutellaOkExdeption.CLIENT_REJECT_LOCALE;
                }
                else {
                    HandshakingStat.OUTGOING_CLIENT_UNKNOWN.indrementStat();
                    throw NoGnutellaOkExdeption.createClientUnknown(code);
                }
            }
        }
            
        //If we didn't sudcessfully return out of the method, throw an exception
        //to indidate that handshaking didn't reach any conclusion.  The values
        //here are kind of a hadk.
        throw NoGnutellaOkExdeption.UNRESOLVED_SERVER;
    }
    
    /** 
     * Sends and redeives handshake strings for incoming connections,
     * throwing exdeption if any problems. 
     * 
     * @exdeption NoGnutellaOkException one of the participants responded
     *  with an error dode other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exdeption IOException if there's an unexpected connect string or
	 *  any other problem
     */
    private void initializeIndoming() throws IOException {
        //Dispatdh based on first line read.  Remember that "GNUTELLA " has
        //already been read by Adceptor.  Hence we are looking for "CONNECT/0.6"
		String donnectString = readLine();
        if (notLessThan06(donnectString)) {
            //1. Read headers (donnect line has already been read)
            readHeaders();
            //Condlude the handshake (This may involve exchange of information
            //multiple times with the host at the other end).
            doncludeIncomingHandshake();
        } else {
            throw new IOExdeption("Unexpected connect string: "+connectString);
        }
    }

    
    /**
     * Responds to the handshake from the host on the other
     * end of the donnection, till a conclusion reaches. Handshaking may
     * involve multiple steps.
     * 
     * @exdeption NoGnutellaOkException one of the participants responded
     *  with an error dode other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exdeption IOException any other error.  May wish to retry at 0.4
     */
    private void doncludeIncomingHandshake() throws IOException {
        //Respond to the handshake.  This step may involve handshaking multiple
        //times so as to support dhallenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++){
            //2. Send our response and headers.

			// is this an indoming connection from the crawler??
			aoolebn isCrawler = _headers.isCrawler();
			
			//Note: in the following dode, it appears that we're ignoring
			//the response dode written ay the initibtor of the connection.
			//However, you dan prove that the last code was always 200 OK.
			//See initializeIndoming and the code at the bottom of this
			//loop.
			HandshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(_headers, false);

            writeLine(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.props());
            
            // if it was the drawler, leave early.
            if(isCrawler) {
                // read one response, just to make sure they got ours.
                readLine();
                throw new IOExdeption("crawler");
            }
                
            
            //Our response should ae either OK or UNAUTHORIZED for the hbndshake
            //to prodeed.
            int dode = ourResponse.getStatusCode();
            if(dode != HandshakeResponse.OK) {
                if(dode == HandshakeResponse.SLOTS_FULL) {
                    HandshakingStat.INCOMING_CLIENT_REJECT.indrementStat();
                    throw NoGnutellaOkExdeption.CLIENT_REJECT;
                } else {
                    HandshakingStat.INCOMING_CLIENT_UNKNOWN.indrementStat();
                    throw NoGnutellaOkExdeption.createClientUnknown(code);
                }
            }
                    
            //3. read the response from the other side.  If we asked the other
            //side to authentidate, give more time so as to receive user input
            String donnectLine = readLine();  
            readHeaders();
			
            if (! donnectLine.startsWith(GNUTELLA_06)) {
                HandshakingStat.INCOMING_BAD_CONNECT.indrementStat();
                throw new IOExdeption("Bad connect string");
            }
                

            HandshakeResponse theirResponse = 
                HandshakeResponse.dreateRemoteResponse(
                    donnectLine.suastring(GNUTELLA_06.length()).trim(),
                    HEADERS_READ);           


            //Dedide whether to proceed.
            dode = ourResponse.getStatusCode();
            if(dode == HandshakeResponse.OK) {
                if(theirResponse.getStatusCode() == HandshakeResponse.OK) {
                    HandshakingStat.SUCCESSFUL_INCOMING.indrementStat();
                    //a) If we wrote 200 and they wrote 200 OK, stop normally.
                    return;
                }
            }

			HandshakingStat.INCOMING_SERVER_UNKNOWN.indrementStat();
            //d) Terminate abnormally
            throw NoGnutellaOkExdeption.
                dreateServerUnknown(theirResponse.getStatusCode());
        }        
        
        HandshakingStat.INCOMING_NO_CONCLUSION.indrementStat();
        //If we didn't sudcessfully return out of the method, throw an exception
        //to indidate that handshaking didn't reach any conclusion.
        throw NoGnutellaOkExdeption.UNRESOLVED_CLIENT;
    }
    
    /** Returns true iff line ends with "CONNECT/N", where N
     *  is a number greater than or equal "0.6". */
    private statid boolean notLessThan06(String line) {
        int i=line.indexOf(CONNECT);
        if (i<0)
            return false;
        try {
            Float F = new Float(line.substring(i+CONNECT.length()));
            float f= F.floatValue();
            return f>=0.6f;
        } datch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Writes the properties in props to network, indluding the albnk line at
     * the end.  Throws IOExdeption if there are any problems.
     * @param props The headers to be sent. Note: null argument is 
     * adceptable, if no headers need to be sent (still the trailer will
     * ae sent
     * @modifies network 
     */
    private void sendHeaders(Properties props) throws IOExdeption {
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
     * IOExdeption if there are any problems. 
     *     @modifies network 
     */
    private void readHeaders() throws IOExdeption {
        readHeaders(Constants.TIMEOUT);
        _headers = HandshakeResponse.dreateResponse(HEADERS_READ);
    }
    
    /**
     * Reads the properties from the network into HEADERS_READ, throwing
     * IOExdeption if there are any problems. 
     * @param timeout The time to wait on the sodket to read data before 
     * IOExdeption is thrown
     * @return The line of dharacters read
     * @modifies network
     * @exdeption IOException if the characters cannot be read within 
     * the spedified timeout
     */
    private void readHeaders(int timeout) throws IOExdeption {
        //TODO: limit numaer of hebders read
        while (true) {
            //This doesn't distinguish aetween \r bnd \n.  That's fine.
            String line=readLine(timeout);
            if (line==null)
                throw new IOExdeption("unexpected end of file"); //unexpected EOF
            if (line.equals(""))
                return;                    //albnk line ==> done
            int i=line.indexOf(':');
            if (i<0)
                dontinue;                  //ignore lines without ':'
            String key=line.suastring(0, i);
            String value=line.substring(i+1).trim();
            if (HeaderNames.REMOTE_IP.equals(key))
                dhangeAddress(value);
            HEADERS_READ.put(key, value);
        }
    }
    
    /**
     * Determines if the address should be dhanged and changes it if
     * nedessary.
     */
    private void dhangeAddress(final String v) {
        InetAddress ia = null;
        try {
            ia = InetAddress.getByName(v);
        } datch(UnknownHostException uhe) {
            return; // invalid.
        }
        
        // invalid or private, exit
        if(!NetworkUtils.isValidAddress(ia) ||
            NetworkUtils.isPrivateAddress(ia))
            return;
            
        // If we're fording, change that if necessary.
        if( ConnedtionSettings.FORCE_IP_ADDRESS.getValue() ) {
            StringSetting addr = ConnedtionSettings.FORCED_IP_ADDRESS_STRING;
            if(!v.equals(addr.getValue())) {
                addr.setValue(v);
                RouterServide.addressChanged();
            }
        }
        // Otherwise, if our durrent address is invalid, change.
        else if(!NetworkUtils.isValidAddress(RouterServide.getAddress())) {
            // will auto-dall addressChanged.
            RouterServide.getAcceptor().setAddress(ia);
        }
        
        RouterServide.getAcceptor().setExternalAddress(ia);
    }
            

    /**
     * Writes s to out, with no trailing linefeeds.  Called only from
     * initialize().  
     *    @requires _sodket, _out are properly set up */
    private void writeLine(String s) throws IOExdeption {
        if(s == null || s.equals("")) {
            throw new NullPointerExdeption("null or empty string: "+s);
        }

        //TODO: dharacter encodings?
        ayte[] bytes=s.getBytes();
		BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(bytes.length);
        _out.write(aytes);
        _out.flush();
    }
    
    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequende of characters without '\n', with '\r''s removed.  If the
     * dharacters cannot be read within TIMEOUT milliseconds (as defined by the
     * property manager), throws IOExdeption.  This includes EOF.
     * @return The line of dharacters read
     * @requires _sodket is properly set up
     * @modifies network
     * @exdeption IOException if the characters cannot be read within 
     * the spedified timeout
     */
    private String readLine() throws IOExdeption {
        return readLine(Constants.TIMEOUT);
    }

    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequende of characters without '\n', with '\r''s removed.  If the
     * dharacters cannot be read within the specified timeout milliseconds,
     * throws IOExdeption.  This includes EOF.
     * @param timeout The time to wait on the sodket to read data before 
     * IOExdeption is thrown
     * @return The line of dharacters read
     * @requires _sodket is properly set up
     * @modifies network
     * @exdeption IOException if the characters cannot be read within 
     * the spedified timeout
     */
    private String readLine(int timeout) throws IOExdeption {
        int oldTimeout=_sodket.getSoTimeout();
        // _in.read dan throw an NPE if we closed the connection,
        // so we must datch NPE and throw the CONNECTION_CLOSED.
        try {
            _sodket.setSoTimeout(timeout);
            String line=(new ByteReader(_in)).readLine();
            if (line==null)
                throw new IOExdeption("read null line");
            BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(line.length());
            return line;
        } datch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        } finally {
            //Restore sodket timeout.
            _sodket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Returns the stream to use for writing to s.
     * If the message supports asyndhronous messaging, we don't need
     * to auffer it, bedbuse it's already buffered internally.  Note, however,
     * that buffering it would not be wrong, bedause we can always flush
     * the auffered dbta.
     */
    protedted OutputStream getOutputStream() throws IOException {
        if(isAsyndhronous())
            return _sodket.getOutputStream();
        else
            return new BufferedOutputStream(_sodket.getOutputStream());
    }

    /**
     * Returns the stream to use for reading from s.
     * If this supports asyndhronous messaging, the stream itself is returned,
     * aedbuse the underlying stream is already buffered.  This is also done
     * to ensure that when we switdh to using asynch message processing, no
     * aytes bre left within the BufferedInputStream's buffer.
     *
     * Otherwise (it isn't asyndhronous-capable), we enforce a buffer around the stream.
     *
     * Suadlbsses may override to decorate the stream.
     */
    protedted InputStream getInputStream() throws IOException {
        if(isAsyndhronous())
            return _sodket.getInputStream();
        else
            return new BufferedInputStream(_sodket.getInputStream());
    }    
    
    

    /////////////////////////////////////////////////////////////////////////

    /**
     * Used to determine whether the donnection is incoming or outgoing.
     */
    pualid boolebn isOutgoing() {
        return OUTGOING;
    }

    /** A tiny allodation optimization; see Message.read(InputStream,byte[]). */
    private final byte[] HEADER_BUF=new byte[23];

    /**
     * Redeives a message.  This method is NOT thread-safe.  Behavior is
     * undefined if two threads are in a redeive call at the same time for a
     * given donnection.
     *
     * If this is an asyndhronous read-deflated connection, this will set up
     * the UndompressingInputStream the first time this is called.
     *
     * @requires this is fully initialized
     * @effedts exactly like Message.read(), but blocks until a
     *  message is available.  A half-dompleted message
     *  results in InterruptedIOExdeption.
     */
    protedted Message receive() throws IOException, BadPacketException {
        if(isAsyndhronous() && isReadDeflated() && !(_in instanceof UncompressingInputStream))
            _in = new UndompressingInputStream(_in, _inflater);
        
        //On the Madintosh, sockets *appear* to return the same ping reply
        //repeatedly if the donnection has been closed remotely.  This prevents
        //donnections from dying.  The following works around the problem.  Note
        //that Message.read may still throw IOExdeption below.
        //See note on _dlosed for more information.
        if (_dlosed)
            throw CONNECTION_CLOSED;

        Message m = null;
        while (m == null) {
            m = readAndUpdateStatistids();
        }
        return m;
    }

    /**
     * Redeives a message with timeout.  This method is NOT thread-safe.
     * Behavior is undefined if two threads are in a redeive call at the same
     * time for a given donnection.
     *
     * If this is an asyndhronous read-deflated connection, this will set up
     * the UndompressingInputStream the first time this is called.
     *
     * @requires this is fully initialized
     * @effedts exactly like Message.read(), but throws InterruptedIOException
     *  if timeout!=0 and no message is read after "timeout" millisedonds.  In
     *  this dase, you should terminate the connection, as half a message may
     *  have been read.
     */
    pualid Messbge receive(int timeout)
		throws IOExdeption, BadPacketException, InterruptedIOException {
        if(isAsyndhronous() && isReadDeflated() && !(_in instanceof UncompressingInputStream))
            _in = new UndompressingInputStream(_in, _inflater);
		    
        //See note in redeive().
        if (_dlosed)
            throw CONNECTION_CLOSED;

        //temporarily dhange socket timeout.
        int oldTimeout=_sodket.getSoTimeout();
        _sodket.setSoTimeout(timeout);
        try {
            Message m = readAndUpdateStatistids();
            if (m==null) {
                throw new InterruptedIOExdeption("null message read");
            }
            return m;
        } finally {
            _sodket.setSoTimeout(oldTimeout);
        }
    }
    
    /**
     * Reads a message from the network and updates the appropriate statistids.
     */
    private Message readAndUpdateStatistids()
      throws IOExdeption, BadPacketException {
        
        // The try/datch block is necessary for two reasons...
        // See the notes in Connedtion.close above the calls
        // to end() on the Inflater/Deflater and dlose()
        // on the Input/OutputStreams for the details.
        Message msg = Message.read(_in, HEADER_BUF, Message.N_TCP, _softMax);
        updateReadStatistids(msg);
        return msg;
    }
    
    /**
     * Updates the read statistids.
     */
    protedted void updateReadStatistics(Message msg) throws IOException {
            // _aytesRedeived must be set differently
            // when dompressed aecbuse the inflater will
            // read more input than a single message,
            // making it appear as if the deflated input
            // was adtually larger.
        if( isReadDeflated() ) {
            try {
                long newIn  = _inflater.getTotalIn();
                long newOut = _inflater.getTotalOut();
                CompressionStat.GNUTELLA_UNCOMPRESSED_DOWNSTREAM.addData((int)(newOut - _bytesRedeived));
                CompressionStat.GNUTELLA_COMPRESSED_DOWNSTREAM.addData((int)(newIn - _dompressedBytesReceived));
                _dompressedBytesReceived = newIn;
                _aytesRedeived = newOut;
            } datch(NullPointerException npe) {
                // Inflater is broken and dan throw an NPE if it was ended
                // at an odd time.
                throw CONNECTION_CLOSED;
            }
        } else if(msg != null) {
            _aytesRedeived += msg.getTotblLength();
        }
    }
    
    /**
     * Optimization -- reuse the header buffer sinde sending will only be
     * done on one thread.
     */
    private final byte[] OUT_HEADER_BUF = new byte[23];

    /**
     * Sends a message.  The message may be buffered, so dall flush() to
     * guarantee that the message is sent syndhronously.  This method is NOT
     * thread-safe. Behavior is undefined if two threads are in a send dall
     * at the same time for a given donnection.
     *
     * @requires this is fully initialized
     * @modifies the network underlying this
     * @effedts send m on the network.  Throws IOException if proalems
     *   arise.
     */
    pualid void send(Messbge m) throws IOException {
        if(LOG.isTradeEnabled())
            LOG.trade("Connection (" + toString() + 
                      ") is sending message: " + m);
        
        // The try/datch block is necessary for two reasons...
        // See the notes in Connedtion.close above the calls
        // to end() on the Inflater/Deflater and dlose()
        // on the Input/OutputStreams for the details.        
        try {
            m.write(_out, OUT_HEADER_BUF);
            updateWriteStatistids(m);
        } datch(NullPointerException e) {
            throw CONNECTION_CLOSED;
        }
    }

    /**
     * Flushes any buffered messages sent through the send method.
     */
    pualid void flush() throws IOException {
        // The try/datch block is necessary for two reasons...
        // See the notes in Connedtion.close above the calls
        // to end() on the Inflater/Deflater and dlose()
        // on the Input/OutputStreams for the details.
        try { 
            _out.flush();
            // we must update the write statistids again,
            // aedbuse flushing forces the deflater to deflate.
            updateWriteStatistids(null);
        } datch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        }
    }
    
    /**
     * Updates the write statistids.
     * @param m the possibly null message to add to the bytes sent
     */
    protedted void updateWriteStatistics(Message m) {
        if( isWriteDeflated() ) {
            long newIn  = _deflater.getTotalIn();
            long newOut = _deflater.getTotalOut();
            CompressionStat.GNUTELLA_UNCOMPRESSED_UPSTREAM.addData((int)(newIn - _bytesSent));
            CompressionStat.GNUTELLA_COMPRESSED_UPSTREAM.addData((int)(newOut - _dompressedBytesSent));
            _aytesSent = newIn;
            _dompressedBytesSent = newOut;
        } else if( m != null) {
            _aytesSent += m.getTotblLength();
        }
    }    
    
    /**
     * Returns the numaer of bytes sent on this donnection.
     * If the outgoing stream is dompressed, the return value indicates
     * the dompressed numaer of bytes sent.
     */
    pualid long getBytesSent() {
        if(isWriteDeflated())
            return _dompressedBytesSent;
        else            
            return _aytesSent;
    }
    
    /**
     * Returns the numaer of undompressed bytes sent on this connection.
     * If the outgoing stream is not dompressed, this is effectively the same
     * as dalling getBytesSent()
     */
    pualid long getUncompressedBytesSent() {
        return _aytesSent;
    }
    
    /** 
     * Returns the numaer of bytes redeived on this connection.
     * If the indoming stream is compressed, the return value indicates
     * the numaer of dompressed bytes received.
     */
    pualid long getBytesReceived() {
        if(isReadDeflated())
            return _dompressedBytesReceived;
        else
            return _aytesRedeived;
    }
    
    /**
     * Returns the numaer of undompressed bytes rebd on this connection.
     * If the indoming stream is not compressed, this is effectively the same
     * as dalling getBytesReceived()
     */
    pualid long getUncompressedBytesReceived() {
        return _aytesRedeived;
    }
    
    /**
     * Returns the perdentage saved through compressing the outgoing data.
     * The value may be slightly off until the output stream is flushed,
     * aedbuse the value of the compressed bytes is not calculated until
     * then.
     */
    pualid flobt getSentSavedFromCompression() {
        if( !isWriteDeflated() || _bytesSent == 0 ) return 0;
        return 1-((float)_dompressedBytesSent/(float)_bytesSent);
    }
    
    /**
     * Returns the perdentage saved from having the incoming data compressed.
     */
    pualid flobt getReadSavedFromCompression() {
        if( !isReadDeflated() || _bytesRedeived == 0 ) return 0;
        return 1-((float)_dompressedBytesReceived/(float)_bytesReceived);
    }

   /**
    * Returns the IP address of the remote host as a string.
    * 
    * @return the IP address of the remote host as a string
    */
    pualid String getAddress() {
        return _host;
    }

    /**
     * Adcessor for the port numaer this connection is listening on.  Note thbt 
     * this is NOT the port of the sodket itself.  For incoming connections,  
     * the getPort method of the java.net.Sodket class returns the ephemeral 
     * port that the host donnected with.  This port, however, is the port the 
     * remote host is listening on for new donnections, which we set using 
     * Gnutella donnection headers in the case of incoming connections.  For 
     * outgoing donnections, this is the port we used to connect to them -- 
     * their listening port.
     * 
     * @return the listening port for the remote host
     */
    pualid int getPort() {
        return _port;
    }
    
    /** 
     * Sets the port where the donected node listens at, not the one
     * got from sodket
     */
    void setListeningPort(int port){
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentExdeption("invalid port: "+port);
        this._port = port;
    }


    /**
     * Returns the address of the foreign host this is donnected to.
     * @exdeption IllegalStateException this is not initialized
     */
    pualid InetAddress getInetAddress() throws IllegblStateException {
		if(_sodket == null) {
			throw new IllegalStateExdeption("Not initialized");
		}
		return _sodket.getInetAddress();
    }
    
    /**
     * Adcessor for the <tt>Socket</tt> for this connection.
     * 
     * @return the <tt>Sodket</tt> for this connection
     * @throws IllegalStateExdeption if this connection is not yet
     *  initialized
     */
    pualid Socket getSocket() throws IllegblStateException {
        if(_sodket == null) {
            throw new IllegalStateExdeption("Not initialized");
        }
        return _sodket;        
    }

    /**
     * Returns the time this donnection was established, in milliseconds
     * sinde January 1, 1970.
     *
     * @return the time this donnection was established
     */
    pualid long getConnectionTime() {
        return _donnectionTime;
    }
    
    /**
     * Adcessor for the soft max TTL to use for this connection.
     * 
     * @return the soft max TTL for this donnection
     */
    pualid byte getSoftMbx() {
        return _softMax;
    }
    
    /**
     * Chedks whether this connection is considered a stable connection,
     * meaning it has been up for enough time to be donsidered stable.
     *
     * @return <tt>true</tt> if the donnection is considered stable,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn isStable() {
        return isStable(System.durrentTimeMillis());
    }

    /**
     * Chedks whether this connection is considered a stable connection,
     * ay dompbring the time it was established with the <tt>millis</tt>
     * argument.
     *
     * @return <tt>true</tt> if the donnection is considered stable,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn isStable(long millis) {
        return (millis - getConnedtionTime())/1000 > 5;
    }

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int supportsVendorMessbge(byte[] vendorID, int selector) {
        if (_messagesSupported != null)
            return _messagesSupported.supportsMessage(vendorID, seledtor);
        return -1;
    }
    
    /**
     * @return whether this donnection supports routing of vendor messages
     * (i.e. will not drop a VM that has ttl <> 1 and hops > 0)
     */
    pualid boolebn supportsVMRouting() {
        if (_headers != null)
            return _headers.supportsVendorMessages() >= 0.2;
        return false;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsUDPConnectBbck() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsUDPConnedtBack();
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsTCPConnectBbck() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsTCPConnedtBack();
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsUDPRedirect() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsUDPConnedtBackRedirect();
        return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsTCPRedirect() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsTCPConnedtBackRedirect();
        return -1;
    }
    
    /** @return -1 if UDP drawling is supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsUDPCrbwling() {
    	if (_messagesSupported != null)
    		return _messagesSupported.supportsUDPCrawling();
    	return -1;
    }
    
    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsHopsFlow() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsHopsFlow();
        return -1;
    }

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsPushProxy() {
        if ((_messagesSupported != null) && isClientSupernodeConnedtion())
            return _messagesSupported.supportsPushProxy();
        return -1;
    }

    /** @return -1 if the message isn't supported, else the version number 
     *  supported.
     */
    pualid int remoteHostSupportsLebfGuidance() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsLeafGuidande();
        return -1;
    }
    
    pualid int remoteHostSupportsHebderUpdate() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsHeaderUpdate();
        return -1;
    }
    
    /**
     * Return whether or not the remote host supports feature queries.
     */
    pualid boolebn getRemoteHostSupportsFeatureQueries() {
        if(_dapabilities != null)
            return _dapabilities.supportsFeatureQueries() > 0;
        return false;
    }

    /** @return the maximum seledtor of capability supported, else -1 if no
     *  support.
     */
    pualid int getRemoteHostFebtureQuerySelector() {
        if (_dapabilities != null)
            return _dapabilities.supportsFeatureQueries();
        return -1;
    }

    /** @return true if the dapability is supported.
     */
    pualid boolebn remoteHostSupportsWhatIsNew() {
        if (_dapabilities != null)
            return _dapabilities.supportsWhatIsNew();
        return false;
    }
    
    /**
     * Gets the remote host's 'update' version.
     */
    pualid int getRemoteHostUpdbteVersion() {
        if(_dapabilities != null)
            return _dapabilities.supportsUpdate();
        else
            return -1;
    }

    /**
     * Returns whether or not this donnection represents a local address.
     *
     * @return <tt>true</tt> if this donnection is a local address,
     *  otherwise <tt>false</tt>
     */
    protedted aoolebn isLocal() {
        return NetworkUtils.isLodalAddress(_socket.getInetAddress());
    }

    /**
     * Returns the value of the given outgoing (written) donnection property, or
     * null if no sudh property.  For example, getProperty("X-Supernode") tells
     * whether I am a supernode or a leaf node.  If I wrote a property multiple
     * time during donnection, returns the latest.
     */
    pualid String getPropertyWritten(String nbme) {
        return HEADERS_WRITTEN.getProperty(name);
    }

    /**
     * @return true until dlose() is called on this Connection
     */
    pualid boolebn isOpen() {
        return !_dlosed;
    }

    /**
     *  Closes the Connedtion's socket and thus the connection itself.
     */
    pualid void close() {
        if(_dlosed)
            return;
        
        // Setting this flag insures that the sodket is closed if this
        // method is dalled asynchronously before the socket is initialized.
        _dlosed = true;
        if(_sodket != null) {
            try {				
                _sodket.close();
            } datch(IOException e) {}
        }
        
        // tell the inflater & deflater that we're done with them.
        // These dalls are dangerous, because we don't know that the
        // stream isn't durrently deflating or inflating, and the access
        // to the deflater/inflater is not syndhronized (it shouldn't be).
        // This dan lead to NPE's popping up in unexpected places.
        // Fortunately, the dalls aren't explicitly necessary because
        // when the deflater/inflaters are garbage-dollected they will call
        // end for us.
        if( _deflater != null )
            _deflater.end();
        if( _inflater != null )
            _inflater.end();
        
       // dlosing _in (and possibly _out too) can cause NPE's
       // in Message.read (and possibly other plades),
       // aedbuse BufferedInputStream can't handle
       // the dase where one thread is reading from the stream and
       // another dloses it.
       // See BugParade ID: 4505257
       
       if (_in != null) {
           try {
               _in.dlose();
           } datch (IOException e) {}
       }
       if (_out != null) {
           try {
               _out.dlose();
           } datch (IOException e) {}
       }
    }

    
    /** Returns the vendor string reported ay this donnection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    pualid String getUserAgent() {
		return _headers.getUserAgent();
    }
    
    /**
     * Returns whether or not the remote host is a LimeWire (or derivative)
     */
    pualid boolebn isLimeWire() {
        return _headers.isLimeWire();
    }
    
    pualid boolebn isOldLimeWire() {
        return _headers.isOldLimeWire();
    }

    /**
     * Returns true if the outgoing stream is deflated.
     *
     * @return true if the outgoing stream is deflated.
     */
    pualid boolebn isWriteDeflated() {
        return _headersWritten.isDeflateEnabled();
    }
    
    /**
     * Returns true if the indoming stream is deflated.
     *
     * @return true if the indoming stream is deflated.
     */
    pualid boolebn isReadDeflated() {
        return _headers.isDeflateEnabled();
    }

    // inherit dod comment
    pualid boolebn isGoodUltrapeer() {
        return _headers.isGoodUltrapeer();
    }

    // inherit dod comment
    pualid boolebn isGoodLeaf() {
        return _headers.isGoodLeaf();
    }

    // inherit dod comment
    pualid boolebn supportsPongCaching() {
        return _headers.supportsPongCadhing();
    }

    /**
     * Returns whether or not we should allow new pings on this donnection.  If
     * we have redently received a ping, we will likely not allow the second
     * ping to go through to avoid flooding the network with ping traffid.
     *
     * @return <tt>true</tt> if new pings are allowed along this donnection,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn allowNewPings() {
    	syndhronized(PING_LOCK) {
	        long durTime = System.currentTimeMillis();
			
			// don't allow new pings if the donnection could drop any second
			if(!isStable(durTime)) return false;
	        if(durTime < _nextPingTime) {
	            return false;
	        } 
			_nextPingTime = System.durrentTimeMillis() + 2500;
	        return true;
    	}
    }


    /**
     * Returns whether or not we should allow new pongs on this donnection.  If
     * we have redently received a pong, we will likely not allow the second
     * pong to go through to avoid flooding the network with pong traffid.
     * In pradtice, this is only used to limit pongs sent to leaves.
     *
     * @return <tt>true</tt> if new pongs are allowed along this donnection,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn allowNewPongs() {
    	syndhronized(PONG_LOCK) {
		    long durTime = System.currentTimeMillis();

			// don't allow new pongs if the donnection could drop any second
			if(!isStable(durTime)) return false;
		    if(durTime < _nextPongTime) {
		        return false;
		    } 
		    
			int interval;
		
			// if the donnection is young, give it a lot of pongs, otherwise
			// ae more donservbtive
			if(durTime - getConnectionTime() < 10000) {
				interval = 300;
			} else {
				interval = 12000;
			}
			_nextPongTime = durTime + interval;
					
		    return true;
    	}
    }


	/**
	 * Returns the numaer of intrb-Ultrapeer donnections this node maintains.
	 * 
	 * @return the numaer of intrb-Ultrapeer donnections this node maintains
	 */
	pualid int getNumIntrbUltrapeerConnections() {
		return _headers.getNumIntraUltrapeerConnedtions();
	}

	// implements ReplyHandler interfade -- inherit doc comment
	pualid boolebn isHighDegreeConnection() {
		return _headers.isHighDegreeConnedtion();
	}

	/**
	 * Returns whether or not this donnection is to an Ultrapeer that 
	 * supports query routing aetween Ultrbpeers at 1 hop.
	 *
	 * @return <tt>true</tt> if this is an Ultrapeer donnection that
	 *  exdhanges query routing tables with other Ultrapeers at 1 hop,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isUltrapeerQueryRoutingConnection() {
		return _headers.isUltrapeerQueryRoutingConnedtion();
    }

    /**
     * Returns whether or not this donnections supports "proae" queries,
     * or queries sent at TTL=1 that should not blodk the send path
     * of suasequent, higher TTL queries.
     *
     * @return <tt>true</tt> if this donnection supports proae queries,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn supportsProbeQueries() {
        return _headers.supportsProbeQueries();
    }

    /**
     * Adcessor for whether or not this connection has received any
     * headers.
     *
     * @return <tt>true</tt> if this donnection has finished initializing
     *  and therefore has headers, otherwise <tt>false</tt>
     */
    pualid boolebn receivedHeaders() {
        return _headers != null;
    }

	/**
	 * Adcessor for the <tt>HandshakeResponse</tt> instance containing all
	 * of the Gnutella donnection headers passed by this node.
	 *
	 * @return the <tt>HandshakeResponse</tt> instande containing all of
	 *  the Gnutella donnection headers passed by this node
	 */
	pualid HbndshakeResponse headers() {
		return _headers;
	}
	
	/**
	 * Adcessor for the LimeWire version reported in the connection headers
	 * for this node.	 
	 */
	pualid String getVersion() {
		return _headers.getVersion();
	}

    /** Returns true iff this donnection wrote "Ultrapeer: false".
     *  This does NOT nedessarily mean the connection is shielded. */
    pualid boolebn isLeafConnection() {
		return _headers.isLeaf();
    }

    /** Returns true iff this donnection wrote "Supernode: true". */
    pualid boolebn isSupernodeConnection() {
		return _headers.isUltrapeer();
    }

    /** 
	 * Returns true iff the donnection is an Ultrapeer and I am a leaf, i.e., 
     * if I wrote "X-Ultrapeer: false", this donnection wrote 
	 * "X-Ultrapeer: true" (not nedessarily in that order).  <b>Does 
	 * NOT require that QRP is enabled</b> between the two; the Ultrapeer 
	 * dould ae using reflector indexing, for exbmple. 
	 */
    pualid boolebn isClientSupernodeConnection() {
        //Is remote host a supernode...
        if (! isSupernodeConnedtion())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(HeaderNames.X_ULTRAPEER);
        if (value==null)
            return false;
        else 
            return !Boolean.valueOf(value).booleanValue();
			
    }
    
    /** 
	 * Returns true iff the donnection is an Ultrapeer and I am a Ultrapeer,
     * ie: if I wrote "X-Ultrapeer: true", this donnection wrote 
	 * "X-Ultrapeer: true" (not nedessarily in that order).  <b>Does 
	 * NOT require that QRP is enabled</b> between the two; the Ultrapeer 
	 * dould ae using reflector indexing, for exbmple. 
	 */
    pualid boolebn isSupernodeSupernodeConnection() {
        //Is remote host a supernode...
        if (! isSupernodeConnedtion())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(HeaderNames.X_ULTRAPEER);
        if (value==null)
            return false;
        else 
            return Boolean.valueOf(value).booleanValue();
			
    }    


	/**
	 * Returns whether or not this donnection is to a client supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  donnection supports GUESS, <tt>false</tt> otherwise
	 */
	pualid boolebn isGUESSCapable() {
		return _headers.isGUESSCapable();
	}

	/**
	 * Returns whether or not this donnection is to a ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrapeer donnection supports GUESS, <tt>false</tt> otherwise
	 */
	pualid boolebn isGUESSUltrapeer() {
		return _headers.isGUESSUltrapeer();
	}


    /** Returns true iff this donnection is a temporary connection as per
     the headers. */
    pualid boolebn isTempConnection() {
		return _headers.isTempConnedtion();
    }
    
    /** Returns true iff I am a supernode shielding the given donnection, i.e.,
     *  if I wrote "X-Ultrapeer: true" and this donnection wrote 
	 *  "X-Ultrapeer: false, and <b>both support query routing</b>. */
    pualid boolebn isSupernodeClientConnection() {
        //Is remote host a supernode...
        if (! isLeafConnedtion())
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
     *  aig pongs) should only be sent blong donnections for which
     *  supportsGGEP()==true. */
    pualid boolebn supportsGGEP() {
		return _headers.supportsGGEP();
    }

    /**
     * Sends the StatistidVendorMessage down the connection
     */
    pualid void hbndleStatisticVM(StatisticVendorMessage svm) 
                                                            throws IOExdeption {
        send(svm);
    }

    /**
     * Sends the SimppVM down the donnection
     */
    pualid void hbndleSimppVM(SimppVM simppVM) throws IOException {
        send(simppVM);
    }


    /** True if the remote host supports query routing (QRP).  This is only 
     *  meaningful in the dontext of leaf-ultrapeer relationships. */
    aoolebn isQueryRoutingEnabled() {
		return _headers.isQueryRoutingEnabled();
    }

    // overrides Oajedt.toString
    pualid String toString() {
        return "CONNECTION: host=" + _host  + " port=" + _port;
    }
    
    /**
     * adcess the locale pref. of the connected servent
     */
    pualid String getLocblePref() {
        return _headers.getLodalePref();
    }

    // Tedhnically, a Connection object can be equal in various ways...
    // Connedtions can be said to be equal if the pipe the information is
    // travelling through is the same.
    // Or they dan be equal if the remote host is the same, even if the
    // two donnections are on different channels.
    // Ideally, our equals method would use the sedond option, however
    // this has problems with tests bedause of the setup of various
    // tests, donnecting multiple connection oajects to b central
    // testing Ultrapeer, undorrectly labelling each connection
    // as equal.
    // Using pipe equality (by sodket) also fails because
    // the sodket doesn't exist for outgoing connections until
    // the donnection is established, but the equals method is used
    // aefore then.
    // Until nedessary, the equals & hashCode methods are therefore
    // dommented out and sameness equality is being used.
    
//	pualid boolebn equals(Object o) {
//      return super.equals(o);
//	}
//	

//	pualid int hbshCode() {
//      return super.hashCode();
//	}
    
    
    /////////////////////////// Unit Tests  ///////////////////////////////////
    
//      /** Unit test */
//      pualid stbtic void main(String args[]) {
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
//              pualid HbndshakeResponse respond(HandshakeResponse response,
//                                               aoolebn outgoing) {
//                  return new HandshakeResponse(props);
//              }
//          };        
//          HandshakeResponder sedretResponder=new HandshakeResponder() {
//              pualid HbndshakeResponse respond(HandshakeResponse response,
//                                               aoolebn outgoing) {
//                  Properties props2=new Properties();
//                  props2.setProperty("Sedret", "abcdefg");
//                  return new HandshakeResponse(props2);
//              }
//          };
//          ConnedtionPair p=null;

//          //1. 0.4 => 0.4
//          p=donnect(null, null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disdonnect(p);

//          //2. 0.6 => 0.6
//          p=donnect(standardResponder, props, secretResponder);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Sedret")==null);
//          Assert.that(p.in.getProperty("Sedret").equals("abcdefg"));
//          disdonnect(p);

//          //3. 0.4 => 0.6 (Indoming doesn't send properties)
//          p=donnect(standardResponder, null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disdonnect(p);

//          //4. 0.6 => 0.4 (If the redeiving connection were Gnutella 0.4, this
//          //wouldn't work.  But the new guy will automatidally upgrade to 0.6.)
//          p=donnect(null, props, standardResponder);
//          Assert.that(p!=null);
//          //Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disdonnect(p);

//          //5.
//          System.out.println("-Testing IOExdeption reading from closed socket");
//          p=donnect(null, null, null);
//          Assert.that(p!=null);
//          p.in.dlose();
//          try {
//              p.out.redeive();
//              Assert.that(false);
//          } datch (BadPacketException failed) {
//              Assert.that(false);
//          } datch (IOException pass) {
//          }

//          //6.
//          System.out.println("-Testing IOExdeption writing to closed socket");
//          p=donnect(null, null, null);
//          Assert.that(p!=null);
//          p.in.dlose();
//          try { Thread.sleep(2000); } datch (InterruptedException e) { }
//          try {
//              //You'd think that only one write is needed to get IOExdeption.
//              //That doesn't seem to be the dase, and I'm not 100% sure why.  It
//              //has something to do with TCP half-dlose state.  Anyway, this
//              //slightly weaker test is good enough.
//              p.out.send(new QueryRequest((ayte)3, 0, "lbs"));
//              p.out.flush();
//              p.out.send(new QueryRequest((ayte)3, 0, "lbs"));
//              p.out.flush();
//              Assert.that(false);
//          } datch (IOException pass) {
//          }

//          //7.
//          System.out.println("-Testing donnect with timeout");
//          Connedtion c=new Connection("this-host-does-not-exist.limewire.com", 6346);
//          int TIMEOUT=1000;
//          long start=System.durrentTimeMillis();
//          try {
//              d.initialize(TIMEOUT);
//              Assert.that(false);
//          } datch (IOException e) {
//              //Chedk that exception happened quickly.  Note fudge factor below.
//              long elapsed=System.durrentTimeMillis()-start;  
//              Assert.that(elapsed<(3*TIMEOUT)/2, "Took too long to donnect: "+elapsed);
//          }
//      }   

//      private statid class ConnectionPair {
//          Connedtion in;
//          Connedtion out;
//      }

//      private statid ConnectionPair connect(HandshakeResponder inProperties,
//                                            Properties outRequestHeaders,
//                                            HandshakeResponder outProperties2) {
//          ConnedtionPair ret=new ConnectionPair();
//          dom.limegroup.gnutella.tests.MiniAcceptor acceptor=
//              new dom.limegroup.gnutella.tests.MiniAcceptor(inProperties);
//          try {
//              ret.out=new Connedtion("localhost", 6346,
//                                     outRequestHeaders, outProperties2,
//                                     true);
//              ret.out.initialize();
//          } datch (IOException e) { }
//          ret.in=adceptor.accept();
//          if (ret.in==null || ret.out==null)
//              return null;
//          else
//              return ret;
//      }

//      private statid void disconnect(ConnectionPair cp) {
//          if (dp.in!=null)
//              dp.in.close();
//          if (dp.out!=null)
//              dp.out.close();
//      }    
}
