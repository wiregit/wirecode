package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inspection.Inspectable;
import org.limewire.io.BandwidthThrottle;
import org.limewire.io.ThrottledOutputStream;
import org.limewire.nio.NBThrottle;
import org.limewire.nio.Throttle;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.DeflaterWriter;
import org.limewire.nio.channel.DelayedBufferWriter;
import org.limewire.nio.channel.InflaterReader;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.channel.ThrottleWriter;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.NetworkUpdateSanityChecker.RequestType;
import com.limegroup.gnutella.connection.CompositeQueue;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionStats;
import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.connection.MessageQueue;
import com.limegroup.gnutella.connection.MessageReader;
import com.limegroup.gnutella.connection.MessageReceiver;
import com.limegroup.gnutella.connection.MessageWriter;
import com.limegroup.gnutella.connection.OutputRunner;
import com.limegroup.gnutella.connection.SentMessageHandler;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent.EventType;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.handshaking.AsyncIncomingHandshaker;
import com.limegroup.gnutella.handshaking.AsyncOutgoingHandshaker;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeObserver;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.Handshaker;
import com.limegroup.gnutella.handshaking.LeafHandshakeResponder;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.handshaking.UltrapeerHandshakeResponder;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.OOBProxyControlVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UpdateRequest;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.Sockets.ConnectType;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * A Connection managed by a ConnectionManager.  Includes a loopForMessages
 * method that runs forever (or until an IOException occurs), receiving and
 * replying to Gnutella messages.  ManagedConnection is only instantiated
 * through a ConnectionManager.<p>
 *
 * ManagedConnection provides a sophisticated message buffering mechanism.  When
 * you call send(Message), the message is not actually delivered to the socket;
 * instead it buffered in an application-level buffer.  Periodically, a thread
 * reads messages from the buffer, writes them to the network, and flushes the
 * socket buffers.  This means that there is no need to manually call flush().
 * Furthermore, ManagedConnection provides a simple form of flow control.  If
 * messages are queued faster than they can be written to the network, they are
 * dropped in the following order: PingRequest, PingReply, QueryRequest, 
 * QueryReply, and PushRequest.  See the implementation notes below for more
 * details.<p>
 *
 * All ManagedConnection's have two underlying spam filters: a personal filter
 * (controls what I see) and a route filter (also controls what I pass along to
 * others).  See SpamFilter for a description.  These filters are configured by
 * the properties in the SettingsManager, but you can change them with
 * setPersonalFilter and setRouteFilter.<p>
 *
 * ManagedConnection maintain a large number of statistics, such as the current
 * bandwidth for upstream & downstream.  ManagedConnection doesn't quite fit the
 * BandwidthTracker interface, unfortunately.  On the query-routing3-branch and
 * pong-caching CVS branches, these statistics have been bundled into a single
 * object, reducing the complexity of ManagedConnection.<p>
 * 
 * ManagedConnection also takes care of various VendorMessage handling, in
 * particular Hops Flow, UDP ConnectBack, and TCP ConnectBack.  See
 * handleVendorMessage().<p>
 *
 * This class implements ReplyHandler to route pongs and query replies that
 * originated from it.<p> 
 */
