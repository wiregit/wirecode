package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.connection.*;

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
 * of bytes read and written.  ManagedConnection doesn't quite fit the
 * BandwidthTracker interface, unfortunately.  On the query-routing3-branch and
 * pong-caching CVS branches, these statistics have been bundled into a single
 * object, reducing the complexity of ManagedConnection.  We will likely merge
 * this change into ManagedConnection in the future, so please bear with this
 * for now.<p>
 * 
 * This class implements ReplyHandler to route pongs and query replies that
 * originated from it.<p> 
 */
public class ManagedConnection
        extends Connection
        implements ReplyHandler {
    /** The timeout to use when connecting, in milliseconds.  This is NOT used
     *  for bootstrap servers.  */
    private static final int CONNECT_TIMEOUT=4000;  //4 seconds
    /** The total amount of upstream messaging bandwidth for ALL connections
     *  in BYTES (not bits) per second. */
    private static final int TOTAL_OUTGOING_MESSAGING_BANDWIDTH=15000;

    private MessageRouter _router;
    private ConnectionManager _manager;

    private volatile SpamFilter _routeFilter = SpamFilter.newRouteFilter();
    private volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();

    /** Priority queue of messages to send. */
    CompositeQueue _queue=new CompositeQueue();

    /** Limits outgoing bandwidth for ALL connections. */
    private final static BandwidthThrottle _throttle=
        new BandwidthThrottle(TOTAL_OUTGOING_MESSAGING_BANDWIDTH);


    /**
     * The amount of time to wait for a handshake ping in reject connections, in
     * milliseconds.     
     */
    private static final int REJECT_TIMEOUT=500;  //0.5 sec


    /**
     * The number of messages sent.  This includes messages that are eventually
     * dropped.  This stat is synchronized by this.
     */
    private int _numMessagesSent;
    /**
     * The number of messages received.  This includes messages that are
     * eventually dropped.  This stat is synchronized by this.
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
    static long HORIZON_UPDATE_TIME=10*60*1000; //10 minutes
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


    /** The total number of bytes sent/received.
     *  These are not synchronized and not guaranteed to be 100% accurate. */
    private volatile long _bytesSent;
    private BandwidthTrackerImpl _upBandwidthTracker=
        new BandwidthTrackerImpl();
    private volatile long _bytesReceived;
    private BandwidthTrackerImpl _downBandwidthTracker=
        new BandwidthTrackerImpl();

    /** 
     * True if this connected to a router (e.g. future router.limewire.com) or
     * pong-cache server (e.g. gnutellahosts.com).  This may be replaced
     * with a more general priority-based scheme later.
     */
    private boolean _isRouter=false;
    /** True iff this should not be policed by the ConnectionWatchdog, e.g.,
     *  because this is a connection to a Clip2 reflector. */
    private boolean _isKillable=true;
    
    /** if I am a supernode shielding the given connection */
    private Boolean _isSupernodeClientConnection=null;
    /** if I am a leaf connected to a supernode  */
    private Boolean _isClientSupernodeConnection=null;
    /** is the GGEP header set?  (null if not yet known) */
    private Boolean _supportsGGEP=null;

    /**
     * The domain to which this connection is authenticated
     */
    private Set _domains = null;
    
    /**
	 * Constant handle to the <tt>SettingsManager</tt> for accessing
	 * various properties.
	 */
	private final SettingsManager SETTINGS = SettingsManager.instance();
    
    /** 
     * Creates an outgoing connection. 
     *
     * @param host the address to connect to in symbolic or dotted-quad format
     *  If host names a special bootstrap server, e.g., "router.limewire.com",
     *  this may take special action, like trying "router4.limewire.com" instead
     *  with a 0.4 handshake.
     * @param port the port to connect to
     * @param router where to report messages
     * @param where to report my death.  Also used for reject connections.
     */
    ManagedConnection(String host,
                      int port,
                      MessageRouter router,
                      ConnectionManager manager) {
        this(translateHost(host), port, router, manager, isRouter(host));
    }

    /**
     * Creates an outgoing connection.  The connection is considered a special
     * LimeWire router connection iff isRouter==true.  In this case host should
     * already be translated.  This constructor exists only for the convenience
     * of implementation.
     */
    private ManagedConnection(String host,
                              int port,
                              MessageRouter router,
                              ConnectionManager manager,
                              boolean isRouter) {
        //If a router connection, connect as 0.4 by setting responders to null.
        //(Yes, it's a hack, but this is how Connection(String,int) is
        //implemented.)  Otherwise connect at 0.6 with re-negotiation, setting
        //the headers according to whether we're supernode capable.
        super(host, port, 
              isRouter ? 
                  null :
                  (manager.isSupernode() ? 
                      (Properties)(new SupernodeProperties(router, host)) : 
                      (Properties)(new ClientProperties(router, host))),
              isRouter ? 
                  null : 
                  (manager.isSupernode() ?
                      (HandshakeResponder)
                      (new SupernodeHandshakeResponder(manager, router, host)) :
                      (HandshakeResponder)
                      (new ClientHandshakeResponder(manager, router, host))),
              !isRouter);
        
        _router = router;
        _manager = manager;
        _isRouter = isRouter;
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
            (HandshakeResponder)(new SupernodeHandshakeResponder(manager,
                router, socket.getInetAddress().getHostAddress())) : 
            (HandshakeResponder)(new ClientHandshakeResponder(manager,
                router, socket.getInetAddress().getHostAddress())));
        _router = router;
        _manager = manager;
    }

    public void initialize(ConnectionListener listener)
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        //Establish the socket (if needed), handshake.
        StatsConnectionListener listener2=
            new StatsConnectionListener(listener);
        if (_isRouter)
            super.initialize(listener2);   //no timeout for bootstrap server
        else
            super.initialize(listener2, CONNECT_TIMEOUT);
    }

    /** 
     * Adds hooks to maintain read statistics.
     */
    private class StatsConnectionListener extends DelegateConnectionListener {
        StatsConnectionListener(ConnectionListener delegate) {
            super(delegate);
        }
        public void read(Connection c, Message m) { 
            synchronized (this) {
                _bytesReceived+=m.getTotalLength();
                _numMessagesReceived++;
                _router.countMessage();  //TODO: move to handleMessage
            }
            delegate.read(c, m);
        }
    }


    ////////////////////// Sending, Outgoing Flow Control //////////////////////


    /** 
     * Like Connection.write(m), but may queue and write more than one message. 
     */
    public boolean write(Message m) {
        if (! supportsGGEP())
            m=m.stripExtendedPayload();

        synchronized (this) {
            _router.countMessage();   //why count messages snet?
            _numMessagesSent++;
            _queue.add(m);
            _numSentMessagesDropped+=_queue.resetDropped();
            //Attempt to write queued data, not necessarily m.  This calls
            //super.write(m'), which may call listener.needsWrite().
            return this.write();   
        }
    }

    /**
     * Like Connection.write(), but may write more than one message.
     * May also call listener.needsWrite.
     */
    public synchronized boolean write() {
        //Terminate when either
        //a) super cannot send any more data because its send
        //   buffer is full OR
        //b) neither super nor this have any queued data
        //c) the socket is closed
        while (isOpen()) {
            //Add more queued data to super if possible.
            if (! super.hasQueued()) {
                Message m=_queue.removeNext();
                if (m==null)
                    return false;    //Nothing left to send (a)
                _numSentMessagesDropped+=_queue.resetDropped();
                _bytesSent+=m.getTotalLength();
                boolean hasUnsentData=super.write(m);
                if (hasUnsentData)
                    return true;     //needs another write (b)
            }     
            //Otherwise write data from super.  Abort if this didn't complete.
            else {
                boolean hasUnsentData=super.write();
                if (hasUnsentData)
                    return true;     //needs another write (b)
            }
        }
        return false;                //socket closed (c)
    }

    protected boolean hasQueued() {
        return _queue.size()>0;
    }


    //////////////////////////////////////////////////////////////////////////

