package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

/**
 * A Connection managed by a connection managed.  Includes a loopForMessages
 * method that runs forever (or until an IOException occurs), receiving and
 * replying to Gnutella messages.
 *
 * This class implements PingReplyHandler to route PingReplies that originated
 * from it.
 *
 * @author Ron Vogl
 */
public class ManagedConnection
        extends Connection
        implements PingReplyHandler, QueryReplyHandler, PushRequestHandler {
    private MessageRouter _router;
    private ConnectionManager _manager;
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    private int _numReceivedMessagesDropped=0;

    /*
     * _lastSent/_lastSentDropped and _lastReceived/_lastRecvDropped the
     * values of sent/sentDropped and received/_numReceivedMessagesDropped at
     * the last call to getPercentDropped.  These are synchronized by this;
     * finer-grained schemes could be used.
     */
    private int _lastReceived=0;
    private int _lastRecvDropped=0;
    private int _lastSent=0;
    private int _lastSentDropped=0;

    private long _totalHorizonFileSize;
    private long _numHorizonFiles;
    private long _numHorizonHosts;

    /**
     * Creates an outgoing connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     */
    ManagedConnection(String host,
                      int port,
                      MessageRouter router,
                      ConnectionManager manager) {
        super(host, port);
        _router = router;
        _manager = manager;
    }

    /**
     * Creates an incoming connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the Gnutella
     *  handshake.
     */
    ManagedConnection(Socket socket,
                      MessageRouter router,
                      ConnectionManager manager) {
        super(socket);
        _router = router;
        _manager = manager;
    }

    /**
     * Override of send to do ConnectionManager stats and to properly shut down
     * the connection on IOException
     */
    public void send(Message m) throws IOException {
        try {
            super.send(m);
        } catch(IOException e) {
            _manager.remove(this);
            throw e;
        }
        _router.countMessage();
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
            _manager.remove(this);
            throw e;
        }
        _router.countMessage();
        return m;
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
     *   Returns if either the connection is closed or an error happens.
     *   If this happens, removes itself from the manager's connection list,
     *   so no further cleanup is necessary.  No exception is thrown
     *
     * @throws IOException passed on from the receive call; failures to forward
     *         or route messages are silently swallowed, allowing the message
     *         loop to continue.
     */
    void loopForMessages()
            throws IOException {
        while (true) {
            Message m=null;
            try {
                m=receive();
                if (m==null)
                    continue;
            } catch (BadPacketException e) {
                // Don't increment any message counters here.  It's as if
                // the packet never existed
                continue;
            }

            // Run through the route spam filter and drop accordingly.
            if (!_routeFilter.allow(m)) {
                _router.countFilteredMessage();
                _numReceivedMessagesDropped++;
                continue;
            }

            if(m instanceof PingRequest)
                _router.handlePingRequest((PingRequest)m, this);
            else if (m instanceof PingReply)
                _router.routePingReply((PingReply)m, this);
            else if (m instanceof QueryRequest)
                _router.handleQueryRequest((QueryRequest)m, this);
            else if (m instanceof QueryReply)
                _router.routeQueryReply((QueryReply)m, this);
            else if (m instanceof PushRequest)
                _router.routePushRequest((PushRequest)m, this);
        }
    }

    //
    // Begin Message dropping and filtering calls
    //

    /**
     * A callback for the ConnectionManager to inform this connection that a
     * message was dropped.  This happens when a reply received from this
     * connection has no routing path
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

    //
    // Begin Message dropping and filtering calls
    //


    //
    // Begin reply forwarding calls
    //

    /**
     * This method is called when a reply is received for a PingRequest
     * originating on this Connection.  So, just adjust the hops and send it
     * back.
     */
    public void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection,
                                MessageRouter router,
                                ActivityCallback callback) {
        pingReply.hop();
        try {
            send(pingReply);
        } catch(IOException e) {
        }
    }

    /**
     * This method is called when a reply is received for a QueryRequest
     * originating on this Connection.  So, just adjust the hops and send it
     * back.  If the sending fails, the calls fails silently.
     *
     * Note that we delegate the sending to the MessageRouter so that
     * it can properly set up a routing for PushRequest sent in reply to
     * the QueryReply.  PushRequests are odd this way; they are
     * QueryReplyReplies
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ManagedConnection receivingConnection,
                                 MessageRouter router,
                                 ActivityCallback callback) {
        queryReply.hop();
        try {
            router.sendQueryReply(queryReply, this, receivingConnection);
        } catch(IOException e) {
        }
    }

    /**
     * This method is called when a PushRequest is received for a QueryReply
     * originating on this Connection.  So, just adjust the hops and send it
     * back.
     */
    public void handlePushRequest(PushRequest pushRequest,
                                  MessageRouter router,
                                  ActivityCallback callback) {
        pushRequest.hop();
        try {
            send(pushRequest);
        } catch(IOException e) {
        }
    }

    //
    // End reply forwarding calls
    //


    /** Clears the statistics about files reachable from me. */
    public void clearHorizonStats() {
        _totalHorizonFileSize=0;
        _numHorizonHosts=0;
        _numHorizonFiles=0;
    }

    /**
     * This method is called when a reply is received by this connection for a
     * PingRequest originated by the host running this process.
     */
    public void updateHorizonStats(PingReply pingReply) {
        _totalHorizonFileSize += pingReply.getKbytes();
        _numHorizonFiles += pingReply.getFiles();
        _numHorizonHosts++;
    }

    /** Returns the number of hosts reachable from me. */
    public long getNumHosts() {
        return _numHorizonHosts;
    }

    /** Returns the number of files reachable from me. */
    public long getNumFiles() {
        return _numHorizonFiles;
    }

    /** Returns the size of all files reachable from me. */
    public long getTotalFileSize() {
        return _totalHorizonFileSize;
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
        int rdiff=getNumMessagesReceived()-_lastReceived;
        int ddiff=_numReceivedMessagesDropped-_lastRecvDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastReceived=getNumMessagesReceived();
        _lastRecvDropped = _numReceivedMessagesDropped;
        return percent;
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were
     *  dropped by this end of the connection.
     */
    public synchronized float getPercentSentDropped() {
        int rdiff=getNumMessagesSent()-_lastSent;
        int ddiff=getNumSentMessagesDropped()-_lastSentDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastSent=getNumMessagesSent();
        _lastSentDropped = getNumSentMessagesDropped();
        return percent;
    }
}
