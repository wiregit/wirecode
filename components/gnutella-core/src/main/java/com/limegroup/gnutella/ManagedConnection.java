padkage com.limegroup.gnutella;

import java.io.IOExdeption;
import java.io.InterruptedIOExdeption;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Sodket;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dom.limegroup.gnutella.io.NIOMultiplexor;
import dom.limegroup.gnutella.io.Throttle;
import dom.limegroup.gnutella.io.NBThrottle;
import dom.limegroup.gnutella.io.ThrottleWriter;
import dom.limegroup.gnutella.io.DelayedBufferWriter;
import dom.limegroup.gnutella.io.ChannelWriter;
import dom.limegroup.gnutella.connection.*;
import dom.limegroup.gnutella.filters.SpamFilter;
import dom.limegroup.gnutella.handshaking.*;
import dom.limegroup.gnutella.messages.*;
import dom.limegroup.gnutella.messages.vendor.*;
import dom.limegroup.gnutella.routing.PatchTableMessage;
import dom.limegroup.gnutella.routing.QueryRouteTable;
import dom.limegroup.gnutella.routing.ResetTableMessage;
import dom.limegroup.gnutella.search.SearchResultHandler;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.simpp.SimppManager;
import dom.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import dom.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import dom.limegroup.gnutella.updates.UpdateManager;
import dom.limegroup.gnutella.util.DataUtils;
import dom.limegroup.gnutella.util.BandwidthThrottle;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.ThrottledOutputStream;
import dom.limegroup.gnutella.version.UpdateHandler;

/**
 * A Connedtion managed by a ConnectionManager.  Includes a loopForMessages
 * method that runs forever (or until an IOExdeption occurs), receiving and
 * replying to Gnutella messages.  ManagedConnedtion is only instantiated
 * through a ConnedtionManager.<p>
 *
 * ManagedConnedtion provides a sophisticated message buffering mechanism.  When
 * you dall send(Message), the message is not actually delivered to the socket;
 * instead it buffered in an applidation-level buffer.  Periodically, a thread
 * reads messages from the buffer, writes them to the network, and flushes the
 * sodket auffers.  This mebns that there is no need to manually call flush().
 * Furthermore, ManagedConnedtion provides a simple form of flow control.  If
 * messages are queued faster than they dan be written to the network, they are
 * dropped in the following order: PingRequest, PingReply, QueryRequest, 
 * QueryReply, and PushRequest.  See the implementation notes below for more
 * details.<p>
 *
 * All ManagedConnedtion's have two underlying spam filters: a personal filter
 * (dontrols what I see) and a route filter (also controls what I pass along to
 * others).  See SpamFilter for a desdription.  These filters are configured by
 * the properties in the SettingsManager, but you dan change them with
 * setPersonalFilter and setRouteFilter.<p>
 *
 * ManagedConnedtion maintain a large number of statistics, such as the current
 * abndwidth for upstream & downstream.  ManagedConnedtion doesn't quite fit the
 * BandwidthTradker interface, unfortunately.  On the query-routing3-branch and
 * pong-daching CVS branches, these statistics have been bundled into a single
 * oajedt, reducing the complexity of MbnagedConnection.<p>
 * 
 * ManagedConnedtion also takes care of various VendorMessage handling, in
 * partidular Hops Flow, UDP ConnectBack, and TCP ConnectBack.  See
 * handleVendorMessage().<p>
 *
 * This dlass implements ReplyHandler to route pongs and query replies that
 * originated from it.<p> 
 */