//      /**
//       * Implements the reject connection mechanism.  Loops until receiving a
//       * handshake ping, responds with the best N pongs, and closes the
//       * connection.  Closes the connection if no ping is received within a
//       * reasonable amount of time.  Does NOT clean up route tables in the case
//       * of an IOException.
//       */
//      void loopToReject(HostCatcher catcher) {
//          //IMPORTANT: note that we do not use this' send or receive methods.
//          //This is an important optimization to prevent calling
//          //RouteTable.removeReplyHandler when the connection is closed.
//          //Unfortunately it still can be triggered by the
//          //OutputRunnerThread. TODO: can we avoid creating the OutputRunner
//          //thread in this case?

//          try {
//          //The first message we get from the remote host should be its initial
//          //ping.  However, some clients may start forwarding packets on the
//          //connection before they send the ping.  Hence the following loop.  The
//          //limit of 10 iterations guarantees that this method will not run for
//          //more than TIMEOUT*10=80 seconds.  Thankfully this happens rarely.
//          for (int i=0; i<10; i++) {
//              Message m=null;
//              try {                
//                  m=super.receive(REJECT_TIMEOUT);
//                  if (m==null)
//                      return; //Timeout has occured and we havent received the ping,
//                              //so just return
//              }// end of try for BadPacketEception from socket
//              catch (BadPacketException e) {
//                  return; //Its a bad packet, just return
//              }
//              if((m instanceof PingRequest) && (m.getHops()==0)) {
//                  // this is the only kind of message we will deal with
//                  // in Reject Connection
//                  // If any other kind of message comes in we drop
              
