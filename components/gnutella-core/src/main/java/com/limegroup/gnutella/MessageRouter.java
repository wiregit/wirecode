package com.limegroup.gnutella;

import com.sun.java.util.collections.Iterator;
import java.io.IOException;

/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
public class MessageRouter
{
    private HostCatcher _catcher;
    private ConnectionManager _manager;
    private Acceptor _acceptor;
    private ActivityCallback _callback;

    private GUID _clientId;

    private PingRequestHandler _pingRequestHandler;
    private PingReplyHandler _pingReplyHandler;
    private QueryRequestHandler _queryRequestHandler;
    private QueryReplyHandler _queryReplyHandler;
    private PushRequestHandler _pushRequestHandler;

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
     */
    private RouteTable _pushRouteTable = new RouteTable(2048);

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
     * Note that excludes PushRequests we generate through HTTPDownloader.
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



    /**
     * Creates a MessageRouter.  Must call initialize before using.
     */
    public MessageRouter(ActivityCallback callback,
                         PingRequestHandler pingRequestHandler,
                         PingReplyHandler pingReplyHandler,
                         QueryRequestHandler queryRequestHandler,
                         QueryReplyHandler queryReplyHandler,
                         PushRequestHandler pushRequestHandler)
    {
        _callback = callback;
        _pingRequestHandler = pingRequestHandler;
        _pingReplyHandler = pingReplyHandler;
        _queryRequestHandler = queryRequestHandler;
        _queryReplyHandler = queryReplyHandler;
        _pushRequestHandler = pushRequestHandler;

        try
        {
            _clientId = new GUID(GUID.fromHexString(
                SettingsManager.instance().getClientID()));
        }
        catch (IllegalArgumentException e)
        {
            //This should never happen! But if it does, we can recover.
            _clientId = new GUID(Message.makeGuid());
        }
    }

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize(Acceptor acceptor,
                           ConnectionManager manager,
                           HostCatcher catcher)
    {
        _acceptor = acceptor;
        _manager = manager;
        _catcher = catcher;
    }

