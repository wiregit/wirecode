pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.InterruptedIOException;
import jbva.io.OutputStream;
import jbva.io.InputStream;
import jbva.net.Socket;
import jbva.util.Arrays;
import jbva.util.Hashtable;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Properties;

import com.limegroup.gnutellb.io.NIOMultiplexor;
import com.limegroup.gnutellb.io.Throttle;
import com.limegroup.gnutellb.io.NBThrottle;
import com.limegroup.gnutellb.io.ThrottleWriter;
import com.limegroup.gnutellb.io.DelayedBufferWriter;
import com.limegroup.gnutellb.io.ChannelWriter;
import com.limegroup.gnutellb.connection.*;
import com.limegroup.gnutellb.filters.SpamFilter;
import com.limegroup.gnutellb.handshaking.*;
import com.limegroup.gnutellb.messages.*;
import com.limegroup.gnutellb.messages.vendor.*;
import com.limegroup.gnutellb.routing.PatchTableMessage;
import com.limegroup.gnutellb.routing.QueryRouteTable;
import com.limegroup.gnutellb.routing.ResetTableMessage;
import com.limegroup.gnutellb.search.SearchResultHandler;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.simpp.SimppManager;
import com.limegroup.gnutellb.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutellb.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutellb.updates.UpdateManager;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.BandwidthThrottle;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.ThrottledOutputStream;
import com.limegroup.gnutellb.version.UpdateHandler;

/**
 * A Connection mbnaged by a ConnectionManager.  Includes a loopForMessages
 * method thbt runs forever (or until an IOException occurs), receiving and
 * replying to Gnutellb messages.  ManagedConnection is only instantiated
 * through b ConnectionManager.<p>
 *
 * MbnagedConnection provides a sophisticated message buffering mechanism.  When
 * you cbll send(Message), the message is not actually delivered to the socket;
 * instebd it buffered in an application-level buffer.  Periodically, a thread
 * rebds messages from the buffer, writes them to the network, and flushes the
 * socket buffers.  This mebns that there is no need to manually call flush().
 * Furthermore, MbnagedConnection provides a simple form of flow control.  If
 * messbges are queued faster than they can be written to the network, they are
 * dropped in the following order: PingRequest, PingReply, QueryRequest, 
 * QueryReply, bnd PushRequest.  See the implementation notes below for more
 * detbils.<p>
 *
 * All MbnagedConnection's have two underlying spam filters: a personal filter
 * (controls whbt I see) and a route filter (also controls what I pass along to
 * others).  See SpbmFilter for a description.  These filters are configured by
 * the properties in the SettingsMbnager, but you can change them with
 * setPersonblFilter and setRouteFilter.<p>
 *
 * MbnagedConnection maintain a large number of statistics, such as the current
 * bbndwidth for upstream & downstream.  ManagedConnection doesn't quite fit the
 * BbndwidthTracker interface, unfortunately.  On the query-routing3-branch and
 * pong-cbching CVS branches, these statistics have been bundled into a single
 * object, reducing the complexity of MbnagedConnection.<p>
 * 
 * MbnagedConnection also takes care of various VendorMessage handling, in
 * pbrticular Hops Flow, UDP ConnectBack, and TCP ConnectBack.  See
 * hbndleVendorMessage().<p>
 *
 * This clbss implements ReplyHandler to route pongs and query replies that
 * originbted from it.<p> 
 */
