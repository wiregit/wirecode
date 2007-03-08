
// Edited for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// LimeWire doesn't use GUESS anymore
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.guess.QueryKey;

// Standard Gnutella packets and LimeWire-specific vendor messages
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;

// Import classes related to QRT, the hash mask of what a computer is sharing
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;

import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.search.ResultCounter;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.StatisticsSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.statistics.RouteErrorStat;
import com.limegroup.gnutella.statistics.RoutedQueryStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.udpconnect.UDPConnectionMessage;
import com.limegroup.gnutella.udpconnect.UDPMultiplexor;
import com.limegroup.gnutella.upelection.PromotionManager;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.FixedsizeHashMap;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.NoMoreStorageException;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.version.UpdateHandler;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
public abstract class MessageRouter {

    /** A debugging log we can write lines of text to as the program runs. */
    private static final Log LOG = LogFactory.getLog(MessageRouter.class);

    /**
     * Access the ConnectionManager object that keeps the list of our Gnutella connections.
     * These are TCP socket Gnutella connections that began with the Gnutella handshake.
     * Each connection is represented by a ManagedConnection object.
     */
    protected static ConnectionManager _manager;

    /**
     * 15, if we get a query message from an old Gnutella program, only forward it to 15 ultrapeers, not all of them.
     * 
     * handleQueryRequest() calls forwardLimitedQueryToUltrapeers() when it gets a query message from a Gnutella program that doesn't support advanced features.
     * forwardLimitedQueryToUltrapeers() only forwards the query to 15 ultrapeers, and tries to find 15 old ones.
     * This is different than forwardQueryToUltrapeers(), which forwards a query from a modern Gnutella program to all our ultrapeers.
     * 
     * Constant for the number of old connections to use when forwarding
     * traffic from old connections.
     */
    private static final int OLD_CONNECTIONS_TO_USE = 15;

    /**
     * Our client ID GUID that uniquely identifies us on the Gnutella network.
     * We chose it when LimeWire first ran, and keep it in ApplicationSettings.CLIENT_ID, which is the CLIENT_ID line in limewire.props.
     * 
     * We'll include our client ID GUID at the end of the query hit packets we send so downloaders can get a push request to us.
     */
    protected byte[] _clientGUID;

	/**
     * The ForMeReplyHandler object that represents us in a RouteTable, and lets us get responses to messages we create and send.
     * 
     * There are 3 classes in LimeWire that implement the ReplyHandler interface.
     * They are ManagedConnection, UDPReplyHandler, and ForMeReplyHandler.
     * ManagedConnection and UDPReplyHandler objects represent remote computers, and the ForMeReplyHandler represents us.
     * 
     * These objects implement ReplyHandler so we can put them under a GUID in a RouteTable.
     * Before broadcasting a request packet forward, we'll put its message GUID and the ReplyHandler that sent it to us in a RouteTable.
     * When we get a response packet, we'll look up its GUID in the RouteTable, and send it back the right way.
     * 
     * When we create a message and send it ourselves, we put the ForMeReplyHandler in the RouteTable.
     * That way, when the response with the same GUID comes back, we'll know it's for us.
     * 
     * There is only one ForMeReplyHandler object as the program runs.
     * This line of code gets a reference to it, and saves it as FOR_ME_REPLY_HANDLER.
	 */
    private final ReplyHandler FOR_ME_REPLY_HANDLER = ForMeReplyHandler.instance();

    /**
     * 50000, a map in a RouteTable can hold 50 thousand GUIDs.
     * 
     * A RouteTable object has 2 maps, the new map and the old map.
     * Each map remembers 50 thousand GUIDs, so the RouteTable holds a total of 100 thousand.
     * 
     * A RouteTable map shouldn't ever hit this maximum.
     * It shifts the GUIDs from the new map to the old map and then throws them away on a timed interval.
     * The timed shifting will keep the contents low, not this high maximum.
     */
    private int MAX_ROUTE_TABLE_SIZE = 50000;

    //do

    /**
     * 150
     * The maximum number of bypassed results to remember per query.
     */
    private final int MAX_BYPASSED_RESULTS = 150;

    //done

    /**
     * A RouteTable that maps ping messge GUIDs to the connection that sent the ping, and will want pongs with the same GUID.
     * 
     * When a ReplyHandler like a ManagedConnection sends us a ping, we'll add it under the ping's message GUID in _pingRouteTable.
     * Later, we may get a pong with the same message GUID.
     * We'll look up the GUID in this RouteTable and know which ReplyHandler to send it back to.
     * 
     * LimeWire no longer broadcasts pings forward, and having a route table for pings and pongs is no longer necessary.
     * When we get a ping, we'll reply with a pong about us and 6 pongs from the PongCacher.
     * We still add the ping's GUID to _pingRouteTable, but since we don't send it onward, we'll never get a pong back with its GUID.
     * 
     * _pingRouteTable is important for the pings we send out.
     * When we ping our connections, we put the ping's GUID in _pingRouteTable with the ForMeReplyHandler object.
     * Then, when we get a pong back that's in response to our ping, the ForMeReplyHandler gets it and processes it.
     * 
     * Every 2 minutes, the ping route table will discard the old map and shift the new map contents there, holding a GUID for between 2 and 4 minutes.
     * Each map can hold 50 thousand GUID mappings for a total maximum of 100 thousand.
     */
    private RouteTable _pingRouteTable = new RouteTable(2 * 60, MAX_ROUTE_TABLE_SIZE); // 2 minutes, 50 thousand entries

    /**
     * A RouteTable that maps query message GUIDs to the connection that sent the query, and will want query hits with the same GUID.
     * 
     * When a ReplyHandler like a ManagedConnection sends us a query, we'll add it under the query's message GUID in _queryRouteTable.
     * Later, we may get a query hit with the same message GUID.
     * We'll look up the GUID in this RouteTable and know which ReplyHandler to send it back to.
     * 
     * Every 5 minutes, the ping route table will discard the old map and shift the new map contents there, holding a GUID for between 5 and 10 minutes.
     * Each map can hold 50 thousand GUID mappings for a total maximum of 100 thousand.
     */
    private RouteTable _queryRouteTable = new RouteTable(5 * 60, MAX_ROUTE_TABLE_SIZE); // 5 minutes, 50 thousand entries

    //do

    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers.
     * 
     * Every 7 minutes, the route table will discard the old map and shift the new map contents there, holding a GUID for between 7 and 14 minutes
     * Each map can hold 50 thousand GUID mappings for a total maximum of 100 thousand.
     */
    private RouteTable _pushRouteTable = new RouteTable(7 * 60, MAX_ROUTE_TABLE_SIZE); // 7 minutes, 50 thousand entries

    /**
     * Maps HeadPong guids to the originating pingers.  Short-lived since
     * we expect replies from our leaves quickly.
     * 
     * Every 10 seconds, the route table will discard the old map and shift the new map contents there, holding a GUID for between 10 and 20 seconds
     * Each map can hold 50 thousand GUID mappings for a total maximum of 100 thousand.
     */
    private RouteTable _headPongRouteTable = new RouteTable(10, MAX_ROUTE_TABLE_SIZE); // 10 seconds, 50 thousand entries

    /**
     * 30000, 30 seconds in milliseconds.
     * Every 30 seconds, 
     *  
     *  How long to buffer up out-of-band replies.
     */
    private static final long CLEAR_TIME = 30 * 1000; // 30 seconds

    /** Time between sending HopsFlow messages.
     */
    private static final long HOPS_FLOW_INTERVAL = 15 * 1000; // 15 seconds

    /**
     * 250
     * The maximum number of UDP replies to buffer up.  Non-final for 
     * testing.
     */
    static int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps track of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryResponseBundle.
     * 
     * The _outOfBandReplies Hashtable looks like this:
     * 
     * Key                                    Value
     * ---                                    -----
     * TimedGUID                              QueryResponseBundle
     *  ._guid     The query's message GUID    ._query             The query packet
     *  .MAX_LIFE  25 seconds                  ._responses         An array of Response objects representing files we're sharing that match
     */
    private final Map _outOfBandReplies = new Hashtable();

    /**
     * Keeps track of potential sources of content.  Comprised of Sets of GUESS
     * Endpoints.  Kept tidy when searches/downloads are killed.
     */
    private final Map _bypassedResults = new HashMap();

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap _udpConnectBacks = new FixedsizeHashMap(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a UDPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_UDP_CONNECTBACK_FORWARDS = 5;

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * TCP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap _tcpConnectBacks = new FixedsizeHashMap(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a TCPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_TCP_CONNECTBACK_FORWARDS = 5;        
    
    /**
     * The processingqueue to add tcpconnectback socket connections to.
     */
    private static final ProcessingQueue TCP_CONNECT_BACKER = new ProcessingQueue("TCPConnectBack");

    //done

    /**
     * Not used.
     * 
     * keeps track of which hosts have sent us head pongs.  We may choose
     * to use these messages for udp tunnel keep-alive, so we don't want to
     * set the minimum interval too high.  Right now it is half of what we
     * believe to be the solicited grace period.
     */
    private static final Set _udpHeadRequests = Collections.synchronizedSet(new FixedSizeExpiringSet(200, ConnectionSettings.SOLICITED_GRACE_PERIOD.getValue() / 2));

    //do

	/**
	 * Constant handle to the <tt>QueryUnicaster</tt> since it is called
	 * upon very frequently.
	 */
	protected final QueryUnicaster UNICASTER = QueryUnicaster.instance();

	/**
	 * Constant for the <tt>QueryDispatcher</tt> that handles dynamically
	 * generated queries that adjust to the number of results received, the
	 * number of connections, etc.
	 */
	private final QueryDispatcher DYNAMIC_QUERIER = QueryDispatcher.instance();

	/**
	 * Handle to the <tt>ActivityCallback</tt> for sending data to the 
	 * display.
	 */
	private ActivityCallback _callback;

	/**
	 * Handle to the <tt>FileManager</tt> instance.
	 */
	private static FileManager _fileManager;

	//done

	/**
	 * A thread that loops forever, calling forwardQueryRouteTables() every 10 seconds.
	 * The program's single MessageRouter object makes this one QRPPropagator object.
	 * QRPPropagator extends MangedThread, which extends Thread, so this is a thread.
	 */
	private final QRPPropagator QRP_PROPAGATOR = new QRPPropagator();

    /**
     * Our QRP table.
     * 
     * The most recent QRP table we made to describe the files we're sharing.
     * If we're an ultrapeer, the QRP table will include our files and those of all our leaves.
     */
    private QueryRouteTable _lastQueryRouteTable;

    //do

    /**
     * 10
     * The maximum number of response to send to a query that has
     * a "high" number of hops.
     */
    private static final int HIGH_HOPS_RESPONSE_LIMIT = 10;

    /**
     * 25000, 25 seconds.
     * 
     * The lifetime of OOBs guids.
     */
    private static final long TIMED_GUID_LIFETIME = 25 * 1000; 

    /**
     * Keeps track of Listeners of GUIDs.
     * GUID -> List of MessageListener
     */
    private volatile Map _messageListeners = Collections.EMPTY_MAP;
    
    /**
     * Lock that registering & unregistering listeners can hold
     * while replacing the listeners map / lists.
     */
    private final Object MESSAGE_LISTENER_LOCK = new Object();

    /**
     * ref to the promotion manager.
     */
    private PromotionManager _promotionManager;
    
    /**
     * Router for UDPConnection messages.
     */
	private final UDPMultiplexor _udpConnectionMultiplexor = UDPMultiplexor.instance(); 

    /**
     * The time we last received a request for a query key.
     */
    private long _lastQueryKeyTime;

    //done

    /**
     * Make the MessageRouter part of the program's single StandardMessageRouter object.
     * Call initialize() before using it.
     * 
     * This constructor gets called when the RouterService(ActivityCallback) constructor makes the program's StandardMessageRouter object.
     * Saves our client ID GUID in _clientGUID.
     */
    protected MessageRouter() {

        // Get our client ID GUID that will uniquely identify us on the Gnutella network
        _clientGUID = RouterService.getMyGUID(); // LimeWire chose our client ID GUID when it first ran, and keeps it in the limewire.props file
    }

    //do

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize() {
        
        _manager = RouterService.getConnectionManager();
		_callback = RouterService.getCallback();
		_fileManager = RouterService.getFileManager();
		_promotionManager = RouterService.getPromotionManager();
	    QRP_PROPAGATOR.start();

        // schedule a runner to clear unused out-of-band replies
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        
        // schedule a runner to clear guys we've connected back to
        RouterService.schedule(new ConnectBackExpirer(), 10 * CLEAR_TIME, 10 * CLEAR_TIME);
        
        // schedule a runner to send hops-flow messages
        RouterService.schedule(new HopsFlowManager(), HOPS_FLOW_INTERVAL * 10, HOPS_FLOW_INTERVAL);
    }

    /**
     * Routes a query GUID to yourself.
     */
    public void originateQueryGUID(byte[] guid) {
        
        _queryRouteTable.routeReply(guid, FOR_ME_REPLY_HANDLER);
    }

    /** Call this to inform us that a query has been killed by a user or
     *  whatever.  Useful for purging unneeded info.<br>
     *  Callers of this should make sure that they have purged the guid from
     *  their tables.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void queryKilled(GUID guid) throws IllegalArgumentException {
        
        if (guid == null) throw new IllegalArgumentException("Input GUID is null!");
        
        synchronized (_bypassedResults) {
            
            if (!RouterService.getDownloadManager().isGuidForQueryDownloading(guid)) _bypassedResults.remove(guid);
        }
    }

    /** Call this to inform us that a download is finished or whatever.  Useful
     *  for purging unneeded info.<br>
     *  If the caller is a Downloader, please be sure to clear yourself from the
     *  active and waiting lists in DownloadManager.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void downloadFinished(GUID guid) throws IllegalArgumentException {
        
        if (guid == null) throw new IllegalArgumentException("Input GUID is null!");
        
        synchronized (_bypassedResults) {
            
            if (!_callback.isQueryAlive(guid) && !RouterService.getDownloadManager().isGuidForQueryDownloading(guid)) _bypassedResults.remove(guid);
        }
    }
    
    /** @returns a Set with GUESSEndpoints that had matches for the
     *  original query guid.  may be empty.
     *  @param guid the guid of the query you want endpoints for.
     */
    public Set getGuessLocs(GUID guid) {
        
        Set clone = new HashSet();
        
        synchronized (_bypassedResults) {
            
            Set eps = (Set)_bypassedResults.get(guid);
            
            if (eps != null) clone.addAll(eps);
        }
        
        return clone;
    }

    //done

    /** Not used. */
    public String getPingRouteTableDump() {
        return _pingRouteTable.toString();
    }

    /** Not used. */
    public String getQueryRouteTableDump() {
        return _queryRouteTable.toString();
    }

    /** Not used. */
    public String getPushRouteTableDump() {
        return _pushRouteTable.toString();
    }

    /**
     * Remove the given ManagedConnection from all the packet routing tables.
     * With it gone, we won't notice when we get a pong or query hit with the same GUID as a ping or query it sent us.
     * ConnectionManager.removeInternal(c) calls this, giving us a ManagedConnection it's closed.
     * 
     * @param rh A ManagedConnection object that we just closed
     */
    public void removeConnection(ReplyHandler rh) {

        // Also remove it from the DYNAMIC_QUERIER QueryDispatcher (do)
        DYNAMIC_QUERIER.removeReplyHandler(rh);

        // Remove it from all 4 of our routing tables.
        _pingRouteTable.removeReplyHandler(rh);
        _queryRouteTable.removeReplyHandler(rh);
        _pushRouteTable.removeReplyHandler(rh);
        _headPongRouteTable.removeReplyHandler(rh);
    }

    //do

	/**
     * Hop a message, sort it by type, and hand it off to a method for that type of message.
     * All the Gnutella packets we get through TCP socket Gnutella connections come here.
     * 
     * Here's what's happened to the message up to this point:
     * The MessageReader sliced data from a remote computer into a Gnutella packet, and parsed it into a Message object.
     * The message went to ManagedConnection.processReadMessage(m) which called ManagedConnection.handleMessageInternal(m).
     * That method called MessageDispatcher.dispatchTCP(m, managedConnection), which packaged the message and connection for an asynchronous call.
     * The "MessageDispatch" thread picked up the message and connection, and called this method here.
     * 
     * @param msg                 A Gnutella packet we received
     * @param receivingConnection The remote computer that sent it to us over a TCP socket Gnutella connection
     */
    public void handleMessage(Message msg, ManagedConnection receivingConnection) {

        // Move 1 from the TTL to the hops count, having the packet record its trip across the Internet to get here
        msg.hop();

        // 0x00 Ping, the remote computer wants to know the addresses of more computers running Gnutella software
        if (msg instanceof PingRequest) {

            // Update statistics and hand off the message
            ReceivedMessageStatHandler.TCP_PING_REQUESTS.addMessage(msg);
            handlePingRequestPossibleDuplicate((PingRequest)msg, receivingConnection);

        // 0x01 Pong, the remote computer is telling us the addresses of more computers running Gnutella software
		} else if (msg instanceof PingReply) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.TCP_PING_REPLIES.addMessage(msg);
            handlePingReply((PingReply)msg, receivingConnection);

        // 0x80 Query, the remote computer is searching us
        } else if (msg instanceof QueryRequest) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(msg);
            handleQueryRequestPossibleDuplicate((QueryRequest)msg, receivingConnection);

        // 0x81 Query Hit, the remote computer is giving us information about shared files we can download
		} else if (msg instanceof QueryReply) {
		    
		    /*
		     * if someone sent a TCP QueryReply with the MCAST header,
		     * that's bad, so ignore it.
		     */

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.TCP_QUERY_REPLIES.addMessage(msg);
            QueryReply qmsg = (QueryReply)msg;
            handleQueryReply(qmsg, receivingConnection);

        // 0x40 Push, a remote computer wants us to push open a new connection to it
		} else if (msg instanceof PushRequest) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, receivingConnection);