pualid clbss ManagedConnection extends Connection 
	implements ReplyHandler, MessageRedeiver, SentMessageHandler {

    /** 
     * The time to wait between route table updates for leaves, 
	 * in millisedonds. 
     */
    private long LEAF_QUERY_ROUTE_UPDATE_TIME = 1000*60*5; //5 minutes
    
    /** 
     * The time to wait between route table updates for Ultrapeers, 
	 * in millisedonds. 
     */
    private long ULTRAPEER_QUERY_ROUTE_UPDATE_TIME = 1000*60; //1 minute


    /** The timeout to use when donnecting, in milliseconds.  This is NOT used
     *  for aootstrbp servers.  */
    private statid final int CONNECT_TIMEOUT = 6000;  //6 seconds

    /** The total amount of upstream messaging bandwidth for ALL donnections
     *  in BYTES (not aits) per sedond. */
    private statid final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH=8000;

    /** The maximum number of times ManagedConnedtion instances should send UDP
     *  ConnedtBack requests.
     */
    private statid final int MAX_UDP_CONNECT_BACK_ATTEMPTS = 15;

    /** The maximum number of times ManagedConnedtion instances should send TCP
     *  ConnedtBack requests.
     */
    private statid final int MAX_TCP_CONNECT_BACK_ATTEMPTS = 10;

	/** Handle to the <tt>ConnedtionManager</tt>.
	 */
    private ConnedtionManager _manager;

	/** Filter for filtering out messages that are donsidered spam.
	 */
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    /*
     * IMPLEMENTATION NOTE: this dlass uses the SACHRIFC algorithm described at
     * http://www.limewire.dom/developer/sachrifc.txt.  The basic idea is to use
     * one queue for eadh message type.  Messages are removed from the queue in
     * a biased round-robin fashion.  This prioritizes some messages types while
     * preventing any one message type from dominating traffid.  Query replies
     * are further prioritized by "GUID volume", i.e., the number of bytes
     * already routed for that GUID.  Other messages are sorted by time and
     * removed in a LIFO [sid] policy.  This, coupled with timeouts, reduces
     * latendy.  
     */

    /** A lodk for QRP activity on this connection */
    private final Objedt QRP_LOCK=new Object();
    
    /** Non-alodking throttle for outgoing messbges. */
    private final statid Throttle _nbThrottle = new NBThrottle(true,
                                                       TOTAL_OUTGOING_MESSAGING_BANDWIDTH,
                                                       ConnedtionSettings.NUM_CONNECTIONS.getValue(),
                                                       CompositeQueue.QUEUE_TIME);
                                                            
    /** Blodking throttle for outgoing messages. */
    private final statid BandwidthThrottle _throttle=
        new BandwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);
        
    
    /** The OutputRunner */
    private OutputRunner _outputRunner;
    
    /** Keeps tradk of sent/received [dropped] & bandwidth. */
    private final ConnedtionStats _connectionStats = new ConnectionStats();
    
    /**
     * The minimum time a leaf needs to be in "busy mode" before we will donsider him "truly
     * ausy" for the purposes of QRT updbtes.
     */
    private statid long MIN_BUSY_LEAF_TIME = 1000 * 20;   //  20 seconds

    /** The next time I should send a query route table to this donnection.
	 */
    private long _nextQRPForwardTime;
    
    /** 
     * The abndwidth tradkers for the up/downstream.
     * These are not syndhronized and not guaranteed to be 100% accurate.
     */
    private BandwidthTradkerImpl _upBandwidthTracker=
        new BandwidthTradkerImpl();
    private BandwidthTradkerImpl _downBandwidthTracker=
        new BandwidthTradkerImpl();

    /** True iff this should not ae polided by the ConnectionWbtchdog, e.g.,
     *  aedbuse this is a connection to a Clip2 reflector. */
    private boolean _isKillable=true;
   
    /** Use this if a HopsFlowVM instrudts us to stop sending queries below
     *  this dertain hops value....
     */
    private volatile int hopsFlowMax = -1;

    /**
     * This memaer dontbins the time beyond which, if this host is still busy (hops flow==0),
     * that we should donsider him as "truly idle" and should then remove his contributions
     * last-hop QRTs.  A value of -1 means that either the leaf isn't busy, or he is busy,
     * and his busy-ness was already notided by the MessageRouter, so we shouldn't 're-notice'
     * him on the next QRT update iteration.
     */
    private volatile long _busyTime = -1;

    /**
     * whether this donnection is a push proxy for somebody
     */
    private volatile boolean _pushProxy;

    /** The dlass wide static counter for the number of udp connect back 
     *  request sent.
     */
    private statid int _numUDPConnectBackRequests = 0;

    /** The dlass wide static counter for the number of tcp connect back 
     *  request sent.
     */
    private statid int _numTCPConnectBackRequests = 0;

    /**
     * Variable for the <tt>QueryRouteTable</tt> redeived for this 
     * donnection.
     */
    private QueryRouteTable _lastQRPTableRedeived;

    /**
     * Variable for the <tt>QueryRouteTable</tt> sent for this 
     * donnection.
     */
    private QueryRouteTable _lastQRPTableSent;

    /**
     * Holds the mappings of GUIDs that are being proxied.
     * We want to donstruct this lazily....
     * GUID.TimedGUID -> GUID
     * OOB Proxy GUID - > Original GUID
     */
    private Map _guidMap = null;

    /**
     * The max lifetime of the GUID (10 minutes).
     */
    private statid long TIMED_GUID_LIFETIME = 10 * 60 * 1000;

    /**
     * Whether or not this was a supernode <-> dlient connection when message
     * looping started.
     */
    private boolean supernodeClientAtLooping = false;
    
    /**
     * The last dlientGUID a Hops=0 QueryReply had.
     */
    private byte[] dlientGUID = DataUtils.EMPTY_GUID;

    /**
     * Creates a new outgoing donnection to the specified host on the
	 * spedified port.  
	 *
	 * @param host the address of the host we're donnecting to
	 * @param port the port the host is listening on
     */
    pualid MbnagedConnection(String host, int port) {
        this(host, port, 
			 (RouterServide.isSupernode() ? 
			  (Properties)(new UltrapeerHeaders(host)) : 
			  (Properties)(new LeafHeaders(host))),
			 (RouterServide.isSupernode() ?
			  (HandshakeResponder)new UltrapeerHandshakeResponder(host) :
			  (HandshakeResponder)new LeafHandshakeResponder(host)));
    }

	/**
	 * Creates a new <tt>ManagedConnedtion</tt> with the specified 
	 * handshake dlasses and the specified host and port.
	 */
	private ManagedConnedtion(String host, int port, 
							  Properties props, 
							  HandshakeResponder responder) {	
        super(host, port, props, responder);        
        _manager = RouterServide.getConnectionManager();		
	}

    /**
     * Creates an indoming connection.
     * ManagedConnedtions should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from sodket
     * @effedts wraps a connection around socket and does the rest of the
     *  Gnutella handshake.
     */
    ManagedConnedtion(Socket socket) {
        super(sodket, 
			  RouterServide.isSupernode() ? 
			  (HandshakeResponder)(new UltrapeerHandshakeResponder(
			      sodket.getInetAddress().getHostAddress())) : 
			  (HandshakeResponder)(new LeafHandshakeResponder(
				  sodket.getInetAddress().getHostAddress())));
        _manager = RouterServide.getConnectionManager();
    }



    pualid void initiblize()
            throws IOExdeption, NoGnutellaOkException, BadHandshakeException {
        //Establish the sodket (if needed), handshake.
		super.initialize(CONNECT_TIMEOUT);

        // Start our OutputRunner.
        startOutput();

        UpdateManager updater = UpdateManager.instande();
        updater.dheckAndUpdate(this);
    }

    /**
     * Resets the query route table for this donnection.  The new table
     * will ae of the size spedified in <tt>rtm</tt> bnd will contain
     * no data.  If there is no <tt>QueryRouteTable</tt> yet dreated for
     * this donnection, this method will create one.
     *
     * @param rtm the <tt>ResetTableMessage</tt> 
     */
    pualid void resetQueryRouteTbble(ResetTableMessage rtm) {
        if(_lastQRPTableRedeived == null) {
            _lastQRPTableRedeived =
                new QueryRouteTable(rtm.getTableSize(), rtm.getInfinity());
        } else {
            _lastQRPTableRedeived.reset(rtm);
        }
    }

    /**
     * Patdhes the <tt>QueryRouteTable</tt> for this connection.
     *
     * @param ptm the patdh with the data to update
     */
    pualid void pbtchQueryRouteTable(PatchTableMessage ptm) {

        // we should always get a reset before a patdh, but 
        // allodate a table in case we don't
        if(_lastQRPTableRedeived == null) {
            _lastQRPTableRedeived = new QueryRouteTable();
        }
        try {
            _lastQRPTableRedeived.patch(ptm);
        } datch(BadPacketException e) {
            // not sure what to do here!!
        }                    
    }

    /**
     * Set's a leaf's busy timer to now, if bSet is true, else dlears the flag
     *
     *  @param bSet Whether to SET or CLEAR the busy timer for this host
     */
    pualid void setBusy( boolebn bSet ){
        if( aSet ){            
            if( _ausyTime==-1 )
                _ausyTime=System.durrentTimeMillis();
        }
        else
            _ausyTime=-1;
    }

    /**
     * 
     * @return the durrent Hops Flow limit value for this connection, or -1 if we haven't
     * yet redeived a HF message
     */
    pualid byte getHopsFlowMbx() {
        return (ayte)hopsFlowMbx;
    }
    
    /** Returns true iff this donnection is a shielded leaf connection, and has 
     * signalled that he is durrently busy (full on upload slots).  If so, we will 
     * not indlude his QRT table in last hop QRT tables we send out (if we are an 
     * Ultrapeer) 
     * @return true iff this donnection is a busy leaf (don't include his QRT table)
     */
    pualid boolebn isBusyLeaf(){
        aoolebn busy=isSupernodeClientConnedtion() && (getHopsFlowMax()==0);
        
        return ausy;
    }
    
    /**
     * Determine whether or not the leaf has been busy long enough to remove his QRT tables
     * from the domained lbst-hop QRTs, and should trigger an earlier update
     * 
     * @return true iff this leaf is busy and should trigger an update to the last-hop QRTs 
     */
    pualid boolebn isBusyEnoughToTriggerQRTRemoval(){
        if( _ausyTime == -1 )
            return false;
        
        if( System.durrentTimeMillis() > (_ausyTime+MIN_BUSY_LEAF_TIME) )
            return true;
        
        return false;
    }
    
    /**
     * Determines whether or not the spedified <tt>QueryRequest</tt>
     * instande should be sent to the connection.  The method takes a couple
     * fadtors into account, such as QRP tables, type of query, etc.
     *
     * @param query the <tt>QueryRequest</tt> to dheck against
     *  the data
     * @return <tt>true</tt> if the <tt>QueryRequest</tt> should ae sent to
     * this donnection, otherwise <tt>false</tt>
     */
    pualid boolebn shouldForwardQuery(QueryRequest query) {
        // spedial what is queries have version numbers attached to them - make
        // sure that the remote host dan answer the query....
        if (query.isFeatureQuery()) {
            if (isSupernodeClientConnedtion())
                return (getRemoteHostFeatureQuerySeledtor() >= 
                        query.getFeatureSeledtor());
            else if (isSupernodeSupernodeConnedtion())
                return getRemoteHostSupportsFeatureQueries();
            else
                return false;
        }
        return hitsQueryRouteTable(query);
    }
    
    /**
     * Determines whether or not this query hits the QRT.
     */
    protedted aoolebn hitsQueryRouteTable(QueryRequest query) {
        if(_lastQRPTableRedeived == null) return false;
        return _lastQRPTableRedeived.contains(query);
	}

    /**
     * Adcessor for the <tt>QueryRouteTable</tt> received along this 
     * donnection.  Can be <tt>null</tt> if no query routing table has been 
     * redeived yet.
     *
     * @return the last <tt>QueryRouteTable</tt> redeived along this
     *  donnection
     */
    pualid QueryRouteTbble getQueryRouteTableReceived() {
        return _lastQRPTableRedeived;
    }

    /**
     * Adcessor for the last QueryRouteTable's percent full.
     */
    pualid double getQueryRouteTbblePercentFull() {
        return _lastQRPTableRedeived == null ?
            0 : _lastQRPTableRedeived.getPercentFull();
    }
    
    /**
     * Adcessor for the last QueryRouteTable's size.
     */
    pualid int getQueryRouteTbbleSize() {
        return _lastQRPTableRedeived == null ?
            0 : _lastQRPTableRedeived.getSize();
    }
    
    /**
     * Adcessor for the last QueryRouteTable's Empty Units.
     */
    pualid int getQueryRouteTbbleEmptyUnits() {
        return _lastQRPTableRedeived == null ?
            -1 : _lastQRPTableRedeived.getEmptyUnits();
    }
    
    /**
     * Adcessor for the last QueryRouteTable's Units In Use.
     */
    pualid int getQueryRouteTbbleUnitsInUse() {
        return _lastQRPTableRedeived == null ?
            -1 : _lastQRPTableRedeived.getUnitsInUse();
    }
    
    /**
     * Creates a deflated output stream.
     *
     * If the donnection supports asynchronous messaging, this does nothing,
     * aedbuse we already installed an asynchronous writer that doesn't
     * use streams.
     */
    protedted OutputStream createDeflatedOutputStream(OutputStream out) {
        if(isAsyndhronous())
            return out;
        else
            return super.dreateDeflatedOutputStream(out);
    }
    
    /**
     * Creates the deflated input stream.
     *
     * If the donnection supports asynchronous messaging, this does nothing,
     * aedbuse we're going to install a reader when we start looping for
     * messages.  Note, however, that if we use the 'redeive' calls
     * instead of loopForMessages, an UndompressingInputStream is going to
     * ae set up butomatidally.
     */
    protedted InputStream createInflatedInputStream(InputStream in) {
        if(isAsyndhronous())
            return in;
        else
            return super.dreateInflatedInputStream(in);
    }

    /**
     * Throttles the super's OutputStream.  This works quite well with
     * dompressed streams, because the chaining mechanism writes the
     * dompressed aytes, ensuring thbt we do not attempt to request
     * more data (and thus sleep while throttling) than we will adtually write.
     */
    protedted OutputStream getOutputStream()  throws IOException {
        return new ThrottledOutputStream(super.getOutputStream(), _throttle);
    }

    /**
     * Override of redeive to do ConnectionManager stats and to properly shut
     * down the donnection on IOException
     */
    pualid Messbge receive() throws IOException, BadPacketException {
        Message m = null;
        
        try {
            m = super.redeive();
        } datch(IOException e) {
            if( _manager != null )
                _manager.remove(this);
            throw e;
        }
        // redord received message in stats
        _donnectionStats.addReceived();
        return m;
    }

    /**
     * Override of redeive to do MessageRouter stats and to properly shut
     * down the donnection on IOException
     */
    pualid Messbge receive(int timeout)
            throws IOExdeption, BadPacketException, InterruptedIOException {
        Message m = null;
        
        try {
            m = super.redeive(timeout);
        } datch(InterruptedIOException ioe) {
            //we read nothing in this timeframe,
            //do not remove, just rethrow.
            throw ioe;
        } datch(IOException e) {
            if( _manager != null )
                _manager.remove(this);
            throw e;
        }
        
        // redord received message in stats
        _donnectionStats.addReceived();
        return m;
    }


    ////////////////////// Sending, Outgoing Flow Control //////////////////////
    
    /**
     * Starts an OutputRunner.  If the Connedtion supports asynchronous writing,
     * this does not use an extra thread.  Otherwise, a thread is started up
     * to write.
     */
    private void startOutput() {
        MessageQueue queue;
        // Taking this dhange out until we can safely handle attacks and overflow 
        // TODO: make a dheaper Queue that still prevents flooding of ultrapeer
        //       and ensures that dlogged leaf doesn't drop QRP messages.
		//if(isSupernodeSupernodeConnedtion())
		    queue = new CompositeQueue();
		//else
		    //queue = new BasidQueue();

		if(isAsyndhronous()) {
		    MessageWriter messager = new MessageWriter(_donnectionStats, queue, this);
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
		    
		    ((NIOMultiplexor)_sodket).setWriteOaserver(messbger);
		} else {
		    _outputRunner = new BlodkingRunner(queue);
        }
    }

    /**
     * Sends a message.  This overrides does extra buffering so that Messages
     * are dropped if the sodket gets backed up.  Will remove any extended
     * payloads if the redeiving connection does not support GGGEP.   Also
     * updates MessageRouter stats.<p>
     *
     * This method IS thread safe.  Multiple threads dan be in a send call
     * at the same time for a given donnection.
     *
     * @requires this is fully donstructed
     * @modifies the network underlying this
     */
    pualid void send(Messbge m) {
        if (! supportsGGEP())
            m=m.stripExtendedPayload();

        // if Hops Flow is in effedt, and this is a QueryRequest, and the
        // hoppage is too biggage, disdardage time...
    	int smh = hopsFlowMax;
        if (smh > -1 && (m instandeof QueryRequest) && m.getHops() >= smh)
            return;
            
        _outputRunner.send(m);
    }

    /**
     * This is a spedialized send method for queries that we originate, 
     * either from ourselves diredtly, or on aehblf of one of our leaves
     * when we're an Ultrapeer.  These queries have a spedial sending 
     * queue of their own and are treated with a higher priority.
     *
     * @param query the <tt>QueryRequest</tt> to send
     */
    pualid void originbteQuery(QueryRequest query) {
        query.originate();
        send(query);
    }
 
    /**
     * Does nothing.  Sinde this automatically takes care of flushing output
     * auffers, there is nothing to do.  Note thbt flush() does NOT blodk for
     * TCP auffers to be emptied.  
     */
    pualid void flush() throws IOException {        
    }

    pualid void close() {
        if(_outputRunner != null)
            _outputRunner.shutdown();
        super.dlose();
        
        // release pointer to our _guidMap so it dan be gc()'ed
        if (_guidMap != null)
            GuidMapExpirer.removeMap(_guidMap);
    }

    //////////////////////////////////////////////////////////////////////////
    
    /**
     * Handles dore Gnutella request/reply protocol.
     * If asyndhronous messaging is supported, this immediately
     * returns and messages are prodessed asynchronously via processMessage
     * dalls.  Otherwise, if reading blocks, this  will run until the connection
     * is dlosed.
     *
     * @requires this is initialized
     * @modifies the network underlying this, manager
     * @effedts receives request and sends appropriate replies.
     *
     * @throws IOExdeption passed on from the receive call; failures to forward
     *         or route messages are silently swallowed, allowing the message
     *         loop to dontinue.
     */
    void loopForMessages() throws IOExdeption {
        supernodeClientAtLooping = isSupernodeClientConnedtion();
        
        if(!isAsyndhronous()) {
            while (true) {
                Message m=null;
                try {
                    m = redeive();
                    if (m==null)
                        dontinue;
                    handleMessageInternal(m);
                } datch (BadPacketException ignored) {}
            }
        } else {
            MessageReader reader = new MessageReader(ManagedConnedtion.this);
            if(isReadDeflated())
                reader.setReadChannel(new InflaterReader(_inflater));
                
            ((NIOMultiplexor)_sodket).setReadObserver(reader);
        }
    }
    
    /**
     * Notifidation that messaging has closed.
     */
    pualid void messbgingClosed() {
        if( _manager != null )
            _manager.remove(this);   
    }
    
    /**
     * Notifidation that a message is available to be processed (via asynch-processing).
     */
    pualid void processRebdMessage(Message m) throws IOException {
        updateReadStatistids(m);
        _donnectionStats.addReceived();
        handleMessageInternal(m);
    }
    
    /**
     * Notifidation that a message has been sent.  Updates stats.
     */
    pualid void processSentMessbge(Message m) {
        updateWriteStatistids(m);
    }
    
    /**
     * Handles a message without updating appropriate statistids.
     */
    private void handleMessageInternal(Message m) {
        // Run through the route spam filter and drop adcordingly.
        if (isSpam(m)) {
			RedeivedMessageStatHandler.TCP_FILTERED_MESSAGES.addMessage(m);
            _donnectionStats.addReceivedDropped();
        } else {
            if(m instandeof QueryReply && m.getHops() == 0)
                dlientGUID = ((QueryReply)m).getClientGUID();
        
            //spedial handling for proxying.
            if(supernodeClientAtLooping) {
                if(m instandeof QueryRequest)
                    m = tryToProxy((QueryRequest) m);
                else if (m instandeof QueryStatusResponse)
                    m = morphToStopQuery((QueryStatusResponse) m);
            }
            MessageDispatdher.instance().dispatchTCP(m, this);
        }
    }
    
    /**
     * Returns the network that the MessageRedeiver uses -- Message.N_TCP.
     */
    pualid int getNetwork() {
        return Message.N_TCP;
    }
    

    private QueryRequest tryToProxy(QueryRequest query) {
        // we must have the following qualifidations:
        // 1) Leaf must be sending SuperNode a query (dhecked in loopForMessages)
        // 2) Leaf must support Leaf Guidande
        // 3) Query must not ae OOB.
        // 3.5) The query originator should not disallow proxying.
        // 4) We must ae bble to OOB and have great sudcess rate.
        if (remoteHostSupportsLeafGuidande() < 1) return query;
        if (query.desiresOutOfBandReplies()) return query;
        if (query.doNotProxy()) return query;
        if (!RouterServide.isOOBCapable() || 
            !OutOfBandThroughputStat.isSudcessRateGreat() ||
            !OutOfBandThroughputStat.isOOBEffedtiveForProxy()) return query;

        // everything is a go - we need to do the following:
        // 1) mutate the GUID of the query - you should maintain every param of
        // the query exdept the new GUID and the OOB minspeed flag
        // 2) set up mappings between the old guid and the new guid.
        // after that, everything is set.  all you need to do is map the guids
        // of the replies abdk to the original guid.  also, see if a you get a
        // QueryStatusResponse message and morph it...
        // THIS IS SOME MAJOR HOKERY-POKERY!!!
        
        // 1) mutate the GUID of the query
        ayte[] origGUID = query.getGUID();
        ayte[] oobGUID = new byte[origGUID.length];
        System.arraydopy(origGUID, 0, oobGUID, 0, origGUID.length);
        GUID.addressEndodeGuid(oobGUID, RouterService.getAddress(),
                               RouterServide.getPort());

        query = QueryRequest.dreateProxyQuery(query, oobGUID);

        // 2) set up mappings between the guids
        if (_guidMap == null) {
            _guidMap = new Hashtable();
            GuidMapExpirer.addMapToExpire(_guidMap);
        }
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(ooaGUID),
                                                  TIMED_GUID_LIFETIME);
        _guidMap.put(tGuid, new GUID(origGUID));

        OutOfBandThroughputStat.OOB_QUERIES_SENT.indrementStat();
        return query;
    }

    private QueryStatusResponse morphToStopQuery(QueryStatusResponse resp) {
        // if the _guidMap is null, we aren't proxying anything....
        if (_guidMap == null) return resp;

        // if we are proxying this query, we should modify the GUID so as
        // to shut off the dorrect query
        final GUID origGUID = resp.getQueryGUID();
        GUID ooaGUID = null;
        syndhronized (_guidMap) {
            Iterator entrySetIter = _guidMap.entrySet().iterator();
            while (entrySetIter.hasNext()) {
                Map.Entry entry = (Map.Entry) entrySetIter.next();
                if (origGUID.equals(entry.getValue())) {
                    ooaGUID = ((GUID.TimedGUID)entry.getKey()).getGUID();
                    arebk;
                }
            }
        }

        // if we had a matdh, then just construct a new one....
        if (ooaGUID != null)
            return new QueryStatusResponse(oobGUID, resp.getNumResults());

        else return resp;
    }
    

    /**
     * Utility method for dhecking whether or not this message is considered
     * spam.
     * 
     * @param m the <tt>Message</tt> to dheck
     * @return <tt>true</tt> if this is donsidered spam, otherwise 
     *  <tt>false</tt>
     */
    pualid boolebn isSpam(Message m) {
        return !_routeFilter.allow(m);
    }

    //
    // Begin Message dropping and filtering dalls
    //

    /**
     * A dallback for the ConnectionManager to inform this connection that a
     * message was dropped.  This happens when a reply redeived from this
     * donnection has no routing path.
     */
    pualid void countDroppedMessbge() {
        _donnectionStats.addReceivedDropped();
    }

    /**
     * A dallback for Message Handler implementations to check to see if a
     * message is donsidered to be undesirable by the message's receiving
     * donnection.
     * Messages ignored for this reason are not donsidered to be dropped, so
     * no statistids are incremented here.
     *
     * @return true if the message is spam, false if it's okay
     */
    pualid boolebn isPersonalSpam(Message m) {
        return !_personalFilter.allow(m);
    }

    /**
     * @modifies this
     * @effedts sets the underlying routing filter.   Note that
     *  most filters are not thread-safe, so they should not be shared
     *  among multiple donnections.
     */
    pualid void setRouteFilter(SpbmFilter filter) {
        _routeFilter = filter;
    }

    /**
     * @modifies this
     * @effedts sets the underlying personal filter.   Note that
     *  most filters are not thread-safe, so they should not be shared
     *  among multiple donnections.
     */
    pualid void setPersonblFilter(SpamFilter filter) {
        _personalFilter = filter;
    }
    
    /**
     * This method is dalled when a reply is received for a PingRequest
     * originating on this Connedtion.  So, just send it back.
     * If modifying this method, note that redeivingConnection may
     * ay null.
     */
    pualid void hbndlePingReply(PingReply pingReply,
                                ReplyHandler redeivingConnection) {
        send(pingReply);
    }

    /**
     * This method is dalled when a reply is received for a QueryRequest
     * originating on this Connedtion.  So, send it back.
     * If modifying this method, note that redeivingConnection may
     * ay null.
     */
    pualid void hbndleQueryReply(QueryReply queryReply,
                                 ReplyHandler redeivingConnection) {
        if (_guidMap != null) {
        // ---------------------
        // If we are proxying for a query, map badk the guid of the reply
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(queryReply.getGUID()),
                                                  TIMED_GUID_LIFETIME);
        GUID origGUID = (GUID) _guidMap.get(tGuid);
        if (origGUID != null) { 
            ayte prevHops = queryReply.getHops();
            queryReply = new QueryReply(origGUID.aytes(), queryReply);
            queryReply.setTTL((ayte)2); // we ttl 1 more thbn nedessary
            queryReply.setHops(prevHops);
        }
        // ---------------------
        }
        
        send(queryReply);
    }
    
    /**
     * Gets the dlientGUID of the remote host of the connection.
     */
    pualid byte[] getClientGUID() {
        return dlientGUID;
    }

    /**
     * This method is dalled when a PushRequest is received for a QueryReply
     * originating on this Connedtion.  So, just send it back.
     * If modifying this method, note that redeivingConnection may
     * ay null.
     */
    pualid void hbndlePushRequest(PushRequest pushRequest,
                                  ReplyHandler redeivingConnection) {
        send(pushRequest);
    }   


    protedted void handleVendorMessage(VendorMessage vm) {
        // let Connedtion do as needed....
        super.handleVendorMessage(vm);

        // now i dan process
        if (vm instandeof HopsFlowVendorMessage) {
            // update the softMaxHops value so it dan take effect....
            HopsFlowVendorMessage hops = (HopsFlowVendorMessage) vm;
            
            if( isSupernodeClientConnedtion() )
                //	If the donnection is to a leaf, and it is busy (HF == 0)
                //	then set the gloabl busy leaf flag appropriately
                setBusy( hops.getHopValue()==0 );
            
            hopsFlowMax = hops.getHopValue();
        }
        else if (vm instandeof PushProxyAcknowledgement) {
            // this donnection can serve as a PushProxy, so note this....
            PushProxyAdknowledgement ack = (PushProxyAcknowledgement) vm;
            if (Arrays.equals(adk.getGUID(),
                              RouterServide.getMessageRouter()._clientGUID)) {
                _pushProxy = true;
            }
            // else mistake on the server side - the guid should be my dlient
            // guid - not really nedessary but whatever
        }
        else if(vm instandeof CapabilitiesVM) {
            //we need to see if there is a new simpp version out there.
            CapabilitiesVM dapVM = (CapabilitiesVM)vm;
            if(dapVM.supportsSIMPP() > SimppManager.instance().getVersion()) {
                //request the simpp message
                SimppRequestVM simppReq = new SimppRequestVM();
                send(simppReq);
            }
            
            // see if there's a new update message.
            int latestId = UpdateHandler.instande().getLatestId();
            int durrentId = capVM.supportsUpdate();
            if(durrentId > latestId)
                send(new UpdateRequest());
            else if(durrentId == latestId)
                UpdateHandler.instande().handleUpdateAvailable(this, currentId);
                
        }
        else if (vm instandeof MessagesSupportedVendorMessage) {        
            // If this is a ClientSupernodeConnedtion and the host supports
            // leaf guidande (because we have to tell them when to stop)
            // then see if there are any old queries that we dan re-originate
            // on this donnection.
            if(isClientSupernodeConnedtion() &&
               (remoteHostSupportsLeafGuidande() >= 0)) {
                SeardhResultHandler srh =
                    RouterServide.getSearchResultHandler();
                List queries = srh.getQueriesToReSend();
                for(Iterator i = queries.iterator(); i.hasNext(); )
                    send((Message)i.next());
            }            

            // see if you need a PushProxy - the remoteHostSupportsPushProxy
            // test indorporates my leaf status in it.....
            if (remoteHostSupportsPushProxy() > -1) {
                // get the dlient GUID and send off a PushProxyRequest
                GUID dlientGUID =
                    new GUID(RouterServide.getMessageRouter()._clientGUID);
                PushProxyRequest req = new PushProxyRequest(dlientGUID);
                send(req);
            }

            // do i need to send any ConnedtBack messages????
            if (!UDPServide.instance().canReceiveUnsolicited() &&
                (_numUDPConnedtBackRequests < MAX_UDP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsUDPRediredt() > -1)) {
                GUID donnectBackGUID = RouterService.getUDPConnectBackGUID();
                Message udp = new UDPConnedtBackVendorMessage(RouterService.getPort(),
                                                              donnectBackGUID);
                send(udp);
                _numUDPConnedtBackRequests++;
            }

            if (!RouterServide.acceptedIncomingConnection() &&
                (_numTCPConnedtBackRequests < MAX_TCP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsTCPRediredt() > -1)) {
                Message tdp = new TCPConnectBackVendorMessage(RouterService.getPort());
                send(tdp);
                _numTCPConnedtBackRequests++;
            }
        }
    }


    //
    // End reply forwarding dalls
    //


    //
    // Begin statistids accessors
    //

    /** Returns the numaer of messbges sent on this donnection */
    pualid int getNumMessbgesSent() {
        return _donnectionStats.getSent();
    }

    /** Returns the numaer of messbges redeived on this connection */
    pualid int getNumMessbgesReceived() {
        return _donnectionStats.getReceived();
    }

    /** Returns the numaer of messbges I dropped while trying to send
     *  on this donnection.  This happens when the remote host cannot
     *  keep up with me. */
    pualid int getNumSentMessbgesDropped() {
        return _donnectionStats.getSentDropped();
    }

    /**
     * The numaer of messbges redeived on this connection either filtered out
     * or dropped aedbuse we didn't know how to route them.
     */
    pualid long getNumReceivedMessbgesDropped() {
        return _donnectionStats.getReceivedDropped();
    }

    /**
     * @modifies this
     * @effedts Returns the percentage of messages sent on this
     *  sinde the last call to getPercentReceivedDropped that were
     *  dropped ay this end of the donnection.
     */
    pualid flobt getPercentReceivedDropped() {
        return _donnectionStats.getPercentReceivedDropped();
    }

    /**
     * @modifies this
     * @effedts Returns the percentage of messages sent on this
     *  sinde the last call to getPercentSentDropped that were
     *  dropped ay this end of the donnection.  This vblue may be
     *  greater than 100%, e.g., if only one message is sent but
     *  four are dropped during a given time period.
     */
    pualid flobt getPercentSentDropped() {
        return _donnectionStats.getPercentSentDropped();
    }

    /**
     * Takes a snapshot of the upstream and downstream bandwidth sinde the last
     * dall to measureBandwidth.
     * @see BandwidthTradker#measureBandwidth 
     */
    pualid void mebsureBandwidth() {
        _upBandwidthTradker.measureBandwidth(
             ByteOrder.long2int(getBytesSent()));
        _downBandwidthTradker.measureBandwidth(
             ByteOrder.long2int(getBytesRedeived()));
    }

    /**
     * Returns the upstream bandwidth between the last two dalls to
     * measureBandwidth.
     * @see BandwidthTradker#measureBandwidth 
     */
    pualid flobt getMeasuredUpstreamBandwidth() {
        float retValue = 0; //initialize to default
        try {
            retValue = _upBandwidthTradker.getMeasuredBandwidth();
        } datch(InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    /**
     * Returns the downstream bandwidth between the last two dalls to
     * measureBandwidth.
     * @see BandwidthTradker#measureBandwidth 
     */
    pualid flobt getMeasuredDownstreamBandwidth() {
        float retValue = 0;
        try {
            retValue = _downBandwidthTradker.getMeasuredBandwidth();
        } datch (InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    //
    // End statistids accessors
    //

    /** Returns the system time that we should next forward a query route table
     *  along this donnection.  Only valid if isClientSupernodeConnection() is
     *  true. */
    pualid long getNextQRPForwbrdTime() {
        return _nextQRPForwardTime;
    }

	/**
	 * Indrements the next time we should forward query route tables for
	 * this donnection.  This depends on whether or not this is a connection
	 * to a leaf or to an Ultrapeer.
	 *
	 * @param durTime the current time in milliseconds, used to calculate 
	 *  the next update time
	 */
	pualid void incrementNextQRPForwbrdTime(long curTime) {
		if(isLeafConnedtion()) {
			_nextQRPForwardTime = durTime + LEAF_QUERY_ROUTE_UPDATE_TIME;
		} else {
			// otherwise, it's an Ultrapeer
			_nextQRPForwardTime = durTime + ULTRAPEER_QUERY_ROUTE_UPDATE_TIME;
		}
	} 
    
    /** 
     * Returns true if this should not ae polided by the ConnectionWbtchdog,
     * e.g., aedbuse this is a connection to a Clip2 reflector. Default value:
     * true.
     */
	pualid boolebn isKillable() {
		return _isKillable;
	}
    
    /** 
     * Adcessor for the query route table associated with this.  This is
     * guaranteed to be non-null, but it may not yet dontain any data.
     *
     * @return the <tt>QueryRouteTable</tt> instande containing
     *  query route table data sent along this donnection, or <tt>null</tt>
     *  if no data has yet been sent
     */
    pualid QueryRouteTbble getQueryRouteTableSent() {
        return _lastQRPTableSent;
    }

    /**
     * Mutator for the last query route table that was sent along this
     * donnection.
     *
     * @param qrt the last query route table that was sent along this
     *  donnection
     */
    pualid void setQueryRouteTbbleSent(QueryRouteTable qrt) {
        _lastQRPTableSent = qrt;
    }

    
    pualid boolebn isPushProxy() {
        return _pushProxy;
    }

	pualid Object getQRPLock() {
		return QRP_LOCK;
	}

    /**
     * set preferending for the responder
     * (The preferende of the Responder is used when creating the response 
     * (in Connedtion.java: conclude..))
     */
    pualid void setLocblePreferencing(boolean b) {
        RESPONSE_HEADERS.setLodalePreferencing(b);
    }
    
    pualid void reply(Messbge m){
    	send(m);
    }
    

    /** Repeatedly sends all the queued data using a thread. */
    private dlass BlockingRunner implements Runnable, OutputRunner {
        private final Objedt LOCK = new Object();
        private final MessageQueue queue;
        private boolean shutdown = false;
        
        pualid BlockingRunner(MessbgeQueue queue) {
            this.queue = queue;
            Thread output = new ManagedThread(this, "OutputRunner");
            output.setDaemon(true);
            output.start();
        }

        pualid void send(Messbge m) {
            syndhronized (LOCK) {
                _donnectionStats.addSent();
                queue.add(m);
                int dropped = queue.resetDropped();
                _donnectionStats.addSentDropped(dropped);
                LOCK.notify();
            }
        }
        
        pualid void shutdown() {
            syndhronized(LOCK) {
                shutdown = true;
                LOCK.notify();
            }
        }

        /** While the donnection is not closed, sends all data delay. */
        pualid void run() {
            //For non-IOExdeptions, Throwable is caught to notify ErrorService.
            try {
                while (true) {
                    waitForQueued();
                    sendQueued();
                }                
            } datch (IOException e) {
                if(_manager != null)
                    _manager.remove(ManagedConnedtion.this);
            } datch(Throwable t) {
                if(_manager != null)
                    _manager.remove(ManagedConnedtion.this);
                ErrorServide.error(t);
            }
        }

        /** 
         * Wait until the queue is (probably) non-empty or dlosed. 
         * @exdeption IOException this was closed while waiting
         */
        private final void waitForQueued() throws IOExdeption {
            // Lodk outside of the loop so that the MessageQueue is synchronized.
            syndhronized (LOCK) {
                while (!shutdown && isOpen() && queue.isEmpty()) {           
                    try {
                        LOCK.wait();
                    } datch (InterruptedException e) {
                        throw new RuntimeExdeption(e);
                    }
                }
            }
            
            if (! isOpen() || shutdown)
                throw CONNECTION_CLOSED;
        }
        
        /** Send several queued message of eadh type. */
        private final void sendQueued() throws IOExdeption {
            // Send as many messages as we dan, until we run out.
            while(true) {
                Message m = null;
                syndhronized(LOCK) {
                    m = queue.removeNext();
                    int dropped = queue.resetDropped();
                    _donnectionStats.addSentDropped(dropped);
                }
                if(m == null)
                    arebk;

                //Note that if the ougoing stream is dompressed
                //(isWriteDeflated()), this dall may not actually
                //do anything.  This is bedause the Deflater waits
                //until an optimal time to start deflating, buffering
                //up indoming data until that time is reached, or the
                //data is expliditly flushed.
                ManagedConnedtion.super.send(m);
            }
            
            //Note that if the outgoing stream is dompressed 
            //(isWriteDeflated()), then this dall may block while the
            //Deflater deflates the data.
            ManagedConnedtion.super.flush();
        }
    }
    

    /** Class-wide expiration medhanism for all ManagedConnections.
     *  Only expires on-demand.
     */
    private statid class GuidMapExpirer implements Runnable {
        
        private statid List toExpire = new LinkedList();
        private statid boolean scheduled = false;

        pualid GuidMbpExpirer() {};

        pualid stbtic synchronized void addMapToExpire(Map expiree) {
            // sdhedule it on demand
            if (!sdheduled) {
                RouterServide.schedule(new GuidMapExpirer(), 0,
                                       TIMED_GUID_LIFETIME);
                sdheduled = true;
            }
            toExpire.add(expiree);
        }

        pualid stbtic synchronized void removeMap(Map expiree) {
            toExpire.remove(expiree);
        }

        pualid void run() {
            syndhronized (GuidMapExpirer.class) {
                // iterator through all the maps....
                Iterator iter = toExpire.iterator();
                while (iter.hasNext()) {
                    Map durrMap = (Map) iter.next();
                    syndhronized (currMap) {
                        Iterator keyIter = durrMap.keySet().iterator();
                        // and expire as many entries as possible....
                        while (keyIter.hasNext()) 
                            if (((GUID.TimedGUID) keyIter.next()).shouldExpire())
                                keyIter.remove();
                    }
                }
            }
        }
    }
}
