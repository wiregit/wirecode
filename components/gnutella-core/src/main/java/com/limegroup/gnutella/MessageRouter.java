package com.limegroup.gnutella;

import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;
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
    private byte[] _clientGUID;

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
    private long QUERY_ROUTE_UPDATE_TIME=1000*60; //60 seconds
    
    /**
     * Maps PingRequest GUIDs to PingReplyHandlers
     */
    private RouteTable _pingRouteTable = new RouteTable(2048);
    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers
     */
    private RouteTable _queryRouteTable = new RouteTable(2048);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers
     * Because client GUID's can be re-mapped to different connections, we
     * must force RouteTable to used FixedsizeForgetfulHashMap instead of
     * the lighter ForgetfulHashMap.
     */
    private RouteTable _pushRouteTable = new RouteTable(2048, true);

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
//        //if crawler ping, send back pongs of neighbors.
//        if (isCrawlerPing(m)) 
//        {
//            sendCrawlerPingReplies((PingRequest)m, receivingConnection);
//            return;
//        }

        // Increment hops and decrease TTL
        m.hop();

        if(m instanceof PingRequest) 
            handlePingRequest((PingRequest)m, receivingConnection);
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
        List list=manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if(c != receivingConnection)
                c.send(pingRequest);
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
        List list=_manager.getInitializedConnections2();
        int newClients = 0; //number of new client connections
        int routedQueries = 0; //number of queries that were routed to new clients
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if(c != receivingConnection) {
                //Send query along any connection to an old cl0ient, or to a new
                //client with routing information for the given keyword.  TODO:
                //because of some very obscure optimization rules, it's actually
                //possible that qi could be non-null but not initialized.  Need
                //to be more careful about locking here.
                ManagedConnectionQueryInfo qi=c.getQueryRouteState();
                if (qi==null || qi.lastReceived==null) 
                    //Either an old client, or a new client that's not yet
                    //sent us a table message.
                    c.send(queryRequest);
                else if (qi.lastReceived.contains(queryRequest))
                {
                    routedQueries++;
                    //A new client with routing entry, or one that hasn't started
                    //sending the patch.
                    c.send(queryRequest);      
                }
            }
        }
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
        _catcher.spy(pingReply, receivingConnection);

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
        ReplyHandler replyHandler =
            _queryRouteTable.getReplyHandler(queryReply.getGUID());

        if(replyHandler != null)
        {
            _numQueryReplies++;
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       receivingConnection);
            replyHandler.handleQueryReply(queryReply,
                                          receivingConnection);
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
        ReplyHandler replyHandler =
            _queryRouteTable.getReplyHandler(queryReply.getGUID());

        if(replyHandler != null)
        {
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       _forMeReplyHandler);
            replyHandler.handleQueryReply(queryReply, null);
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
    public GroupPingRequest createGroupPingRequest(String group)
    {
        FileManager fm = FileManager.instance();
        int num_files = fm.getNumFiles();
        int kilobytes = fm.getSize()/1024;

        GroupPingRequest pingRequest =
          new GroupPingRequest(SettingsManager.instance().getTTL(),
            _acceptor.getPort(), _acceptor.getAddress(),
            num_files, kilobytes, group);
        return( pingRequest );
    }

        /**
     * Handles a query route table update message that originated from
     * receivingConnection.
     */
    public void handleRouteTableMessage(RouteTableMessage m,
                                        ManagedConnection receivingConnection) {
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
        synchronized (queryUpdateLock) {
            //Check time.  Skip or update.
            long time=System.currentTimeMillis();
            if (time<nextQueryUpdateTime) 
                return;
            nextQueryUpdateTime=time+QUERY_ROUTE_UPDATE_TIME;

            //For all connections to new hosts c needing an update...
            //TODO3: use getInitializedConnections2?
            List list=_manager.getInitializedConnections();
            for(int i=0; i<list.size(); i++) {                        
                ManagedConnection c=(ManagedConnection)list.get(i);
                
                //check if we do need to forward the query route tables
                //Presently, it will be sent only by a shielded-client to
                //its supernode
                if(SettingsManager.instance().isSupernode() || 
                    !c.isSupernodeConnection()){
                    return;
                }
                
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
