package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import com.limegroup.gnutella.routing.*;

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
 * ManagedConnection maintain a large number of statistics, such as the number
 * of bytes read and written.  On the query-routing3-branch and pong-caching CVS
 * branches, these statistics have been bundled into a single object, reducing
 * the complexity of ManagedConnection.  We will likely merge this change into
 * ManagedConnection in the future, so please bear with this for now.<p>
 * 
 * This class implements ReplyHandler to route pongs and query replies that
 * originated from it.<p> 
 */
public class ManagedConnection
        extends Connection
        implements ReplyHandler {
    private MessageRouter _router;
    private ConnectionManager _manager;

    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    /*  The underlying socket, its address, and input and output
     *  streams.  sock, in, and out are null iff this is in the
     *  unconnected state.  For thread synchronization reasons, it is
     *  important that this only be modified by the send(m) and
     *  receive() methods.
     *
     *  This implementation has two goals:
     *    1) a slow connection cannot prevent other connections from making
     *       progress.  Packets must be dropped.
     *    2) packets should be sent in large batches to the OS, but the
     *       batches should not be so long as to cause undue latency.
     *
     *  Towards this end, we queue sent messages on the front of
     *  outputQueue.  Whenever outputQueue contains at least
     *  BATCH_SIZE messages or QUEUE_TIME milliseconds has passed, the
     *  messages on outputQueue are written to out.  Out is then
     *  flushed exactly once. outputQueue is fixed size, so if the
     *  output thread can't keep up with the producer, packets will be
     *  (intentionally) droppped.  LOCKING: obtain outputQueueLock
     *  lock before modifying or replacing outputQueue.
     *
     *  One problem with this scheme is that IOExceptions from sending
     *  data happen asynchronously.  When this happens, _connectionClosed
     *  is set to true.  Then the next time send is called, an IOException
     *  is thrown.  
     */

    /** The (approximate) max time a packet can be queued, in milliseconds. */
    private static final int QUEUE_TIME=750;
    /** The number of packets to present to the OS at a time.  This
     *  should be roughly proportional to the OS's send buffer. */
    private static final int BATCH_SIZE=50;
    /** The size of the queue.  This must be larger than BATCH_SIZE.
     *  Larger values tolerate temporary bursts of producer traffic
     *  without traffic but may result in overall latency. */
    private static final int QUEUE_SIZE=500;
    /** A lock to protect the swapping of outputQueue and oldOutputQueue. */
    private Object _outputQueueLock=new Object();
    /** The producer's queue. */
    private volatile Buffer _outputQueue=new Buffer(QUEUE_SIZE);
    /** The consumer's queue. */
    private volatile Buffer _oldOutputQueue=new Buffer(QUEUE_SIZE);
    /** True iff the output thread should write the queue immediately.
     *  Synchronized by _outputQueueLock. */
    private volatile boolean _flushImmediately=false;
    /** A condition variable used to implement the flush() method.
     *  Call notify when outputQueueLock and oldOutputQueueLock are
     *  empty. */
    private Object _flushLock=new Object();


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

    /***************************************************************************
     * Horizon statistics. We measure the horizon by looking at all ping replies
     * coming per connection--regardless whether they are in response to pings
     * originating from us.  To avoid double-counting ping replies, we keep a
     * set of Endpoint's around, bounded in size to save memory.  This scheme is
     * robust in the face of pong throttling.  Note however that we cannot
     * discern pings from multiple hosts with the same private address.  But you
     * are probably not interested in such hosts anyway.  Also, it cannot detect
     * duplicates reachable through multiple connections.
     *
     * The problem with this scheme is that the numbers tend to grow without
     * bound, even if hosts leave the network.  Ideally we'd like to clear all
     * pongs that are more than HORIZON_UPDATE_TIME milliseconds old, but that's
     * difficult to implement efficiently.  As a simplication, we periodically
     * clear the set of pongs every HORIZON_UPDATE_TIME milliseconds (by calling
     * updateHorizonStats) and start recounting.  While we are recounting, we
     * return the last size of the set.  So pongs in the set are
     * HORIZON_UPDATE_TIME to 2*HORIZON_UPDATE_TIME milliseconds old.
     * 
     * LOCKING: obtain this' monitor
     **************************************************************************/
    private boolean _horizonEnabled=true;
    /** The approximate time to expire pongs, in milliseconds. */
    private final static long HORIZON_UPDATE_TIME=10*60*1000; //10 minutes
    /** The last time refreshHorizonStats was called. */
    private long _lastRefreshHorizonTime=System.currentTimeMillis();
    /** True iff refreshHorizonStats has been called. */
    private boolean _refreshedHorizonStats=false;
    /** The max number of pongs to save. */
    private static final int MAX_PING_REPLIES=4000;
    /** The endpoints of pongs seen before.  Eliminates duplicates. */
    private Set /* of Endpoint */ _pingReplies=new HashSet();
    /** The size of _pingReplies before updateHorizonStats was called. */
    private long _totalHorizonFileSize=0;
    private long _numHorizonFiles=0;
    private long _numHorizonHosts=0;
    /** INVARIANT: _nextTotalHorizonFileSize==_pingReplies.size() */
    private long _nextTotalHorizonFileSize=0;
    private long _nextNumHorizonFiles=0;
    private long _nextNumHorizonHosts=0;
    

    /**
     * The query routing state for each "new client" connection, or null if the
     * connection doesn't support QRP.  Helps you decide when to send queries.
     * (Compare with _querySourceTable of MessageRouter, which helps filter
     * duplicate queries and decide where to send responses.)  
     */
    private volatile ManagedConnectionQueryInfo queryInfo = null;
    /** The next time I should send a query route table to this connection.
     *  Valid only for client-supernode connections. */
    private long _nextQRPForwardTime;


    /** The total number of bytes sent/received since last checked. 
     *  These are not synchronized and not guaranteed to be 100% accurate. */
    private volatile long _bytesSent;
    private volatile long _bytesReceived;

    /** 
     * True if this connected to a router (e.g. future router.limewire.com) or
     * pong-cache server (e.g. gnutellahosts.com).  This may be replaced
     * with a more general priority-based scheme later.
     */
    private boolean _isRouter=false;
    /** True iff this should not be policed by the ConnectionWatchdog, e.g.,
     *  because this is a connection to a Clip2 reflector. */
    private boolean _isKillable=true;

    /** Same as ManagedConnection(host, port, router, manager, false); */
    ManagedConnection(String host,
                      int port,
                      MessageRouter router,
                      ConnectionManager manager) {
        this(host, port, router, manager, false);
    }

    /**
     * Creates an outgoing connection.  The connection is considered a special
     * "router connection" iff isRouter==true.  ManagedConnections should only
     * be constructed within ConnectionManager.  
     */
    ManagedConnection(String host,
                      int port,
                      MessageRouter router,
                      ConnectionManager manager,
                      boolean isRouter) {
        super(host, port, 
            manager.isSupernode() ? 
            (Properties)(new SupernodeProperties(router)) : 
            (Properties)(new ClientProperties(router)),
            manager.isSupernode() ? 
            (HandshakeResponder)(new SupernodeHandshakeResponder(manager)) :
            (HandshakeResponder)(new ClientHandshakeResponder(manager)),
            true);
        
        _router = router;
        _manager = manager;
        _isRouter = isRouter;

        new OutputRunner(); // Start the thread to empty the output queue
    }

    /**
     * Creates an incoming connection.
     * ManagedConnections should only be constructed within ConnectionManager.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the
     *  Gnutella handshake.
     */
    ManagedConnection(Socket socket,
                      MessageRouter router,
                      ConnectionManager manager) {
        super(socket, 
            manager.isSupernode() ? 
            (HandshakeResponder)(new SupernodeHandshakeResponder(manager)) : 
            (HandshakeResponder)(new ClientHandshakeResponder(manager)));
        _router = router;
        _manager = manager;

        new OutputRunner(); // Start the thread to empty the output queue
    }

    /**
     * Override of receive to do ConnectionManager stats and to properly shut
     * down the connection on IOException
     */
    public Message receive() throws IOException, BadPacketException {
        Message m = null;
        try {
            m = super.receive();
            _bytesReceived+=m.getTotalLength();
        } catch(IOException e) {
            _manager.remove(this);
            throw e;
        }
        _numMessagesReceived++;
        _router.countMessage();
        return m;
    }

    /**
     * Override of receive to do MessageRouter stats and to properly shut
     * down the connection on IOException
     */
    public Message receive(int timeout)
            throws IOException, BadPacketException, InterruptedIOException {
        Message m = null;
        try {
            m = super.receive(timeout);
            _bytesReceived+=m.getTotalLength();
        } catch(IOException e) {
            _manager.remove(this);
            throw e;
        }
        _numMessagesReceived++;
        _router.countMessage();
        return m;
    }

    /**
     * Sends a message.  This overrides does extra buffering so that Messages
     * are dropped if the socket gets backed up.  It also does MessageRouter
     * stats.
     * This overrides IS thread safe.  Multiple threads can be in a send call
     * at the same time for a given connection.
     *
     * @requires this is fully constructed
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if the connection
     *  is already closed.  This is thread-safe and guaranteed not to block.
     */
    public void send(Message m) {
        synchronized (_outputQueueLock) {
            _numMessagesSent++;
            _router.countMessage();
            if (_outputQueue.isFull()) {
                //Drop case. Instead of using a FIFO replacement scheme, we
                //use the following:
                //  1) Throw away m if it is (a) a ping request
                //     whose hops count is not zero or (b) a pong.
                //  2) If that doesn't work, throw away the oldest message
                //     message meeting above criteria.
                //  3) If that doesn't work, throw away the oldest message.
                _numSentMessagesDropped++;
                if (isDisposeable(m))
                    return;

                //It's possible to optimize this by keeping track of the
                //last value of i, but case (1) occurs more frequently.
                int i;
                for (i=_outputQueue.getSize()-1; i>=0; i--) {
                    Message mi=(Message)_outputQueue.get(i);
                    if (isDisposeable(mi))
                        break;
                }

                if (i>=0)
                    _outputQueue.set(i,m);
                else
                    _outputQueue.addFirst(m);
            } else {
                //Normal case.
                _outputQueue.addFirst(m);
                if (_outputQueue.getSize() >= BATCH_SIZE)
                    _outputQueueLock.notify();
            }
        }
    }

    /**
     * @requires no other threads are calling send() or flush()
     * @effects block until all queued data is written.  Normally,
     *  there is no need to call this method; the output buffers are
     *  automatically flushed every few seconds (at most).  However, it
     *  may be necessary to call this method in situations where high
     *  latencies are not tolerable, e.g., in the network
     *  discoverer.
     */
    public void flush()
            throws IOException {
        synchronized (_outputQueueLock) {
            _flushImmediately=true;
            _outputQueueLock.notify();
        }
        synchronized (_flushLock) {
            while (! (_outputQueue.isEmpty() && _oldOutputQueue.isEmpty())) {
                try {
                    _flushLock.wait();
                } catch (InterruptedException e) { }
                //Flush is needed in case the wait() returns
                //prematurely.
                super.flush();
            }
        }
    }

    /**
     * This is a hack to avoid a compiler bug.  See the call to this below.
     */
    private void superSend(Message m) throws IOException {
        super.send(m);
        _bytesSent+=m.getTotalLength();
    }

    /**
     * This is a hack to avoid a compiler bug.  See the call to this below.
     */
    private void superFlush() throws IOException {
        super.flush();
    }

    private static boolean isDisposeable(Message m) {
        return  ((m instanceof PingRequest) && (m.getHops()!=0))
          || (m instanceof PingReply);
    }

    /** Repeatedly sends all the queued data every few seconds. */
    private class OutputRunner extends Thread {
        public OutputRunner() {
            setDaemon(true);
            start();
        }

        public void run() {
            while (isOpen()) {
                //1. Wait until (1) the queue is full or (2) the
                //maximum allowable send latency has passed (and the
                //queue is not empty)...
                synchronized (_outputQueueLock) {
                    try {
                        if (!_flushImmediately &&
                                _outputQueue.getSize() < BATCH_SIZE)
                            _outputQueueLock.wait(QUEUE_TIME);
                    } catch (InterruptedException e) {
                    }
                    _flushImmediately=false;
                    if (_outputQueue.isEmpty())
                        continue;
                    //...and swap _outputQueue and _oldOutputQueue.
                    Buffer tmp=_outputQueue;
                    _outputQueue=_oldOutputQueue;
                    _outputQueue.clear();
                    _oldOutputQueue=tmp;
                }

                try {
                    //2. Now send all the data on the old queue.
                    //No need for any locks here since there is only one
                    //OutputRunner thread.
                    while (! _oldOutputQueue.isEmpty())
                        // This should be ManagedConnection.super.flush(), but
                        // the compiler sucks, so we do this
                        superSend((Message)_oldOutputQueue.removeLast());

                    //3. Flush.
                    // This should be ManagedConnection.super.flush(), but the
                    // compiler sucks, so we do this
                    superFlush();
                } catch (IOException e) {
                    _manager.remove(ManagedConnection.this);
                }

                synchronized(_flushLock) {
                    //note that oldOutputQueue.isEmpty()
                    if (_outputQueue.isEmpty())
                        _flushLock.notify();
                }
            }
        }
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

            //call MessageRouter to handle and process the message
            _router.handleMessage(m, this);
            
//            // Increment hops and decrease TTL
//            m.hop();
//
//            if(m instanceof PingRequest)
//                _router.handlePingRequestPossibleDuplicate(
//                    (PingRequest)m, this);
//            else if (m instanceof PingReply)
//                _router.handlePingReply((PingReply)m, this);
//            else if (m instanceof QueryRequest)
//                _router.handleQueryRequestPossibleDuplicate(
//                    (QueryRequest)m, this);
//            else if (m instanceof QueryReply)
//                _router.handleQueryReply((QueryReply)m, this);
//            else if (m instanceof PushRequest)
//                _router.handlePushRequest((PushRequest)m, this);
        }
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

    //
    // Begin Message dropping and filtering calls
    //


    //
    // Begin reply forwarding calls
    //

    /**
     * This method is called when a reply is received for a PingRequest
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection) {
        send(pingReply);
    }

    /**
     * This method is called when a reply is received for a QueryRequest
     * originating on this Connection.  So, send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ManagedConnection receivingConnection) {
        send(queryReply);
    }

    /**
     * This method is called when a PushRequest is received for a QueryReply
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePushRequest(PushRequest pushRequest,
                                  ManagedConnection receivingConnection) {
        send(pushRequest);
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
     *  dropped by this end of the connection.
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
     * @modifies this
     * @effects returns and resets the number of bytes received since the last
     *  call to this method.  This number may exclude bytes read in bad messages
     *  and bytes from TCP/IP protocol headers.
     */
    public synchronized long getBytesReceived() {
        long ret=_bytesReceived;
        _bytesReceived=0;
        return ret;
    }
    
    /**
     * @modifies this
     * @effects returns and resets the number of bytes sent since the last
     *  call to this method.  This number may exclude TCP/IP protocol headers.
     */
    public synchronized long getBytesSent() {
        long ret=_bytesSent;
        _bytesSent=0;
        return ret;
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

        //Have we already seen a ping from this hosts?
        Endpoint host=new Endpoint(pingReply.getIP(), pingReply.getPort());
        if (_pingReplies.size()<MAX_PING_REPLIES && _pingReplies.add(host)) {
            //Nope.  Increment numbers. 
            _nextTotalHorizonFileSize += pingReply.getKbytes();
            _nextNumHorizonFiles += pingReply.getFiles();
            _nextNumHorizonHosts++;           
        }
    }

    /**
     * Updates this' horizon statistics based on the ping replies seen.  
     * This should be called at least every HORIZON_UPDATE_TIME milliseconds,
     * and may safely be called more often.
     *     @modifies this
     */
     public synchronized void refreshHorizonStats() {         
         //Makes sure enough time has elapsed.
         long now=System.currentTimeMillis();
         long elapsed=now-_lastRefreshHorizonTime;        
         if (elapsed<HORIZON_UPDATE_TIME)
             return;
         _lastRefreshHorizonTime=now;
        
         //Ok, now update stats.
         _numHorizonHosts=_nextNumHorizonHosts;
         _numHorizonFiles=_nextNumHorizonFiles;
         _totalHorizonFileSize=_nextTotalHorizonFileSize;

         _nextNumHorizonHosts=0;
         _nextNumHorizonFiles=0;
         _nextTotalHorizonFileSize=0;

         _pingReplies.clear();
         _refreshedHorizonStats=true;
    }

    /** Returns the number of hosts reachable from me. */
    public synchronized long getNumHosts() {
        if (_refreshedHorizonStats)
            return _numHorizonHosts;
        else 
            return _nextNumHorizonHosts;
    }

    /** Returns the number of files reachable from me. */
    public synchronized long getNumFiles() {
        if (_refreshedHorizonStats) 
            return _numHorizonFiles;
        else
            return _nextNumHorizonFiles;
    }

    /** Returns the size of all files reachable from me. */
    public synchronized long getTotalFileSize() {
        if (_refreshedHorizonStats) 
            return _totalHorizonFileSize;
        else
            return _nextTotalHorizonFileSize;
    }

    //
    // End statistics accessors
    //

    /** Returns true if this is as a special "router" connection, e.g. to
     *  router.limewire.com.  */
    public boolean isRouterConnection() {
        return this._isRouter;
    }

    /** Returns true iff this is a connection to a shielded leaf node. 
     *  Note that isClientConnection() ==> !isSupernodeConnection(). */
    public boolean isClientConnection() {
        String value=getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
        if (value==null)
            return false;
        else
            //X-Supernode: true  ==> false
            //X-Supernode: false ==> true
            return !Boolean.valueOf(value).booleanValue();
    }

    /** Returns true iff this is a connection to a supernode.
     *  Note that isSupernodeConnection() ==> !isClientConnection(). */
    public boolean isSupernodeConnection() {
        String value=getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
        if (value==null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();
    }

    /** Returns true iff this is a connection to a supernode.
     *  Note that isSupernodeConnection() ==> !isClientConnection(). */
    public boolean isClientSupernodeConnection() {
        //Is remote host a supernode...
        if (! isSupernodeConnection())
            return false;

        //...and am I a leaf node?
        String value=getPropertyWritten(
            ConnectionHandshakeHeaders.X_SUPERNODE);
        if (value==null)
            return false;
        else
            return !Boolean.valueOf(value).booleanValue();
    }

    /** Returns the system time that we should next forward a query route table
     *  along this connection.  Only valid if isClientSupernodeConnection() is
     *  true. */
    public long getNextQRPForwardTime() {
        return _nextQRPForwardTime;
    }

    /** Sets the system time that we should next forward a query route table
     *  along this connection.  Only valid if isClientSupernodeConnection() is
     *  true. */
    public void setNextQRPForwardTime(long time) {
        _nextQRPForwardTime=time;
    }

    public void setKillable(boolean killable) {
        this._isKillable=killable;
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
     * Returns string representing addresses of other hosts that may
     * be connected thru gnutella.
     * @param properties The properties instance to which to add host addresses
     * @param manager Reference to the connection manager from whom 
     * to retrieve the addressses
     * @return Returns string representing addresses of other hosts that may
     * be connected thru gnutella. Also includes the addresses of the
     * supernodes it is connected to
     * <p> Host address string returned is in the form:
     * <p> IP Address:Port [,IPAddress:Port]* 
     * <p> e.g. 123.4.5.67:6346,234.5.6.78:6347
     */
    private static void addHostAddresses(Properties properties, 
        ConnectionManager manager){
        StringBuffer hostString = new StringBuffer();
        boolean isFirstHost = true;
        //get the connected supernodes and pass them
        for(Iterator iter = manager.getBestHosts(10);iter.hasNext();){
            //get the next endpoint
            Endpoint endpoint =(Endpoint)iter.next();
            //if the first endpoint that we are adding
            if(!isFirstHost){
                //append separator to separate the entries
                hostString.append(Constants.HTTP_ENTRY_SEPARATOR);
            }else{
                //unset the flag
                isFirstHost = false;
            }
            //append the host information
            hostString.append(endpoint.getHostname());
            hostString.append(":");
            hostString.append(endpoint.getPort());
        }
        //set the property
        properties.setProperty(ConnectionHandshakeHeaders.X_TRY, 
            hostString.toString());

        //Also add neighbouring supernodes
        Set connectedSupernodeEndpoints 
            = manager.getConnectedSupernodeEndpoints();
        //if nothing to add, return
        if(connectedSupernodeEndpoints.size() < 0)
            return;
        
        //else add the supernodes
        hostString = new StringBuffer();
        isFirstHost = true;
        for(Iterator iter = connectedSupernodeEndpoints.iterator();
            iter.hasNext();){
            //get the next endpoint
            Endpoint endpoint =(Endpoint)iter.next();
            //if the first endpoint that we are adding
            if(!isFirstHost){
                //append separator to separate the entries
                hostString.append(Constants.HTTP_ENTRY_SEPARATOR);
            }else{
                //unset the flag
                isFirstHost = false;
            }
            //append the host information
            hostString.append(endpoint.getHostname());
            hostString.append(":");
            hostString.append(endpoint.getPort());
        }
        //set the property
        properties.setProperty(ConnectionHandshakeHeaders.X_TRY_SUPERNODES
            , hostString.toString());
    }
    
    private static class LazyProperties extends Properties {
        private MessageRouter router;
        
        LazyProperties(MessageRouter router) {
            this.router=router;
            if (router!=null) {
                setProperty(ConnectionHandshakeHeaders.X_MY_ADDRESS, "");  
                //just temporary!
            }
        }
        
        //We don't define one method in terms of the other since that could
        //cause infinite loops depending on the implementation of the
        //superclass.
        public String getProperty(String key, String defaultValue) {
            if (router!=null && key.equals(
                ConnectionHandshakeHeaders.X_MY_ADDRESS)) {
                Endpoint e=new Endpoint(router.getAddress(), router.getPort());
                return e.getHostname()+":"+e.getPort();
            } else {
                return super.getProperty(key, defaultValue);
            }
        }
        
        public String getProperty(String key) {
            if (router!=null && key.equals(
                ConnectionHandshakeHeaders.X_MY_ADDRESS)) {
                Endpoint e=new Endpoint(router.getAddress(), router.getPort());
                return e.getHostname()+":"+e.getPort();
            } else {
                return super.getProperty(key);
            }
        }
    }

    /** Sets the common properties in props, like Query-Routing and
     *  User-Agent. Typically this method is only called once per connection. */
    private static void addCommonProperties(Properties props) {
        props.setProperty(ConnectionHandshakeHeaders.X_QUERY_ROUTING,
                          "0.1");
        props.setProperty(ConnectionHandshakeHeaders.USER_AGENT,
                          CommonUtils.getVendor());                
    }

    /**
     * Properties for connection handshake, if the node is a supernode
     */
    private static class SupernodeProperties extends LazyProperties{
        
        public SupernodeProperties(MessageRouter router){
            super(router);
            //set supernode property
            setProperty(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
            addCommonProperties(this);
        }
    }
    
    /**
     * Properties for connection handshake, if the node is a client
     */
    private static class ClientProperties extends LazyProperties{
        
        public ClientProperties(MessageRouter router){
            super(router);
            //set supernode property
            setProperty(ConnectionHandshakeHeaders.X_SUPERNODE, "False");
            addCommonProperties(this);
        }
    }

    /**
     * A very simple responder to be used by supernodes during the
     * connection handshake while accepting incoming connections
     */
    private static class SupernodeHandshakeResponder 
        implements HandshakeResponder{
        ConnectionManager _manager;
        
        public SupernodeHandshakeResponder(ConnectionManager manager){
            this._manager = manager;
        }
        
        public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) {
            Properties ret=new Properties();
            
            if(!outgoing) {
                //Incoming connection....
                ret.setProperty(ConnectionHandshakeHeaders.X_SUPERNODE, "True");
                addCommonProperties(ret);
                
                //guide the incoming connection to be a supernode/clientnode
                ret.setProperty(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED,
                    (new Boolean(_manager.supernodeNeeded())).toString());
                
                //give own IP address
                ret.setProperty(ConnectionHandshakeHeaders.X_MY_ADDRESS,
                    _manager.getSelfAddress().getHostname() + ":"
                    + _manager.getSelfAddress().getPort());
                    
                
                //also add some host addresses in the response 
                addHostAddresses(ret, _manager);
            } else {
                //Outgoing connection.  Did the server request we become a leaf?
                String neededS=response.getHeaders().
                    getProperty(ConnectionHandshakeHeaders.X_SUPERNODE_NEEDED);
                if (neededS!=null 
                       && !Boolean.valueOf(neededS).booleanValue()
                       && _manager.allowClientMode()) {
                    //Fine, we'll become a leaf.
                    ret.setProperty(ConnectionHandshakeHeaders.X_SUPERNODE, 
                                    "False");
                }
            }
            return new HandshakeResponse(ret);
        }
    }
    
    /**
     * A very simple responder to be used by client-nodes during the
     * connection handshake while accepting incoming connections
     */
    private static class ClientHandshakeResponder implements HandshakeResponder{
        ConnectionManager _manager;
        
        public ClientHandshakeResponder(ConnectionManager manager){
            this._manager = manager;
        }
        
        public HandshakeResponse respond(HandshakeResponse response, 
            boolean outgoing) throws IOException{
            int code = 200;
            String message = "OK";
            
            //set common properties
            Properties ret=new Properties();
            ret.setProperty(ConnectionHandshakeHeaders.X_SUPERNODE, "False");

            //do stuff specific to connection direction
            if(!outgoing){
                addCommonProperties(ret);

                //client should never accept the connection. Therefore, set the
                //appropriate status
                code = 503;
                message = "I am a shielded client";
                
                //also add some host addresses in the response
                addHostAddresses(ret, _manager);
            }
            
            return new HandshakeResponse(code, message, ret);
        }
    }
    
    /** Returns the query route state associated with this, or null if no
     *  such state. 
     */
    public ManagedConnectionQueryInfo getQueryRouteState() {
        return queryInfo;
    }

    /** Associates the given query route state with this.  Typically this method
     *  is called once per connection. 
     */
    void setQueryRouteState(ManagedConnectionQueryInfo qi) {
        this.queryInfo=qi;
    } 
    
    /** Unit test.  Only tests statistics methods. */
    /*
    public static void main(String args[]) {        
        ManagedConnection mc=new ManagedConnection();
        //For testing.  Make HORIZON_UPDATE_TIME non-final to compile.
        mc.HORIZON_UPDATE_TIME=1*1000;   
        PingReply pr1=new PingReply(GUID.makeGuid(), (byte)3, 6346,
                                    new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
                                    1, 10);
        PingReply pr2=new PingReply(GUID.makeGuid(), (byte)3, 6347,
                                    new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
                                    2, 20);
        PingReply pr3=new PingReply(GUID.makeGuid(), (byte)3, 6346,
                                    new byte[] {(byte)127, (byte)0, (byte)0, (byte)2},
                                    3, 30);

        Assert.that(mc.getNumFiles()==0);
        Assert.that(mc.getNumHosts()==0);
        Assert.that(mc.getTotalFileSize()==0);

        mc.updateHorizonStats(pr1);
        mc.updateHorizonStats(pr1);  //check duplicates
        Assert.that(mc.getNumFiles()==1);
        Assert.that(mc.getNumHosts()==1);
        Assert.that(mc.getTotalFileSize()==10);

        try { Thread.sleep(HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }
            
        mc.refreshHorizonStats();    
        mc.updateHorizonStats(pr1);  //should be ignored for now
        mc.updateHorizonStats(pr2);
        mc.updateHorizonStats(pr3);
        Assert.that(mc.getNumFiles()==1);
        Assert.that(mc.getNumHosts()==1);
        Assert.that(mc.getTotalFileSize()==10);
        mc.refreshHorizonStats();    //should be ignored
        Assert.that(mc.getNumFiles()==1);
        Assert.that(mc.getNumHosts()==1);
        Assert.that(mc.getTotalFileSize()==10);

        try { Thread.sleep(HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }            

        mc.refreshHorizonStats();    //update stats
        Assert.that(mc.getNumFiles()==(1+2+3));
        Assert.that(mc.getNumHosts()==3);
        Assert.that(mc.getTotalFileSize()==(10+20+30));

        try { Thread.sleep(HORIZON_UPDATE_TIME*2); } 
        catch (InterruptedException e) { }       

        mc.refreshHorizonStats();
        Assert.that(mc.getNumFiles()==0);
        Assert.that(mc.getNumHosts()==0);
        Assert.that(mc.getTotalFileSize()==0);                
    }

    // Stub for testing statistics
    private ManagedConnection() {
        super("", 0);
    }
    */
}
