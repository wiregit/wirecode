package com.limegroup.gnutella;

import com.limegroup.gnutella.util.Utilities;
import com.limegroup.gnutella.security.User;

import com.sun.java.util.collections.*;
import java.io.IOException;

import com.limegroup.gnutella.routing.*;

/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
public abstract class MessageRouter
{
    protected HostCatcher _catcher;
    protected ConnectionManager _manager;
    protected Acceptor _acceptor;

    /**
     * @return the GUID we attach to QueryReplies to allow PushRequests in
     *         response.
     */
    protected byte[] _clientGUID;

    private ForMeReplyHandler _forMeReplyHandler = new ForMeReplyHandler();

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

    /** The maximum number of QueryReply bytes to route for any given query GUID. 
     *  Provides a primitive form of flow control by cutting off run-away replies. 
     *  Assuming 100 bytes per result, this allows 500 results.  */
    public static final int MAX_REPLY_ROUTE_BYTES=50000; //50k

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

    // NOTE: THESE VARIABLES ARE NOT SYNCHRONIZED...SO THE STATISTICS MAY NOT
    // BE 100% ACCURATE.

    /**
     * The total number of messages that pass through ManagedConnection.send()
     * and ManagedConnection.receive().
     */
    private volatile int _numMessages;
    /**
     * The number of PingRequests we actually respond to after filtering.
     * Note that excludes PingRequests we generate.
     */
    private volatile int _numPingRequests;
    /**
     * The number of PingReplies we actually process after filtering, either by
     * routing to another connection or updating our horizon statistics.
     * Note that excludes PingReplies we generate.  That number is basically
     * _numPingRequests (the number of PingRequests for which we generate a
     * PingReply)
     */
    private volatile int _numPingReplies;
    /**
     * The number of QueryRequests we actually respond to after filtering.
     * Note that excludes QueryRequests we generate.
     */
    private volatile int _numQueryRequests;
    /**
     * The number of QueryReplies we actually process after filtering, either by
     * routing to another connection or by querying the local store.
     * Note that excludes QueryReplies we generate.  That number is close to
     * _numQueryRequests (the number of QueryRequests for which we attempt to
     * generate a QueryReply)
     */
    private volatile int _numQueryReplies;
    /**
     * The number of PushRequests we actually process after filtering, either by
     * routing to another connection or by launching an HTTPUploader.
     * Note that excludes PushRequests we generate through Downloader.
     */
    private volatile int _numPushRequests;
    /**
     * The number of messages dropped by route filters
     */
    private volatile int _numFilteredMessages;
    /**
     * The number of replies dropped because they have no routing table entry
     */
    private volatile int _numRouteErrors;

	protected UploadManager _uploadManager;

    /**
     * Creates a MessageRouter.  Must call initialize before using.
     */
    protected MessageRouter()
    {
        try
        {
            _clientGUID = new GUID(GUID.fromHexString(
                SettingsManager.instance().getClientID())).bytes();
        }
        catch (IllegalArgumentException e)
        {
            //This should never happen! But if it does, we can recover.
            _clientGUID = Message.makeGuid();
        }
    }

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize(Acceptor acceptor, ConnectionManager manager,
                    HostCatcher catcher, UploadManager uploadManager)
    {
        _acceptor = acceptor;
        _manager = manager;
        _catcher = catcher;
        _uploadManager = uploadManager;
    }

    public String getPingRouteTableDump()
    {
        return _pingRouteTable.toString();
    }

    public String getQueryRouteTableDump()
    {
        return _queryRouteTable.toString();
    }

    public String getPushRouteTableDump()
    {
        return _pushRouteTable.toString();
    }

