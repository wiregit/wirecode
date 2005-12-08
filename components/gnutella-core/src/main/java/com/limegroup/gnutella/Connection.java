pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.InterruptedIOException;
import jbva.io.OutputStream;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.UnknownHostException;
import jbva.util.Enumeration;
import jbva.util.Properties;
import jbva.util.zip.Deflater;
import jbva.util.zip.Inflater;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.handshaking.*;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutellb.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutellb.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutellb.messages.vendor.SimppVM;
import com.limegroup.gnutellb.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutellb.messages.vendor.VendorMessage;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.StringSetting;
import com.limegroup.gnutellb.statistics.BandwidthStat;
import com.limegroup.gnutellb.statistics.CompressionStat;
import com.limegroup.gnutellb.statistics.ConnectionStat;
import com.limegroup.gnutellb.statistics.HandshakingStat;
import com.limegroup.gnutellb.util.CompressingOutputStream;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.Sockets;
import com.limegroup.gnutellb.util.UncompressingInputStream;

/**
 * A Gnutellb messaging connection.  Provides handshaking functionality and
 * routines for rebding and writing of Gnutella messages.  A connection is
 * either incoming (crebted from a Socket) or outgoing (created from an
 * bddress).  This class does not provide sophisticated buffering or routing
 * logic; use MbnagedConnection for that. <p>
 *
 * You will note thbt the constructors don't actually involve the network and
 * hence never throw exceptions or block. <b>To bctual initialize a connection,
 * you must cbll initialize().</b> While this is somewhat awkward, it is
 * intentionbl.  It makes it easier, for example, for the GUI to show
 * uninitiblized connections.<p>
 *
 * <tt>Connection</tt> supports only 0.6 hbndshakes.  Gnutella 0.6 connections
 * hbve a list of properties read and written during the handshake sequence.
 * Typicbl property/value pairs might be "Query-Routing: 0.3" or "User-Agent:
 * LimeWire".<p>
 *
 * This clbss augments the basic 0.6 handshaking mechanism to allow
 * buthentication via "401" messages.  Authentication interactions can take
 * multiple rounds.<p>
 *
 * This clbss supports reading and writing streams using 'deflate' compression.
 * The HbndshakeResponser is what actually determines whether or not
 * deflbte will be used.  This class merely looks at what the responses are in
 * order to set up the bppropriate streams.  Compression is implemented by
 * chbining the input and output streams, meaning that even if an extending
 * clbss implements getInputStream() and getOutputStream(), the actual input
 * bnd output stream used may not be an instance of the expected class.
 * However, the informbtion is still chained through the appropriate stream.<p>
 *
 * The bmount of bytes written and received are maintained by this class.  This
 * is necessbry because of compression and decompression are considered
 * implementbtion details in this class.<p>
 * 
 * Finblly, <tt>Connection</tt> also handles setting the SOFT_MAX_TTL on a
 * per-connection bbsis.  The SOFT_MAX TTL is the limit for hops+TTL on all
 * incoming trbffic, with the exception of query hits.  If an incoming 
 * messbge has hops+TTL greater than SOFT_MAX, we set the TTL to 
 * SOFT_MAX-hops.  We do this on b per-connection basis because on newer
 * connections thbt understand X-Max-TTL, we can regulate the TTLs they 
 * send us.  This helps prevent mblicious hosts from using headers like 
 * X-Mbx-TTL to simply get connections.  This way, they also have to abide
 * by the contrbct of the X-Max-TTL header, illustrated by sending lower
 * TTL trbffic generally.
 */
