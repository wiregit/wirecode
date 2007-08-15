package com.limegroup.gnutella;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inspection.Inspectable;
import org.limewire.io.CompressingOutputStream;
import org.limewire.io.Connectable;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.io.Pools;
import org.limewire.io.UncompressingInputStream;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.ssl.SSLBandwidthTracker;
import org.limewire.nio.ssl.SSLUtils;

import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.BlockingIncomingHandshaker;
import com.limegroup.gnutella.handshaking.BlockingOutgoingHandshaker;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.Handshaker;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.CompressionStat;
import com.limegroup.gnutella.statistics.ConnectionStat;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

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
public class Connection implements IpPort, Inspectable, Connectable {
    
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
    private final ConnectType _connectType;
    private final String _host;
    private int _port;
    protected volatile Socket _socket;
    private volatile InputStream _in;
    private volatile OutputStream _out;
    private final boolean OUTGOING;
    
    /** The bandwidth tracker managing the stats for the SSL connection. */
    private volatile SSLBandwidthTracker _sslTracker;
    
    /**
     * The Inflater to use for inflating read streams, initialized
     * in initialize() if the connection told us it's sending with
     * a Content-Encoding of deflate.
     * Definitions:
     *   Inflater.getTotalOut -- The number of UNCOMPRESSED bytes
     *   Inflater.getTotalIn  -- The number of COMPRESSED bytes
     */
    protected volatile Inflater _inflater;
    
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
    protected volatile Deflater _deflater;
    
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
     * This also protects us from calling methods on the Inflater/Deflater
     * objects after end() has been called on them.
     */
    private final AtomicBoolean _closed = new AtomicBoolean(false);

    /** The <tt>HandshakeResponse</tt> wrapper for all headers we read from the remote side. */
    private HandshakeResponse _headersRead = HandshakeResponse.createEmptyResponse();
        
    /** The <tt>HandshakeResponse</tt> wrapper for all headers we wrote to the remote side. */
	private HandshakeResponse _headersWritten = HandshakeResponse.createEmptyResponse();

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

    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final SocketsManager socketsManager;
    private final Acceptor acceptor;
    private final MessagesSupportedVendorMessage supportedVendorMessage;
    /**
     * Cache the 'connection closed' exception, so we have to allocate
     * one for every closed connection.
     */
    protected static final IOException CONNECTION_CLOSED =
        new IOException("connection closed");

    private final MessageFactory messageFactory;

    /**
     * Creates an uninitialized outgoing Gnutella 0.6 connection with the
     * desired outgoing properties that may use TLS.
     *
     * @param host the name of the host to connect to
     * @param port the port of the remote host
     * @param connectType the type of connection that should be made
     * @throws <tt>NullPointerException</tt> if any of the arguments are
     *  <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    Connection(String host, int port, ConnectType connectType, 
            CapabilitiesVMFactory capabilitiesVMFactory,
            SocketsManager socketsManager, Acceptor acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
        if(host == null)
			throw new NullPointerException("null host");
		if(!NetworkUtils.isValidPort(port))
			throw new IllegalArgumentException("illegal port: "+port);

        _host = host;
        _port = port;
        OUTGOING = true;
        _connectType = connectType;
        _sslTracker = SSLUtils.EmptyTracker.instance(); // default to an empty tracker to avoid NPEs
		ConnectionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
		this.capabilitiesVMFactory = capabilitiesVMFactory;
		this.socketsManager = socketsManager;
		this.acceptor = acceptor;
		this.supportedVendorMessage = supportedVendorMessage;
    }

    /**
     * Creates an uninitialized incoming 0.6 Gnutella connection. If the
	 * client is attempting to connect using an 0.4 handshake, it is
	 * rejected.
     * 
     * @param socket the socket accepted by a ServerSocket.  The word
     *  "GNUTELLA " and nothing else must have been read from the socket.
     * @param responder the headers to be sent in response to the client's 
	 *  "GNUTELLA CONNECT".  
	 * @throws <tt>NullPointerException</tt> if any of the arguments are
	 *  <tt>null</tt>
     */
    Connection(Socket socket, CapabilitiesVMFactory capabilitiesVMFactory,
            Acceptor acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory) {
		if(socket == null)
			throw new NullPointerException("null socket");
        
        //Get the address in dotted-quad format.  It's important not to do a
        //reverse DNS lookup here, as that can block.  And on the Mac, it blocks
        //your entire system!
        _host = socket.getInetAddress().getHostAddress();
        _port = socket.getPort();
        _socket = socket;
        OUTGOING = false;
        _connectType = SSLUtils.isTLSEnabled(socket) ? ConnectType.TLS : ConnectType.PLAIN;
        _sslTracker = SSLUtils.getSSLBandwidthTracker(socket);
		ConnectionStat.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
		this.capabilitiesVMFactory = capabilitiesVMFactory;
		this.socketsManager = null;
		this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.messageFactory = messageFactory;
    }


