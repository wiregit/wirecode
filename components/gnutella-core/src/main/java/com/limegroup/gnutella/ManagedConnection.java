package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.util.Buffer;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.routing.RouteTableMessage;

/**
 * A Connection managed by a connection managed.  Includes a loopForMessages
 * method that runs forever (or until an IOException occurs), receiving and
 * replying to Gnutella messages.
 *
 * This class implements PingReplyHandler to route PingReplies that originated
 * from it.
 *
 * @author Ron Vogl
 * @author Christopher Rohrs
 */
public class ManagedConnection
        extends Connection
        implements ReplyHandler {
    private MessageRouter _router;
    private ConnectionManager _manager;
    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

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
     * Reference to Message Statistics for this connection.
     * LOCKING: use _outputQueueLock for locking since only when writing
     * to the output queue, we increment the number of messages sent
     * or number of messages sent (that were dropped also).
     */ 
    private ManagedConnectionMessageStats _myMessageStats = 
        new ManagedConnectionMessageStats(this);


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

    /** 
     * indicates whether the connection is to an "old Gnutella client" 
     * (according to the the Protocol Version number in any message GUID).
     */
    private boolean _isOldClient = false;

    /**
     * First Ping is used to determine if this is a connection to an older
     * client or not.  Then, we can set the accept time for allowing Pings
     * from this older client.
     */
    private boolean _receivedFirstPing = false;

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
        super(host, port);
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
        super(socket);
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
        _myMessageStats.countReceivedMessage();
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
        _myMessageStats.countReceivedMessage();
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
            _myMessageStats.countSentMessage();
            _router.countMessage();
            if (_outputQueue.isFull()) {
                //Drop case. Instead of using a FIFO replacement scheme, we
                //use the following:
                //  1) Throw away m if it is (a) a ping request
                //     whose hops count is not zero or (b) a pong.
                //  2) If that doesn't work, throw away the oldest message
                //     message meeting above criteria.
                //  3) If that doesn't work, throw away the oldest message.
                _myMessageStats.countSentMessageDropped();
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
                _myMessageStats.countDroppedMessage();
                continue;
            }

            //if crawler ping, send back pongs of neighbors.
            if ((m instanceof PingRequest) && (isCrawlerPing(m))) {
                _router.sendCrawlerPingReplies(m.getGUID(),this);
                continue;
            }

            // Increment hops and decrease TTL
            m.hop();

            if(m instanceof PingRequest) {
                //first ping should be checked if it is an older client (by
                //Gnutella protocol version in GUID).
                if (!_receivedFirstPing) 
                    checkForOlderClient(m); 
                if (!isHandshake(m)) //if handshake, just continue;
                    _router.handlePingRequest((PingRequest)m, this);
            }
            else if (m instanceof PingReply) {
                _router.handlePingReply((PingReply)m, this);
            }
            else if (m instanceof QueryRequest)
                _router.handleQueryRequestPossibleDuplicate(
                    (QueryRequest)m, this);
            else if (m instanceof QueryReply)
                _router.handleQueryReply((QueryReply)m, this);
            else if (m instanceof PushRequest)
                _router.handlePushRequest((PushRequest)m, this);
            else if (m instanceof RouteTableMessage)
                _router.handleRouteTableMessage((RouteTableMessage)m, this);
        }
    }

    /**
     * Determines if this connection is to an older client by checking the
     * Protocol Version (of the GUID of the Message) and sets necessary
     * flags.
     */
    private void checkForOlderClient(Message m)
    {
        _receivedFirstPing = true;

        //if the protocol version is less than 1, it's an older client.
        if (GUID.getProtocolVersion(m.getGUID()) < 
            GUID.GNUTELLA_VERSION_06) 
            _isOldClient = true;
    }

    /**
     * Returns whether the Ping received was from a GNUTELLA crawler, by 
     * looking at the TTL and hops count.
     */
    private boolean isCrawlerPing(Message m) {
        int ttl = (int)m.getTTL();
        int hops = (int)m.getHops();

        if ((ttl == 2) && (hops == 0))
            return true;
        else
            return false;
    }

    /**
     * Returns whether the Ping received was a handshake ping by looking at the
     * ttl and hops count.  (ttl should be 0 and hops should be 1) since we 
     * should be calling this method after calling hop on the messsage.
     *
     * @required - m.hop() has been called
     */
    private boolean isHandshake(Message m) {
        int ttl = (int)m.getTTL(); 
        int hops = (int)m.getHops();

        if ((ttl == 0) && (hops == 1))
            return true;
        else
            return false;
    }

    /**
     * A callback for the ConnectionManager to inform this connection that a
     * message was dropped.  This happens when a reply received from this
     * connection has no routing path or a PingRequest or PingReply was throttled
     * since it was from an older client.
     */
    public void countDroppedMessage() {
        _myMessageStats.countDroppedMessage();
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
    // Begin statistics accessors via ManagedConnectionMessageStats instance.
    //

    /** Returns the number of messages sent on this connection */
    public int getNumMessagesSent() {
        return _myMessageStats.getNumMessagesSent();
    }

    /** Returns the number of messages received on this connection */
    public int getNumMessagesReceived() {
        return _myMessageStats.getNumMessagesReceived();
    }

    /** Returns the number of messages I dropped while trying to send
     *  on this connection.  This happens when the remote host cannot
     *  keep up with me. */
    public int getNumSentMessagesDropped() {
        return _myMessageStats.getNumSentMessagesDropped();
    }

    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them or needed to throttle
     * them since they were an older client.
     */
    public long getNumReceivedMessagesDropped() {
        return _myMessageStats.getNumReceivedMessagesDropped();
    }

    /**
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.  Actual work is done by 
     *  ManagedConnectionMessageStats.
     */
    public synchronized float getPercentReceivedDropped() {
        return _myMessageStats.getPercentReceivedDropped();
    }

    /**
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were
     *  dropped by this end of the connection.  Actual work is done by
     *  ManagedConnectionMessageStats.
     */
    public synchronized float getPercentSentDropped() {
        return _myMessageStats.getPercentSentDropped();
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

    //
    // End statistics accessors
    //

    /** Returns true if this is as a special "router" connection, e.g. to
     *  router.limewire.com.  */
    public boolean isRouterConnection() {
        return this._isRouter;
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
     * Is this an older client (by protocol version number in GUID)
     */
    public boolean isOldClient() {
        return _isOldClient;
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