public clbss Connection implements IpPort {
    
    privbte static final Log LOG = LogFactory.getLog(Connection.class);
	
	/**
	 * Lock for mbintaining accurate data for when to allow ping forwarding.
     */
	privbte final Object PING_LOCK = new Object();

    /**
	 * Lock for mbintaining accurate data for when to allow pong forwarding.
	 */
    privbte final Object PONG_LOCK = new Object();
    
    /** 
     * The underlying socket, its bddress, and input and output streams.  sock,
     * in, bnd out are null iff this is in the unconnected state.  For thread
     * synchronizbtion reasons, it is important that this only be modified by
     * the send(m) bnd receive() methods.
     */
    privbte final String _host;
    privbte int _port;
    protected Socket _socket;
    privbte InputStream _in;
    privbte OutputStream _out;
    privbte final boolean OUTGOING;
    
    /**
     * The Inflbter to use for inflating read streams, initialized
     * in initiblize() if the connection told us it's sending with
     * b Content-Encoding of deflate.
     * Definitions:
     *   Inflbter.getTotalOut -- The number of UNCOMPRESSED bytes
     *   Inflbter.getTotalIn  -- The number of COMPRESSED bytes
     */
    protected Inflbter _inflater;
    
    /**
     * The Deflbter to use for deflating written streams, initialized
     * in initiblize() if we told the connection we're sending with
     * b Content-Encoding of deflate.
     * Note thbt this is the same as '_out', but is assigned here
     * bs the appropriate type so we don't have to cast when we
     * wbnt to measure the compression savings.
     * Definitions:
     *   Deflbter.getTotalOut -- The number of COMPRESSED bytes
     *   Deflbter.getTotalIn  -- The number of UNCOMPRESSED bytes
     */
    protected Deflbter _deflater;
    
    /**
     * The number of bytes sent to the output strebm.
     */
    privbte volatile long _bytesSent;
    
    /**
     * The number of bytes recieved from the input strebm.
     */
    privbte volatile long _bytesReceived;
    
    /**
     * The number of compressed bytes sent to the strebm.
     * This is effectively the sbme as _deflater.getTotalOut(),
     * but must be cbched because Deflater's behaviour is undefined
     * bfter end() has been called on it, which is done when this
     * connection is closed.
     */
    privbte volatile long _compressedBytesSent;
    
    /**
     * The number of compressed bytes rebd from the stream.
     * This is effectively the sbme as _inflater.getTotalIn(),
     * but must be cbched because Inflater's behaviour is undefined
     * bfter end() has been called on it, which is done when this
     * connection is closed.
     */
    privbte volatile long _compressedBytesReceived;

    /** The possibly non-null VendorMessbgePayload which describes what
     *  VendorMessbges the guy on the other side of this connection supports.
     */
    protected MessbgesSupportedVendorMessage _messagesSupported = null;
    
    /** The possibly non-null VendorMessbgePayload which describes what
     *  Cbpabilities the guy on the other side of this connection supports.
     */
    protected CbpabilitiesVM _capabilities = null;
    
    /**
     * Trigger bn opening connection to close after it opens.  This
     * flbg is set in shutdown() and then checked in initialize()
     * to insure the _socket.close() hbppens if shutdown is called
     * bsynchronously before initialize() completes.  Note that the 
     * connection mby have been remotely closed even if _closed==true.  
     * Protected (instebd of private) for testing purposes only.
     * This blso protects us from calling methods on the Inflater/Deflater
     * objects bfter end() has been called on them.
     */
    protected volbtile boolean _closed=false;

    /** 
	 * The hebders read from the connection.
	 */
    privbte final Properties HEADERS_READ = new Properties();

    /**
     * The <tt>HbndshakeResponse</tt> wrapper for the connection headers.
     */
	privbte volatile HandshakeResponse _headers = 
        HbndshakeResponse.createEmptyResponse();
        
    /**
     * The <tt>HbndshakeResponse</tt> wrapper for written connection headers.
     */
	privbte HandshakeResponse _headersWritten = 
        HbndshakeResponse.createEmptyResponse();        

    /** For outgoing Gnutellb 0.6 connections, the properties written
     *  bfter "GNUTELLA CONNECT".  Null otherwise. */
    privbte final Properties REQUEST_HEADERS;

    /** 
     * For outgoing Gnutellb 0.6 connections, a function calculating the
     *  properties written bfter the server's "GNUTELLA OK".  For incoming
     *  Gnutellb 0.6 connections, the properties written after the client's
     *  "GNUTELLA CONNECT".
     * Non-finbl so that the responder can be garbage collected after we've
     * concluded the responding (by setting to null).
     */
    protected HbndshakeResponder RESPONSE_HEADERS;

    /** The list of bll properties written during the handshake sequence,
     *  bnalogous to HEADERS_READ.  This is needed because
     *  RESPONSE_HEADERS lbzily calculates properties according to what it
     *  rebd. */
    privbte final Properties HEADERS_WRITTEN = new Properties();

	/**
	 * Gnutellb 0.6 connect string.
	 */
    privbte String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";

	/**
	 * Gnutellb 0.6 accept connection string.
	 */
    public stbtic final String GNUTELLA_OK_06 = "GNUTELLA/0.6 200 OK";
    public stbtic final String GNUTELLA_06 = "GNUTELLA/0.6";
    public stbtic final String _200_OK     = " 200 OK";
    public stbtic final String GNUTELLA_06_200 = "GNUTELLA/0.6 200";
    public stbtic final String CONNECT="CONNECT/";
    /** End of line for Gnutellb 0.6 */
    public stbtic final String CRLF="\r\n";
    
    /**
     * Time to wbit for inut from user at the remote end. (in milliseconds)
     */
    public stbtic final int USER_INPUT_WAIT_TIME = 2 * 60 * 1000; //2 min
    
    /**
     * The number of times we will respond to b given challenge 
     * from the other side, or otherwise, during connection hbndshaking
     */
    public stbtic final int MAX_HANDSHAKE_ATTEMPTS = 5;  

    /**
     * The time in milliseconds since 1970 thbt this connection was
     * estbblished.
     */
    privbte long _connectionTime = Long.MAX_VALUE;

    /**
     * The "soft mbx" ttl to use for this connection.
     */
    privbte byte _softMax;

    /**
     * Vbriable for the next time to allow a ping.  Volatile to avoid
     * multiple threbds caching old data for the ping time.
     */
    privbte volatile long _nextPingTime = Long.MIN_VALUE;

    /**
     * Vbriable for the next time to allow a pong.  Volatile to avoid
     * multiple threbds caching old data for the pong time.
     */
    privbte volatile long _nextPongTime = Long.MIN_VALUE;
    
    /**
     * Cbche the 'connection closed' exception, so we have to allocate
     * one for every closed connection.
     */
    protected stbtic final IOException CONNECTION_CLOSED =
        new IOException("connection closed");

    /**
     * Crebtes an uninitialized outgoing Gnutella 0.6 connection with the
     * desired outgoing properties, possibly reverting to Gnutellb 0.4 if
     * needed.
     * 
     * If properties1 bnd properties2 are null, forces connection at the 0.4
     * level.  This is b bit of a hack to make implementation in this and
     * subclbsses easier; outside classes are discouraged from using it.
     *
     * @pbram host the name of the host to connect to
     * @pbram port the port of the remote host
     * @pbram requestHeaders the headers to be sent after "GNUTELLA CONNECT"
     * @pbram responseHeaders a function returning the headers to be sent
     *  bfter the server's "GNUTELLA OK".  Typically this returns only
     *  vendor-specific properties.
	 * @throws <tt>NullPointerException</tt> if bny of the arguments are
	 *  <tt>null</tt>
	 * @throws <tt>IllegblArgumentException</tt> if the port is invalid
     */
    public Connection(String host, int port,
                      Properties requestHebders,
                      HbndshakeResponder responseHeaders) {

		if(host == null) {
			throw new NullPointerException("null host");
		}
		if(!NetworkUtils.isVblidPort(port)) {
			throw new IllegblArgumentException("illegal port: "+port);
		}
		if(requestHebders == null) {
			throw new NullPointerException("null request hebders");
		}
		if(responseHebders == null) {
			throw new NullPointerException("null response hebders");
		}		

        _host = host;
        _port = port;
        OUTGOING = true;
        REQUEST_HEADERS = requestHebders;
        RESPONSE_HEADERS = responseHebders;            
		ConnectionStbt.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
    }

    /**
     * Crebtes an uninitialized incoming 0.6 Gnutella connection. If the
	 * client is bttempting to connect using an 0.4 handshake, it is
	 * rejected.
     * 
     * @pbram socket the socket accepted by a ServerSocket.  The word
     *  "GNUTELLA " bnd nothing else must have been read from the socket.
     * @pbram responseHeaders the headers to be sent in response to the client's 
	 *  "GNUTELLA CONNECT".  
	 * @throws <tt>NullPointerException</tt> if bny of the arguments are
	 *  <tt>null</tt>
     */
    public Connection(Socket socket, HbndshakeResponder responseHeaders) {
		if(socket == null) {
			throw new NullPointerException("null socket");
		}
		if(responseHebders == null) {
			throw new NullPointerException("null response hebders");
		}
        //Get the bddress in dotted-quad format.  It's important not to do a
        //reverse DNS lookup here, bs that can block.  And on the Mac, it blocks
        //your entire system!
        _host = socket.getInetAddress().getHostAddress();
        _port = socket.getPort();
        _socket = socket;
        OUTGOING = fblse;
        RESPONSE_HEADERS = responseHebders;	
		REQUEST_HEADERS = null;
		ConnectionStbt.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
    }


    /** Cbll this method when the Connection has been initialized and accepted
     *  bs 'long-lived'.
     */
    protected void postInit() {
        try { // TASK 1 - Send b MessagesSupportedVendorMessage if necessary....
			if(_hebders.supportsVendorMessages() > 0) {
                send(MessbgesSupportedVendorMessage.instance());
                send(CbpabilitiesVM.instance());
			}
        } cbtch (IOException ioe) {
        }
    }

    /**
     * Cbll this method if you want to send your neighbours a message with your
     * updbted capabilities.
     */
    protected void sendUpdbtedCapabilities() {
        try {
            if(_hebders.supportsVendorMessages() > 0)
                send(CbpabilitiesVM.instance());
        } cbtch (IOException iox) { }
    }

    /**
     * Cbll this method when you want to handle us to handle a VM.  We may....
     */
    protected void hbndleVendorMessage(VendorMessage vm) {
        if (vm instbnceof MessagesSupportedVendorMessage)
            _messbgesSupported = (MessagesSupportedVendorMessage) vm;
        if (vm instbnceof CapabilitiesVM)
            _cbpabilities = (CapabilitiesVM) vm;
        if (vm instbnceof HeaderUpdateVendorMessage) {
            HebderUpdateVendorMessage huvm = (HeaderUpdateVendorMessage)vm;
            HEADERS_READ.putAll(huvm.getProperties());
            try {
                _hebders = HandshakeResponse.createResponse(HEADERS_READ);
            }cbtch(IOException ignored){}
        }
    }


    /** 
     * Initiblizes this without timeout; exactly like initialize(0). 
     * @see initiblize(int)
     */
    public void initiblize() 
		throws IOException, NoGnutellbOkException, BadHandshakeException {
        initiblize(0);
    }

    /**
     * Initiblize the connection by doing the handshake.  Throws IOException
     * if we were unbble to establish a normal messaging connection for
     * bny reason.  Do not call send or receive if this happens.
     *
     * @pbram timeout for outgoing connections, the timeout in milliseconds
     *  to use in estbblishing the socket, or 0 for no timeout.  If the 
     *  plbtform does not support native timeouts, it will be emulated with
     *  threbds.
     * @exception IOException we were unbble to connect to the host
     * @exception NoGnutellbOkException one of the participants responded
     *  with bn error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception BbdHandshakeException some other problem establishing 
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during hbndshaking, etc.
     */
    public void initiblize(int timeout) 
		throws IOException, NoGnutellbOkException, BadHandshakeException {

        if(isOutgoing())
            _socket=Sockets.connect(_host, _port, timeout);

        // Check to see if close() wbs called while the socket was initializing
        if (_closed) {
            _socket.close();
            throw CONNECTION_CLOSED;
        } 
        
        // Check to see if this is bn attempt to connect to ourselves
		InetAddress locblAddress = _socket.getLocalAddress();
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getVblue() &&
            _socket.getInetAddress().equbls(localAddress) &&
            _port == ConnectionSettings.PORT.getVblue()) {
            throw new IOException("Connection to self");
        }
        
        // Notify the bcceptor of our address.
        RouterService.getAcceptor().setAddress(locblAddress);

        try {
            _in = getInputStrebm();
            _out = getOutputStrebm();
            if (_in == null) throw new IOException("null input strebm");
			else if(_out == null) throw new IOException("null output strebm");
        } cbtch (Exception e) {
            //Appbrently Socket.getInput/OutputStream throws
            //NullPointerException if the socket is closed.  (See Sun bug
            //4091706.)  Unfortunbtely the socket may have been closed after the
            //the check bbove, e.g., if the user pressed disconnect.  So we
            //cbtch NullPointerException here--and any other weird possible
            //exceptions.  An blternative is to obtain a lock before doing these
            //cblls, but we are afraid that getInput/OutputStream may be a
            //blocking operbtion.  Just to be safe, we also check that in/out
            //bre not null.
            close();
            throw new IOException("could not estbblish connection");
        }

        try {
            //In bll the line reading code below, we are somewhat lax in
            //distinguishing between '\r' bnd '\n'.  Who cares?
            if(isOutgoing())
                initiblizeOutgoing();
            else
                initiblizeIncoming();

            _hebdersWritten = HandshakeResponse.createResponse(HEADERS_WRITTEN);

            _connectionTime = System.currentTimeMillis();

            // Now set the soft mbx TTL that should be used on this connection.
            // The +1 on the soft mbx for "good" connections is because the message
            // mby come from a leaf, and therefore can have an extra hop.
            // "Good" connections bre connections with features such as 
            // intrb-Ultrapeer QRP passing.
            _softMbx = ConnectionSettings.SOFT_MAX.getValue();
            if(isGoodUltrbpeer() || isGoodLeaf()) {
                // we give these bn extra hop because they might be sending
                // us trbffic from their leaves
                _softMbx++;
            } 
            
            //wrbp the streams with inflater/deflater
            // These cblls must be delayed until absolutely necessary (here)
            // becbuse the native construction for Deflater & Inflater 
            // bllocate buffers outside of Java's memory heap, preventing 
            // Jbva from fully knowing when/how to GC.  The call to end()
            // (done explicitly in the close() method of this clbss, and
            //  implicitly in the finblization of the Deflater & Inflater)
            // relebses these buffers.
            if(isWriteDeflbted()) {
                _deflbter = new Deflater();
                _out = crebteDeflatedOutputStream(_out);
            }            
            if(isRebdDeflated()) {
                _inflbter = new Inflater();
                _in = crebteInflatedInputStream(_in);
            }
            
            // remove the reference to the RESPONSE_HEADERS, since we'll no
            // longer be responding.
            // This does not need to be in b finally clause, because if an
            // exception wbs thrown, the connection will be removed anyway.
            RESPONSE_HEADERS = null;
						
        } cbtch (NoGnutellaOkException e) {
            close();
            throw e;
        } cbtch (IOException e) {
            close();
            throw new BbdHandshakeException(e);
        }
    }
    
    /** Crebtes the output stream for deflating */
    protected OutputStrebm createDeflatedOutputStream(OutputStream out) {
        return new CompressingOutputStrebm(out, _deflater);
    }
    
    /** Crebtes the input stream for inflating */
    protected InputStrebm createInflatedInputStream(InputStream in) {
        return new UncompressingInputStrebm(in, _inflater);
    }
    
    /**
     * Determines whether this connection is cbpable of asynchronous messaging.
     */
    public boolebn isAsynchronous() {
        return _socket.getChbnnel() != null;
    }

    /**
     * Accessor for whether or not this connection hbs been initialized.
     * Severbl methods of this class require that the connection is 
     * initiblized, particularly that the socket is established.  These
     * methods should verify thbt the connection is initialized before
     * being cblled.
     *
     * @return <tt>true</tt> if the connection hbs been initialized and
     *  the socket estbblished, otherwise <tt>false</tt>
     */
    public boolebn isInitialized() {
        return _socket != null;
    }

    /** 
     * Sends bnd receives handshake strings for outgoing connections,
     * throwing exception if bny problems. 
     * 
     * @exception NoGnutellbOkException one of the participants responded
     *  with bn error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException bny other error.  
     */
    privbte void initializeOutgoing() throws IOException {
        //1. Send "GNUTELLA CONNECT/0.6" bnd headers
        writeLine(GNUTELLA_CONNECT_06+CRLF);
        sendHebders(REQUEST_HEADERS);   
        
        //conclude the hbndshake (This may involve exchange of 
        //informbtion multiple times with the host at the other end).
        concludeOutgoingHbndshake();
    }
    
    /**
     * Responds to the responses/chbllenges from the host on the other
     * end of the connection, till b conclusion reaches. Handshaking may
     * involve multiple steps. 
     *
     * @exception NoGnutellbOkException one of the participants responded
     *  with bn error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException bny other error.  
     */
    privbte void concludeOutgoingHandshake() throws IOException {
        //This step mby involve handshaking multiple times so as
        //to support chbllenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++) {

			//2. Rebd "GNUTELLA/0.6 200 OK"  
			String connectLine = rebdLine();
			Assert.thbt(connectLine != null, "null connectLine");
			if (! connectLine.stbrtsWith(GNUTELLA_06)) {
                HbndshakingStat.OUTGOING_BAD_CONNECT.incrementStat();
                throw new IOException("Bbd connect string");
            }
				

			//3. Rebd the Gnutella headers. 
			rebdHeaders(Constants.TIMEOUT);

            //Terminbte abnormally if we read something other than 200 or 401.
            HbndshakeResponse theirResponse = 
                HbndshakeResponse.createRemoteResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(), 
                    HEADERS_READ);
			_hebders = theirResponse;
            Assert.thbt(theirResponse != null, "null theirResponse");

            int code = theirResponse.getStbtusCode();
            if (code != HbndshakeResponse.OK) {
                if(code == HbndshakeResponse.SLOTS_FULL) {
                    if(theirResponse.isLimeWire()) {
                        if(theirResponse.isUltrbpeer()) {
                            HbndshakingStat.
                                OUTGOING_LIMEWIRE_ULTRAPEER_REJECT.
                                    incrementStbt();
                        } else {
                            HbndshakingStat.
                                OUTGOING_LIMEWIRE_LEAF_REJECT.
                                    incrementStbt();
                        }
                    } else {
                        if(theirResponse.isUltrbpeer()) {
                            HbndshakingStat.
                                OUTGOING_OTHER_ULTRAPEER_REJECT.
                                    incrementStbt();
                        } else {
                            HbndshakingStat.
                                OUTGOING_OTHER_LEAF_REJECT.
                                    incrementStbt();
                        }                            
                    } 
                    throw NoGnutellbOkException.SERVER_REJECT;
                } else {
                    HbndshakingStat.OUTGOING_SERVER_UNKNOWN.incrementStat();
                    throw NoGnutellbOkException.createServerUnknown(code);
                }
            }

            //4. Write "GNUTELLA/0.6" plus response code, such bs "200 OK", 
			//   bnd headers.
			Assert.thbt(RESPONSE_HEADERS != null, "null RESPONSE_HEADERS");			
            HbndshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(theirResponse, true);
            
            Assert.thbt(ourResponse != null, "null ourResponse");
            writeLine(GNUTELLA_06 + " " + ourResponse.getStbtusLine() + CRLF);
            sendHebders(ourResponse.props());

            code = ourResponse.getStbtusCode();
            //Consider terminbtion...
            if(code == HbndshakeResponse.OK) {
                if(HbndshakeResponse.OK_MESSAGE.equals(
                    ourResponse.getStbtusMessage())){
                    HbndshakingStat.SUCCESSFUL_OUTGOING.incrementStat();
                    //b) Terminate normally if we wrote "200 OK".
                    return;
                } else {
                    //b) Continue loop if we wrote "200 AUTHENTICATING".
                    continue;
                }
            } else {                
                //c) Terminbte abnormally if we wrote anything else.
                if(code == HbndshakeResponse.SLOTS_FULL) {
                    HbndshakingStat.OUTGOING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellbOkException.CLIENT_REJECT;
                } 
                else if(code == HbndshakeResponse.LOCALE_NO_MATCH) {
                    //if responder's locble preferencing was set 
                    //bnd didn't match the locale this code is used.
                    //(currently in use by the dedicbted connectionfetcher)
                    throw NoGnutellbOkException.CLIENT_REJECT_LOCALE;
                }
                else {
                    HbndshakingStat.OUTGOING_CLIENT_UNKNOWN.incrementStat();
                    throw NoGnutellbOkException.createClientUnknown(code);
                }
            }
        }
            
        //If we didn't successfully return out of the method, throw bn exception
        //to indicbte that handshaking didn't reach any conclusion.  The values
        //here bre kind of a hack.
        throw NoGnutellbOkException.UNRESOLVED_SERVER;
    }
    
    /** 
     * Sends bnd receives handshake strings for incoming connections,
     * throwing exception if bny problems. 
     * 
     * @exception NoGnutellbOkException one of the participants responded
     *  with bn error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException if there's bn unexpected connect string or
	 *  bny other problem
     */
    privbte void initializeIncoming() throws IOException {
        //Dispbtch based on first line read.  Remember that "GNUTELLA " has
        //blready been read by Acceptor.  Hence we are looking for "CONNECT/0.6"
		String connectString = rebdLine();
        if (notLessThbn06(connectString)) {
            //1. Rebd headers (connect line has already been read)
            rebdHeaders();
            //Conclude the hbndshake (This may involve exchange of information
            //multiple times with the host bt the other end).
            concludeIncomingHbndshake();
        } else {
            throw new IOException("Unexpected connect string: "+connectString);
        }
    }

    
    /**
     * Responds to the hbndshake from the host on the other
     * end of the connection, till b conclusion reaches. Handshaking may
     * involve multiple steps.
     * 
     * @exception NoGnutellbOkException one of the participants responded
     *  with bn error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException bny other error.  May wish to retry at 0.4
     */
    privbte void concludeIncomingHandshake() throws IOException {
        //Respond to the hbndshake.  This step may involve handshaking multiple
        //times so bs to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++){
            //2. Send our response bnd headers.

			// is this bn incoming connection from the crawler??
			boolebn isCrawler = _headers.isCrawler();
			
			//Note: in the following code, it bppears that we're ignoring
			//the response code written by the initibtor of the connection.
			//However, you cbn prove that the last code was always 200 OK.
			//See initiblizeIncoming and the code at the bottom of this
			//loop.
			HbndshakeResponse ourResponse = 
				RESPONSE_HEADERS.respond(_hebders, false);

            writeLine(GNUTELLA_06 + " " + ourResponse.getStbtusLine() + CRLF);
            sendHebders(ourResponse.props());
            
            // if it wbs the crawler, leave early.
            if(isCrbwler) {
                // rebd one response, just to make sure they got ours.
                rebdLine();
                throw new IOException("crbwler");
            }
                
            
            //Our response should be either OK or UNAUTHORIZED for the hbndshake
            //to proceed.
            int code = ourResponse.getStbtusCode();
            if(code != HbndshakeResponse.OK) {
                if(code == HbndshakeResponse.SLOTS_FULL) {
                    HbndshakingStat.INCOMING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellbOkException.CLIENT_REJECT;
                } else {
                    HbndshakingStat.INCOMING_CLIENT_UNKNOWN.incrementStat();
                    throw NoGnutellbOkException.createClientUnknown(code);
                }
            }
                    
            //3. rebd the response from the other side.  If we asked the other
            //side to buthenticate, give more time so as to receive user input
            String connectLine = rebdLine();  
            rebdHeaders();
			
            if (! connectLine.stbrtsWith(GNUTELLA_06)) {
                HbndshakingStat.INCOMING_BAD_CONNECT.incrementStat();
                throw new IOException("Bbd connect string");
            }
                

            HbndshakeResponse theirResponse = 
                HbndshakeResponse.createRemoteResponse(
                    connectLine.substring(GNUTELLA_06.length()).trim(),
                    HEADERS_READ);           


            //Decide whether to proceed.
            code = ourResponse.getStbtusCode();
            if(code == HbndshakeResponse.OK) {
                if(theirResponse.getStbtusCode() == HandshakeResponse.OK) {
                    HbndshakingStat.SUCCESSFUL_INCOMING.incrementStat();
                    //b) If we wrote 200 and they wrote 200 OK, stop normally.
                    return;
                }
            }

			HbndshakingStat.INCOMING_SERVER_UNKNOWN.incrementStat();
            //c) Terminbte abnormally
            throw NoGnutellbOkException.
                crebteServerUnknown(theirResponse.getStatusCode());
        }        
        
        HbndshakingStat.INCOMING_NO_CONCLUSION.incrementStat();
        //If we didn't successfully return out of the method, throw bn exception
        //to indicbte that handshaking didn't reach any conclusion.
        throw NoGnutellbOkException.UNRESOLVED_CLIENT;
    }
    
    /** Returns true iff line ends with "CONNECT/N", where N
     *  is b number greater than or equal "0.6". */
    privbte static boolean notLessThan06(String line) {
        int i=line.indexOf(CONNECT);
        if (i<0)
            return fblse;
        try {
            Flobt F = new Float(line.substring(i+CONNECT.length()));
            flobt f= F.floatValue();
            return f>=0.6f;
        } cbtch (NumberFormatException e) {
            return fblse;
        }
    }

    /**
     * Writes the properties in props to network, including the blbnk line at
     * the end.  Throws IOException if there bre any problems.
     * @pbram props The headers to be sent. Note: null argument is 
     * bcceptable, if no headers need to be sent (still the trailer will
     * be sent
     * @modifies network 
     */
    privbte void sendHeaders(Properties props) throws IOException {
        if(props != null) {
            Enumerbtion names=props.propertyNames();
            while (nbmes.hasMoreElements()) {
                String key=(String)nbmes.nextElement();
                String vblue=props.getProperty(key);
                // Overwrite bny domainname with true IP address
                if ( HebderNames.REMOTE_IP.equals(key) )
                    vblue=getInetAddress().getHostAddress();
                if (vblue==null)
                    vblue="";
                writeLine(key+": "+vblue+CRLF);   
                HEADERS_WRITTEN.put(key, vblue);
            }
        }
        //send the trbiler
        writeLine(CRLF);
    }


    /**
     * Rebds the properties from the network into HEADERS_READ, throwing
     * IOException if there bre any problems. 
     *     @modifies network 
     */
    privbte void readHeaders() throws IOException {
        rebdHeaders(Constants.TIMEOUT);
        _hebders = HandshakeResponse.createResponse(HEADERS_READ);
    }
    
    /**
     * Rebds the properties from the network into HEADERS_READ, throwing
     * IOException if there bre any problems. 
     * @pbram timeout The time to wait on the socket to read data before 
     * IOException is thrown
     * @return The line of chbracters read
     * @modifies network
     * @exception IOException if the chbracters cannot be read within 
     * the specified timeout
     */
    privbte void readHeaders(int timeout) throws IOException {
        //TODO: limit number of hebders read
        while (true) {
            //This doesn't distinguish between \r bnd \n.  That's fine.
            String line=rebdLine(timeout);
            if (line==null)
                throw new IOException("unexpected end of file"); //unexpected EOF
            if (line.equbls(""))
                return;                    //blbnk line ==> done
            int i=line.indexOf(':');
            if (i<0)
                continue;                  //ignore lines without ':'
            String key=line.substring(0, i);
            String vblue=line.substring(i+1).trim();
            if (HebderNames.REMOTE_IP.equals(key))
                chbngeAddress(value);
            HEADERS_READ.put(key, vblue);
        }
    }
    
    /**
     * Determines if the bddress should be changed and changes it if
     * necessbry.
     */
    privbte void changeAddress(final String v) {
        InetAddress ib = null;
        try {
            ib = InetAddress.getByName(v);
        } cbtch(UnknownHostException uhe) {
            return; // invblid.
        }
        
        // invblid or private, exit
        if(!NetworkUtils.isVblidAddress(ia) ||
            NetworkUtils.isPrivbteAddress(ia))
            return;
            
        // If we're forcing, chbnge that if necessary.
        if( ConnectionSettings.FORCE_IP_ADDRESS.getVblue() ) {
            StringSetting bddr = ConnectionSettings.FORCED_IP_ADDRESS_STRING;
            if(!v.equbls(addr.getValue())) {
                bddr.setValue(v);
                RouterService.bddressChanged();
            }
        }
        // Otherwise, if our current bddress is invalid, change.
        else if(!NetworkUtils.isVblidAddress(RouterService.getAddress())) {
            // will buto-call addressChanged.
            RouterService.getAcceptor().setAddress(ib);
        }
        
        RouterService.getAcceptor().setExternblAddress(ia);
    }
            

    /**
     * Writes s to out, with no trbiling linefeeds.  Called only from
     * initiblize().  
     *    @requires _socket, _out bre properly set up */
    privbte void writeLine(String s) throws IOException {
        if(s == null || s.equbls("")) {
            throw new NullPointerException("null or empty string: "+s);
        }

        //TODO: chbracter encodings?
        byte[] bytes=s.getBytes();
		BbndwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(bytes.length);
        _out.write(bytes);
        _out.flush();
    }
    
    /**
     * Rebds and returns one line from the network.  A line is defined as a
     * mbximal sequence of characters without '\n', with '\r''s removed.  If the
     * chbracters cannot be read within TIMEOUT milliseconds (as defined by the
     * property mbnager), throws IOException.  This includes EOF.
     * @return The line of chbracters read
     * @requires _socket is properly set up
     * @modifies network
     * @exception IOException if the chbracters cannot be read within 
     * the specified timeout
     */
    privbte String readLine() throws IOException {
        return rebdLine(Constants.TIMEOUT);
    }

    /**
     * Rebds and returns one line from the network.  A line is defined as a
     * mbximal sequence of characters without '\n', with '\r''s removed.  If the
     * chbracters cannot be read within the specified timeout milliseconds,
     * throws IOException.  This includes EOF.
     * @pbram timeout The time to wait on the socket to read data before 
     * IOException is thrown
     * @return The line of chbracters read
     * @requires _socket is properly set up
     * @modifies network
     * @exception IOException if the chbracters cannot be read within 
     * the specified timeout
     */
    privbte String readLine(int timeout) throws IOException {
        int oldTimeout=_socket.getSoTimeout();
        // _in.rebd can throw an NPE if we closed the connection,
        // so we must cbtch NPE and throw the CONNECTION_CLOSED.
        try {
            _socket.setSoTimeout(timeout);
            String line=(new ByteRebder(_in)).readLine();
            if (line==null)
                throw new IOException("rebd null line");
            BbndwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(line.length());
            return line;
        } cbtch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        } finblly {
            //Restore socket timeout.
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Returns the strebm to use for writing to s.
     * If the messbge supports asynchronous messaging, we don't need
     * to buffer it, becbuse it's already buffered internally.  Note, however,
     * thbt buffering it would not be wrong, because we can always flush
     * the buffered dbta.
     */
    protected OutputStrebm getOutputStream() throws IOException {
        if(isAsynchronous())
            return _socket.getOutputStrebm();
        else
            return new BufferedOutputStrebm(_socket.getOutputStream());
    }

    /**
     * Returns the strebm to use for reading from s.
     * If this supports bsynchronous messaging, the stream itself is returned,
     * becbuse the underlying stream is already buffered.  This is also done
     * to ensure thbt when we switch to using asynch message processing, no
     * bytes bre left within the BufferedInputStream's buffer.
     *
     * Otherwise (it isn't bsynchronous-capable), we enforce a buffer around the stream.
     *
     * Subclbsses may override to decorate the stream.
     */
    protected InputStrebm getInputStream() throws IOException {
        if(isAsynchronous())
            return _socket.getInputStrebm();
        else
            return new BufferedInputStrebm(_socket.getInputStream());
    }    
    
    

    /////////////////////////////////////////////////////////////////////////

    /**
     * Used to determine whether the connection is incoming or outgoing.
     */
    public boolebn isOutgoing() {
        return OUTGOING;
    }

    /** A tiny bllocation optimization; see Message.read(InputStream,byte[]). */
    privbte final byte[] HEADER_BUF=new byte[23];

    /**
     * Receives b message.  This method is NOT thread-safe.  Behavior is
     * undefined if two threbds are in a receive call at the same time for a
     * given connection.
     *
     * If this is bn asynchronous read-deflated connection, this will set up
     * the UncompressingInputStrebm the first time this is called.
     *
     * @requires this is fully initiblized
     * @effects exbctly like Message.read(), but blocks until a
     *  messbge is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    protected Messbge receive() throws IOException, BadPacketException {
        if(isAsynchronous() && isRebdDeflated() && !(_in instanceof UncompressingInputStream))
            _in = new UncompressingInputStrebm(_in, _inflater);
        
        //On the Mbcintosh, sockets *appear* to return the same ping reply
        //repebtedly if the connection has been closed remotely.  This prevents
        //connections from dying.  The following works bround the problem.  Note
        //thbt Message.read may still throw IOException below.
        //See note on _closed for more informbtion.
        if (_closed)
            throw CONNECTION_CLOSED;

        Messbge m = null;
        while (m == null) {
            m = rebdAndUpdateStatistics();
        }
        return m;
    }

    /**
     * Receives b message with timeout.  This method is NOT thread-safe.
     * Behbvior is undefined if two threads are in a receive call at the same
     * time for b given connection.
     *
     * If this is bn asynchronous read-deflated connection, this will set up
     * the UncompressingInputStrebm the first time this is called.
     *
     * @requires this is fully initiblized
     * @effects exbctly like Message.read(), but throws InterruptedIOException
     *  if timeout!=0 bnd no message is read after "timeout" milliseconds.  In
     *  this cbse, you should terminate the connection, as half a message may
     *  hbve been read.
     */
    public Messbge receive(int timeout)
		throws IOException, BbdPacketException, InterruptedIOException {
        if(isAsynchronous() && isRebdDeflated() && !(_in instanceof UncompressingInputStream))
            _in = new UncompressingInputStrebm(_in, _inflater);
		    
        //See note in receive().
        if (_closed)
            throw CONNECTION_CLOSED;

        //temporbrily change socket timeout.
        int oldTimeout=_socket.getSoTimeout();
        _socket.setSoTimeout(timeout);
        try {
            Messbge m = readAndUpdateStatistics();
            if (m==null) {
                throw new InterruptedIOException("null messbge read");
            }
            return m;
        } finblly {
            _socket.setSoTimeout(oldTimeout);
        }
    }
    
    /**
     * Rebds a message from the network and updates the appropriate statistics.
     */
    privbte Message readAndUpdateStatistics()
      throws IOException, BbdPacketException {
        
        // The try/cbtch block is necessary for two reasons...
        // See the notes in Connection.close bbove the calls
        // to end() on the Inflbter/Deflater and close()
        // on the Input/OutputStrebms for the details.
        Messbge msg = Message.read(_in, HEADER_BUF, Message.N_TCP, _softMax);
        updbteReadStatistics(msg);
        return msg;
    }
    
    /**
     * Updbtes the read statistics.
     */
    protected void updbteReadStatistics(Message msg) throws IOException {
            // _bytesReceived must be set differently
            // when compressed becbuse the inflater will
            // rebd more input than a single message,
            // mbking it appear as if the deflated input
            // wbs actually larger.
        if( isRebdDeflated() ) {
            try {
                long newIn  = _inflbter.getTotalIn();
                long newOut = _inflbter.getTotalOut();
                CompressionStbt.GNUTELLA_UNCOMPRESSED_DOWNSTREAM.addData((int)(newOut - _bytesReceived));
                CompressionStbt.GNUTELLA_COMPRESSED_DOWNSTREAM.addData((int)(newIn - _compressedBytesReceived));
                _compressedBytesReceived = newIn;
                _bytesReceived = newOut;
            } cbtch(NullPointerException npe) {
                // Inflbter is broken and can throw an NPE if it was ended
                // bt an odd time.
                throw CONNECTION_CLOSED;
            }
        } else if(msg != null) {
            _bytesReceived += msg.getTotblLength();
        }
    }
    
    /**
     * Optimizbtion -- reuse the header buffer since sending will only be
     * done on one threbd.
     */
    privbte final byte[] OUT_HEADER_BUF = new byte[23];

    /**
     * Sends b message.  The message may be buffered, so call flush() to
     * gubrantee that the message is sent synchronously.  This method is NOT
     * threbd-safe. Behavior is undefined if two threads are in a send call
     * bt the same time for a given connection.
     *
     * @requires this is fully initiblized
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   brise.
     */
    public void send(Messbge m) throws IOException {
        if(LOG.isTrbceEnabled())
            LOG.trbce("Connection (" + toString() + 
                      ") is sending messbge: " + m);
        
        // The try/cbtch block is necessary for two reasons...
        // See the notes in Connection.close bbove the calls
        // to end() on the Inflbter/Deflater and close()
        // on the Input/OutputStrebms for the details.        
        try {
            m.write(_out, OUT_HEADER_BUF);
            updbteWriteStatistics(m);
        } cbtch(NullPointerException e) {
            throw CONNECTION_CLOSED;
        }
    }

    /**
     * Flushes bny buffered messages sent through the send method.
     */
    public void flush() throws IOException {
        // The try/cbtch block is necessary for two reasons...
        // See the notes in Connection.close bbove the calls
        // to end() on the Inflbter/Deflater and close()
        // on the Input/OutputStrebms for the details.
        try { 
            _out.flush();
            // we must updbte the write statistics again,
            // becbuse flushing forces the deflater to deflate.
            updbteWriteStatistics(null);
        } cbtch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        }
    }
    
    /**
     * Updbtes the write statistics.
     * @pbram m the possibly null message to add to the bytes sent
     */
    protected void updbteWriteStatistics(Message m) {
        if( isWriteDeflbted() ) {
            long newIn  = _deflbter.getTotalIn();
            long newOut = _deflbter.getTotalOut();
            CompressionStbt.GNUTELLA_UNCOMPRESSED_UPSTREAM.addData((int)(newIn - _bytesSent));
            CompressionStbt.GNUTELLA_COMPRESSED_UPSTREAM.addData((int)(newOut - _compressedBytesSent));
            _bytesSent = newIn;
            _compressedBytesSent = newOut;
        } else if( m != null) {
            _bytesSent += m.getTotblLength();
        }
    }    
    
    /**
     * Returns the number of bytes sent on this connection.
     * If the outgoing strebm is compressed, the return value indicates
     * the compressed number of bytes sent.
     */
    public long getBytesSent() {
        if(isWriteDeflbted())
            return _compressedBytesSent;
        else            
            return _bytesSent;
    }
    
    /**
     * Returns the number of uncompressed bytes sent on this connection.
     * If the outgoing strebm is not compressed, this is effectively the same
     * bs calling getBytesSent()
     */
    public long getUncompressedBytesSent() {
        return _bytesSent;
    }
    
    /** 
     * Returns the number of bytes received on this connection.
     * If the incoming strebm is compressed, the return value indicates
     * the number of compressed bytes received.
     */
    public long getBytesReceived() {
        if(isRebdDeflated())
            return _compressedBytesReceived;
        else
            return _bytesReceived;
    }
    
    /**
     * Returns the number of uncompressed bytes rebd on this connection.
     * If the incoming strebm is not compressed, this is effectively the same
     * bs calling getBytesReceived()
     */
    public long getUncompressedBytesReceived() {
        return _bytesReceived;
    }
    
    /**
     * Returns the percentbge saved through compressing the outgoing data.
     * The vblue may be slightly off until the output stream is flushed,
     * becbuse the value of the compressed bytes is not calculated until
     * then.
     */
    public flobt getSentSavedFromCompression() {
        if( !isWriteDeflbted() || _bytesSent == 0 ) return 0;
        return 1-((flobt)_compressedBytesSent/(float)_bytesSent);
    }
    
    /**
     * Returns the percentbge saved from having the incoming data compressed.
     */
    public flobt getReadSavedFromCompression() {
        if( !isRebdDeflated() || _bytesReceived == 0 ) return 0;
        return 1-((flobt)_compressedBytesReceived/(float)_bytesReceived);
    }

   /**
    * Returns the IP bddress of the remote host as a string.
    * 
    * @return the IP bddress of the remote host as a string
    */
    public String getAddress() {
        return _host;
    }

    /**
     * Accessor for the port number this connection is listening on.  Note thbt 
     * this is NOT the port of the socket itself.  For incoming connections,  
     * the getPort method of the jbva.net.Socket class returns the ephemeral 
     * port thbt the host connected with.  This port, however, is the port the 
     * remote host is listening on for new connections, which we set using 
     * Gnutellb connection headers in the case of incoming connections.  For 
     * outgoing connections, this is the port we used to connect to them -- 
     * their listening port.
     * 
     * @return the listening port for the remote host
     */
    public int getPort() {
        return _port;
    }
    
    /** 
     * Sets the port where the conected node listens bt, not the one
     * got from socket
     */
    void setListeningPort(int port){
        if (!NetworkUtils.isVblidPort(port))
            throw new IllegblArgumentException("invalid port: "+port);
        this._port = port;
    }


    /**
     * Returns the bddress of the foreign host this is connected to.
     * @exception IllegblStateException this is not initialized
     */
    public InetAddress getInetAddress() throws IllegblStateException {
		if(_socket == null) {
			throw new IllegblStateException("Not initialized");
		}
		return _socket.getInetAddress();
    }
    
    /**
     * Accessor for the <tt>Socket</tt> for this connection.
     * 
     * @return the <tt>Socket</tt> for this connection
     * @throws IllegblStateException if this connection is not yet
     *  initiblized
     */
    public Socket getSocket() throws IllegblStateException {
        if(_socket == null) {
            throw new IllegblStateException("Not initialized");
        }
        return _socket;        
    }

    /**
     * Returns the time this connection wbs established, in milliseconds
     * since Jbnuary 1, 1970.
     *
     * @return the time this connection wbs established
     */
    public long getConnectionTime() {
        return _connectionTime;
    }
    
    /**
     * Accessor for the soft mbx TTL to use for this connection.
     * 
     * @return the soft mbx TTL for this connection
     */
    public byte getSoftMbx() {
        return _softMbx;
    }
    
    /**
     * Checks whether this connection is considered b stable connection,
     * mebning it has been up for enough time to be considered stable.
     *
     * @return <tt>true</tt> if the connection is considered stbble,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn isStable() {
        return isStbble(System.currentTimeMillis());
    }

    /**
     * Checks whether this connection is considered b stable connection,
     * by compbring the time it was established with the <tt>millis</tt>
     * brgument.
     *
     * @return <tt>true</tt> if the connection is considered stbble,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn isStable(long millis) {
        return (millis - getConnectionTime())/1000 > 5;
    }

    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int supportsVendorMessbge(byte[] vendorID, int selector) {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsMessage(vendorID, selector);
        return -1;
    }
    
    /**
     * @return whether this connection supports routing of vendor messbges
     * (i.e. will not drop b VM that has ttl <> 1 and hops > 0)
     */
    public boolebn supportsVMRouting() {
        if (_hebders != null)
            return _hebders.supportsVendorMessages() >= 0.2;
        return fblse;
    }
    
    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsUDPConnectBbck() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsUDPConnectBack();
        return -1;
    }
    
    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsTCPConnectBbck() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsTCPConnectBack();
        return -1;
    }
    
    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsUDPRedirect() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsUDPConnectBackRedirect();
        return -1;
    }
    
    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsTCPRedirect() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsTCPConnectBackRedirect();
        return -1;
    }
    
    /** @return -1 if UDP crbwling is supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsUDPCrbwling() {
    	if (_messbgesSupported != null)
    		return _messbgesSupported.supportsUDPCrawling();
    	return -1;
    }
    
    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsHopsFlow() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsHopsFlow();
        return -1;
    }

    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsPushProxy() {
        if ((_messbgesSupported != null) && isClientSupernodeConnection())
            return _messbgesSupported.supportsPushProxy();
        return -1;
    }

    /** @return -1 if the messbge isn't supported, else the version number 
     *  supported.
     */
    public int remoteHostSupportsLebfGuidance() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsLeafGuidance();
        return -1;
    }
    
    public int remoteHostSupportsHebderUpdate() {
        if (_messbgesSupported != null)
            return _messbgesSupported.supportsHeaderUpdate();
        return -1;
    }
    
    /**
     * Return whether or not the remote host supports febture queries.
     */
    public boolebn getRemoteHostSupportsFeatureQueries() {
        if(_cbpabilities != null)
            return _cbpabilities.supportsFeatureQueries() > 0;
        return fblse;
    }

    /** @return the mbximum selector of capability supported, else -1 if no
     *  support.
     */
    public int getRemoteHostFebtureQuerySelector() {
        if (_cbpabilities != null)
            return _cbpabilities.supportsFeatureQueries();
        return -1;
    }

    /** @return true if the cbpability is supported.
     */
    public boolebn remoteHostSupportsWhatIsNew() {
        if (_cbpabilities != null)
            return _cbpabilities.supportsWhatIsNew();
        return fblse;
    }
    
    /**
     * Gets the remote host's 'updbte' version.
     */
    public int getRemoteHostUpdbteVersion() {
        if(_cbpabilities != null)
            return _cbpabilities.supportsUpdate();
        else
            return -1;
    }

    /**
     * Returns whether or not this connection represents b local address.
     *
     * @return <tt>true</tt> if this connection is b local address,
     *  otherwise <tt>fblse</tt>
     */
    protected boolebn isLocal() {
        return NetworkUtils.isLocblAddress(_socket.getInetAddress());
    }

    /**
     * Returns the vblue of the given outgoing (written) connection property, or
     * null if no such property.  For exbmple, getProperty("X-Supernode") tells
     * whether I bm a supernode or a leaf node.  If I wrote a property multiple
     * time during connection, returns the lbtest.
     */
    public String getPropertyWritten(String nbme) {
        return HEADERS_WRITTEN.getProperty(nbme);
    }

    /**
     * @return true until close() is cblled on this Connection
     */
    public boolebn isOpen() {
        return !_closed;
    }

    /**
     *  Closes the Connection's socket bnd thus the connection itself.
     */
    public void close() {
        if(_closed)
            return;
        
        // Setting this flbg insures that the socket is closed if this
        // method is cblled asynchronously before the socket is initialized.
        _closed = true;
        if(_socket != null) {
            try {				
                _socket.close();
            } cbtch(IOException e) {}
        }
        
        // tell the inflbter & deflater that we're done with them.
        // These cblls are dangerous, because we don't know that the
        // strebm isn't currently deflating or inflating, and the access
        // to the deflbter/inflater is not synchronized (it shouldn't be).
        // This cbn lead to NPE's popping up in unexpected places.
        // Fortunbtely, the calls aren't explicitly necessary because
        // when the deflbter/inflaters are garbage-collected they will call
        // end for us.
        if( _deflbter != null )
            _deflbter.end();
        if( _inflbter != null )
            _inflbter.end();
        
       // closing _in (bnd possibly _out too) can cause NPE's
       // in Messbge.read (and possibly other places),
       // becbuse BufferedInputStream can't handle
       // the cbse where one thread is reading from the stream and
       // bnother closes it.
       // See BugPbrade ID: 4505257
       
       if (_in != null) {
           try {
               _in.close();
           } cbtch (IOException e) {}
       }
       if (_out != null) {
           try {
               _out.close();
           } cbtch (IOException e) {}
       }
    }

    
    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wbsn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
		return _hebders.getUserAgent();
    }
    
    /**
     * Returns whether or not the remote host is b LimeWire (or derivative)
     */
    public boolebn isLimeWire() {
        return _hebders.isLimeWire();
    }
    
    public boolebn isOldLimeWire() {
        return _hebders.isOldLimeWire();
    }

    /**
     * Returns true if the outgoing strebm is deflated.
     *
     * @return true if the outgoing strebm is deflated.
     */
    public boolebn isWriteDeflated() {
        return _hebdersWritten.isDeflateEnabled();
    }
    
    /**
     * Returns true if the incoming strebm is deflated.
     *
     * @return true if the incoming strebm is deflated.
     */
    public boolebn isReadDeflated() {
        return _hebders.isDeflateEnabled();
    }

    // inherit doc comment
    public boolebn isGoodUltrapeer() {
        return _hebders.isGoodUltrapeer();
    }

    // inherit doc comment
    public boolebn isGoodLeaf() {
        return _hebders.isGoodLeaf();
    }

    // inherit doc comment
    public boolebn supportsPongCaching() {
        return _hebders.supportsPongCaching();
    }

    /**
     * Returns whether or not we should bllow new pings on this connection.  If
     * we hbve recently received a ping, we will likely not allow the second
     * ping to go through to bvoid flooding the network with ping traffic.
     *
     * @return <tt>true</tt> if new pings bre allowed along this connection,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn allowNewPings() {
    	synchronized(PING_LOCK) {
	        long curTime = System.currentTimeMillis();
			
			// don't bllow new pings if the connection could drop any second
			if(!isStbble(curTime)) return false;
	        if(curTime < _nextPingTime) {
	            return fblse;
	        } 
			_nextPingTime = System.currentTimeMillis() + 2500;
	        return true;
    	}
    }


    /**
     * Returns whether or not we should bllow new pongs on this connection.  If
     * we hbve recently received a pong, we will likely not allow the second
     * pong to go through to bvoid flooding the network with pong traffic.
     * In prbctice, this is only used to limit pongs sent to leaves.
     *
     * @return <tt>true</tt> if new pongs bre allowed along this connection,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn allowNewPongs() {
    	synchronized(PONG_LOCK) {
		    long curTime = System.currentTimeMillis();

			// don't bllow new pongs if the connection could drop any second
			if(!isStbble(curTime)) return false;
		    if(curTime < _nextPongTime) {
		        return fblse;
		    } 
		    
			int intervbl;
		
			// if the connection is young, give it b lot of pongs, otherwise
			// be more conservbtive
			if(curTime - getConnectionTime() < 10000) {
				intervbl = 300;
			} else {
				intervbl = 12000;
			}
			_nextPongTime = curTime + intervbl;
					
		    return true;
    	}
    }


	/**
	 * Returns the number of intrb-Ultrapeer connections this node maintains.
	 * 
	 * @return the number of intrb-Ultrapeer connections this node maintains
	 */
	public int getNumIntrbUltrapeerConnections() {
		return _hebders.getNumIntraUltrapeerConnections();
	}

	// implements ReplyHbndler interface -- inherit doc comment
	public boolebn isHighDegreeConnection() {
		return _hebders.isHighDegreeConnection();
	}

	/**
	 * Returns whether or not this connection is to bn Ultrapeer that 
	 * supports query routing between Ultrbpeers at 1 hop.
	 *
	 * @return <tt>true</tt> if this is bn Ultrapeer connection that
	 *  exchbnges query routing tables with other Ultrapeers at 1 hop,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isUltrapeerQueryRoutingConnection() {
		return _hebders.isUltrapeerQueryRoutingConnection();
    }

    /**
     * Returns whether or not this connections supports "probe" queries,
     * or queries sent bt TTL=1 that should not block the send path
     * of subsequent, higher TTL queries.
     *
     * @return <tt>true</tt> if this connection supports probe queries,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn supportsProbeQueries() {
        return _hebders.supportsProbeQueries();
    }

    /**
     * Accessor for whether or not this connection hbs received any
     * hebders.
     *
     * @return <tt>true</tt> if this connection hbs finished initializing
     *  bnd therefore has headers, otherwise <tt>false</tt>
     */
    public boolebn receivedHeaders() {
        return _hebders != null;
    }

	/**
	 * Accessor for the <tt>HbndshakeResponse</tt> instance containing all
	 * of the Gnutellb connection headers passed by this node.
	 *
	 * @return the <tt>HbndshakeResponse</tt> instance containing all of
	 *  the Gnutellb connection headers passed by this node
	 */
	public HbndshakeResponse headers() {
		return _hebders;
	}
	
	/**
	 * Accessor for the LimeWire version reported in the connection hebders
	 * for this node.	 
	 */
	public String getVersion() {
		return _hebders.getVersion();
	}

    /** Returns true iff this connection wrote "Ultrbpeer: false".
     *  This does NOT necessbrily mean the connection is shielded. */
    public boolebn isLeafConnection() {
		return _hebders.isLeaf();
    }

    /** Returns true iff this connection wrote "Supernode: true". */
    public boolebn isSupernodeConnection() {
		return _hebders.isUltrapeer();
    }

    /** 
	 * Returns true iff the connection is bn Ultrapeer and I am a leaf, i.e., 
     * if I wrote "X-Ultrbpeer: false", this connection wrote 
	 * "X-Ultrbpeer: true" (not necessarily in that order).  <b>Does 
	 * NOT require thbt QRP is enabled</b> between the two; the Ultrapeer 
	 * could be using reflector indexing, for exbmple. 
	 */
    public boolebn isClientSupernodeConnection() {
        //Is remote host b supernode...
        if (! isSupernodeConnection())
            return fblse;

        //...bnd am I a leaf node?
        String vblue=getPropertyWritten(HeaderNames.X_ULTRAPEER);
        if (vblue==null)
            return fblse;
        else 
            return !Boolebn.valueOf(value).booleanValue();
			
    }
    
    /** 
	 * Returns true iff the connection is bn Ultrapeer and I am a Ultrapeer,
     * ie: if I wrote "X-Ultrbpeer: true", this connection wrote 
	 * "X-Ultrbpeer: true" (not necessarily in that order).  <b>Does 
	 * NOT require thbt QRP is enabled</b> between the two; the Ultrapeer 
	 * could be using reflector indexing, for exbmple. 
	 */
    public boolebn isSupernodeSupernodeConnection() {
        //Is remote host b supernode...
        if (! isSupernodeConnection())
            return fblse;

        //...bnd am I a leaf node?
        String vblue=getPropertyWritten(HeaderNames.X_ULTRAPEER);
        if (vblue==null)
            return fblse;
        else 
            return Boolebn.valueOf(value).booleanValue();
			
    }    


	/**
	 * Returns whether or not this connection is to b client supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  connection supports GUESS, <tt>fblse</tt> otherwise
	 */
	public boolebn isGUESSCapable() {
		return _hebders.isGUESSCapable();
	}

	/**
	 * Returns whether or not this connection is to b ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrbpeer connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolebn isGUESSUltrapeer() {
		return _hebders.isGUESSUltrapeer();
	}


    /** Returns true iff this connection is b temporary connection as per
     the hebders. */
    public boolebn isTempConnection() {
		return _hebders.isTempConnection();
    }
    
    /** Returns true iff I bm a supernode shielding the given connection, i.e.,
     *  if I wrote "X-Ultrbpeer: true" and this connection wrote 
	 *  "X-Ultrbpeer: false, and <b>both support query routing</b>. */
    public boolebn isSupernodeClientConnection() {
        //Is remote host b supernode...
        if (! isLebfConnection())
            return fblse;

        //...bnd am I a supernode?
        String vblue=getPropertyWritten(
            HebderNames.X_ULTRAPEER);
        if (vblue==null)
            return fblse;
        else if (!Boolebn.valueOf(value).booleanValue())
            return fblse;

        //...bnd do both support QRP?
        return isQueryRoutingEnbbled();
    }

    /** Returns true if this supports GGEP'ed messbges.  GGEP'ed messages (e.g.,
     *  big pongs) should only be sent blong connections for which
     *  supportsGGEP()==true. */
    public boolebn supportsGGEP() {
		return _hebders.supportsGGEP();
    }

    /**
     * Sends the StbtisticVendorMessage down the connection
     */
    public void hbndleStatisticVM(StatisticVendorMessage svm) 
                                                            throws IOException {
        send(svm);
    }

    /**
     * Sends the SimppVM down the connection
     */
    public void hbndleSimppVM(SimppVM simppVM) throws IOException {
        send(simppVM);
    }


    /** True if the remote host supports query routing (QRP).  This is only 
     *  mebningful in the context of leaf-ultrapeer relationships. */
    boolebn isQueryRoutingEnabled() {
		return _hebders.isQueryRoutingEnabled();
    }

    // overrides Object.toString
    public String toString() {
        return "CONNECTION: host=" + _host  + " port=" + _port;
    }
    
    /**
     * bccess the locale pref. of the connected servent
     */
    public String getLocblePref() {
        return _hebders.getLocalePref();
    }

    // Technicblly, a Connection object can be equal in various ways...
    // Connections cbn be said to be equal if the pipe the information is
    // trbvelling through is the same.
    // Or they cbn be equal if the remote host is the same, even if the
    // two connections bre on different channels.
    // Ideblly, our equals method would use the second option, however
    // this hbs problems with tests because of the setup of various
    // tests, connecting multiple connection objects to b central
    // testing Ultrbpeer, uncorrectly labelling each connection
    // bs equal.
    // Using pipe equblity (by socket) also fails because
    // the socket doesn't exist for outgoing connections until
    // the connection is estbblished, but the equals method is used
    // before then.
    // Until necessbry, the equals & hashCode methods are therefore
    // commented out bnd sameness equality is being used.
    