    /** Call this method when the Connection has been initialized and accepted
     *  as 'long-lived'.
     */
    protected void postInit() {
        try { // TASK 1 - Send a MessagesSupportedVendorMessage if necessary....
			if(_headersRead.supportsVendorMessages() > 0) {
                send(supportedVendorMessage);
                send(capabilitiesVMFactory.getCapabilitiesVM());
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
            if(_headersRead.supportsVendorMessages() > 0)
                send(capabilitiesVMFactory.getCapabilitiesVM());
        } catch (IOException iox) { }
    }

    /**
     * Call this method when you want to handle us to handle a VM.  We may....
     */
    protected void handleVendorMessage(VendorMessage vm) {
        if (vm instanceof MessagesSupportedVendorMessage)
            _messagesSupported = (MessagesSupportedVendorMessage) vm;
        if (vm instanceof CapabilitiesVM) {
            _capabilities = (CapabilitiesVM) vm;
        }
        if (vm instanceof HeaderUpdateVendorMessage) {
            HeaderUpdateVendorMessage huvm = (HeaderUpdateVendorMessage)vm;
            Properties props = _headersRead.props();
            props.putAll(huvm.getProperties());
            _headersRead = HandshakeResponse.createResponse(props);
        }
    }


    /** 
     * Initializes this without timeout; exactly like initialize(0). 
     * @see initialize(int)
     */
    public void initialize(Properties requestHeaders, HandshakeResponder responder)
      throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(requestHeaders, responder, 0, null);
    }
    