public class ManagedConnection extends Connection 
	implements ReplyHandler, MessageReceiver, SentMessageHandler, Shutdownable,
    Inspectable {
    
    private static final Log LOG = LogFactory.getLog(ManagedConnection.class);

    /** 
     * The time to wait between route table updates for leaves, 
	 * in milliseconds. 
     */
    private long LEAF_QUERY_ROUTE_UPDATE_TIME = 1000*60*5; //5 minutes
    
    /** 
     * The time to wait between route table updates for Ultrapeers, 
	 * in milliseconds. 
     */
    private long ULTRAPEER_QUERY_ROUTE_UPDATE_TIME = 1000*60; //1 minute


    /** The timeout to use when connecting, in milliseconds.  This is NOT used
     *  for bootstrap servers.  */
    private static final int CONNECT_TIMEOUT = 6000;  //6 seconds

    /** The total amount of upstream messaging bandwidth for ALL connections
     *  in BYTES (not bits) per second. */
    private static final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH=8000;

    /** The maximum number of times ManagedConnection instances should send UDP
     *  ConnectBack requests.
     */
    private static final int MAX_UDP_CONNECT_BACK_ATTEMPTS = 15;

    /** The maximum number of times ManagedConnection instances should send TCP
     *  ConnectBack requests.
     */
    private static final int MAX_TCP_CONNECT_BACK_ATTEMPTS = 10;

	/** Handle to the <tt>ConnectionManager</tt>.
	 */
    private ConnectionManager _manager;

	/** Filter for filtering out messages that are considered spam.
	 */
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    /*
     * IMPLEMENTATION NOTE: this class uses the SACHRIFC algorithm described at
     * http://www.limewire.com/developer/sachrifc.txt.  The basic idea is to use
     * one queue for each message type.  Messages are removed from the queue in
     * a biased round-robin fashion.  This prioritizes some messages types while
     * preventing any one message type from dominating traffic.  Query replies
     * are further prioritized by "GUID volume", i.e., the number of bytes
     * already routed for that GUID.  Other messages are sorted by time and
     * removed in a LIFO [sic] policy.  This, coupled with timeouts, reduces
     * latency.  
     */

    /** A lock for QRP activity on this connection */
    private final Object QRP_LOCK=new Object();
    
    /** Non-blocking throttle for outgoing messages. */
    private final static Throttle _nbThrottle = new NBThrottle(true,
                                                       TOTAL_OUTGOING_MESSAGING_BANDWIDTH,
                                                       ConnectionSettings.NUM_CONNECTIONS.getValue(),
                                                       CompositeQueue.QUEUE_TIME);
                                                            
    /** Blocking throttle for outgoing messages. */
    private final static BandwidthThrottle _throttle=
        new BandwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);
        
    
    /** The OutputRunner */
    private OutputRunner _outputRunner;
    
    /** Keeps track of sent/received [dropped] & bandwidth. */
    private final ConnectionStats _connectionStats = new ConnectionStats();
    
    /**
     * The minimum time a leaf needs to be in "busy mode" before we will consider him "truly
     * busy" for the purposes of QRT updates.
     */
    private static long MIN_BUSY_LEAF_TIME = 1000 * 20;   //  20 seconds

    /** The next time I should send a query route table to this connection.
	 */
    private long _nextQRPForwardTime;
    
    /** 
     * The bandwidth trackers for the up/downstream.
     * These are not synchronized and not guaranteed to be 100% accurate.
     */
    private BandwidthTrackerImpl _upBandwidthTracker=
        new BandwidthTrackerImpl();
    private BandwidthTrackerImpl _downBandwidthTracker=
        new BandwidthTrackerImpl();

    /** True iff this should not be policed by the ConnectionWatchdog, e.g.,
     *  because this is a connection to a Clip2 reflector. */
    private boolean _isKillable=true;
   
    /** Use this if a HopsFlowVM instructs us to stop sending queries below
     *  this certain hops value....
     */
    private volatile int hopsFlowMax = -1;

    /**
     * This member contains the time beyond which, if this host is still busy (hops flow==0),
     * that we should consider him as "truly idle" and should then remove his contributions
     * last-hop QRTs.  A value of -1 means that either the leaf isn't busy, or he is busy,
     * and his busy-ness was already noticed by the MessageRouter, so we shouldn't 're-notice'
     * him on the next QRT update iteration.
     */
    private volatile long _busyTime = -1;

    /**
     * Whether this connection is a push proxy for me
     */
    private volatile boolean myPushProxy;

    /**
     * Whether I am a push proxy for this connection
     */
    private volatile boolean pushProxyFor;
    
    /** 
     * The class wide static counter for the number of udp connect back 
     *  request sent.
     */
    private static int _numUDPConnectBackRequests = 0;

    /** 
     * The class wide static counter for the number of tcp connect back 
     *  request sent.
     */
    private static int _numTCPConnectBackRequests = 0;

    /**
     * Variable for the <tt>QueryRouteTable</tt> received for this 
     * connection.
     */
    private QueryRouteTable _lastQRPTableReceived;

    /**
     * Variable for the <tt>QueryRouteTable</tt> sent for this 
     * connection.
     */
    private QueryRouteTable _lastQRPTableSent;

    /**
     * Holds the mappings of GUIDs that are being proxied.
     */
    private GuidMap _guidMap = GuidMapFactory.getMap();

    /**
     * Whether or not this was a supernode <-> client connection when message
     * looping started.
     */
    private boolean supernodeClientAtLooping = false;
    
    /**
     * The last clientGUID a Hops=0 QueryReply had.
     */
    private byte[] clientGUID = DataUtils.EMPTY_GUID;

    /** Whether or not the HandshakeResponder should use locale preferencing during handshaking. */
    private boolean _useLocalPreference;
    
    /** If we've received a capVM before. */
    private boolean receivedCapVM = false;
 
	/**
	 * The maximum protocol version for which OOB proxying has beend turned
	 * off by leaf peer. Defaults to 0 to allow all OOB versions to be
	 * proxied.
	 */
    private int _maxDisabledOOBProtocolVersion = 0;

    /**
     * Creates a new outgoing connection to the specified host on the
	 * specified port.  
	 *
	 * @param host the address of the host we're connecting to
	 * @param port the port the host is listening on
     */
    public ManagedConnection(String host, int port) {
        super(host, port);
        _manager = RouterService.getConnectionManager();
    }
    
    /**
     * Creates a new outgoing connection to the specified host on the
     * specified port, using the specified kind of ConnectType.
     *
     * @param host the address of the host we're connecting to
     * @param port the port the host is listening on
     * @param type the type of outgoing connection we want to make (TLS, PLAIN, etc)
     */
    public ManagedConnection(String host, int port, ConnectType type) {
        super(host, port, type);
        _manager = RouterService.getConnectionManager();
    }
      
    /**
     * Creates an incoming connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the
     *  Gnutella handshake.
     */
    ManagedConnection(Socket socket) {
        super(socket);
        _manager = RouterService.getConnectionManager();
    }
    
    /**
     * Stub for calling initialize(null);
     */
    public void initialize() throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(null);
    }

    /**
     * Attempts to initialize the connection.  If observer is non-null and this wasn't
     * created with a pre-existing Socket this will return immediately.  Otherwise,
     * this will block while connecting or initializing the handshake.
     * return immediately, 
     * 
     * @param observer
     * @throws IOException
     * @throws NoGnutellaOkException
     * @throws BadHandshakeException
     */
    public void initialize(GnetConnectObserver observer) throws IOException, NoGnutellaOkException, BadHandshakeException {
        Properties requestHeaders;
        HandshakeResponder responder;
        
        if(isOutgoing()) {
            String host = getAddress();
            if(RouterService.isSupernode()) {
                requestHeaders = new UltrapeerHeaders(host);
                responder = new UltrapeerHandshakeResponder(host);
            } else {
                requestHeaders = new LeafHeaders(host);
                responder = new LeafHandshakeResponder(host);
            }
        } else {
            String host = getSocket().getInetAddress().getHostAddress();
            requestHeaders = null;
            if(RouterService.isSupernode()) {
                responder = new UltrapeerHandshakeResponder(host);
            } else {
                responder = new LeafHandshakeResponder(host);
            }
        }        
        
        // Establish the socket (if needed), handshake.
        super.initialize(requestHeaders, responder, CONNECT_TIMEOUT, observer);
        
        // Nothing else should be done here.  All post-init-sequences
        // should be triggered from finishInitialize, which will be called
        // when the socket is connected (if it connects).
    }
    
    /** Constructs a Connector that will do an asynchronous handshake. */
    protected ConnectObserver createAsyncConnectObserver(Properties requestHeaders, 
                           HandshakeResponder responder, GnetConnectObserver observer) {
        return new AsyncHandshakeConnecter(requestHeaders, responder, observer);
    }
    
    /**
     * Completes the initialization process.
     */
    protected void preHandshakeInitialize(Properties requestHeaders, HandshakeResponder responder, GnetConnectObserver observer)
      throws IOException, NoGnutellaOkException, BadHandshakeException {
        responder.setLocalePreferencing(_useLocalPreference);
        super.preHandshakeInitialize(requestHeaders, responder, observer);
    }
    
    /**
     * Performs the handshake.
     * 
     * If there is a GnetConnectObserver (it is non-null) & this connection supports
     * asynchronous messaging, then this method will return immediately and the observer
     * will be notified when handshaking completes (either succesfully or unsuccesfully).
     * 
     * Otherwise, this will block until handshaking completes.
     */
    protected void performHandshake(Properties requestHeaders, HandshakeResponder responder, GnetConnectObserver observer)
      throws IOException, BadHandshakeException, NoGnutellaOkException {
        if(observer == null || !isAsynchronous()) {
            if(!isOutgoing() && observer != null)
                throw new IllegalStateException("cannot support incoming blocking w/ observer");
            super.performHandshake(requestHeaders, responder, observer);
        } else {
            Handshaker shaker = createAsyncHandshaker(requestHeaders, responder, observer);
            try {
                shaker.shake();
            } catch (IOException iox) {
                ErrorService.error(iox); // impossible.
            }
        }
    }
    
    /** Creates the asynchronous handshaker. */
    protected Handshaker createAsyncHandshaker(Properties requestHeaders,
                                               HandshakeResponder responder,
                                               GnetConnectObserver observer) {
        
        HandshakeWatcher shakeObserver = new HandshakeWatcher(observer);
        Handshaker shaker;
        
        if(isOutgoing())
            shaker = new AsyncOutgoingHandshaker(requestHeaders, responder, _socket, shakeObserver);
        else
            shaker = new AsyncIncomingHandshaker(responder, _socket, shakeObserver);
        
        shakeObserver.setHandshaker(shaker);
        return shaker;
    }
    
    /**
     * Starts out OutputRunners & notifies UpdateManager that this
     * connection may have an update on it.
     */
    protected void postHandshakeInitialize(Handshaker shaker) {
        super.postHandshakeInitialize(shaker);

        // Start our OutputRunner.
        startOutput();
        // See if this connection had an old-style update msg.
    }

    /**
     * Resets the query route table for this connection. The new table will be of the size specified in <tt>rtm</tt>
     * and will contain no data. If there is no <tt>QueryRouteTable</tt> yet created for this connection, this method
     * will create one.
     * 
     * @param rtm
     *            the <tt>ResetTableMessage</tt>
     */
    public void resetQueryRouteTable(ResetTableMessage rtm) {
        if (_lastQRPTableReceived == null) {
            _lastQRPTableReceived =
                new QueryRouteTable(rtm.getTableSize(), rtm.getInfinity());
        } else {
            _lastQRPTableReceived.reset(rtm);
        }
    }

    /**
     * Patches the <tt>QueryRouteTable</tt> for this connection.
     *
     * @param ptm the patch with the data to update
     */
    public void patchQueryRouteTable(PatchTableMessage ptm) {

        // we should always get a reset before a patch, but 
        // allocate a table in case we don't
        if(_lastQRPTableReceived == null) {
            _lastQRPTableReceived = new QueryRouteTable();
        }
        try {
            _lastQRPTableReceived.patch(ptm);
        } catch(BadPacketException e) {
            // not sure what to do here!!
        }                    
    }

    /**
     * Set's a leaf's busy timer to now, if bSet is true, else clears the flag
     *
     *  @param bSet Whether to SET or CLEAR the busy timer for this host
     */
    public void setBusy( boolean bSet ){
        if( bSet ){            
            if( _busyTime==-1 )
                _busyTime=System.currentTimeMillis();
        }
        else
            _busyTime=-1;
    }

    /**
     * 
     * @return the current Hops Flow limit value for this connection, or -1 if we haven't
     * yet received a HF message
     */
    public byte getHopsFlowMax() {
        return (byte)hopsFlowMax;
    }
    
    /** 
     * Returns true iff this connection is a shielded leaf connection, and has 
     * signalled that it does not want to receive routed queries (no upload slots or some other reason).  
     * If so, we will not include its QRT table in last hop QRT tables we send out 
     * (if we are an Ultrapeer) 
     * @return true iff this connection is a busy leaf (don't include his QRT table)
     */
    public boolean isBusyLeaf(){
        if (!isSupernodeClientConnection())
            return false;
        int hfm = getHopsFlowMax();
        return hfm >=0 && hfm < 3;
    }
    
    /**
     * Determine whether or not the leaf has been busy long enough to remove his QRT tables
     * from the combined last-hop QRTs, and should trigger an earlier update
     * 
     * @return true iff this leaf is busy and should trigger an update to the last-hop QRTs 
     */
    public boolean isBusyEnoughToTriggerQRTRemoval(){
        if( _busyTime == -1 )
            return false;
        
        if( System.currentTimeMillis() > (_busyTime+MIN_BUSY_LEAF_TIME) )
            return true;
        
        return false;
    }
    
    /**
     * Determines whether or not the specified <tt>QueryRequest</tt>
     * instance should be sent to the connection.  The method takes a couple
     * factors into account, such as QRP tables, type of query, etc.
     *
     * @param query the <tt>QueryRequest</tt> to check against
     *  the data
     * @return <tt>true</tt> if the <tt>QueryRequest</tt> should be sent to
     * this connection, otherwise <tt>false</tt>
     */
    public boolean shouldForwardQuery(QueryRequest query) {
        // special what is queries have version numbers attached to them - make
        // sure that the remote host can answer the query....
        if (query.isFeatureQuery()) {
            if (isSupernodeClientConnection())
                return (getRemoteHostFeatureQuerySelector() >= 
                        query.getFeatureSelector());
            else if (isSupernodeSupernodeConnection())
                return getRemoteHostSupportsFeatureQueries();
            else
                return false;
        }
        return hitsQueryRouteTable(query);
    }
    
    /**
     * Determines whether or not this query hits the QRT.
     */
    protected boolean hitsQueryRouteTable(QueryRequest query) {
        if(_lastQRPTableReceived == null) return false;
        return _lastQRPTableReceived.contains(query);
	}

    /**
     * Accessor for the <tt>QueryRouteTable</tt> received along this 
     * connection.  Can be <tt>null</tt> if no query routing table has been 
     * received yet.
     *
     * @return the last <tt>QueryRouteTable</tt> received along this
     *  connection
     */
    public QueryRouteTable getQueryRouteTableReceived() {
        return _lastQRPTableReceived;
    }

    /**
     * Accessor for the last QueryRouteTable's percent full.
     */
    public double getQueryRouteTablePercentFull() {
        return _lastQRPTableReceived == null ?
            0 : _lastQRPTableReceived.getPercentFull();
    }
    
    /**
     * Accessor for the last QueryRouteTable's size.
     */
    public int getQueryRouteTableSize() {
        return _lastQRPTableReceived == null ?
            0 : _lastQRPTableReceived.getSize();
    }
    
    /**
     * Accessor for the last QueryRouteTable's Empty Units.
     */
    public int getQueryRouteTableEmptyUnits() {
        return _lastQRPTableReceived == null ?
            -1 : _lastQRPTableReceived.getEmptyUnits();
    }
    
    /**
     * Accessor for the last QueryRouteTable's Units In Use.
     */
    public int getQueryRouteTableUnitsInUse() {
        return _lastQRPTableReceived == null ?
            -1 : _lastQRPTableReceived.getUnitsInUse();
    }
    
    /**
     * Creates a deflated output stream.
     *
     * If the connection supports asynchronous messaging, this does nothing,
     * because we already installed an asynchronous writer that doesn't
     * use streams.
     */
    protected OutputStream createDeflatedOutputStream(OutputStream out) {
        if(isAsynchronous())
            return out;
        else
            return super.createDeflatedOutputStream(out);
    }
    
    /**
     * Creates the deflated input stream.
     *
     * If the connection supports asynchronous messaging, this does nothing,
     * because we're going to install a reader when we start looping for
     * messages.  Note, however, that if we use the 'receive' calls
     * instead of loopForMessages, an UncompressingInputStream is going to
     * be set up automatically.
     */
    protected InputStream createInflatedInputStream(InputStream in) {
        if(isAsynchronous())
            return in;
        else
            return super.createInflatedInputStream(in);
    }

    /**
     * Throttles the super's OutputStream.  This works quite well with
     * compressed streams, because the chaining mechanism writes the
     * compressed bytes, ensuring that we do not attempt to request
     * more data (and thus sleep while throttling) than we will actually write.
     */
    protected OutputStream getOutputStream()  throws IOException {
        return new ThrottledOutputStream(super.getOutputStream(), _throttle);
    }

    /**
     * Override of receive to do ConnectionManager stats and to properly shut
     * down the connection on IOException
     */
    public Message receive() throws IOException, BadPacketException {
        Message m = null;
        
        try {
            m = super.receive();
        } catch(IOException e) {
            if( _manager != null )
                _manager.remove(this);
            throw e;
        }
        // record received message in stats
        _connectionStats.addReceived();
        return m;
    }

    /**
     * Override of receive to do MessageRouter stats and to properly shut
     * down the connection on IOException
     */
    public Message receive(int timeout)
            throws IOException, BadPacketException, InterruptedIOException {
        Message m = null;
        
        try {
            m = super.receive(timeout);
        } catch(InterruptedIOException ioe) {
            //we read nothing in this timeframe,
            //do not remove, just rethrow.
            throw ioe;
        } catch(IOException e) {
            if( _manager != null )
                _manager.remove(this);
            throw e;
        }
        
        // record received message in stats
        _connectionStats.addReceived();
        return m;
    }


    ////////////////////// Sending, Outgoing Flow Control //////////////////////
    
    /**
     * Starts an OutputRunner.  If the Connection supports asynchronous writing,
     * this does not use an extra thread.  Otherwise, a thread is started up
     * to write.
     */
    private void startOutput() {
        MessageQueue queue;
        // Taking this change out until we can safely handle attacks and overflow 
        // TODO: make a cheaper Queue that still prevents flooding of ultrapeer
        //       and ensures that clogged leaf doesn't drop QRP messages.
		//if(isSupernodeSupernodeConnection())
		    queue = new CompositeQueue();
		//else
		    //queue = new BasicQueue();

		if(isAsynchronous()) {
		    MessageWriter messager = new MessageWriter(_connectionStats, queue, this);
		    _outputRunner = messager;
		    ChannelWriter writer = messager;
		    
		    if(isWriteDeflated()) {
		        DeflaterWriter deflater = new DeflaterWriter(_deflater);
		        messager.setWriteChannel(deflater);
                writer = deflater;
            }
            DelayedBufferWriter delayer = new DelayedBufferWriter(1400);
            writer.setWriteChannel(delayer);
            writer = delayer;
            writer.setWriteChannel(new ThrottleWriter(_nbThrottle));
		    
		    ((NIOMultiplexor)_socket).setWriteObserver(messager);
		} else {
		    _outputRunner = new BlockingRunner(queue);
        }
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
     */
    public void send(Message m) {
        // if Hops Flow is in effect, and this is a QueryRequest, and the
        // hoppage is too biggage, discardage time...
    	int smh = hopsFlowMax;
        if (smh > -1 && (m instanceof QueryRequest) && m.getHops() >= smh)
            return;
                    
        
        _outputRunner.send(m);
    }

    /**
     * This is a specialized send method for queries that we originate, 
     * either from ourselves directly, or on behalf of one of our leaves
     * when we're an Ultrapeer.  These queries have a special sending 
     * queue of their own and are treated with a higher priority.
     *
     * @param query the <tt>QueryRequest</tt> to send
     */
    public void originateQuery(QueryRequest query) {
        query.originate();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("do not proxy condition " + 
                     isClientSupernodeConnection() + " " +
                     (getSupportedOOBProxyControlVersion() == -1)  + " " +
                     SearchSettings.DISABLE_OOB_V2.getValueAsString());
        }
        
        if (isClientSupernodeConnection()
                && getSupportedOOBProxyControlVersion() == -1
                && SearchSettings.DISABLE_OOB_V2.getBoolean()) {
            // don't proxy if we are a leaf and the ultrapeer 
            // does not know OOB v3 and they would proxy for us
            query = QueryRequest.createDoNotProxyQuery(query);
            query.originate();
        }
        
        send(query);
    }
 
    /**
     * Does nothing.  Since this automatically takes care of flushing output
     * buffers, there is nothing to do.  Note that flush() does NOT block for
     * TCP buffers to be emptied.  
     */
    public void flush() throws IOException {        
    }
    
    public void shutdown() {
        close();
    }

    public void close() {
        if(_outputRunner != null)
            _outputRunner.shutdown();
        super.close();
        
        // release pointer to our _guidMap so it can be gc()'ed
        GuidMapFactory.removeMap(_guidMap);
    }

    //////////////////////////////////////////////////////////////////////////
    
    /**
     * Handles core Gnutella request/reply protocol.
     * If asynchronous messaging is supported, this immediately
     * returns and messages are processed asynchronously via processMessage
     * calls.  Otherwise, if reading blocks, this  will run until the connection
     * is closed.
     *
     * @requires this is initialized
     * @modifies the network underlying this, manager
     * @effects receives request and sends appropriate replies.
     *
     * @throws IOException passed on from the receive call; failures to forward
     *         or route messages are silently swallowed, allowing the message
     *         loop to continue.
     */
    void loopForMessages() throws IOException {
        supernodeClientAtLooping = isSupernodeClientConnection();
        
        if(!isAsynchronous()) {
            LOG.debug("Starting blocking connection");
            Thread.currentThread().setName("MessageLoopingThread");
            while (true) {
                Message m=null;
                try {
                    m = receive();
                    if (m==null)
                        continue;
                    handleMessageInternal(m);
                } catch (BadPacketException ignored) {}
            }
        } else {
            LOG.debug("Starting asynchronous connection");
            _socket.setSoTimeout(0); // no timeout for reading.
            
            MessageReader reader = new MessageReader(ManagedConnection.this);
            if(isReadDeflated())
                reader.setReadChannel(new InflaterReader(_inflater));
                
            ((NIOMultiplexor)_socket).setReadObserver(reader);
        }
    }
    
    /**
     * Notification that messaging has closed.
     */
    public void messagingClosed() {
        // we must run this in another thread, as manager.remove
        // obtains locks, but this can be called from the NIO thread
        if( _manager != null ) {
            RouterService.getMessageDispatcher().dispatch(new Runnable() {
                public void run() {
                    _manager.remove(ManagedConnection.this);
                }
            });
        }
    }
    
    /**
     * Notification that a message is available to be processed (via asynch-processing).
     */
    public void processReadMessage(Message m) throws IOException {
        updateReadStatistics(m);
        _connectionStats.addReceived();
        handleMessageInternal(m);
    }
    
    /**
     * Notification that a message has been sent.  Updates stats.
     */
    public void processSentMessage(Message m) {
        updateWriteStatistics(m);
    }
    
    /**
     * Handles a message without updating appropriate statistics.
     */
    private void handleMessageInternal(Message m) {
        // Run through the route spam filter and drop accordingly.
        if (isSpam(m)) {
			ReceivedMessageStatHandler.TCP_FILTERED_MESSAGES.addMessage(m);
            _connectionStats.addReceivedDropped();
        } else {
            if(m instanceof QueryReply){ 
            	_connectionStats.replyReceived();
            	if(m.getHops() == 0)
            		clientGUID = ((QueryReply)m).getClientGUID();
            }
        
            if (m instanceof QueryRequest) 
                _connectionStats.queryReceived();
            //special handling for proxying.
            if(supernodeClientAtLooping) {
                if(m instanceof QueryRequest)
                    m = tryToProxy((QueryRequest) m);
                else if (m instanceof QueryStatusResponse)
                    m = morphToStopQuery((QueryStatusResponse) m);
            }
            RouterService.getMessageDispatcher().dispatchTCP(m, this);
        }
    }
    
    public long getNumQueryReplies() {
    	return _connectionStats.getRepliesReceived();
    }
    
    /**
     * Returns the network that the MessageReceiver uses -- Message.N_TCP.
     */
    public Network getNetwork() {
        return Network.TCP;
    }
    

    private QueryRequest tryToProxy(QueryRequest query) {
        // we must have the following qualifications:
        // 1) Leaf must be sending SuperNode a query (checked in loopForMessages)
        // 2) Leaf must support Leaf Guidance
        // 3) Query must not be OOB.
        // 3.5) The query originator should not disallow proxying.
        // 4) We must be able to OOB and have great success rate.
        if (remoteHostSupportsLeafGuidance() < 1) return query;
        if (query.desiresOutOfBandRepliesV3()) return query;
        
        if (query.doNotProxy()) return query;
        
        if (_maxDisabledOOBProtocolVersion >= ReplyNumberVendorMessage.VERSION) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("query not proxied because disabled version is " + _maxDisabledOOBProtocolVersion);
            }
            return query;
        }
        else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("query might be proxied for max disabled version " + _maxDisabledOOBProtocolVersion + " " + Arrays.toString(query.getGUID()));
            }
        }
        
        if (!RouterService.isOOBCapable() || 
            !OutOfBandThroughputStat.isSuccessRateGreat() ||
            !OutOfBandThroughputStat.isOOBEffectiveForProxy()) return query;

        // everything is a go - we need to do the following:
        // 1) mutate the GUID of the query - you should maintain every param of
        // the query except the new GUID and the OOB minspeed flag
        // 2) set up mappings between the old guid and the new guid.
        // after that, everything is set.  all you need to do is map the guids
        // of the replies back to the original guid.  also, see if a you get a
        // QueryStatusResponse message and morph it...
        // THIS IS SOME MAJOR HOKERY-POKERY!!!
        
        // 1) mutate the GUID of the query
        byte[] origGUID = query.getGUID();
        byte[] oobGUID = new byte[origGUID.length];
        System.arraycopy(origGUID, 0, oobGUID, 0, origGUID.length);
        GUID.addressEncodeGuid(oobGUID, RouterService.getAddress(),
                               RouterService.getPort());
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(oobGUID);

        query = QueryRequest.createProxyQuery(query, oobGUID);
        
        // 2) set up mappings between the guids
        _guidMap.addMapping(origGUID, oobGUID);

        OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
        return query;
    }

    private QueryStatusResponse morphToStopQuery(QueryStatusResponse resp) {
        GUID oobGUID = _guidMap.getNewGUID(resp.getQueryGUID());
        // if we had a match, then just construct a new one....
        if (oobGUID != null)
            return new QueryStatusResponse(oobGUID, resp.getNumResults());
        else
            return resp;
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
     * A callback for the ConnectionManager to inform this connection that a
     * message was dropped.  This happens when a reply received from this
     * connection has no routing path.
     */
    public void countDroppedMessage() {
        _connectionStats.addReceivedDropped();
    }

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
     * This method is called when a reply is received for a PingRequest
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePingReply(PingReply pingReply,
                                ReplyHandler receivingConnection) {
        send(pingReply);
    }

    /**
     * This method is called when a reply is received for a QueryRequest
     * originating on this Connection.  So, send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler receivingConnection) {
    	
    	boolean checkOOB = true;
        if (_guidMap != null) {
            byte[] origGUID = _guidMap.getOriginalGUID(queryReply.getGUID());
            if (origGUID != null) {
            	checkOOB = false;
                byte prevHops = queryReply.getHops();
                queryReply = new QueryReply(origGUID, queryReply);
                queryReply.setTTL((byte)2); // we ttl 1 more than necessary
                queryReply.setHops(prevHops);
            }
            // ---------------------
        }
        
        // drop UDP replies that are not being proxied.
        if (checkOOB && queryReply.isUDP() 
        		&& !queryReply.isReplyToMulticastQuery()) 
        	return;
        
        send(queryReply);
    }
    
    /**
     * Gets the clientGUID of the remote host of the connection.
     */
    public byte[] getClientGUID() {
        return clientGUID;
    }

    /**
     * This method is called when a PushRequest is received for a QueryReply
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePushRequest(PushRequest pushRequest,
                                  ReplyHandler receivingConnection) {
        send(pushRequest);
    }   


    protected void handleVendorMessage(VendorMessage vm) {
        // let Connection do as needed....
        super.handleVendorMessage(vm);

        // now i can process
        if (vm instanceof HopsFlowVendorMessage) {
            // update the softMaxHops value so it can take effect....
            HopsFlowVendorMessage hops = (HopsFlowVendorMessage) vm;
            
            if( isSupernodeClientConnection() )
                //	If the connection is to a leaf, and it is busy (HF == 0)
                //	then set the global busy leaf flag appropriately
                setBusy( hops.getHopValue()==0 );
            
            hopsFlowMax = hops.getHopValue();
        }
        else if (vm instanceof PushProxyAcknowledgement) {
            // this connection can serve as a PushProxy, so note this....
            PushProxyAcknowledgement ack = (PushProxyAcknowledgement) vm;
            if (Arrays.equals(ack.getGUID(),
                              RouterService.getMessageRouter()._clientGUID)) {
                myPushProxy = true;
            }
            // else mistake on the server side - the guid should be my client
            // guid - not really necessary but whatever
        }
        else if(vm instanceof CapabilitiesVM) {
            //we need to see if there is a new simpp version out there.
            CapabilitiesVM capVM = (CapabilitiesVM)vm;
            int smpV = capVM.supportsSIMPP();
            if(smpV != -1 && (!receivedCapVM || smpV > SimppManager.instance().getVersion())) {
                //request the simpp message
                RouterService.getNetworkUpdateSanityChecker().handleNewRequest(this, RequestType.SIMPP);
                send(new SimppRequestVM());
            }
            
            // see if there's a new update message.
            int latestId = UpdateHandler.instance().getLatestId();
            int currentId = capVM.supportsUpdate();
            if(currentId != -1 && (!receivedCapVM || currentId > latestId)) {
                RouterService.getNetworkUpdateSanityChecker().handleNewRequest(this, RequestType.VERSION);
                send(new UpdateRequest());
            } else if(currentId == latestId) {
                UpdateHandler.instance().handleUpdateAvailable(this, currentId);
            }
            
            receivedCapVM = true;
            //fire a vendor event
            _manager.dispatchEvent(new ConnectionLifecycleEvent(this, 
                    EventType.CONNECTION_CAPABILITIES , this));
                
        }
        else if (vm instanceof MessagesSupportedVendorMessage) {        
            // If this is a ClientSupernodeConnection and the host supports
            // leaf guidance (because we have to tell them when to stop)
            // then see if there are any old queries that we can re-originate
            // on this connection.
            if(isClientSupernodeConnection() &&
               (remoteHostSupportsLeafGuidance() >= 0)) {
                SearchResultHandler srh =
                    RouterService.getSearchResultHandler();
                List<QueryRequest> queries = srh.getQueriesToReSend();
                for(QueryRequest qr : queries) {
                    send(qr);
                }
            }            

            // see if you need a PushProxy - the remoteHostSupportsPushProxy
            // test incorporates my leaf status in it.....
            if (remoteHostSupportsPushProxy() > -1) {
                // get the client GUID and send off a PushProxyRequest
                GUID clientGUID =
                    new GUID(RouterService.getMessageRouter()._clientGUID);
                PushProxyRequest req = new PushProxyRequest(clientGUID);
                send(req);
            }

            // do i need to send any ConnectBack messages????
            if (!UDPService.instance().canReceiveUnsolicited() &&
                (_numUDPConnectBackRequests < MAX_UDP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsUDPRedirect() > -1)) {
                GUID connectBackGUID = RouterService.getUDPConnectBackGUID();
                Message udp = new UDPConnectBackVendorMessage(RouterService.getPort(),
                                                              connectBackGUID);
                send(udp);
                _numUDPConnectBackRequests++;
            }

            if (!RouterService.acceptedIncomingConnection() &&
                (_numTCPConnectBackRequests < MAX_TCP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsTCPRedirect() > -1)) {
                Message tcp = new TCPConnectBackVendorMessage(RouterService.getPort());
                send(tcp);
                _numTCPConnectBackRequests++;
            }

            // disable oobv2 explicitly.
            if (isClientSupernodeConnection()&&
                    SearchSettings.DISABLE_OOB_V2.getBoolean() &&
                    getSupportedOOBProxyControlVersion() != -1) {
                Message stopv2 = 
                    new OOBProxyControlVendorMessage(
                            OOBProxyControlVendorMessage.Control.DISABLE_VERSION_2);
                send(stopv2);
            }
        }
        else if (vm instanceof OOBProxyControlVendorMessage) {
            _maxDisabledOOBProtocolVersion = ((OOBProxyControlVendorMessage)vm).getMaximumDisabledVersion();
            if (LOG.isTraceEnabled()) {
                LOG.trace("_maxDisabledOOBProtocolVersion set to " + _maxDisabledOOBProtocolVersion);
            }
        }
    }


    //
    // End reply forwarding calls
    //


    //
    // Begin statistics accessors
    //

    /** Returns the number of messages sent on this connection */
    public int getNumMessagesSent() {
        return _connectionStats.getSent();
    }

    /** Returns the number of messages received on this connection */
    public int getNumMessagesReceived() {
        return _connectionStats.getReceived();
    }

    /** Returns the number of messages I dropped while trying to send
     *  on this connection.  This happens when the remote host cannot
     *  keep up with me. */
    public int getNumSentMessagesDropped() {
        return _connectionStats.getSentDropped();
    }

    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    public long getNumReceivedMessagesDropped() {
        return _connectionStats.getReceivedDropped();
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.
     */
    public float getPercentReceivedDropped() {
        return _connectionStats.getPercentReceivedDropped();
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were
     *  dropped by this end of the connection.  This value may be
     *  greater than 100%, e.g., if only one message is sent but
     *  four are dropped during a given time period.
     */
    public float getPercentSentDropped() {
        return _connectionStats.getPercentSentDropped();
    }

    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public void measureBandwidth() {
        _upBandwidthTracker.measureBandwidth(
             ByteOrder.long2int(getBytesSent()));
        _downBandwidthTracker.measureBandwidth(
             ByteOrder.long2int(getBytesReceived()));
    }

    /**
     * Returns the upstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredUpstreamBandwidth() {
        float retValue = 0; //initialize to default
        try {
            retValue = _upBandwidthTracker.getMeasuredBandwidth();
        } catch(InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    /**
     * Returns the downstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredDownstreamBandwidth() {
        float retValue = 0;
        try {
            retValue = _downBandwidthTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    //
    // End statistics accessors
    //

    /** Returns the system time that we should next forward a query route table
     *  along this connection.  Only valid if isClientSupernodeConnection() is
     *  true. */
    public long getNextQRPForwardTime() {
        return _nextQRPForwardTime;
    }

	/**
	 * Increments the next time we should forward query route tables for
	 * this connection.  This depends on whether or not this is a connection
	 * to a leaf or to an Ultrapeer.
	 *
	 * @param curTime the current time in milliseconds, used to calculate 
	 *  the next update time
	 */
	public void incrementNextQRPForwardTime(long curTime) {
		if(isLeafConnection()) {
			_nextQRPForwardTime = curTime + LEAF_QUERY_ROUTE_UPDATE_TIME;
		} else {
			// otherwise, it's an Ultrapeer
			_nextQRPForwardTime = curTime + ULTRAPEER_QUERY_ROUTE_UPDATE_TIME;
		}
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

    /**
     * Returns whether or not this connection is a push proxy for me
     */
    public boolean isMyPushProxy() {
        return myPushProxy;
    }

    /**
     * Returns whether or not I'm a push proxy for this connection
     */
    public boolean isPushProxyFor() {
        return pushProxyFor;
    }
    
    /**
     * Sets whether or not I'm a push proxy for this connection
     */
    public void setPushProxyFor(boolean pushProxyFor) {
        this.pushProxyFor = pushProxyFor;
    }
    
	public Object getQRPLock() {
		return QRP_LOCK;
	}

    /**
     * set preferencing for the responder
     * (The preference of the Responder is used when creating the response 
     * (in Connection.java: conclude..))
     */
    public void setLocalePreferencing(boolean b) {
        _useLocalPreference = b;
    }
    
    public void reply(Message m){
    	send(m);
    }
    

    /** Repeatedly sends all the queued data using a thread. */
    private class BlockingRunner implements Runnable, OutputRunner {
        private final Object LOCK = new Object();
        private final MessageQueue queue;
        private boolean shutdown = false;
        
        public BlockingRunner(MessageQueue queue) {
            this.queue = queue;
            ThreadExecutor.startThread(this, "OutputRunner");
        }

        public void send(Message m) {
            synchronized (LOCK) {
                _connectionStats.addSent(m);
                queue.add(m);
                int dropped = queue.resetDropped();
                _connectionStats.addSentDropped(dropped);
                LOCK.notify();
            }
        }
        
        public void shutdown() {
            synchronized(LOCK) {
                shutdown = true;
                LOCK.notify();
            }
        }

        /** While the connection is not closed, sends all data delay. */
        public void run() {
            //For non-IOExceptions, Throwable is caught to notify ErrorService.
            try {
                while (true) {
                    waitForQueued();
                    sendQueued();
                }                
            } catch (IOException e) {
                if(_manager != null)
                    _manager.remove(ManagedConnection.this);
            } catch(Throwable t) {
                if(_manager != null)
                    _manager.remove(ManagedConnection.this);
                ErrorService.error(t);
            }
        }

        /** 
         * Wait until the queue is (probably) non-empty or closed. 
         * @exception IOException this was closed while waiting
         */
        private final void waitForQueued() throws IOException {
            // Lock outside of the loop so that the MessageQueue is synchronized.
            synchronized (LOCK) {
                while (!shutdown && isOpen() && queue.isEmpty()) {           
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            
            if (! isOpen() || shutdown)
                throw CONNECTION_CLOSED;
        }
        
        /** Send several queued message of each type. */
        private final void sendQueued() throws IOException {
            // Send as many messages as we can, until we run out.
            while(true) {
                Message m = null;
                synchronized(LOCK) {
                    m = queue.removeNext();
                    int dropped = queue.resetDropped();
                    _connectionStats.addSentDropped(dropped);
                }
                if(m == null)
                    break;

                //Note that if the ougoing stream is compressed
                //(isWriteDeflated()), this call may not actually
                //do anything.  This is because the Deflater waits
                //until an optimal time to start deflating, buffering
                //up incoming data until that time is reached, or the
                //data is explicitly flushed.
                ManagedConnection.super.send(m);
            }
            
            //Note that if the outgoing stream is compressed 
            //(isWriteDeflated()), then this call may block while the
            //Deflater deflates the data.
            ManagedConnection.super.flush();
        }
    }
    

    /**
     * A ConnectObserver that continues the handshaking process in the same thread,
     * expecting that performHandshake(...) callback to the observer.
     */
    private class AsyncHandshakeConnecter implements ConnectObserver {

        private Properties requestHeaders;
        private HandshakeResponder responder;
        private GnetConnectObserver observer;

        AsyncHandshakeConnecter(Properties requestHeaders, HandshakeResponder responder, GnetConnectObserver observer) {
            this.requestHeaders = requestHeaders;
            this.responder = responder;
            this.observer = observer;
        }
        
        public void handleConnect(Socket socket) throws IOException {
            // _socket may not really have been set yet, this ensures it
            // is.
            _socket = socket;
            preHandshakeInitialize(requestHeaders, responder, observer);
        }

        public void shutdown() {
            observer.shutdown();
        }

        //ignored.
        public void handleIOException(IOException iox) {}
    }
    
    /**
     * A HandshakeObserver that notifies the GnetConnectObserver when handshaking finishes.
     */
    private class HandshakeWatcher implements HandshakeObserver {
        
        private Handshaker shaker;
        private  GnetConnectObserver observer;

        HandshakeWatcher(GnetConnectObserver observer) {
            this.observer = observer;
        }
        
        void setHandshaker(Handshaker shaker) {
            this.shaker = shaker;
        }

        public void shutdown() {
            setHeaders(shaker);
            close();
            observer.shutdown();            
        }

        public void handleHandshakeFinished(Handshaker shaker) {
            postHandshakeInitialize(shaker);
            observer.handleConnect();
        }

        public void handleBadHandshake() {
            setHeaders(shaker);
            close();
            observer.handleBadHandshake();
        }

        public void handleNoGnutellaOk(int code, String msg) {
            setHeaders(shaker);
            close();
            observer.handleNoGnutellaOk(code, msg);
        }
    }
    
    public Map<String,Object> inspect() {
        // get all kinds of data
        Map<String, Object> data = super.inspect();
        data.put("hfm",getHopsFlowMax());
        data.put("mdb",getMeasuredDownstreamBandwidth());
        data.put("mub",getMeasuredUpstreamBandwidth());
        data.put("qrpft",getNextQRPForwardTime());
        data.put("nmr",getNumMessagesReceived());
        data.put("nms",getNumMessagesSent());
        _connectionStats.addStats(data);
        data.put("nrmd",getNumReceivedMessagesDropped());
        data.put("nsmd",getNumSentMessagesDropped());
        data.put("qrteu",getQueryRouteTableEmptyUnits());
        data.put("qrtpf",getQueryRouteTablePercentFull());
        data.put("qrts", getQueryRouteTableSize());
        data.put("betqr",isBusyEnoughToTriggerQRTRemoval());
        data.put("bl",isBusyLeaf());
        data.put("k", isKillable());
        data.put("pp",isPushProxyFor());
        data.put("rhsi", remoteHostSupportsInspections());
        data.put("tlsc", isTLSCapable());
        data.put("tlse", isTLSEncoded());
        return data;
    }
}
