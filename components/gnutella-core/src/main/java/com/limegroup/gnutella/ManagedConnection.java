package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.filters.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.statistics.*;

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
    private static final int CONNECT_TIMEOUT=4000;  //4 seconds

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
    
    /**
     * Handle to the message writer for this connection.
     */
    private MessageWriter _messageWriter;
    
    /**
     * Handle to the message reader for this connection.
     */
    private MessageReader _messageReader;

	/** Filter for filtering out messages that are considered spam.
	 */
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    /** A lock for QRP activity on this connection */
    private final Object QRP_LOCK = new Object();

    /** True if the OutputRunner died.  For testing only. */
    private boolean _runnerDied = false;

                                                            
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
    private int softMaxHops = -1;

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
    }


    /**
     * Initializes this connection with a timeout.
     */
    public void initialize()
        throws IOException, NoGnutellaOkException, BadHandshakeException {
        //Establish the socket (if needed), handshake.
		super.initialize(CONNECT_TIMEOUT);
    }

    /** 
     * Call this method when the Connection has been initialized and accepted
     * as 'long-lived'.
     */
    protected void postInit() {
        try { // TASK 1 - Send a MessagesSupportedVendorMessage if necessary....
            if(headers().supportsVendorMessages()) {
                send(MessagesSupportedVendorMessage.instance());
            }
        //} catch (IOException ioe) {
        } catch (BadPacketException bpe) {
            // should never happen.
            ErrorService.error(bpe);
        }
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
            _lastQRPTableReceived = new QueryRouteTable(rtm.getTableSize());
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
     * instance has a hit in the query routing tables.  If this 
     * connection has not yet sent a query route table, this returns
     * <tt>false</tt>.
     *
     * @param query the <tt>QueryRequest</tt> to check against
     *  the tables
     * @return <tt>true</tt> if the <tt>QueryRequest</tt> has a hit
     *  in the tables, otherwise <tt>false</tt>
     */
    public boolean hitsQueryRouteTable(QueryRequest query) {
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
        Message m = super.receive();
        
        // record received message in stats
        addReceived();
        return m;
    }

    /**
     * Override of receive to do MessageRouter stats and to properly shut
     * down the connection on IOException
     * 
     * TODO:: this method is only used in tests -- we should probably convert
     *  tests to use the receive() method (so we test code that's actually
     *  used in the client), or we should have the receive() method delegate
     *  to this
     */
    public Message receive(int timeout)
            throws IOException, BadPacketException, InterruptedIOException {
        Message m = super.receive(timeout);
        
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
    public void send(Message msg) {
        // if Hops Flow is in effect, and this is a QueryRequest, and the
        // hoppage is too biggage, discardage time....
        if ((softMaxHops > -1) &&
            (msg instanceof QueryRequest) &&
            (msg.getHops() >= softMaxHops)) {
                
            //TODO: record stats for this
            return;
        }

        if (! supportsGGEP())
            msg = msg.stripExtendedPayload();
            
        try {
            _messageWriter.write(msg);
        } catch (IOException e) {
            // this should never happen
            ErrorService.error(e);
        }
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
        // TODO:: this doesn't currently set the priority correctly
        // change implementaation of priority calculation in 
        // CompositeMessageQueue
        try {
            _messageWriter.write(query);
        } catch (IOException e) {
            // this should never happen
            ErrorService.error(e);
        }
    }

    /**
     * Utility method for adding dropped message data.
     * 
     * @param dropped the number of dropped messages to add
     */
    public void addSentDropped(int dropped) {
        _numSentMessagesDropped += dropped;
    }
    
    /**
     * Increments the number of messages sent for this connection.
     */
    public void addSent() {
        _numMessagesSent++;    
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
     * Does nothing.  Since this automatically takes care of flushing output
     * buffers, there is nothing to do.  Note that flush() does NOT block for
     * TCP buffers to be emptied.  
     */
    public void flush() throws IOException {        
    }
    
    /**
     * Accessor for the <tt>MessageReader</tt> instance for this connection.
     * The reader handles reading all messages to the network.
     * 
     * @return the <tt>MessageReader</tt> for this connection
     */
    public MessageReader getReader() {
        return _messageReader;    
    }
    
    /**
     * Accessor for the <tt>MessageWriter</tt> instance for this connection.
     * The writer handles writing all messages to the network.
     * 
     * @return the <tt>MessageWriter</tt> for this connection
     */
    public MessageWriter getWriter() {
        return _messageWriter;    
    }
    
    /**
     * Builds queues and starts the OutputRunner.  This is intentionally not
     * in initialize(), as we do not want to create the queues and start
     * the OutputRunner for reject connections.
     */
    public void buildAndStartQueues() {
        // at this point, everything's initialized, so create our readers and
        // writers.
        _messageWriter = new MessageWriterProxy(this);
    }        

    /**
     * Overridden to also close the message writer.
     */
    public void close() {
        _messageWriter.close();
        super.close();      
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
                    // TODO: this bypasses the recording of received messages
                    // in this class -- use ManagedConnection receive method?            
					m = super.receive(REJECT_TIMEOUT);
					if (m==null) {
                        // Timeout has occured and we havent received the ping.
                        return;              
                    }
						 
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
        List /*<ManagedConnection>*/ nonLeafConnections = 
            RouterService.getConnectionManager().getInitializedConnections2();
        
        supersendNeighborPongs(m, nonLeafConnections);
        
        //send the pongs for leaves
        List /*<ManagedConnection>*/ leafConnections = 
            RouterService.getConnectionManager().
                getInitializedClientConnections2();
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
                                   connection.getListeningPort(),
                                   connection.getInetAddress().getAddress(), 
                                   true);
            } else if(connection.isLeafConnection() 
                || connection.isOutgoing()){
                //we know the listening port of the host in this case
                pr = PingReply.
                    createExternal(m.getGUID(), (byte)2, 
                                   connection.getListeningPort(),
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
            super.sendMessage(pr);
        }
        
        //Because we are guaranteed that the stream is not compressed,
        //this call will not block.
        super.flushMessage();
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
                addReceivedDropped();
                continue;
            }

            //call MessageRouter to handle and process the message
            router.handleMessage(m, this);            
        }
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
     * Sets whether or not the sending thread has died -- USED ONLY
     * FOR TESTING!
     * 
     * @param died specifies whether or not the sender thread has died
     */
    public void setSenderDied(boolean died) {
        _runnerDied = died;    
    }
    

//	// overrides Object.toString
//	public String toString() {
//		return "ManagedConnection: Ultrapeer: "+isSupernodeConnection()+
//			" Leaf: "+isLeafConnection();
//	}
    
   
    
    /***************************************************************************
     * UNIT TESTS: tests/com/limegroup/gnutella/ManagedConnectionBufferTest
     **************************************************************************/


    /** FOR TESTING PURPOSES ONLY! */
    boolean runnerDied() {
        return _runnerDied;
    }

	public Object getQRPLock() {
		return QRP_LOCK;
	}
}