//	public boolebn equals(Object o) {
//      return super.equbls(o);
//	}
//	

//	public int hbshCode() {
//      return super.hbshCode();
//	}
    
    
    /////////////////////////// Unit Tests  ///////////////////////////////////
    
//      /** Unit test */
//      public stbtic void main(String args[]) {
//          Assert.thbt(! notLessThan06("CONNECT"));
//          Assert.thbt(! notLessThan06("CONNECT/0.4"));
//          Assert.thbt(! notLessThan06("CONNECT/0.599"));
//          Assert.thbt(! notLessThan06("CONNECT/XP"));
//          Assert.thbt(notLessThan06("CONNECT/0.6"));
//          Assert.thbt(notLessThan06("CONNECT/0.7"));
//          Assert.thbt(notLessThan06("GNUTELLA CONNECT/1.0"));

//          finbl Properties props=new Properties();
//          props.setProperty("Query-Routing", "0.3");        
//          HbndshakeResponder standardResponder=new HandshakeResponder() {
//              public HbndshakeResponse respond(HandshakeResponse response,
//                                               boolebn outgoing) {
//                  return new HbndshakeResponse(props);
//              }
//          };        
//          HbndshakeResponder secretResponder=new HandshakeResponder() {
//              public HbndshakeResponse respond(HandshakeResponse response,
//                                               boolebn outgoing) {
//                  Properties props2=new Properties();
//                  props2.setProperty("Secret", "bbcdefg");
//                  return new HbndshakeResponse(props2);
//              }
//          };
//          ConnectionPbir p=null;

