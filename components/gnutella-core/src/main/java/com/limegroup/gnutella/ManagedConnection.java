package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.limegroup.gnutella.connection.MessageQueue;
import com.limegroup.gnutella.connection.PriorityMessageQueue;
import com.limegroup.gnutella.connection.SimpleMessageQueue;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.LeafHandshakeResponder;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.handshaking.UltrapeerHandshakeResponder;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.updates.UpdateManager;
import com.limegroup.gnutella.util.BandwidthThrottle;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.ThrottledOutputStream;

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
	implements ReplyHandler, PushProxyInterface {

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
    /** A lock to protect _outputQueue. */
    private Object _outputQueueLock=new Object();
    /** The producer's queues, one priority per mesage type. 
     *  INVARIANT: _outputQueue.length==PRIORITIES
     *  LOCKING: obtain _outputQueueLock. */
    private MessageQueue[] _outputQueue=new MessageQueue[PRIORITIES];
    /** The number of queued messages.  Maintained for performance.
     *  INVARIANT: _queued==sum of _outputQueue[i].size() 
     *  LOCKING: obtain _outputQueueLock */
    private int _queued=0;
    /** True if the OutputRunner died.  For testing only. */
    private boolean _runnerDied=false;
    /** The priority of the last message added to _outputQueue. This is an
     *  optimization to keep OutputRunner from iterating through all priorities.
     *  This value is only a hint and can be legally set to any priority.  Hence
     *  no locking is necessary.  Package-access for testing purposes only. */
    int _lastPriority=0;
    /** The size of the queue per priority. Larger values tolerate larger bursts
     *  of producer traffic, though they waste more memory. This queue is 
     *  slightly larger than the standard to accomodate higher priority 
     *  messages, such as queries and query hits. */
    private static final int BIG_QUEUE_SIZE = 100;

    /** The size of the queue per priority. Larger values tolerate larger bursts
     *  of producer traffic, though they waste more memory. This queue is
     *  slightly smaller so that we don't waste too much memory on lower
     *  priority messages. */
    private static final int QUEUE_SIZE = 1;
    /** The max time to keep reply messages and pushes in the queues, in
     *  milliseconds. */
    private static int BIG_QUEUE_TIME=10*1000;
    /** The max time to keep queries, pings, and pongs in the queues, in
     *  milliseconds.  Package-access for testing purposes only! */
    static int QUEUE_TIME=5*1000;
    /** The number of different priority levels. */
    private static final int PRIORITIES = 8;
    /** Names for each priority. "Other" includes QRP messages and is NOT
     * reordered.  These numbers do NOT translate directly to priorities;
     * that's determined by the cycle fields passed to MessageQueue. */
    private static final int PRIORITY_WATCHDOG=0;
    private static final int PRIORITY_PUSH=1;
    private static final int PRIORITY_QUERY_REPLY=2;
    private static final int PRIORITY_QUERY=3; //TODO: add requeries
    private static final int PRIORITY_PING_REPLY=4;
    private static final int PRIORITY_PING=5;
    private static final int PRIORITY_OTHER=6;    
    
    /**
     * Separate priority for queries that we originate.  These are very
     * high priority because we don't want to drop queries that are
     * originating from us -- we want to largely bypass the message
     * queues when we are first sending a query out on the network.
     */
    private static final int PRIORITY_OUR_QUERY=7;
                                                            
    /** Limits outgoing bandwidth for ALL connections. */
    private final static BandwidthThrottle _throttle=
        new BandwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);


    /**
     * The amount of time to wait for a handshake ping in reject connections, in
     * milliseconds.     
     */
    private static final int REJECT_TIMEOUT=500;  //0.5 sec


    /**
     * The number of messages received.  This messages that are eventually
     * dropped.  This stat is synchronized by _outputQueueLock;
     */
    private int _numMessagesSent;
    /**
     * The number of messages received.  This includes messages that are
     * eventually dropped.  This stat is not synchronized because receiving
     * is not thread-safe; callers are expected to make sure only one thread
     * at a time is calling receive on a given connection.
     */
    private int _numMessagesReceived;
    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    private int _numReceivedMessagesDropped;
    /**
     * The number of messages I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     */
    private int _numSentMessagesDropped;


    /**
     * _lastSent/_lastSentDropped and _lastReceived/_lastRecvDropped the values
     * of _numMessagesSent/_numSentMessagesDropped and
     * _numMessagesReceived/_numReceivedMessagesDropped at the last call to
     * getPercentDropped.  LOCKING: These are synchronized by this;
     * finer-grained schemes could be used. 
     */
    private int _lastReceived;
    private int _lastRecvDropped;
    private int _lastSent;
    private int _lastSentDropped;

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
   
    /**
     * The domain to which this connection is authenticated
     */
    private Set _domains = null;

    /** Use this if a HopsFlowVM instructs us to stop sending queries below
     *  this certain hops value....
     */
    private volatile int softMaxHops = -1;

    /** Use this if a PushProxyAck is received for this MC meaning the remote
     *  Ultrapeer can serve as a PushProxy
     */
    private InetAddress pushProxyAddr = null;

    /** Use this if a PushProxyAck is received for this MC meaning the remote
     *  Ultrapeer can serve as a PushProxy
     */
    private int pushProxyPort = -1;

    /** The class wide static counter for the number of udp connect back 
     *  request sent.
     */
    private static int _numUDPConnectBackRequests = 0;

    /** The class wide static counter for the number of tcp connect back 
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
     * We want to construct this lazily....
     * GUID.TimedGUID -> GUID
     * OOB Proxy GUID - > Original GUID
     */
    private Map _guidMap = null;

    /**
     * The max lifetime of the GUID (10 minutes).
     */
    private static long TIMED_GUID_LIFETIME = 10 * 60 * 1000;

    /**
     * Whether or not horizon counting is enabled from this connection.
     */
    private boolean _horizonEnabled = true;

    /**
     * Creates a new outgoing connection to the specified host on the
	 * specified port.  
	 *
	 * @param host the address of the host we're connecting to
	 * @param port the port the host is listening on
     */
    public ManagedConnection(String host, int port) {
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
	static ManagedConnection 
        createTestConnection(String host, int port, 
		  Properties props, HandshakeResponder responder) {	
		return new ManagedConnection(host, port, props, responder);
	}

	/**
	 * Creates a new <tt>ManagedConnection</tt> with the specified 
	 * handshake classes and the specified host and port.
	 */
	private ManagedConnection(String host, int port, 
							  Properties props, 
							  HandshakeResponder responder) {	
        super(host, port, props, responder);        
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
        super(socket, 
			  RouterService.isSupernode() ? 
			  (HandshakeResponder)(new UltrapeerHandshakeResponder(
			      socket.getInetAddress().getHostAddress())) : 
			  (HandshakeResponder)(new LeafHandshakeResponder(
				  socket.getInetAddress().getHostAddress())));
        _manager = RouterService.getConnectionManager();
    }



    public void initialize()
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        //Establish the socket (if needed), handshake.
		super.initialize(CONNECT_TIMEOUT);

        UpdateManager updater = UpdateManager.instance();
        updater.checkAndUpdate(this);
    }


    /**
     * Resets the query route table for this connection.  The new table
     * will be of the size specified in <tt>rtm</tt> and will contain
     * no data.  If there is no <tt>QueryRouteTable</tt> yet created for
     * this connection, this method will create one.
     *
     * @param rtm the <tt>ResetTableMessage</tt> 
     */
    public void resetQueryRouteTable(ResetTableMessage rtm) {
        if(_lastQRPTableReceived == null) {
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
        addReceived();
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
        addReceived();
        return m;
    }


    ////////////////////// Sending, Outgoing Flow Control //////////////////////

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
    public void send(Message m) {
        send(m, calculatePriority(m));
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
        send(query, PRIORITY_OUR_QUERY);
    }

    /**
     * Sends the message with the specified, pre-calculated priority.
     *
     * @param m the <tt>Message</tt> to send
     * @param priority the priority to send the message with
     */
    private void send(Message m, int priority) {
        if (! supportsGGEP())
            m=m.stripExtendedPayload();

        // if Hops Flow is in effect, and this is a QueryRequest, and the
        // hoppage is too biggage, discardage time...
	int smh = softMaxHops;.
        if ((smh > -1) &&
            (m instanceof QueryRequest) &&
            (m.getHops() >= smh))
            return;

        repOk();
        Assert.that(_outputQueue!=null, "Connection not initialized");
        synchronized (_outputQueueLock) {
            _numMessagesSent++;
            _outputQueue[priority].add(m);
            int dropped=_outputQueue[priority].resetDropped();
            addSentDropped(dropped);
            _queued+=1-dropped;
            _lastPriority=priority;
            _outputQueueLock.notify();
        }
        repOk();        
    }

    /**
     * Utility method for adding dropped message data.
     * 
     * @param dropped the number of dropped messages to add
     */
    private void addSentDropped(int dropped) {
        _numSentMessagesDropped += dropped;
    }
    
    /**
     * Increments the number of received messages that have been dropped.
     */
    public void addReceivedDropped() {
        _numReceivedMessagesDropped++;   
    }
    
    /**
     * Increments the stat for the number of messages received.
     */
    public void addReceived() {
        _numMessagesReceived++;
    }
 
    /** 
     * Returns the send priority for the given message, with higher number for
     * higher priorities.  TODO: this method will eventually be moved to
     * MessageRouter and account for number of reply bytes.
     */
    private int calculatePriority(Message m) {
        byte opcode=m.getFunc();
        switch (opcode) {
            case Message.F_QUERY:
                return PRIORITY_QUERY;
            case Message.F_QUERY_REPLY: 
                return PRIORITY_QUERY_REPLY;
            case Message.F_PING_REPLY: 
                return (m.getHops()==0 && m.getTTL()<=2) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING_REPLY;
            case Message.F_PING: 
                return (m.getHops()==0 && m.getTTL()==1) ? 
                    PRIORITY_WATCHDOG : PRIORITY_PING;
            case Message.F_PUSH: 
                return PRIORITY_PUSH;                
            default: 
                return PRIORITY_OTHER;  //includes QRP Tables
        }
    }

    /**
     * Does nothing.  Since this automatically takes care of flushing output
     * buffers, there is nothing to do.  Note that flush() does NOT block for
     * TCP buffers to be emptied.  
     */
    public void flush() throws IOException {        
    }
    
    /**
     * Builds queues and starts the OutputRunner.  This is intentionally not
     * in initialize(), as we do not want to create the queues and start
     * the OutputRunner for reject connections.
     */
    public void buildAndStartQueues() {
        //Instantiate queues.  TODO: for ultrapeer->leaf connections, we can
        //save a fair bit of memory by not using buffering at all.  But this
        //requires the CompositeMessageQueue class from nio-branch.
        _outputQueue[PRIORITY_WATCHDOG]     //LIFO, no timeout or priorities
            = new SimpleMessageQueue(1, Integer.MAX_VALUE, BIG_QUEUE_SIZE, 
                true);
        _outputQueue[PRIORITY_PUSH]
            = new PriorityMessageQueue(6, BIG_QUEUE_TIME, BIG_QUEUE_SIZE);
        _outputQueue[PRIORITY_QUERY_REPLY]
            = new PriorityMessageQueue(6, BIG_QUEUE_TIME, BIG_QUEUE_SIZE);
        _outputQueue[PRIORITY_QUERY]      
            = new PriorityMessageQueue(3, QUEUE_TIME, BIG_QUEUE_SIZE);
        _outputQueue[PRIORITY_PING_REPLY] 
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        _outputQueue[PRIORITY_PING]       
            = new PriorityMessageQueue(1, QUEUE_TIME, QUEUE_SIZE);
        _outputQueue[PRIORITY_OUR_QUERY]
            = new PriorityMessageQueue(10, BIG_QUEUE_TIME, BIG_QUEUE_SIZE);
        _outputQueue[PRIORITY_OTHER]       //FIFO, no timeout
            = new SimpleMessageQueue(1, Integer.MAX_VALUE, BIG_QUEUE_SIZE, 
                false);
        
        //Start the thread to empty the output queue
        startOutputRunner();
    }
    
    /**
     * Creates and starts an OutputRunner.
     * Exists as a hook for tests.
     */
    protected void startOutputRunner() {
        new OutputRunner();
    }    

    /** Repeatedly sends all the queued data. */
    private class OutputRunner extends ManagedThread {
        public OutputRunner() {
            setName("OutputRunner");
            setDaemon(true);
            start();
        }

        /** While the connection is not closed, sends all data delay. */
        public void managedRun() {
            //Exceptions are only caught to set the _runnerDied variable
            //to make testing easier.  For non-IOExceptions, Throwable
            //is caught to notify ErrorService.
            try {
                while (true) {
                    repOk();
                    waitForQueued();
                    sendQueued();
                    repOk();
                }                
            } catch (IOException e) {
                if(_manager != null)
                    _manager.remove(ManagedConnection.this);
                _runnerDied=true;
            } catch(Throwable t) {
                if(_manager != null)
                    _manager.remove(ManagedConnection.this);
                _runnerDied=true;
                ErrorService.error(t);
            }
        }

        /** 
         * Wait until the queue is (probably) non-empty or closed. 
         * @exception IOException this was closed while waiting
         */
        private final void waitForQueued() throws IOException {
            //The synchronized statement is outside the while loop to
            //protect _queued.
            synchronized (_outputQueueLock) {
                while (isOpen() && _queued==0) {           
                    try {
                        _outputQueueLock.wait();
                    } catch (InterruptedException e) {
                        Assert.that(false, "OutputRunner Interrupted");
                    }
                }
            }
            
            if (! isOpen())
                throw CONNECTION_CLOSED;
        }
        
        /** Send several queued message of each type. */
        private final void sendQueued() throws IOException {  
            //1. For each priority i send as many messages as desired for that
            //type.  As an optimization, we start with the buffer of the last
            //message sent, wrapping around the buffer.  You can also search
            //from 0 to the end.
            int start=_lastPriority;
            int i=start;
            do {                   
                //IMPORTANT: we only obtain _outputQueueLock while touching the
                //queue, not while actually sending (which can block).
                MessageQueue queue=_outputQueue[i];
                queue.resetCycle();
                boolean emptied=false;
                while (true) {
                    Message m=null;
                    synchronized (_outputQueueLock) {
                        m = queue.removeNext();
                        int dropped=queue.resetDropped();
                        addSentDropped(dropped);
                        _queued-=(m==null?0:1)+dropped;  //maintain invariant
                        if (_queued==0)
                            emptied=true;                        
                        if (m==null)
                            break;
                    }

                    //Note that if the ougoing stream is compressed
                    //(isWriteDeflated()), this call may not actually
                    //do anything.  This is because the Deflater waits
                    //until an optimal time to start deflating, buffering
                    //up incoming data until that time is reached, or the
                    //data is explicitly flushed.
                    ManagedConnection.super.send(m);
                }
                
                //Optimization: the if statement below is not needed for
                //correctness but works nicely with the _priorityHint trick.
                if (emptied)
                    break;
                i=(i+1)%PRIORITIES;
            } while (i!=start);
            
            
            //2. Now force data from Connection's BufferedOutputStream into the
            //kernel's TCP send buffer.  It doesn't force TCP to
            //actually send the data to the network.  That is determined
            //by the receiver's window size and Nagle's algorithm.
            //Note that if the outgoing stream is compressed 
            //(isWriteDeflated()), then this call may block while the
            //Deflater deflates the data.
            ManagedConnection.super.flush();
        }
    } //end OutputRunner


    /** 
     * For debugging only: prints to stdout the number of queued messages in
     * this, by type.
     */
    private void dumpQueueStats() {
        synchronized (_outputQueueLock) {
            for (int i=0; i<PRIORITIES; i++) {
                System.out.println(i+" "+_outputQueue[i].size());
            }
            System.out.println("* "+_queued+"\n");
        }
    }


    public void close() {
        //Ensure OutputRunner terminates.
        synchronized (_outputQueueLock) {
            super.close();
            _outputQueueLock.notify();
        }
        // release pointer to our _guidMap so it can be gc()'ed
        if (_guidMap != null)
            GuidMapExpirer.removeMap(_guidMap);
    }

    //////////////////////////////////////////////////////////////////////////

    /**
     * Implements the reject connection mechanism.  Loops until receiving a
     * handshake ping, responds with the best N pongs, and closes the
     * connection.  Closes the connection if no ping is received within a
     * reasonable amount of time.  Does NOT clean up route tables in the case
     * of an IOException.
     */
    void loopToReject() {
        //IMPORTANT: note that we do not use this' send or receive methods.
        //This is an important optimization to prevent calling
        //RouteTable.removeReplyHandler when the connection is closed.

        try {
			//The first message we get from the remote host should be its 
            //initial ping.  However, some clients may start forwarding packets 
            //on the connection before they send the ping.  Hence the following 
            //loop.  The limit of 10 iterations guarantees that this method 
            //will not run for more than TIMEOUT*10=80 seconds.  Thankfully 
            //this happens rarely.
			for (int i=0; i<10; i++) {
				Message m=null;
				try {                
					m=super.receive(REJECT_TIMEOUT);
					if (m==null)
						return; //Timeout has occured and we havent received the ping,
					//so just return
				}// end of try for BadPacketEception from socket
				catch (BadPacketException e) {
					return; //Its a bad packet, just return
				}
				if((m instanceof PingRequest) && (m.getHops()==0)) {
					// this is the only kind of message we will deal with
					// in Reject Connection
					// If any other kind of message comes in we drop
					
					//SPECIAL CASE: for crawler ping
					if(m.getTTL() == 2) {
						handleCrawlerPing((PingRequest)m);
						return;
					}
				}// end of (if m is PingRequest)
			} // End of while(true)
        } catch (IOException e) {
        } finally {
            close();
        }
    }

    /**
     * Handles the crawler ping of Hops=0 & TTL=2, by sending pongs 
     * corresponding to all its neighbors
     * @param m The ping request received
     * @exception In case any I/O error occurs while writing Pongs over the
     * connection
     */
    private void handleCrawlerPing(PingRequest m) throws IOException {
        //IMPORTANT: note that we do not use this' send or receive methods.
        //This is an important optimization to prevent calling
        //RouteTable.removeReplyHandler when the connection is closed.

        //send the pongs for the Ultrapeer & 0.4 connections
        List /*<ManagedConnection>*/ nonLeafConnections 
            = _manager.getInitializedConnections();
        
        supersendNeighborPongs(m, nonLeafConnections);
        
        //send the pongs for leaves
        List /*<ManagedConnection>*/ leafConnections 
            = _manager.getInitializedClientConnections();
        supersendNeighborPongs(m, leafConnections);
        
        //Note that sending its own pong is not necessary, as the crawler has
        //already connected to this node, and is not sent therefore. 
        //May be sent for completeness though
    }
    
    /**
     * Uses the super class's send message to send the pongs corresponding 
     * to the list of connections passed.
     * This prevents calling RouteTable.removeReplyHandler when 
     * the connection is closed.
     * @param m Th epingrequest received that needs Pongs
     * @param neigbors List (of ManagedConnection) of  neighboring connections
     * @exception In case any I/O error occurs while writing Pongs over the
     * connection
     */
    private void supersendNeighborPongs(PingRequest m, List neighbors) 
        throws IOException {
        for(Iterator iterator = neighbors.iterator();
            iterator.hasNext();) {
            //get the next connection
            ManagedConnection connection = (ManagedConnection)iterator.next();
            
            //create the pong for this connection
            //mark the pong if supernode
            PingReply pr;
            if(connection.isSupernodeConnection()) {
                pr = PingReply.
                    createExternal(m.getGUID(), (byte)2, 
                                   connection.getPort(),
                                   connection.getInetAddress().getAddress(), 
                                   true);
            } else if(connection.isLeafConnection() 
                || connection.isOutgoing()){
                //we know the listening port of the host in this case
                pr = PingReply.
                    createExternal(m.getGUID(), (byte)2, 
                                   connection.getPort(),
                                   connection.getInetAddress().getAddress(), 
                                   false);
            }
            else{
                //Use the port '0' in this case, as we dont know the listening
                //port of the host
                pr = PingReply.
                    createExternal(m.getGUID(), (byte)2, 0,
                                   connection.getInetAddress().getAddress(), 
                                   false);
            }
            
            //hop the message, as it is ideally coming from the connected host
            pr.hop();

            //send the message
            //This is called only during a Reject connection, and thus
            //it is impossible for the stream to be compressed.
            //That is a Good Thing (tm) because we're sending such little
            //data, that the compression may actually hurt.
            super.send(pr);
        }
        
        //Because we are guaranteed that the stream is not compressed,
        //this call will not block.
        super.flush();
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
    void loopForMessages() throws IOException {
		MessageRouter router = RouterService.getMessageRouter();
        final boolean isSupernodeClientConnection=isSupernodeClientConnection();
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
				ReceivedMessageStatHandler.TCP_FILTERED_MESSAGES.addMessage(m);
                addReceivedDropped();
                continue;
            }

            //special handling for proxying - note that for
            //non-SupernodeClientConnections a good compiler will ignore this
            //code
            if (isSupernodeClientConnection && 
                (m instanceof QueryRequest)) m = tryToProxy((QueryRequest) m);
            if (isSupernodeClientConnection &&
                (m instanceof QueryStatusResponse)) 
                m = morphToStopQuery((QueryStatusResponse) m);
            //call MessageRouter to handle and process the message
            router.handleMessage(m, this);            
        }
    }
    
    private QueryRequest tryToProxy(QueryRequest query) {
        // we must have the following qualifications:
        // 1) Leaf must be sending SuperNode a query (checked in loopForMessages)
        // 2) Leaf must support Leaf Guidance
        // 3) Query must not be OOB.
        // 3.5) The query originator should not disallow proxying.
        // 4) We must be able to OOB and have great success rate.
        if (remoteHostSupportsLeafGuidance() < 1) return query;
        if (query.desiresOutOfBandReplies()) return query;
        if (query.doNotProxy()) return query;
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

        query = QueryRequest.createProxyQuery(query, oobGUID);

        // 2) set up mappings between the guids
        if (_guidMap == null) {
            _guidMap = new Hashtable();
            GuidMapExpirer.addMapToExpire(_guidMap);
        }
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(oobGUID),
                                                  TIMED_GUID_LIFETIME);
        _guidMap.put(tGuid, new GUID(origGUID));

        OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
        return query;
    }

    private QueryStatusResponse morphToStopQuery(QueryStatusResponse resp) {
        // if the _guidMap is null, we aren't proxying anything....
        if (_guidMap == null) return resp;

        // if we are proxying this query, we should modify the GUID so as
        // to shut off the correct query
        final GUID origGUID = resp.getQueryGUID();
        GUID oobGUID = null;
        synchronized (_guidMap) {
            Iterator entrySetIter = _guidMap.entrySet().iterator();
            while (entrySetIter.hasNext()) {
                Map.Entry entry = (Map.Entry) entrySetIter.next();
                if (origGUID.equals(entry.getValue())) {
                    oobGUID = ((GUID.TimedGUID)entry.getKey()).getGUID();
                    break;
                }
            }
        }

        // if we had a match, then just construct a new one....
        if (oobGUID != null)
            return new QueryStatusResponse(oobGUID, resp.getNumResults());

        else return resp;
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
		_numReceivedMessagesDropped++;
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
//        return (String[])_domains.toArray(new String[0]);
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
				//domainsAuthenticated = getProperty(
                //HeaderNames.X_DOMAINS_AUTHENTICATED);
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
        if (_guidMap != null) {
        // ---------------------
        // If we are proxying for a query, map back the guid of the reply
        GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(queryReply.getGUID()),
                                                  TIMED_GUID_LIFETIME);
        GUID origGUID = (GUID) _guidMap.get(tGuid);
        if (origGUID != null) { 
            byte prevHops = queryReply.getHops();
            queryReply = new QueryReply(origGUID.bytes(), queryReply);
            queryReply.setTTL((byte)2); // we ttl 1 more than necessary
            queryReply.setHops(prevHops);
        }
        // ---------------------
        }
        
        send(queryReply);
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
            softMaxHops = hops.getHopValue();
        }
        else if (vm instanceof PushProxyAcknowledgement) {
            // this connection can serve as a PushProxy, so note this....
            PushProxyAcknowledgement ack = (PushProxyAcknowledgement) vm;
            if (Arrays.equals(ack.getGUID(),
                              RouterService.getMessageRouter()._clientGUID)) {
                pushProxyPort = ack.getListeningPort();
                pushProxyAddr = ack.getListeningAddress();
            }
            // else mistake on the server side - the guid should be my client
            // guid - not really necessary but whatever
        }
        else if(vm instanceof CapabilitiesVM) {
            //we need to see if there is a new simpp version out there.
            CapabilitiesVM capVM = (CapabilitiesVM)vm;
            if(capVM.supportsSIMPP() > SimppManager.instance().getVersion()) {
                //request the simpp message
                SimppRequestVM simppReq = new SimppRequestVM();
                send(simppReq);
            }
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
                List queries = srh.getQueriesToReSend();
                for(Iterator i = queries.iterator(); i.hasNext(); )
                    send((Message)i.next());
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
        return _numMessagesSent;
    }

    /** Returns the number of messages received on this connection */
    public int getNumMessagesReceived() {
        return _numMessagesReceived;
    }

    /** Returns the number of messages I dropped while trying to send
     *  on this connection.  This happens when the remote host cannot
     *  keep up with me. */
    public int getNumSentMessagesDropped() {
        return _numSentMessagesDropped;
    }

    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    public long getNumReceivedMessagesDropped() {
        return _numReceivedMessagesDropped;
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.
     */
    public synchronized float getPercentReceivedDropped() {
        int rdiff = _numMessagesReceived - _lastReceived;
        int ddiff = _numReceivedMessagesDropped - _lastRecvDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastReceived = _numMessagesReceived;
        _lastRecvDropped = _numReceivedMessagesDropped;
        return percent;
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were
     *  dropped by this end of the connection.  This value may be
     *  greater than 100%, e.g., if only one message is sent but
     *  four are dropped during a given time period.
     */
    public synchronized float getPercentSentDropped() {
        int rdiff = _numMessagesSent - _lastSent;
        int ddiff = _numSentMessagesDropped - _lastSentDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastSent = _numMessagesSent;
        _lastSentDropped = _numSentMessagesDropped;
        return percent;
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

    /** 
     * @modifies this
     * @effects enables or disables updateHorizon. Typically this method
     *  is used to temporarily disable horizon statistics before sending a 
     *  ping with a small TTL to make sure a connection is up.
     */
    public synchronized void setHorizonEnabled(boolean enable) {
        _horizonEnabled=enable;
    }

    /**
     * This method is called when a reply is received by this connection for a
     * PingRequest that originated from LimeWire.
     * 
     * @modifies this 
     * @effects adds the statistics from pingReply to this' horizon statistics,
     *  unless horizon statistics have been disabled via setHorizonEnabled(false).
     *  It's possible that the horizon statistics will not actually be updated
     *  until refreshHorizonStats is called.
     */
    public synchronized void updateHorizonStats(PingReply pingReply) {
        if (! _horizonEnabled)
            return;
        
        HorizonCounter.instance().addPong(pingReply);
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

    
    /** @return a non-negative integer representing the proxy's port for HTTP
     *  communication, a negative number if PushProxy isn't supported.
     */
    public int getPushProxyPort() {
        return pushProxyPort;
    }

    /** @return the InetAddress of the remote host - only meaningful if
     *  getPushProxyPort() > -1
     *  @see getPushProxyPort()
     */
    public InetAddress getPushProxyAddress() {
        return pushProxyAddr;
    }
    

    /** 
     * Tests representation invariants.  For performance reasons, this is
     * private and final.  Make protected if ManagedConnection is subclassed.
     */
    private final void repOk() {
        /*
        //Check _queued invariant.
        synchronized (_outputQueueLock) {
            int sum=0;
            for (int i=0; i<_outputQueue.length; i++) 
                sum+=_outputQueue[i].size();
            Assert.that(sum==_queued, "Expected "+sum+", got "+_queued);
        }
        */
    }

//	// overrides Object.toString
//	public String toString() {
//		return "ManagedConnection: Ultrapeer: "+isSupernodeConnection()+
//			" Leaf: "+isLeafConnection();
//	}
    
    /***************************************************************************
     * UNIT TESTS: tests/com/limegroup/gnutella/ManagedConnectionTest
     **************************************************************************/

    /** FOR TESTING PURPOSES ONLY! */
    void stopOutputRunner() {
        //Ensure OutputRunner terminates.
        synchronized (_outputQueueLock) {
            super._closed=true;  //doesn't close socket
            _outputQueueLock.notify();
        }
        //Wait for OutputRunner to terminate
        while (! _runnerDied) { 
            Thread.yield();
        }
        //Make it alive again (except for runner)
        _runnerDied=false;
        super._closed=false;
    }

    /** FOR TESTING PURPOSES ONLY! */
    boolean runnerDied() {
        return _runnerDied;
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
        RESPONSE_HEADERS.setLocalePreferencing(b);
    }
    
    public void reply(Message m){
    	send(m);
    }

    /** Class-wide expiration mechanism for all ManagedConnections.
     *  Only expires on-demand.
     */
    private static class GuidMapExpirer implements Runnable {
        
        private static List toExpire = new LinkedList();
        private static boolean scheduled = false;

        public GuidMapExpirer() {};

        public static synchronized void addMapToExpire(Map expiree) {
            // schedule it on demand
            if (!scheduled) {
                RouterService.schedule(new GuidMapExpirer(), 0,
                                       TIMED_GUID_LIFETIME);
                scheduled = true;
            }
            toExpire.add(expiree);
        }

        public static synchronized void removeMap(Map expiree) {
            toExpire.remove(expiree);
        }

        public void run() {
            synchronized (GuidMapExpirer.class) {
                // iterator through all the maps....
                Iterator iter = toExpire.iterator();
                while (iter.hasNext()) {
                    Map currMap = (Map) iter.next();
                    synchronized (currMap) {
                        Iterator keyIter = currMap.keySet().iterator();
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