    /**
     * A callback for ConnectionManager to clear a ManagedConnection from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ManagedConnection connection)
    {
        _pingRouteTable.removeReplyHandler(connection);
        _queryRouteTable.removeReplyHandler(connection);
        _pushRouteTable.removeReplyHandler(connection);
    }

        /**
     * The handler for all message types.  Processes a message based on the 
     * message type.
     */
    public void handleMessage(Message m, ManagedConnection receivingConnection)
    {
        // Increment hops and decrease TTL.
        m.hop();

        if(m instanceof PingRequest) 
            handlePingRequestPossibleDuplicate((PingRequest)m, receivingConnection);
        else if (m instanceof PingReply) 
            handlePingReply((PingReply)m, receivingConnection);
        else if (m instanceof QueryRequest)
            handleQueryRequestPossibleDuplicate(
                (QueryRequest)m, receivingConnection);
        else if (m instanceof QueryReply)
            handleQueryReply((QueryReply)m, receivingConnection);
        else if (m instanceof PushRequest)
            handlePushRequest((PushRequest)m, receivingConnection);
        else if (m instanceof RouteTableMessage)
            handleRouteTableMessage((RouteTableMessage)m,
                                    receivingConnection);

        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
        forwardQueryRouteTables();      
    }
    
    
    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handlePingRequestPossibleDuplicate(
        PingRequest pingRequest, ManagedConnection receivingConnection)
    {
        if(_pingRouteTable.tryToRouteReply(pingRequest.getGUID(),
                                      receivingConnection))
            handlePingRequest(pingRequest, receivingConnection);
    }

    /**
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplicate(
        QueryRequest queryRequest, ManagedConnection receivingConnection)
    {
        if(_queryRouteTable.tryToRouteReply(queryRequest.getGUID(),
                                            receivingConnection))
            handleQueryRequest(queryRequest, receivingConnection);
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
                                     ManagedConnection receivingConnection)
    {
        _numPingRequests++;

        if(pingRequest.getTTL() > 0)
            broadcastPingRequest(pingRequest, receivingConnection,
                                 _manager);

        respondToPingRequest(pingRequest, _acceptor);
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
    protected void handleQueryRequest(QueryRequest queryRequest,
                                   ManagedConnection receivingConnection)
    {
        _numQueryRequests++;

        //Hack! If this is the indexing query from a Clip2 reflector, mark the
        //connection as unkillable so the ConnectionWatchdog will not police it
        //any more.
        if ((receivingConnection.getNumMessagesReceived()<=2)
                && (queryRequest.getHops()<=1)  //actually ==1 will do
                && (queryRequest.getQuery().equals(
                    FileManager.INDEXING_QUERY))) {
            receivingConnection.setKillable(false);
        }


        if(queryRequest.getTTL() > 0)
            broadcastQueryRequest(queryRequest, receivingConnection,
                                  _manager);

        respondToQueryRequest(queryRequest, _acceptor, _clientGUID);
    }

    /**
     * Sends the ping request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest pingRequest,
                                ManagedConnection connection)
    {
        _pingRouteTable.routeReply(pingRequest.getGUID(), _forMeReplyHandler);
        connection.send(pingRequest);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest queryRequest,
                                 ManagedConnection connection)
    {
        _queryRouteTable.routeReply(queryRequest.getGUID(), _forMeReplyHandler);
        connection.send(queryRequest);
    }

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastPingRequest(PingRequest pingRequest)
    {
        _pingRouteTable.routeReply(pingRequest.getGUID(), _forMeReplyHandler);
        broadcastPingRequest(pingRequest, null, _manager);
    }

    /**
     * Broadcasts the query request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastQueryRequest(QueryRequest queryRequest)
    {
        _queryRouteTable.routeReply(queryRequest.getGUID(), _forMeReplyHandler);
        broadcastQueryRequest(queryRequest, null, _manager);
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
    protected void broadcastPingRequest(PingRequest pingRequest,
                                        ManagedConnection receivingConnection,
                                        ConnectionManager manager)
    {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.

        //Broadcast the ping to other connected nodes (supernodes or older
        //nodes), but DON'T forward any ping not originating from me (i.e.,
        //receivingConnection!=null) along leaf to ultrapeer connections.
        List list=manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if (   receivingConnection==null   //came from me
                || (c!=receivingConnection
                     && !c.isClientSupernodeConnection())) {
                c.send(pingRequest);
            }
        }
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
                                        ManagedConnection receivingConnection,
                                        ConnectionManager manager)
    {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.
        
        //Broadcast the query to other connected nodes (supernodes or older
        //nodes), but DON'T forward any queries not originating from me (i.e.,
        //receivingConnection!=null) along leaf to ultrapeer connections.
        List list=_manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++){
            ManagedConnection c = (ManagedConnection)list.get(i);
            if (   receivingConnection==null   //came from me
                || (c!=receivingConnection
                     && !c.isClientSupernodeConnection())) {
                sendQueryRequest(queryRequest, c, receivingConnection);
            }
        }
        
        //use query routing to route queries to client connections
        //send queries only to the clients from whom query routing 
        //table has been received
        list=_manager.getInitializedClientConnections2();
        for(int i=0; i<list.size(); i++){
            ManagedConnection c = (ManagedConnection)list.get(i);
            if(c != receivingConnection) {
                //TODO:
                //because of some very obscure optimization rules, it's actually
                //possible that qi could be non-null but not initialized.  Need
                //to be more careful about locking here.
                ManagedConnectionQueryInfo qi=c.getQueryRouteState();
                if (qi==null || qi.lastReceived==null) 
                    return;
                else if (qi.lastReceived.contains(queryRequest))
                {
                    //A new client with routing entry, or one that hasn't started
                    //sending the patch.
                    sendQueryRequest(queryRequest, c, receivingConnection);
                }
            }
        }
    }
    
    /**
     * Sends the passed query request, received on receivingConnection, 
     * to the passed sendConnection, only if the receivingConnection and
     * the sendConnection are authenticated to a common domain
     * @param queryRequest Query Request to send
     * @param sendConnection The connection on which to send out the query
     * @param receivingConnection The connection on which we originally
     * received the query
     */
    protected void sendQueryRequest(QueryRequest queryRequest, ManagedConnection
        sendConnection, ManagedConnection receivingConnection)
    {
        //send the query over this connection only if any of the following
        //is true:
        //1. The query originated from our node (receiving connection 
        //is null)
        //2. The connection under  consideration is an unauthenticated 
        //connection (normal gnutella connection)
        //3. It is an authenticated connection, and the connection on 
        //which we received query and this connection, are both 
        //authenticated to a common domain
        if((receivingConnection == null)
            || containsDefaultUnauthenticatedDomainOnly(
                sendConnection.getDomains())
            || Utilities.hasIntersection(
            receivingConnection.getDomains(), sendConnection.getDomains()))
            sendConnection.send(queryRequest);
    }
    

