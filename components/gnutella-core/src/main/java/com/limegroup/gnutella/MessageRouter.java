package com.limegroup.gnutella;

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


    ///////////////////////////// Core Routing State ///////////////////////    
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
    private long QUERY_ROUTE_UPDATE_TIME=1000*15;  //15 seconds for testing 

    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers
     */
    private RouteTable _querySourceTable = new RouteTable(2048);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers
     * Because client GUID's can be re-mapped to different connections, we
     * must force RouteTable to used FixedsizeForgetfulHashMap instead of
     * the lighter ForgetfulHashMap.
     */
    private RouteTable _pushSourceTable = new RouteTable(2048, true);


    ///////////////////////////// Statistics ////////////////////////////////
    // NOTE: THESE VARIABLES ARE NOT SYNCHRONIZED...SO THE STATISTICS MAY NOT
    // BE 100% ACCURATE.

    /**
     * The total number of messages that pass through ManagedConnection.send()
     * and ManagedConnection.receive().
     */
    private volatile int _numMessages;
    /**
     * The number of PingReplies we actually process after filtering, either by
     * routing to another connection or updating our horizon statistics.
     * Note that excludes PingReplies we generate.  That number is basically
     * _numRecvdPingRequests (the number of PingRequests for which we generate a
     * PingReply)
     */
    private volatile int _numPingReplies;
    /**
     * The number of PingRequests that we received and processed.  This is not
     * the total number of Ping Requests received, as ping requests that are 
     * handshakes (i.e., ttl of 1), crawler ping requests, and ping requests from
     * old clients (within a certain time frame) are received, but are not 
     * counted as processed ping requests, even though crawler ping request are
     * processed, but are handled in a different manner.
     */
    private volatile int _numProcessedPingRequests;
    /**
     * The number of PingRequests that we broadcast to all the neighbors.  This
     * count is only incremented when the pong cache is expired, and we need to
     * refill it by broadcasting a ping to all the neighbors.
     */
    private volatile int _numBroadcastPingRequests;
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
     * Max TTL used when sending PingRequests for refreshing the HostCatcher.
     * Also used to store how many Pongs (PingReplies) we need to send to
     * another client in response to a Ping.
     */
    public static final int MAX_TTL_FOR_CACHE_REFRESH = 7;

    /**
     * The max number of pongs to return in response to a ping.
     */
    public static final int MAX_PONGS_TO_RETURN = 10;

    /**
     * Counter used to determine whether or not to broadcast ping request (i.e.,
     * when refreshing the cache) to old clients or not.
     */
    private int _pingBroadcastCount = 0;

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

    public String getQueryRouteTableDump()
    {
        return _querySourceTable.toString();
    }

    public String getPushRouteTableDump()
    {
        return _pushSourceTable.toString();
    }


    /**
     * A callback for ConnectionManager to clear a ManagedConnection from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ManagedConnection connection)
    {
        _querySourceTable.removeReplyHandler(connection);
        _pushSourceTable.removeReplyHandler(connection);
    }

    /**
     * The handler for all message types.  Processes a message based on the 
     * message type.
     */
    public void handleMessage(Message m, ManagedConnection receivingConnection)
    {
        //if crawler ping, send back pongs of neighbors.
        if (isCrawlerPing(m)) 
        {
            sendCrawlerPingReplies((PingRequest)m, receivingConnection);
            return;
        }

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
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     */
    private final void handleQueryRequestPossibleDuplicate(
        QueryRequest queryRequest, ManagedConnection receivingConnection)
    {
        if(_querySourceTable.tryToRouteReply(queryRequest.getGUID(),
                                            receivingConnection))
            handleQueryRequest(queryRequest, receivingConnection);
    }

    /**
     * Returns whether the Ping received was from a GNUTELLA crawler, by 
     * looking at the TTL and hops count.  
     */
    private boolean isCrawlerPing(Message m) {
        if (!(m instanceof PingRequest))
            return false;

        int ttl = (int)m.getTTL();
        int hops = (int)m.getHops();

        if ( (ttl == 2) && (hops == 0))
            return true;
        else
            return false;
    }

    /**
     * Returns whether the PingRequest received was a handshake by looking at 
     * the  ttl and hops count.  (ttl should be 0 and hops should be 1) since we 
     * should be calling this method after calling hop on the messsage.
     *
     * @required - m.hop() has been called
     */
    private boolean isHandshakePing(PingRequest pr) {
        int ttl = (int)pr.getTTL(); 
        int hops = (int)pr.getHops();

        if ((ttl == 0) && (hops == 1))
            return true;
        else
            return false;
    }


    /**
     * The default handler for PingRequests received.  It increments the number 
     * of ping requests received, checks if the Ping Reply Cache has expired, 
     * (and if so, refreshes the cache), sets the ping information for this
     * particular managed connection and sends back some pongs (from the cache).
     */
    protected void handlePingRequest(PingRequest pingRequest,
                                     ManagedConnection receivingConnection)
    {
        if (isHandshakePing(pingRequest))
        {
            //respond with our own address
            respondToPingRequest(pingRequest, _acceptor, receivingConnection); 
            return;
        }
        

        //if the receiving connection needs to throttle the ping, then just
        //return.
        if (receivingConnection.throttlePing())
            return;
            
        _numProcessedPingRequests++;

        //if PingReplyCache expired, refresh cache.  This has to be synchronized,
        //so that two different threads (i.e., ManagedConnections) don't think 
        //that the cache has expired and cause two broadcast pings.
        synchronized(this) {
            if (_catcher.cacheExpired())
                refreshPingReplyCache(receivingConnection);
        }

        //set necessary info in the connection.  Since hop() has been called on
        //the message, we need to set the needed to the original TTL.
        receivingConnection.setLastPingGUID(pingRequest.getGUID());
        receivingConnection.setNeededPingReplies(((int)pingRequest.getTTL()+1));
        
        respondToPingRequest(pingRequest, _acceptor, receivingConnection);

        //send back pongs
        sendSomePongs(receivingConnection);
    }

    /**
     * Sends Pongs to the receiving connection after retrieving random pongs
     * from the host catcher.  The pongs are sent based on the number of 
     * pongs needed per TTL.  The max number of pongs sent back is 
     * MAX_PONGS_TO_RETURN.  
     */
    private void sendSomePongs(ManagedConnection receivingConnection)
    {
        byte[] guid = receivingConnection.getLastPingGUID();
        int[] neededPongs = receivingConnection.getNeededPongsList();
        int ttl = receivingConnection.getLastPingTTL(); 
        int neededCount = receivingConnection.getTotalPongsNeeded();
        if (neededCount > MAX_PONGS_TO_RETURN)
            neededCount = MAX_PONGS_TO_RETURN;

        for (Iterator iter = _catcher.getNPingReplies(receivingConnection, 
                                                      neededCount, ttl); 
             iter.hasNext(); )
        {
            PingReply cachedPingReply = (PingReply)iter.next();
            int hops = (int)cachedPingReply.getHops();
            if (neededPongs[hops-1] > 0) 
            {
                PingReply pr = new PingReply(guid, cachedPingReply.getTTL(), 
                    (byte)hops, cachedPingReply.getPort(), 
                    cachedPingReply.getIPBytes(), cachedPingReply.getFiles(), 
                    cachedPingReply.getKbytes());
                receivingConnection.send(pr);
                neededPongs[hops-1]--;
            }
        }
    }


    /**
     * Sends out the Ping Replies of all the neighbors that we are connected to.
     * (i.e., Ping Replies that we received that are 1 hop away from us.  Since
     *  a crawler might send back a dummy PingReply when we send a PingRequest 
     * to it, we have to make sure, we don't send back the dummy PingReply.  
     *
     * @requires - sending the pings to a crawler connection (i.e., a Gnutella
     *             crawler connected to us and wants pongs of all of our 
     *             neighbors.
     */
    private void sendCrawlerPingReplies(PingRequest pingRequest,
                                        ManagedConnection crawlerConnection)
    {
        byte[] guid = pingRequest.getGUID();

        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.
        List list=_manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection conn = (ManagedConnection)list.get(i);
            //don't send ping to connection whose ping caused the pong cache 
            //refresh and don't send if remote stored pong is null.
            if (conn != crawlerConnection) 
            {
                PingReply neighborInfo = conn.getRemotePong(guid);
                if (neighborInfo != null)
                    crawlerConnection.send(neighborInfo);
            }
        }
    }

    /**
     * Refresh the PingReplyCache by clearing the main pong cache (which 
     * internally copies the contents of the main cache into the reserve cache)
     * and then broadcasting a ping to all the new clients.
     */
    private void refreshPingReplyCache(ManagedConnection receivingConnection)
    {
        _catcher.clearCache();

        broadcastPingRequest(receivingConnection);
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
     * Sends the ping request to the designated connection.  This is used
     * by the ConnectionManager to send the initial Ping Request, once a 
     * connection is established.
     */
    public void sendPingRequest(PingRequest pingRequest,
                                ManagedConnection connection)
    {
        connection.send(pingRequest);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest queryRequest,
                                 ManagedConnection connection)
    {
        _querySourceTable.routeReply(queryRequest.getGUID(), _forMeReplyHandler);
        connection.send(queryRequest);
    }

    /**
     * Broadcasts the query request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastQueryRequest(QueryRequest queryRequest)
    {
        _querySourceTable.routeReply(queryRequest.getGUID(), _forMeReplyHandler);
        broadcastQueryRequest(queryRequest, null, _manager);
    }

    /**
     * Broadcast a ping request with TTL = max ttl for ping reply cache fresh.
     * Most times only broadcast the ping request to newer clients (i.e., 
     * Gnutella Protocol Version 0.6 or higher), but every ten times send it
     * to the older clients also.
     * 
     * Note: This method should only be called when refreshing the PingReply 
     * Cache.
     */
    protected void broadcastPingRequest(ManagedConnection receivingConnection)
    {
        boolean sendToOlderClients = false;
        if (_pingBroadcastCount >= 10)
        {
            sendToOlderClients = true;
            _pingBroadcastCount = 0;
        }
        else
        {
            _pingBroadcastCount++;
        }

        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.
        List list=_manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection conn = (ManagedConnection)list.get(i);
            //don't send ping to connection whose ping caused the pong cache 
            //refresh.
            if (conn != receivingConnection) 
            {
                if (!sendToOlderClients) //send only to "newer" clients.
                {
                    if(conn.isOldClient()) 
                        continue;
                }
                conn.send(
                    new PingRequest((byte)this.MAX_TTL_FOR_CACHE_REFRESH));
            }
        }
        _numBroadcastPingRequests++;
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
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if(c != receivingConnection) {
                //Send query along any connection to an old client, or to a new
                //client with routing information for the given keyword.  TODO:
                //because of some very obscure optimization rules, it's actually
                //possible that qi could be non-null but not initialized.  Need
                //to be more careful about locking here.
                ManagedConnectionQueryInfo qi=c.getQueryRouteState();
                if (qi==null) 
                    //Either a new client, or an old client that's not yet
                    //sent us a table message.
                    c.send(queryRequest);
                else if (!qi.lastReceived.isPatched() 
                             || qi.lastReceived.contains(queryRequest))
                    //A new client with routing entry, or one that hasn't yet
                    //finished sending the patch.
                    c.send(queryRequest);              
            }
        }
    }

    /**
     * Respond to the ping request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or send
     * a ping reply to the connection.
     * This method is called from the default handlePingRequest.
     */
    protected abstract void respondToPingRequest(PingRequest pingRequest,
                                                 Acceptor acceptor,
                                                 ManagedConnection connection);


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
     * Try to add the ping reply to the cache in the host catcher.  If it was
     * successful, try to send the pong to other connections that need the 
     * pong.
     */
    protected void handlePingReply(PingReply pingReply,
                                   ManagedConnection receivingConnection)
    {
        //If pingReply is a handshake ping, store it in receivingConnection.
        //Recall that the handshake ping is sent when creating a connection in
        //order to discover the ports of incoming connections, among other
        //things.  These ports (and addresses!) can be given to crawlers later.
        //
        //Here's the catch: what do you do with handshake pongs from outgoing
        //connections?  If you don't add them to the cache, you won't be able to
        //manually connect (through the GUI) to host discovery services like
        //router.limewire.com.  On the other hand, if you store them in the
        //cache, you may later give out the addresses of people who can't accept
        //incoming connections.  Thankfully there is an easy solution; only add
        //outgoing handshake pongs to the cache if their addresses and ports
        //don't match the address and port of the connection.  The problem is
        //that if someone forces their port, you add bogus pongs to the cache.
        //Similar things happen if you connect to "127.0.0.1".
        if (pingReply.getHops()==1
               && matches(pingReply.getGUID(),
                          receivingConnection.getHandshakeGUID())
               && (!receivingConnection.isOutgoing() 
                      || matches(pingReply, receivingConnection))) {
            receivingConnection.setRemotePong(pingReply);
            return;
        }
        
        //add to cache and send pong to other connections, if it was 
        //successfully added to the cache.
        if (_catcher.addToCache(pingReply, receivingConnection))
        {
            _numPingReplies++;
            //send pong to other connections
            sendPongToOtherConnections(pingReply, receivingConnection);
        }
    }
    
    /** Returns true iff g1 and g2 are non-null and bytewise equal.
     *  @param g1 a 16-byte GUID, or null
     *  @param g2 a 16-byte GUID, or null */
    private static boolean matches(byte[] g1, byte[] g2) {
        if (g1==null || g2==null)
            return false;
        return (new GUID(g1)).equals(new GUID(g2));
    }

    /** Returns true iff pong has the same address and port as conn. */
    private static boolean matches(PingReply pong, Connection conn) {
        byte[] pb=pong.getIPBytes();
        byte[] cb=conn.getInetAddress().getAddress();
        return pong.getPort()==conn.getOrigPort()
            && Arrays.equals(pong.getIPBytes(),
                             conn.getInetAddress().getAddress());
    }
                                   

    /**
     * Forward on the PingReply to all connections except the one that
     * we received the PingReply on.  But only forward the PingReply
     * if the connection needs a PingReply (for that hops).
     */
    protected void sendPongToOtherConnections(PingReply pingReply,
                                              ManagedConnection connection)
    {
        //necessary fields for forwarding the PingReply
        byte hops = pingReply.getHops();
        byte[] ip = pingReply.getIPBytes();
        int port = pingReply.getPort();
        long files = pingReply.getFiles();
        long kbytes = pingReply.getKbytes();
        byte ttl = pingReply.getTTL();

        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.
        List list=_manager.getInitializedConnections2();
        for (int i=0; i<list.size(); i++)
        {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if (c != connection)
            {
                int[] neededPongs = c.getNeededPongsList();
                if (neededPongs[hops-1] > 0)
                {
                    byte[] guid = c.getLastPingGUID();
                    //send pong
                    PingReply pr = new PingReply(guid, ttl, hops, port, ip,
                        files, kbytes);
                    c.send(pr);
                    neededPongs[hops-1]--;
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
    protected void handleQueryReply(QueryReply queryReply,
                                    ManagedConnection receivingConnection)
    {
        ReplyHandler replyHandler =
            _querySourceTable.getReplyHandler(queryReply.getGUID());

        if(replyHandler != null)
        {
            _numQueryReplies++;
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushSourceTable.routeReply(queryReply.getClientGUID(),
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
    protected void handlePushRequest(PushRequest pushRequest,
                                     ManagedConnection receivingConnection)
    {
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler =
            _pushSourceTable.getReplyHandler(pushRequest.getClientGUID());

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
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendQueryReply(QueryReply queryReply)
        throws IOException
    {
        ReplyHandler replyHandler =
            _querySourceTable.getReplyHandler(queryReply.getGUID());

        if(replyHandler != null)
        {
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushSourceTable.routeReply(queryReply.getClientGUID(),
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
            _pushSourceTable.getReplyHandler(pushRequest.getClientGUID());

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
                //There's really no need to check if c is an old client here.
                qi=new ManagedConnectionQueryInfo();
                receivingConnection.setQueryRouteState(qi);
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
                ManagedConnectionQueryInfo qi=c.getQueryRouteState();
                if (qi==null) {
                    if (c.isOldClient())
                        continue;
                    qi=new ManagedConnectionQueryInfo();
                    c.setQueryRouteState(qi);
                }
                    
                //Create table to send on this connection...
                QueryRouteTable table=createRouteTable(c);

                //..and send each piece.
                //TODO2: use incremental and interleaved update
                for (Iterator iter=table.encode(qi.lastSent); iter.hasNext(); ) {  
                    RouteTableMessage m=(RouteTableMessage)iter.next();
                    System.out.println("    Sending "+m.toString()+" to "+c);
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

        //...and those of all neighbors except c.  Use higher TTLs on these.
        List list=_manager.getInitializedConnections();
        for(int i=0; i<list.size(); i++) { 
            ManagedConnection c2=(ManagedConnection)list.get(i);
            if (c2==c)
                continue;
            ManagedConnectionQueryInfo qi=c2.getQueryRouteState();
            if (qi!=null)
                ret.addAll(qi.lastReceived);
        }
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
     * @return the number of Ping Requests broadcasted to all the neighbors.
     */
    public int getNumBroadcastPingRequests()
    {
        return _numBroadcastPingRequests;
    }

    /**
     * @return the number of processed Ping Requests.
     */
    public int getNumProcessedPingRequests()
    {
        return _numProcessedPingRequests;
    }

    /**
     * @return the number of PingReplies we actually process after filtering,
     * either by routing to another connection or updating our horizon
     * statistics.
     * Note that excludes PingReplies we generate.  That number is basically
     * _numRecvdPingRequests (the number of PingRequests for which we generate a
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