//          //1. 0.4 => 0.4
//          p=connect(null, null, null);
//          Assert.thbt(p!=null);
//          Assert.thbt(p.in.getProperty("Query-Routing")==null);
//          Assert.thbt(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //2. 0.6 => 0.6
//          p=connect(stbndardResponder, props, secretResponder);
//          Assert.thbt(p!=null);
//          Assert.thbt(p.in.getProperty("Query-Routing").equals("0.3"));
//          Assert.thbt(p.out.getProperty("Query-Routing").equals("0.3"));
//          Assert.thbt(p.out.getProperty("Secret")==null);
//          Assert.thbt(p.in.getProperty("Secret").equals("abcdefg"));
//          disconnect(p);

//          //3. 0.4 => 0.6 (Incoming doesn't send properties)
//          p=connect(stbndardResponder, null, null);
//          Assert.thbt(p!=null);
//          Assert.thbt(p.in.getProperty("Query-Routing")==null);
//          Assert.thbt(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //4. 0.6 => 0.4 (If the receiving connection were Gnutellb 0.4, this
//          //wouldn't work.  But the new guy will butomatically upgrade to 0.6.)
//          p=connect(null, props, stbndardResponder);
//          Assert.thbt(p!=null);
//          //Assert.thbt(p.in.getProperty("Query-Routing")==null);
//          Assert.thbt(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //5.
//          System.out.println("-Testing IOException rebding from closed socket");
//          p=connect(null, null, null);
//          Assert.thbt(p!=null);
//          p.in.close();
//          try {
//              p.out.receive();
//              Assert.thbt(false);
//          } cbtch (BadPacketException failed) {
//              Assert.thbt(false);
//          } cbtch (IOException pass) {
//          }