//                  //SPECIAL CASE: for Crawle ping
//                  if(m.getTTL() == 2) {
//                      handleCrawlerPing((PingRequest)m);
//                      return;
//                  }

//                  Iterator iter = catcher.getNormalHosts(10);
//                   // we are going to send rejected host the top ten
//                   // connections
//                  while(iter.hasNext()) {
//                      Endpoint bestEndPoint =(Endpoint)iter.next();
//                      // make a pong with this host info
//                      PingReply pr = new PingReply(m.getGUID(),(byte)1,
//                          bestEndPoint.getPort(),
//                          bestEndPoint.getHostBytes(), 0, 0);
//                      // the ttl is 1; and for now the number of files
//                      // and kbytes is set to 0 until chris stores more
//                      // state in the hostcatcher
//                      super.send(pr);
//                  }
//                  super.flush();
//                  return;
//              }// end of (if m is PingRequest)
//          } // End of while(true)
//          } catch (IOException e) {
//          } finally {
//              close();
//          }
//      }

//      /**
//       * Handles the crawler ping of Hops=0 & TTL=2, by sending pongs 
//       * corresponding to all its neighbors
//       * @param m The ping request received
//       * @exception In case any I/O error occurs while writing Pongs over the
//       * connection
//       */
//      private void handleCrawlerPing(PingRequest m) throws IOException {
//          //IMPORTANT: note that we do not use this' send or receive methods.
//          //This is an important optimization to prevent calling
//          //RouteTable.removeReplyHandler when the connection is closed.
//          //Unfortunately it still can be triggered by the
//          //OutputRunnerThread. TODO: can we avoid creating the OutputRunner
//          //thread in this case?

//          //send the pongs for the Ultrapeer & 0.4 connections
//          List /*<ManagedConnection>*/ nonLeafConnections 
//              = _manager.getInitializedConnections2();
        
//          supersendNeighborPongs(m, nonLeafConnections);
        
//          //send the pongs for leaves
//          List /*<ManagedConnection>*/ leafConnections 
//              = _manager.getInitializedClientConnections2();
//          supersendNeighborPongs(m, leafConnections);
        
//          //Note that sending its own pong is not necessary, as the crawler has
//          //already connected to this node, and is not sent therefore. 
//          //May be sent for completeness though
//      }
    
//      /**
//       * Uses the super class's send message to send the pongs corresponding 
//       * to the list of connections passed.
//       * This prevents calling RouteTable.removeReplyHandler when 
//       * the connection is closed.
//       * @param m Th epingrequest received that needs Pongs
//       * @param neigbors List (of ManagedConnection) of  neighboring connections
//       * @exception In case any I/O error occurs while writing Pongs over the
//       * connection
//       */
//      private void supersendNeighborPongs(PingRequest m, List neighbors) 
//          throws IOException {
//          for(Iterator iterator = neighbors.iterator();
//              iterator.hasNext();) {
//              //get the next connection
//              ManagedConnection connection = (ManagedConnection)iterator.next();
            
//              //create the pong for this connection
//              //mark the pong if supernode
//              PingReply pr;
//              if(connection.isSupernodeConnection()) {
//                  pr = new PingReply(m.getGUID(),(byte)2,
//                  connection.getOrigPort(),
//                  connection.getInetAddress().getAddress(), 0, 0, true);  
//              } else if(connection.isClientConnection() 
//                  || connection.isOutgoing()){
//                  //we know the listening port of the host in this case
//                  pr = new PingReply(m.getGUID(),(byte)2,
//                  connection.getOrigPort(),
//                  connection.getInetAddress().getAddress(), 0, 0); 
//              }
//              else{
//                  //Use the port '0' in this case, as we dont know the listening
//                  //port of the host
//                  pr = new PingReply(m.getGUID(),(byte)2,
//                  0,
//                  connection.getInetAddress().getAddress(), 0, 0); 
//              }
            
//              //hop the message, as it is ideally coming from the connected host
//              pr.hop();
            
//              //send the message
//              super.send(pr);
//          }
        
