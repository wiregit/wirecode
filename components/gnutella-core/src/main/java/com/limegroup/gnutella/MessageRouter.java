package com.limegroup.gnutella;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.statistics.*;

import com.sun.java.util.collections.*;
import java.io.IOException;
import java.net.*;


/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
public abstract class MessageRouter {

    protected ConnectionManager _manager;

    /**
     * @return the GUID we attach to QueryReplies to allow PushRequests in
     *         response.
     */
    protected byte[] _clientGUID;

	/**
	 * Reference to the <tt>ReplyHandler</tt> for messages intended for 
	 * this node.
	 */
    private final ReplyHandler FOR_ME_REPLY_HANDLER = new ForMeReplyHandler();

    /**
     * The lock to hold before updating or propogating tables.  TODO3: it's 
     * probably possible to use finer-grained locking.
     */
    private Object queryUpdateLock=new Object();
    /**
     * The time when we should next broadcast route table updates.
     * (Route tables are stored per connection in ManagedConnectionQueryInfo.)
     * LOCKING: obtain queryUpdateLock
     */
    private long nextQueryUpdateTime=0l;
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


	/**
	 * Constant handle to the <tt>QueryUnicaster</tt> since it is called
	 * upon very frequently.
	 */
	protected final QueryUnicaster UNICASTER = QueryUnicaster.instance();