//          //6.
//          System.out.println("-Testing IOException writing to closed socket");
//          p=connect(null, null, null);
//          Assert.thbt(p!=null);
//          p.in.close();
//          try { Threbd.sleep(2000); } catch (InterruptedException e) { }
//          try {
//              //You'd think thbt only one write is needed to get IOException.
//              //Thbt doesn't seem to be the case, and I'm not 100% sure why.  It
//              //hbs something to do with TCP half-close state.  Anyway, this
//              //slightly webker test is good enough.
//              p.out.send(new QueryRequest((byte)3, 0, "lbs"));
//              p.out.flush();
//              p.out.send(new QueryRequest((byte)3, 0, "lbs"));
//              p.out.flush();
//              Assert.thbt(false);
//          } cbtch (IOException pass) {
//          }

//          //7.
//          System.out.println("-Testing connect with timeout");
//          Connection c=new Connection("this-host-does-not-exist.limewire.com", 6346);
//          int TIMEOUT=1000;
//          long stbrt=System.currentTimeMillis();
//          try {
//              c.initiblize(TIMEOUT);
//              Assert.thbt(false);
//          } cbtch (IOException e) {
//              //Check thbt exception happened quickly.  Note fudge factor below.
//              long elbpsed=System.currentTimeMillis()-start;  
//              Assert.thbt(elapsed<(3*TIMEOUT)/2, "Took too long to connect: "+elapsed);
//          }
//      }   

//      privbte static class ConnectionPair {
//          Connection in;
//          Connection out;
//      }

//      privbte static ConnectionPair connect(HandshakeResponder inProperties,
//                                            Properties outRequestHebders,
//                                            HbndshakeResponder outProperties2) {
//          ConnectionPbir ret=new ConnectionPair();
//          com.limegroup.gnutellb.tests.MiniAcceptor acceptor=
//              new com.limegroup.gnutellb.tests.MiniAcceptor(inProperties);
//          try {
//              ret.out=new Connection("locblhost", 6346,
//                                     outRequestHebders, outProperties2,
//                                     true);
//              ret.out.initiblize();
//          } cbtch (IOException e) { }
//          ret.in=bcceptor.accept();
//          if (ret.in==null || ret.out==null)
//              return null;
//          else
//              return ret;
//      }

//      privbte static void disconnect(ConnectionPair cp) {
//          if (cp.in!=null)
//              cp.in.close();
//          if (cp.out!=null)
//              cp.out.close();
//      }    
}
