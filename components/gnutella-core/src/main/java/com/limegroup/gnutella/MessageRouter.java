package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import java.io.IOException;
import com.limegroup.gnutella.routing.QueryRouteTable;

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
    //even though the Ping Reply Cache is a singleton, holding onto a 
    //reference is better since we access it many times.
    protected PingReplyCache _pongCache;


    ///////////////////////////// Core Routing State ///////////////////////    
    /**
     * @return the GUID we attach to QueryReplies to allow PushRequests in
     *         response.
     */
    private byte[] _clientGUID;

    private ForMeReplyHandler _forMeReplyHandler = new ForMeReplyHandler();

    /**
     * Maps Managed Connection to ManagedConnectionPingInfo to keep track
     * of Ping Request Information used when routing Ping Replies and 
     * throttling Ping Requests from old client.s
     */
    private HashMap /* ManagedConnection -> ManagedConnectionPingInfo */ 
        _pingInformationTable = new HashMap();

    /**
     * Keeps track of query route table state for each connection.  
     * This helps you decide where to send queries.  (Compare with
     * _queryRouteTable, which helps you decide where to send responses,
     * and helps filter duplicate queries.)
     */
    private HashMap /* ManagedConnection -> QueryRouteTable */ 
        _queryInformationTable = new HashMap();

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
     * Used to make sure we are not waiting forever when trying to return
     * some Ping Reply to a Ping Request.  (This can happen if there is 
     * not that many values in the cache at a given time).
     */
    private static final long MAX_WAIT_TIME_GETTING_PING_REPLIES = 200;

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
        _pongCache = PingReplyCache.instance();
    }

    public String getQueryRouteTableDump()
    {
        return _querySourceTable.toString();
    }

    public String getPushRouteTableDump()
    {
        return _pushSourceTable.toString();
    }

    public String getPingInformationTableDump()
    {
        return _pingInformationTable.toString();
    }

    /**
     * A callback for ConnectionManager to clear a ManagedConnection from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ManagedConnection connection)
    {
        _pingInformationTable.remove(connection);
        _queryInformationTable.remove(connection);
        _querySourceTable.removeReplyHandler(connection);
        _pushSourceTable.removeReplyHandler(connection);
    }

    /**
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplicate(
        QueryRequest queryRequest, ManagedConnection receivingConnection)
    {
        if(_querySourceTable.tryToRouteReply(queryRequest.getGUID(),
                                            receivingConnection))
            handleQueryRequest(queryRequest, receivingConnection);
    }

    /**
     * The default handler for PingRequests received in
     * ManagedConnection.loopForMessages().  It increments the number of
     * ping requests received, checks if the Ping Reply Cache has expired, 
     * (and if so, refreshes the cache), sets the ping information for this
     * particular managed connection and sends back some pongs (from the
     * cache).
     */
    public void handlePingRequest(PingRequest pingRequest,
                                  ManagedConnection receivingConnection)
    {
        ManagedConnectionPingInfo pingInfo = null;
        boolean oldClient = receivingConnection.isOldClient();

        //try to get Ping Info for managed connection.  If it doesn't exist,
        //then add to the mapping of connection to Ping Info.
        if (!_pingInformationTable.containsKey(receivingConnection)) 
        {
            pingInfo = new ManagedConnectionPingInfo(oldClient);
            _pingInformationTable.put(receivingConnection,pingInfo);
        }
        else
        {
            pingInfo = (ManagedConnectionPingInfo)_pingInformationTable.get(
                 receivingConnection);
            //if an older client, see if we need to throttle this ping.
            if (oldClient) 
            {
                if (pingInfo.throttlePing())
                {
                    receivingConnection.countDroppedMessage();
                    return;
                }
            }
        }

        //check for duplicates
        if (pingRequest.getGUID() == pingInfo.getLastGUID())
        {
            receivingConnection.countDroppedMessage();
            return;
        }

        _numPingRequests++;

        //if PingReplyCache expired, refresh cache.
        if (_pongCache.expired())
            refreshPingReplyCache();

        //set necessary info in pingInfo
        pingInfo.setLastGUID(pingRequest.getGUID());
        pingInfo.setNeededPingReplies((int)pingRequest.getTTL());
        
        respondToPingRequest(pingRequest, _acceptor, receivingConnection);

        //send back pongs
        if (_pongCache.size() > pingInfo.getTotalNeeded())
            sendSomePongs(receivingConnection, pingInfo);
        else
            sendAllPongs(receivingConnection, pingInfo);
    }

    /**
     * Sends PingReplies to the receiving connection from the PingReplyCache.
     * The PingReplies are sent based on the number of Ping Replies needed (in
     * the Ping Info) per TTL.  The max number of PingReplies sent back is 
     * MAX_PONGS_TO_RETURN.  The pongs are randomly retrieved from the Ping
     * ReplyCache.
     */
    private void sendSomePongs(ManagedConnection receivingConnection,
                               ManagedConnectionPingInfo pingInfo)
    {
        byte[] guid = pingInfo.getLastGUID();
        int[] neededPongs = pingInfo.getNeededPingReplies();
        int i; //ping reply (based on hops) to return.
        int ttl = pingInfo.getLastTTL(); //ttl of last PingRequest.
        int sentCount = 0;
        //use this to make sure we don't return the same pong twice.
        HashSet pongs = new HashSet();
        Random random = new Random(); //for ping reply ttl
        int neededCount = pingInfo.getTotalNeeded();

        //make sure not to return more than the max num allowed to return.
        if (neededCount > MAX_PONGS_TO_RETURN)
            neededCount = MAX_PONGS_TO_RETURN;

        //this is used to ensure that we are not waiting forever to send the 
        //MAX_PONGS_TO_RETURN.  It avoids the possibility of a race condition
        //where the cache has to be expired while we were still not done sending
        //MAX_PONGS_TO_RETURN pongs to the ping request (because we kept getting
        //the same random i (hops to use) in the cache
        long totalWaitTime = System.currentTimeMillis() + 
            MAX_WAIT_TIME_GETTING_PING_REPLIES;

        while ((sentCount < neededCount) && 
               (System.currentTimeMillis() < totalWaitTime))
        {
            i = random.nextInt(ttl);
            if (neededPongs[i] > 0) 
            {
                PingReply pingReply = 
                    getAPingReply(receivingConnection, pingInfo, i+1);
                //if we already have sent this ping, keep trying until we get
                //one we haven't sent yet.  However, count ensures that if 
                //we get the same one three times, then it's highly likely that
                //there are no more Ping Replies (for that hops) left in the 
                //cache that we haven't already sent out.
                int count = 0; 
                while ((pongs.contains(pingReply)) && (count < 3))
                {
                    pingReply = getAPingReply(receivingConnection, pingInfo, 
                        i+1);
                    count++;
                }
                //if null retrieved from the PingReplyCache, just continue. 
                if (pingReply == null)
                    continue;

                if (count < 3)
                {
                    pongs.add(pingReply);
                    receivingConnection.send(pingReply);
                    neededPongs[i]--;
                    sentCount++;
                }
            }
        }

        pongs.clear(); //clear HashSet, we don't need it anymore
    }

    /**
     * Sends ping replies from the PingReplyCache to the receiving connection.
     * However, in this case, it sends all the Ping Replies stored in the 
     * cache, as there are less Ping Replies in the cache, then the total
     * needed Ping Replies for the receiving connection.
     *
     * @requires - size of PingReplyCache < total num of needed pongs by the
     *             receiving connection.
     */
    private void sendAllPongs(ManagedConnection receivingConnection,
                              ManagedConnectionPingInfo pingInfo)
    {
        byte[] guid = pingInfo.getLastGUID();
        int[] neededPongs = pingInfo.getNeededPingReplies();
        
        for (Iterator iter = _pongCache.iterator(); 
             iter.hasNext(); ) 
        {
            PingReplyCacheEntry entry = (PingReplyCacheEntry)iter.next();
            if (entry.getManagedConnection() != receivingConnection)
            {
                PingReply cachedPingReply = entry.getPingReply();
                int hops = cachedPingReply.getHops();
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
     * Returns a Ping Reply for the specified hops count that was not sent from
     * the receiving connection (in which case, it returns null).  Also
     * returns null, if the entry retrieved from the cache is null.
     */
    private PingReply getAPingReply(ManagedConnection receivingConnection, 
                                    ManagedConnectionPingInfo pingInfo, int hops)
    {
        PingReplyCacheEntry entry = _pongCache.getEntry(hops-1);
        byte[] guid = pingInfo.getLastGUID();

        if (entry == null)
            return null;

        if (entry.getManagedConnection() != receivingConnection)
        {
            PingReply cachedPingReply = entry.getPingReply();
            PingReply pr = new PingReply(guid, cachedPingReply.getTTL(), 
                (byte)hops, cachedPingReply.getPort(), 
                cachedPingReply.getIPBytes(), cachedPingReply.getFiles(), 
                cachedPingReply.getKbytes());
            return pr;
        }
        else
        {
            return null;
        }
    }

    /**
     * Sends out the Ping Replies of all the neighbors that we are connected to.
     * (i.e., Ping Replies that we received with a TTL of 1, or Hosts that are
     * a TTL of 1 (and hop of 1) away from us.  Since a crawler might send back
     * a dummy PingReply when we send a PingRequest to it, we have to make 
     * sure, we don't send back the dummy PingReply
     *
     * @requires - sending the pings to a crawler connection (i.e., a Gnutella
     *             crawler connected to us and wants pongs of all of our 
     *             neighbors.
     */
    public void sendCrawlerPingReplies(byte[] guid,
                                       ManagedConnection connection)
    {
        for (Iterator iter = _pongCache.iterator(1); iter.hasNext(); )
        {
            PingReplyCacheEntry entry = (PingReplyCacheEntry)iter.next();
            if (entry.getManagedConnection() != connection)
            {
                PingReply origReply = entry.getPingReply();
                //ttl = 1 for crawler ping replies.
                PingReply pr = new PingReply(guid, (byte)1, origReply.getPort(), 
                    origReply.getIPBytes(), origReply.getFiles(), 
                    origReply.getKbytes());
                connection.send(pr);
            }
        }
    }

    /**
     * Refresh the PingReplyCache by first emptying contents of the cache into
     * reserve cache (in HostCatcher), clearing the caching, and then 
     * broadcasting a ping to all the new clients.
     */
    private void refreshPingReplyCache()
    {
        _catcher.copyCacheContents(); //copy PingReplyCache into reserve cache.
        
        _pongCache.clear();

        broadcastPingRequest();
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
    public void handleQueryRequest(QueryRequest queryRequest,
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
     * However, only broadcast a ping to newer clients (Gnutella Protocol 
     * Version 0.6 or higher).  
     * 
     * Note: This method should only be called when refreshing the PingReply 
     * Cache.
     */
    private void broadcastPingRequest()
    {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.
        List list=_manager.getInitializedConnections2();
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection conn = (ManagedConnection)list.get(i);
            if(!conn.isOldClient()) //send only to "newer" clients.
                conn.send(new PingRequest((byte)this.MAX_TTL_FOR_CACHE_REFRESH));
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
        for(int i=0; i<list.size(); i++)
        {
            ManagedConnection c = (ManagedConnection)list.get(i);
            if(c != receivingConnection) {
                //Send query along any connection to an old client, or to a new client
                //with routing information for the given keyword.
                QueryRouteTable qrt=(QueryRouteTable)_queryInformationTable.get(c);
                if (qrt==null)
                    c.send(queryRequest);
                else if (qrt.contains(queryRequest))
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
    public void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection)
    {
        //if received from an old client or from a router, place the PingReply
        //in the reserve cache (i.e., hostcatcher).
        if ((receivingConnection.isOldClient()) || 
            (receivingConnection.isRouterConnection()))
        {
            _catcher.addToReserveCache(pingReply, receivingConnection);
            receivingConnection.countDroppedMessage();
            return;
        }

        //add to cache
        _pongCache.addPingReply(pingReply, receivingConnection);

        _numPingReplies++;
        //send pong to other connections
        sendPongToOtherConnections(pingReply, receivingConnection);        
    }

    /**
     * Forward on the PingReply to all connections except the one that
     * we received the PingReply on.  But only forward the PingReply
     * if the connection needs a PingReply (for that hops).  
     */
    private void sendPongToOtherConnections(PingReply pingReply,
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
                ManagedConnectionPingInfo pingInfo = 
                    (ManagedConnectionPingInfo)_pingInformationTable.get(c);
                //no ping information, connection hasn't sent a ping yet (other
                //than maybe a handshake ping), so just continue.
                if (pingInfo == null)
                    continue;
                
                int[] neededPongs = pingInfo.getNeededPingReplies();
                if (neededPongs[hops-1] > 0)
                {
                    byte[] guid = pingInfo.getLastGUID();
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
    public void handleQueryReply(QueryReply queryReply,
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
    public void handlePushRequest(PushRequest pushRequest,
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