	/**
	 * Constant for whether or not to record stats.
	 */
	private final boolean RECORD_STATS = !CommonUtils.isJava118();

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
    }

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize() {
        _manager = RouterService.getConnectionManager();
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
     * A callback for ConnectionManager to clear a ManagedConnection from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ManagedConnection connection) {
        _pingRouteTable.removeReplyHandler(connection);
        _queryRouteTable.removeReplyHandler(connection);
        _pushRouteTable.removeReplyHandler(connection);
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param m the <tt>Message</tt> instance to route appropriately
	 * @param receivingConnection the <tt>ManagedConnection</tt> over which
	 *  the message was received
     */
    public void handleMessage(Message msg, ManagedConnection receivingConnection) {
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

        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
        forwardQueryRouteTables();      
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
		UDPReplyHandler handler = new UDPReplyHandler(address, port);
		
        if (msg instanceof QueryRequest) {
			sendAcknowledgement(datagram, msg.getGUID());
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(msg);
			// a TTL above zero may indicate a malicious client, as UDP
			// messages queries should not be sent with TTL above 1.
			//if(msg.getTTL() > 0) return;
            handleUDPQueryRequestPossibleDuplicate((QueryRequest)msg, 
												   handler);
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
		}
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
				reply = new PingReply(guid, (byte)1,
									  host.getPort(),
									  host.getHostBytes(),
									  (long)0, (long)0, true);
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
			return new PingReply(guid, (byte)1,
								 RouterService.getPort(),
								 RouterService.getAddress(),
								 RouterService.getNumSharedFiles(),
								 RouterService.getSharedFileSize()/1024,
								 RouterService.isSupernode(),
								 Statistics.instance().calculateDailyUptime());		
		} else {
			return new PingReply(guid, (byte)1,
								 endpoint.getPort(),
								 endpoint.getAddress().getAddress(),
								 0, 0, true, 0);
		}
	}



	
    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handlePingRequestPossibleDuplicate(
        PingRequest pingRequest, ReplyHandler handler) {
        if(_pingRouteTable.tryToRouteReply(pingRequest.getGUID(),
										   handler))
            handlePingRequest(pingRequest, handler);
    }

    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handleUDPPingRequestPossibleDuplicate(													 
        PingRequest pingRequest, ReplyHandler handler, DatagramPacket datagram) {
        if(_pingRouteTable.tryToRouteReply(pingRequest.getGUID(), handler))
            handleUDPPingRequest(pingRequest, handler, datagram);
    }

    /**
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplicate(
        QueryRequest request, ManagedConnection receivingConnection) {
        if(_queryRouteTable.tryToRouteReply(request.getGUID(),
                                            receivingConnection)) {
			//Hack! If this is the indexing query from a Clip2 reflector, mark the
			//connection as unkillable so the ConnectionWatchdog will not police it
			//any more.
			if ((receivingConnection.getNumMessagesReceived()<=2)
                && (request.getHops()<=1)  //actually ==1 will do
                && (request.getQuery().equals(FileManager.INDEXING_QUERY))) {
				receivingConnection.setKillable(false);
			}
            handleQueryRequest(request, receivingConnection);
		} else {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.TCP_DUPLICATE_QUERIES.addMessage(request);
		}
    }
	
	/**
	 * Special handler for UDP queries.
	 *
	 * @param query the UDP <tt>QueryRequest</tt> 
	 * @param handler the <tt>ReplyHandler</tt> that will handle the reply
	 */
	final void handleUDPQueryRequestPossibleDuplicate(QueryRequest request,
													  ReplyHandler handler)  {
        if(_queryRouteTable.tryToRouteReply(request.getGUID(), handler)) {
            handleQueryRequest(request, handler);
		} else {
			if(RECORD_STATS)
				ReceivedMessageStatHandler.UDP_DUPLICATE_QUERIES.addMessage(request);
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
        respondToUDPPingRequest(pingRequest, datagram);
    }
    

    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {

        // also add the sender of the pong if different from the host
        // described in the reply...
        if((reply.getPort() != port) || 
           (!reply.getIP().equals(address.getHostAddress()))) {
            UNICASTER.addUnicastEndpoint(address, port);
		}

		// TODO: are we sure we want to do this?
        // notify neighbors of new unicast endpoint...
        Iterator guessUltrapeers = 
			_manager.getConnectedGUESSUltrapeers().iterator();
        while (guessUltrapeers.hasNext()) {
            ManagedConnection currMC = 
				(ManagedConnection) guessUltrapeers.next();
			currMC.handlePingReply(reply, handler);
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
     */
    protected void handleQueryRequest(QueryRequest request,
									  ReplyHandler handler) {
		// if it's a request from a leaf and we GUESS, send it out via GUESS --
		// otherwise, broadcast it if it still has TTL
		//if(handler.isSupernodeClientConnection() && 
		// RouterService.isGUESSCapable()) 
		//unicastQueryRequest(request, handler);
        //else if(request.getTTL() > 0) {

		if(request.getTTL() > 0) {
			// send the request to intra-Ultrapeer connections -- this does
			// not send the request to leaves
			broadcastQueryRequest(request, handler);
		}
			
		// always forward any queries to leaves -- this only does
		// anything when this node's an Ultrapeer
		forwardQueryRequestToLeaves(request, handler);
        // if I'm not firewalled and the source isn't firewalled THEN reply....
        if (request.isFirewalledSource() &&
            !RouterService.acceptedIncomingConnection())
            return;
        respondToQueryRequest(request, _clientGUID);
    }

    /**
     * Sends the ping request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest request,
                                ManagedConnection connection) {
        _pingRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest request,
                                 ManagedConnection connection) {
        _queryRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastPingRequest(PingRequest pingRequest) {
        _pingRouteTable.routeReply(pingRequest.getGUID(), FOR_ME_REPLY_HANDLER);
        broadcastPingRequest(pingRequest, null, _manager);
    }

    /**
     * Broadcasts the query request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastQueryRequest(QueryRequest request) {
        _queryRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        //if (RouterService.isGUESSCapable()) {
		//unicastQueryRequest(request, null);
		//} else {
		broadcastQueryRequest(request, null);
		//}
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
        //nodes), but DON'T forward any ping not originating from me (i.e.,
        //receivingConnection!=null) along leaf to ultrapeer connections.
        List list=manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++) {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if (   receivingConnection==null   //came from me
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
            ManagedConnection c = (ManagedConnection)list.get(i);
            if(c != handler) {
                //TODO:
                //because of some very obscure optimization rules, it's actually
                //possible that qi could be non-null but not initialized.  Need
                //to be more careful about locking here.
                ManagedConnectionQueryInfo qi = c.getQueryRouteState();
                if (qi==null || qi.lastReceived==null) 
                    return;
                else if (qi.lastReceived.contains(request)) {
                    //A new client with routing entry, or one that hasn't started
                    //sending the patch.
                    sendQueryRequest(request, c, handler);
                }
            }
        }
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
     * Broadcasts the query request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated QueryReplyHandler.  This is called from the default
     * handleQueryRequest and the default broadcastQueryRequest(QueryRequest)
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    protected void broadcastQueryRequest(QueryRequest queryRequest,
										 ReplyHandler handler) {
		// Note the use of initializedConnections only.
		// Note that we have zero allocations here.
		
		//Broadcast the query to other connected nodes (supernodes or older
		//nodes), but DON'T forward any queries not originating from me (i.e.,
		//handler!=null) along leaf to ultrapeer connections.
		List list=_manager.getInitializedConnections2();
		for(int i=0; i<list.size(); i++){
			ManagedConnection c = (ManagedConnection)list.get(i);
			if (   handler==null   //came from me
				   || (c!=handler
					   && !c.isClientSupernodeConnection())) {
				sendQueryRequest(queryRequest, c, handler);
			}
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
    protected void sendQueryRequest(QueryRequest request, 
									ManagedConnection sendConnection, 
									ReplyHandler handler) {
        //send the query over this connection only if any of the following
        //is true:
        //1. The query originated from our node (receiving connection 
        //is null)
        //2. The connection under  consideration is an unauthenticated 
        //connection (normal gnutella connection)
        //3. It is an authenticated connection, and the connection on 
        //which we received query and this connection, are both 
        //authenticated to a common domain
        if((handler == null ||
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
     * the PingReply count is incremented.
     *
     * In all cases, the ping reply is recorded into the host catcher.
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

		boolean supportsUnicast = false;
		try {
			supportsUnicast = reply.supportsUnicast();
		} catch(BadPacketException e) {
		}

        //Then, if a marked pong from an Ultrapeer that we've never seen before,
        //send to all leaf connections except replyHandler (which may be null),
        //irregardless of GUID.  The leafs will add the address then drop the
        //pong as they have no routing entry.  Note that if Ultrapeers are very
        //prevalent, this may consume too much bandwidth.
		//Also forward any GUESS pongs to all leaves.
        if ((newAddress && reply.isMarked()) || supportsUnicast) {
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
     * the QueryReply count is incremented.
     *
     * Override as desired, but you probably want to call super.handleQueryReply
     * if you do.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler handler) {
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength());

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

			// TODO: What happens if we get a TTL=0 query that's not intended
			// for us?  At first glance, it looks like we keep forwarding it!
            if(!shouldDropReply(rrp.getBytesRouted(), queryReply.getTTL()) ||
			   rrp.getReplyHandler()==FOR_ME_REPLY_HANDLER) {
                rrp.getReplyHandler().handleQueryReply(queryReply,
                                                       handler);
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
        // send replies with ttl above 3 if we've routed under 50K 
        if(ttl > 3 && bytesRouted < 50    * 1024) return false;
        // send replies with ttl 0 if we've routed under 50K, as this 
		// shouldn't happen 
        if(ttl == 0 && bytesRouted < 50   * 1024) return false;
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && bytesRouted < 1000 * 1024) return false;
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && bytesRouted < 333  * 1024) return false;
        // send replies with ttl 3 if we've routed under 111K 
        if(ttl == 3 && bytesRouted < 111  * 1024) return false;

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
    public void handlePushRequest(PushRequest request,
                                  ReplyHandler handler) {
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
    public void sendPingReply(PingReply pingReply)
        throws IOException {
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(pingReply.getGUID());

        if(replyHandler != null)
            replyHandler.handlePingReply(pingReply, null);
        else
            throw new IOException();
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendQueryReply(QueryReply queryReply)
        throws IOException {
 
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength());

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
            throw new IOException();
    }

    /**
     * Uses the push route table to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPushRequest(PushRequest pushRequest)
        throws IOException {
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushRouteTable.getReplyHandler(pushRequest.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(pushRequest, null);
        else
            throw new IOException();
    }
     
    /**
     * Handles a query route table update message that originated from
     * receivingConnection.
     */
    private void handleRouteTableMessage(RouteTableMessage m,
										 ManagedConnection receivingConnection) {
        //if not a supernode-client, ignore
        if(! receivingConnection.isSupernodeClientConnection())
            return;
                                            
        //Mutate query route table associated with receivingConnection.  
        //(This is legal.)  Create a new one if none exists.
        synchronized (queryUpdateLock) {
            ManagedConnectionQueryInfo qi=receivingConnection.getQueryRouteState();
            if (qi==null) {
                //There's really no need to check if c is an old client here;
                //it certainly didn't send a RouteTableMessage by accident!
                qi=new ManagedConnectionQueryInfo();
                receivingConnection.setQueryRouteState(qi);
            }
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

    /**
     * Sends route updated query routing tables to all connections which haven't
     * been updated in a while.  You can call this method as often as you want;
     * it takes care of throttling.
     *     @modifies connections
     */    
    public void forwardQueryRouteTables() {
        //As a tiny optimization, we skip the propogate if we have not in
        //shielded mode.
        if (! _manager.hasClientSupernodeConnection())
            return;

        synchronized (queryUpdateLock) {
            //For all connections to new hosts c needing an update...
            //TODO3: use getInitializedConnections2?
            List list=_manager.getInitializedConnections();
            for(int i=0; i<list.size(); i++) {                        
                ManagedConnection c=(ManagedConnection)list.get(i);
                
                //Not every connection need be a leaf/supernode connection.
                if (! (c.isClientSupernodeConnection() 
					   && c.isQueryRoutingEnabled())) 
                    continue;
                
                //Check the time to decide if it needs an update.
                long time=System.currentTimeMillis();
                if (time<c.getNextQRPForwardTime())
                    continue;
                c.setNextQRPForwardTime(time+QUERY_ROUTE_UPDATE_TIME);

                ManagedConnectionQueryInfo qi=c.getQueryRouteState();
                if (qi==null) {
                    qi=new ManagedConnectionQueryInfo();
                    c.setQueryRouteState(qi);
                }
                    
                //Create table to send on this connection...
                QueryRouteTable table=createRouteTable(c);

                //..and send each piece.
                //TODO2: use incremental and interleaved update
                for (Iterator iter=table.encode(qi.lastSent); iter.hasNext(); ) {  
                    RouteTableMessage m=(RouteTableMessage)iter.next();
                    c.send(m);
                }
                qi.lastSent=table;
            }
        }
    }

    /**
     * Creates a query route table appropriate for forwarding to connection c.
     * This will not include information from c.
     *     @requires queryUpdateLock held
     */
    private QueryRouteTable createRouteTable(ReplyHandler c) {
        //TODO: choose size according to what's been propogated.
        QueryRouteTable ret=new QueryRouteTable();
        
        //Add my files...
        addQueryRoutingEntries(ret);
        return ret;
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

            // create the new queryReply
            List qrList = createQueryReply(guid, ttl, port, ip, speed, 
                                           res, _clientGUID, !incoming, 
                                           busy, uploaded, measuredSpeed, 
                                           chat);

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
                                    boolean measuredSpeed, boolean chat) {
        List list = new ArrayList();
        list.add(new QueryReply(guid, ttl, port, ip,
                                speed, res, _clientGUID, 
                                notIncoming, busy, uploaded, 
                                measuredSpeed, chat));
        return list;
    }
                                    

    /**
     * Adds all query routing tables for this' files to qrt.
     *     @modifies qrt
     */
    protected abstract void addQueryRoutingEntries(QueryRouteTable qrt);
}