    /**
     * Initializes this with a timeout
     */
    public void initialize(Properties requestHeaders, HandshakeResponder responder, int timeout)
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(requestHeaders, responder, timeout, null);
    }
    
    /**
     * Initializes this without a timeout, using the given ConnectObserver.
     */
    public void initialize(Properties requestHeaders, HandshakeResponder responder, GnetConnectObserver observer) 
     throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(requestHeaders, responder, 0, observer);
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
    protected void initialize(Properties requestHeaders, HandshakeResponder responder,
                              int timeout, GnetConnectObserver observer) 
		throws IOException, NoGnutellaOkException, BadHandshakeException {
        if(isOutgoing()) {
            if(observer != null) {
                _socket = connect(_host, _port, timeout, createAsyncConnectObserver(requestHeaders, responder, observer));
                _sslTracker = SSLUtils.getSSLBandwidthTracker(_socket);
            } else {
                _socket = connect(_host, _port, timeout);
                _sslTracker = SSLUtils.getSSLBandwidthTracker(_socket);
                preHandshakeInitialize(requestHeaders, responder, observer);
            }
        } else {
            preHandshakeInitialize(requestHeaders, responder, observer);
        }
    }
    
    protected Socket connect(String addr, int port, int timeout) throws IOException {
        return socketsManager.connect(new InetSocketAddress(addr, port), timeout, _connectType);
    }

    protected Socket connect(String addr, int port, int timeout, ConnectObserver observer) throws IOException {
        return socketsManager.connect(new InetSocketAddress(addr, port), timeout, observer, _connectType);
    }

    /**
     * Constructs the ConnectObserver that will be used to continue the connection process asynchronously.
     */
    protected ConnectObserver createAsyncConnectObserver(Properties requestHeaders, HandshakeResponder responder,
                                                         GnetConnectObserver observer) {
        return new Connector(requestHeaders, responder, observer);
    }
    
    /**
     * Finishes the initialization process.  This blocks during handshaking.
     * 
     * @throws IOException
     * @throws NoGnutellaOkException
     * @throws BadHandshakeException
     */
    protected void preHandshakeInitialize(Properties requestHeaders, HandshakeResponder responder,  
                                          GnetConnectObserver observer) throws IOException,
            NoGnutellaOkException, BadHandshakeException {
        // Check to see if close() was called while the socket was initializing
        if (_closed.get()) {
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
        acceptor.setAddress(localAddress);        
        performHandshake(requestHeaders, responder, observer);
    }
    
    /**
     * Delegates to the Handshaker to perform the handshake, and then calls
     * postHandshakeInitialize.
     * 
     * @throws IOException
     * @throws BadHandshakeException
     * @throws NoGnutellaOkException
     */
    protected void performHandshake(Properties requestHeaders, HandshakeResponder responder, GnetConnectObserver observer)
      throws IOException, BadHandshakeException, NoGnutellaOkException {     
        Handshaker shaker = createHandshaker(requestHeaders, responder);
        
        try {
            shaker.shake();
        } catch (NoGnutellaOkException e) {
            setHeaders(shaker);
            close();
            throw e;
        } catch (IOException e) {
            setHeaders(shaker);            
            close();
            throw new BadHandshakeException(e);
        }
            
        postHandshakeInitialize(shaker);
    }
    
    /** Constructs the Handshaker object. */
    protected Handshaker createHandshaker(Properties requestHeaders, HandshakeResponder responder)
      throws IOException {
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
        
        if(isOutgoing())
            return new BlockingOutgoingHandshaker(requestHeaders, responder, _socket, _in, _out);
        else
            return new BlockingIncomingHandshaker(responder, _socket, _in, _out);
    }
    
    /**
     * Sets the headers read & written.
     * 
     * @param shaker
     */
    protected void setHeaders(Handshaker shaker) {
        _headersWritten = shaker.getWrittenHeaders();
        _headersRead = shaker.getReadHeaders();
    }
    
    /**
     * Sets up the connection for post-handshake info.
     * @param shaker
     */
    protected void postHandshakeInitialize(Handshaker shaker) {
        setHeaders(shaker);
        _connectionTime = System.currentTimeMillis();

        // Now set the soft max TTL that should be used on this connection.
        // The +1 on the soft max for "good" connections is because the message
        // may come from a leaf, and therefore can have an extra hop.
        // "Good" connections are connections with features such as
        // intra-Ultrapeer QRP passing.
        _softMax = ConnectionSettings.SOFT_MAX.getValue();
        if (isGoodUltrapeer() || isGoodLeaf()) {
            // we give these an extra hop because they might be sending
            // us traffic from their leaves
            _softMax++;
        }

        // wrap the streams with inflater/deflater
        // These calls must be delayed until absolutely necessary (here)
        // because the native construction for Deflater & Inflater
        // allocate buffers outside of Java's memory heap, preventing
        // Java from fully knowing when/how to GC. The call to end()
        // (done explicitly in the close() method of this class, and
        // implicitly in the finalization of the Deflater & Inflater)
        // releases these buffers.
        if (isWriteDeflated()) {
            _deflater = Pools.getDeflaterPool().borrowObject();
            _out = createDeflatedOutputStream(_out);
        }
        
        if (isReadDeflated()) {
            _inflater = Pools.getInflaterPool().borrowObject();
            _in = createInflatedInputStream(_in);
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
        if (_closed.get())
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
        if (_closed.get())
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
        Message msg = messageFactory.read(_in, HEADER_BUF, Network.TCP, _softMax);
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
     * If SSL is enabled, this number includes the overhead
     * of the SSL wrapping.  This value will be reduced if compression
     * is enabled for sending on this connection.
     */
    public long getBytesSent() {
        if(isTLSCapable())
            return _sslTracker.getWrittenBytesProduced();
        else if(isWriteDeflated())
            return _compressedBytesSent;
        else            
            return _bytesSent;
    }
    
    /**
     * Returns the number of uncompressed bytes sent on this connection.
     * This is equal to the size of the number of messages sent on this connection.
     */
    public long getUncompressedBytesSent() {
        return _bytesSent;
    }
    
    /** 
     * Returns the number of bytes received on this connection.
     * If SSL is enabled, this number includes the overhead of incoming
     * SSL wrapped messages.  This value will be reduced if compression
     * is enabled for receiving on this connection.
     */
    public long getBytesReceived() {
        if(isTLSCapable())
            return _sslTracker.getReadBytesConsumed();
        else if(isReadDeflated())
            return _compressedBytesReceived;
        else
            return _bytesReceived;
    }
    
    /**
     * Returns the number of uncompressed bytes read on this connection.
     * This is equal to the size of all messages received through this connection.
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
        if( !isWriteDeflated() || _bytesSent == 0 )
            return 0;
        else
            return 1-((float)_compressedBytesSent/(float)_bytesSent);
    }
    
    /**
     * Returns the percentage saved from having the incoming data compressed.
     */
    public float getReadSavedFromCompression() {
        if( !isReadDeflated() || _bytesReceived == 0 )
            return 0;
        else
            return 1-((float)_compressedBytesReceived/(float)_bytesReceived);
    }
    
    /** Returns the percentage lost from outgoing SSL transformations. */
    public float getSentLostFromSSL() {
        if( !isTLSCapable() || _sslTracker.getWrittenBytesConsumed() == 0 )
            return 0;
        else
            return 1-(float)_sslTracker.getWrittenBytesConsumed() / (float)_sslTracker.getWrittenBytesProduced();
    }
    
    /** Returns the percentage lost from incoming SSL transformations. */
    public float getReadLostFromSSL() {
        if( !isTLSCapable() || _sslTracker.getReadBytesProduced() == 0 )
            return 0;
        else
            return 1-(float)_sslTracker.getReadBytesProduced() / (float)_sslTracker.getReadBytesConsumed();
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
     * Gets the port that this connection is listening on.
     * If this is an outgoing connetion, it will return the port to which
     * the socket connected.  Otherwise, if it is an incoming connection,
     * it will return the port that the remote side had in the Listen-IP
     * header.  If there was no port describe, it will return -1.
     */
    public int getListeningPort() {
        if (isOutgoing()) {
            if (_socket == null) {
                return -1;
            } else {
                return _socket.getPort();
            }
        } else {
            return _headersRead.getListeningPort();
        }
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
     * Returns the InetSocketAddress of the foreign host this is connected to.
     * This is a combination of the getInetAddress() & getPort() methods,
     * it is not the remote socket address (as the listening port may have
     * been updated by connection headers).
     * 
     * @throws IllegalStateException if this is not initialized
     */
    public InetSocketAddress getInetSocketAddress() throws IllegalStateException {
        return new InetSocketAddress(getInetAddress(), getPort());
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
        if (_headersRead != null)
            return _headersRead.supportsVendorMessages() >= 0.2;
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
     * Returns the peer's supported version of the out-of-band proxying
     * control message or -1.
     */
    public int getSupportedOOBProxyControlVersion() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsOOBProxyingControl();
        return -1;
    }
    
    /**
     * @return the peer's supported version of inspection requests or -1.
     */
    public int remoteHostSupportsInspections() {
        if (_messagesSupported != null)
            return _messagesSupported.supportsInspectionRequests();
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
     * Returns the DHT version if the remote host is an active DHT node
     * or -1 if it is not.
     */
    public int remostHostIsActiveDHTNode() {
        if(_capabilities != null) {
            return _capabilities.isActiveDHTNode();
        }
        return -1;
    }
    
    /**
     * Returns the DHT version if the remote host is a passive DHT node
     * or -1 if it is not.
     * 
     */
    public int remostHostIsPassiveDHTNode() {
        if(_capabilities != null) {
            return _capabilities.isPassiveDHTNode();
        }
        return -1;
    }
    
    /**
     * Returns the DHT version of the remote host is a passive leaf DHT node
     * or -1 if it is not.
     */
    public int remoteHostIsPassiveLeafNode() {
        if (_capabilities != null) {
            return _capabilities.isPassiveLeafNode();
        }
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
        return _headersWritten.props().getProperty(name);
    }

    /**
     * @return true until close() is called on this Connection
     */
    public boolean isOpen() {
        return !_closed.get();
    }

    /**
     *  Closes the Connection's socket and thus the connection itself.
     */
    public void close() {
        // return if it was already closed.
        if(_closed.getAndSet(true))
            return;
        
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
            Pools.getDeflaterPool().returnObject(_deflater);
        if( _inflater != null )
            Pools.getInflaterPool().returnObject(_inflater);
        
       // closing _in (and possibly _out too) can cause NPE's
       // in Message.read (and possibly other places),
       // because BufferedInputStream can't handle
       // the case where one thread is reading from the stream and
       // another closes it.
       // See BugParade ID: 4505257
        
        IOUtils.close(_in);
        IOUtils.close(_out);
    }

    
    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
		return _headersRead.getUserAgent();
    }
    
    /**
     * Returns whether or not the remote host is a LimeWire (or derivative)
     */
    public boolean isLimeWire() {
        return _headersRead.isLimeWire();
    }
    
    public boolean isOldLimeWire() {
        return _headersRead.isOldLimeWire();
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
        return _headersRead.isDeflateEnabled();
    }
    
    /**
     * Returns true if no capabilites VM is received and the connection
     * is TLS encoded, or if a capabilites VM is received and it advertises
     * support for TLS.  Otherwise, returns false.
     */
    public boolean isTLSCapable() {
        if(_capabilities == null && isTLSEncoded())
            return true;
        else if(_capabilities != null && _capabilities.supportsTLS() >= 1)
            return true;
        else
            return false;
    }
    
    /** Returns true if the connection is currently over TLS. */
    public boolean isTLSEncoded() {
        return _connectType == ConnectType.TLS;
    }

    // inherit doc comment
    public boolean isGoodUltrapeer() {
        return _headersRead.isGoodUltrapeer();
    }

    // inherit doc comment
    public boolean isGoodLeaf() {
        return _headersRead.isGoodLeaf();
    }

    // inherit doc comment
    public boolean supportsPongCaching() {
        return _headersRead.supportsPongCaching();
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
		return _headersRead.getNumIntraUltrapeerConnections();
	}

	// implements ReplyHandler interface -- inherit doc comment
	public boolean isHighDegreeConnection() {
		return _headersRead.isHighDegreeConnection();
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
		return _headersRead.isUltrapeerQueryRoutingConnection();
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
        return _headersRead.supportsProbeQueries();
    }

    /**
     * Accessor for whether or not this connection has received any
     * headers.
     *
     * @return <tt>true</tt> if this connection has finished initializing
     *  and therefore has headers, otherwise <tt>false</tt>
     */
    public boolean receivedHeaders() {
        return _headersRead != HandshakeResponse.createEmptyResponse();
    }

	/**
	 * Accessor for the <tt>HandshakeResponse</tt> instance containing all
	 * of the Gnutella connection headers passed by this node.
	 *
	 * @return the <tt>HandshakeResponse</tt> instance containing all of
	 *  the Gnutella connection headers passed by this node
	 */
	public HandshakeResponse headers() {
		return _headersRead;
	}
	
	/**
	 * Accessor for the LimeWire version reported in the connection headers
	 * for this node.	 
	 */
	public String getVersion() {
		return _headersRead.getVersion();
	}

    /** Returns true iff this connection wrote "Ultrapeer: false".
     *  This does NOT necessarily mean the connection is shielded. */
    public boolean isLeafConnection() {
		return _headersRead.isLeaf();
    }

    /** Returns true iff this connection wrote "Supernode: true". */
    public boolean isSupernodeConnection() {
		return _headersRead.isUltrapeer();
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
		return _headersRead.isGUESSCapable();
	}

	/**
	 * Returns whether or not this connection is to a ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrapeer connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolean isGUESSUltrapeer() {
		return _headersRead.isGUESSUltrapeer();
	}


    /** Returns true iff this connection is a temporary connection as per
     the headers. */
    public boolean isTempConnection() {
		return _headersRead.isTempConnection();
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
		return _headersRead.supportsGGEP();
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
		return _headersRead.isQueryRoutingEnabled();
    }

    // overrides Object.toString
    public String toString() {
        return "CONNECTION: host=" + _host  + " port=" + _port;
    }
    
    /**
     * access the locale pref. of the connected servent
     */
    public String getLocalePref() {
        return _headersRead.getLocalePref();
    }
    
    /**
     * A ConnectObserver to finish the initialization process prior
     * to handing the connection to the underlying ConnectObserver.
     */
    private class Connector implements ConnectObserver, Runnable {
        
        private final Properties requestHeaders;
        private final HandshakeResponder responder;
        private final GnetConnectObserver observer;
        
        Connector(Properties requestHeaders, HandshakeResponder responder, GnetConnectObserver observer) {
            this.requestHeaders = requestHeaders;
            this.responder = responder;
            this.observer = observer;
        }
        
        // unused.
        public void handleIOException(IOException iox) {}

        /**
         * The connection couldn't be created.
         */
        public void shutdown() {
            observer.shutdown();
        }

        /** We got a connection. */
        public void handleConnect(Socket socket) {
            // if we get the callback before the Sockets.connect call returned,
            // then _socket will be null, but the rest of the code needs it.
            // so we set it here just incase -- it's no big deal if it gets overwritten
            // later.
            _socket = socket;
            ThreadExecutor.startThread(this, "Handshaking");
        }
        
        /** Does the handshaking & completes the connection process. */
        public void run() {
            try {
                preHandshakeInitialize(requestHeaders, responder, observer);
                observer.handleConnect();
            } catch(NoGnutellaOkException ex) {
                observer.handleNoGnutellaOk(ex.getCode(), ex.getMessage());
            } catch(BadHandshakeException ex) {
                observer.handleBadHandshake();
            } catch(IOException iox) {
                observer.shutdown();
            }
        }
    }

    public Map<String,Object> inspect() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("br",getBytesReceived());
        data.put("bs",getBytesSent());
        data.put("ct",getConnectionTime());
        data.put("lp",getListeningPort());
        data.put("lpref",getLocalePref());
        data.put("ubr",getUncompressedBytesReceived());
        data.put("ubs",getUncompressedBytesSent());
        data.put("ua", getUserAgent());
        data.put("v",getVersion());
        // only one should be true, but include all three
        // to detect bugs
        data.put("pln", remoteHostIsPassiveLeafNode());
        data.put("pdn", remostHostIsPassiveDHTNode());
        data.put("pan", remostHostIsActiveDHTNode());
        return data;
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
}