    /**
     * Checks if the passed set of domains contains only
     * default unauthenticated domain 
     * @param domains Set (of String) of domains to be tested
     * @return true if the passed set of domains contains only
     * default unauthenticated domain, false otherwise
     */
    private static boolean containsDefaultUnauthenticatedDomainOnly(Set domains)
    {
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
    protected abstract void respondToPingRequest(PingRequest pingRequest,
                                                 Acceptor acceptor);


    /**
     * Respond to the query request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is called from the default handleQueryRequest.
     */
    protected abstract void respondToQueryRequest(QueryRequest queryRequest,
                                                  Acceptor acceptor,
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
    protected void handlePingReply(PingReply pingReply,
                                   ManagedConnection receivingConnection)
    {
        //update hostcatcher (even if the reply isn't for me)
        boolean newAddress=_catcher.add(pingReply, receivingConnection);

        //First route to originator in usual manner.
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(pingReply.getGUID());

        if(replyHandler != null)
        {
            _numPingReplies++;
            replyHandler.handlePingReply(pingReply,
                                         receivingConnection);
        }
        else
        {
            _numRouteErrors++;
            receivingConnection.countDroppedMessage();
        }

        //Then, if a marked pong from a supernode that we've never seen before,
        //send to all leaf connections except replyHandler (which may be null),
        //irregardless of GUID.  The leafs will add the address then drop the
        //pong as they have no routing entry.  Note that if supernodes are very
        //prevalent, this may consume too much bandwidth.
        if (newAddress && pingReply.isMarked()) 
        {
            List list=_manager.getInitializedClientConnections2();
            for (int i=0; i<list.size(); i++) 
            {
                ManagedConnection c = (ManagedConnection)list.get(i);
                if (c!=receivingConnection && c!=replyHandler)
                    c.send(pingReply);        
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
                                 ManagedConnection receivingConnection)
    {
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength());

        if(rrp != null)
        {
            queryReply.setPriority(rrp.getBytesRouted());
            _numQueryReplies++;
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       receivingConnection);
            //Simple flow control: don't route this message along other
            //connections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.
            if (rrp.getBytesRouted()<MAX_REPLY_ROUTE_BYTES ||
                    rrp.getReplyHandler()==_forMeReplyHandler) {
                rrp.getReplyHandler().handleQueryReply(queryReply,
                                                       receivingConnection);
            }
        }
        else
        {
            _numRouteErrors++;
            receivingConnection.countDroppedMessage();
        }
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
    public void handlePushRequest(PushRequest pushRequest,
                                  ManagedConnection receivingConnection)
    {
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushRouteTable.getReplyHandler(pushRequest.getClientGUID());

        if(replyHandler != null)
        {
            _numPingReplies++;
            replyHandler.handlePushRequest(pushRequest, receivingConnection);
        }
        else
        {
            _numRouteErrors++;
            receivingConnection.countDroppedMessage();
        }
    }

    /**
     * Uses the ping route table to send a PingReply to the appropriate
     * connection.  Since this is used for PingReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPingReply(PingReply pingReply)
        throws IOException
    {
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
        throws IOException
    {
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength());

        if(rrp != null)
        {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       _forMeReplyHandler);
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
        throws IOException
    {
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushRouteTable.getReplyHandler(pushRequest.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(pushRequest, null);
        else
            throw new IOException();
    }

    /**
     * Allow the controlled creation of a GroupPingRequest
     */
     public abstract GroupPingRequest createGroupPingRequest(String group);
     
    /**
     * Handles a query route table update message that originated from
     * receivingConnection.
     */
    public void handleRouteTableMessage(RouteTableMessage m,
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
                qi.lastReceived=new QueryRouteTable(
                    QueryRouteTable.DEFAULT_TABLE_SIZE,
                    QueryRouteTable.DEFAULT_INFINITY);
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
                    //System.out.println("    Sending "+m.toString()+" to "+c);
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
    private QueryRouteTable createRouteTable(ManagedConnection c) {
        //TODO: choose size according to what's been propogated.
        QueryRouteTable ret=new QueryRouteTable(
            QueryRouteTable.DEFAULT_TABLE_SIZE,
            (byte)QueryRouteTable.DEFAULT_INFINITY);
        
        //Add my files...
        addQueryRoutingEntries(ret);

//        //...and those of all neighbors except c.  Use higher TTLs on these.
//        List list=_manager.getInitializedConnections();
//        for(int i=0; i<list.size(); i++) { 
//            ManagedConnection c2=(ManagedConnection)list.get(i);
//            if (c2==c)
//                continue;
//            ManagedConnectionQueryInfo qi=c2.getQueryRouteState();
//            if (qi!=null && qi.lastReceived!=null)
//                ret.addAll(qi.lastReceived);
//        }
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
        int port = _acceptor.getPort();
        byte[] ip = _acceptor.getAddress();

        //Return measured speed if possible, or user's speed otherwise.
        long speed = _uploadManager.measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=SettingsManager.instance().getConnectionSpeed();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        while (numResponses > 0) {
            int arraySize;
            // if there are more than 255 responses,
            // create an array of 255 to send in the queryReply
            // otherwise, create an array of whatever size is left.
            if (numResponses < 255) {
                // break;
                arraySize = numResponses;
            }
            else
                arraySize = 255;

            Response[] res;
            // a special case.  in the common case where there
            // are less than 256 responses being sent, there
            // is no need to copy one array into another.
            if ( (index == 0) && (arraySize < 255) ) {
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
			boolean busy = _uploadManager.isBusy();
            boolean uploaded = _uploadManager.hadSuccesfulUpload();

			// see if we have ever accepted an incoming connection
			boolean incoming = _acceptor.acceptedIncoming();

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
    
    /**
     * Handle a reply to a PingRequest that originated here.
     * Implementations typically process that various statistics in the reply.
     * This method is called from the default handlePingReply.
     */
    protected abstract void handlePingReplyForMe(
        PingReply pingReply,
        ManagedConnection receivingConnection);

    /**
     * Handle a reply to a QueryRequest that originated here.
     * Implementations typically display or record the results of the query.
     * This method is called from the default handleQueryReply.
     */
    protected abstract void handleQueryReplyForMe(
        QueryReply queryReply,
        ManagedConnection receivingConnection);

    /**
     * Handle a PushRequest reply to a QueryReply that originated here.
     * Implementations typically display or record the results of the query.
     * This method is called from the default handlePushRequest
     */
    protected abstract void handlePushRequestForMe(
        PushRequest pushRequest,
        ManagedConnection receivingConnection);




    //
    // Begin Statistics Accessors
    //

    /**
     * The overall number of messages should be maintained in the send and
     * receive calls on ManagedConnection.
     */
    public void countMessage()
    {
        _numMessages++;
    }

    /**
     * This method should only be called from the ManagedConnection message
     * dropping calls.  The ManagedConnection that received the doomed message
     * should count it.
     * See that class for instructions on counting dropped messages.
     */
    public void countFilteredMessage()
    {
        _numFilteredMessages++;
    }

    /**
     * @return the number of PingRequests we actually respond to after
     * filtering.
     * Note that excludes PingRequests we generate.
     */
    public int getNumPingRequests()
    {
        return _numPingRequests;
    }

    /**
     * @return the number of PingReplies we actually process after filtering,
     * either by routing to another connection or updating our horizon
     * statistics.
     * Note that excludes PingReplies we generate.  That number is basically
     * _numPingRequests (the number of PingRequests for which we generate a
     * PingReply)
     */
    public int getNumPingReplies()
    {
        return _numPingReplies;
    }

    /**
     * @return The number of QueryRequests we actually respond to after
     * filtering.
     * Note that excludes QueryRequests we generate.
     */
    public int getNumQueryRequests()
    {
        return _numQueryRequests;
    }

    /**
     * @return the number of QueryReplies we actually process after filtering,
     * either by routing to another connection or by querying the local store.
     * Note that excludes QueryReplies we generate.  That number is close to
     * _numQueryRequests (the number of QueryRequests for which we attempt to
     * generate a QueryReply)
     */
    public int getNumQueryReplies()
    {
        return _numQueryReplies;
    }

    /**
     * @return the number of PushRequests we actually process after filtering,
     * either by routing to another connection or by launching an HTTPUploader.
     * Note that excludes PushRequests we generate through Downloader.
     */
    public int getNumPushRequests()
    {
        return _numPushRequests;
    }

    /**
     * @return the number of messages dropped by routing filters
     */
    public int getNumFilteredMessages()
    {
        return _numFilteredMessages;
    }

    /**
     * @return the number of replies dropped because they have no routing table
     * entry
     */
    public int getNumRouteErrors()
    {
        return _numRouteErrors;
    }

    /**
     * A convenience method for getNumFilteredMessages() + getNumRouteErrors()
     *
     * @return The total number of messages dropped for filtering or routing
     *         reasons
     */
    public int getNumDroppedMessages()
    {
        return _numFilteredMessages + _numRouteErrors;
    }

    /**
     * @return the total number of messages sent or received.  This includes
     * dropped messages, whatever.  If we attempt to send a message or
     * if we receive a well-formed message on a socket, we count it.
     */
    public int getNumMessages()
    {
        return _numMessages;
    }
    
    /**
     * Returns the address used for downloads and messaging connections.
     * Used to fill out the My-Address header in ManagedConnection.
     * @see Acceptor#getAddress
     */
    public byte[] getAddress() {
        return _acceptor.getAddress();
    }

    /**
     * Returns the port used for downloads and messaging connections.
     * Used to fill out the My-Address header in ManagedConnection.
     * @see Acceptor#getPort
     */    
    public int getPort() {
        return _acceptor.getPort();
    }

    //
    // End Statistics Accessors
    //

    /**
     * This is the class that goes in the route table when a request is
     * sent whose reply is for me.
     */
    private final class ForMeReplyHandler
        implements ReplyHandler
    {
        public ForMeReplyHandler() {}

        public void handlePingReply(PingReply pingReply,
                                    ManagedConnection receivingConnection)
        {
            handlePingReplyForMe(pingReply, receivingConnection);
        }

        public void handleQueryReply(QueryReply queryReply,
                                     ManagedConnection receivingConnection)
        {
            handleQueryReplyForMe(queryReply, receivingConnection);
        }

        public void handlePushRequest(PushRequest pushRequest,
                                      ManagedConnection receivingConnection)
        {
            handlePushRequestForMe(pushRequest, receivingConnection);
        }

        public boolean isOpen()
        {
            //I'm always ready to handle replies.
            return true;
        }
    }
}