public clbss ManagedConnection extends Connection 
	implements ReplyHbndler, MessageReceiver, SentMessageHandler {

    /** 
     * The time to wbit between route table updates for leaves, 
	 * in milliseconds. 
     */
    privbte long LEAF_QUERY_ROUTE_UPDATE_TIME = 1000*60*5; //5 minutes
    
    /** 
     * The time to wbit between route table updates for Ultrapeers, 
	 * in milliseconds. 
     */
    privbte long ULTRAPEER_QUERY_ROUTE_UPDATE_TIME = 1000*60; //1 minute


    /** The timeout to use when connecting, in milliseconds.  This is NOT used
     *  for bootstrbp servers.  */
    privbte static final int CONNECT_TIMEOUT = 6000;  //6 seconds

    /** The totbl amount of upstream messaging bandwidth for ALL connections
     *  in BYTES (not bits) per second. */
    privbte static final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH=8000;

    /** The mbximum number of times ManagedConnection instances should send UDP
     *  ConnectBbck requests.
     */
    privbte static final int MAX_UDP_CONNECT_BACK_ATTEMPTS = 15;

    /** The mbximum number of times ManagedConnection instances should send TCP
     *  ConnectBbck requests.
     */
    privbte static final int MAX_TCP_CONNECT_BACK_ATTEMPTS = 10;

	/** Hbndle to the <tt>ConnectionManager</tt>.
	 */
    privbte ConnectionManager _manager;

	/** Filter for filtering out messbges that are considered spam.
	 */
    privbte volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    privbte volatile SpamFilter _personalFilter =
        SpbmFilter.newPersonalFilter();

    /*
     * IMPLEMENTATION NOTE: this clbss uses the SACHRIFC algorithm described at
     * http://www.limewire.com/developer/sbchrifc.txt.  The basic idea is to use
     * one queue for ebch message type.  Messages are removed from the queue in
     * b biased round-robin fashion.  This prioritizes some messages types while
     * preventing bny one message type from dominating traffic.  Query replies
     * bre further prioritized by "GUID volume", i.e., the number of bytes
     * blready routed for that GUID.  Other messages are sorted by time and
     * removed in b LIFO [sic] policy.  This, coupled with timeouts, reduces
     * lbtency.  
     */

    /** A lock for QRP bctivity on this connection */
    privbte final Object QRP_LOCK=new Object();
    
    /** Non-blocking throttle for outgoing messbges. */
    privbte final static Throttle _nbThrottle = new NBThrottle(true,
                                                       TOTAL_OUTGOING_MESSAGING_BANDWIDTH,
                                                       ConnectionSettings.NUM_CONNECTIONS.getVblue(),
                                                       CompositeQueue.QUEUE_TIME);
                                                            
    /** Blocking throttle for outgoing messbges. */
    privbte final static BandwidthThrottle _throttle=
        new BbndwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);
        
    
    /** The OutputRunner */
    privbte OutputRunner _outputRunner;
    
    /** Keeps trbck of sent/received [dropped] & bandwidth. */
    privbte final ConnectionStats _connectionStats = new ConnectionStats();
    
    /**
     * The minimum time b leaf needs to be in "busy mode" before we will consider him "truly
     * busy" for the purposes of QRT updbtes.
     */
    privbte static long MIN_BUSY_LEAF_TIME = 1000 * 20;   //  20 seconds

    /** The next time I should send b query route table to this connection.
	 */
    privbte long _nextQRPForwardTime;
    
    /** 
     * The bbndwidth trackers for the up/downstream.
     * These bre not synchronized and not guaranteed to be 100% accurate.
     */
    privbte BandwidthTrackerImpl _upBandwidthTracker=
        new BbndwidthTrackerImpl();
    privbte BandwidthTrackerImpl _downBandwidthTracker=
        new BbndwidthTrackerImpl();

    /** True iff this should not be policed by the ConnectionWbtchdog, e.g.,
     *  becbuse this is a connection to a Clip2 reflector. */
    privbte boolean _isKillable=true;
   
    /** Use this if b HopsFlowVM instructs us to stop sending queries below
     *  this certbin hops value....
     */
    privbte volatile int hopsFlowMax = -1;

    /**
     * This member contbins the time beyond which, if this host is still busy (hops flow==0),
     * thbt we should consider him as "truly idle" and should then remove his contributions
     * lbst-hop QRTs.  A value of -1 means that either the leaf isn't busy, or he is busy,
     * bnd his busy-ness was already noticed by the MessageRouter, so we shouldn't 're-notice'
     * him on the next QRT updbte iteration.
     */
    privbte volatile long _busyTime = -1;

    /**
     * whether this connection is b push proxy for somebody
     */
    privbte volatile boolean _pushProxy;

    /** The clbss wide static counter for the number of udp connect back 
     *  request sent.
     */
    privbte static int _numUDPConnectBackRequests = 0;

    /** The clbss wide static counter for the number of tcp connect back 
     *  request sent.
     */
    privbte static int _numTCPConnectBackRequests = 0;

    /**
     * Vbriable for the <tt>QueryRouteTable</tt> received for this 
     * connection.
     */
    privbte QueryRouteTable _lastQRPTableReceived;

    /**
     * Vbriable for the <tt>QueryRouteTable</tt> sent for this 
     * connection.
     */
    privbte QueryRouteTable _lastQRPTableSent;

    /**
     * Holds the mbppings of GUIDs that are being proxied.
     * We wbnt to construct this lazily....
     * GUID.TimedGUID -> GUID
     * OOB Proxy GUID - > Originbl GUID
     */
    privbte Map _guidMap = null;

    /**
     * The mbx lifetime of the GUID (10 minutes).
     */
    privbte static long TIMED_GUID_LIFETIME = 10 * 60 * 1000;

    /**
     * Whether or not this wbs a supernode <-> client connection when message
     * looping stbrted.
     */
    privbte boolean supernodeClientAtLooping = false;
    
    /**
     * The lbst clientGUID a Hops=0 QueryReply had.
     */
    privbte byte[] clientGUID = DataUtils.EMPTY_GUID;

    /**
     * Crebtes a new outgoing connection to the specified host on the
	 * specified port.  
	 *
	 * @pbram host the address of the host we're connecting to
	 * @pbram port the port the host is listening on
     */
    public MbnagedConnection(String host, int port) {
        this(host, port, 
			 (RouterService.isSupernode() ? 
			  (Properties)(new UltrbpeerHeaders(host)) : 
			  (Properties)(new LebfHeaders(host))),
			 (RouterService.isSupernode() ?
			  (HbndshakeResponder)new UltrapeerHandshakeResponder(host) :
			  (HbndshakeResponder)new LeafHandshakeResponder(host)));
    }

	/**
	 * Crebtes a new <tt>ManagedConnection</tt> with the specified 
	 * hbndshake classes and the specified host and port.
	 */
	privbte ManagedConnection(String host, int port, 
							  Properties props, 
							  HbndshakeResponder responder) {	
        super(host, port, props, responder);        
        _mbnager = RouterService.getConnectionManager();		
	}

    /**
     * Crebtes an incoming connection.
     * MbnagedConnections should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " bnd nothing else has just been read
     *  from socket
     * @effects wrbps a connection around socket and does the rest of the
     *  Gnutellb handshake.
     */
    MbnagedConnection(Socket socket) {
        super(socket, 
			  RouterService.isSupernode() ? 
			  (HbndshakeResponder)(new UltrapeerHandshakeResponder(
			      socket.getInetAddress().getHostAddress())) : 
			  (HbndshakeResponder)(new LeafHandshakeResponder(
				  socket.getInetAddress().getHostAddress())));
        _mbnager = RouterService.getConnectionManager();
    }



    public void initiblize()
            throws IOException, NoGnutellbOkException, BadHandshakeException {
        //Estbblish the socket (if needed), handshake.
		super.initiblize(CONNECT_TIMEOUT);

        // Stbrt our OutputRunner.
        stbrtOutput();

        UpdbteManager updater = UpdateManager.instance();
        updbter.checkAndUpdate(this);
    }

    /**
     * Resets the query route tbble for this connection.  The new table
     * will be of the size specified in <tt>rtm</tt> bnd will contain
     * no dbta.  If there is no <tt>QueryRouteTable</tt> yet created for
     * this connection, this method will crebte one.
     *
     * @pbram rtm the <tt>ResetTableMessage</tt> 
     */
    public void resetQueryRouteTbble(ResetTableMessage rtm) {
        if(_lbstQRPTableReceived == null) {
            _lbstQRPTableReceived =
                new QueryRouteTbble(rtm.getTableSize(), rtm.getInfinity());
        } else {
            _lbstQRPTableReceived.reset(rtm);
        }
    }

    /**
     * Pbtches the <tt>QueryRouteTable</tt> for this connection.
     *
     * @pbram ptm the patch with the data to update
     */
    public void pbtchQueryRouteTable(PatchTableMessage ptm) {

        // we should blways get a reset before a patch, but 
        // bllocate a table in case we don't
        if(_lbstQRPTableReceived == null) {
            _lbstQRPTableReceived = new QueryRouteTable();
        }
        try {
            _lbstQRPTableReceived.patch(ptm);
        } cbtch(BadPacketException e) {
            // not sure whbt to do here!!
        }                    
    }

    /**
     * Set's b leaf's busy timer to now, if bSet is true, else clears the flag
     *
     *  @pbram bSet Whether to SET or CLEAR the busy timer for this host
     */
    public void setBusy( boolebn bSet ){
        if( bSet ){            
            if( _busyTime==-1 )
                _busyTime=System.currentTimeMillis();
        }
        else
            _busyTime=-1;
    }

    /**
     * 
     * @return the current Hops Flow limit vblue for this connection, or -1 if we haven't
     * yet received b HF message
     */
    public byte getHopsFlowMbx() {
        return (byte)hopsFlowMbx;
    }
    
    /** Returns true iff this connection is b shielded leaf connection, and has 
     * signblled that he is currently busy (full on upload slots).  If so, we will 
     * not include his QRT tbble in last hop QRT tables we send out (if we are an 
     * Ultrbpeer) 
     * @return true iff this connection is b busy leaf (don't include his QRT table)
     */
    public boolebn isBusyLeaf(){
        boolebn busy=isSupernodeClientConnection() && (getHopsFlowMax()==0);
        
        return busy;
    }
    
    /**
     * Determine whether or not the lebf has been busy long enough to remove his QRT tables
     * from the combined lbst-hop QRTs, and should trigger an earlier update
     * 
     * @return true iff this lebf is busy and should trigger an update to the last-hop QRTs 
     */
    public boolebn isBusyEnoughToTriggerQRTRemoval(){
        if( _busyTime == -1 )
            return fblse;
        
        if( System.currentTimeMillis() > (_busyTime+MIN_BUSY_LEAF_TIME) )
            return true;
        
        return fblse;
    }
    
    /**
     * Determines whether or not the specified <tt>QueryRequest</tt>
     * instbnce should be sent to the connection.  The method takes a couple
     * fbctors into account, such as QRP tables, type of query, etc.
     *
     * @pbram query the <tt>QueryRequest</tt> to check against
     *  the dbta
     * @return <tt>true</tt> if the <tt>QueryRequest</tt> should be sent to
     * this connection, otherwise <tt>fblse</tt>
     */
    public boolebn shouldForwardQuery(QueryRequest query) {
        // specibl what is queries have version numbers attached to them - make
        // sure thbt the remote host can answer the query....
        if (query.isFebtureQuery()) {
            if (isSupernodeClientConnection())
                return (getRemoteHostFebtureQuerySelector() >= 
                        query.getFebtureSelector());
            else if (isSupernodeSupernodeConnection())
                return getRemoteHostSupportsFebtureQueries();
            else
                return fblse;
        }
        return hitsQueryRouteTbble(query);
    }
    
    /**
     * Determines whether or not this query hits the QRT.
     */
    protected boolebn hitsQueryRouteTable(QueryRequest query) {
        if(_lbstQRPTableReceived == null) return false;
        return _lbstQRPTableReceived.contains(query);
	}

    /**
     * Accessor for the <tt>QueryRouteTbble</tt> received along this 
     * connection.  Cbn be <tt>null</tt> if no query routing table has been 
     * received yet.
     *
     * @return the lbst <tt>QueryRouteTable</tt> received along this
     *  connection
     */
    public QueryRouteTbble getQueryRouteTableReceived() {
        return _lbstQRPTableReceived;
    }

    /**
     * Accessor for the lbst QueryRouteTable's percent full.
     */
    public double getQueryRouteTbblePercentFull() {
        return _lbstQRPTableReceived == null ?
            0 : _lbstQRPTableReceived.getPercentFull();
    }
    
    /**
     * Accessor for the lbst QueryRouteTable's size.
     */
    public int getQueryRouteTbbleSize() {
        return _lbstQRPTableReceived == null ?
            0 : _lbstQRPTableReceived.getSize();
    }
    
    /**
     * Accessor for the lbst QueryRouteTable's Empty Units.
     */
    public int getQueryRouteTbbleEmptyUnits() {
        return _lbstQRPTableReceived == null ?
            -1 : _lbstQRPTableReceived.getEmptyUnits();
    }
    
    /**
     * Accessor for the lbst QueryRouteTable's Units In Use.
     */
    public int getQueryRouteTbbleUnitsInUse() {
        return _lbstQRPTableReceived == null ?
            -1 : _lbstQRPTableReceived.getUnitsInUse();
    }
    
    /**
     * Crebtes a deflated output stream.
     *
     * If the connection supports bsynchronous messaging, this does nothing,
     * becbuse we already installed an asynchronous writer that doesn't
     * use strebms.
     */
    protected OutputStrebm createDeflatedOutputStream(OutputStream out) {
        if(isAsynchronous())
            return out;
        else
            return super.crebteDeflatedOutputStream(out);
    }
    
    /**
     * Crebtes the deflated input stream.
     *
     * If the connection supports bsynchronous messaging, this does nothing,
     * becbuse we're going to install a reader when we start looping for
     * messbges.  Note, however, that if we use the 'receive' calls
     * instebd of loopForMessages, an UncompressingInputStream is going to
     * be set up butomatically.
     */
    protected InputStrebm createInflatedInputStream(InputStream in) {
        if(isAsynchronous())
            return in;
        else
            return super.crebteInflatedInputStream(in);
    }

    /**
     * Throttles the super's OutputStrebm.  This works quite well with
     * compressed strebms, because the chaining mechanism writes the
     * compressed bytes, ensuring thbt we do not attempt to request
     * more dbta (and thus sleep while throttling) than we will actually write.
     */
    protected OutputStrebm getOutputStream()  throws IOException {
        return new ThrottledOutputStrebm(super.getOutputStream(), _throttle);
    }

    /**
     * Override of receive to do ConnectionMbnager stats and to properly shut
     * down the connection on IOException
     */
    public Messbge receive() throws IOException, BadPacketException {
        Messbge m = null;
        
        try {
            m = super.receive();
        } cbtch(IOException e) {
            if( _mbnager != null )
                _mbnager.remove(this);
            throw e;
        }
        // record received messbge in stats
        _connectionStbts.addReceived();
        return m;
    }

    /**
     * Override of receive to do MessbgeRouter stats and to properly shut
     * down the connection on IOException
     */
    public Messbge receive(int timeout)
            throws IOException, BbdPacketException, InterruptedIOException {
        Messbge m = null;
        
        try {
            m = super.receive(timeout);
        } cbtch(InterruptedIOException ioe) {
            //we rebd nothing in this timeframe,
            //do not remove, just rethrow.
            throw ioe;
        } cbtch(IOException e) {
            if( _mbnager != null )
                _mbnager.remove(this);
            throw e;
        }
        
        // record received messbge in stats
        _connectionStbts.addReceived();
        return m;
    }


    ////////////////////// Sending, Outgoing Flow Control //////////////////////
    
    /**
     * Stbrts an OutputRunner.  If the Connection supports asynchronous writing,
     * this does not use bn extra thread.  Otherwise, a thread is started up
     * to write.
     */
    privbte void startOutput() {
        MessbgeQueue queue;
        // Tbking this change out until we can safely handle attacks and overflow 
        // TODO: mbke a cheaper Queue that still prevents flooding of ultrapeer
        //       bnd ensures that clogged leaf doesn't drop QRP messages.
		//if(isSupernodeSupernodeConnection())
		    queue = new CompositeQueue();
		//else
		    //queue = new BbsicQueue();

		if(isAsynchronous()) {
		    MessbgeWriter messager = new MessageWriter(_connectionStats, queue, this);
		    _outputRunner = messbger;
		    ChbnnelWriter writer = messager;
		    
		    if(isWriteDeflbted()) {
		        DeflbterWriter deflater = new DeflaterWriter(_deflater);
		        messbger.setWriteChannel(deflater);
                writer = deflbter;
            }
            DelbyedBufferWriter delayer = new DelayedBufferWriter(1400);
            writer.setWriteChbnnel(delayer);
            writer = delbyer;
            writer.setWriteChbnnel(new ThrottleWriter(_nbThrottle));
		    
		    ((NIOMultiplexor)_socket).setWriteObserver(messbger);
		} else {
		    _outputRunner = new BlockingRunner(queue);
        }
    }

    /**
     * Sends b message.  This overrides does extra buffering so that Messages
     * bre dropped if the socket gets backed up.  Will remove any extended
     * pbyloads if the receiving connection does not support GGGEP.   Also
     * updbtes MessageRouter stats.<p>
     *
     * This method IS threbd safe.  Multiple threads can be in a send call
     * bt the same time for a given connection.
     *
     * @requires this is fully constructed
     * @modifies the network underlying this
     */
    public void send(Messbge m) {
        if (! supportsGGEP())
            m=m.stripExtendedPbyload();

        // if Hops Flow is in effect, bnd this is a QueryRequest, and the
        // hoppbge is too biggage, discardage time...
    	int smh = hopsFlowMbx;
        if (smh > -1 && (m instbnceof QueryRequest) && m.getHops() >= smh)
            return;
            
        _outputRunner.send(m);
    }

    /**
     * This is b specialized send method for queries that we originate, 
     * either from ourselves directly, or on behblf of one of our leaves
     * when we're bn Ultrapeer.  These queries have a special sending 
     * queue of their own bnd are treated with a higher priority.
     *
     * @pbram query the <tt>QueryRequest</tt> to send
     */
    public void originbteQuery(QueryRequest query) {
        query.originbte();
        send(query);
    }
 
    /**
     * Does nothing.  Since this butomatically takes care of flushing output
     * buffers, there is nothing to do.  Note thbt flush() does NOT block for
     * TCP buffers to be emptied.  
     */
    public void flush() throws IOException {        
    }

    public void close() {
        if(_outputRunner != null)
            _outputRunner.shutdown();
        super.close();
        
        // relebse pointer to our _guidMap so it can be gc()'ed
        if (_guidMbp != null)
            GuidMbpExpirer.removeMap(_guidMap);
    }

    //////////////////////////////////////////////////////////////////////////
    
    /**
     * Hbndles core Gnutella request/reply protocol.
     * If bsynchronous messaging is supported, this immediately
     * returns bnd messages are processed asynchronously via processMessage
     * cblls.  Otherwise, if reading blocks, this  will run until the connection
     * is closed.
     *
     * @requires this is initiblized
     * @modifies the network underlying this, mbnager
     * @effects receives request bnd sends appropriate replies.
     *
     * @throws IOException pbssed on from the receive call; failures to forward
     *         or route messbges are silently swallowed, allowing the message
     *         loop to continue.
     */
    void loopForMessbges() throws IOException {
        supernodeClientAtLooping = isSupernodeClientConnection();
        
        if(!isAsynchronous()) {
            while (true) {
                Messbge m=null;
                try {
                    m = receive();
                    if (m==null)
                        continue;
                    hbndleMessageInternal(m);
                } cbtch (BadPacketException ignored) {}
            }
        } else {
            MessbgeReader reader = new MessageReader(ManagedConnection.this);
            if(isRebdDeflated())
                rebder.setReadChannel(new InflaterReader(_inflater));
                
            ((NIOMultiplexor)_socket).setRebdObserver(reader);
        }
    }
    
    /**
     * Notificbtion that messaging has closed.
     */
    public void messbgingClosed() {
        if( _mbnager != null )
            _mbnager.remove(this);   
    }
    
    /**
     * Notificbtion that a message is available to be processed (via asynch-processing).
     */
    public void processRebdMessage(Message m) throws IOException {
        updbteReadStatistics(m);
        _connectionStbts.addReceived();
        hbndleMessageInternal(m);
    }
    
    /**
     * Notificbtion that a message has been sent.  Updates stats.
     */
    public void processSentMessbge(Message m) {
        updbteWriteStatistics(m);
    }
    
    /**
     * Hbndles a message without updating appropriate statistics.
     */
    privbte void handleMessageInternal(Message m) {
        // Run through the route spbm filter and drop accordingly.
        if (isSpbm(m)) {
			ReceivedMessbgeStatHandler.TCP_FILTERED_MESSAGES.addMessage(m);
            _connectionStbts.addReceivedDropped();
        } else {
            if(m instbnceof QueryReply && m.getHops() == 0)
                clientGUID = ((QueryReply)m).getClientGUID();
        
            //specibl handling for proxying.
            if(supernodeClientAtLooping) {
                if(m instbnceof QueryRequest)
                    m = tryToProxy((QueryRequest) m);
                else if (m instbnceof QueryStatusResponse)
                    m = morphToStopQuery((QueryStbtusResponse) m);
            }
            MessbgeDispatcher.instance().dispatchTCP(m, this);
        }
    }
    
    /**
     * Returns the network thbt the MessageReceiver uses -- Message.N_TCP.
     */
    public int getNetwork() {
        return Messbge.N_TCP;
    }
    

    privbte QueryRequest tryToProxy(QueryRequest query) {
        // we must hbve the following qualifications:
        // 1) Lebf must be sending SuperNode a query (checked in loopForMessages)
        // 2) Lebf must support Leaf Guidance
        // 3) Query must not be OOB.
        // 3.5) The query originbtor should not disallow proxying.
        // 4) We must be bble to OOB and have great success rate.
        if (remoteHostSupportsLebfGuidance() < 1) return query;
        if (query.desiresOutOfBbndReplies()) return query;
        if (query.doNotProxy()) return query;
        if (!RouterService.isOOBCbpable() || 
            !OutOfBbndThroughputStat.isSuccessRateGreat() ||
            !OutOfBbndThroughputStat.isOOBEffectiveForProxy()) return query;

        // everything is b go - we need to do the following:
        // 1) mutbte the GUID of the query - you should maintain every param of
        // the query except the new GUID bnd the OOB minspeed flag
        // 2) set up mbppings between the old guid and the new guid.
        // bfter that, everything is set.  all you need to do is map the guids
        // of the replies bbck to the original guid.  also, see if a you get a
        // QueryStbtusResponse message and morph it...
        // THIS IS SOME MAJOR HOKERY-POKERY!!!
        
        // 1) mutbte the GUID of the query
        byte[] origGUID = query.getGUID();
        byte[] oobGUID = new byte[origGUID.length];
        System.brraycopy(origGUID, 0, oobGUID, 0, origGUID.length);
        GUID.bddressEncodeGuid(oobGUID, RouterService.getAddress(),
                               RouterService.getPort());

        query = QueryRequest.crebteProxyQuery(query, oobGUID);

        // 2) set up mbppings between the guids
        if (_guidMbp == null) {
            _guidMbp = new Hashtable();
            GuidMbpExpirer.addMapToExpire(_guidMap);
        }
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(oobGUID),
                                                  TIMED_GUID_LIFETIME);
        _guidMbp.put(tGuid, new GUID(origGUID));

        OutOfBbndThroughputStat.OOB_QUERIES_SENT.incrementStat();
        return query;
    }

    privbte QueryStatusResponse morphToStopQuery(QueryStatusResponse resp) {
        // if the _guidMbp is null, we aren't proxying anything....
        if (_guidMbp == null) return resp;

        // if we bre proxying this query, we should modify the GUID so as
        // to shut off the correct query
        finbl GUID origGUID = resp.getQueryGUID();
        GUID oobGUID = null;
        synchronized (_guidMbp) {
            Iterbtor entrySetIter = _guidMap.entrySet().iterator();
            while (entrySetIter.hbsNext()) {
                Mbp.Entry entry = (Map.Entry) entrySetIter.next();
                if (origGUID.equbls(entry.getValue())) {
                    oobGUID = ((GUID.TimedGUID)entry.getKey()).getGUID();
                    brebk;
                }
            }
        }

        // if we hbd a match, then just construct a new one....
        if (oobGUID != null)
            return new QueryStbtusResponse(oobGUID, resp.getNumResults());

        else return resp;
    }
    

    /**
     * Utility method for checking whether or not this messbge is considered
     * spbm.
     * 
     * @pbram m the <tt>Message</tt> to check
     * @return <tt>true</tt> if this is considered spbm, otherwise 
     *  <tt>fblse</tt>
     */
    public boolebn isSpam(Message m) {
        return !_routeFilter.bllow(m);
    }

    //
    // Begin Messbge dropping and filtering calls
    //

    /**
     * A cbllback for the ConnectionManager to inform this connection that a
     * messbge was dropped.  This happens when a reply received from this
     * connection hbs no routing path.
     */
    public void countDroppedMessbge() {
        _connectionStbts.addReceivedDropped();
    }

    /**
     * A cbllback for Message Handler implementations to check to see if a
     * messbge is considered to be undesirable by the message's receiving
     * connection.
     * Messbges ignored for this reason are not considered to be dropped, so
     * no stbtistics are incremented here.
     *
     * @return true if the messbge is spam, false if it's okay
     */
    public boolebn isPersonalSpam(Message m) {
        return !_personblFilter.allow(m);
    }

    /**
     * @modifies this
     * @effects sets the underlying routing filter.   Note thbt
     *  most filters bre not thread-safe, so they should not be shared
     *  bmong multiple connections.
     */
    public void setRouteFilter(SpbmFilter filter) {
        _routeFilter = filter;
    }

    /**
     * @modifies this
     * @effects sets the underlying personbl filter.   Note that
     *  most filters bre not thread-safe, so they should not be shared
     *  bmong multiple connections.
     */
    public void setPersonblFilter(SpamFilter filter) {
        _personblFilter = filter;
    }
    
    /**
     * This method is cblled when a reply is received for a PingRequest
     * originbting on this Connection.  So, just send it back.
     * If modifying this method, note thbt receivingConnection may
     * by null.
     */
    public void hbndlePingReply(PingReply pingReply,
                                ReplyHbndler receivingConnection) {
        send(pingReply);
    }

    /**
     * This method is cblled when a reply is received for a QueryRequest
     * originbting on this Connection.  So, send it back.
     * If modifying this method, note thbt receivingConnection may
     * by null.
     */
    public void hbndleQueryReply(QueryReply queryReply,
                                 ReplyHbndler receivingConnection) {
        if (_guidMbp != null) {
        // ---------------------
        // If we bre proxying for a query, map back the guid of the reply
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(queryReply.getGUID()),
                                                  TIMED_GUID_LIFETIME);
        GUID origGUID = (GUID) _guidMbp.get(tGuid);
        if (origGUID != null) { 
            byte prevHops = queryReply.getHops();
            queryReply = new QueryReply(origGUID.bytes(), queryReply);
            queryReply.setTTL((byte)2); // we ttl 1 more thbn necessary
            queryReply.setHops(prevHops);
        }
        // ---------------------
        }
        
        send(queryReply);
    }
    
    /**
     * Gets the clientGUID of the remote host of the connection.
     */
    public byte[] getClientGUID() {
        return clientGUID;
    }

    /**
     * This method is cblled when a PushRequest is received for a QueryReply
     * originbting on this Connection.  So, just send it back.
     * If modifying this method, note thbt receivingConnection may
     * by null.
     */
    public void hbndlePushRequest(PushRequest pushRequest,
                                  ReplyHbndler receivingConnection) {
        send(pushRequest);
    }   


    protected void hbndleVendorMessage(VendorMessage vm) {
        // let Connection do bs needed....
        super.hbndleVendorMessage(vm);

        // now i cbn process
        if (vm instbnceof HopsFlowVendorMessage) {
            // updbte the softMaxHops value so it can take effect....
            HopsFlowVendorMessbge hops = (HopsFlowVendorMessage) vm;
            
            if( isSupernodeClientConnection() )
                //	If the connection is to b leaf, and it is busy (HF == 0)
                //	then set the globbl busy leaf flag appropriately
                setBusy( hops.getHopVblue()==0 );
            
            hopsFlowMbx = hops.getHopValue();
        }
        else if (vm instbnceof PushProxyAcknowledgement) {
            // this connection cbn serve as a PushProxy, so note this....
            PushProxyAcknowledgement bck = (PushProxyAcknowledgement) vm;
            if (Arrbys.equals(ack.getGUID(),
                              RouterService.getMessbgeRouter()._clientGUID)) {
                _pushProxy = true;
            }
            // else mistbke on the server side - the guid should be my client
            // guid - not reblly necessary but whatever
        }
        else if(vm instbnceof CapabilitiesVM) {
            //we need to see if there is b new simpp version out there.
            CbpabilitiesVM capVM = (CapabilitiesVM)vm;
            if(cbpVM.supportsSIMPP() > SimppManager.instance().getVersion()) {
                //request the simpp messbge
                SimppRequestVM simppReq = new SimppRequestVM();
                send(simppReq);
            }
            
            // see if there's b new update message.
            int lbtestId = UpdateHandler.instance().getLatestId();
            int currentId = cbpVM.supportsUpdate();
            if(currentId > lbtestId)
                send(new UpdbteRequest());
            else if(currentId == lbtestId)
                UpdbteHandler.instance().handleUpdateAvailable(this, currentId);
                
        }
        else if (vm instbnceof MessagesSupportedVendorMessage) {        
            // If this is b ClientSupernodeConnection and the host supports
            // lebf guidance (because we have to tell them when to stop)
            // then see if there bre any old queries that we can re-originate
            // on this connection.
            if(isClientSupernodeConnection() &&
               (remoteHostSupportsLebfGuidance() >= 0)) {
                SebrchResultHandler srh =
                    RouterService.getSebrchResultHandler();
                List queries = srh.getQueriesToReSend();
                for(Iterbtor i = queries.iterator(); i.hasNext(); )
                    send((Messbge)i.next());
            }            

            // see if you need b PushProxy - the remoteHostSupportsPushProxy
            // test incorporbtes my leaf status in it.....
            if (remoteHostSupportsPushProxy() > -1) {
                // get the client GUID bnd send off a PushProxyRequest
                GUID clientGUID =
                    new GUID(RouterService.getMessbgeRouter()._clientGUID);
                PushProxyRequest req = new PushProxyRequest(clientGUID);
                send(req);
            }

            // do i need to send bny ConnectBack messages????
            if (!UDPService.instbnce().canReceiveUnsolicited() &&
                (_numUDPConnectBbckRequests < MAX_UDP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsUDPRedirect() > -1)) {
                GUID connectBbckGUID = RouterService.getUDPConnectBackGUID();
                Messbge udp = new UDPConnectBackVendorMessage(RouterService.getPort(),
                                                              connectBbckGUID);
                send(udp);
                _numUDPConnectBbckRequests++;
            }

            if (!RouterService.bcceptedIncomingConnection() &&
                (_numTCPConnectBbckRequests < MAX_TCP_CONNECT_BACK_ATTEMPTS) &&
                (remoteHostSupportsTCPRedirect() > -1)) {
                Messbge tcp = new TCPConnectBackVendorMessage(RouterService.getPort());
                send(tcp);
                _numTCPConnectBbckRequests++;
            }
        }
    }


    //
    // End reply forwbrding calls
    //


    //
    // Begin stbtistics accessors
    //

    /** Returns the number of messbges sent on this connection */
    public int getNumMessbgesSent() {
        return _connectionStbts.getSent();
    }

    /** Returns the number of messbges received on this connection */
    public int getNumMessbgesReceived() {
        return _connectionStbts.getReceived();
    }

    /** Returns the number of messbges I dropped while trying to send
     *  on this connection.  This hbppens when the remote host cannot
     *  keep up with me. */
    public int getNumSentMessbgesDropped() {
        return _connectionStbts.getSentDropped();
    }

    /**
     * The number of messbges received on this connection either filtered out
     * or dropped becbuse we didn't know how to route them.
     */
    public long getNumReceivedMessbgesDropped() {
        return _connectionStbts.getReceivedDropped();
    }

    /**
     * @modifies this
     * @effects Returns the percentbge of messages sent on this
     *  since the lbst call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.
     */
    public flobt getPercentReceivedDropped() {
        return _connectionStbts.getPercentReceivedDropped();
    }

    /**
     * @modifies this
     * @effects Returns the percentbge of messages sent on this
     *  since the lbst call to getPercentSentDropped that were
     *  dropped by this end of the connection.  This vblue may be
     *  grebter than 100%, e.g., if only one message is sent but
     *  four bre dropped during a given time period.
     */
    public flobt getPercentSentDropped() {
        return _connectionStbts.getPercentSentDropped();
    }

    /**
     * Tbkes a snapshot of the upstream and downstream bandwidth since the last
     * cbll to measureBandwidth.
     * @see BbndwidthTracker#measureBandwidth 
     */
    public void mebsureBandwidth() {
        _upBbndwidthTracker.measureBandwidth(
             ByteOrder.long2int(getBytesSent()));
        _downBbndwidthTracker.measureBandwidth(
             ByteOrder.long2int(getBytesReceived()));
    }

    /**
     * Returns the upstrebm bandwidth between the last two calls to
     * mebsureBandwidth.
     * @see BbndwidthTracker#measureBandwidth 
     */
    public flobt getMeasuredUpstreamBandwidth() {
        flobt retValue = 0; //initialize to default
        try {
            retVblue = _upBandwidthTracker.getMeasuredBandwidth();
        } cbtch(InsufficientDataException ide) {
            return 0;
        }
        return retVblue;
    }

    /**
     * Returns the downstrebm bandwidth between the last two calls to
     * mebsureBandwidth.
     * @see BbndwidthTracker#measureBandwidth 
     */
    public flobt getMeasuredDownstreamBandwidth() {
        flobt retValue = 0;
        try {
            retVblue = _downBandwidthTracker.getMeasuredBandwidth();
        } cbtch (InsufficientDataException ide) {
            return 0;
        }
        return retVblue;
    }

    //
    // End stbtistics accessors
    //

    /** Returns the system time thbt we should next forward a query route table
     *  blong this connection.  Only valid if isClientSupernodeConnection() is
     *  true. */
    public long getNextQRPForwbrdTime() {
        return _nextQRPForwbrdTime;
    }

	/**
	 * Increments the next time we should forwbrd query route tables for
	 * this connection.  This depends on whether or not this is b connection
	 * to b leaf or to an Ultrapeer.
	 *
	 * @pbram curTime the current time in milliseconds, used to calculate 
	 *  the next updbte time
	 */
	public void incrementNextQRPForwbrdTime(long curTime) {
		if(isLebfConnection()) {
			_nextQRPForwbrdTime = curTime + LEAF_QUERY_ROUTE_UPDATE_TIME;
		} else {
			// otherwise, it's bn Ultrapeer
			_nextQRPForwbrdTime = curTime + ULTRAPEER_QUERY_ROUTE_UPDATE_TIME;
		}
	} 
    
    /** 
     * Returns true if this should not be policed by the ConnectionWbtchdog,
     * e.g., becbuse this is a connection to a Clip2 reflector. Default value:
     * true.
     */
	public boolebn isKillable() {
		return _isKillbble;
	}
    
    /** 
     * Accessor for the query route tbble associated with this.  This is
     * gubranteed to be non-null, but it may not yet contain any data.
     *
     * @return the <tt>QueryRouteTbble</tt> instance containing
     *  query route tbble data sent along this connection, or <tt>null</tt>
     *  if no dbta has yet been sent
     */
    public QueryRouteTbble getQueryRouteTableSent() {
        return _lbstQRPTableSent;
    }

    /**
     * Mutbtor for the last query route table that was sent along this
     * connection.
     *
     * @pbram qrt the last query route table that was sent along this
     *  connection
     */
    public void setQueryRouteTbbleSent(QueryRouteTable qrt) {
        _lbstQRPTableSent = qrt;
    }

    
    public boolebn isPushProxy() {
        return _pushProxy;
    }

	public Object getQRPLock() {
		return QRP_LOCK;
	}

    /**
     * set preferencing for the responder
     * (The preference of the Responder is used when crebting the response 
     * (in Connection.jbva: conclude..))
     */
    public void setLocblePreferencing(boolean b) {
        RESPONSE_HEADERS.setLocblePreferencing(b);
    }
    
    public void reply(Messbge m){
    	send(m);
    }
    

    /** Repebtedly sends all the queued data using a thread. */
    privbte class BlockingRunner implements Runnable, OutputRunner {
        privbte final Object LOCK = new Object();
        privbte final MessageQueue queue;
        privbte boolean shutdown = false;
        
        public BlockingRunner(MessbgeQueue queue) {
            this.queue = queue;
            Threbd output = new ManagedThread(this, "OutputRunner");
            output.setDbemon(true);
            output.stbrt();
        }

        public void send(Messbge m) {
            synchronized (LOCK) {
                _connectionStbts.addSent();
                queue.bdd(m);
                int dropped = queue.resetDropped();
                _connectionStbts.addSentDropped(dropped);
                LOCK.notify();
            }
        }
        
        public void shutdown() {
            synchronized(LOCK) {
                shutdown = true;
                LOCK.notify();
            }
        }

        /** While the connection is not closed, sends bll data delay. */
        public void run() {
            //For non-IOExceptions, Throwbble is caught to notify ErrorService.
            try {
                while (true) {
                    wbitForQueued();
                    sendQueued();
                }                
            } cbtch (IOException e) {
                if(_mbnager != null)
                    _mbnager.remove(ManagedConnection.this);
            } cbtch(Throwable t) {
                if(_mbnager != null)
                    _mbnager.remove(ManagedConnection.this);
                ErrorService.error(t);
            }
        }

        /** 
         * Wbit until the queue is (probably) non-empty or closed. 
         * @exception IOException this wbs closed while waiting
         */
        privbte final void waitForQueued() throws IOException {
            // Lock outside of the loop so thbt the MessageQueue is synchronized.
            synchronized (LOCK) {
                while (!shutdown && isOpen() && queue.isEmpty()) {           
                    try {
                        LOCK.wbit();
                    } cbtch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            
            if (! isOpen() || shutdown)
                throw CONNECTION_CLOSED;
        }
        
        /** Send severbl queued message of each type. */
        privbte final void sendQueued() throws IOException {
            // Send bs many messages as we can, until we run out.
            while(true) {
                Messbge m = null;
                synchronized(LOCK) {
                    m = queue.removeNext();
                    int dropped = queue.resetDropped();
                    _connectionStbts.addSentDropped(dropped);
                }
                if(m == null)
                    brebk;

                //Note thbt if the ougoing stream is compressed
                //(isWriteDeflbted()), this call may not actually
                //do bnything.  This is because the Deflater waits
                //until bn optimal time to start deflating, buffering
                //up incoming dbta until that time is reached, or the
                //dbta is explicitly flushed.
                MbnagedConnection.super.send(m);
            }
            
            //Note thbt if the outgoing stream is compressed 
            //(isWriteDeflbted()), then this call may block while the
            //Deflbter deflates the data.
            MbnagedConnection.super.flush();
        }
    }
    

    /** Clbss-wide expiration mechanism for all ManagedConnections.
     *  Only expires on-dembnd.
     */
    privbte static class GuidMapExpirer implements Runnable {
        
        privbte static List toExpire = new LinkedList();
        privbte static boolean scheduled = false;

        public GuidMbpExpirer() {};

        public stbtic synchronized void addMapToExpire(Map expiree) {
            // schedule it on dembnd
            if (!scheduled) {
                RouterService.schedule(new GuidMbpExpirer(), 0,
                                       TIMED_GUID_LIFETIME);
                scheduled = true;
            }
            toExpire.bdd(expiree);
        }

        public stbtic synchronized void removeMap(Map expiree) {
            toExpire.remove(expiree);
        }

        public void run() {
            synchronized (GuidMbpExpirer.class) {
                // iterbtor through all the maps....
                Iterbtor iter = toExpire.iterator();
                while (iter.hbsNext()) {
                    Mbp currMap = (Map) iter.next();
                    synchronized (currMbp) {
                        Iterbtor keyIter = currMap.keySet().iterator();
                        // bnd expire as many entries as possible....
                        while (keyIter.hbsNext()) 
                            if (((GUID.TimedGUID) keyIter.next()).shouldExpire())
                                keyIter.remove();
                    }
                }
            }
        }
    }
}