        // 0x30 QRP, 0x00 Reset Table, a remote computer is defining the size and values of its QRP table
        } else if (msg instanceof ResetTableMessage) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handleResetTableMessage((ResetTableMessage)msg, receivingConnection);

        // 0x30 QRP, 0x01 Patch Table, a remote computer is sending us its QRP table
		} else if (msg instanceof PatchTableMessage) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handlePatchTableMessage((PatchTableMessage)msg, receivingConnection);

        } else if (msg instanceof TCPConnectBackVendorMessage) {

            ReceivedMessageStatHandler.TCP_TCP_CONNECTBACK.addMessage(msg);
            handleTCPConnectBackRequest((TCPConnectBackVendorMessage)msg, receivingConnection);

        } else if (msg instanceof UDPConnectBackVendorMessage) {

			ReceivedMessageStatHandler.TCP_UDP_CONNECTBACK.addMessage(msg);
            handleUDPConnectBackRequest((UDPConnectBackVendorMessage)msg, receivingConnection);

        } else if (msg instanceof TCPConnectBackRedirect) {

            handleTCPConnectBackRedirect((TCPConnectBackRedirect)msg, receivingConnection);

        } else if (msg instanceof UDPConnectBackRedirect) {

            handleUDPConnectBackRedirect((UDPConnectBackRedirect)msg, receivingConnection);

        } else if (msg instanceof PushProxyRequest) {

            handlePushProxyRequest((PushProxyRequest)msg, receivingConnection);

        // BEAR 12 version 1 Query Status Response vendor message, a leaf is telling us how many hits it has for a search we're running on its behalf
        } else if (msg instanceof QueryStatusResponse) {

            // Have the QueryDispatcher find the QueryHandler that represents the search, and save the updated number there
            handleQueryStatus((QueryStatusResponse)msg, receivingConnection);

        } else if (msg instanceof GiveStatsVendorMessage) {

            handleGiveStats((GiveStatsVendorMessage)msg, receivingConnection);

        } else if(msg instanceof StatisticVendorMessage) {

            handleStatisticsMessage((StatisticVendorMessage)msg, receivingConnection);

        } else if (msg instanceof HeadPing) {

        	handleHeadPing((HeadPing)msg, receivingConnection);

        } else if(msg instanceof SimppRequestVM) {

            handleSimppRequest((SimppRequestVM)msg, receivingConnection);

        } else if(msg instanceof SimppVM) {

            handleSimppVM((SimppVM)msg);

        } else if(msg instanceof UpdateRequest) {

            handleUpdateRequest((UpdateRequest)msg, receivingConnection);

        } else if(msg instanceof UpdateResponse) {

            handleUpdateResponse((UpdateResponse)msg, receivingConnection);

        } else if (msg instanceof HeadPong) {

            handleHeadPong((HeadPong)msg, receivingConnection);

        // It's some other kind of vendor message
        } else if (msg instanceof VendorMessage) {

            // Have ManagedConnection.handleVendorMessage() sort it and handle it
            receivingConnection.handleVendorMessage((VendorMessage)msg);
        }

        /*
         * This may trigger propogation of query route tables.  We do this AFTER
         * any handshake pings.  Otherwise we'll think all clients are old
         * clients.
         * forwardQueryRouteTables();
         */

        notifyMessageListener(msg, receivingConnection);
    }

    /**
     * Notifies any message listeners of this message's guid about the message.
     * This holds no locks.
     */
    private final void notifyMessageListener(Message msg, ReplyHandler handler) {
        
        List all = (List)_messageListeners.get(msg.getGUID());
        
        if (all != null) {
            
            for (Iterator i = all.iterator(); i.hasNext(); ) {
                
                MessageListener next = (MessageListener)i.next();
                next.processMessage(msg, handler);
            }
        }
    }

	/**
     * Hop a message, sort it by type, and hand it off to a method for that type of message.
     * All the Gnutella messages we get in UDP packets come here.
     * 
     * Here's what's happened to the message up to this point:
     * NIODispatcher.process(SelectionKey, Object, int) finds a key selected for reading.
     * UDPService.handleRead() gets the source InetSocketAddress from the channel, reads the data and parses it into a Message object.
     * UDPService.processMessage(Message, InetSocketAddress) calls the next method.
     * MessageDispatcher.dispatchUDP(Message, InetSocketAddress) has the "MessageDispatch" thread call the next method.
     * MessageDispatcher.UDPDispatch.run() calls here.
     * 
     * @param msg  A Gnutella message we got in a UDP packet
     * @param addr The IP address and port number it came from
     */
	public void handleUDPMessage(Message msg, InetSocketAddress addr) {

        // Move 1 from the TTL to the hops count, having the packet record its trip across the Internet to get here
        msg.hop();

        // Get the IP address and port number of the computer that sent us the Gnutella message in a UDP packet
		InetAddress address = addr.getAddress(); // UDPService.handleRead() got the source InetSocketAddress from the channel object
		int         port    = addr.getPort();

        // Make sure our IP address we're saying doesn't start 0 or 255, and our port number isn't 0
		if (!RouterService.isIpPortValid()) return; // If we have bad address information for ourself, we can't send a pong about us

		// Send UDPConnection messages on to the connection multiplexor
		// for routing to the appropriate connection processor
		if (msg instanceof UDPConnectionMessage) {
		    _udpConnectionMultiplexor.routeMessage((UDPConnectionMessage)msg, address, port);
			return;
		}

        // Make a new UDPReplyHandler that will wrap Gnutella packets into UDP packets and send them to the given IP address and port number
		ReplyHandler handler = new UDPReplyHandler(address, port);

        // We received a query by UDP
        if (msg instanceof QueryRequest) {

            //TODO1: compare QueryKey with old generation params.  if it matches
            //send a new one generated with current params
            if (hasValidQueryKey(address, port, (QueryRequest) msg)) {
                sendAcknowledgement(addr, msg.getGUID());
                // a TTL above zero may indicate a malicious client, as UDP
                // messages queries should not be sent with TTL above 1.
                //if(msg.getTTL() > 0) return;
                if (!handleUDPQueryRequestPossibleDuplicate((QueryRequest)msg, handler)) {
                    ReceivedMessageStatHandler.UDP_DUPLICATE_QUERIES.addMessage(msg);
                }
            }
            ReceivedMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(msg);

        // We received a query hit by UDP
		} else if (msg instanceof QueryReply) {

            QueryReply qr = (QueryReply) msg;
			ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
            int numResps = qr.getResultCount();
            // only account for OOB stuff if this was response to a 
            // OOB query, multicast stuff is sent over UDP too....
            if (!qr.isReplyToMulticastQuery()) OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
            handleQueryReply(qr, handler);

        // We received a ping by UDP
		} else if (msg instanceof PingRequest) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.UDP_PING_REQUESTS.addMessage(msg);
			handleUDPPingRequestPossibleDuplicate((PingRequest)msg, handler, addr);

        // We received a pong by UDP
		} else if (msg instanceof PingReply) {

            // Update statistics and hand off the message
			ReceivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
            handleUDPPingReply((PingReply)msg, handler, address, port);

        // We received a push by UDP
		} else if (msg instanceof PushRequest) {

			ReceivedMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(msg);
			handlePushRequest((PushRequest)msg, handler);

        } else if (msg instanceof LimeACKVendorMessage) {
            
			ReceivedMessageStatHandler.UDP_LIME_ACK.addMessage(msg);
            handleLimeACKMessage((LimeACKVendorMessage)msg, addr);

        // LIME 12 version 2 Reply Number vendor message, a computer with hits for our search is telling us how many it has
        } else if (msg instanceof ReplyNumberVendorMessage) {

            handleReplyNumberMessage((ReplyNumberVendorMessage) msg, addr);

        } else if (msg instanceof GiveStatsVendorMessage) {
            
            handleGiveStats((GiveStatsVendorMessage) msg, handler);
            
        } else if (msg instanceof StatisticVendorMessage) {
            
            handleStatisticsMessage((StatisticVendorMessage)msg, handler);
            
        } else if (msg instanceof UDPCrawlerPing) {
            
        	//TODO1: add the statistics recording code
        	handleUDPCrawlerPing((UDPCrawlerPing)msg, handler);
            
        } else if (msg instanceof HeadPing) {
            
        	//TODO1: add the statistics recording code
        	handleHeadPing((HeadPing)msg, handler);
            
        } else if (msg instanceof UpdateRequest) {
            
            handleUpdateRequest((UpdateRequest)msg, handler);
        }
        
        notifyMessageListener(msg, handler);
    }
    
    /**
     * The handler for Multicast messages. Processes a message based on the
     * message type.
     *
     * @param msg the <tt>Message</tt> recieved.
     * @param addr the <tt>InetSocketAddress</tt> containing the IP and
     *  port of the client node.
     */
	public void handleMulticastMessage(Message msg, InetSocketAddress addr) {
    
        // Use this assert for testing only -- it is a dangerous assert
        // to have in the field, as not all messages currently set the
        // network int appropriately.
        // If someone sends us messages we're not prepared to handle,
        // this could cause widespreaad AssertFailures
        //Assert.that(msg.isMulticast(),
        //   "non multicast message in handleMulticastMessage: " + msg);
    
        // no multicast messages should ever have been
        // set with a TTL greater than 1.
        if( msg.getTTL() > 1 )
            return;

        // Increment hops and decrement TTL.
        msg.hop();

		InetAddress address = addr.getAddress();
		int port = addr.getPort();
		
        if (NetworkUtils.isLocalAddress(address) &&
          !ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.getValue())
            return;

        // Make a new UDPReplyHandler that will wrap Gnutella packets into UDP packets and send them to the given IP address and port number
		ReplyHandler handler = new UDPReplyHandler(address, port);

        if (msg instanceof QueryRequest) {
            
            if (!handleUDPQueryRequestPossibleDuplicate((QueryRequest)msg, handler)) {
                
                ReceivedMessageStatHandler.MULTICAST_DUPLICATE_QUERIES.addMessage(msg);
            }
            
            ReceivedMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(msg);
            
		} else if (msg instanceof PingRequest) {
            
			ReceivedMessageStatHandler.MULTICAST_PING_REQUESTS.addMessage(msg);
			handleUDPPingRequestPossibleDuplicate((PingRequest)msg, handler, addr);
            
		} else if (msg instanceof PushRequest) {
            
            ReceivedMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(msg);
			handlePushRequest((PushRequest)msg, handler);
		}
        
        notifyMessageListener(msg, handler);
    }

    /**
     * Returns true if the Query has a valid QueryKey.  false if it isn't
     * present or valid.
     */
    protected boolean hasValidQueryKey(InetAddress ip, int port, QueryRequest qr) {
        
        if (qr.getQueryKey() == null) return false;
        QueryKey computedQK = QueryKey.getQueryKey(ip, port);
        return qr.getQueryKey().equals(computedQK);
    }

	/**
	 * Sends an ack back to the GUESS client node.  
	 */
	protected void sendAcknowledgement(InetSocketAddress addr, byte[] guid) {
        
		ConnectionManager manager = RouterService.getConnectionManager();
		Endpoint host = manager.getConnectedGUESSUltrapeer();
		PingReply reply;
        
		if (host != null) {
            
			try {
                
                reply = PingReply.createGUESSReply(guid, (byte)1, host);
                
            } catch (UnknownHostException e) {
                
				reply = createPingReply(guid);
            }
            
		} else {
            
			reply = createPingReply(guid);
		}
		
		// No GUESS endpoints existed and our IP/port was invalid.
		if (reply == null) return;

        UDPService.instance().send(reply, addr.getAddress(), addr.getPort());
		SentMessageStatHandler.UDP_PING_REPLIES.addMessage(reply);
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or a <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply createPingReply(byte[] guid) {
        
		GUESSEndpoint endpoint = UNICASTER.getUnicastEndpoint();
        
		if (endpoint == null) {
            
		    if (RouterService.isIpPortValid()) return PingReply.create(guid, (byte)1);
            else                               return null;
            
		} else {
            
            return PingReply.createGUESSReply(guid, (byte)1, endpoint.getPort(), endpoint.getAddress().getAddress());
		}
	}

    //done

    /**
     * Adds the ping's message GUID and the ManagedConnection that sent it to us to our RouteTable for pings and pongs.
     * If the GUID wasn't already listed, calls handlePingRequest(request, handler) to keep going.
     * 
     * handleMessage(Message, ManagedConnection) calls this.
     * 
     * @param request A ping packet we received
     * @param handler The remote computer that sent it to us over a TCP socket Gnutella connection
     */
    final void handlePingRequestPossibleDuplicate(PingRequest request, ReplyHandler handler) {

        // Add the ping to our route table, leaving without continuing if we already have its GUID
		if (

		    // Add the ping's message GUID and the ManagedConnection that sent it to us to our RouteTable for pings and pongs
            _pingRouteTable.tryToRouteReply(request.getGUID(), handler)

            // tryToRouteReply() will return null if the GUID is already listed, or the ManagedConnection is closed
            != null)

            // If it didn't, keep going with handlePingRequest(request, handler)
            handlePingRequest(request, handler);
    }

    /**
     * Adds the ping's message GUID and the UDPRelyHandler that sent it to us to our RouteTable for pings and pongs.
     * If the GUID wasn't already listed, calls handleUDPPingRequest(request, handler, addr) to keep going.
     * 
     * handleUDPMessage(Message, InetSocketAddress) and handleMulticastMessage(Message, InetSocketAddress) call this.
     * 
     * @param request A ping packet we received
     * @param handler The UDPReplyHandler object that represents the remote computer that sent it to us in a UDP packet
     * @param addr    The IP address and port number of the remote computer
     */
    final void handleUDPPingRequestPossibleDuplicate(PingRequest request, ReplyHandler handler, InetSocketAddress  addr) {

        // Add the ping to our route table, leaving without continuing if we already have its GUID
		if (

            // Add the ping's message GUID and the UDPReplyHandler that sent it to us to our RouteTable for pings and pongs
            _pingRouteTable.tryToRouteReply(request.getGUID(), handler)

            // tryToRouteReply() will return null if the GUID is already listed
            != null)

            // If it didn't, keep going with handleUDPPingRequest(request, handler, addr)
            handleUDPPingRequest(request, handler, addr);
    }

    /**
     * Adds the query to the RouteTable, and sees if it's already there.
     * Adjusts the TTL for probe queries and probe query extensions.
     * Leads to calls to handleQueryRequest() to start dynamic querying, search our shared files and respond with query hit packets, and forward the query to our ultrapeers.
     * handleMessage() calls this when we've received a query packet through a TCP socket Gnutella connection.
     * 
     * The query packet is one of 3 things: (do)
     * A query
     * A probe query
     * A probe query extension
     * 
     * This method looks at its hops and TTL to distinguish between query and probe query.
     * It calls wasProbeQuery() below to identify a probe query extension.
     * 
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     * 
     * With the new handling of probe queries (TTL 1, Hops 0), we have a few new options:
     * 1) If we have a probe query....
     *    a) If you have never seen it before, put it in the route table and set the ttl appropriately
     *    b) If you have seen it before, then just count it as a duplicate
     * 2) If it isn't a probe query....
     *    a) Is it an extension of a probe? If so re-adjust the TTL.
     *    b) Is it a 'normal' query (no probe extension or already extended)?
     *    Then check if it is a duplicate:
     *       1) If it a duplicate, just count it as one
     *       2) If it isn't, put it in the route table but no need to setTTL
     * 
     * @param request             A query packet we received
     * @param receivingConnection The ManagedConnection that sent it to us
     */
    final void handleQueryRequestPossibleDuplicate(QueryRequest request, ManagedConnection receivingConnection) {

        /*
         * we msg.hop() before we get here....
         */

        /*
         * hops may be 1 or 2 because we may be probing with a leaf query....
         */

        // If the query has 1 or 2 hops and can't leave us, it's a probe query
        final boolean isProbeQuery =                                 // Determine if this is a probe query
            ((request.getTTL() == 0) &&                              // It doesn't have any TTL left, and
            ((request.getHops() == 1) || (request.getHops() == 2))); // It hopped 1 or 2 times to get here

        // Add the query's message GUID to our RouteTable for queries and query hits
		ResultCounter counter = _queryRouteTable.tryToRouteReply(request.getGUID(), receivingConnection); // Returns a RouteTableEntry object you can call .getNumResults() on

        // We haven't seen this query before
		if (counter != null) {

            /*
             * 1a: set the TTL of the query so it can be potentially extended
             */

            // If this is a probe query, save a TTL of 1 with it in the RouteTable
            if (isProbeQuery) _queryRouteTable.setTTL(counter, (byte)1); // Calls counter.setTTL(1) to change the TTL the RouteTableEntry remembers from 0 to 1

            /*
             * 1a and 2b2
             * if a new probe or a new request, do everything (so input true below)
             */

            // Start dynamic querying, search our shared files and respond with query hit packets, and forward the query to our leaves and ultrapeers
            handleQueryRequest(request, receivingConnection, counter, true); // Pass true to forward the query to our leaves

        // We've received this query packet before, and it isn't a probe query
		} else if ((counter == null) && !isProbeQuery) {

            /*
             * probe extension?
             */

            /*
             * rebroadcast out but don't locally evaluate....
             */

            // The query is a probe extension
            if (wasProbeQuery(request)) { // If the query is a probe extension, give it another TTL in the RouteTable and return true

                // Start dynamic querying, search our shared files and respond with query hit packets, and forward the query to our ultrapeers
                handleQueryRequest(request, receivingConnection, counter, false); // Pass false to not forward the query to our leaves

            // It's not, it's just a duplicate
            } else {

                /*
                 * 2b1: not a correct extension, so call it a duplicate....
                 */

                // Record the duplicate query in statistics
                tallyDupQuery(request);
            }

        // We've received this probe query before
        } else if ((counter == null) && isProbeQuery) {

            // Record the duplicate query in statistics
            tallyDupQuery(request);

        // 2b1: duplicate normal query
        } else {

            /*
             * TODO1:kfaaborg How can control reach here?
             */

            // Record the duplicate query in statistics
            tallyDupQuery(request);
        }
    }

    /**
     * Takes a query that isn't a probe query, but might be a probe extension. (do)
     * If so, give it another TTL in the RouteTable.
     * Only handleQueryRequestPossibleDuplicate() above calls this.
     * 
     * @param request A query packet we've seen before that either has some TTL left or hopped 3 or more times to get here, making it not look like a probe query.
     * @return        True if the query still had some TTL left, its RouteTableEntry was remembering a TTL of 1, and we changed it to remember 1 more than the query's actual TTL.
     *                False if we didn't do that.
     */
    private boolean wasProbeQuery(QueryRequest request) {

        /*
         * if the current TTL is large enough and the old TTL was 1, then this
         * was a probe query....
         * NOTE: that i'm setting the ttl to be the actual ttl of the query.  i
         * could set it to some max value, but since we only allow TTL 1 queries
         * to be extended, it isn't a big deal what i set it to.  in fact, i'm
         * setting the ttl to the correct value if we had full expanding rings
         * of queries.
         */

        // Return true if the query still has some TTL left, its RouteTableEntry was remembering a TTL of 1, and we changed it to remember 1 more than the query's actual TTL
        return (

            // If the query still has some TTL left
            (request.getTTL() > 0) &&

            // And the RouteTableEntry is remembering a TTL of 1, have it remember 1 more than the query's actual TTL
            _queryRouteTable.getAndSetTTL(      // (4) Returns true if we guessed correctly in (2) and set the value in (3)
                request.getGUID(),              // (1) A GUID that should be listed in this RouteTable
                (byte)1,                        // (2) If the TTL the RouteTableEntry is remembering is 1
                (byte)(request.getTTL() + 1))); // (3) Change it to remember 1 more than the query's actual TTL
    }

    /**
     * Give the query packet to the ReceivedMessageStatHandler for TCP duplicate queries.
     * Only handleQueryRequestPossibleDuplicate() above calls this.
     * 
     * @param request A query packet we've received
     */
    private void tallyDupQuery(QueryRequest request) {

        // Count the statistic
        ReceivedMessageStatHandler.TCP_DUPLICATE_QUERIES.addMessage(request);
    }

	/**
     * Start dynamic querying, search our shared files and respond with query hit packets, and forward the query to our leaves and ultrapeers.
     * If we've seen the given query packet before, does nothing and returns false.
     * 
     * @param request A query packet we received over UDP.
     * @param handler The remote computer that sent it to us.
     * @return        True if it had a GUID not yet in the RouteTable, and we responded and forwarded it.
     *                False if the query's message GUID is already in the route table, this method did nothing.
	 */
	final boolean handleUDPQueryRequestPossibleDuplicate(QueryRequest request, ReplyHandler handler) {

        // Add the query packet's message GUID to our RouteTable for queries and query hits
		ResultCounter counter = _queryRouteTable.tryToRouteReply(request.getGUID(), handler);

        // The RouteTable hadn't seen that GUID before
		if (counter != null) {

            // Start dynamic querying, search our shared files and respond with query hit packets, and forward the query to our leaves and ultrapeers
            handleQueryRequest(request, handler, counter, true); // True to forward the query to our leaves

            // True, the search was unique and we did everything
            return true;
		}

        // The query's message GUID was already in the route table, do nothing and return false
		return false;
	}

    /**
     * Only continues if the ping has 1 hop and no TTL, or the remote computer it's from hasn't pinged us for at least 2.5 seconds.
     * Calls respondToPingRequest(ping, handler) to keep going.
     * 
     * Handles pings from the network.  With the addition of pong caching, this
     * method will either respond with cached pongs, or it will ignore the ping
     * entirely if another ping has been received from this connection very
     * recently.  If the ping is TTL=1, we will always process it, as it may
     * be a hearbeat ping to make sure the connection is alive and well.
     * 
     * @param request A ping packet we received
     * @param handler The remote computer that sent it to us over a TCP socket Gnutella connection
     */
    final private void handlePingRequest(PingRequest ping, ReplyHandler handler) {

        /*
         * Send it along if it's a heartbeat ping or if we should allow new
         * pings on this connection.
         */

        // Only reply with a pong if this ping is keeping the socket open, or the remote computer hasn't pinged us in awhile
        if (ping.isHeartbeat() ||      // This ping has 1 hop and 0 TTL, the remote computer may have sent it to keep a quiet socket open
            handler.allowNewPings()) { // It's been 2.5 seconds since we've received a ping from this remote computer

            // The ping made it through those checks, keep going
            respondToPingRequest(ping, handler);
        }
    }

    /**
     * Call StandardMessageRouter.respondToUDPPingRequest(pingRequest, addr, handler).
     * Makes sure the ping doesn't have the a GGEP block with the "QK" extension.
     * 
     * @param request A ping packet we received
     * @param handler The UDPReplyHandler object that represents the remote computer that sent it to us in a UDP packet
     * @param addr    The IP address and port number of the remote computer
     */
    protected void handleUDPPingRequest(PingRequest pingRequest, ReplyHandler handler, InetSocketAddress addr) {

        /*
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

        // The ping has "QK" in its GGEP block
        if (pingRequest.isQueryKeyRequest()) {

            // QueryKey is a part of GUESS, which LimeWire doesn't use anymore
            sendQueryKeyPong(pingRequest, addr);

        // The ping doesn't have the "QK" extension
        } else {

            // Call StandardMessageRouter.respondToUDPPingRequest(pingRequest, addr, handler)
            respondToUDPPingRequest(pingRequest, addr, handler);
        }
    }

    /**
     * QueryKey is a part of GUESS, and no longer used.
     * Called when we get a ping through UDP that has the "QK" GGEP extension.
     * 
     * Generates a QueryKey for the source (described by addr) and sends the
     * QueryKey to it via a QueryKey pong....
     */
    protected void sendQueryKeyPong(PingRequest pr, InetSocketAddress addr) {
        // check if we're getting bombarded
        long now = System.currentTimeMillis();
        if (now - _lastQueryKeyTime < SearchSettings.QUERY_KEY_DELAY.getValue()) return;
        _lastQueryKeyTime = now;
        // after find more sources and OOB queries, everyone can dole out query
        // keys....
        // generate a QueryKey (quite quick - current impl. (DES) is super
        // fast!
        InetAddress address = addr.getAddress();
        int port = addr.getPort();
        QueryKey key = QueryKey.getQueryKey(address, port);
        // respond with Pong with QK, as GUESS requires....
        PingReply reply = PingReply.createQueryKeyReply(pr.getGUID(), (byte)1, key);
        UDPService.instance().send(reply, addr.getAddress(), addr.getPort());
    }

    /**
     * Handle a pong we've received through UDP.
     * Gives the source IP address of the packet to the QueryUnicaster if it's different from the address in the pong.
     * Adds the pong to the HostCatcher and PongCacher, gives pongs for us to the ForMeReplyHandler, and forwards the pong to our leaves.
     * 
     * MessageRouter.handleUDPMessage() calls this when we get a pong in a UDP packet.
     * 
     * @param reply   A pong we've received in a UDP packet
     * @param handler The UDPReplyHandler that represents the remote computer that sent it to us
     * @param address The IP address the packet came from
     * @param port    The port number the packet came from
     */
    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler, InetAddress address, int port) {

        // QueryKey is a part of GUESS, which LimeWire doesn't use anymore
        if (reply.getQueryKey() != null) {
            // this is a PingReply in reply to my QueryKey Request -
            //consume the Pong and return, don't process as usual....
            OnDemandUnicaster.handleQueryKeyPong(reply);
            return;
        }

        /*
         * also add the sender of the pong if different from the host
         * described in the reply...
         */

        // If the address information in the pong doesn't match the address we got it from
        if ((reply.getPort() != port) || (!reply.getInetAddress().equals(address))) {

            // Give the address that sent it to us to the QueryUnicaster
            UNICASTER.addUnicastEndpoint(address, port);
		}

        // Add the pong to the HostCatcher and PongCacher, give pongs for us to the ForMeReplyHandler, and forward to the pong to our leaves.
        handlePingReply(reply, handler);
    }

    /**
     * Given a query packet we've received, start dynamic querying, search our shared files and respond with query hit packets, and forward the query to our leaves and ultrapeers.
     * 
     * If the query is from one of our leaves, this method uses dynamic querying:
     * It runs the query against our shared files, and sends query hit packets in response.
     * It wraps the query packet, leaf handler, and route table entry in a QueryHandler object, and gives it to the QueryDispatcher to start the dynamic query.
     * It doesn't forward the query to our leaves.
     * 
     * Instead, if the query is from a fellow ultrapeer and still has some TTL left:
     * This method forwards the query to our ultrapeers.
     * 
     * If the caller set locallyEvaluate:
     * It forwards the query to our leaves.
     * It runs the query against our shared files, and sends query hit packets in response.
     * 
     * @param request         A query packet we received.
     * @param handler         The remote computer that sent it to us.
     * @param counter         The RouteTableEntry object we added for the query packet.
     *                        If the query's message GUID was already listed, counter is null.
     * @param locallyEvaluate True if you want this method forward the query to our leaves.
     */
    protected void handleQueryRequest(QueryRequest request, ReplyHandler handler, ResultCounter counter, boolean locallyEvaluate) {

        // If the given query packet makes it through the program's filter for what we can show the user, give the search text to the GUI
        if (!handler.isPersonalSpam(request)) _callback.handleQueryString(request.getQuery());

        /*
         * if it's a request from a leaf and we GUESS, send it out via GUESS --
         * otherwise, broadcast it if it still has TTL
         * if(handler.isSupernodeClientConnection() &&
         * RouterService.isGUESSCapable())
         * unicastQueryRequest(request, handler);
         * else if(request.getTTL() > 0) {
         */

        // Does nothing
        updateMessage(request, handler); // TODO1:kfaaborg Remove this line

        // The query is from one of our leaves
		if (handler.isSupernodeClientConnection() && // We're an ultrapeer, and the computer that sent us this query is one of our leaves, and
            counter != null) {                       // The RouteTable didn't already have the ping's message GUID, and listed it

            // We could send a UDP packet to the searching computer that made this query, and it would get it
            if (request.desiresOutOfBandReplies()) { // Looks for 0x04 in the speed flags bytes

                /*
                 * this query came from a leaf - so check if it desires OOB
                 * responses and make sure that the IP it advertises is legit - if it isn't drop it
                 */

                /*
                 * There are 3 IP addresses:
                 * remoteAddr is the IP address of the leaf which sent us this query.
                 * myAddress is our IP address.
                 * request.getReplyAddress() is the searching computer's IP address it hid in the message GUID when it made it.
                 */

                // Get the leaf's IP address and our IP address
                String remoteAddr = handler.getInetAddress().getHostAddress();         // The leaf's IP address
                String myAddress = NetworkUtils.ip2string(RouterService.getAddress()); // Our IP address

                // The leaf made the query packet and sent it to us, the leaf is the searching computer
                if (request.getReplyAddress().equals(remoteAddr)) { // The IP addresses of the searching computer and the leaf are the same

                    /*
                     * continue below, everything looks good
                     */

                // We are the searching computer
                } else if (request.getReplyAddress().equals(myAddress) && // The IP addresses of the searching computer and us are the same, and
                    RouterService.isOOBCapable()) {                       // We can accept unsolicited UDP packets

                    /*
                     * i am proxying - maybe i should check my success rate but
                     * whatever...
                     */

                // Something else
                } else {

                    // Leave without doing anything
                    return;
                }
            }

            /*
             * don't send it to leaves here -- the dynamic querier will
             * handle that
             */

            // Were going to use dynamic querying, so we shouldn't forward the query to our leaves
            locallyEvaluate = false;

            /*
             * do respond with files that we may have, though
             */

            // See which of our shared files match a given query packet, and generate and send query hit packets in response
            respondToQueryRequest( // Calls StandardMessageRouter.respondToQueryRequest()
                request,           // The query packet we received
                _clientGUID,       // Not used, our client ID GUID that uniquely identifies us on the Gnutella network
                handler);          // The remote computer that sent it to us

            // Forward the query packet to the rest of our LAN in a multicast UDP packet
            multicastQueryRequest(request);

            // Wrap the query packet, leaf handler, and route table entry in a QueryHandler object, and give it to the QueryDispatcher, initiating the dynamic query
			if (handler.isGoodLeaf()) sendDynamicQuery(QueryHandler.createHandlerForNewLeaf(request, handler, counter), handler); // The leaf supports advanced Gnutella features
			else                      sendDynamicQuery(QueryHandler.createHandlerForOldLeaf(request, handler, counter), handler); // The leaf is running old Gnutella software

        // The query is from a fellow ultrapeer, and still has some TTL left
        } else if (request.getTTL() > 0 && RouterService.isSupernode()) {

            /*
             * send the request to intra-Ultrapeer connections -- this does
             * not send the request to leaves
             */

            // Forward the query packet to our ultrapeers
            if (handler.isGoodUltrapeer()) forwardQueryToUltrapeers(request, handler);        // The remote ultrapeer supports advanced Gnutella features, send its query to all our connections
            else                           forwardLimitedQueryToUltrapeers(request, handler); // The remote ultrapper is running old Gnutella software, only send its query to some of our connections
		}

        // The caller requested that we forward this query to our leaves, and it's not from one of our leaves
        if (locallyEvaluate) {

            /*
             * always forward any queries to leaves -- this only does
             * anything when this node's an Ultrapeer
             */

            // Forward the query packet to our leaves
            forwardQueryRequestToLeaves(request, handler);

            /*
             * if (I'm firewalled AND the source is firewalled) AND
             * NOT(he can do a FW transfer and so can i) then don't reply...
             */

            // Only run the search against our shared files and respond with our own query hits if we can get a file to the searching computer
            if ((request.isFirewalledSource()                && // The searching computer is firewalled, looks for 0x40 in the speed flags bytes, and
                !RouterService.acceptedIncomingConnection()) && // We're firewalled too, and
                !(request.canDoFirewalledTransfer()          && // The searching computer can't do firewall-to-firewall transfers, looks for 0x02 in the speed flags bytes, and
                UDPService.instance().canDoFWT()))              // We can do firewall-to-firewall transfers (do)
                return;                                         // Leave without running the search against our files and sending query hit packets in response

            // See which of our shared files match a given query packet, and generate and send query hit packets in response
            respondToQueryRequest( // Calls StandardMessageRouter.respondToQueryRequest()
                request,           // The query packet we received
                _clientGUID,       // Not used, our client ID GUID that uniquely identifies us on the Gnutella network
                handler);          // The remote computer that sent it to us
        }
    }

    //do

    /** Handles a ACK message - looks up the QueryReply and sends it out of
     *  band.
     */
    protected void handleLimeACKMessage(LimeACKVendorMessage ack, InetSocketAddress addr) {

        GUID.TimedGUID refGUID = new GUID.TimedGUID(new GUID(ack.getGUID()), TIMED_GUID_LIFETIME);
        QueryResponseBundle bundle = (QueryResponseBundle) _outOfBandReplies.remove(refGUID);

        if ((bundle != null) && (ack.getNumResults() > 0)) {
            
            InetAddress iaddr = addr.getAddress();
            int port = addr.getPort();

            //convert responses to QueryReplies, but only send as many as the
            //node wants
            
            Iterator iterator = null;
            
            if (ack.getNumResults() < bundle._responses.length) {
                
                Response[] desired = new Response[ack.getNumResults()];
                for (int i = 0; i < desired.length; i++) desired[i] = bundle._responses[i];
                iterator = responsesToQueryReplies(desired, bundle._query, 1);
                
            } else {
                
                iterator = responsesToQueryReplies(bundle._responses, bundle._query, 1); 
            }
            
            //send the query replies
            while (iterator.hasNext()) {
                
                QueryReply queryReply = (QueryReply)iterator.next();
                UDPService.instance().send(queryReply, iaddr, port);
            }
        }
        
        // else some sort of routing error or attack?
        // TODO1: tally some stat stuff here
    }

    //done

    /**
     * A remote computer is telling us how many results it has, reply to request them all.
     * 
     * A remote computer sent us a LIME 12 2 Reply Number vendor message in a UDP packet.
     * Our query packet reached it, and it has hits for us.
     * It's telling us how many hits it has.
     * 
     * This method confirms we're still interested in the search, and we don't already have too many hits for it yet.
     * Makes a LIME 11 2 Lime Acknowledgement vendor message to request all the results, and sends it back to the remote computer over UDP.
     * 
     * This is called when a client on the network has results for us that we
     * may want.  We may contact them back directly or just cache them for
     * use.
     * 
     * @param reply The LIME 12 2 Reply Number vendor message
     * @param addr  The IP address and port number of the computer that sent it to us in a UDP packet
     */
    protected void handleReplyNumberMessage(ReplyNumberVendorMessage reply, InetSocketAddress addr) {

        // Get the message GUID of the LIME 12 2 Reply Number vendor message
        GUID qGUID = new GUID(reply.getGUID()); // All the packets that are a part of this search have this as their message GUID, it identifies the search

        /*
         * Find out how many hits we've found for this search.
         * If this search is for us, get the hit count from the SearchResultHandler, which passes information up to the GUI.
         * If the SearchResultHandler doesn't know about it, this is probably a proxy search we're performing for one of our leaves.
         * Look up the hit count in the QueryDispatcher instead.
         */

        // Look up the GUID in the program's SearchResultHandler object, and find out how many results we've told the GUI about for it
        int numResults = RouterService.getSearchResultHandler().getNumResultsForQuery(qGUID); // Returns -1 if not found

        // If the SearchResultHandler doesn't know about this search, it may be a proxy query, look up the results number in the QueryDispatcher instead
        if (numResults < 0) numResults = DYNAMIC_QUERIER.getLeafResultsForQuery(qGUID); // Returns -1 if not found

        /*
         * see if we need more results for this query....
         * if not, remember this location for a future, 'find more sources'
         * targeted GUESS query, as long as the other end said they can receive
         * unsolicited.
         */

        // If we couldn't find the search in the SearchResultHandler or QueryDispatcher, or we did find it and it has more than 150 hits
        if ((numResults < 0) || (numResults > QueryHandler.ULTRAPEER_RESULTS)) {

            // We'll bypass this result
            OutOfBandThroughputStat.RESPONSES_BYPASSED.addData(reply.getNumResults());

            /*
             * if the reply cannot receive unsolicited udp, there is no point storing it.
             */

            // Make sure the hit computer can get a UDP packet
            if (!reply.canReceiveUnsolicited()) return; // Looks for the flag in the second payload byte

            // Access the program's DownloadManager object
            DownloadManager dManager = RouterService.getDownloadManager();

            /*
             * only store result if it is being shown to the user or if a
             * file with the same guid is being downloaded
             */

            // Only continue if the user interface or the download manager know about the search
            if (!_callback.isQueryAlive(qGUID) &&           // The user interface doesn't have the search anymore, and
                !dManager.isGuidForQueryDownloading(qGUID)) // The DownloadManager doesn't recognize it
                return;                                     // Leave

            // Wrap the IP address and port number of the remote computer that sent us the UDP packet in a GUESSEndpoint object
            GUESSEndpoint ep = new GUESSEndpoint(addr.getAddress(), addr.getPort());

            // Only let one thread access _bypassedResults at once
            synchronized (_bypassedResults) {

                /*
                 * this is a quick critical section for _bypassedResults
                 * AND the set within it
                 */

                // Loop up the search GUID in the _bypassedResults list
                Set eps = (Set)_bypassedResults.get(qGUID);
                if (eps == null) {

                    // Not found, add the GUESSEndpoint to it under the search GUID
                    eps = new HashSet();
                    _bypassedResults.put(qGUID, eps);
                }

                // If we don't have 150 GUESSEndpoint objects stored under the search GUID yet, add the one we made there
                if (_bypassedResults.size() <= MAX_BYPASSED_RESULTS) eps.add(ep);
            }

            // Leave, we're bypassing the result
            return;
        }

        /*
         * We found the search in the SearchResultHandler or QueryDispatcher
         */

        // Make a Lime Acknowledgement vendor message to request those results from the remote computer, and send it
        LimeACKVendorMessage ack = new LimeACKVendorMessage(qGUID, reply.getNumResults()); // Ask for all the results
        UDPService.instance().send(ack, addr.getAddress(), addr.getPort());
        OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(reply.getNumResults());
    }

    /**
     * Save a query packet we received and our list of file hits to send them later.
     * Only StandardMessageRouter.sendResponses() calls this.
     * 
     * Stores (for a limited time) the resps for later out-of-band delivery -
     * interacts with handleLimeACKMessage
     * 
     * @param query A query packet we've received.
     * @param resps The Response objects that represent files we're sharing that match its search.
     * @return      True if this method stored the query and responses in the _outOfBandReplies Hashtable to send later.
     *              False if _outOfBandReplies is full, so we didn't keep it for later.
     */
    protected boolean bufferResponsesForLaterDelivery(QueryRequest query, Response[] resps) {

        /*
         * store responses by guid for later retrieval
         */

        // Only let one thread access the _outOfBandReplies HashTable at a time
        synchronized (_outOfBandReplies) {

            // The _outOfBandReplies list has less than 250 objects in it
            if (_outOfBandReplies.size() < MAX_BUFFERED_REPLIES) {

                // Make a new TimedGUID object with the query's message GUID and a time of 25 seconds
                GUID.TimedGUID tGUID = new GUID.TimedGUID( // Make a new TimedGUID object that will hold a GUID and an expiration time
                    new GUID(query.getGUID()),             // Copy the message GUID of the query packet to a new GUID
                    TIMED_GUID_LIFETIME);                  // Specify an expiration time of 25 seconds

                /*
                 * The _outOfBandReplies Hashtable looks like this:
                 * 
                 * Key                                    Value
                 * ---                                    -----
                 * TimedGUID                              QueryResponseBundle
                 *  ._guid     The query's message GUID    ._query             The query packet
                 *  .MAX_LIFE  25 seconds                  ._responses         An array of Response objects representing files we're sharing that match
                 */

                // Add the query's message GUID, 25 seconds, the query packet, and the array of Response objects to our Hashtable for out of band replies.
                _outOfBandReplies.put(                      // (3) Put them in the _outOfBandReplies Hashtable
                    tGUID,                                  // (2) List them under the TimedGUID with the query's message GUID and 25 seconds
                    new QueryResponseBundle(query, resps)); // (1) Bundle the QueryRequest packet and Response file hits together

                // Yes, we saved the given query and file hits to send later
                return true;
            }

            // No, we didn't have enough room
            return false;
        }
    }

    //do

    /**
     * Forwards the UDPConnectBack to neighboring peers
     * as a UDPConnectBackRedirect request.
     */
    protected void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp, Connection source) {

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new UDPConnectBackRedirect(guidToUse, sourceAddr, portToContact);

        int sentTo = 0;
        List peers = new ArrayList(_manager.getInitializedConnections());
        Collections.shuffle(peers);
        
        for (Iterator i = peers.iterator(); i.hasNext() && sentTo < MAX_UDP_CONNECTBACK_FORWARDS;) {
            
            ManagedConnection currMC = (ManagedConnection)i.next();
            
            if (currMC == source) continue;

            if (currMC.remoteHostSupportsUDPRedirect() >= 0) {
                
                currMC.send(msg);
                sentTo++;
            }
        }
    }

    /**
     * Sends a ping to the person requesting the connectback request.
     */
    protected void handleUDPConnectBackRedirect(UDPConnectBackRedirect udp, Connection source) {
        
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection()) return;

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress addrToContact = udp.getConnectBackAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact.getAddress(), portToContact);
        if (_manager.isConnectedTo(endPoint.getAddress())) return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        String addrString = addrToContact.getHostAddress();
        if (!shouldServiceRedirect(_udpConnectBacks,addrString)) return;

        PingRequest pr = new PingRequest(guidToUse.bytes(), (byte)1, (byte)0);
        UDPService.instance().send(pr, addrToContact, portToContact);
    }
    
    /**
     * @param map the map that keeps track of recent redirects
     * @param key the key which we would (have) store(d) in the map
     * @return whether we should service the redirect request
     * @modifies the map
     */
    private boolean shouldServiceRedirect(FixedsizeHashMap map, Object key) {
        
        synchronized (map) {
            
            Object placeHolder = map.get(key);
            
            if (placeHolder == null) {
                
                try {
                    
                    map.put(key, map);
                    return true;
                    
                } catch (NoMoreStorageException nomo) {
                    
                    return false;  // we've done too many connect backs, stop....
                }
                
            } else {
                
                return false;  // we've connected back to this guy recently....
            }
        }
    }

    /**
     * Forwards the request to neighboring Ultrapeers as a
     * TCPConnectBackRedirect message.
     */
    protected void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp, Connection source) {
        
        final int portToContact = tcp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new TCPConnectBackRedirect(sourceAddr, portToContact);

        int sentTo = 0;
        List peers = new ArrayList(_manager.getInitializedConnections());
        Collections.shuffle(peers);
        
        for (Iterator i = peers.iterator(); i.hasNext() && sentTo < MAX_TCP_CONNECTBACK_FORWARDS;) {
            
            ManagedConnection currMC = (ManagedConnection)i.next();
            if (currMC == source) continue;

            if (currMC.remoteHostSupportsTCPRedirect() >= 0) {
                
                currMC.send(msg);
                sentTo++;
            }
        }        
    }

    /**
     * Basically, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     */
    protected void handleTCPConnectBackRedirect(TCPConnectBackRedirect tcp, Connection source) {
        
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection()) return;

        final int portToContact = tcp.getConnectBackPort();
        final String addrToContact =tcp.getConnectBackAddress().getHostAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact, portToContact);
        if (_manager.isConnectedTo(endPoint.getAddress())) return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        if (!shouldServiceRedirect(_tcpConnectBacks,addrToContact)) return;

        TCP_CONNECT_BACKER.add(new Runnable() {
            
            public void run() {
                
                Socket sock = null;
                
                try {
                    
                    sock = Sockets.connect(addrToContact, portToContact, 12000);
                    OutputStream os = sock.getOutputStream();
                    os.write("CONNECT BACK\r\n\r\n".getBytes());
                    os.flush();
                    
                    if (LOG.isTraceEnabled()) LOG.trace("Succesful connectback to: " + addrToContact);
                    
                    try {
                        
                        Thread.sleep(500); // let the other side get it.
                        
                    } catch(InterruptedException ignored) {
                        
                        LOG.warn("Interrupted connectback", ignored);
                    }
                    
                } catch (IOException ignored) {
                    
                    LOG.warn("IOX during connectback", ignored);
                    
                } catch (Throwable t) {
                    
                    ErrorService.error(t);
                    
                } finally {
                    
                    IOUtils.close(sock);
                }
            }
        });
    }

    /**
     * 
     * 
     * 1) confirm that the connection is Ultrapeer to Leaf, then send your
     * listening port in a PushProxyAcknowledgement.
     * 2) Also cache the client's client GUID.
     * 
     * 
     */
    protected void handlePushProxyRequest(PushProxyRequest ppReq, ManagedConnection source) {
        
        if (source.isSupernodeClientConnection() && RouterService.isIpPortValid()) {
            
            String stringAddr = NetworkUtils.ip2string(RouterService.getAddress());
            InetAddress addr = null;
            
            try {
                
                addr = InetAddress.getByName(stringAddr);
                
            } catch (UnknownHostException uhe) {
                
                ErrorService.error(uhe); // impossible
            }

            // 1)
            PushProxyAcknowledgement ack = new PushProxyAcknowledgement(addr,RouterService.getPort(), ppReq.getClientGUID());
            source.send(ack);
            
            // 2)
            _pushRouteTable.routeReply(ppReq.getClientGUID().bytes(), source);
        }
    }

    //done

    /**
     * One of our leaves sent us a BEAR 12 1 QueryStatusResponse message, telling us how many hits it has for a search we're running on its behalf.
     * Saves the updated number in the QueryHandler object that represents the search.
     * 
     * This method should be invoked when this node receives a
     * QueryStatusResponse message from the wire.  If this node is an
     * Ultrapeer, we should update the Dynamic Querier about the status of
     * the leaf's query.
     * 
     * @param resp A BEAR 12 1 QueryStatusResponse vendor message one of our leaves sent us telling us how many hits it has gotten and kept for a search we're running for it 
     * @param leaf The ManagedConnection object that represents our connection down to the leaf, over which it sent us this packet
     */
    protected void handleQueryStatus(QueryStatusResponse resp, ManagedConnection leaf) {

        // Only do something if we're an ultrapeer and the remote computer that sent us this packet is one of our leaves
        if (!leaf.isSupernodeClientConnection()) return;

        // Read the 2 important pieces of information from the Query Status Response vendor message
        GUID queryGUID = resp.getQueryGUID();  // The message GUID, which identifies the search and is used by all the packets for this search
        int numResults = resp.getNumResults(); // The number of results the leaf says it has

        // Have the QueryDispatcher look up the QueryHandler for the search in its list, and save the updated hit count there
        DYNAMIC_QUERIER.updateLeafResultsForQuery(queryGUID, numResults); // Calls QueryDispatcher.updateLeafResultsForQuery()
    }

    /**
     * Send a ping to a remote computer, adding it to the RouteTable so the ForMeReplyHandler will get a pong response.
     * ConnectionWatchdog.killIfStillDud(List) uses this to ping connections that may have died.
     * 
     * @param request    A ping packet
     * @param connection The ManagedConnection to send it to
     */
    public void sendPingRequest(PingRequest request, ManagedConnection connection) {

        // Make sure the caller gave us a packet and connection
        if (request    == null) throw new NullPointerException("null ping");
        if (connection == null) throw new NullPointerException("null connection");

        // In our RouteTable for pings and pongs, list the ping's message GUID with the ForMeReplyHandler
        _pingRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER); // If we get a pong with the same GUID, we'll know it's a response for us

        // Send the ping to the remote computer
        connection.send(request);
    }

    /**
     * Not used.
     * 
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest request, ManagedConnection connection) {
        if (request    == null) throw new NullPointerException("null query");
        if (connection == null) throw new NullPointerException("null connection");
        _queryRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Send the given ping to 30% of our Gnutella connections.
     * Pinger.run() calls this every 3 seconds.
     * 
     * @param ping The ping packet we made, and will send
     */
    public void broadcastPingRequest(PingRequest ping) {

        // Make sure the Pinger gave us a ping
		if (ping == null) throw new NullPointerException("null ping");

        // List the ping's message GUID in our RouteTable for pings and pongs with the ForMeReplyHandler
        _pingRouteTable.routeReply(ping.getGUID(), FOR_ME_REPLY_HANDLER); // When we get a pong with the same GUID, we'll know to send it to the ForMeReplyHandler

        // Send the ping to 30% of our Gnutella connections
        broadcastPingRequest(ping, FOR_ME_REPLY_HANDLER, _manager);
    }

	/**
     * Make a new dynamic query for our own search.
     * Use this method when we're searching for our user at this computer, not on behalf of one of our leaves.
     * We can be a leaf or an ultrapeer.
     * 
     * Adds the query to our RouteTable for searches with the ForMeReplyHandler to catch the responses.
     * If we're an ultrapeer, wraps the query in a QueryHandler, and gives it to the QueryDispatcher.
     * If we're a leaf, sends the query packet up to our ultrapeers, who will search for us.
     * 
     * DownloadManager.sendQuery() and RouterService.recordAndSendQuery() call this.
     * 
	 * Generates a new dynamic query.  This method is used to send a new
	 * dynamic query from this host (the user initiated this query directly,
	 * so it's replies are intended for this node.
	 * 
     * @param query The query packet to search with
	 */
	public void sendDynamicQuery(QueryRequest query) {

        // Make sure the caller gave us a query packet
		if (query == null) throw new NullPointerException("null QueryHandler");

        // Add the query's message GUID to our RouteTable for queries and query hits
		ResultCounter counter =          // Get the RouteTableEntry object entered in the RouteTable, we can call counter.getNumResults() on it to see how many hits we've routed back
            _queryRouteTable.routeReply( // Add the search to our RouteTable for searches
                query.getGUID(),         // Store it in the route table under the query packet's message GUID, this identifies the search
                FOR_ME_REPLY_HANDLER);   // Use the ForMeReplyHandler object in place of a ManagedConnection or UDPReplyHandler, we want the hits, this search is for us

        // We're an ultrapeer
		if (RouterService.isSupernode()) {

            // Wrap the query packet and RouteTableEntry into a QueryHandler, and give it to the QueryDispatcher
			sendDynamicQuery(                                    // (2) Call QueryDispatcher.addQuery(qh) to give the QueryHandler object to the QueryDispatcher
                QueryHandler.createHandlerForMe(query, counter), // (1) Make a new QueryHandler to keep the given objects together, and set a goal of 172 hits
                FOR_ME_REPLY_HANDLER);                           // Not used, we specified the ForMeReplyHandler object above

        // We're a leaf
		} else {

            // Send the query packet up to our ultrapeers, who will search for us
            originateLeafQuery(query); // If we can get UDP packets, we've already set 0x04 in the query's speed flags bytes
		}

        // Turn the query into a multicast query, and send it over UDP multicast on the LAN
		multicastQueryRequest(QueryRequest.createMulticastQuery(query));
	}

	/**
     * Pass the given QueryHandler object to the QueryDispatcher, initiating a dynamic query.
     * Calls QueryDispatcher.addQuery(qh) to pass the given QueryHandler to the program's QueryDispatcher object.
     * 
	 * Initiates a dynamic query.  Only Ultrapeer should call this method,
	 * as this technique relies on fairly high numbers of connections to
	 * dynamically adjust the TTL based on the number of results received,
	 * the number of remaining connections, etc.
	 * The QueryHandler instance generates queries for this dynamic query.
     * 
     * @param qh      A QueryHandler object made with the query packet we received, the computer to give replies, and the RouteTableEntry for it in the RouteTable
     * @param handler Not used
	 */
	private void sendDynamicQuery(QueryHandler qh, ReplyHandler handler) {

        // Make sure the passed objects aren't null
		if      (qh      == null) throw new NullPointerException("null QueryHandler");
		else if (handler == null) throw new NullPointerException("null ReplyHandler");

        // Give the QueryHandler object to the QueryDispatcher, initiating the dynamic query
		DYNAMIC_QUERIER.addQuery(qh);
	}

    /**
     * Sends the given ping to 30% of our Gnutella connections.
     * Only broadcastPingRequest(PingRequest) calls this.
     * 
     * @param request             The ping we made to send to all our connections
     * @param receivingConnection The ForMeReplyHandler, which we've placed in the RouteTable for pings and pongs to notice when pongs with the same GUID come back
     * @param manager             The program's ConnectionManager object which keeps a list of all our TCP socket Gnutella connections
     */
    private void broadcastPingRequest(PingRequest request, ReplyHandler receivingConnection, ConnectionManager manager) {

        /*
         * Note the use of initializedConnections only.
         * Note that we have zero allocations here.
         */

        /*
         * Broadcast the ping to other connected nodes (supernodes or older
         * nodes), but DON'T forward any ping not originating from me
         * along leaf to ultrapeer connections.
         */

        // Get a list of the remote computers with which we've completed the Gnutella handshake and are exchanging Gnutella packets
        List list = manager.getInitializedConnections(); // A List of ManagedConnection objects
        int size = list.size();                          // The number of connections in the list

        // If we have more than 3 connections, we'll randomly ping just some of them
        boolean randomlyForward = false;
        if (size > 3) randomlyForward = true; // This method only runs when were an ultrapeer, so we should have many more than 3 connections

        // Loop for each connection we could ping
        double percentToIgnore; // We'll set this to 70% or 90%
        for (int i = 0; i < size; i++) {
            ManagedConnection mc = (ManagedConnection)list.get(i);

            // If we haven't been exchanging Gnutella packets with this remote computer for 5 seconds yet, don't ping it
            if (!mc.isStable()) continue;

            // receivingConnection is FOR_ME_REPLY_HANDLER
            if (receivingConnection == FOR_ME_REPLY_HANDLER || (mc != receivingConnection && !mc.isClientSupernodeConnection())) {

                // Set the probability we'll ping this packet based on whether it supports pong caching or not
                if (mc.supportsPongCaching()) percentToIgnore = 0.70; // It caches pongs and won't broadcast our ping forward, set a 30% chance we'll ping it
                else                          percentToIgnore = 0.90; // It doesn't cache pongs and will broadcast our ping forward, set a lower 10% chance we'll ping it

                // If we're randomly skipping computers and we select this one for skipping
                if (randomlyForward &&  // If we're randomly skipping computers, and
                    (Math.random() <    // A random number from 0.0 to 1.0 is less than
                    percentToIgnore)) { // The 70% or 90% we chose above for this remote computer

                    // Skip this one
                    continue;

                // We're not skipping computers, or this one made the 30% or 10% chance
                } else {

                    // Send the ping to the computer
                    mc.send(request);
                }
            }
        }
    }

	/**
     * Forwards a given query packet to our leaves.
     * 
     * If a leaf's query route table blocks the search, doesn't send it.
     * If the search makes it through 80% of the query route tables, only sends it to a quarter of our leaves.
     * 
     * @param query   A query packet we received
     * @param handler The remote compuer that sent it to us
     */
	public final void forwardQueryRequestToLeaves(QueryRequest query, ReplyHandler handler) {

        // Only do this if we're an ultrapeer
		if (!RouterService.isSupernode()) return;

        /*
         * use query routing to route queries to client connections
         * send queries only to the clients from whom query routing
         * table has been received
         */

        // Get a list of all of our leaves
        List list = _manager.getInitializedClientConnections(); // A List of ManagedConnection objects that are leaves we've completed the Gnutella handshake with

        // Make a list to hold the leaves that don't have query route tables that block this search
        List hitConnections = new ArrayList();

        // Loop for each of our leaves
        for (int i = 0; i < list.size(); i++) {
            ManagedConnection mc = (ManagedConnection)list.get(i);

            // This leaf sent us the query packet
            if (mc == handler) continue; // Go to the next leaf

            /*
             * Tour Point
             * 
             * This is where we use QRP, the Query Routing Protocol and QRP tables.
             * ManagedConnection.shouldForwardQuery(query) sees if the search passes through the leaf's QRP table.
             */

            // If the query passes through this leaf's query route table
            if (mc.shouldForwardQuery(query)) {

                // Add it to the list of leaves we'll send it to
                hitConnections.add(mc);
            }
        }

        /*
         * forward only to a quarter of the leaves in case the query is
         * very popular.
         */

        // If the search is popular, randomly choose 1/4th of the leaves in the hitConnections list to only send the query to them
        if (list.size() > 8 &&                                          // We have more than 8 leaves, and
            (double)hitConnections.size() / (double)list.size() > .8) { // The search made it through more than 80% of the query route tables

            // Choose a random starting index from the start to 3/4ths of the way to the end of the array
        	int startIndex =
                (int)Math.floor(               // (3) Math.floor rounds up to the next highest number
                Math.random() *                // (2) Math.random() returns a random number from 0.0 to 1.0, move the starting index back to a random location
                hitConnections.size() * 0.75); // (1) Choose the maximum possible starting index 3/4ths of the way into the hitConnections array

            // Crop the hitConnections list to be 1/4th its original size, starting from the randomly selected index
            hitConnections = hitConnections.subList(startIndex, startIndex + hitConnections.size() / 4);
        }

        // Calculate how many leaves we're skipping, and record that number in statistics
        int notSent = list.size() - hitConnections.size();
        RoutedQueryStat.LEAF_DROP.addData(notSent);

        // Loop through the leaves we chose to forward the query to
        for (int i = 0; i < hitConnections.size(); i++) {
            ManagedConnection mc = (ManagedConnection)hitConnections.get(i);

            /*
             * sendRoutedQueryToHost is not called because
             * we have already ensured it hits the routing table
             * by filling up the 'hitsConnection' list.
             */

            // Send the query packet to this leaf
            mc.send(query);
            RoutedQueryStat.LEAF_SEND.incrementStat();
        }
	}

	/**
	 * See if a query makes it through an ultrapeer's QRP table, and send it if it does.
	 * Only forwardQueryToUltrapeer() calls this method.
	 * 
	 * @param query   The query packet to send
	 * @param mc      The ultrapeer to send it to
	 * @param handler Not used
	 * @return        True if we sent it, false if we didn't
	 */
	private boolean sendRoutedQueryToHost(QueryRequest query, ManagedConnection mc, ReplyHandler handler) {

		// If the search makes it through the ultrapeer's QRP table
		if (mc.shouldForwardQuery(query)) {

			// Send it the query packet and report we sent it
			mc.send(query);
			return true;
		}

		// The search was stopped by the ultrapeer's QRP table, we didn't send it
		return false;
	}

    /**
     * Not used.
     * 
     * Adds the QueryRequest to the unicaster module.  Not much work done here,
     * see QueryUnicaster for more details.
     */
    protected void unicastQueryRequest(QueryRequest query, ReplyHandler conn) {
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
		UNICASTER.addQuery(query, conn);
	}

    /**
     * Send the given query packet over multicast UDP to the computers on our LAN.
     * 
     * @param query A query packet
     */
    protected void multicastQueryRequest(QueryRequest query) {

        // Set the TTL on outgoing UDP queries to 1
		query.setTTL((byte)1);

        // Count this multicast query request in statistics
		SentMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(query);

        // Have the MulticastService sent it
		MulticastService.instance().send(query);
	}

    /**
     * Forward a query message we received to all our ultrapeers.
     * handleQueryRequest() uses this method when we get a query from a modern ultrapeer.
     * It sends the query to each of our fellow ultrapeers, using QRP to block pointless searches on the last hop.
     * 
     * Broadcasts the query request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated QueryReplyHandler.  This is called from the default
     * handleQueryRequest and the default broadcastQueryRequest(QueryRequest)
     * 
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     * 
     * @param query   The query message to send.
     * @param handler The computer that we got the query message from.
     *                Used to make sure that we don't send the query back to the computer that gave it to us.
     */
    private void forwardQueryToUltrapeers(QueryRequest query, ReplyHandler handler) {

    	/*
		 * Note the use of initializedConnections only.
		 * Note that we have zero allocations here.
		 * 
		 * Broadcast the query to other connected nodes (ultrapeers or older
		 * nodes), but DON'T forward any queries not originating from me
		 * along leaf to ultrapeer connections.
    	 */

    	// Loop for each ultrapeer we're connected to
		List list = _manager.getInitializedConnections();
        int limit = list.size();
		for (int i = 0; i < limit; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);

			// Send the query to the remote computer, making sure it's not being sent back and using QRP on the last hop
            forwardQueryToUltrapeer(query, handler, mc);
        }
    }

    /**
     * Forward a query message we received from an old Gnutella program to up to 15 of our ultrapeers.
     * handleQueryRequest() uses this method instead of the one above when we get a query from an older ultrapeer.
     * It sends the query to up to 15 of our fellow ultrapeers, and tries to find 15 old ones.
     * 
     * Performs a limited broadcast of the specified query.  This is
     * useful, for example, when receiving queries from old-style
     * connections that we don't want to forward to all connected
     * Ultrapeers because we don't want to overly magnify the query.
     * 
     * @param query   The query message to send.
     * @param handler The computer that we got the query message from.
     *                Used to make sure that we don't send the query back to the computer that gave it to us.
     */
    private void forwardLimitedQueryToUltrapeers(QueryRequest query, ReplyHandler handler) {

    	/*
		 * Broadcast the query to other connected nodes (ultrapeers or older
		 * nodes), but DON'T forward any queries not originating from me
		 * along leaf to ultrapeer connections.
    	 */

    	// Loop for each ultrapeer we're connected to
		List list = _manager.getInitializedConnections();
        int limit = list.size();
        int connectionsNeededForOld = OLD_CONNECTIONS_TO_USE; // 15, we'll stop after send the query to 15 ultrapeers
		for (int i = 0; i < limit; i++) {

			// If we've already sent this query to 15 of our ultrapeers, leave the loop and the method
            if (connectionsNeededForOld == 0) break;

            // Get the next ultrapeer in the list
			ManagedConnection mc = (ManagedConnection)list.get(i);

			/*
             * if the query is comiing from an old connection, try to
             * send it's traffic to old connections.  Only send it to
             * new connections if we only have a minimum number left
			 */

			// If this destination ultrapeer is modern, and we have more ultrapeers to try, don't send it to this one
			if (mc.isGoodUltrapeer() &&    // The ultrapeer we're going to send it to supports advanced Gnutella features, and
            	(limit - i) >              // The number of ultrapeers we have left in our list is greater than
            	connectionsNeededForOld) { // The number of ultrapeers we still have to send the query to in our first 15

            	// Skip this ultrapeer, going to the next one
                continue;
            }

            // Send the query to the remote computer, making sure it's not being sent back and using QRP on the last hop
            forwardQueryToUltrapeer(query, handler, mc);

            // Count we have one less ultrapeer to send it to in our initial count of 15 of them
            connectionsNeededForOld--;
		}
    }

    /**
     * Send a query message to an ultrapeer, checking the ultrapeer's QRP table if this is the query message's last hop.
     * We are also an ultrapeer, this method is for broadcasting query messages forward between ultrapeers on the Gnutella network.
     * 
     * Checks the following things before sending the message:
     * Makes sure the destination ultrapeer isn't where we got the message from.
     * Makes sure we aren't a leaf.
     * If the query is a What's New search, makes sure the destination ultrapeer understands them.
     * If the query has just 1 hop left, makes sure the search isn't stopped by the ultrapeer's QRP table.
     * 
     * Only the 2 methods above call this one.
     * 
     * Forwards the specified query to the specified Ultrapeer.  This
     * encapsulates all necessary logic for forwarding queries to
     * Ultrapeers, for example handling last hop Ultrapeers specially
     * when the receiving Ultrapeer supports Ultrapeer query routing,
     * meaning that we check it's routing tables for a match before sending
     * the query.
     * 
     * @param query     The query message to send.
     * @param handler   The computer that we got the query message from.
     *                  Used to make sure that we don't send the query back to the computer that gave it to us.
     * @param ultrapeer The remote ultrapeer to send the query message to.
     */
    private void forwardQueryToUltrapeer(QueryRequest query, ReplyHandler handler, ManagedConnection ultrapeer) {

    	// Don't forward the query message to the remote computer that sent it to us
        if (ultrapeer == handler) return; // The references point to the same object, return without doing anything

        // If we are a leaf beneath the given ultrapeer, we shouldn't be forwarding query messages at all
        if (ultrapeer.isClientSupernodeConnection()) return;

        // If this is a What's New search, make sure the ultrapeer understands them
        if (query.isFeatureQuery() &&                         // The query message is a What's New search, and
        	!ultrapeer.getRemoteHostSupportsFeatureQueries()) // The ultrapeer didn't say "WHAT" in the Capabilities vendor message
        	return;                                           // It can't understand What's New searches, don't send it this one

        // Determine if the query can just travel one more hop
		boolean lastHop = query.getTTL() == 1; // If so, the ultrapeer we send it to won't send it any farther

		// If this is the last hop, we can use the ultrapeer's QRP table to see if the ultrapeer and it's leaves might have a hit for it
        if (lastHop && ultrapeer.isUltrapeerQueryRoutingConnection()) {

        	// See if the query makes it through the ultrapeer's QRP table, and send it if it does
            boolean sent = sendRoutedQueryToHost(query, ultrapeer, handler); // Doesn't use handler

            // Record what happened in statistics
            if (sent) RoutedQueryStat.ULTRAPEER_SEND.incrementStat(); // The search made it through the QRP table so we sent it
            else      RoutedQueryStat.ULTRAPEER_DROP.incrementStat(); // The search was blocked by the QRP table so we didn't send it

        // If the query has more hops, the QRP table can't help us
        } else {

        	// Just send it to the ultrapeer
            ultrapeer.send(query);
        }
    }

    /**
     * Send the given query packet up to our ultrapeers, who will search with it for us.
     * 
     * Only sendDynamicQuery() calls this.
     * We're a leaf, and our user has searched for something.
     * If we can get UDP packets, we've adjusted the given query packet to request out of band results.
     * The query packet's message GUID has our IP address inside, and is marked with 0x04 in the speed flags byte.
     * Hit computers will send query hit packets directly back to us, the leaf.
     * originateLeafQuery() sends the query packet up to several of our ultrapeers, which perform the dynamic query for us.
     * We'll tell them how many hits we've gotten with BEAR 12 1 QueryStatusResponse vendor messages.
     * 
     * Originate a new query from this leaf node.
     * 
     * @param qr The query packet to send to our ultrapeers
     */
    private void originateLeafQuery(QueryRequest qr) {

        // Get a list of our connections up to ultrapeers
		List list = _manager.getInitializedConnections();

        /*
         * only send to at most 4 Ultrapeers, as we could have more
         * as a result of race conditions - also, don't send what is new
         * requests down too many connections
         */

        // Set the number of ultrapeers we'll send our search to as 3, unless it's a What's New search, then make it just 2
        final int max = qr.isWhatIsNewRequest() ? 2 : 3; // Look for GGEP "WH" in the query packet, making this a What's New search

        // Choose what index in the list to start at
        int start =
            !qr.isWhatIsNewRequest() ?                            // Look for GGEP "WH" in the query packet, making this a What's New search
            0 :                                                   // Normal search, start at the beginning
            (int)(Math.floor(Math.random() * (list.size() - 1))); // What's New search, randomly choose an index in the list to start at

        // Set limit to the smallest one, max or the number of ultrapeers in our list
        int limit = Math.min(max, list.size());

        // Determine if the given query packet is marked to request out of band replies
        final boolean wantsOOB = qr.desiresOutOfBandReplies(); // Look for 0x04 in the speed flags bytes

        // Loop through our list of ultrapeers, from start but within limit
        for (int i = start; i < start + limit; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i); // Get the ManagedConnection object that represents this ultrapeer

            // If the query packet wants out of band replies, but the ultrapeer we're about to send it to can't do dynamic querying, have it ask for in band hits instead
			QueryRequest qrToSend = qr;
            if (wantsOOB &&                                 // The query packet wants out of band replies, it has 0x04 set in its speed flags bytes, and
                (mc.remoteHostSupportsLeafGuidance() < 0))  // The ultrapeer we're about to send it to doesn't support BEAR 11 1
                qrToSend = QueryRequest.unmarkOOBQuery(qr); // Remove the 0x04 out of band UDP bit, returns a copy of the QueryRequest object

            // Send the query to the ultrapeer
            mc.send(qrToSend);
        }
    }

    /**
     * Send the given query packet to the given remote computer.
     * 
     * Makes sure we're not sending a What's New search to a computer that doesn't support them.
     * Sets query.originated to true so the sacrifice algorithm doesn't kill it.
     * Leads to the call mc.send(query).
     * 
     * @param query A query packet
     * @param mc    The remote computer to send it to
     * @return      True if we sent the query packet, false if we didn't because it's a What's New search and the computer doesn't support them
     */
    public boolean originateQuery(QueryRequest query, ManagedConnection mc) {

        // Make sure the given objects aren't null
        if (query == null) throw new NullPointerException("null query");
        if (mc    == null) throw new NullPointerException("null connection");

        /*
         * if this is a feature query & the other side doesn't
         * support it, then don't send it
         * This is an optimization of network traffic, and doesn't
         * necessarily need to exist.  We could be shooting ourselves
         * in the foot by not sending this, rendering Feature Searches
         * inoperable for some users connected to bad Ultrapeers.
         */

        // If the given query packet is a What's New search and the remote computer we're about to send it to doesn't support them, leave without doing anything
        if (query.isFeatureQuery() &&                  // This is a What's New search, the GGEP block has "WH" in it, and
            !mc.getRemoteHostSupportsFeatureQueries()) // This remote computer doesn't support What's New search, it's Capabilities vendor message didn't mention "WHAT"
            return false;                              // Don't send it

        // Send the query packet to the remote computer, and return true
        mc.originateQuery(query); // Set query.originated to true first so the sacrifice algorithm doesn't kill it
        return true;
    }

    /*
     * Respond to the ping request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendPingReply(PingReply).
     * This method is called from the default handlePingRequest.
     */

    // Implemented in StandardMessageRouter.respondToPingRequest()
    protected abstract void respondToPingRequest(PingRequest request, ReplyHandler handler);

	/*
	 * Responds to a ping received over UDP -- implementations
	 * handle this differently from pings received over TCP, as it is 
	 * assumed that the requester only wants pongs from other nodes
	 * that also support UDP messaging.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param addr the <tt>InetSocketAddress</tt> containing the ping
     * @param handler the <tt>ReplyHandler</tt> instance from which the
     *  ping was received and to which pongs should be sent
	 */

    // Implemented in StandardMessageRouter.respondToUDPPingRequest()
    protected abstract void respondToUDPPingRequest(PingRequest request, InetSocketAddress addr, ReplyHandler handler);

    /*
     * Respond to the query request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is called from the default handleQueryRequest.
     */

    // Implemented in StandardMessageRouter.respondToQueryRequest()
    protected abstract boolean respondToQueryRequest(QueryRequest queryRequest, byte[] clientGUID, ReplyHandler handler);

    /**
     * Handle a pong we've received through TCP or UDP.
     * Adds the pong to the HostCatcher and the PongCacher.
     * Identifies pong responses to pings we created and sent, and gives them to the ForMeReplyHandler.
     * Sends the pong to all our leaves.
     * 
     * handleMessage() calls this when a remote computer sends us a pong through a TCP Gnutella connection.
     * handleUDPPingReply() calls this when a remote computer sends us a pong with UDP.
     * 
     * @param reply   A pong we've just received through TCP or UDP
     * @param handler The remote computer that sent it to us
     */
    protected void handlePingReply(PingReply reply, ReplyHandler handler) {

        // Have the HostCatcher add all the IP addresses and port numbers in the pong to the list it keeps
        boolean newAddress = RouterService.getHostCatcher().add(reply); // Returns true if there were some addresses it didn't have yet

        // If the pong contains some addresses the HostCatcher hadn't heard about yet, and the pong isn't about a UDP host cache
        if (newAddress && !reply.isUDPHostCache()) { // The pong doesn't have the "UDPHC" extension

            // Add the pong we received to the PongCacher
            PongCacher.instance().addPong(reply);
        }

        // Look up which Gnutella connection or UDP address sent us a ping with the same message GUID
        ReplyHandler replyHandler = _pingRouteTable.getReplyHandler(reply.getGUID());
        if (replyHandler != null) { // We found one

            /*
             * LimeWire no longer broadcasts pings forward.
             * It only lists pings we send in the _pingRouteTable, along with the ForMeReplyHandler.
             * So, control will only reach here when we've received a pong response to our ping.
             * And handler will always be FOR_ME_REPLY_HANDLER.
             */

            // Have the computer the pong is meant for take it
            replyHandler.handlePingReply(reply, handler);

        // We received a pong with a GUID that doesn't match any ping GUIDs we know about
        } else {

            // Count the error, but keep going in this method
            RouteErrorStat.PING_REPLY_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }

        // Determine if the remote computer the pong describes can receive UDP packets
		boolean supportsUnicast = reply.supportsUnicast(); // It has the "GUE" extension, meaning it can

        /*
         * Then, if a marked pong from an Ultrapeer that we've never seen before,
         * send to all leaf connections except replyHandler (which may be null),
         * irregardless of GUID.  The leafs will add the address then drop the
         * pong as they have no routing entry.  Note that if Ultrapeers are very
         * prevalent, this may consume too much bandwidth.
         * Also forward any GUESS pongs to all leaves.
         */

        // If this pong was new to us, send it to our leaves
        if (newAddress &&           // This pong has IP addresses the HostCatcher hadn't heard of yet, and
            (reply.isUltrapeer() || // Either the pong describes an ultrapeer, which should always be the case, or
            supportsUnicast)) {     // The pong describes a remote computer externally contactable for UDP

            // Loop through our leaves
            List list = _manager.getInitializedClientConnections();
            for (int i = 0; i < list.size(); i++) {
                ManagedConnection c = (ManagedConnection)list.get(i);
                Assert.that(c != null, "null c.");

                // If we haven't ponged this leaf recently, do it now
                if (c != handler      && // This leaf isn't the computer that sent us the pong
                    c != replyHandler && // It also isn't the computer that sent us the ping
                    c.allowNewPongs()) { // We haven't ponged this leaf of ours for at least 12 seconds

                    // Send the pong to the leaf
                    c.handlePingReply(reply, handler);
                }
            }
        }
    }

    //do

    /**
     * The default handler for QueryReplies received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the query route table to route a query reply.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the QueryReply count is incremented.<p>
     *
     * Override as desired, but you probably want to call super.handleQueryReply
     * if you do.  This is public for testing purposes.
     */
    public void handleQueryReply(QueryReply queryReply, ReplyHandler handler) {
        
        if (queryReply == null) throw new NullPointerException("null query reply");
        if (handler == null) throw new NullPointerException("null ReplyHandler");
        
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(
                queryReply.getGUID(),
                queryReply.getTotalLength(),
				queryReply.getResultCount());

        if (rrp != null) {
            
            queryReply.setPriority(rrp.getBytesRouted());
            
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            
            _pushRouteTable.routeReply(queryReply.getClientGUID(), handler);
            
            //Simple flow control: don't route this message along other
            //connections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.

            ReplyHandler rh = rrp.getReplyHandler();

            if (!shouldDropReply(rrp, rh, queryReply)) {
                
                rh.handleQueryReply(queryReply, handler);
                // also add to the QueryUnicaster for accounting - basically,
                // most results will not be relevant, but since it is a simple
                // HashSet lookup, it isn't a prohibitive expense...
                UNICASTER.handleQueryReply(queryReply);

            } else {
                
				RouteErrorStat.HARD_LIMIT_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
                final byte ttl = queryReply.getTTL();
                
                if (ttl < RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length) {
                    
                    RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[ttl].incrementStat();
                    
                } else {
                    
                    RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length - 1].incrementStat();
                }
                
                handler.countDroppedMessage();
            }
            
        } else {
            
			RouteErrorStat.NO_ROUTE_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
    }

    /**
     * Checks if the <tt>QueryReply</tt> should be dropped for various reasons.
     *
     * Reason 1) The reply has already routed enough traffic.  Based on per-TTL
     * hard limits for the number of bytes routed for the given reply guid.
     * This algorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits have more liberal limits than high TTL
     * hits.  This ensures that hits that are closer to the query originator
     * -- hits for which we've already done most of the work, are not 
     * dropped unless we've routed a really large number of bytes for that
     * guid.  This method also checks that hard number of results that have
     * been sent for this GUID.  If this number is greater than a specified
     * limit, we simply drop the reply.
     *
     * Reason 2) The reply was meant for me -- DO NOT DROP.
     *
     * Reason 3) The TTL is 0, drop.
     *
     * @param rrp the <tt>ReplyRoutePair</tt> containing data about what's 
     *  been routed for this GUID
     * @param ttl the time to live of the query hit
     * @return <tt>true if the reply should be dropped, otherwise <tt>false</tt>
     */
    private boolean shouldDropReply(RouteTable.ReplyRoutePair rrp, ReplyHandler rh, QueryReply qr) {
        
        int ttl = qr.getTTL();
                                           
        // Reason 2 --  The reply is meant for me, do not drop it.
        if (rh == FOR_ME_REPLY_HANDLER) return false;
        
        // Reason 3 -- drop if TTL is 0.
        if (ttl == 0) return true;                

        // Reason 1 ...
        
        int resultsRouted = rrp.getResultsRouted();

        // drop the reply if we've already sent more than the specified number
        // of results for this GUID
        if(resultsRouted > 100) return true;

        int bytesRouted = rrp.getBytesRouted();
        
        // send replies with ttl above 2 if we've routed under 50K 
        if(ttl > 2 && bytesRouted < 50    * 1024) return false;
        
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && bytesRouted < 200 * 1024) return false;
        
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && bytesRouted < 100  * 1024) return false;

        // if none of the above conditions holds true, drop the reply
        return true;
    }

    private void handleGiveStats(final GiveStatsVendorMessage gsm, final ReplyHandler replyHandler) {
        
        StatisticVendorMessage statVM = null;
        
        try {
            
            //create the reply if we understand how
            if (StatisticVendorMessage.isSupported(gsm)) {
                
                statVM = new StatisticVendorMessage(gsm);
                //OK. Now send this message back to the client that asked for
                //stats
                replyHandler.handleStatisticVM(statVM);
            }
            
        } catch (IOException iox) {
            
            return; //what can we really do now?
        }
    }

    private void handleStatisticsMessage(final StatisticVendorMessage svm, final ReplyHandler handler) {
        
        if (StatisticsSettings.RECORD_VM_STATS.getValue()) {
            
            Thread statHandler = new ManagedThread("Stat writer ") {
                
                public void managedRun() {
                    
                    RandomAccessFile file = null;
                    
                    try {
                        
                        file = new RandomAccessFile("stats_log.log", "rw");
                        file.seek(file.length()); //go to the end.
                        file.writeBytes(svm.getReportedStats() + "\n");
                        
                    } catch (IOException iox) {
                        
                        ErrorService.error(iox);
                        
                    } finally {
                        
                        if (file != null) {
                            
                            try {
                                
                                file.close();
                                
                            } catch (IOException iox) {
                                
                                ErrorService.error(iox);
                            }
                        }
                    }
                }
            };
            
            statHandler.start();
        }
    }

    /**
     * If we get and SimppRequest, get the payload we need from the
     * SimppManager and send the simpp bytes the the requestor in a SimppVM. 
     */
    private void handleSimppRequest(final SimppRequestVM simppReq, final ReplyHandler handler ) {
        
        if (simppReq.getVersion() > SimppRequestVM.VERSION) return; //we are not going to deal with these types of requests. 
        byte[] simppBytes = SimppManager.instance().getSimppBytes();
        
        if (simppBytes != null) {
            
            SimppVM simppVM = new SimppVM(simppBytes);
            
            try {
                
                handler.handleSimppVM(simppVM);
                
            } catch (IOException iox) { //uanble to send the SimppVM. Nothing I can do
                
                return;
            }
        }
    }

    /**
     * Passes on the SimppVM to the SimppManager which will verify it and
     * make sure we it's newer than the one we know about, and then make changes
     * to the settings as necessary, and cause new CapabilityVMs to be sent down
     * all connections.
     */
    private void handleSimppVM(final SimppVM simppVM) {
        
        SimppManager.instance().checkAndUpdate(simppVM.getPayload());
    }

    /**
     * Handles an update request by sending a response.
     */
    private void handleUpdateRequest(UpdateRequest req, ReplyHandler handler ) {

        byte[] data = UpdateHandler.instance().getLatestBytes();
        if (data != null) {
            
            UpdateResponse msg = UpdateResponse.createUpdateResponse(data,req);
            handler.reply(msg);
        }
    }

    /**
     * Passes the request onto the update manager.
     */
    private void handleUpdateResponse(UpdateResponse resp, ReplyHandler handler) {
        
        UpdateHandler.instance().handleNewData(resp.getUpdate());
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
    protected void handlePushRequest(PushRequest request, ReplyHandler handler) {
        
        if (request == null) throw new NullPointerException("null request");
        if (handler == null) throw new NullPointerException("null ReplyHandler");

        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(request.getClientGUID());

        if (replyHandler != null) {
            
            replyHandler.handlePushRequest(request, handler);
            
        } else {
            
			RouteErrorStat.PUSH_REQUEST_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
    }

    //done

    /**
     * Look up the client ID GUID a push packet is addressed to in our push route table, and return the connected computer that it's for.
     * 
     * Takes the client ID that a push packet is addressed to.
     * Looks up the given client ID GUID in the _pushRouteTable, which has client ID GUIDs instead of message GUIDs like the other route tables.
     * If we're connected to a remote computer with the given client ID, returns the ReplyHandler object that represents it.
     * If the push packet is addressed to our client ID GUID, returns the ForMeReplyHandler object, which will have us take it and do the push.
     * This method does this even if the ForMeReplyHandler object isn't in the table.
     * 
     * (do)
     * This will get a push request to the computer we're connected to which needs to do the push
     * but, what routes the push on the hops before this final one
     * what trail does it follow back
     * 
     * @param guid The client ID GUID that a push request packet is addressed to
     * @return     The ReplyHandler object that represents the remote computer we've been exchanging packets with that has that client ID GUID
     */
    protected ReplyHandler getPushHandler(byte[] guid) {

        // Look up the GUID in our RouteTable for push packets
        ReplyHandler replyHandler = _pushRouteTable.getReplyHandler(guid); // The push route table uses client ID GUIDs, while the others use message GUIDs

        // We found our connection to the remote computer that has a client ID that matches the one in the push packet
        if (replyHandler != null) {

            // Return it
            return replyHandler; // A ManagedConnection or UDPReplyHandler object that represents a remote computer and can send the push packet to it

        // Not found, but the push packet is addressed to our client ID
        } else if (Arrays.equals(_clientGUID, guid)) {

            // Return the ForMeReplyHandler to have us process the packet and do the push
            return FOR_ME_REPLY_HANDLER;

        // No connection found, and not for us
        } else {

            // Return null, we can't route this packet
            return null;
        }
    }

    /**
     * Sends the given pong to the remote computer the ReplyHandler represents.
     * Calls handler.handlePingReply(pong).
     * If handler is actually a ManagedConnection, calls ManagedConnection.handlePingReply(pong).
     */
    protected void sendPingReply(PingReply pong, ReplyHandler handler) {

        // Make sure pong and handler aren't null
        if (pong    == null) throw new NullPointerException("null pong");
        if (handler == null) throw new NullPointerException("null reply handler");

        // Have the ReplyHandler send the pong to the computer it represents
        handler.handlePingReply(pong, null); // If handler is a ManagedConnection, calls managedConnection.handlePingReply(pong)
    }

    /**
     * Send a query hit packet we made back to the computer that sent us the query.
     * 
     * Here's how we find the computer to send our packet to:
     * We gave our query hit the same message GUID as the original query.
     * We listed the computer that sent us the query under this GUID in our RouteTable for queries.
     * 
     * Takes a query hit packet with information about us and our shared files that we've prepared in response to a query we received.
     * Looks up the message GUID in our RouteTable for queries and query hits.
     * Sends our query hit back to the computer that sent us the query.
     * 
     * @param  queryReply  A query hit packet with information about us and files we're sharing that we made in response to a query packet
     * @throws IOException Our RouteTable for queries and query hits doesn't have the message GUID, so we don't know what computer to send it do
     */
    protected void sendQueryReply(QueryReply queryReply) throws IOException {

        // Make sure the caller gave us a packet
        if (queryReply == null) throw new NullPointerException("null reply");

        /*
         * For flow control reasons, we keep track of the bytes routed for this
         * GUID.  Replies with less volume have higher priorities (i.e., lower
         * numbers).
         */

        // Look up the query's message GUID in the RouteTable for queries and query hits to see which computer to send our reponse back to
        RouteTable.ReplyRoutePair rrp =       // Returns a ReplyRoutePair object that keeps a ReplyHandler with transfer statistics
            _queryRouteTable.getReplyHandler( // Look up the message GUID in our RouteTable for queries and query hits
                queryReply.getGUID(),         // The message GUID of the query packet we received and the query hit packets we've composed in response

                // Give getReplyHandler() statistics about this packet
                queryReply.getTotalLength(),  // The number of additional bytes of reply packet data we're routing back because of the request
                queryReply.getResultCount()); // The number of additional reply packets we're routing back because of the request

        // We found the remote computer that sent us the query packet
        if (rrp != null) {

            // Use the number of byts of packet data we've already routed back for this search to set the priority
            queryReply.setPriority(rrp.getBytesRouted()); // A lower number means a higher priority

            // Send our query hit packet back to the remote computer that sent us the query
            rrp.getReplyHandler().handleQueryReply(queryReply, null);

        // Not found
        } else {

            // The GUID should have been in the RouteTable
            throw new IOException("no route for reply");
        }
    }

    //do

    /**
     * Uses the push route table to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPushRequest(PushRequest push) throws IOException {
        
        if (push == null) throw new NullPointerException("null push");

        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(push.getClientGUID());

        if (replyHandler != null) replyHandler.handlePushRequest(push, FOR_ME_REPLY_HANDLER);
        else                      throw new IOException("no route for push");
    }

    /**
     * Sends a push request to the multicast network.  No lookups are
     * performed in the push route table, because the message will always
     * be broadcast to everyone.
     */
    protected void sendMulticastPushRequest(PushRequest push) {
        
        if (push == null) throw new NullPointerException("null push");
        
        // must have a TTL of 1
        Assert.that(push.getTTL() == 1, "multicast push ttl not 1");
        
        MulticastService.instance().send(push);
        SentMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(push);
    }

    //done

    /**
     * Package given Response objects in groups of 10 into QueryReply packets.
     * Makes QueryReply packets with information about us, like our IP address.
     * 
     * @param responses    An array of Response objects describing the files we're sharing that match a query we've received
     * @param queryRequest A query packet we've received, and will compose query hits in reply
     * @return             An Iterator you can move over the QueryReply objects this method generated
     */
    public Iterator responsesToQueryReplies(Response[] responses, QueryRequest queryRequest) {

        // Call the next method, having it put the information from 10 Response objects in each QueryReply
        return responsesToQueryReplies(responses, queryRequest, 10);
    }

    /**
     * Package given Response objects in groups into QueryReply packets.
     * Makes QueryReply packets with information about us, like our IP address.
     * 
     * @param responses    An array of Response objects describing the files we're sharing that match a query we've received
     * @param queryRequest A query packet we've received, and will compose query hits in reply
     * @param REPLY_LIMIT  The maximum number of files to describe in each query hit packet, like 10, or just 1
     * @return             An Iterator you can move over the QueryReply objects this method generated
     */
    private Iterator responsesToQueryReplies(Response[] responses, QueryRequest queryRequest, final int REPLY_LIMIT) {

        // Make a List to hold the QueryReply objects we create
        List queryReplies = new LinkedList(); // We'll return an Iterator on the start of this list

        // Get information from the query packet for the query hit packets we'll generate
        byte[] guid = queryRequest.getGUID();          // Use the same GUID to route the query hits back to the computer that made the query
        byte ttl = (byte)(queryRequest.getHops() + 1); // Let the query hits travel back one more hop than it took them to get here

        // Get our measured upload speed if possible, or the speed the user set otherwise
        UploadManager um = RouterService.getUploadManager(); // Access the program's UploadManager object
        long speed = um.measuredUploadSpeed(); // Ask it how fast it's been able to upload files
        boolean measuredSpeed = true;          // Set 0x10 in the flags and controls bytes to 1, the upload speed is from real measured data
        if (speed == -1) {                     // It doesn't know
            speed = ConnectionSettings.CONNECTION_SPEED.getValue(); // Get the speed from settings
            measuredSpeed = false;             // Set 0x10 in the flags and controls bytes to 0, the upload speed is just from a setting the user entered
        }

        // Variables to move through the given array of Response objects
        int numResponses = responses.length; // The number of Response objects in the array
        int index = 0;                       // The one we're on

        /*
         * limit the responses if we're not delivering this
         * out-of-band and we have a lot of responses
         */

        // If we have to reply in band and have more than 10 responses, randomly choose only 10 to send
        int numHops = queryRequest.getHops();          // Find out how many hops the query packet made to get to us
        if (REPLY_LIMIT  > 1 &&                        // The caller is letting us put more than 1 response in each query hit packet, and
            numHops      > 2 &&                        // The query packet hopped twice to get here, it's in band and so our query hit will have to be in band too, and
            numResponses > HIGH_HOPS_RESPONSE_LIMIT) { // We've got more than 10 responses, the maximum we can send in band

            // Randomly pick 10 responses from the given array to send
            int j = (int)(Math.random() * numResponses) % (numResponses - HIGH_HOPS_RESPONSE_LIMIT); // Pick j, the index we'll grab them from
            Response[] newResponses = new Response[HIGH_HOPS_RESPONSE_LIMIT]; // Make an array for the 10 responses we'll take
            for (int i = 0; i < 10; i++, j++) { // Loop 10 times, moving i and j forward

                /*
                 * TODO1:kfaaborg Above, it should be i < HIGH_HOPS_RESPONSE_LIMIT, not the magic number 10, which is the current value
                 */

                // Pick a Response object from the given array
                newResponses[i] = responses[j];
            }

            // Save the shortened array of Response objects, and it's new length of 10
            responses = newResponses;
            numResponses = responses.length;
        }

        // Loop until we've loaded all the Response objects into QueryReply packets
        while (numResponses > 0) {

            // Grab the next group of up to 10 Response objects from responses[] in an array called res[]
            int arraySize;                                            // The number of responses we'll take in this group
            if (numResponses < REPLY_LIMIT) arraySize = numResponses; // If we have less than 10 left, take all of them
            else                            arraySize = REPLY_LIMIT;  // If we have more than 10, we'll just grab the first 10
            Response[] res;                                           // An array of the Response objects we're taking now
            if ((index == 0) && (arraySize < REPLY_LIMIT) ) {         // They all fit into the first bunch
                res = responses;                                      // Point res at responses, the aray of all the Response objects
            } else {                                                  // The first bunch was full
                res = new Response[arraySize];                        // Make a new array that can hold the group of Response objects
                for (int i = 0; i < arraySize; i++) {                 // Loop once for each Response we're going to take now
                    res[i] = responses[index];                        // Take one
                    index++;                                          // Move to the next one in the source array
                }
            }
            numResponses -= arraySize;                                // Record we have that many Response objects left

            // Ask the UploadManager if it has an open slot, and if it has ever actually uploaded a file
			boolean busy     = !um.isServiceable();     // Sets 0x04 in the flags and controls bytes, all our upload slots are full right now
            boolean uploaded = um.hadSuccesfulUpload(); // Sets 0x08 in the flags and controls bytes, we have actually uploaded a file

            // Determine if our query hits will be replies to a query sent over multicast UDP by a computer on the same LAN as us
            boolean mcast =                                            // Will add "MCAST" to our query hit, marking it the response to a multicast query
                queryRequest.isMulticast() &&                          // If the query packet came in from multicast, and
                (queryRequest.getTTL() + queryRequest.getHops()) == 1; // The query only traveled a single hop to reach us

            // Only advertise our ability to do firewall-to-firewall transfers if we and the searching computer can do it, and we aren't externally contactable for TCP
            final boolean fwTransfer =                       // Will add "FW" to our query hit, advertising our ability to do firewall-to-firewall transfers
                queryRequest.canDoFirewalledTransfer() &&    // In the query's speed flags bytes, 0x02 is set indicating the searching computer can do firewall-to-firewall, and
                UDPService.instance().canDoFWT()       &&    // We can do firewall-to-firewall transfers, and
                !RouterService.acceptedIncomingConnection(); // We can't accept an incoming TCP socket connection

            // If we're responding to a LAN multicast query and we're going to add the "MCAST" extension, also set the TTL to 1
			if (mcast) ttl = 1;

            /*
             * Include _clientGUID, our client ID GUID, in the QueryReply messages we create.
             * It will go into the last 16 bytes of the message.
             * If we're firewalled, this will let downloaders get a push request back to us.
             * When we get it, we'll push open a connection to them to deliver the file they want.
             */

            // Package our Response objects into QueryReply packets, grouping as many responses into each query hit as we can while keeping the XML under 32 KB
            List replies = createQueryReply( // Calls StandardMessageRouter.createQueryReply(), which returns an ArrayList of QueryReply objects
                guid,          // For the header, the message GUID
                ttl,           // For the header, the message TTL
                speed,         // For the payload, our upload speed
                res,           // The array of Response objects that each describe a file we're sharing that matches the search we received
                _clientGUID,   // Write our client ID GUID into the last 16 bytes of the query hit packet so downloaders can get a push request to us
                busy,          // Sets 0x04 in the flags and controls bytes, all our upload slots are full right now
                uploaded,      // Sets 0x08 in the flags and controls bytes, we have actually uploaded a file
                measuredSpeed, // Sets 0x10 in the flags and controls bytes, the upload speed is from real measured data, not just a setting the user entered
                mcast,         // Makes "MCAST" in the GGEP block, this query hit is responding to a multicast query
                fwTransfer);   // Makes "FW" in the GGEP block, we can do a firewall-to-firewall file transfer

            // Add it to the list
            queryReplies.addAll(replies);
        }

        // Return an Iterator the caller can use to move over the QueryReply objects we made and added to the queryReplies list
        return queryReplies.iterator();
    }

    /*
     * Abstract method for creating query hits.  Subclasses must specify
     * how this list is created.
     * 
     * @return a <tt>List</tt> of <tt>QueryReply</tt> instances
     */

    // Implemented in StandardMessageRouter.createQueryReply()
    protected abstract List createQueryReply(byte[] guid, byte ttl, long speed, Response[] res, byte[] clientGUID, boolean busy, boolean uploaded, boolean measuredSpeed, boolean isFromMcast, boolean shouldMarkForFWTransfer);

    /**
     * We've received a QRP reset table message.
     * A fellow ultrapeer or one of our leaves is telling us how big its QRP table is and what infinity it uses.
     * Save that information in the ManagedConnection object that represents the leaf, and remake our composite QRP table.
     * Only MessageRouter.handleMessage() calls this method.
     * 
     * @param rtm A QRP reset table message
     * @param mc  The remote computer that sent it to us
     */
    private void handleResetTableMessage(ResetTableMessage rtm, ManagedConnection mc) {

    	// Only do something if it's from one of our leaves, or a fellow ultrapeer that told us "X-Ultrapeer-Query-Routing: 0.1" in the handshake
        if (!isQRPConnection(mc)) return;

        // Save the table size and chosen infinity value in the ManagedConnection object that represents the remote computer
        synchronized (mc.getQRPLock()) {
            mc.resetQueryRouteTable(rtm);
        }

        /*
         * if this is coming from a leaf, make sure we update
         * our tables so that the dynamic querier has correct
         * data
         */

        // The remote computer that sent us the reset message is one of our leaves
        if (mc.isLeafConnection()) {

        	// Remake our QRP table, which shows what we and our leaves are sharing
            _lastQueryRouteTable = createRouteTable();
        }
    }

    /**
     * We've received a QRP patch table message.
     * A fellow ultrapeer or one of our leaves is giving us patches we can use to update our record of its QRP table.
     * Use them to bring our record of the remote computer's QRP table up to date, and remake our composite QRP table.
     * Only MessageRouter.handleMessage() calls this method.
     * 
     * @param ptm A QRP patch table message
     * @param mc  The remote computer that sent it to us
     */
    private void handlePatchTableMessage(PatchTableMessage ptm, ManagedConnection mc) {

    	// Only do something if it's from one of our leaves, or a fellow ultrapeer that told us "X-Ultrapeer-Query-Routing: 0.1" in the handshake
        if (!isQRPConnection(mc)) return;

        // Bring our record of the remote computer's QRP table up to date
        synchronized (mc.getQRPLock()) {
            mc.patchQueryRouteTable(ptm);
        }

        /*
         * if this is coming from a leaf, make sure we update
         * our tables so that the dynamic querier has correct
         * data
         */

        // The remote computer that sent us the patch message is one of our leaves
        if (mc.isLeafConnection()) {

        	// Remake our QRP table, which shows what we and our leaves are sharing
            _lastQueryRouteTable = createRouteTable();
        }
    }

    /**
     * Does nothing.
     * Only handleQueryRequest() calls this.
     * 
     * @param request A query packet we received
     * @param handler The remote computer that sent it to us
     */
    private void updateMessage(QueryRequest request, ReplyHandler handler) {

        // Only do something if we got the query packet through TCP, from a remote computer we have represented as a ManagedConnection object
        if (!(handler instanceof Connection)) return;

        // Only do something if the query came directly to us from LimeWire version 3.3 or earlier
        Connection c  = (Connection)handler;
        if (request.getHops() == 1 && c.isOldLimeWire()) {
            if (StaticMessages.updateReply == null) return;
            QueryReply qr = new QueryReply(request.getGUID(),StaticMessages.updateReply);
            try {
                sendQueryReply(qr);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Determine if a given remote computer should be sending us QRP messages.
     * Returns true if it's our leaf, or if it said "X-Ultrapeer-Query-Routing: 0.1" in the handshake.
     * 
     * @param c The ManagedConnection object that represents the remote computer we did the Gnutella handshake with
     * @return  true if we expect QRP messages from this computer, false if we don't
     */
    private static boolean isQRPConnection(Connection c) {

    	// If we are an ultrapeer and the remote computer is our leaf beneath us, yes, it should be sending us QRP messages
        if (c.isSupernodeClientConnection()) return true;

        // If the remote computer said "X-Ultrapeer-Query-Routing: 0.1", yes, it should be sending us QRP messages
        if (c.isUltrapeerQueryRoutingConnection()) return true;

        // Otherwise, the remote computer has made no promise to send us QRP messages.
        return false;
    }

    /**
	 * A thread that loops forever, calling forwardQueryRouteTables() every 10 seconds.
	 * The program's single MessageRouter object makes this one QRPPropagator object.
	 * QRPPropagator extends MangedThread, which extends Thread, so this is a thread.
     */
    private class QRPPropagator extends ManagedThread {

    	/**
    	 * Make the program's single QRPPropagator object.
    	 */
        public QRPPropagator() {

        	// Label this thread "QRPPropagator", and let Java exit even if it's still running
            setName("QRPPropagator");
            setDaemon(true);
        }

        /**
         * Loop forever, calling forwardQueryRouteTables() every 10 seconds.
         * When initialize() calls QRP_PROPAGATOR.start(), this managedRun() method gets called.
         */
        public void managedRun() {

            try {

            	// Loop forever
                while (true) {

                	// Wait here for 10 seconds
                	Thread.sleep(10 * 1000);

                	// Send our QRP table to all our ultrapeers that we haven't updated in the last minute
                	forwardQueryRouteTables();
                }

            // Pass an exception to the ErrorService, and let the thread exit
            } catch (Throwable t) { ErrorService.error(t); }
        }
    }

    /**
     * Send a group of QRP messages to all our ultrapeers to bring their records of our QRP table up to date.
     * Prepares a custom group of QRP messages for each ultrapeer that we haven't updated in the last minute, and sends them to it.
     * 
     * Only QRPPropagator.managedRun() above calls this method.
     * The "QRPPropagator" thread calls here every 10 seconds as the program runs.
     * 
     * Sends updated query routing tables to all connections which haven't
     * been updated in a while.  You can call this method as often as you want;
     * it takes care of throttling.
     */
    private void forwardQueryRouteTables() {

		// Check the time to decide if it needs an update.
		long time = System.currentTimeMillis();

		// Get the list of all the ultrapeers we're connected to
		List list = _manager.getInitializedConnections();

		// Our current QRP table
		QueryRouteTable table = null;

		// The QRP messages we'll have to send a remote computer to bring their record of our QRP table up to date
		List patches = null; // A List of RouteTableMessage objects

		// A remote computer's out of date version of our QRP table
		QueryRouteTable lastSent = null;

		// Loop for each ultrapeer we're connected to
		for (int i = 0; i < list.size(); i++) {
			ManagedConnection c = (ManagedConnection)list.get(i);

			// Skip this ultrapeer if it can't do QRP
			if (RouterService.isSupernode()) {                // If we're an ultrapeer, and
				if (!c.isUltrapeerQueryRoutingConnection()) { // This remote ultrapeer didn't tell us "X-Ultrapeer-Query-Routing: 0.1" in the handshake
					continue;                                 // Go to the start of the loop to get the next ultrapeer
				}
			} else if (!(                          // Otherwise, make sure that we're a leaf and c, our ultrapeer above us, supports QRP
				c.isClientSupernodeConnection() && // We're a leaf and c is our ultrapeer above us, and
				c.isQueryRoutingEnabled())) {      // It said it can accept our QRP table
				continue;                          // Either of those weren't true, go to the start of the loop to get the next ultrapeer
			}

			// Only send an ultrapeer our QRP table once every minute
			if (time < c.getNextQRPForwardTime()) continue; // We haven't waited long a minute yet, skip this ultrapeer and go to the next one
			c.incrementNextQRPForwardTime(time);            // It's been more than a minute, set the next time to 1 minute from now

			// Make our current QRP table
			if (table == null) {
				table = createRouteTable();
                _lastQueryRouteTable = table; // Keep a record of our last QRP table we've sent
			}

			/*
			 * Because we tend to send the same list of patches to lots of
			 * Connections, we can reuse the list of RouteTableMessages
			 * between those connections if their last sent
			 * table is exactly the same.
			 * This allows us to only reduce the amount of times we have
			 * to call encode.
			 */

			/*
			 * Make patches, the group of messages we'll send this remote computer to bring its record of our QRP table up to date.
			 * The line of code that does it is:
			 * 
			 * patches = table.encode(lastSent, true);
			 * 
			 * lastSent is the version of our QRP table this remote computer has.
			 * table is our current QRP table.
			 * true allows compression within the patch messages.
			 */

			// If the last QRP table we sent this computer is represented by the same object as lastSent, only make patches if we don't have them already
			if (lastSent == c.getQueryRouteTableSent()) { // This compares references, not the contents of the objects, and works when both references are null

				// If we don't have them yet, make all the QRP messages we need to patch lastSent into table
			    if (patches == null) patches = table.encode(lastSent, true);

			// They aren't the same, we need to make a custom group of patch messages for this remote computer
            } else {

            	// Get our record of the remote computer's record of our QRP table
			    lastSent = c.getQueryRouteTableSent(); // This is the QRP table the remote computer has, the last one we've sent it

			    // Make all the QRP messages we need to patch lastSent, the last QRP table we sent this remote computer, into table, our current QRP table
			    patches = table.encode(lastSent, true); // True to allow compression
            }

			// If settings have turned off QRP entirely, leave the method without sending anything to anyone
            if (!ConnectionSettings.SEND_QRP.getValue()) return;

            // Send the group of QRP messages to the remote computer
		    for (Iterator iter = patches.iterator(); iter.hasNext(); ) {
		        c.send((RouteTableMessage)iter.next());
    	    }

		    // Save the table we updated the remote computer to in its ManagedConnection object
            c.setQueryRouteTableSent(table); // This way, we'll know what we need to change to patch it up to date next time
		}
    }

    /**
     * Our QRP table that we've been sending our ultrapeers.
     * If we're an ultrapeer, our QRP table will include our files and all the files all our leaves are sharing, too.
     * 
     * Every 10 seconds, forwardQueryRouteTables() makes _lastQueryRouteTable with createRouteTable().
     * createRouteTable() gives _lastQueryRouteTable the files we and all of our leaves are sharing.
     * Then, forwardQueryRouteTables() loops through all our ultrapeers.
     * Those that we haven't updated in 1 minute are sent a custom group of QRP patch messages.
     * The patch message group brings the out-of-date record of our QRP table the remote ultrapeer has up to date.
     * 
     * Accessor for the most recently calculated <tt>QueryRouteTable</tt>
     * for this node.  If this node is an Ultrapeer, the table will include
     * all data for leaf nodes in addition to data for this node's files.
     * 
     * @return the <tt>QueryRouteTable</tt> for this node
     */
    public QueryRouteTable getQueryRouteTable() {

    	// Return our QRP table we've been using to update ultrapeers
        return _lastQueryRouteTable;
    }

    /**
     * Make our QRP table, which shows what we and our leaves are sharing.
     * 
     * Gets a copy of our QRP table from the FileManager, showing what we're sharing.
     * If we're an ultrapeer, mixes in the QRP tables from our leaves.
     * 
     * Creates a query route table appropriate for forwarding to connection c.
     * This will not include information from c.
     * requires queryUpdateLock held
     * 
     * @return Our QRP table
     */
    private static QueryRouteTable createRouteTable() {

    	// Get a copy of our QRP table that describes the files we're sharing
        QueryRouteTable ret = _fileManager.getQRT();

        // If we're an ultrapeer
        if (RouterService.isSupernode()) {

        	// Mix in the QRP tables of our leaves
            addQueryRoutingEntriesForLeaves(ret);
        }

        // Return our QRP table, which shows what we and our leaves are sharing
        return ret;
    }

	/**
	 * Merge the QRP tables from all our leaves into our own.
	 * Only createRouteTable() above calls this method.
	 * 
	 * Adds all query routing tables of leaves to the query routing table for
	 * this node for propagation to other Ultrapeers at 1 hop.
	 * 
	 * Added "busy leaf" support to prevent a busy leaf from having its QRT
	 * table added to the Ultrapeer's last-hop QRT table.  This should reduce
	 * BW costs for UPs with busy leaves.
	 * 
	 * @param qrt Our QRP table, to add to.
	 *            This method doesn't return anything because it edits qrt in place.
	 */
	private static void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {

		// Loop for each of our leaves
		List leaves = _manager.getInitializedClientConnections();
		for (int i = 0; i < leaves.size(); i++) {
			ManagedConnection mc = (ManagedConnection)leaves.get(i);
        	synchronized (mc.getQRPLock()) {

        		// Only do something if this leaf has free file upload slots
        	    if (!mc.isBusyLeaf()) {

        	    	// Get the leaf's QRP table
                	QueryRouteTable qrtr = mc.getQueryRouteTableReceived();
					if (qrtr != null) {

						// Add all the 1s in the leaf's QRP table to our own
						qrt.addAll(qrtr);
					}
        	    }
			}
		}
	}

	//do

    /**
     * Adds the specified MessageListener for messages with this GUID.
     * You must manually unregister the listener.
     *
     * This works by replacing the necessary maps & lists, so that 
     * notifying doesn't have to hold any locks.
     */
    public void registerMessageListener(byte[] guid, MessageListener ml) {
        
        ml.registered(guid);
        
        synchronized (MESSAGE_LISTENER_LOCK) {
            
            Map listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
            listeners.putAll(_messageListeners);
            List all = (List)listeners.get(guid);
            
            if (all == null) {
                
                all = new ArrayList(1);
                all.add(ml);
                
            } else {
                
                List temp = new ArrayList(all.size() + 1);
                temp.addAll(all);
                all = temp;
                all.add(ml);
            }
            
            listeners.put(guid, Collections.unmodifiableList(all));
            _messageListeners = Collections.unmodifiableMap(listeners);
        }
    }
    
    /**
     * Unregisters this MessageListener from listening to the GUID.
     *
     * This works by replacing the necessary maps & lists so that
     * notifying doesn't have to hold any locks.
     */
    public void unregisterMessageListener(byte[] guid, MessageListener ml) {
        
        boolean removed = false;
        
        synchronized (MESSAGE_LISTENER_LOCK) {
            
            List all = (List)_messageListeners.get(guid);
            
            if (all != null) {
                
                all = new ArrayList(all);
                
                if (all.remove(ml)) {
                    
                    removed = true;
                    Map listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
                    listeners.putAll(_messageListeners);
                    
                    if (all.isEmpty()) listeners.remove(guid);
                    else               listeners.put(guid, Collections.unmodifiableList(all));
                    
                    _messageListeners = Collections.unmodifiableMap(listeners);
                }
            }
        }
        
        if (removed) ml.unregistered(guid);
    }

    /**
     * responds to a request for the list of ultrapeers or leaves.  It is sent right back to the
     * requestor on the UDP receiver thread.
     * @param msg the request message
     * @param handler the UDPHandler to send it to.
     */
    private void handleUDPCrawlerPing(UDPCrawlerPing msg, ReplyHandler handler){
    	
    	//make sure the same person doesn't request too often
    	//note: this should only happen on the UDP receiver thread, that's why
    	//I'm not locking it.
        
    	if (!_promotionManager.allowUDPPing(handler)) return; 
    	UDPCrawlerPong newMsg = new UDPCrawlerPong(msg);
    	handler.reply(newMsg);
    }
    
    /**
     * Replies to a head ping sent from the given ReplyHandler.
     */
    private void handleHeadPing(HeadPing ping, ReplyHandler handler) {
        
        if (DownloadSettings.DROP_HEADPINGS.getValue()) return;
        
        GUID clientGUID = ping.getClientGuid();
        ReplyHandler pingee;
        
        if (clientGUID != null) pingee = getPushHandler(clientGUID.bytes());
        else                    pingee = FOR_ME_REPLY_HANDLER; // handle ourselves.
        
        //drop the ping if no entry for the given clientGUID
        if (pingee == null) return; 
        
        //don't bother routing if this is intended for me. 
        // TODO1:  Clean up ReplyHandler interface so we aren't
        //        afraid to use it like it's intended.
        //        That way, we can do pingee.handleHeadPing(ping)
        //        and not need this anti-OO instanceof check.
        if (pingee instanceof ForMeReplyHandler) {
            
            // If it's for me, reply directly to the person who sent it.
            HeadPong pong = new HeadPong(ping);
            handler.reply(pong); // 
            
        } else {
            
            // Otherwise, remember who sent it and forward it on.
            //remember where to send the pong to. 
            //the pong will have the same GUID as the ping. 
            // Note that this uses the messageGUID, not the clientGUID
            _headPongRouteTable.routeReply(ping.getGUID(), handler); 
            
            //and send off the routed ping 
            if (!(handler instanceof Connection) || ((Connection)handler).supportsVMRouting()) {
                
                pingee.reply(ping);
                
            } else {
                
                pingee.reply(new HeadPing(ping)); 
            }
        }
    }

    /** 
     * Handles a pong received from the given handler.
     */ 
    private void handleHeadPong(HeadPong pong, ReplyHandler handler) {
        
        ReplyHandler forwardTo =  _headPongRouteTable.getReplyHandler(pong.getGUID()); 

        // TODO1: Clean up ReplyHandler interface so we're not afraid
        //       to use it correctly.
        //       Ideally, we'd do forwardTo.handleHeadPong(pong)
        //       instead of this instanceof check
         
        // if this pong is for me, process it as usual (not implemented yet)
        if (forwardTo != null && !(forwardTo instanceof ForMeReplyHandler)) {
            
            forwardTo.reply(pong); 
            _headPongRouteTable.removeReplyHandler(forwardTo); 
        } 
    }

    //done

    /**
     * A QueryResponseBundle object keeps a QueryRequest packet and Response[] array of file hits together.
     * bufferResponsesForLaterDelivery() lists QueryResponseBundle objects under query packet message GUIDs in the _outOfBandReplies Hashtable.
     */
    private static class QueryResponseBundle {

        /** The QueryRequest packet we received. */
        public final QueryRequest _query;

        /** The array of Response objects we found that match the search. */
        public final Response[] _responses;

        /**
         * Make a new QueryResponsesBundle object.
         * Keep a QueryRequest packet and Response[] array of file hits together.
         * 
         * @param query     A QueryRequest packet we received
         * @param responses The array of Response objects we found that match the search
         */
        public QueryResponseBundle(QueryRequest query, Response[] responses) {

            // Save the object and array
            _query     = query;
            _responses = responses;
        }
    }

    //do

    /** Can be run to invalidate out-of-band ACKs that we are waiting for....
     */
    private class Expirer implements Runnable {
        
        public void run() {
            
            try {
                
                Set toRemove = new HashSet();
                
                synchronized (_outOfBandReplies) {
                    
                    Iterator keys = _outOfBandReplies.keySet().iterator();
                    
                    while (keys.hasNext()) {
                        
                        GUID.TimedGUID currQB = (GUID.TimedGUID)keys.next();
                        if ((currQB != null) && (currQB.shouldExpire())) toRemove.add(currQB);
                    }
                    
                    // done iterating through _outOfBandReplies, remove the 
                    // keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext()) _outOfBandReplies.remove(keys.next());
                }
                
            } catch (Throwable t) {
                
                ErrorService.error(t);
            }
        }
    }

    /** This is run to clear out the registry of connect back attempts...
     *  Made package access for easy test access.
     */
    static class ConnectBackExpirer implements Runnable {
        
        public void run() {
            
            try {
                
                _tcpConnectBacks.clear();
                _udpConnectBacks.clear();
                
            } catch(Throwable t) {
                
                ErrorService.error(t);
            }
        }
    }

    static class HopsFlowManager implements Runnable {
        
        /* in case we don't want any queries any more */
        private static final byte BUSY_HOPS_FLOW = 0;

    	/* in case we want to reenable queries */
    	private static final byte FREE_HOPS_FLOW = 5;

        /* small optimization:
           send only HopsFlowVendorMessages if the busy state changed */
        private static boolean _oldBusyState = false;
           
        public void run() {
            
            // only leafs should use HopsFlow
            if (RouterService.isSupernode()) return;
            
            // busy hosts don't want to receive any queries, if this node is not
            // busy, we need to reset the HopsFlow value
            boolean isBusy = !RouterService.getUploadManager().isServiceable();
            
            // state changed? don't bother the ultrapeer with information
            // that it already knows. we need to inform new ultrapeers, though.
            final List connections = _manager.getInitializedConnections();
            final HopsFlowVendorMessage hops = new HopsFlowVendorMessage(isBusy ? BUSY_HOPS_FLOW : FREE_HOPS_FLOW);
            
            if (isBusy == _oldBusyState) {
                
                for (int i = 0; i < connections.size(); i++) {
                    
                    ManagedConnection c = (ManagedConnection)connections.get(i);
                    
                    // Yes, we may tell a new ultrapeer twice, but
                    // without a buffer of some kind, we might forget
                    // some ultrapeers. The clean solution would be
                    // to remember the hops-flow value in the connection.
                    if (c != null && c.getConnectionTime() + 1.25 * HOPS_FLOW_INTERVAL > System.currentTimeMillis() && c.isClientSupernodeConnection()) c.send(hops);
                }
                
            } else {
                
                _oldBusyState = isBusy;
                
                for (int i = 0; i < connections.size(); i++) {
                    
                    ManagedConnection c = (ManagedConnection)connections.get(i);
                    
                    if (c != null && c.isClientSupernodeConnection()) c.send(hops);
                }
            }
        }
    }
}