//          super.flush();
//      }
    
    /**
     * Handles a read of message <tt>m</tt> from <tt>this</tt>.
     */
    public void handleRead(Message m) {
        // Run through the route spam filter and drop accordingly.
        if (!_routeFilter.allow(m)) {
            _router.countFilteredMessage();
            _numReceivedMessagesDropped++;
            return;
        }

        //Call MessageRouter to handle and process the message
        //TODO: reject connection handles differently
        _router.handleMessage(m, this);         
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
    
    /**
     * Returns the domain to which this connection is authenticated
     * @return the set (of String) of domains to which this connection 
     * is authenticated. Returns
     * null, in case of unauthenticated connection
     */
    public Set getDomains(){
        //Note that this method is not synchronized, and so _domains may 
        //get initialized multiple times (in case multiple threads invoke this
        //method, before domains is initialized). But thats not a problem as
        //all the instances will have same values, and all but 1 of them 
        //will get garbage collected
        
        if(_domains == null){
            //initialize domains
            _domains = createDomainSet();
        }
        //return the initialized domains
        return _domains;
//        return (String[])_domains.toArray(new String[0]);
    }

    /**
     * creates the set (of String) of domains from the properties sent/received
     * @return the set (of String) of domains
     */
    private Set createDomainSet(){
        Set domainSet;
        //get the domain property
        //In case of outgoing connection, we received the domains from the
        //remote host to whom we authenticated, viceversa for incoming
        //connection
        String domainsAuthenticated;
        if(this.isOutgoing())
            domainsAuthenticated = getProperty(
                ConnectionHandshakeHeaders.X_DOMAINS_AUTHENTICATED);
        else
            domainsAuthenticated = getPropertyWritten(
                ConnectionHandshakeHeaders.X_DOMAINS_AUTHENTICATED);

        //for unauthenticated connections
        if(domainsAuthenticated == null){
            //if no authentication done, initialize to a default domain set
            domainSet = User.createDefaultDomainSet();
        }else{
            domainSet = StringUtils.getSetofValues(domainsAuthenticated);
        }
        
        //return the domain set
        return domainSet;
    }
    
    /**
     * This method is called when a reply is received for a PingRequest
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection) {
        write(pingReply);
    }

    /**
     * This method is called when a reply is received for a QueryRequest
     * originating on this Connection.  So, send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ManagedConnection receivingConnection) {
        write(queryReply);
    }

    /**
     * This method is called when a PushRequest is received for a QueryReply
     * originating on this Connection.  So, just send it back.
     * If modifying this method, note that receivingConnection may
     * by null.
     */
    public void handlePushRequest(PushRequest pushRequest,
                                  ManagedConnection receivingConnection) {
        write(pushRequest);
    }

    //
    // End reply forwarding calls
    //


    //
    // Begin statistics accessors
    //

    /** Returns the number of bytes sent on this connection. */
    public long getBytesSent() {
        return _bytesSent;
    }
    
    /** Returns the number of bytes received on this connection. */
    public long getBytesReceived() {
        return _bytesReceived;
    }

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
     *  dropped by this end of the connection.  This value may be
     *  greater than 100%, e.g., if only one message is sent but
     *  four are dropped during a given time period.
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
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public void measureBandwidth() {
        _upBandwidthTracker.measureBandwidth(
             ByteOrder.long2int(_bytesSent));
        _downBandwidthTracker.measureBandwidth(
             ByteOrder.long2int(_bytesReceived));
    }

    /**
     * Returns the upstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredUpstreamBandwidth() {
        float retValue = 0; //initialize to default
        try {
            retValue = _upBandwidthTracker.getMeasuredBandwidth();
        } catch(InsufficientDataException ide) {
            return 0;
        }
        return retValue;
    }

    /**
     * Returns the downstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredDownstreamBandwidth() {
        float retValue = 0;
        try {
            retValue = _downBandwidthTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            return 0;
        }
        return retValue;
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

    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
        return getProperty(
            com.limegroup.gnutella.handshaking.
                ConnectionHandshakeHeaders.USER_AGENT);
    }

    /** Returns true if this is as a special "router" connection, e.g. to
     *  router.limewire.com.  */
    public boolean isRouterConnection() {
        return this._isRouter;
    }

    /** Returns true iff this connection wrote "Supernode: false".
     *  This does NOT necessarily mean the connection is shielded. */
    public boolean isClientConnection() {
        String value=getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
        if (value==null)
            return false;
        else
            //X-Supernode: true  ==> false
            //X-Supernode: false ==> true
            return !Boolean.valueOf(value).booleanValue();
    }

    /** Returns true iff this connection wrote "Supernode: true". */
    public boolean isSupernodeConnection() {
        String value=getProperty(ConnectionHandshakeHeaders.X_SUPERNODE);
        if (value==null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();
    }

    /** Returns true iff the connection is a supernode and I am a leaf, i.e., if
     *  I wrote "Supernode: false", this connection wrote "Supernode: true" (not
     *  necessarily in that order).  <b>Does NOT require that QRP is enabled</b>
     *  between the two; the supernode could be using reflector indexing, for
     *  example. */
    public boolean isClientSupernodeConnection() {
        if(_isClientSupernodeConnection == null) {
            _isClientSupernodeConnection = 
                new Boolean(isClientSupernodeConnection2());
        }
        return _isClientSupernodeConnection.booleanValue();
    }

    private boolean isClientSupernodeConnection2() {
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

    /** Returns true iff this connection is a temporary connection as per
     the headers. */
    public boolean isTempConnection() {
        //get the X-Temp-Connection from either the headers received
        String value=getProperty(ConnectionHandshakeHeaders.X_TEMP_CONNECTION);
        //if X-Temp-Connection header is not received, return false, else
        //return the value received
        if(value == null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();
    }
    
    /** Returns true iff I am a supernode shielding the given connection, i.e.,
     *  if I wrote "Supernode: true" and this connection wrote "Supernode:
     *  false, and <b>both support query routing</b>. */
    public boolean isSupernodeClientConnection() {
        if(_isSupernodeClientConnection == null) {
            _isSupernodeClientConnection = 
                new Boolean(isSupernodeClientConnection2());
        }
        return _isSupernodeClientConnection.booleanValue();
    }
    
    /** Returns true iff I am a supernode shielding the given connection, i.e.,
     *  if I wrote "Supernode: true" and this connection wrote "Supernode:
     *  false, and <b>both support query routing</b>. */
    private boolean isSupernodeClientConnection2() {
        //Is remote host a supernode...
        if (! isClientConnection())
            return false;

        //...and am I a supernode?
        String value=getPropertyWritten(
            ConnectionHandshakeHeaders.X_SUPERNODE);
        if (value==null)
            return false;
        else if (!Boolean.valueOf(value).booleanValue())
            return false;

        //...and do both support QRP?
        return isQueryRoutingEnabled();
    }

    /** Returns true if this supports GGEP'ed messages.  GGEP'ed messages (e.g.,
     *  big pongs) should only be sent along connections for which
     *  supportsGGEP()==true. */
    public boolean supportsGGEP() {
        if (_supportsGGEP==null)
            _supportsGGEP=new Boolean(supportsGGEP2());
        return _supportsGGEP.booleanValue();
    }

    private boolean supportsGGEP2() {
        String value=getProperty(ConnectionHandshakeHeaders.GGEP);
        //Currently we don't care about the version number.
        return value!=null;
    }

    /** True if the remote host supports query routing (QRP).  This is only 
     *  meaningful in the context of leaf-supernode relationships. */
    boolean isQueryRoutingEnabled() {
        //We are ALWAYS QRP-enabled, so we only need to look at what the remote
        //host wrote.
        String value=getProperty(ConnectionHandshakeHeaders.X_QUERY_ROUTING);
        if (value==null)
            return false;
        try {            
            Float f=new Float(value);
            return f.floatValue() >= 0.1f;   //TODO: factor into constant!
        } catch (NumberFormatException e) {
            return false;
        }
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

    /** Maps router.limewire.com to router4.limewire.com. 
     *  Package-access for testing purposes only. */
    static String translateHost(String hostname) {
        if (hostname.equals(SettingsManager.DEFAULT_LIMEWIRE_ROUTER))
            return SettingsManager.DEDICATED_LIMEWIRE_ROUTER;
        else
            return hostname;
    }

    /** Returns true iff hostname is any of the routerX.limewire.com's (possibly
     *  in dotted-quad form). */
    public static boolean isRouter(String hostname) {
        //Taken from the old ConnectionManager.createRouterConnection method.
        if (hostname.startsWith("router") && hostname.endsWith("limewire.com"))
            return true;
        //Take from the old HostCatcher.isRouter method.  
        //Check for 64.61.25.139-143 and 64.61.25.171
        if (hostname.startsWith("64.61.25") 
            && (hostname.endsWith("171")
                || hostname.endsWith("139")
                || hostname.endsWith("140")
                || hostname.endsWith("141")
                || hostname.endsWith("142")
                || hostname.endsWith("143"))) 
            return true;
        return false;
    }
    
    /***************************************************************************
     * UNIT TESTS: tests/com/limegroup/gnutella/ManagedConnectionTest
     **************************************************************************/
}
