package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.simpp.*;
import com.limegroup.gnutella.upelection.*;
import com.sun.java.util.collections.*;
import java.util.StringTokenizer;
import java.io.*;
import java.net.*;


/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
public abstract class MessageRouter {
	
    /**
     * Handle to the <tt>ConnectionManager</tt> to access our TCP connections.
     */
    protected static ConnectionManager _manager;

    /**
     * Constant for the number of old connections to use when forwarding
     * traffic from old connections.
     */
    private static final int OLD_CONNECTIONS_TO_USE = 15;

    /**
     * The GUID we attach to QueryReplies to allow PushRequests in
     * responses.
     */
    protected byte[] _clientGUID;


	/**
	 * Reference to the <tt>ReplyHandler</tt> for messages intended for 
	 * this node.
	 */
    private final ReplyHandler FOR_ME_REPLY_HANDLER = 
		ForMeReplyHandler.instance();
		
    /**
     * The maximum size for <tt>RouteTable</tt>s.
     */
    private int MAX_ROUTE_TABLE_SIZE = 50000;  //actually 100,000 entries

    /**
     * The maximum number of bypassed results to remember per query.
     */
    private final int MAX_BYPASSED_RESULTS = 150;

    /**
     * Maps PingRequest GUIDs to PingReplyHandlers.  Stores 2-4 minutes,
     * typically around 2500 entries, but never more than 100,000 entries.
     */
    private RouteTable _pingRouteTable = 
        new RouteTable(2*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typically around 13000 entries, but never more than 100,000 entries.
     */
    private RouteTable _queryRouteTable = 
        new RouteTable(5*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typically around 3500 entries, but never more than 100,000
     * entries.  
     */
    private RouteTable _pushRouteTable = 
        new RouteTable(7*60, MAX_ROUTE_TABLE_SIZE);

    /** How long to buffer up out-of-band replies.
     */
    private static final long CLEAR_TIME = 30 * 1000; // 30 seconds

    /** The maximum number of UDP replies to buffer up.  Non-final for 
     *  testing.
     */
    static int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps track of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryResponseBundle.
     */
    private final Map _outOfBandReplies = new Hashtable();

    /**
     * Keeps track of potential sources of content.  Comprised of Sets of GUESS
     * Endpoints.  Kept tidy when searches/downloads are killed.
     */
    private final Map _bypassedResults = new HashMap();

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap _udpConnectBacks = 
        new FixedsizeHashMap(200);

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * TCP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap _tcpConnectBacks = 
        new FixedsizeHashMap(200);

	/**
	 * Constant handle to the <tt>QueryUnicaster</tt> since it is called
	 * upon very frequently.
	 */
	protected final QueryUnicaster UNICASTER = QueryUnicaster.instance();

	/**
	 * Constant for the <tt>QueryDispatcher</tt> that handles dynamically
	 * generated queries that adjust to the number of results received, the
	 * number of connections, etc.
	 */
	private final QueryDispatcher DYNAMIC_QUERIER =
		QueryDispatcher.instance();
	
	/**
	 * Handle to the <tt>ActivityCallback</tt> for sending data to the 
	 * display.
	 */
	private ActivityCallback _callback;

	/**
	 * Handle to the <tt>FileManager</tt> instance.
	 */
	private static FileManager _fileManager;
	
	/**
	 * Constant for whether or not to record stats.
	 */
	private static final boolean RECORD_STATS = !CommonUtils.isJava118();

	/**
	 * A handle to the thread that deals with QRP Propagation
	 */
	private final QRPPropagator QRP_PROPAGATOR = new QRPPropagator();


    /**
     * Variable for the most recent <tt>QueryRouteTable</tt> created
     * for this node.  If this node is an Ultrapeer, the routing
     * table will include the tables from its leaves.
     */
    private QueryRouteTable _lastQueryRouteTable;

    /**
     * The maximum number of response to send to a query that has
     * a "high" number of hops.
     */
    private static final int HIGH_HOPS_RESPONSE_LIMIT = 10;

    /**
     * The lifetime of OOBs guids.
     */
    private static final long TIMED_GUID_LIFETIME = 25 * 1000; 

    /**
     * Keeps track of Listeners of GUIDs.
     * GUID -> MessageListener
     */
    private final Map _messageListeners = new Hashtable();

    /**
     * ref to the promotion manager.
     */
    private PromotionManager _promotionManager;
    
    /**
     * Creates a MessageRouter.  Must call initialize before using.
     */
    protected MessageRouter() {
        try {
            _clientGUID = new GUID(GUID.fromHexString(
                ApplicationSettings.CLIENT_ID.getValue())).bytes();
        }
        catch (IllegalArgumentException e) {
            //This should never happen! But if it does, we can recover.
            _clientGUID = Message.makeGuid();
            // And store the next ID in our settings
            ApplicationSettings.CLIENT_ID.setValue(
                new GUID(_clientGUID).toHexString() );
        }
    }

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize() {
        _manager = RouterService.getConnectionManager();
		_callback = RouterService.getCallback();
		_fileManager = RouterService.getFileManager();
		_promotionManager = RouterService.getPromotionManager();
		DYNAMIC_QUERIER.start();
	    QRP_PROPAGATOR.start();

        // schedule a runner to clear unused out-of-band replies
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        // schedule a runner to clear guys we've connected back to
        RouterService.schedule(new ConnectBackExpirer(), 10 * CLEAR_TIME, 
                               10 * CLEAR_TIME);
    }

    /** Call this to inform us that a query has been killed by a user or
     *  whatever.  Useful for purging unneeded info.<br>
     *  Callers of this should make sure that they have purged the guid from
     *  their tables.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void queryKilled(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentException("Input GUID is null!");
        synchronized (_bypassedResults) {
        if (!RouterService.getDownloadManager().isGuidForQueryDownloading(guid))
            _bypassedResults.remove(guid);
        }
    }

    /** Call this to inform us that a download is finished or whatever.  Useful
     *  for purging unneeded info.<br>
     *  If the caller is a Downloader, please be sure to clear yourself from the
     *  active and waiting lists in DownloadManager.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void downloadFinished(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentException("Input GUID is null!");
        synchronized (_bypassedResults) {
        if (!_callback.isQueryAlive(guid) && 
            !RouterService.getDownloadManager().isGuidForQueryDownloading(guid))
            _bypassedResults.remove(guid);
        }
    }
    
    /** @returns a Set with GUESSEndpoints that had matches for the
     *  original query guid.  may be empty.
     *  @param guid the guid of the query you want endpoints for.
     */
    public Set getGuessLocs(GUID guid) {
        Set clone = new HashSet();
        synchronized (_bypassedResults) {
            Set eps = (Set) _bypassedResults.get(guid);
            if (eps != null)
                clone.addAll(eps);
        }
        return clone;
    }
    
    public String getPingRouteTableDump() {
        return _pingRouteTable.toString();
    }

    public String getQueryRouteTableDump() {
        return _queryRouteTable.toString();
    }

    public String getPushRouteTableDump() {
        return _pushRouteTable.toString();
    }

    /**
     * A callback for ConnectionManager to clear a <tt>ReplyHandler</tt> from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ReplyHandler rh) {
        DYNAMIC_QUERIER.removeReplyHandler(rh);
        _pingRouteTable.removeReplyHandler(rh);
        _queryRouteTable.removeReplyHandler(rh);
        _pushRouteTable.removeReplyHandler(rh);
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param m the <tt>Message</tt> instance to route appropriately
	 * @param receivingConnection the <tt>ManagedConnection</tt> over which
	 *  the message was received
     */
    public void handleMessage(Message msg, 
                              ManagedConnection receivingConnection) {
        // Increment hops and decrease TTL.
        msg.hop();
	   
        if(msg instanceof PingRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_PING_REQUESTS.addMessage(msg);
            handlePingRequestPossibleDuplicate((PingRequest)msg, 
											   receivingConnection);
		} else if (msg instanceof PingReply) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_PING_REPLIES.addMessage(msg);
            handlePingReply((PingReply)msg, receivingConnection);
		} else if (msg instanceof QueryRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(msg);
            handleQueryRequestPossibleDuplicate(
                (QueryRequest)msg, receivingConnection);
		} else if (msg instanceof QueryReply) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_QUERY_REPLIES.addMessage(msg);
            // if someone sent a TCP QueryReply with the MCAST header,
            // that's bad, so ignore it.
            QueryReply qmsg = (QueryReply)msg;
            handleQueryReply(qmsg, receivingConnection);            
		} else if (msg instanceof PushRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, receivingConnection);
		} else if (msg instanceof ResetTableMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handleResetTableMessage((ResetTableMessage)msg,
                                    receivingConnection);
		} else if (msg instanceof PatchTableMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handlePatchTableMessage((PatchTableMessage)msg,
                                    receivingConnection);            
        }
        else if (msg instanceof MessagesSupportedVendorMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_MESSAGES_SUPPORTED.addMessage(msg);
            receivingConnection.handleVendorMessage((VendorMessage) msg);
        }
        else if (msg instanceof CapabilitiesVM) {
			if(RECORD_STATS)
                ;
            receivingConnection.handleVendorMessage((VendorMessage) msg);
        }
        else if (msg instanceof HopsFlowVendorMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_HOPS_FLOW.addMessage(msg);
            receivingConnection.handleVendorMessage((VendorMessage) msg);
        }
        else if (msg instanceof TCPConnectBackVendorMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_TCP_CONNECTBACK.addMessage(msg);
            handleTCPConnectBackRequest((TCPConnectBackVendorMessage) msg,
                                        receivingConnection);
        }
        else if (msg instanceof UDPConnectBackVendorMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_UDP_CONNECTBACK.addMessage(msg);
            handleUDPConnectBackRequest((UDPConnectBackVendorMessage) msg,
                                        receivingConnection);
        }
        else if (msg instanceof TCPConnectBackRedirect) {
			if(RECORD_STATS)
                ;
            handleTCPConnectBackRedirect((TCPConnectBackRedirect) msg,
                                         receivingConnection);
        }
        else if (msg instanceof UDPConnectBackRedirect) {
			if(RECORD_STATS)
                ;
            handleUDPConnectBackRedirect((UDPConnectBackRedirect) msg,
                                         receivingConnection);
        }
        else if (msg instanceof PushProxyRequest) {
			if(RECORD_STATS)
                ;
            handlePushProxyRequest((PushProxyRequest) msg, receivingConnection);
        }
        else if (msg instanceof PushProxyAcknowledgement) {
			if(RECORD_STATS)
                ;
            receivingConnection.handleVendorMessage((VendorMessage) msg);
        }
        else if (msg instanceof QueryStatusResponse) {
			if(RECORD_STATS)
                ;
            handleQueryStatus((QueryStatusResponse) msg, receivingConnection);
        }
        else if (msg instanceof GiveStatsVendorMessage) {
            if(RECORD_STATS)
                ; //TODO: add the statistics recording code
            handleGiveStats((GiveStatsVendorMessage)msg, receivingConnection);
        }
        else if(msg instanceof StatisticVendorMessage) {
            if(RECORD_STATS) 
                ;//TODO: add the statistics recording code
            handleStatisticsMessage(
                            (StatisticVendorMessage)msg, receivingConnection);
        }
        else if(msg instanceof SimppRequestVM) {
            handleSimppRequest((SimppRequestVM)msg, receivingConnection);
        }
        else if(msg instanceof SimppVM) {
            handleSimppVM((SimppVM)msg);
        }
        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
		//forwardQueryRouteTables();
        notifyMessageListener(msg);
    }

    private final void notifyMessageListener(Message msg) {
        MessageListener ml = 
            (MessageListener) _messageListeners.get(new GUID(msg.getGUID()));
        if (ml != null) ml.processMessage(msg);
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param msg the <tt>Message</tt> received
	 * @param datagram the <tt>DatagramPacket</tt> containing the IP and 
	 *  port of the client node
     */	
	public void handleUDPMessage(Message msg, DatagramPacket datagram) {
        // Increment hops and decrement TTL.
        msg.hop();

		InetAddress address = datagram.getAddress();
		int port = datagram.getPort();
		// Verify that the address and port are valid.
		// If they are not, we cannot send any replies to them.
		if(!NetworkUtils.isValidAddress(address) ||
		   !NetworkUtils.isValidPort(port))
		    return;

		ReplyHandler handler = new UDPReplyHandler(address, port);
		
        if (msg instanceof QueryRequest) {
            //TODO: compare QueryKey with old generation params.  if it matches
            //send a new one generated with current params 
            if (hasValidQueryKey(address, port, (QueryRequest) msg)) {
                sendAcknowledgement(datagram, msg.getGUID());
                // a TTL above zero may indicate a malicious client, as UDP
                // messages queries should not be sent with TTL above 1.
                //if(msg.getTTL() > 0) return;
                if (!handleUDPQueryRequestPossibleDuplicate(
                  (QueryRequest)msg, handler) ) {
                    ReceivedMessageStatHandler.UDP_DUPLICATE_QUERIES.addMessage(msg);
                }  
            }
            if(RECORD_STATS)
                ReceivedMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(msg);
		} else if (msg instanceof QueryReply) {
            QueryReply qr = (QueryReply) msg;
			if(RECORD_STATS) {
				ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
                int numResps = qr.getResultCount();
                try {
                    // only account for OOB stuff if this was response to a 
                    // OOB query, multicast stuff is sent over UDP too....
                    if (!qr.isReplyToMulticastQuery())
                        OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
                }
                catch (BadPacketException bpe) {
                    return;
                }
            }
            handleQueryReply(qr, handler);
		} else if(msg instanceof PingRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_PING_REQUESTS.addMessage(msg);
			handleUDPPingRequestPossibleDuplicate((PingRequest)msg, 
												  handler, datagram);
		} else if(msg instanceof PingReply) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
            handleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instanceof PushRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(msg);
			handlePushRequest((PushRequest)msg, handler);
		} else if(msg instanceof LimeACKVendorMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_LIME_ACK.addMessage(msg);
            handleLimeACKMessage((LimeACKVendorMessage)msg, datagram);
        }
        else if(msg instanceof ReplyNumberVendorMessage) {
			if(RECORD_STATS)
                ;
            handleReplyNumberMessage((ReplyNumberVendorMessage) msg, datagram);
        }
        else if(msg instanceof GiveStatsVendorMessage) {
            if(RECORD_STATS)
                ;
            handleGiveStats((GiveStatsVendorMessage) msg, handler);
        }
        else if(msg instanceof StatisticVendorMessage) {
            if(RECORD_STATS)
                ;
            handleStatisticsMessage((StatisticVendorMessage)msg, handler);
        }
        else if(msg instanceof UDPCrawlerPing) {
        	if(RECORD_STATS)
        		;//TODO: add the statistics recording code
        	handleUDPCrawlerPing((UDPCrawlerPing)msg, handler);
        }
        notifyMessageListener(msg);
    }
    
    /**
     * The handler for Multicast messages. Processes a message based on the
     * message type.
     *
     * @param msg the <tt>Message</tt> recieved.
     * @param datagram the <tt>DatagramPacket</tt> containing the IP and
     *  port of the client node.
     */
	public void handleMulticastMessage(Message msg, DatagramPacket datagram) {
    
        // Use this assert for testing only -- it is a dangerous assert
        // to have in the field, as not all messages currently set the
        // network int appropriately.
        // If someone sends us messages we're not prepared to handle,
        // this could cause widespreaad AssertFailures
        //Assert.that(msg.isMulticast(),
        //   "non multicast message in handleMulticastMessage: " + msg);
    
        // no multicast messages should ever have been
        // set with a TTL greater than 1.
        if( msg.getTTL() > 1 )
            return;

        // Increment hops and decrement TTL.
        msg.hop();

		InetAddress address = datagram.getAddress();
		int port = datagram.getPort();
		
        if (NetworkUtils.isLocalAddress(address) &&
          !ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.getValue())
            return;
		
		ReplyHandler handler = new UDPReplyHandler(address, port);
		
        if (msg instanceof QueryRequest) {
            if(!handleUDPQueryRequestPossibleDuplicate(
              (QueryRequest)msg, handler) ) {
                ReceivedMessageStatHandler.MULTICAST_DUPLICATE_QUERIES.addMessage(msg);
            }
            if(RECORD_STATS)
                ReceivedMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(msg);
	//	} else if (msg instanceof QueryReply) {			
	//		if(RECORD_STATS)
	//			ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
    //        handleQueryReply((QueryReply)msg, handler);
		} else if(msg instanceof PingRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.MULTICAST_PING_REQUESTS.addMessage(msg);
			handleUDPPingRequestPossibleDuplicate((PingRequest)msg, 
												  handler, datagram);
	//	} else if(msg instanceof PingReply) {
	//		if(RECORD_STATS)
	//			ReceivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
    //        handleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instanceof PushRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(msg);
			handlePushRequest((PushRequest)msg, handler);
		}
        notifyMessageListener(msg);
    }


    /**
     * Returns true if the Query has a valid QueryKey.  false if it isn't
     * present or valid.
     */
    protected boolean hasValidQueryKey(InetAddress ip, int port, 
                                       QueryRequest qr) {
        if (qr.getQueryKey() == null)
            return false;
        QueryKey computedQK = QueryKey.getQueryKey(ip, port);
        return qr.getQueryKey().equals(computedQK);
    }

	/**
	 * Sends an ack back to the GUESS client node.  
	 */
	protected void sendAcknowledgement(DatagramPacket datagram, byte[] guid) {
		ConnectionManager manager = RouterService.getConnectionManager();
		Endpoint host = manager.getConnectedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
                
                reply = PingReply.createGUESSReply(guid, (byte)1, host);
            } catch(UnknownHostException e) {
				reply = createPingReply(guid);
            }
		} else {
			reply = createPingReply(guid);
		}
		
		// No GUESS endpoints existed and our IP/port was invalid.
		if( reply == null )
		    return;

        UDPService.instance().send(reply, datagram.getAddress(), 
                                   datagram.getPort());
		if(RECORD_STATS)
			SentMessageStatHandler.UDP_PING_REPLIES.addMessage(reply);
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or a <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply createPingReply(byte[] guid) {
		GUESSEndpoint endpoint = UNICASTER.getUnicastEndpoint();
		if(endpoint == null) {
		    if(NetworkUtils.isValidPort(RouterService.getPort()) &&
		       NetworkUtils.isValidAddress(RouterService.getAddress()))
                return PingReply.create(guid, (byte)1);
            else
                return null;
		} else {
            return PingReply.createGUESSReply(guid, (byte)1, 
                                              endpoint.getPort(),
                                              endpoint.getAddress().getAddress());
		}
	}



	
    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handlePingRequestPossibleDuplicate(
        PingRequest request, ReplyHandler handler) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handlePingRequest(request, handler);
    }

    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handleUDPPingRequestPossibleDuplicate(													 
        PingRequest request, ReplyHandler handler, DatagramPacket datagram) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handleUDPPingRequest(request, handler, datagram);
    }

    /**
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplicate(
        QueryRequest request, ManagedConnection receivingConnection) {

        // With the new handling of probe queries (TTL 1, Hops 0), we have a few
        // new options:
        // 1) If we have a probe query....
        //  a) If you have never seen it before, put it in the route table and
        //  set the ttl appropriately
        //  b) If you have seen it before, then just count it as a duplicate
        // 2) If it isn't a probe query....
        //  a) Is it an extension of a probe?  If so re-adjust the TTL.
        //  b) Is it a 'normal' query (no probe extension or already extended)?
        //  Then check if it is a duplicate:
        //    1) If it a duplicate, just count it as one
        //    2) If it isn't, put it in the route table but no need to setTTL

        // we msg.hop() before we get here....
        // hops may be 1 or 2 because we may be probing with a leaf query....
        final boolean isProbeQuery = 
            ((request.getTTL() == 0) && 
             ((request.getHops() == 1) || (request.getHops() == 2)));

		ResultCounter counter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 receivingConnection);

		if(counter != null) {  // query is new (probe or normal)

            // 1a: set the TTL of the query so it can be potentially extended  
            if (isProbeQuery) 
                _queryRouteTable.setTTL(counter, (byte)1);

            // 1a and 2b2
            // if a new probe or a new request, do everything (so input true
            // below)
            handleQueryRequest(request, receivingConnection, counter, true);
		}
        // if (counter == null) the query has been seen before, few subcases... 
        else if ((counter == null) && !isProbeQuery) {// probe extension?

            if (wasProbeQuery(request))
                // rebroadcast out but don't locally evaluate....
                handleQueryRequest(request, receivingConnection, counter, 
                                   false);
            else  // 2b1: not a correct extension, so call it a duplicate....
                tallyDupQuery(request);
        }
        else if ((counter == null) && isProbeQuery) // 1b: duplicate probe
            tallyDupQuery(request);
        else // 2b1: duplicate normal query
            tallyDupQuery(request);
    }
	
    private boolean wasProbeQuery(QueryRequest request) {
        // if the current TTL is large enough and the old TTL was 1, then this
        // was a probe query....
        // NOTE: that i'm setting the ttl to be the actual ttl of the query.  i
        // could set it to some max value, but since we only allow TTL 1 queries
        // to be extended, it isn't a big deal what i set it to.  in fact, i'm
        // setting the ttl to the correct value if we had full expanding rings
        // of queries.
        return ((request.getTTL() > 0) && 
                _queryRouteTable.getAndSetTTL(request.getGUID(), (byte)1, 
                                              (byte)(request.getTTL()+1)));
    }

    private void tallyDupQuery(QueryRequest request) {
        if(RECORD_STATS)
            ReceivedMessageStatHandler.TCP_DUPLICATE_QUERIES.addMessage(request);
    }

	/**
	 * Special handler for UDP queries.  Checks the routing table to see if
	 * the request has already been seen, handling it if not.
	 *
	 * @param query the UDP <tt>QueryRequest</tt> 
	 * @param handler the <tt>ReplyHandler</tt> that will handle the reply
	 * @return false if it was a duplicate, true if it was not.
	 */
	final boolean handleUDPQueryRequestPossibleDuplicate(QueryRequest request,
													  ReplyHandler handler)  {
		ResultCounter counter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 handler);
		if(counter != null) {
            handleQueryRequest(request, handler, counter, true);
            return true;
		}
		return false;
	}

    /**
     * Handles pings from the network.  With the addition of pong caching, this
     * method will either respond with cached pongs, or it will ignore the ping
     * entirely if another ping has been received from this connection very
     * recently.  If the ping is TTL=1, we will always process it, as it may
     * be a hearbeat ping to make sure the connection is alive and well.
     *
     * @param ping the ping to handle
     * @param handler the <tt>ReplyHandler</tt> instance that sent the ping
     */
    final private void handlePingRequest(PingRequest ping,
                                         ReplyHandler handler) {
        // Send it along if it's a heartbeat ping or if we should allow new 
        // pings on this connection.
        if(ping.isHeartbeat() || handler.allowNewPings()) {
            respondToPingRequest(ping, handler);
        } 
    }


    /**
     * The default handler for PingRequests received in
     * ManagedConnection.loopForMessages().  This implementation updates stats,
     * does the broadcast, and generates a response.
     *
     * You can customize behavior in three ways:
     *   1. Override. You can assume that duplicate messages
     *      (messages with the same GUID that arrived via different paths) have
     *      already been filtered.  If you want stats updated, you'll
     *      have to call super.handlePingRequest.
     *   2. Override broadcastPingRequest.  This allows you to use the default
     *      handling framework and just customize request routing.
     *   3. Implement respondToPingRequest.  This allows you to use the default
     *      handling framework and just customize responses.
     */
    protected void handleUDPPingRequest(PingRequest pingRequest,
										ReplyHandler handler, 
										DatagramPacket datagram) {
        if (pingRequest.isQueryKeyRequest())
            sendQueryKeyPong(pingRequest, datagram);
        else
            respondToUDPPingRequest(pingRequest, datagram, handler);
    }
    

    /**
     * Generates a QueryKey for the source (described by datagram) and sends the
     * QueryKey to it via a QueryKey pong....
     */
    protected void sendQueryKeyPong(PingRequest pr, DatagramPacket datagram) {

        // after find more sources and OOB queries, everyone can dole out query
        // keys....

        // generate a QueryKey (quite quick - current impl. (DES) is super
        // fast!
        InetAddress address = datagram.getAddress();
        int port = datagram.getPort();
        QueryKey key = QueryKey.getQueryKey(address, port);
        
        // respond with Pong with QK, as GUESS requires....
        PingReply reply = 
            PingReply.createQueryKeyReply(pr.getGUID(), (byte)1, key);
        UDPService.instance().send(reply, datagram.getAddress(),
                                   datagram.getPort());
    }


    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {
        if (reply.getQueryKey() != null) {
            // this is a PingReply in reply to my QueryKey Request - 
            //consume the Pong and return, don't process as usual....
            OnDemandUnicaster.handleQueryKeyPong(reply);
            return;
        }

        // also add the sender of the pong if different from the host
        // described in the reply...
        if((reply.getPort() != port) || 
           (!reply.getInetAddress().equals(address))) {
            UNICASTER.addUnicastEndpoint(address, port);
		}
        
        // normal pong processing...
        handlePingReply(reply, handler);
    }

    
    /**
     * The default handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  This implementation updates stats,
     * does the broadcast, and generates a response.
     *
     * You can customize behavior in three ways:
     *   1. Override. You can assume that duplicate messages
     *      (messages with the same GUID that arrived via different paths) have
     *      already been filtered.  If you want stats updated, you'll
     *      have to call super.handleQueryRequest.
     *   2. Override broadcastQueryRequest.  This allows you to use the default
     *      handling framework and just customize request routing.
     *   3. Implement respondToQueryRequest.  This allows you to use the default
     *      handling framework and just customize responses.
     *
     * @param locallyEvaluate false if you don't want to send the query to
     * leaves and yourself, true otherwise....
     */
    protected void handleQueryRequest(QueryRequest request,
									  ReplyHandler handler, 
									  ResultCounter counter,
                                      boolean locallyEvaluate) {
        // Apply the personal filter to decide whether the callback
        // should be informed of the query
        if (!handler.isPersonalSpam(request)) {
            _callback.handleQueryString(request.getQuery());
        }
        
		// if it's a request from a leaf and we GUESS, send it out via GUESS --
		// otherwise, broadcast it if it still has TTL
		//if(handler.isSupernodeClientConnection() && 
		// RouterService.isGUESSCapable()) 
		//unicastQueryRequest(request, handler);
        //else if(request.getTTL() > 0) {
        updateMessage(request, handler);

		if(handler.isSupernodeClientConnection() && counter != null) {
            if (request.desiresOutOfBandReplies()) {
                // this query came from a leaf - so check if it desires OOB
                // responses and make sure that the IP it advertises is legit -
                // if it isn't drop away....
                // no need to check the port - if you are attacking yourself you
                // got problems
                String remoteAddr = handler.getInetAddress().getHostAddress();
                String myAddress = 
                    NetworkUtils.ip2string(RouterService.getAddress());
                if (request.getReplyAddress().equals(remoteAddr))
                    ; // continue below, everything looks good
                else if (request.getReplyAddress().equals(myAddress) && 
                         RouterService.isOOBCapable())
                    // i am proxying - maybe i should check my success rate but
                    // whatever...
                    ; 
                else return;
            }

            // don't send it to leaves here -- the dynamic querier will 
            // handle that
            locallyEvaluate = false;
            
            // do respond with files that we may have, though
            respondToQueryRequest(request, _clientGUID, handler);
            
            multicastQueryRequest(request);
            
			if(handler.isGoodLeaf()) {
				sendDynamicQuery(QueryHandler.createHandlerForNewLeaf(request, 
																	  handler,
                                                                      counter), 
								 handler);
			} else {
				sendDynamicQuery(QueryHandler.createHandlerForOldLeaf(request,
																	  handler,
                                                                      counter), 
								 handler);
			}
		} else if(request.getTTL() > 0 && RouterService.isSupernode()) {
            // send the request to intra-Ultrapeer connections -- this does
			// not send the request to leaves
            if(handler.isGoodUltrapeer()) {
                // send it to everyone
                forwardQueryToUltrapeers(request, handler);
            } else {
                // otherwise, only send it to some connections
                forwardLimitedQueryToUltrapeers(request, handler);
            }
		}
			
        if (locallyEvaluate) {
            // always forward any queries to leaves -- this only does
            // anything when this node's an Ultrapeer
            forwardQueryRequestToLeaves(request, handler);
            
            // if I'm not firewalled AND the source isn't firewalled reply ....
            if (request.isFirewalledSource() &&
                !RouterService.acceptedIncomingConnection() &&
                !ApplicationSettings.SERVER.getValue())
                return;
            respondToQueryRequest(request, _clientGUID, handler);
        }
    }

    /** Handles a ACK message - looks up the QueryReply and sends it out of
     *  band.
     */
    protected void handleLimeACKMessage(LimeACKVendorMessage ack,
                                        DatagramPacket datagram) {

        GUID.TimedGUID refGUID = new GUID.TimedGUID(new GUID(ack.getGUID()),
                                                    TIMED_GUID_LIFETIME);
        QueryResponseBundle bundle = 
            (QueryResponseBundle) _outOfBandReplies.remove(refGUID);

        if ((bundle != null) && (ack.getNumResults() > 0)) {
            InetAddress addr = datagram.getAddress();
            int port = datagram.getPort();

            //convert responses to QueryReplies, but only send as many as the
            //node wants
            Iterator iterator = null;
            if (ack.getNumResults() < bundle._responses.length) {
                Response[] desired = new Response[ack.getNumResults()];
                for (int i = 0; i < desired.length; i++)
                    desired[i] = bundle._responses[i];
                iterator = responsesToQueryReplies(desired, bundle._query, 1);
            }
            else 
                iterator = responsesToQueryReplies(bundle._responses, 
                                                   bundle._query, 1);

            //send the query replies
            while(iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();
                UDPService.instance().send(queryReply, addr, port);
            }
        }
        // else some sort of routing error or attack?
        // TODO: tally some stat stuff here
    }

    /** This is called when a client on the network has results for us that we
     *  may want.  We may contact them back directly or just cache them for
     *  use.
     */
    protected void handleReplyNumberMessage(ReplyNumberVendorMessage reply,
                                            DatagramPacket datagram) {
        GUID qGUID = new GUID(reply.getGUID());
        int numResults = 
        RouterService.getSearchResultHandler().getNumResultsForQuery(qGUID);
        if (numResults < 0) // this may be a proxy query
            numResults = DYNAMIC_QUERIER.getLeafResultsForQuery(qGUID);

        // see if we need more results for this query....
        // if not, remember this location for a future, 'find more sources'
        // targeted GUESS query.
        if ((numResults<0) || (numResults>QueryHandler.ULTRAPEER_RESULTS)) {
            if (RECORD_STATS)
                OutOfBandThroughputStat.RESPONSES_BYPASSED.addData(reply.getNumResults());

            DownloadManager dManager = RouterService.getDownloadManager();
            // only store result if it is being shown to the user or if a
            // file with the same guid is being downloaded
            if (!_callback.isQueryAlive(qGUID) && 
                !dManager.isGuidForQueryDownloading(qGUID))
                return;

            GUESSEndpoint ep = new GUESSEndpoint(datagram.getAddress(),
                                                 datagram.getPort());
            synchronized (_bypassedResults) {
                // this is a quick critical section for _bypassedResults
                // AND the set within it
                Set eps = (Set) _bypassedResults.get(qGUID);
                if (eps == null) {
                    eps = new HashSet();
                    _bypassedResults.put(qGUID, eps);
                }
                if (_bypassedResults.size() <= MAX_BYPASSED_RESULTS)
                    eps.add(ep);
            }

            return;
        }

        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(qGUID, reply.getNumResults());
        UDPService.instance().send(ack, datagram.getAddress(),
                                   datagram.getPort());
        if (RECORD_STATS)
            OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(reply.getNumResults());
    }


    /** Stores (for a limited time) the resps for later out-of-band delivery -
     *  interacts with handleLimeACKMessage
     *  @return true if the operation failed, false if not (i.e. too busy)
     */
    protected boolean bufferResponsesForLaterDelivery(QueryRequest query,
                                                      Response[] resps) {
        // store responses by guid for later retrieval
        synchronized (_outOfBandReplies) {
            if (_outOfBandReplies.size() < MAX_BUFFERED_REPLIES) {
                GUID.TimedGUID tGUID = 
                    new GUID.TimedGUID(new GUID(query.getGUID()),
                                       TIMED_GUID_LIFETIME);
                _outOfBandReplies.put(tGUID, new QueryResponseBundle(query, 
                                                                     resps));
                return true;
            }
            return false;
        }
    }


    /**
     * Basically, just get the correct parameters, create a temporary 
     * DatagramSocket, and send a Ping.
     * This method will soon change to just forward a new message, a 
     * UDPConnectBackRedirect, to a third party.
     */
    protected void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connection source) {
        // two options here:
        // 1) if we are connected to an Ultrapeer that supports UDPCBRedirect
        //    messages, just transplant the info from this CB into a Redirect
        //    message and forward it to that Ultrapeer.
        // 2) if we can't a find a UP that supports redirect, then just do the
        //    old procedure (do the connect back yourself).  We will deprecate
        //    this as the user base upgrades....

        // 1)
        final GUID guidToUse = udp.getConnectBackGUID();
        final int portToContact = udp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        List redirect = _manager.getUDPRedirectUltrapeers();
        if (redirect.size() > 0) {
            UDPConnectBackRedirect redir = null;
            // make a new redirect message
            redir = new UDPConnectBackRedirect(guidToUse, sourceAddr, 
                                               portToContact);
            Iterator iter = redirect.iterator();
            while (iter.hasNext())
                ((ManagedConnection)iter.next()).send(redir);
            return;
        }

        // 2)
        // we used to do old style ConnectBacks here but no use - the chances
        // not having ANY redirct candidates is small.
    }


    /**
     * Basically, just get the correct parameters, create a temporary 
     * DatagramSocket, and send a Ping.
     */
    protected void handleUDPConnectBackRedirect(UDPConnectBackRedirect udp,
                                               Connection source) {
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection()) return;

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress addrToContact = udp.getConnectBackAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact.getAddress(),
                                         portToContact);
        if (_manager.isConnectedTo(endPoint.getAddress())) return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        String addrString = addrToContact.getHostAddress();
        Object placeHolder = _udpConnectBacks.get(addrString);
        if (placeHolder == null) {
            try {
                _udpConnectBacks.put(addrString, new Object());
            }
            catch (NoMoreStorageException nomo) {
                return;  // we've done too many connect backs, stop....
            }
        }
        else
            return;  // we've connected back to this guy recently....

        PingRequest pr = new PingRequest(guidToUse.bytes(), (byte) 1,
                                         (byte) 0);
        UDPService.instance().send(pr, addrToContact, portToContact);
    }



    /**
     * Basically, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     * This method will soon change to just forward a new message, a 
     * UDPConnectBackRedirect, to a third party.
     */
    protected void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connection source) {

        // two options here:
        // 1) if we are connected to an Ultrapeer that supports TCPCBRedirect
        //    messages, just transplant the info from this CB into a Redirect
        //    message and forward it to that Ultrapeer.
        // 2) if we can't a find a UP that supports redirect, then just do the
        //    old procedure (do the connect back yourself).  We will deprecate
        //    this as the user base upgrades....

        // 1)
        final int portToContact = tcp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        List redirect = _manager.getTCPRedirectUltrapeers();
        if (redirect.size() > 0) {
            TCPConnectBackRedirect redir = null;
            // make a new redirect message
            redir = new TCPConnectBackRedirect(sourceAddr, portToContact);
            Iterator iter = redirect.iterator();
            while (iter.hasNext())
                ((ManagedConnection)iter.next()).send(redir);
            return;
        }

        // 2)
        // we used to do old style ConnectBacks here but no use - the chances
        // not having ANY redirct candidates is small.
    }


    /**
     * Basically, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     */
    protected void handleTCPConnectBackRedirect(TCPConnectBackRedirect tcp,
                                                Connection source) {
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection()) return;

        final int portToContact = tcp.getConnectBackPort();
        final String addrToContact =tcp.getConnectBackAddress().getHostAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact, portToContact);
        if (_manager.isConnectedTo(endPoint.getAddress())) return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        Object placeHolder = _tcpConnectBacks.get(addrToContact);
        if (placeHolder == null) {
            try {
                _tcpConnectBacks.put(addrToContact, new Object());
            }
            catch (NoMoreStorageException nomo) {
                return;  // we've done too many connect backs, stop....
            }
        }
        else
            return;  // we've connected back to this guy recently....

        Thread connectBack = new ManagedThread( new Runnable() {
            public void run() {
                Socket sock = null;
                OutputStream os = null;
                try {
                    sock = Sockets.connect(addrToContact, portToContact, 12);
                    os = sock.getOutputStream();
                    os.write("\n\n".getBytes());
                } catch (IOException ignored) {
                } catch (SecurityException ignored) {
                } catch (Throwable t) {
                    ErrorService.error(t);
                } finally {
                    if(sock != null)
                        try { sock.close(); } catch(IOException ignored) {}
                    if(os != null)
                        try { os.close(); } catch(IOException ignored) {}
                }
            }
        }, "TCPConnectBackThread");
        connectBack.start();
    }


    /**
     * 1) confirm that the connection is Ultrapeer to Leaf, then send your
     * listening port in a PushProxyAcknowledgement.
     * 2) Also cache the client's client GUID.
     */
    protected void handlePushProxyRequest(PushProxyRequest ppReq,
                                          ManagedConnection source) {
        if (source.isSupernodeClientConnection() &&
            NetworkUtils.isValidAddress(RouterService.getAddress()) &&
            NetworkUtils.isValidPort(RouterService.getPort())) {
            String stringAddr = 
                NetworkUtils.ip2string(RouterService.getAddress());
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(stringAddr);
            } catch(UnknownHostException uhe) {
                ErrorService.error(uhe); // impossible
            }

            // 1)
            PushProxyAcknowledgement ack = 
                new PushProxyAcknowledgement(addr,RouterService.getPort(),
                                             ppReq.getClientGUID());
            source.send(ack);
            
            // 2)
            _pushRouteTable.routeReply(ppReq.getClientGUID().bytes(),
                                       source);
        }
    }

    /** This method should be invoked when this node receives a
     *  QueryStatusResponse message from the wire.  If this node is an
     *  Ultrapeer, we should update the Dynamic Querier about the status of
     *  the leaf's query.
     */    
    protected void handleQueryStatus(QueryStatusResponse resp,
                                     ManagedConnection leaf) {
        // message only makes sense if i'm a UP and the sender is a leaf
        if (!leaf.isSupernodeClientConnection())
            return;

        GUID queryGUID = resp.getQueryGUID();
        int numResults = resp.getNumResults();
        
        // get the QueryHandler and update the stats....
        DYNAMIC_QUERIER.updateLeafResultsForQuery(queryGUID, numResults);
    }


    /**
     * Sends the ping request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest request,
                                ManagedConnection connection) {
        if(request == null) {
            throw new NullPointerException("null ping");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _pingRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest request,
                                 ManagedConnection connection) {        
        if(request == null) {
            throw new NullPointerException("null query");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _queryRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastPingRequest(PingRequest ping) {
		if(ping == null) {
			throw new NullPointerException("null ping");
		}
        _pingRouteTable.routeReply(ping.getGUID(), FOR_ME_REPLY_HANDLER);
        broadcastPingRequest(ping, FOR_ME_REPLY_HANDLER, _manager);
    }

	/**
	 * Generates a new dynamic query.  This method is used to send a new 
	 * dynamic query from this host (the user initiated this query directly,
	 * so it's replies are intended for this node.
	 *
	 * @param query the <tt>QueryRequest</tt> instance that generates
	 *  queries for this dynamic query
	 * @throws <tt>NullPointerException</tt> if the <tt>QueryHandler</tt> 
	 *  argument is <tt>null</tt>
	 */
	public void sendDynamicQuery(QueryRequest query) {
		if(query == null) {
			throw new NullPointerException("null QueryHandler");
		}
		// get the result counter so we can track the number of results
		ResultCounter counter = 
			_queryRouteTable.routeReply(query.getGUID(), 
										FOR_ME_REPLY_HANDLER);
		if(RouterService.isSupernode()) {
			sendDynamicQuery(QueryHandler.createHandlerForMe(query, 
                                                             counter), 
							 FOR_ME_REPLY_HANDLER);
		} else {
            originateLeafQuery(query);
		} 
		
		// always send the query to your multicast people
		multicastQueryRequest(QueryRequest.createMulticastQuery(query));		
	}

	/**
	 * Initiates a dynamic query.  Only Ultrapeer should call this method,
	 * as this technique relies on fairly high numbers of connections to 
	 * dynamically adjust the TTL based on the number of results received, 
	 * the number of remaining connections, etc.
	 *
	 * @param qh the <tt>QueryHandler</tt> instance that generates
	 *  queries for this dynamic query
     * @param handler the <tt>ReplyHandler</tt> for routing replies for
     *  this query
	 * @throws <tt>NullPointerException</tt> if the <tt>ResultCounter</tt>
	 *  for the guid cannot be found -- this should never happen, or if any
	 *  of the arguments is <tt>null</tt>
	 */
	private void sendDynamicQuery(QueryHandler qh, ReplyHandler handler) {
		if(qh == null) {
			throw new NullPointerException("null QueryHandler");
		} else if(handler == null) {
			throw new NullPointerException("null ReplyHandler");
		} 
		DYNAMIC_QUERIER.addQuery(qh);
	}

    /**
     * Broadcasts the ping request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated PingReplyHandler.  This is called from the default
     * handlePingRequest and the default broadcastPingRequest(PingRequest)
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    private void broadcastPingRequest(PingRequest request,
                                      ReplyHandler receivingConnection,
                                      ConnectionManager manager) {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.

        //Broadcast the ping to other connected nodes (supernodes or older
        //nodes), but DON'T forward any ping not originating from me 
        //along leaf to ultrapeer connections.
        List list = manager.getInitializedConnections();
        int size = list.size();

        boolean randomlyForward = false;
        if(size > 3) randomlyForward = true;
        double percentToIgnore;
        for(int i=0; i<size; i++) {
            ManagedConnection mc = (ManagedConnection)list.get(i);
            if(!mc.isStable()) continue;
            if (receivingConnection == FOR_ME_REPLY_HANDLER || 
                (mc != receivingConnection && 
                 !mc.isClientSupernodeConnection())) {

                if(mc.supportsPongCaching()) {
                    percentToIgnore = 0.70;
                } else {
                    percentToIgnore = 0.90;
                }
                if(randomlyForward && 
                   (Math.random() < percentToIgnore)) {
                    continue;
                } else {
                    mc.send(request);
                }
            }
        }
    }

	/**
	 * Forwards the query request to any leaf connections.
	 *
	 * @param request the query to forward
	 * @param handler the <tt>ReplyHandler</tt> that responds to the
	 *  request appropriately
	 * @param manager the <tt>ConnectionManager</tt> that provides
	 *  access to any leaf connections that we should forward to
     */
	public final void forwardQueryRequestToLeaves(QueryRequest query,
                                                  ReplyHandler handler) {
		if(!RouterService.isSupernode()) return;
        
        //use query routing to route queries to client connections
        //send queries only to the clients from whom query routing 
        //table has been received
        List list = _manager.getInitializedClientConnections();
        List hitConnections = new ArrayList();
        for(int i=0; i<list.size(); i++) {
            ManagedConnection mc = (ManagedConnection)list.get(i);
            if(mc == handler) continue;
            if(mc.shouldForwardQuery(query)) {
                hitConnections.add(mc);
            }
        }

        //forward only to a quarter of the leaves in case the query is
        //very popular.
        if(list.size() > 8 && 
           (double)hitConnections.size()/(double)list.size() > .8) {
        	int startIndex = (int) Math.floor(
        			Math.random() * hitConnections.size() * 0.75);
            hitConnections = 
                hitConnections.subList(startIndex, startIndex+hitConnections.size()/4);
        }
        
        int notSent = list.size() - hitConnections.size();
        RoutedQueryStat.LEAF_DROP.addData(notSent);
        
        for(int i=0; i<hitConnections.size(); i++) {
            ManagedConnection mc = (ManagedConnection)hitConnections.get(i);
            
            // sendRoutedQueryToHost is not called because 
            // we have already ensured it hits the routing table
            // by filling up the 'hitsConnection' list.
            sendQueryRequest(query, mc, handler);
            RoutedQueryStat.LEAF_SEND.incrementStat();
        }
	}

	/**
	 * Factored-out method that sends a query to a connection that supports
	 * query routing.  The query is only forwarded if there's a hit in the
	 * query routing entries.
	 *
	 * @param query the <tt>QueryRequest</tt> to potentially forward
	 * @param mc the <tt>ManagedConnection</tt> to forward the query to
	 * @param handler the <tt>ReplyHandler</tt> that will be entered into
	 *  the routing tables to handle any replies
	 * @return <tt>true</tt> if the query was sent, otherwise <tt>false</tt>
	 */
	private boolean sendRoutedQueryToHost(QueryRequest query, ManagedConnection mc,
										  ReplyHandler handler) {
		if (mc.shouldForwardQuery(query)) {
			//A new client with routing entry, or one that hasn't started
			//sending the patch.
			sendQueryRequest(query, mc, handler);
			return true;
		}
		return false;
	}

    /**
     * Adds the QueryRequest to the unicaster module.  Not much work done here,
     * see QueryUnicaster for more details.
     */
    protected void unicastQueryRequest(QueryRequest query,
                                       ReplyHandler conn) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
				
		UNICASTER.addQuery(query, conn);
	}
	
    /**
     * Send the query to the multicast group.
     */
    protected void multicastQueryRequest(QueryRequest query) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
		// record the stat
		if (RECORD_STATS)
		    SentMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(query);
				
		MulticastService.instance().send(query);
	}	


    /**
     * Broadcasts the query request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated QueryReplyHandler.  This is called from teh default
     * handleQueryRequest and the default broadcastQueryRequest(QueryRequest)
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    private void forwardQueryToUltrapeers(QueryRequest query,
                                          ReplyHandler handler) {
		// Note the use of initializedConnections only.
		// Note that we have zero allocations here.
		
		//Broadcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forward any queries not originating from me 
		//along leaf to ultrapeer connections.
	 
		List list = _manager.getInitializedConnections();
        int limit = list.size();

		for(int i=0; i<limit; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);      
            forwardQueryToUltrapeer(query, handler, mc);  
        }
    }

    /**
     * Performs a limited broadcast of the specified query.  This is
     * useful, for example, when receiving queries from old-style 
     * connections that we don't want to forward to all connected
     * Ultrapeers because we don't want to overly magnify the query.
     *
     * @param query the <tt>QueryRequest</tt> instance to forward
     * @param handler the <tt>ReplyHandler</tt> from which we received
     *  the query
     */
    private void forwardLimitedQueryToUltrapeers(QueryRequest query,
                                                 ReplyHandler handler) {
		//Broadcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forward any queries not originating from me 
		//along leaf to ultrapeer connections.
	 
		List list = _manager.getInitializedConnections();
        int limit = list.size();

        int connectionsNeededForOld = OLD_CONNECTIONS_TO_USE;
		for(int i=0; i<limit; i++) {
            
            // if we've already queried enough old connections for
            // an old-style query, break out
            if(connectionsNeededForOld == 0) break;

			ManagedConnection mc = (ManagedConnection)list.get(i);
            
            // if the query is comiing from an old connection, try to
            // send it's traffic to old connections.  Only send it to
            // new connections if we only have a minimum number left
            if(mc.isGoodUltrapeer() && 
               (limit-i) > connectionsNeededForOld) {
                continue;
            }
            forwardQueryToUltrapeer(query, handler, mc);
            
            // decrement the connections to use
            connectionsNeededForOld--;
		}    
    }

    /**
     * Forwards the specified query to the specified Ultrapeer.  This
     * encapsulates all necessary logic for forwarding queries to
     * Ultrapeers, for example handling last hop Ultrapeers specially
     * when the receiving Ultrapeer supports Ultrapeer query routing,
     * meaning that we check it's routing tables for a match before sending 
     * the query.
     *
     * @param query the <tt>QueryRequest</tt> to forward
     * @param handler the <tt>ReplyHandler</tt> that sent the query
     * @param ultrapeer the Ultrapeer to send the query to
     */
    private void forwardQueryToUltrapeer(QueryRequest query, 
                                         ReplyHandler handler,
                                         ManagedConnection ultrapeer) {    
        // don't send a query back to the guy who sent it
        if(ultrapeer == handler) return;

        // make double-sure we don't send a query received
        // by a leaf to other Ultrapeers
        if(ultrapeer.isClientSupernodeConnection()) return;

        // special what is queries have version numbers attached to them - make
        // sure that the remote host can answer the query....
        if ((query.getCapabilitySelector() > 0) &&
            (ultrapeer.getRemoteHostCapabilitySelector() <
             CapabilitiesVM.FEATURE_SEARCH_MIN_SELECTOR)) return;

        // is this the last hop for the query??
		boolean lastHop = query.getTTL() == 1; 
           
        // if it's the last hop to an Ultrapeer that sends
        // query route tables, route it.
        if(lastHop &&
           ultrapeer.isUltrapeerQueryRoutingConnection()) {
            boolean sent = sendRoutedQueryToHost(query, ultrapeer, handler);
            if(sent && RECORD_STATS) {
                RoutedQueryStat.ULTRAPEER_SEND.incrementStat();
            } else if(RECORD_STATS) {
                RoutedQueryStat.ULTRAPEER_DROP.incrementStat();
            }
        } else {
            // otherwise, just send it out
            sendQueryRequest(query, ultrapeer, handler);
        }
    }


    /**
     * Originate a new query from this leaf node.
     *
     * @param qr the <tt>QueryRequest</tt> to send
     */
    private void originateLeafQuery(QueryRequest qr) {
		List list = _manager.getInitializedConnections();

        // only send to at most 4 Ultrapeers, as we could have more
        // as a result of race conditions - also, don't send what is new
        // requests down too many connections
        final int max = qr.isWhatIsNewRequest() ? 2 : 4;
	int start = !qr.isWhatIsNewRequest() ? 0 :
		(int) (Math.floor(Math.random()*(list.size()-1)));
        int limit = Math.min(max, list.size());
        final boolean wantsOOB = qr.desiresOutOfBandReplies();
        for(int i=start; i<start+limit; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);
            QueryRequest qrToSend = qr;
            if (wantsOOB && (mc.remoteHostSupportsLeafGuidance() < 0))
                qrToSend = QueryRequest.unmarkOOBQuery(qr);
            sendQueryRequest(qrToSend, mc, FOR_ME_REPLY_HANDLER);
        }
    }
    
    /**
     * Sends the passed query request, received on handler, 
     * to the passed sendConnection, only if the handler and
     * the sendConnection are authenticated to a common domain
     *
     * To only send it the route table has a hit, use
     * sendRoutedQueryToHost.
     *
     * @param queryRequest Query Request to send
     * @param sendConnection The connection on which to send out the query
     * @param handler The connection on which we originally
     * received the query
     */
    public void sendQueryRequest(QueryRequest request, 
								 ManagedConnection sendConnection, 
								 ReplyHandler handler) {
		if(request == null) {
			throw new NullPointerException("null query");
		}
		if(sendConnection == null) {
			throw new NullPointerException("null send connection");
		}
		if(handler == null) {
			throw new NullPointerException("null reply handler");
		}

        //send the query over this connection only if any of the following
        //is true:
        //1. The query originated from our node 
        //2. The connection under  consideration is an unauthenticated 
        //connection (normal gnutella connection)
        //3. It is an authenticated connection, and the connection on 
        //which we received query and this connection, are both 
        //authenticated to a common domain
        if((handler == FOR_ME_REPLY_HANDLER ||
            containsDefaultUnauthenticatedDomainOnly(sendConnection.getDomains())
            || Utilities.hasIntersection(handler.getDomains(), 
										 sendConnection.getDomains()))) {
            sendConnection.send(request);
		}		
    }
    
    /**
     * Originates a new query request to the ManagedConnection.
     *
     * @param request The query to send.
     * @param mc The ManagedConnection to send the query along
     * @return false if the query was not sent, true if so
     */
    public boolean originateQuery(QueryRequest query, ManagedConnection mc) {
        if( query == null )
            throw new NullPointerException("null query");
        if( mc == null )
            throw new NullPointerException("null connection");
        
        // special what is queries have version numbers attached to them - make
        // sure that the remote host can answer the query....
        if ((query.getCapabilitySelector() > 0) &&
            (mc.getRemoteHostCapabilitySelector() < 
             CapabilitiesVM.FEATURE_SEARCH_MIN_SELECTOR)
            ) return false;
        mc.originateQuery(query);
        return true;
    }
    

    /**
     * Checks if the passed set of domains contains only
     * default unauthenticated domain 
     * @param domains Set (of String) of domains to be tested
     * @return true if the passed set of domains contains only
     * default unauthenticated domain, false otherwise
     */
    private static boolean containsDefaultUnauthenticatedDomainOnly(Set domains) {
        //check if the set contains only one entry, and that entry is the
        //default unauthenticated domain 
        if((domains.size() == 1) && domains.contains(
            User.DEFAULT_UNAUTHENTICATED_DOMAIN))
            return true;
        else
            return false;
    }
    
    /**
     * Respond to the ping request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendPingReply(PingReply).
     * This method is called from the default handlePingRequest.
     */
    protected abstract void respondToPingRequest(PingRequest request,
                                                 ReplyHandler handler);

	/**
	 * Responds to a ping received over UDP -- implementations
	 * handle this differently from pings received over TCP, as it is 
	 * assumed that the requester only wants pongs from other nodes
	 * that also support UDP messaging.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param datagram the <tt>DatagramPacket</tt> containing the ping
     * @param handler the <tt>ReplyHandler</tt> instance from which the
     *  ping was received and to which pongs should be sent
	 */
    protected abstract void respondToUDPPingRequest(PingRequest request, 
													DatagramPacket datagram,
                                                    ReplyHandler handler);


    /**
     * Respond to the query request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is called from the default handleQueryRequest.
     */
    protected abstract boolean respondToQueryRequest(QueryRequest queryRequest,
                                                     byte[] clientGUID,
                                                     ReplyHandler handler);

    /**
     * The default handler for PingRequests received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the ping route table to route a ping reply.  If an appropriate route
     * doesn't exist, records the error statistics.  On sucessful routing,
     * the PingReply count is incremented.<p>
     *
     * In all cases, the ping reply is recorded into the host catcher.<p>
     *
     * Override as desired, but you probably want to call super.handlePingReply
     * if you do.
     */
    protected void handlePingReply(PingReply reply,
                                   ReplyHandler handler) {
        //update hostcatcher (even if the reply isn't for me)
        boolean newAddress = RouterService.getHostCatcher().add(reply);

        if(newAddress) {
            PongCacher.instance().addPong(reply);
        }

        //First route to originator in usual manner.
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(reply.getGUID());

        if(replyHandler != null) {
            replyHandler.handlePingReply(reply, handler);
        }
        else {
            if(RECORD_STATS) 
                RouteErrorStat.PING_REPLY_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
		boolean supportsUnicast = reply.supportsUnicast();
        
        //Then, if a marked pong from an Ultrapeer that we've never seen before,
        //send to all leaf connections except replyHandler (which may be null),
        //irregardless of GUID.  The leafs will add the address then drop the
        //pong as they have no routing entry.  Note that if Ultrapeers are very
        //prevalent, this may consume too much bandwidth.
		//Also forward any GUESS pongs to all leaves.
        if (newAddress && (reply.isUltrapeer() || supportsUnicast)) {
            List list=_manager.getInitializedClientConnections();
            for (int i=0; i<list.size(); i++) {
                ManagedConnection c = (ManagedConnection)list.get(i);
                Assert.that(c != null, "null c.");
                if (c!=handler && c!=replyHandler && c.allowNewPongs()) {
                    c.handlePingReply(reply, handler);
                }
            }
        }
    }

    /**
     * The default handler for QueryReplies received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the query route table to route a query reply.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the QueryReply count is incremented.<p>
     *
     * Override as desired, but you probably want to call super.handleQueryReply
     * if you do.  This is public for testing purposes.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler handler) {
        if(queryReply == null) {
            throw new NullPointerException("null query reply");
        }
        if(handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       handler);
            //Simple flow control: don't route this message along other
            //connections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.

            ReplyHandler rh = rrp.getReplyHandler();

            if(!shouldDropReply(rrp, rh, queryReply)) {                
                rh.handleQueryReply(queryReply, handler);
                // also add to the QueryUnicaster for accounting - basically,
                // most results will not be relevant, but since it is a simple
                // HashSet lookup, it isn't a prohibitive expense...
                UNICASTER.handleQueryReply(queryReply);

            } else {
				if(RECORD_STATS) {
					RouteErrorStat.HARD_LIMIT_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
                    final byte ttl = queryReply.getTTL();
                    if (ttl < RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length)
					RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[ttl].incrementStat();
                    else
					RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length-1].incrementStat();
                }                    
                handler.countDroppedMessage();
            }
        }
        else {
			if(RECORD_STATS) 
				RouteErrorStat.NO_ROUTE_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
    }

    /**
     * Checks if the <tt>QueryReply</tt> should be dropped for various reasons.
     *
     * Reason 1) The reply has already routed enough traffic.  Based on per-TTL
     * hard limits for the number of bytes routed for the given reply guid.
     * This algorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits have more liberal limits than high TTL
     * hits.  This ensures that hits that are closer to the query originator
     * -- hits for which we've already done most of the work, are not 
     * dropped unless we've routed a really large number of bytes for that
     * guid.  This method also checks that hard number of results that have
     * been sent for this GUID.  If this number is greater than a specified
     * limit, we simply drop the reply.
     *
     * Reason 2) The reply was meant for me -- DO NOT DROP.
     *
     * Reason 3) The TTL is 0, drop.
     *
     * @param rrp the <tt>ReplyRoutePair</tt> containing data about what's 
     *  been routed for this GUID
     * @param ttl the time to live of the query hit
     * @return <tt>true if the reply should be dropped, otherwise <tt>false</tt>
     */
    private boolean shouldDropReply(RouteTable.ReplyRoutePair rrp,
                                    ReplyHandler rh,
                                    QueryReply qr) {
        int ttl = qr.getTTL();
                                           
        // Reason 2 --  The reply is meant for me, do not drop it.
        if( rh == FOR_ME_REPLY_HANDLER ) return false;
        
        // Reason 3 -- drop if TTL is 0.
        if( ttl == 0 ) return true;                

        // Reason 1 ...
        
        int resultsRouted = rrp.getResultsRouted();

        // drop the reply if we've already sent more than the specified number
        // of results for this GUID
        if(resultsRouted > 100) return true;

        int bytesRouted = rrp.getBytesRouted();
        // send replies with ttl above 2 if we've routed under 50K 
        if(ttl > 2 && bytesRouted < 50    * 1024) return false;
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && bytesRouted < 200 * 1024) return false;
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && bytesRouted < 100  * 1024) return false;

        // if none of the above conditions holds true, drop the reply
        return true;
    }

    private void handleGiveStats(final GiveStatsVendorMessage gsm, 
                                             final ReplyHandler replyHandler) {
        StatisticVendorMessage statVM = null;
        try {
            //create the reply if we understand how
            if(StatisticVendorMessage.isSupported(gsm)) {
                statVM = new StatisticVendorMessage(gsm);
                //OK. Now send this message back to the client that asked for
                //stats
                replyHandler.handleStatisticVM(statVM);
            }
        } catch(IOException iox) {
            return; //what can we really do now?
        }
    }

    private void handleStatisticsMessage(final StatisticVendorMessage svm, 
                                         final ReplyHandler handler) {
        if(StatisticsSettings.RECORD_VM_STATS.getValue()) {
            Thread statHandler = new ManagedThread("Stat writer ") {
                public void managedRun() {
                    RandomAccessFile file = null;
                    try {
                        file = new RandomAccessFile("stats_log.log", "rw");
                        file.seek(file.length());//go to the end.
                        file.writeBytes(svm.getReportedStats()+"\n");
                    } catch (IOException iox) {
                        ErrorService.error(iox);
                    } finally {
                        if(file != null) {
                            try {
                                file.close();
                            } catch (IOException iox) {
                                ErrorService.error(iox);
                            }
                        }
                    }
                }
            };
            statHandler.start();
        }
    }

    /**
     *  If we get and SimppRequest, get the payload we need from the
     *  SimppManager and send the simpp bytes the the requestor in a SimppVM. 
     */
    private void handleSimppRequest(final SimppRequestVM simppReq, 
                                                  final ReplyHandler handler ) {
        if(simppReq.getVersion() > SimppRequestVM.VERSION)
            return; //we are not going to deal with these types of requests. 
        byte[] simppBytes = SimppManager.instance().getSimppBytes();
        SimppVM simppVM = new SimppVM(simppBytes);
        try {
            handler.handleSimppVM(simppVM);
        } catch(IOException iox) {//uanble to send the SimppVM. Nothing I can do
            return;
        }
    }
    

    /**
     * Passes on the SimppVM to the SimppManager which will authenticate it and
     * make sure we it's newer than the one we know about, and then make changes
     * to the settings as necessary, and cause new CapabilityVMs to be sent down
     * all connections.
     */
    private void handleSimppVM(final SimppVM simppVM) {
        //TODO1: Should this be in a thread of it's own? Maybe we should check
        //that this is a solicited SimppVM and if it is, then create a thread to
        //handle it otherwise not.
        if(false) //change this to check  if the SimppVM was solicited
            return;
        SimppManager.instance().checkAndUpdate(simppVM.getPayload());
    }


    /**
     * The default handler for PushRequests received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the push route table to route a push request.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the PushRequest count is incremented.
     *
     * Override as desired, but you probably want to call
     * super.handlePushRequest if you do.
     */
    protected void handlePushRequest(PushRequest request,
                                  ReplyHandler handler) {
        if(request == null) {
            throw new NullPointerException("null request");
        }
        if(handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushRouteTable.getReplyHandler(request.getClientGUID());

        if(replyHandler != null) {
            replyHandler.handlePushRequest(request, handler);
        }
        else {
			if(RECORD_STATS) 
				RouteErrorStat.PUSH_REQUEST_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
    }

    /**
     * Uses the ping route table to send a PingReply to the appropriate
     * connection.  Since this is used for PingReplies orginating here, no
     * stats are updated.
     */
    protected void sendPingReply(PingReply pong, ReplyHandler handler) {
        if(pong == null) {
            throw new NullPointerException("null pong");
        }

        if(handler == null) {
            throw new NullPointerException("null reply handler");
        }
 
        handler.handlePingReply(pong, null);
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    protected void sendQueryReply(QueryReply queryReply)
        throws IOException {
        
        if(queryReply == null) {
            throw new NullPointerException("null reply");
        }
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       FOR_ME_REPLY_HANDLER);
            rrp.getReplyHandler().handleQueryReply(queryReply, null);
        }
        else
            throw new IOException("no route for reply");
    }

    /**
     * Uses the push route table to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPushRequest(PushRequest push)
        throws IOException {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushRouteTable.getReplyHandler(push.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(push, FOR_ME_REPLY_HANDLER);
        else
            throw new IOException("no route for push");
    }
    
    /**
     * Sends a push request to the multicast network.  No lookups are
     * performed in the push route table, because the message will always
     * be broadcast to everyone.
     */
    protected void sendMulticastPushRequest(PushRequest push) {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        
        // must have a TTL of 1
        Assert.that(push.getTTL() == 1, "multicast push ttl not 1");
        
        MulticastService.instance().send(push);
        SentMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(push);
    }


    /**
     * Converts the passed responses to QueryReplies. Each QueryReply can
     * accomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in case the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt have any side effect, 
     * and does not modify the state of this object
     * @param responses The responses to be converted
     * @param queryRequest The query request corresponding to which we are
     * generating query replies.
     * @return Iterator (on QueryReply) over the Query Replies
     */
    public Iterator responsesToQueryReplies(Response[] responses,
                                            QueryRequest queryRequest) {
        return responsesToQueryReplies(responses, queryRequest, 10);
    }


    /**
     * Converts the passed responses to QueryReplies. Each QueryReply can
     * accomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in case the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt have any side effect, 
     * and does not modify the state of this object
     * @param responses The responses to be converted
     * @param queryRequest The query request corresponding to which we are
     * generating query replies.
     * @param REPLY_LIMIT the maximum number of responses to have in each reply.
     * @return Iterator (on QueryReply) over the Query Replies
     */
    private Iterator responsesToQueryReplies(Response[] responses,
                                             QueryRequest queryRequest,
                                             final int REPLY_LIMIT) {
        //List to store Query Replies
        List /*<QueryReply>*/ queryReplies = new LinkedList();
        
        // get the appropriate queryReply information
        byte[] guid = queryRequest.getGUID();
        byte ttl = (byte)(queryRequest.getHops() + 1);

		UploadManager um = RouterService.getUploadManager();

        //Return measured speed if possible, or user's speed otherwise.
        long speed = um.measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=ConnectionSettings.CONNECTION_SPEED.getValue();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        // limit the responses if we're not delivering this 
        // out-of-band and we have a lot of responses
        if(REPLY_LIMIT > 1 && 
           numHops > 2 && 
           numResponses > HIGH_HOPS_RESPONSE_LIMIT) {
            int j = 
                (int)(Math.random() * numResponses) % 
                (numResponses - HIGH_HOPS_RESPONSE_LIMIT);

            Response[] newResponses = 
                new Response[HIGH_HOPS_RESPONSE_LIMIT];
            for(int i=0; i<10; i++, j++) {
                newResponses[i] = responses[j];
            }
            responses = newResponses;
            numResponses = responses.length;
        }
        while (numResponses > 0) {
            int arraySize;
            // if there are more than 255 responses,
            // create an array of 255 to send in the queryReply
            // otherwise, create an array of whatever size is left.
            if (numResponses < REPLY_LIMIT) {
                // break;
                arraySize = numResponses;
            }
            else
                arraySize = REPLY_LIMIT;

            Response[] res;
            // a special case.  in the common case where there
            // are less than 256 responses being sent, there
            // is no need to copy one array into another.
            if ( (index == 0) && (arraySize < REPLY_LIMIT) ) {
                res = responses;
            }
            else {
                res = new Response[arraySize];
                // copy the reponses into bite-size chunks
                for(int i =0; i < arraySize; i++) {
                    res[i] = responses[index];
                    index++;
                }
            }

            // decrement the number of responses we have left
            numResponses-= arraySize;

			// see if there are any open slots
			boolean busy = !um.isServiceable();
            boolean uploaded = um.hadSuccesfulUpload();
			
            // We only want to return a "reply to multicast query" QueryReply
            // if the request travelled a single hop.
			boolean mcast = queryRequest.isMulticast() && 
                (queryRequest.getTTL() + queryRequest.getHops()) == 1;
			
            
			if ( mcast ) {
                ttl = 1; // not strictly necessary, but nice.
            }
            
            List replies =
                createQueryReply(guid, ttl, speed, res, 
                                 _clientGUID, busy, uploaded, 
                                 measuredSpeed, mcast);

            //add to the list
            queryReplies.addAll(replies);

        }//end of while
        
        return queryReplies.iterator();
    }

    /**
     * Abstract method for creating query hits.  Subclasses must specify
     * how this list is created.
     *
     * @return a <tt>List</tt> of <tt>QueryReply</tt> instances
     */
    protected abstract List createQueryReply(byte[] guid, byte ttl,
                                            long speed, 
                                             Response[] res, byte[] clientGUID, 
                                             boolean busy, 
                                             boolean uploaded, 
                                             boolean measuredSpeed, 
                                             boolean isFromMcast);

    /**
     * Handles a message to reset the query route table for the given
     * connection.
     *
     * @param rtm the <tt>ResetTableMessage</tt> for resetting the query
     *  route table
     * @param mc the <tt>ManagedConnection</tt> for which the query route
     *  table should be reset
     */
    private void handleResetTableMessage(ResetTableMessage rtm,
                                         ManagedConnection mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // reset the query route table for this connection
        synchronized (mc.getQRPLock()) {
            mc.resetQueryRouteTable(rtm);
        }

        // if this is coming from a leaf, make sure we update
        // our tables so that the dynamic querier has correct
        // data
        if(mc.isLeafConnection()) {
            _lastQueryRouteTable = createRouteTable();
        }
    }

    /**
     * Handles a message to patch the query route table for the given
     * connection.
     *
     * @param rtm the <tt>PatchTableMessage</tt> for patching the query
     *  route table
     * @param mc the <tt>ManagedConnection</tt> for which the query route
     *  table should be patched
     */
    private void handlePatchTableMessage(PatchTableMessage ptm,
                                         ManagedConnection mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // patch the query route table for this connection
        synchronized(mc.getQRPLock()) {
            mc.patchQueryRouteTable(ptm);
        }

        // if this is coming from a leaf, make sure we update
        // our tables so that the dynamic querier has correct
        // data
        if(mc.isLeafConnection()) {
            _lastQueryRouteTable = createRouteTable();
        }
    }

    private void updateMessage(QueryRequest request, ReplyHandler handler) {
        if(! (handler instanceof Connection) )
            return;
        Connection c  = (Connection) handler;
        if(request.getHops()==1 && c.isOldLimeWire()) {
            if(StaticMessages.updateReply ==null) 
                return;
            QueryReply qr
                 = new QueryReply(request.getGUID(),StaticMessages.updateReply);
            try {
                sendQueryReply(qr);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Utility method for checking whether or not the given connection
     * is able to pass QRP messages.
     *
     * @param c the <tt>Connection</tt> to check
     * @return <tt>true</tt> if this is a QRP-enabled connection,
     *  otherwise <tt>false</tt>
     */
    private static boolean isQRPConnection(Connection c) {
        if(c.isSupernodeClientConnection()) return true;
        if(c.isUltrapeerQueryRoutingConnection()) return true;
        return false;
    }

    /** Thread the processing of QRP Table delivery. */
    private class QRPPropagator extends ManagedThread {
        public QRPPropagator() {
            setName("QRPPropagator");
            setDaemon(true);
        }

        /** While the connection is not closed, sends all data delay. */
        public void managedRun() {
            try {
                while (true) {
					// Check for any scheduled QRP table propagations
					// every 10 seconds
                    Thread.sleep(10*1000);
    				forwardQueryRouteTables();
                }
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }

    } //end QRPPropagator


    /**
     * Sends updated query routing tables to all connections which haven't
     * been updated in a while.  You can call this method as often as you want;
     * it takes care of throttling.
     *     @modifies connections
     */    
    private void forwardQueryRouteTables() {
		//Check the time to decide if it needs an update.
		long time = System.currentTimeMillis();

		//For all connections to new hosts c needing an update...
		List list=_manager.getInitializedConnections();
		QueryRouteTable table = null;
		List /* of RouteTableMessage */ patches = null;
		QueryRouteTable lastSent = null;
		for(int i=0; i<list.size(); i++) {                        
			ManagedConnection c=(ManagedConnection)list.get(i);
			

			// continue if I'm an Ultrapeer and the node on the
			// other end doesn't support Ultrapeer-level query
			// routing
			if(RouterService.isSupernode()) { 
				// only skip it if it's not an Ultrapeer query routing
				// connection
				if(!c.isUltrapeerQueryRoutingConnection()) { 
					continue;
				}
			} 				
			// otherwise, I'm a leaf, and don't send routing
			// tables if it's not a connection to an Ultrapeer
			// or if query routing is not enabled on the connection
			else if (!(c.isClientSupernodeConnection() && 
					   c.isQueryRoutingEnabled())) {
				continue;
			}
			
			// See if it is time for this connections QRP update
			// This call is safe since only this thread updates time
			if (time<c.getNextQRPForwardTime())
				continue;


			c.incrementNextQRPForwardTime(time);
				
			// Create a new query route table if we need to
			if (table == null) {
				table = createRouteTable();
                _lastQueryRouteTable = table;
			} 

			//..and send each piece.
			
			// Because we tend to send the same list of patches to lots of
			// Connections, we can reuse the list of RouteTableMessages
			// between those connections if their last sent
			// table is exactly the same.
			// This allows us to only reduce the amount of times we have
			// to call encode.
			
			//  (This if works for 'null' sent tables too)
			if( lastSent == c.getQueryRouteTableSent() ) {
			    // if we have not constructed the patches yet, then do so.
			    if( patches == null )
			        patches = table.encode(lastSent, true);
			}
			// If they aren't the same, we have to encode a new set of
			// patches for this connection.
			else {
			    lastSent = c.getQueryRouteTableSent();
			    patches = table.encode(lastSent, true);
            }
            
            // If sending QRP tables is turned off, don't send them.  
            if(!ConnectionSettings.SEND_QRP.getValue()) {
                return;
            }
            
		    for(Iterator iter = patches.iterator(); iter.hasNext();) {
		        c.send((RouteTableMessage)iter.next());
    	    }
    	    
            c.setQueryRouteTableSent(table);
		}
    }

    /**
     * Accessor for the most recently calculated <tt>QueryRouteTable</tt>
     * for this node.  If this node is an Ultrapeer, the table will include
     * all data for leaf nodes in addition to data for this node's files.
     *
     * @return the <tt>QueryRouteTable</tt> for this node
     */
    public QueryRouteTable getQueryRouteTable() {
        return _lastQueryRouteTable;
    }

    /**
     * Creates a query route table appropriate for forwarding to connection c.
     * This will not include information from c.
     *     @requires queryUpdateLock held
     */
    private static QueryRouteTable createRouteTable() {
        QueryRouteTable ret = _fileManager.getQRT();
        
        // Add leaves' files if we're an Ultrapeer.
        if(RouterService.isSupernode()) {
            addQueryRoutingEntriesForLeaves(ret);
        }
        return ret;
    }


	/**
	 * Adds all query routing tables of leaves to the query routing table for
	 * this node for propagation to other Ultrapeers at 1 hop.
	 *
	 * @param qrt the <tt>QueryRouteTable</tt> to add to
	 */
	private static void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {
		List leaves = _manager.getInitializedClientConnections();
		
		for(int i=0; i<leaves.size(); i++) {
			ManagedConnection mc = (ManagedConnection)leaves.get(i);
        	synchronized (mc.getQRPLock()) {
                QueryRouteTable qrtr = mc.getQueryRouteTableReceived();
				if(qrtr != null) {
					qrt.addAll(qrtr);
				}
			}
		}
	}

    
    /** Add a message listener if you want to be notified.  Please unregister
     *  yourself ASAP.
     */
    public void registerMessageListener(GUID guid, MessageListener ml) {
        _messageListeners.put(guid, ml);
    }
    
    /** Unregister a message listener for a certain guid.
     */
    public void unregisterMessageListener(GUID guid) {
        _messageListeners.remove(guid);
    }


    /**
     * responds to a request for the list of ultrapeers or leaves.  It is sent right back to the
     * requestor on the UDP receiver thread.
     * @param msg the request message
     * @param handler the UDPHandler to send it to.
     */
    private void handleUDPCrawlerPing(UDPCrawlerPing msg, ReplyHandler handler){
    	
    	//make sure the same person doesn't request too often
    	//note: this should only happen on the UDP receiver thread, that's why
    	//I'm not locking it.
    	if (!_promotionManager.allowUDPPing(handler))
    		return; 
    	UDPCrawlerPong newMsg = new UDPCrawlerPong(msg);
    	handler.handleUDPCrawlerPong(newMsg);
    }
    
    private static class QueryResponseBundle {
        public final QueryRequest _query;
        public final Response[] _responses;
        
        public QueryResponseBundle(QueryRequest query, Response[] responses) {
            _query = query;
            _responses = responses;
        }
    }


    /** Can be run to invalidate out-of-band ACKs that we are waiting for....
     */
    private class Expirer implements Runnable {
        public void run() {
            try {
                Set toRemove = new HashSet();
                synchronized (_outOfBandReplies) {
                    Iterator keys = _outOfBandReplies.keySet().iterator();
                    while (keys.hasNext()) {
                        GUID.TimedGUID currQB = (GUID.TimedGUID) keys.next();
                        if ((currQB != null) && (currQB.shouldExpire()))
                            toRemove.add(currQB);
                    }
                    // done iterating through _outOfBandReplies, remove the 
                    // keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext())
                        _outOfBandReplies.remove(keys.next());
                }
            } 
            catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }


    /** This is run to clear out the registry of connect back attempts...
     *  Made package access for easy test access.
     */
    static class ConnectBackExpirer implements Runnable {
        public void run() {
            try {
                _tcpConnectBacks.clear();
                _udpConnectBacks.clear();
            } 
            catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }


}