    /**
     * @return the GUID we attach to QueryReplies to allow PushRequests in
     *         response.
     */
    public byte[] getClientGUID()
    {
        return _clientId.bytes();
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
    public void removeReplyHandler(ReplyHandler replyHandler)
    {
        _pingRouteTable.removeReplyHandler(replyHandler);
        _queryRouteTable.removeReplyHandler(replyHandler);
        _pushRouteTable.removeReplyHandler(replyHandler);
    }

    //
    // Begin message receiving calls
    //

    /**
     * Checks that a PingRequest is not yet routed, then increments the
     * PingRequest count and delegates to the PingRequestHandler to handle it.
     */
    public void handlePingRequest(PingRequest pingRequest,
                                  ManagedConnection receivingConnection)
            throws IOException
    {
        if(_pingRouteTable.getReplyHandler(pingRequest.getGUID()) == null)
        {
            _numPingRequests++;
            _pingRequestHandler.handlePingRequest(pingRequest,
                                                  receivingConnection,
                                                  this,
                                                  _callback,
                                                  _acceptor);
        }
    }

    /**
     * Checks that a QueryRequest is not yet routed, then increments the
     * QueryRequest count and delegates to the PingRequestHandler to handle it.
     */
    public void handleQueryRequest(QueryRequest queryRequest,
                                   ManagedConnection receivingConnection)
            throws IOException
    {
        if(_queryRouteTable.getReplyHandler(queryRequest.getGUID()) == null)
        {
            _numQueryRequests++;
            _queryRequestHandler.handleQueryRequest(queryRequest,
                                                    receivingConnection,
                                                    this,
                                                    _callback,
                                                    _acceptor);
        }
    }

    /**
     * Uses the ping route table to route a ping reply.  If an appropriate route
     * doesn't exist, records the error statistics.  On sucessful routing,
     * the PingReply count is incremented.
     *
     * In all cases, the ping reply is recorded into the host catcher.
     */
    public void routePingReply(PingReply pingReply,
                               ManagedConnection receivingConnection)
    {
        //update hostcatcher (even if the reply isn't for me)
        _catcher.spy(pingReply);

        PingReplyHandler replyHandler = (PingReplyHandler)
            _pingRouteTable.getReplyHandler(pingReply.getGUID());

        if(replyHandler != null)
        {
            _numPingReplies++;
            replyHandler.handlePingReply(pingReply,
                                         receivingConnection,
                                         this,
                                         _callback);
        }
        else
        {
            _numRouteErrors++;
            receivingConnection.countDroppedMessage();
        }
    }

    /**
     * Uses the query route table to route a query reply.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the QueryReply count is incremented, and a routing is created for
     * subsequent PushRequests
     */
    public void routeQueryReply(QueryReply queryReply,
                                ManagedConnection receivingConnection)
    {
        QueryReplyHandler replyHandler = (QueryReplyHandler)
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
                                          receivingConnection,
                                          this,
                                          _callback);
        }
        else
        {
            _numRouteErrors++;
            receivingConnection.countDroppedMessage();
        }
    }

    /**
     * Uses the push route table to route a push request.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the PushRequest count is incremented.
     */
    public void routePushRequest(PushRequest pushRequest,
                                 ManagedConnection receivingConnection)
    {
        // Note the use of getClientGUID() here, not getGUID()
        PushRequestHandler replyHandler = (PushRequestHandler)
            _pushRouteTable.getReplyHandler(pushRequest.getClientGUID());

        if(replyHandler != null)
        {
            _numPushRequests++;
            replyHandler.handlePushRequest(pushRequest, this, _callback);
        }
        else
        {
            _numRouteErrors++;
            receivingConnection.countDroppedMessage();
        }
    }

    //
    // End message receiving calls
    //

    //
    // Begin message sending calls
    //

    /**
     * Sends the ping request to the designated connection,
     * setting up the routing to the MessageRouter's PingReplyHandler
     */
    public void sendPingRequest(PingRequest pingRequest,
                                ManagedConnection connection)
        throws IOException
    {
        _pingRouteTable.routeReply(pingRequest.getGUID(), _pingReplyHandler);
        connection.send(pingRequest);
    }

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the routing to the MessageRouter's PingReplyHandler
     */
    public void broadcastPingRequest(PingRequest pingRequest)
    {
        broadcastPingRequest(pingRequest, _pingReplyHandler);
    }

    /**
     * Broadcasts the ping request to all initialized connections that
     * are not the designated PingReplyHandler, setting up the routing
     * to the designated PingReplyHandler
     */
    public void broadcastPingRequest(PingRequest pingRequest,
                                     PingReplyHandler replyHandler)
    {
        _pingRouteTable.routeReply(pingRequest.getGUID(), replyHandler);
        broadcastRequest(pingRequest, replyHandler);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the routing to the MessageRouter's QueryReplyHandler
     */
    public void sendQueryRequest(QueryRequest queryRequest,
                                 ManagedConnection connection)
            throws IOException
    {
        _queryRouteTable.routeReply(queryRequest.getGUID(), _queryReplyHandler);
        connection.send(queryRequest);
    }

    /**
     * Broadcasts the query request to all initialized connections,
     * setting up the routing to the MessageRouter's QueryReplyHandler
     */
    public void broadcastQueryRequest(QueryRequest queryRequest)
    {
        broadcastQueryRequest(queryRequest, _queryReplyHandler);
    }

    /**
     * Broadcasts the query request to all initialized connections that
     * are not the designated QueryReplyHandler, setting up the routing
     * to the designated QueryReplyHandler
     */
    public void broadcastQueryRequest(QueryRequest queryRequest,
                                      QueryReplyHandler replyHandler)
    {
        _queryRouteTable.routeReply(queryRequest.getGUID(), replyHandler);
        broadcastRequest(queryRequest, replyHandler);
    }

    /**
     * Sends the query reply to the designated connection,
     * setting up the routing to the MessageRouter's PushRequestHandler.
     */
    public void sendQueryReply(QueryReply queryReply,
                               ManagedConnection connection)
        throws IOException
    {
        // Note the use of getClientGUID() here, not getGUID()
        _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                   _pushRequestHandler);
        connection.send(queryReply);
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
        PushRequestHandler replyHandler = (PushRequestHandler)
            _pushRouteTable.getReplyHandler(pushRequest.getClientGUID());

        if(replyHandler != null)
        {
            _numPushRequests++;
            replyHandler.handlePushRequest(pushRequest, this, _callback);
        }
        else
        {
            throw new IOException();
        }
    }

    /**
     * A private method called from broadcastPingRequest and
     * broadcastQueryRequest.
     *
     * @modifies network
     * @effects sends the message m to all connections that are not the
     * designated reply handler
     *
     * Underlying IO errors (e.g., because a connection has closed) are caught
     * and silently ignored.
     */
    private void broadcastRequest(Message m, Object replyHandler)
    {
        Assert.that(m != null);
        // Note the use of initializedConnections only.
        for(Iterator iterConnections =
                _manager.initializedConnections();
            iterConnections.hasNext();  )
        {
            Connection c = (Connection)iterConnections.next();
            if(c != replyHandler)
            {
                try
                {
                    c.send(m);
                }
                catch (IOException e)
                {}
            }
        }
    }

    //
    // End message sending calls
    //


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
     * Note that excludes PushRequests we generate through HTTPDownloader.
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
}
