package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;

import com.sun.java.util.collections.*;
import java.io.IOException;
import java.io.OutputStream;
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
    private static final int OLD_CONNECTIONS_TO_USE = 5;

    /**
     * The GUID we attach to QueryReplies to allow PushRequests in
     * responses.
     */
    protected byte[] _clientGUID;

    /**
     * The SecretKey used for QueryKey generation.
     */
    protected QueryKey.SecretKey _secretKey;
    /**
     * The LAST SecretKey used for QueryKey generation.  Used to honor older
     * (but not too old) requests.
     */
    protected QueryKey.SecretKey _lastSecretKey;

    /**
     * The SecretPad used for QueryKey generation.
     */
    protected QueryKey.SecretPad _secretPad;
    /**
     * The LAST SecretPad used for QueryKey generation.  Used to honor older
     * (but not too old) requests.
     */
    protected QueryKey.SecretPad _lastSecretPad;


	/**
	 * Reference to the <tt>ReplyHandler</tt> for messages intended for 
	 * this node.
	 */
    private final ReplyHandler FOR_ME_REPLY_HANDLER = 
		ForMeReplyHandler.instance();
		
    /** 
     * The time to wait between route table updates, in milliseconds. 
     */
    private long QUERY_ROUTE_UPDATE_TIME=1000*60*5; //5 minutes
    private int MAX_ROUTE_TABLE_SIZE=50000;        //actually 100,000 entries

    /**
     * Maps PingRequest GUIDs to PingReplyHandlers.  Stores 2-4 minutes,
     * typically around 2500 entries, but never more than 100,000 entries.
     */
    private RouteTable _pingRouteTable = new RouteTable(2*60, 
														MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typically around 13000 entries, but never more than 100,000 entries.
     */
    private RouteTable _queryRouteTable = new RouteTable(5*60,
														 MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typically around 3500 entries, but never more than 100,000
     * entries.  
     */
    private RouteTable _pushRouteTable = new RouteTable(7*60,
														MAX_ROUTE_TABLE_SIZE);

    /** How long to buffer up out-of-band replies.
     */
    private static final long CLEAR_TIME = 30 * 1000; // 30 seconds

    /** The maximum amount of UDP replies to buffer up
     */
    static int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps track of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryReply
     */
    private Hashtable _outOfBandReplies = new Hashtable();

	/**
	 * Constant handle to the <tt>QueryUnicaster</tt> since it is called
	 * upon very frequently.
	 */
	protected final QueryUnicaster UNICASTER = QueryUnicaster.instance();

	/**
	 * Constant for the <tt>DynamicQueryHandler</tt> that handles dynamically
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
     * Creates a MessageRouter.  Must call initialize before using.
     */
    protected MessageRouter() {
        try {
            _clientGUID = new GUID(GUID.fromHexString(
                SettingsManager.instance().getClientID())).bytes();
        }
        catch (IllegalArgumentException e) {
            //This should never happen! But if it does, we can recover.
            _clientGUID = Message.makeGuid();
        }
        _secretKey = QueryKey.generateSecretKey();
        _secretPad = QueryKey.generateSecretPad();
    }

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize() {
        _manager = RouterService.getConnectionManager();
		_callback = RouterService.getCallback();
		_fileManager = RouterService.getFileManager();
		DYNAMIC_QUERIER.start();
	    QRP_PROPAGATOR.start();

        // schedule a runner to clear unused out-of-band replies
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
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
            handleQueryReply((QueryReply)msg, receivingConnection);
		} else if (msg instanceof PushRequest) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, receivingConnection);
		} else if (msg instanceof RouteTableMessage) {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handleRouteTableMessage((RouteTableMessage)msg,
                                    receivingConnection);
		} 
        else if (msg instanceof MessagesSupportedVendorMessage) 
            receivingConnection.handleVendorMessage((VendorMessage) msg);
        else if (msg instanceof HopsFlowVendorMessage)
            receivingConnection.handleVendorMessage((VendorMessage) msg);
        else if (msg instanceof TCPConnectBackVendorMessage)
            handleTCPConnectBackRequest((TCPConnectBackVendorMessage) msg,
                                        receivingConnection);
        else if (msg instanceof UDPConnectBackVendorMessage)
            handleUDPConnectBackRequest((UDPConnectBackVendorMessage) msg,
                                        receivingConnection);

        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
		//forwardQueryRouteTables();      
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
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
            handleQueryReply((QueryReply)msg, handler);
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
            handleLimeACKMessage((LimeACKVendorMessage)msg, datagram);
        }
        
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
        // Increment hops and decrement TTL.
        msg.hop();

		InetAddress address = datagram.getAddress();
		int port = datagram.getPort();
		
        if (NetworkUtils.isLocalAddress(address))
            return;
		
		ReplyHandler handler = new UDPReplyHandler(address, port);
		
        if (msg instanceof QueryRequest) {
            //TODO: compare QueryKey with old generation params.  if it matches
            //send a new one generated with current params 
            //if (hasValidQueryKey(address, port, (QueryRequest) msg)) {
            //    sendAcknowledgement(datagram, msg.getGUID());
                // a TTL above zero may indicate a malicious client, as UDP
                // messages queries should not be sent with TTL above 1.
                if(msg.getTTL() > 0) return;
                if(!handleUDPQueryRequestPossibleDuplicate(
                  (QueryRequest)msg, handler) ) {
                    ReceivedMessageStatHandler.MULTICAST_DUPLICATE_QUERIES.addMessage(msg);
                }
           // }
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
	//	} else if(msg instanceof PushRequest) {
	//		if(RECORD_STATS)
	//			ReceivedMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(msg);
	//		handlePushRequest((PushRequest)msg, handler);
		}
    }


    /**
     * Returns true if the Query has a valid QueryKey.  false if it isn't
     * present or valid.
     */
    protected boolean hasValidQueryKey(InetAddress ip, int port, 
                                       QueryRequest qr) {
        if (qr.getQueryKey() == null)
            return false;
        QueryKey computedQK = QueryKey.getQueryKey(ip, port, _secretKey,
                                                   _secretPad);
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
            return PingReply.create(guid, (byte)1);
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
    protected void handlePingRequest(PingRequest pingRequest,
                                     ReplyHandler receivingConnection) {
        if(pingRequest.getTTL() > 0)
            broadcastPingRequest(pingRequest, receivingConnection,
                                 _manager);

        respondToPingRequest(pingRequest);
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
            respondToUDPPingRequest(pingRequest, datagram);
    }
    

    /**
     * Generates a QueryKey for the source (described by datagram) and sends the
     * QueryKey to it via a QueryKey pong....
     */
    protected void sendQueryKeyPong(PingRequest pr, DatagramPacket datagram) {

        boolean isSupernode = RouterService.isSupernode();
        if (isSupernode) { // only UPs should be doling out QKs....
            // generate a QueryKey (quite quick - current impl. (DES) is super
            // fast!
            InetAddress address = datagram.getAddress();
            int port = datagram.getPort();
            QueryKey key = QueryKey.getQueryKey(address, port, 
                                                _secretKey, _secretPad);

            // respond with Pong with QK, as GUESS requires....
            int num_files = RouterService.getNumSharedFiles();
            int kilobytes = RouterService.getSharedFileSize()/1024;           

            PingReply reply = 
                PingReply.createQueryKeyReply(pr.getGUID(), (byte)1, key);
            UDPService.instance().send(reply, datagram.getAddress(),
                                       datagram.getPort());
            if (RECORD_STATS)
                SentMessageStatHandler.UDP_PING_REPLIES.addMessage(reply);
        }
    }


    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {
        if (reply.getQueryKey() != null) {
            // this is a PingReply in reply to my QueryKey Request - 
            //consume the Pong and return, don't process as usual....
            UNICASTER.handleQueryKeyPong(reply);
            return;
        }

        // also add the sender of the pong if different from the host
        // described in the reply...
        if((reply.getPort() != port) || 
           (!reply.getIP().equals(address.getHostAddress()))) {
            UNICASTER.addUnicastEndpoint(address, port);
		}

		// TODO: are we sure we want to do this?
        // notify neighbors of new unicast endpoint...
//         Iterator guessUltrapeers = 
// 			_manager.getConnectedGUESSUltrapeers().iterator();
//         while (guessUltrapeers.hasNext()) {
//             ManagedConnection currMC = 
// 				(ManagedConnection) guessUltrapeers.next();
// 			currMC.handlePingReply(reply, handler);
//         }
        
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

		if(handler.isSupernodeClientConnection()) {
            if (request.desiresOutOfBandReplies() &&
                (handler instanceof Connection)) {
                // this query came from a leaf - so check if it desires out-of-band
                // responses and make sure that the IP it advertises is legit -
                // if it isn't drop away....
                // no need to check the port - if you are attacking yourself you
                // got problems
                Connection conn = (Connection) handler;
                String remoteAddr = conn.getInetAddress().getHostAddress();
                if (!request.getReplyAddress().equals(remoteAddr))
                    return;
                // continue below, everything looks good
            }

			if(handler.isGoodConnection()) {
				sendDynamicQuery(QueryHandler.createHandlerForNewLeaf(request, 
																	  handler), 
								 handler, counter);
			} else {
				sendDynamicQuery(QueryHandler.createHandlerForOldLeaf(request,
																	  handler), 
								 handler, counter);
			}
		} else if(request.getTTL() > 0 && !RouterService.isShieldedLeaf()) {
            // send the request to intra-Ultrapeer connections -- this does
			// not send the request to leaves
            if(handler.isGoodConnection()) {
                // send it to everyone
                forwardQueryToUltrapeers(request, handler);
            } else {
                forwardLimitedQueryToUltrapeers(request, handler);
            }
		}
			
        if (locallyEvaluate) {
            // always forward any queries to leaves -- this only does
            // anything when this node's an Ultrapeer
            forwardQueryRequestToLeaves(request, handler);
            
            // if I'm not firewalled AND the source isn't firewalled reply ....
            if (request.isFirewalledSource() &&
                !RouterService.acceptedIncomingConnection())
                return;
            respondToQueryRequest(request, _clientGUID);
        }
    }

    /** Handles a ACK message - looks up the QueryReply and sends it out of
     *  band.
     */
    protected void handleLimeACKMessage(LimeACKVendorMessage ack,
                                        DatagramPacket datagram) {
        GUID refGUID = new GUID(ack.getGUID());
        QueryReply reply = (QueryReply) _outOfBandReplies.remove(refGUID);
        if (reply != null) {
            InetAddress addr = datagram.getAddress();
            int port = datagram.getPort();
            UDPService.instance().send(reply, addr, port);
        }
        // else some sort of routing error or attack?
        // TODO: tally some stat stuff here
    }

    /**
     * Basically, just get the correct parameters, create a temporary 
     * DatagramSocket, and send a Ping.
     */
    protected void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connection source) {
        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress addrToContact = null;
        try {
            addrToContact = source.getInetAddress();
        }
        catch (IllegalStateException ise) {
            return;
        }
        PingRequest pr = new PingRequest(guidToUse.bytes(), (byte) 1,
                                         (byte) 0);
        UDPService.instance().send(pr, addrToContact, portToContact);
    }


    /**
     * Basically, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     */
    protected void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connection source) {
        int portToContact = tcp.getConnectBackPort();
        InetAddress addrToContact = null;
        try {
            addrToContact = source.getInetAddress();
        }
        catch (IllegalStateException ise) {
            return;
        }
        try {
            Socket sock = new Socket(addrToContact, portToContact);
            OutputStream os = sock.getOutputStream();
            os.write("\n\n".getBytes());
            sock.close();
        }
        catch (IOException ioe) {
            // whatever
        }
        catch (SecurityException se) {
            // whatever
        }
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
			// create a query to send to leaves
			forwardQueryRequestToLeaves(query, 
										FOR_ME_REPLY_HANDLER);
			sendDynamicQuery(QueryHandler.createHandler(query, 
														FOR_ME_REPLY_HANDLER), 
							 FOR_ME_REPLY_HANDLER, counter);
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
	 * @throws <tt>NullPointerException</tt> if the <tt>ResultCounter</tt>
	 *  for the guid cannot be found -- this should never happen, or if any
	 *  of the arguments is <tt>null</tt>
	 */
	private void sendDynamicQuery(QueryHandler qh, ReplyHandler handler,
								  ResultCounter counter) {
		if(qh == null) {
			throw new NullPointerException("null QueryHandler");
		} else if(handler == null) {
			throw new NullPointerException("null ReplyHandler");
		} else if(counter == null) {
			throw new NullPointerException("null ResultCounter");
		}

		qh.setResultCounter(counter);
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
    protected void broadcastPingRequest(PingRequest request,
                                        ReplyHandler receivingConnection,
                                        ConnectionManager manager) {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.

        //Broadcast the ping to other connected nodes (supernodes or older
        //nodes), but DON'T forward any ping not originating from me 
        //along leaf to ultrapeer connections.
        List list=manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++) {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if (   receivingConnection==FOR_ME_REPLY_HANDLER
				   || (c!=receivingConnection
                     && !c.isClientSupernodeConnection())) {
                c.send(request);
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
	protected void forwardQueryRequestToLeaves(QueryRequest request,
											   ReplyHandler handler) {
		if(!RouterService.isSupernode()) return;
        //use query routing to route queries to client connections
        //send queries only to the clients from whom query routing 
        //table has been received
        List list = _manager.getInitializedClientConnections2();
        for(int i=0; i<list.size(); i++) {
            ManagedConnection mc = (ManagedConnection)list.get(i);
            if(mc != handler) {
				boolean sent = sendRoutedQueryToHost(request, mc, handler);
				if(sent && RECORD_STATS) {
					RoutedQueryStat.LEAF_SEND.incrementStat();
				} else if(RECORD_STATS) {
					RoutedQueryStat.LEAF_DROP.incrementStat();
				}				
            }
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
		//TODO:
		//because of some very obscure optimization rules, it's actually
		//possible that qi could be non-null but not initialized.  Need
		//to be more careful about locking here.
		ManagedConnectionQueryInfo qi = mc.getQueryRouteState();
		if (qi.lastReceived==null) 
			return false;
		else if (qi.lastReceived.contains(query)) {
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
    protected synchronized void unicastQueryRequest(QueryRequest query,
                                                    ReplyHandler conn) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
				
		UNICASTER.addQuery(query, conn);
	}
	
    /**
     * Send the query to the multicast group.
     */
    protected synchronized void multicastQueryRequest(QueryRequest query) {
        
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
	 
		List list = _manager.getInitializedConnections2();
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
	 
		List list = _manager.getInitializedConnections2();
        int limit = list.size();

		// are we sending it to the last hop??
		boolean lastHop = query.getTTL() == 1;
        int connectionsNeededForOld = OLD_CONNECTIONS_TO_USE;
		for(int i=0; i<limit; i++) {
            
            // if we've already queried enough old connections for
            // an old-style query, break out
            if(connectionsNeededForOld == 0) break;

			ManagedConnection mc = (ManagedConnection)list.get(i);
            
            // if the query is comiing from an old connection, try to
            // send it's traffic to old connections.  Only send it to
            // new connections if we only have a minimum number left
            if(mc.isGoodConnection() && 
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
		List list = _manager.getInitializedConnections2();

        // only send to at most 3 Ultrapeers
        int limit = Math.min(3, list.size());
        for(int i=0; i<limit; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);            
            sendQueryRequest(qr, mc, FOR_ME_REPLY_HANDLER);
        }
    }
    
    /**
     * Sends the passed query request, received on handler, 
     * to the passed sendConnection, only if the handler and
     * the sendConnection are authenticated to a common domain
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

		//TODO:: make sure to look at query routing tables!!!

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
    protected abstract void respondToPingRequest(PingRequest request);

	/**
	 * Responds to a ping received over UDP -- implementations
	 * handle this differently from pings received over TCP, as it is 
	 * assumed that the requester only wants pongs from other nodes
	 * that also support UDP messaging.
	 *
	 * @param request the <tt>PingRequest</tt> to service
	 */
    protected abstract void respondToUDPPingRequest(PingRequest request, 
													DatagramPacket datagram);


    /**
     * Respond to the query request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is called from the default handleQueryRequest.
     */
    protected abstract void respondToQueryRequest(QueryRequest queryRequest,
                                                  byte[] clientGUID);
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
        boolean newAddress = 
		    RouterService.getHostCatcher().add(reply, handler);

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
        if (newAddress && (reply.isMarked() || supportsUnicast)) {
            List list=_manager.getInitializedClientConnections2();
            for (int i=0; i<list.size(); i++) {
                ManagedConnection c = (ManagedConnection)list.get(i);
                if (c!=handler && c!=replyHandler) {
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

			// TODO: What happens if we get a TTL=0 query that's not intended
			// for us?  At first glance, it looks like we keep forwarding it!
            if(!shouldDropReply(rrp.getBytesRouted(), queryReply.getTTL()) ||
			   rh == FOR_ME_REPLY_HANDLER) {
                
                rh.handleQueryReply(queryReply, handler);
                // also add to the QueryUnicaster for accounting - basically,
                // most results will not be relevant, but since it is a simple
                // HashSet lookup, it isn't a prohibitive expense...
                UNICASTER.handleQueryReply(queryReply);

            } else {
				if(RECORD_STATS) 
					RouteErrorStat.HARD_LIMIT_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
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
     * Checks if the <tt>QueryReply</tt> should be dropped based on per-TTL
     * hard limits for the number of bytes routed for the given reply guid.
     * This algorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits have more liberal limits than high TTL
     * hits.  This ensures that hits that are closer to the query originator
     * -- hits for which we've already done most of the work, are not 
     * dropped unless we've routed a really large number of bytes for that
     * guid.
     */
    private static boolean shouldDropReply(int bytesRouted, int ttl) {
        // send replies with ttl above 2 if we've routed under 50K 
        if(ttl > 2 && bytesRouted < 50    * 1024) return false;
        // send replies with ttl 0 if we've routed under 50K, as this 
		// shouldn't happen 
        if(ttl == 0 && bytesRouted < 50   * 1024) return false;
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && bytesRouted < 200 * 1024) return false;
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && bytesRouted < 100  * 1024) return false;

        // if none of the above conditions holds true, drop the reply
        return true;
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
     * @throws IOException if no appropriate route exists.
     */
    protected void sendPingReply(PingReply pong)
        throws IOException {
        if(pong == null) {
            throw new NullPointerException("null pong");
        }
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(pong.getGUID());

        if(replyHandler != null)
            replyHandler.handlePingReply(pong, null);
        else
            throw new IOException("could not find reply handler");
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    protected void sendQueryReply(QueryRequest query, QueryReply queryReply)
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
            // Here we can do a couple of things - if the query wants
            // out-of-band replies we should do things differently.  else just
            // send it off as usual.  only send out-of-band if you aren't 
            // directly connected.
            if (!query.desiresOutOfBandReplies() || (query.getHops() < 2)) 
                rrp.getReplyHandler().handleQueryReply(queryReply, null);
            else {
                // special out of band handling....
                InetAddress addr = null;
                try {
                    addr = InetAddress.getByName(query.getReplyAddress());
                }
                catch (UnknownHostException uhe) {
                    throw new IOException("Couldn't locate host!!");
                }
                int port = query.getReplyPort();
                
                if (!UDPService.instance().canReceiveSolicited()) 
                    // if i can't receive solicited traffic, then just send
                    // the reply out of band
                    UDPService.instance().send(queryReply, addr, port);
                else {
                    // send a ReplyNumberVM to the host - he'll ACK you if he
                    // wants the whole shebang
                    ReplyNumberVendorMessage vm = null;
                    GUID guid = new GUID(query.getGUID());
                    try {
                        int resultCount = queryReply.getResultCount();
                        vm = new ReplyNumberVendorMessage(guid, resultCount);
                    }
                    catch (BadPacketException bpe) {
                        throw new IOException("Could not construct VM:" + bpe);
                    }
                    // store reply by guid for later retrieval1
                    synchronized (_outOfBandReplies) {
                        if (_outOfBandReplies.size() < MAX_BUFFERED_REPLIES) {
                            _outOfBandReplies.put(new TimedGUID(guid), 
                                                  queryReply);
                            UDPService.instance().send(vm, addr, port);
                        }
                        // else "tough noogies, i'm too busy" - shouldn't
                        // happen much
                    }
                }
            }
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
    protected void sendPushRequest(PushRequest push)
        throws IOException {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushRouteTable.getReplyHandler(push.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(push, null);
        else
            throw new IOException("no route for push");
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
        //List to store Query Replies
        List /*<QueryReply>*/ queryReplies = new LinkedList();
        
        // get the appropriate queryReply information
        byte[] guid = queryRequest.getGUID();
        byte ttl = (byte)(queryRequest.getHops() + 1);
        int port = RouterService.getPort();
        byte[] ip = RouterService.getAddress();

		UploadManager um = RouterService.getUploadManager();

        //Return measured speed if possible, or user's speed otherwise.
        long speed = um.measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=SettingsManager.instance().getConnectionSpeed();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

		final int REPLY_LIMIT = 10;
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

			// see id there are any open slots
			boolean busy = um.isBusy();
            boolean uploaded = um.hadSuccesfulUpload();

			// see if we have ever accepted an incoming connection
			boolean incoming = RouterService.acceptedIncomingConnection();

			boolean chat = SettingsManager.instance().getChatEnabled();
			
			boolean mcast = queryRequest.isMulticast();
			// if it is a multicasted response, use the non-forced address.
			if ( mcast ) {
			    ip = RouterService.getNonForcedAddress();
			    port = RouterService.getNonForcedPort();
            }

            // create the new queryReply
            List qrList = createQueryReply(guid, ttl, port, ip, speed, 
                                           res, _clientGUID, !incoming, 
                                           busy, uploaded, measuredSpeed, 
                                           chat, mcast);

            if (qrList != null) 
                //add to the list
                queryReplies.addAll(qrList);

            // we only want to send multiple queryReplies
            // if the number of hops is small.
            if (numHops > 2)
                break;

        }//end of while
        
        return queryReplies.iterator();
    }
    
    /** If there is special processing needed to building a query reply,
     * subclasses can override this method as necessary.
     * @return A (possibly empty) List of query replies
     */
    protected List createQueryReply(byte[] guid, byte ttl, int port, 
                                    byte[] ip , long speed, Response[] res,
                                    byte[] clientGUID, boolean notIncoming,
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, boolean chat,
                                    boolean isFromMcast) {
        List list = new ArrayList();
        list.add(new QueryReply(guid, ttl, port, ip,
                                speed, res, _clientGUID, 
                                notIncoming, busy, uploaded, 
                                measuredSpeed, chat, isFromMcast));
        return list;
    }


    /**
     * Handles a query route table update message that originated from
     * receivingConnection.
     */
    private void handleRouteTableMessage(RouteTableMessage m,
										 ManagedConnection receivingConnection) {
        //System.out.println("MessageRouter::handleRouteTableMessage: "+receivingConnection); 
        //if not a supernode-client, ignore
        if(! receivingConnection.isSupernodeClientConnection() &&
		   ! receivingConnection.isUltrapeerQueryRoutingConnection())
            return;
                                            
        //Mutate query route table associated with receivingConnection.  
        //(This is legal.)  Create a new one if none exists.
        synchronized (receivingConnection.getQRPLock()) {
            ManagedConnectionQueryInfo qi =
                receivingConnection.getQueryRouteState();
            if (qi.lastReceived==null) {
                //TODO3: it's somewhat silly to allocate a new table and then
                //immediately replace its state with RESET.  Probably best to
                //have QueryRouteTable lazily allocate memory.
                qi.lastReceived=new QueryRouteTable();
            }
            try {
                qi.lastReceived.update(m);    
            } catch (BadPacketException e) {
                //TODO: ?
            }
        }
    }


    /** Thread the processing of QRP Table delivery. */
    private class QRPPropagator extends Thread {
        public QRPPropagator() {
            setName("QRPPropagator");
            setDaemon(true);
        }

        /** While the connection is not closed, sends all data delay. */
        public void run() {
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
		List list=_manager.getInitializedConnections2();
		QueryRouteTable table = null;
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

			ManagedConnectionQueryInfo qi=c.getQueryRouteState();
				
			//Create table to send on this connection...
			if (table == null) {
				table=createRouteTable();
			}                    

			//..and send each piece.
			//TODO2: use incremental and interleaved update
			
			//If writing is deflated, then do not allow the message to be
			//compressed.  This is because the message is going
			//to be compressed as a part of the outgoing stream, anyway.
			//Iterator iter=table.encode(qi.lastSent, !c.isWriteDeflated());
			
			// (We always want to allow deflation of the QRP tables. This
			//  is because we don't want to potentially clutter the stream's
			//  dictionary with rare data, and because we want better stats.)
			Iterator iter=table.encode(qi.lastSent, true);
			for (; iter.hasNext(); ) {  
				RouteTableMessage m=(RouteTableMessage)iter.next();
				c.send(m);
			}
			qi.lastSent=table;
		}
    }

    /**
     * Creates a query route table appropriate for forwarding to connection c.
     * This will not include information from c.
     *     @requires queryUpdateLock held
     */
    private static QueryRouteTable createRouteTable() {
        //TODO: choose size according to what's been propogated.
        QueryRouteTable ret=new QueryRouteTable();
        
        //Add my files...
        addQueryRoutingEntries(ret);
        return ret;
    }


    /**
     * Adds all query routing tables for this' files to qrt.
     *     @modifies qrt
     */
    private static void addQueryRoutingEntries(QueryRouteTable qrt) {
        Iterator words = _fileManager.getKeyWords().iterator();
        while(words.hasNext())
            qrt.add((String)words.next());
        // get 'indivisible' words and handle appropriately - you don't want the
        // qrt to divide these guys up....
        Iterator indivisibleWords = _fileManager.getIndivisibleKeyWords().iterator();
        while (indivisibleWords.hasNext()) 
            qrt.addIndivisible((String) indivisibleWords.next());
		if(RouterService.isSupernode()) {
			addQueryRoutingEntriesForLeaves(qrt);
		}
    }

	/**
	 * Adds all query routing tables of leaves to the query routing table for
	 * this node for propagation to other Ultrapeers at 1 hop.
	 *
	 * @param qrt the <tt>QueryRouteTable</tt> to add to
	 */
	private static void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {
		List leaves = _manager.getInitializedClientConnections2();
		
		for(int i=0; i<leaves.size(); i++) {
			ManagedConnection mc = (ManagedConnection)leaves.get(i);
        	synchronized (mc.getQRPLock()) {
				ManagedConnectionQueryInfo qi = mc.getQueryRouteState();
				if(qi.lastReceived != null) {
					qrt.addAll(qi.lastReceived);
				}
			}
		}
	}

    /** Simply couples a GUID with a timestamp.  Needed for expiration of
     *  QueryReplies waiting for out-of-band delivery.
     */
    private static class TimedGUID {
        public static final long MAX_LIFE = 25 * 1000; // 25 seconds
        private final GUID _guid;
        private final long _creationTime;

        public TimedGUID(GUID guid) {
            _guid = guid;
            _creationTime = System.currentTimeMillis();
        }

        /** @return true if other is a GUID that is the same as the GUID
         *  in this bundle.
         */
        public boolean equals(Object other) {
            if (other instanceof GUID)
                return _guid.equals(other);
            else if (other instanceof TimedGUID) 
                return _guid.equals(((TimedGUID) other)._guid);
            return false;
        }

        /** Since guids will be all we have when we do a lookup in a hashtable,
         *  we want the hash code to be the same as the GUID. 
         */
        public int hashCode() {
            return _guid.hashCode();
        }

        /** @return true if this bundle is greater than MAX_LIFE seconds old.
         */
        public boolean shouldExpire() {
            long currTime = System.currentTimeMillis();
            if (currTime - _creationTime >= MAX_LIFE)
                return true;
            return false;
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
                        TimedGUID currQB = (TimedGUID) keys.next();
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

}
