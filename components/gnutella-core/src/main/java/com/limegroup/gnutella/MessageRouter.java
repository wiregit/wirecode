package com.limegroup.gnutella;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.collection.FixedsizeHashMap;
import org.limewire.collection.NoMoreStorageException;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ManagedThread;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messagehandlers.AdvancedToggleHandler;
import com.limegroup.gnutella.messagehandlers.DualMessageHandler;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.AdvancedStatsToggle;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPing;
import com.limegroup.gnutella.messages.vendor.UpdateRequest;
import com.limegroup.gnutella.messages.vendor.UpdateResponse;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.ResultCounter;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.LimeSentMessageStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.statistics.RouteErrorStat;
import com.limegroup.gnutella.statistics.RoutedQueryStat;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.SocketsManager;


/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
@Singleton
public abstract class MessageRouter {
    
    private static final Log LOG = LogFactory.getLog(MessageRouter.class);

    /**
     * Constant for the number of old connections to use when forwarding
     * traffic from old connections.
     */
    private static final int OLD_CONNECTIONS_TO_USE = 15;

    /**
     * The GUID we attach to QueryReplies to allow PushRequests in
     * responses.
     */
    protected byte[] _clientGUID;


		
    /**
     * The maximum size for <tt>RouteTable</tt>s.
     */
    private int MAX_ROUTE_TABLE_SIZE = 50000;  //actually 100,000 entries

    /**
     * Maps PingRequest GUIDs to PingReplyHandlers.  Stores 2-4 minutes,
     * typically around 2500 entries, but never more than 100,000 entries.
     */
    private RouteTable _pingRouteTable = 
        new RouteTable(2*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typically around 13000 entries, but never more than 100,000 entries.
     */
    private RouteTable _queryRouteTable = 
        new RouteTable(5*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typically around 3500 entries, but never more than 100,000
     * entries.  
     */
    private RouteTable _pushRouteTable = 
        new RouteTable(7*60, MAX_ROUTE_TABLE_SIZE);
    
    /**
     * Maps HeadPong guids to the originating pingers.  Short-lived since
     * we expect replies from our leaves quickly.
     */
    private RouteTable _headPongRouteTable = 
    	new RouteTable(10, MAX_ROUTE_TABLE_SIZE);

    /** How long to buffer up out-of-band replies.
     */
    private static final long CLEAR_TIME = 30 * 1000; // 30 seconds
    
    /**
     * The amount of time after which to expire an OOBSession.
     */
    private static final long OOB_SESSION_EXPIRE_TIME = 2 * 60 * 1000;

    /** Time between sending HopsFlow messages.
     */
    private static final long HOPS_FLOW_INTERVAL = 15 * 1000; // 15 seconds

    /** The maximum number of UDP replies to buffer up.  Non-final for 
     *  testing.
     */
    static int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps track of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryResponseBundle.
     */
    private final Map<GUID.TimedGUID, QueryResponseBundle> _outOfBandReplies =
        new Hashtable<GUID.TimedGUID, QueryResponseBundle>();

    
    private BypassedResultsCache _bypassedResultsCache;
    
    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap<String, String> _udpConnectBacks = 
        new FixedsizeHashMap<String, String>(200);
        
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
    private static final FixedsizeHashMap<String, String> _tcpConnectBacks = 
        new FixedsizeHashMap<String, String>(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a TCPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_TCP_CONNECTBACK_FORWARDS = 5;        
    
    /**
     * The processingqueue to add tcpconnectback socket connections to.
     */
    private static final ExecutorService TCP_CONNECT_BACKER =
        ExecutorsHelper.newProcessingQueue("TCPConnectBack");
    
	/**
	 * Constant for the <tt>QueryDispatcher</tt> that handles dynamically
	 * generated queries that adjust to the number of results received, the
	 * number of connections, etc.
	 */
	private final QueryDispatcher DYNAMIC_QUERIER = ProviderHacks.getQueryDispatcher();
	
	/**
	 * Handle to the <tt>ActivityCallback</tt> for sending data to the 
	 * display.
	 */
	private ActivityCallback _callback;
    
	/**
	 * A handle to the thread that deals with QRP Propagation
	 */
	private final QRPPropagator QRP_PROPAGATOR = new QRPPropagator();


    /**
     * Variable for the most recent <tt>QueryRouteTable</tt> created
     * for this node.  If this node is an Ultrapeer, the routing
     * table will include the tables from its leaves.
     */
    private QueryRouteTable _lastQueryRouteTable;

    /**
     * The maximum number of response to send to a query that has
     * a "high" number of hops.
     */
    private static final int HIGH_HOPS_RESPONSE_LIMIT = 10;

    /**
     * The lifetime of OOBs guids.
     */
    private static final long TIMED_GUID_LIFETIME = 25 * 1000; 

    /** Keeps track of Listeners of GUIDs. */
    private volatile Map<byte[], List<MessageListener>> _messageListeners =
        Collections.emptyMap();
    
    /**
     * Lock that registering & unregistering listeners can hold
     * while replacing the listeners map / lists.
     */
    private final Object MESSAGE_LISTENER_LOCK = new Object();

    /**
     * The time we last received a request for a query key.
     */
    private long _lastQueryKeyTime;
    
    /** Handlers for TCP messages. */
    private ConcurrentMap<Class<? extends Message>, MessageHandler> messageHandlers =
        new ConcurrentHashMap<Class<? extends Message>, MessageHandler>(30, 0.75f, 3);
    
    /** Handler for UDP messages. */
    private ConcurrentMap<Class<? extends Message>, MessageHandler> udpMessageHandlers =
        new ConcurrentHashMap<Class<? extends Message>, MessageHandler>(15, 0.75f, 3);
    
    /** Handler for TCP messages. */
    private ConcurrentMap<Class<? extends Message>, MessageHandler> multicastMessageHandlers =
        new ConcurrentHashMap<Class<? extends Message>, MessageHandler>(5, 0.75f, 3);
    
    /** Map for Multicast morphed GUIDs. */
    private GuidMap _multicastGuidMap = GuidMapFactory.getMap();
    
    /** The length of time a multicast guid should stay alive. */
    private static final long MULTICAST_GUID_EXPIRE_TIME = 60 * 1000;
    
    /** How long to remember cached udp reply handlers. */
    private static final int UDP_REPLY_CACHE_TIME = 60 * 1000;
    
    /** A mapping of UDPReplyHandlers, to prevent creation of them over-and-over. */
    private static final FixedsizeForgetfulHashMap<InetSocketAddress, UDPReplyHandler> _udpReplyHandlerCache =
        new FixedsizeForgetfulHashMap<InetSocketAddress, UDPReplyHandler>(500);
    
    protected final NetworkManager networkManager;
    protected final QueryRequestFactory queryRequestFactory;
    protected final QueryHandlerFactory queryHandlerFactory;
    protected final OnDemandUnicaster onDemandUnicaster;
    protected final HeadPongFactory headPongFactory;
    protected final PingReplyFactory pingReplyFactory;
    protected final ConnectionManager connectionManager;
    protected final ReplyHandler forMeReplyHandler;
    protected final QueryUnicaster queryUnicaster;
    protected final FileManager fileManager;
    protected final ContentManager contentManager;
    protected final DHTManager dhtManager;
    protected final UploadManager uploadManager;
    protected final DownloadManager downloadManager;
    protected final UDPService udpService;
    protected final SearchResultHandler searchResultHandler;
    protected final SocketsManager socketsManager;
    protected final HostCatcher hostCatcher;
    protected final QueryReplyFactory queryReplyFactory;
    protected final StaticMessages staticMessages;
    protected final Provider<MessageDispatcher> messageDispatcher;
    protected final MulticastService multicastService;
    
    /**
     * Creates a MessageRouter. Must call initialize before using.
     */
    @Inject
    protected MessageRouter(NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            QueryHandlerFactory queryHandlerFactory,
            OnDemandUnicaster onDemandUnicaster, 
            HeadPongFactory headPongFactory,
            PingReplyFactory pingReplyFactory,
            ConnectionManager connectionManager,
            @Named("forMeReplyHandler") ReplyHandler forMeReplyHandler,
            QueryUnicaster queryUnicaster,
            FileManager fileManager,
            ContentManager contentManager,
            DHTManager dhtManager,
            UploadManager uploadManager,
            DownloadManager downloadManager,
            UDPService udpService,
            SearchResultHandler searchResultHandler,
            SocketsManager socketsManager,
            HostCatcher hostCatcher,
            QueryReplyFactory queryReplyFactory,
            StaticMessages staticMessages,
            Provider<MessageDispatcher> messageDispatcher,
            MulticastService multicastService
            ) {
        _clientGUID = RouterService.getMyGUID();
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.queryHandlerFactory = queryHandlerFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.headPongFactory = headPongFactory;
        this.pingReplyFactory = pingReplyFactory;
        this.connectionManager = connectionManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.queryUnicaster = queryUnicaster;
        this.fileManager = fileManager;
        this.contentManager = contentManager;
        this.dhtManager = dhtManager;
        this.uploadManager = uploadManager;
        this.downloadManager = downloadManager;
        this.udpService = udpService;
        this.searchResultHandler = searchResultHandler;
        this.socketsManager = socketsManager;
        this.hostCatcher = hostCatcher;
        this.queryReplyFactory = queryReplyFactory;
        this.staticMessages = staticMessages;
        this.messageDispatcher = messageDispatcher;
        this.multicastService = multicastService;
         
    }
    
    /** Sets a new handler to the given handlerMap, for the given class. */
    private boolean setHandler(ConcurrentMap<Class<? extends Message>, MessageHandler> handlerMap,
                            Class<? extends Message> clazz, MessageHandler handler) {
        
        if (handler != null) {
            MessageHandler old = handlerMap.put(clazz, handler);
            if(old != null) {
                LOG.warn("Ejecting old handler: " + old + " for clazz: " + clazz);
            }
            return true;
        } else {
            return handlerMap.remove(clazz) != null;
        }
    }
    
    /** 
     * Adds the given handler to the handlerMap for the given class.
     * If a handler already existed, this will construct a DualMessageHandler
     * so that both are handlers are notified.
     * 
     * @param handlerMap
     * @param clazz
     * @param handler
     */
    private void addHandler(ConcurrentMap<Class<? extends Message>, MessageHandler> handlerMap,
                            Class<? extends Message> clazz, MessageHandler handler) {
        MessageHandler existing = handlerMap.get(clazz);
        if(existing != null) {
            // non-blocking addition -- continue trying until we succesfully
            // replace the prior handler w/ a dual version of that handler
            while(true) {
                MessageHandler dual = new DualMessageHandler(handler, existing);
                if(handlerMap.replace(clazz, existing, dual))
                    break;
                existing = handlerMap.get(clazz);
                dual = new DualMessageHandler(handler, existing);
            }
        } else {
            setHandler(handlerMap, clazz, handler);
        }
    }
    
    /**
     * Installs a MessageHandler for "regular" Messages.
     * 
     * @link #handleMessage(Message, ManagedConnection)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void setMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        setHandler(messageHandlers, clazz, handler);
    }
    
    /**
     * Adds the new handler as a handler in addition to other handlers.
     * 
     * @link #handleMessage(Message, ManagedConnection)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void addMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        addHandler(messageHandlers, clazz, handler);
    }
    
    /**
     * Returns a MessageHandler for the specified Message Class
     * or null if no such MessageHandler exists.
     */
    public MessageHandler getMessageHandler(Class<? extends Message> clazz) {
        return messageHandlers.get(clazz);
    }
    
    /**
     * Installs a MessageHandler for UDP Messages.
     * 
     * @link #handleUDPMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void setUDPMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        setHandler(udpMessageHandlers, clazz, handler);
    }
    
    /**
     * Adds the new handler as a handler in addition to other handlers for UDP messages.
     * 
     * @link #handleUDPMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void addUDPMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        addHandler(udpMessageHandlers, clazz, handler);
    }
    
    /**
     * Returns a MessageHandler for the specified Message Class
     * or null if no such MessageHandler exists.
     */
    public MessageHandler getUDPMessageHandler(Class<? extends Message> clazz) {
        return udpMessageHandlers.get(clazz);
    }
    
    /**
     * Installs a MessageHandler for Multicast Messages.
     * 
     * @link #handleMulticastMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void setMulticastMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        setHandler(multicastMessageHandlers, clazz, handler);
    }
    
    /**
     * Adds the new handler as a handler in addition to other handlers for multicast messages.
     * 
     * @link #handleMulticastMessage(Message, InetSocketAddress)
     * @param clazz The Class of the Message
     * @param handler The Handler of the Message
     */
    public void addMulticastMessageHandler(Class<? extends Message> clazz, MessageHandler handler) {
        addHandler(multicastMessageHandlers, clazz, handler);
    }
    
    /**
     * Returns a MessageHandler for the specified Message Class
     * or null if no such MessageHandler exists.
     */
    public MessageHandler getMulticastMessageHandler(Class<? extends Message> clazz) {
        return multicastMessageHandlers.get(clazz);
    }
    
    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize() {
		_callback = RouterService.getCallback();
        
		_bypassedResultsCache = new BypassedResultsCache(_callback, downloadManager);
        
	    QRP_PROPAGATOR.start();

        // schedule a runner to clear unused out-of-band replies
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        // schedule a runner to clear guys we've connected back to
        RouterService.schedule(new ConnectBackExpirer(), 10 * CLEAR_TIME, 
                               10 * CLEAR_TIME);
        // schedule a runner to send hops-flow messages
        RouterService.schedule(new HopsFlowManager(uploadManager, connectionManager), HOPS_FLOW_INTERVAL*10, 
                               HOPS_FLOW_INTERVAL);
        RouterService.schedule(new UDPReplyCleaner(), UDP_REPLY_CACHE_TIME, UDP_REPLY_CACHE_TIME);
        
        // runner to clean up OOB sessions
        OOBHandler oobHandler = new OOBHandler(this);
        RouterService.schedule(oobHandler, CLEAR_TIME, CLEAR_TIME);
        
        // handler for inspection requests
        InspectionRequestHandler inspectionHandler = new InspectionRequestHandler(this, networkManager);
        
        setMessageHandler(PingRequest.class, new PingRequestHandler());
        setMessageHandler(PingReply.class, new PingReplyHandler());
        setMessageHandler(QueryRequest.class, new QueryRequestHandler());
        setMessageHandler(QueryReply.class, new QueryReplyHandler());
        setMessageHandler(PushRequest.class, new PushRequestHandler());
        setMessageHandler(ResetTableMessage.class, new ResetTableHandler());
        setMessageHandler(PatchTableMessage.class, new PatchTableHandler());
        setMessageHandler(TCPConnectBackVendorMessage.class, new TCPConnectBackHandler());
        setMessageHandler(UDPConnectBackVendorMessage.class, new UDPConnectBackHandler());
        setMessageHandler(TCPConnectBackRedirect.class, new TCPConnectBackRedirectHandler());
        setMessageHandler(UDPConnectBackRedirect.class, new UDPConnectBackRedirectHandler());
        setMessageHandler(PushProxyRequest.class, new PushProxyRequestHandler());
        setMessageHandler(QueryStatusResponse.class, new QueryStatusResponseHandler());
        setMessageHandler(HeadPing.class, new HeadPingHandler());
        setMessageHandler(SimppRequestVM.class, new SimppRequestVMHandler());
        setMessageHandler(SimppVM.class, new SimppVMHandler());
        setMessageHandler(UpdateRequest.class, new UpdateRequestHandler());
        setMessageHandler(UpdateResponse.class, new UpdateResponseHandler());
        setMessageHandler(HeadPong.class, new HeadPongHandler());
        setMessageHandler(DHTContactsMessage.class, new DHTContactsMessageHandler());
        setMessageHandler(VendorMessage.class, new VendorMessageHandler());
        setMessageHandler(InspectionRequest.class, inspectionHandler);
        
        setUDPMessageHandler(QueryRequest.class, new UDPQueryRequestHandler());
        setUDPMessageHandler(QueryReply.class, new UDPQueryReplyHandler(oobHandler));
        setUDPMessageHandler(PingRequest.class, new UDPPingRequestHandler());
        setUDPMessageHandler(PingReply.class, new UDPPingReplyHandler());
        setUDPMessageHandler(PushRequest.class, new UDPPushRequestHandler());
        setUDPMessageHandler(LimeACKVendorMessage.class, new UDPLimeACKVendorMessageHandler());
        setUDPMessageHandler(ReplyNumberVendorMessage.class, oobHandler);
        setUDPMessageHandler(UDPCrawlerPing.class, new UDPCrawlerPingHandler(networkManager));
        setUDPMessageHandler(HeadPing.class, new UDPHeadPingHandler());
        setUDPMessageHandler(UpdateRequest.class, new UDPUpdateRequestHandler());
        setUDPMessageHandler(ContentResponse.class, new UDPContentResponseHandler());
        setUDPMessageHandler(InspectionRequest.class, inspectionHandler);
        setUDPMessageHandler(AdvancedStatsToggle.class, new AdvancedToggleHandler(networkManager));
        
        setMulticastMessageHandler(QueryRequest.class, new MulticastQueryRequestHandler());
        //setMulticastMessageHandler(QueryReply.class, new MulticastQueryReplyHandler());
        setMulticastMessageHandler(PingRequest.class, new MulticastPingRequestHandler());
        //setMulticastMessageHandler(PingReply.class, new MulticastPingReplyHandler());
        setMulticastMessageHandler(PushRequest.class, new MulticastPushRequestHandler());
    }

    /**
     * Routes a query GUID to yourself.
     */
    public void originateQueryGUID(byte[] guid) {
        _queryRouteTable.routeReply(guid, forMeReplyHandler);
    }

    /** Call this to inform us that a query has been killed by a user or
     *  whatever.  Useful for purging unneeded info.<br>
     *  Callers of this should make sure that they have purged the guid from
     *  their tables.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void queryKilled(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentException("Input GUID is null!");
        _bypassedResultsCache.queryKilled(guid);
    }

    /** Call this to inform us that a download is finished or whatever.  Useful
     *  for purging unneeded info.<br>
     *  If the caller is a Downloader, please be sure to clear yourself from the
     *  active and waiting lists in DownloadManager.
     *  @throws IllegalArgumentException if the guid is null
     */
    public void downloadFinished(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentException("Input GUID is null!");
        _bypassedResultsCache.downloadFinished(guid);
    }
    
    /** @returns a Set with GUESSEndpoints that had matches for the
     *  original query guid.  may be empty.
     *  @param guid the guid of the query you want endpoints for.
     */
    public Set<GUESSEndpoint> getQueryLocs(GUID guid) {
        return _bypassedResultsCache.getQueryLocs(guid);
    }
    
    public String getPingRouteTableDump() {
        return _pingRouteTable.toString();
    }

    public String getQueryRouteTableDump() {
        return _queryRouteTable.toString();
    }

    public String getPushRouteTableDump() {
        return _pushRouteTable.toString();
    }

    /**
     * A callback for ConnectionManager to clear a <tt>ReplyHandler</tt> from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ReplyHandler rh) {
        DYNAMIC_QUERIER.removeReplyHandler(rh);
        _pingRouteTable.removeReplyHandler(rh);
        _queryRouteTable.removeReplyHandler(rh);
        _pushRouteTable.removeReplyHandler(rh);
        _headPongRouteTable.removeReplyHandler(rh);
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param m the <tt>Message</tt> instance to route appropriately
	 * @param receivingConnection the <tt>ManagedConnection</tt> over which
	 *  the message was received
     */
    public void handleMessage(Message msg, 
                              ManagedConnection receivingConnection) {
        // Increment hops and decrease TTL.
        msg.hop();
	   
        MessageHandler msgHandler = getMessageHandler(msg.getClass());
        if (msgHandler != null) {
            msgHandler.handleMessage(msg, null, receivingConnection);
        } else if (msg instanceof VendorMessage) {
            msgHandler = getMessageHandler(VendorMessage.class);
            if (msgHandler != null) {
                msgHandler.handleMessage(msg, null, receivingConnection);
            }
        }
        
        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
		//forwardQueryRouteTables();
        notifyMessageListener(msg, receivingConnection);
    }

    /**
     * Notifies any message listeners of this message's guid about the message.
     * This holds no locks.
     */
    private final void notifyMessageListener(Message msg, ReplyHandler handler) {
        List<MessageListener> all = _messageListeners.get(msg.getGUID());
        if(all != null) {
            for(MessageListener next : all) {
                next.processMessage(msg, handler);
            }
        }
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param msg the <tt>Message</tt> received
	 * @param addr the <tt>InetSocketAddress</tt> containing the IP and 
	 *  port of the client node
     */	
	public void handleUDPMessage(Message msg, InetSocketAddress addr) {
	    // Increment hops and decrement TTL.
	    msg.hop();

        if(msg instanceof QueryReply) {
            // check to see if it was from the multicast map.
            byte[] origGUID = _multicastGuidMap.getOriginalGUID(msg.getGUID());
            if(origGUID != null) {
                msg = queryReplyFactory.createQueryReply(origGUID, (QueryReply)msg);
                ((QueryReply)msg).setMulticastAllowed(true);
            }
        }
        
        UDPReplyHandler replyHandler = _udpReplyHandlerCache.get(addr);
        if(replyHandler == null)
            replyHandler = new UDPReplyHandler(addr);
        _udpReplyHandlerCache.put(addr, replyHandler); // renew it
        
        MessageHandler msgHandler = getUDPMessageHandler(msg.getClass());
        if (msgHandler != null) {
            msgHandler.handleMessage(msg, addr, replyHandler);
        }  else if (msg instanceof VendorMessage) {
            msgHandler = getUDPMessageHandler(VendorMessage.class);
            if (msgHandler != null) {
                msgHandler.handleMessage(msg, addr, replyHandler);
            }
        }
        
        notifyMessageListener(msg, replyHandler);
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
        // Assert.that(msg.isMulticast(),
        // "non multicast message in handleMulticastMessage: " + msg);

        // no multicast messages should ever have been
        // set with a TTL greater than 1.
        if (msg.getTTL() > 1) {
            return;
        }

        // Increment hops and decrement TTL.
        msg.hop();

        if (NetworkUtils.isLocalAddress(addr.getAddress())
                && !ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.getValue()) {
            return;
        }

        UDPReplyHandler replyHandler = _udpReplyHandlerCache.get(addr);
        if(replyHandler == null)
            replyHandler = new UDPReplyHandler(addr);
        _udpReplyHandlerCache.put(addr, replyHandler); // renew it

        MessageHandler msgHandler = getMulticastMessageHandler(msg.getClass());
        if (msgHandler != null) {
            msgHandler.handleMessage(msg, addr, replyHandler);
        } else if (msg instanceof VendorMessage) {
            msgHandler = getMulticastMessageHandler(VendorMessage.class);
            if (msgHandler != null) {
                msgHandler.handleMessage(msg, addr, replyHandler);
            }
        }

        notifyMessageListener(msg, replyHandler);
    }


    /**
     * Returns true if the Query has a valid AddressSecurityToken. false if it isn't present
     * or valid.
     */
    protected boolean hasValidQueryKey(InetAddress ip, int port, 
                                       QueryRequest qr) {
        AddressSecurityToken qk = qr.getQueryKey();
        if (qk == null)
            return false;
        
        return qk.isFor(ip, port);
    }

	/**
	 * Sends an ack back to the GUESS client node.  
	 */
	protected void sendAcknowledgement(InetSocketAddress addr, byte[] guid) {
		Endpoint host = connectionManager.getConnectedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
                
                reply = pingReplyFactory.createGUESSReply(guid, (byte)1, host);
            } catch(UnknownHostException e) {
				reply = createPingReply(guid);
            }
		} else {
			reply = createPingReply(guid);
		}
		
		// No GUESS endpoints existed and our IP/port was invalid.
		if( reply == null )
		    return;

        udpService.send(reply, addr.getAddress(), addr.getPort());
		SentMessageStatHandler.UDP_PING_REPLIES.addMessage(reply);
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or a <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply createPingReply(byte[] guid) {
		GUESSEndpoint endpoint = queryUnicaster.getUnicastEndpoint();
		if(endpoint == null) {
		    if(networkManager.isIpPortValid())
                return pingReplyFactory.create(guid, (byte)1);
            else
                return null;
		} else {
            return pingReplyFactory.createGUESSReply(guid, (byte)1, 
                                              endpoint.getPort(),
                                              endpoint.getInetAddress().getAddress());
		}
	}



	
    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handlePingRequestPossibleDuplicate(
        PingRequest request, ReplyHandler handler) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handlePingRequest(request, handler);
    }

    /**
     * The handler for PingRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handlePingRequest.
     */
    final void handleUDPPingRequestPossibleDuplicate(													 
        PingRequest request, ReplyHandler handler, InetSocketAddress  addr) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handleUDPPingRequest(request, handler, addr);
    }

    /**
     * The handler for QueryRequests received in
     * ManagedConnection.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, calls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplicate(
        QueryRequest request, ManagedConnection receivingConnection) {
        
        // With the new handling of probe queries (TTL 1, Hops 0), we have a few
        // new options:
        // 1) If we have a probe query....
        //  a) If you have never seen it before, put it in the route table and
        //  set the ttl appropriately
        //  b) If you have seen it before, then just count it as a duplicate
        // 2) If it isn't a probe query....
        //  a) Is it an extension of a probe?  If so re-adjust the TTL.
        //  b) Is it a 'normal' query (no probe extension or already extended)?
        //  Then check if it is a duplicate:
        //    1) If it a duplicate, just count it as one
        //    2) If it isn't, put it in the route table but no need to setTTL

        // we msg.hop() before we get here....
        // hops may be 1 or 2 because we may be probing with a leaf query....
        final boolean isProbeQuery = 
            ((request.getTTL() == 0) && 
             ((request.getHops() == 1) || (request.getHops() == 2)));

		ResultCounter counter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 receivingConnection);

		if(counter != null) {  // query is new (probe or normal)
            // 1a: set the TTL of the query so it can be potentially extended  
            if (isProbeQuery) 
                _queryRouteTable.setTTL(counter, (byte)1);

            // 1a and 2b2
            // if a new probe or a new request, do everything (so input true
            // below)
            handleQueryRequest(request, receivingConnection, counter, true);
		} else if (!isProbeQuery) {// probe extension?
            if (wasProbeQuery(request)) {
                // rebroadcast out but don't locally evaluate....
                handleQueryRequest(request, receivingConnection, counter, 
                                   false);
            } else { // 2b1: not a correct extension, so call it a duplicate....
                tallyDupQuery(request);
            }
        } else if (isProbeQuery) { // 1b: duplicate probe
            tallyDupQuery(request);
        }
    }
	
    private boolean wasProbeQuery(QueryRequest request) {
        // if the current TTL is large enough and the old TTL was 1, then this
        // was a probe query....
        // NOTE: that i'm setting the ttl to be the actual ttl of the query.  i
        // could set it to some max value, but since we only allow TTL 1 queries
        // to be extended, it isn't a big deal what i set it to.  in fact, i'm
        // setting the ttl to the correct value if we had full expanding rings
        // of queries.
        return ((request.getTTL() > 0) && 
                _queryRouteTable.getAndSetTTL(request.getGUID(), (byte)1, 
                                              (byte)(request.getTTL()+1)));
    }

    private void tallyDupQuery(QueryRequest request) {
        ReceivedMessageStatHandler.TCP_DUPLICATE_QUERIES.addMessage(request);
    }

	/**
	 * Special handler for UDP queries.  Checks the routing table to see if
	 * the request has already been seen, handling it if not.
	 *
	 * @param query the UDP <tt>QueryRequest</tt> 
	 * @param handler the <tt>ReplyHandler</tt> that will handle the reply
	 * @return false if it was a duplicate, true if it was not.
	 */
	final boolean handleUDPQueryRequestPossibleDuplicate(QueryRequest request,
													  ReplyHandler handler)  {
		ResultCounter counter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 handler);
		if(counter != null) {
            handleQueryRequest(request, handler, counter, true);
            return true;
		}
		return false;
	}

    /**
     * Handles pings from the network.  With the addition of pong caching, this
     * method will either respond with cached pongs, or it will ignore the ping
     * entirely if another ping has been received from this connection very
     * recently.  If the ping is TTL=1, we will always process it, as it may
     * be a hearbeat ping to make sure the connection is alive and well.
     *
     * @param ping the ping to handle
     * @param handler the <tt>ReplyHandler</tt> instance that sent the ping
     */
    final private void handlePingRequest(PingRequest ping,
                                         ReplyHandler handler) {
        // Send it along if it's a heartbeat ping or if we should allow new 
        // pings on this connection.
        if(ping.isHeartbeat() || handler.allowNewPings()) {
            respondToPingRequest(ping, handler);
        } 
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
    protected void handleUDPPingRequest(PingRequest pingRequest,
										ReplyHandler handler, 
										InetSocketAddress addr) {
        if (pingRequest.isQueryKeyRequest())
            sendQueryKeyPong(pingRequest, addr);
        else
            respondToUDPPingRequest(pingRequest, addr, handler);
    }
    

    /**
     * Generates a AddressSecurityToken for the source (described by addr) and sends the
     * AddressSecurityToken to it via a AddressSecurityToken pong....
     */
    protected void sendQueryKeyPong(PingRequest pr, InetSocketAddress addr) {

        // check if we're getting bombarded
        long now = System.currentTimeMillis();
        if (now - _lastQueryKeyTime < SearchSettings.QUERY_KEY_DELAY.getValue())
            return;
        
        _lastQueryKeyTime = now;
        
        // after find more sources and OOB queries, everyone can dole out query
        // keys....

        // generate a AddressSecurityToken (quite quick - current impl. (DES) is super
        // fast!
        InetAddress address = addr.getAddress();
        int port = addr.getPort();
        AddressSecurityToken key = new AddressSecurityToken(address, port);
        
        // respond with Pong with QK, as GUESS requires....
        PingReply reply = 
            pingReplyFactory.createQueryKeyReply(pr.getGUID(), (byte)1, key);
        udpService.send(reply, addr.getAddress(), addr.getPort());
    }


    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {
        if (reply.getQueryKey() != null) {
            // this is a PingReply in reply to my AddressSecurityToken Request - 
            //consume the Pong and return, don't process as usual....
            onDemandUnicaster.handleQueryKeyPong(reply);
            return;
        }

        // do not process the pong if different from the host
        // described in the reply 
        if((reply.getPort() != port) || 
           (!reply.getInetAddress().equals(address))) {
            return;
		}
        
        // normal pong processing...
        handlePingReply(reply, handler);
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
     *
     * @param locallyEvaluate false if you don't want to send the query to
     * leaves and yourself, true otherwise....
     */
    protected void handleQueryRequest(QueryRequest request,
									  ReplyHandler handler, 
									  ResultCounter counter,
                                      boolean locallyEvaluate) {
        // Apply the personal filter to decide whether the callback
        // should be informed of the query
        if (!handler.isPersonalSpam(request)) {
            _callback.handleQueryString(request.getQuery());
        }
        
		// if it's a request from a leaf and we GUESS, send it out via GUESS --
		// otherwise, broadcast it if it still has TTL
		//if(handler.isSupernodeClientConnection() && 
		// RouterService.isGUESSCapable()) 
		//unicastQueryRequest(request, handler);
        //else if(request.getTTL() > 0) {
        updateMessage(request, handler);

		if(handler.isSupernodeClientConnection() && counter != null) {
            if (request.desiresOutOfBandReplies()) {
                // this query came from a leaf - so check if it desires OOB
                // responses and make sure that the IP it advertises is legit -
                // if it isn't drop away....
                // no need to check the port - if you are attacking yourself you
                // got problems
                String remoteAddr = handler.getInetAddress().getHostAddress();
                String myAddress = 
                    NetworkUtils.ip2string(networkManager.getAddress());
                if (request.getReplyAddress().equals(remoteAddr))
                    ; // continue below, everything looks good
                else if (request.getReplyAddress().equals(myAddress) && 
                         networkManager.isOOBCapable())
                    // i am proxying - maybe i should check my success rate but
                    // whatever...
                    ; 
                else return;
            }

            // don't send it to leaves here -- the dynamic querier will 
            // handle that
            locallyEvaluate = false;
            
            // do respond with files that we may have, though
            respondToQueryRequest(request, _clientGUID, handler);
            
            multicastQueryRequest(request);
            
			if(handler.isGoodLeaf()) {
				sendDynamicQuery(queryHandlerFactory.createHandlerForNewLeaf(request, 
																	  handler,
                                                                      counter), 
								 handler);
			} else {
				sendDynamicQuery(queryHandlerFactory.createHandlerForOldLeaf(request,
																	  handler,
                                                                      counter), 
								 handler);
			}
		} else if(request.getTTL() > 0 && RouterService.isSupernode()) {
            // send the request to intra-Ultrapeer connections -- this does
			// not send the request to leaves
            if(handler.isGoodUltrapeer()) {
                // send it to everyone
                forwardQueryToUltrapeers(request, handler);
            } else {
                // otherwise, only send it to some connections
                forwardLimitedQueryToUltrapeers(request, handler);
            }
		}
			
        if (locallyEvaluate) {
            // always forward any queries to leaves -- this only does
            // anything when this node's an Ultrapeer
            forwardQueryRequestToLeaves(request, handler);
            
            // if (I'm firewalled AND the source is firewalled) AND 
            // NOT(he can do a FW transfer and so can i) then don't reply...
            if ((request.isFirewalledSource() &&
                 !networkManager.acceptedIncomingConnection()) &&
                !(request.canDoFirewalledTransfer() &&
                  udpService.canDoFWT())
                )
                return;
            respondToQueryRequest(request, _clientGUID, handler);
        }
    }

    /** Handles a ACK message - looks up the QueryReply and sends it out of
     *  band.
     */
    protected void handleLimeACKMessage(LimeACKVendorMessage ack,
                                        InetSocketAddress addr) {

        GUID.TimedGUID refGUID = new GUID.TimedGUID(new GUID(ack.getGUID()),
                                                    TIMED_GUID_LIFETIME);
        QueryResponseBundle bundle = _outOfBandReplies.remove(refGUID);
        
        // token is null for old oob messages, it will just be ignored then
        SecurityToken securityToken = ack.getSecurityToken();
       
        if ((bundle != null) && (ack.getNumResults() > 0)) {
            InetAddress iaddr = addr.getAddress();
            int port = addr.getPort();

            //convert responses to QueryReplies, but only send as many as the
            //node wants
            Iterable<QueryReply> iterable;
            if (ack.getNumResults() < bundle._responses.length) {
                // TODO move selection to responseToQueryReplies methods for randomization
                Response[] desired = new Response[ack.getNumResults()];
                System.arraycopy(bundle._responses, 0, desired, 0, desired.length);
                iterable = responsesToQueryReplies(desired, bundle._query, 1, securityToken);
            } else { 
                iterable = responsesToQueryReplies(bundle._responses, 
                                                   bundle._query, 1, securityToken);
            }
            
            //send the query replies
            for(QueryReply queryReply : iterable)
                udpService.send(queryReply, iaddr, port);
        }
        // else some sort of routing error or attack?
        // TODO: tally some stat stuff here
    }
    
    /**
     * Adds the address of <code>handler</code> to the {@link BypassedResultsCache}
     * if it can receive unsolicited udp.
     * 
     * @return true if successfully added to the bypassed results cache
     */
    public boolean addBypassedSource(ReplyNumberVendorMessage reply, ReplyHandler handler) {
        
        //if the reply cannot receive unsolicited udp, there is no point storing it
        if (!reply.canReceiveUnsolicited()) {
            return false;
        }

        GUESSEndpoint ep = new GUESSEndpoint(handler.getInetAddress(), handler.getPort());
        return _bypassedResultsCache.addBypassedSource(new GUID(reply.getGUID()), ep);
    }
    
    /**
     * Adds the address of <code>handler</code> to the {@link BypassedResultsCache}
     * if it is likely to not be firewalled.
     */
    public boolean addBypassedSource(QueryReply reply, ReplyHandler handler) {
        try {
            if (reply.getHostData().isFirewalled())
                return false;
        } catch (BadPacketException bpe){
            return false;
        }
        GUESSEndpoint ep = new GUESSEndpoint(handler.getInetAddress(), handler.getPort());
        return _bypassedResultsCache.addBypassedSource(new GUID(reply.getGUID()), ep);
    }

    /**
     * Returns the number of results to request from source of <code>reply</code>.
     * 
     * @return -1 if no results are desired
     */
    public int getNumOOBToRequest(ReplyNumberVendorMessage reply) {
    	GUID qGUID = new GUID(reply.getGUID());
    	
        int numResults = searchResultHandler.getNumResultsForQuery(qGUID);
    	
        if (numResults < 0) // this may be a proxy query
    		numResults = DYNAMIC_QUERIER.getLeafResultsForQuery(qGUID);

        if (numResults < 0 || numResults > QueryHandler.ULTRAPEER_RESULTS) {
            return -1;
        }
        
    	return reply.getNumResults();
    }
    
    /**
     * @return true if there is still a route for this reply
     */
    public boolean isQueryAlive(GUID guid) {
        return _queryRouteTable.getReplyHandler(guid.bytes()) != null;
    }

    /** Stores (for a limited time) the resps for later out-of-band delivery -
     *  interacts with handleLimeACKMessage
     *  @return true if the operation failed, false if not (i.e. too busy)
     */
    protected boolean bufferResponsesForLaterDelivery(QueryRequest query,
                                                      Response[] resps) {
        // store responses by guid for later retrieval
        synchronized (_outOfBandReplies) {
            if (_outOfBandReplies.size() < MAX_BUFFERED_REPLIES) {
                GUID.TimedGUID tGUID = 
                    new GUID.TimedGUID(new GUID(query.getGUID()),
                                       TIMED_GUID_LIFETIME);
                _outOfBandReplies.put(tGUID, new QueryResponseBundle(query, 
                                                                     resps));
                return true;
            }
            return false;
        }
    }
    
    /**
     * Determines if we've sent a unicast OOB query to
     * the given host using the given query GUID.
     */
    public boolean isHostUnicastQueried(GUID guid, IpPort host) {
        return onDemandUnicaster.isHostQueriedForGUID(guid, host);
    }
    
    /**
     * Forwards the UDPConnectBack to neighboring peers
     * as a UDPConnectBackRedirect request.
     */
    protected void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connection source) {

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new UDPConnectBackRedirect(guidToUse, sourceAddr, 
                                                 portToContact);

        int sentTo = 0;
        List<ManagedConnection> peers =
            new ArrayList<ManagedConnection>(connectionManager.getInitializedConnections());
        Collections.shuffle(peers);
        for(ManagedConnection currMC : peers) {
            if(sentTo >= MAX_UDP_CONNECTBACK_FORWARDS)
                break;
            
            if(currMC == source)
                continue;

            if (currMC.remoteHostSupportsUDPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }
    }


    /**
     * Sends a ping to the person requesting the connectback request.
     */
    protected void handleUDPConnectBackRedirect(UDPConnectBackRedirect udp,
                                               Connection source) {
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection())
            return;

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress addrToContact = udp.getConnectBackAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact.getAddress(),
                                         portToContact);
        if (connectionManager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        String addrString = addrToContact.getHostAddress();
        if (!shouldServiceRedirect(_udpConnectBacks,addrString))
            return;

        // mutating twice restores the original guid
        UDPService.mutateGUID(guidToUse.bytes(), addrToContact, portToContact);
        PingRequest pr = new PingRequest(guidToUse.bytes(), (byte) 1,
                                         (byte) 0);
        udpService.send(pr, addrToContact, portToContact);
    }
    
    /**
     * @param map the map that keeps track of recent redirects
     * @param key the key which we would (have) store(d) in the map
     * @return whether we should service the redirect request
     * @modifies the map
     */
    private boolean shouldServiceRedirect(FixedsizeHashMap<String, String> map, String key) {
        synchronized(map) {
            String placeHolder = map.get(key);
            if (placeHolder == null) {
                try {
                    map.put(key, key);
                    return true;
                } catch (NoMoreStorageException nomo) {
                    return false;  // we've done too many connect backs, stop....
                }
            } else 
                return false;  // we've connected back to this guy recently....
        }
    }



    /**
     * Forwards the request to neighboring Ultrapeers as a
     * TCPConnectBackRedirect message.
     */
    protected void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connection source) {
        final int portToContact = tcp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new TCPConnectBackRedirect(sourceAddr, portToContact);

        int sentTo = 0;
        List<ManagedConnection> peers =
            new ArrayList<ManagedConnection>(connectionManager.getInitializedConnections());
        Collections.shuffle(peers);
        for(ManagedConnection currMC : peers) {
            if(sentTo >= MAX_TCP_CONNECTBACK_FORWARDS)
                break;
            
            if(currMC == source)
                continue;

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
    protected void handleTCPConnectBackRedirect(TCPConnectBackRedirect tcp,
                                                Connection source) {
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection())
            return;

        final int portToContact = tcp.getConnectBackPort();
        final String addrToContact =tcp.getConnectBackAddress().getHostAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact, portToContact);
        if (connectionManager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        if (!shouldServiceRedirect(_tcpConnectBacks,addrToContact))
            return;

        TCP_CONNECT_BACKER.execute(new Runnable() {
            public void run() {
                Socket sock = null;
                try {
                    // DPINJ: Change to using passed-in SocketsManager!!!
                    sock = socketsManager.connect(new InetSocketAddress(addrToContact, portToContact), 12000);
                    OutputStream os = sock.getOutputStream();
                    os.write("CONNECT BACK\r\n\r\n".getBytes());
                    os.flush();
                    if(LOG.isTraceEnabled())
                        LOG.trace("Succesful connectback to: " + addrToContact);
                    try {
                        Thread.sleep(500); // let the other side get it.
                    } catch(InterruptedException ignored) {
                        LOG.warn("Interrupted connectback", ignored);
                    }
                } catch (IOException ignored) {
                    LOG.warn("IOX during connectback", ignored);
                } finally {
                    IOUtils.close(sock);
                }
            }
        });
    }


    /**
     * 1) confirm that the connection is Ultrapeer to Leaf, then send your
     * listening port in a PushProxyAcknowledgement.
     * 2) Also cache the client's client GUID.
     */
    protected void handlePushProxyRequest(PushProxyRequest ppReq,
                                          ManagedConnection source) {
        if (source.isSupernodeClientConnection() 
                && networkManager.isIpPortValid()) {
            String stringAddr = 
                NetworkUtils.ip2string(networkManager.getAddress());
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(stringAddr);
            } catch(UnknownHostException uhe) {
                ErrorService.error(uhe); // impossible
            }

            // 1)
            PushProxyAcknowledgement ack = 
                new PushProxyAcknowledgement(addr,networkManager.getPort(),
                                             ppReq.getClientGUID());
            source.send(ack);
            
            // 2)
            _pushRouteTable.routeReply(ppReq.getClientGUID().bytes(), source);
            source.setPushProxyFor(true);
        }
    }

    /** This method should be invoked when this node receives a
     *  QueryStatusResponse message from the wire.  If this node is an
     *  Ultrapeer, we should update the Dynamic Querier about the status of
     *  the leaf's query.
     */    
    protected void handleQueryStatus(QueryStatusResponse resp,
                                     ManagedConnection leaf) {
        // message only makes sense if i'm a UP and the sender is a leaf
        if (!leaf.isSupernodeClientConnection())
            return;

        GUID queryGUID = resp.getQueryGUID();
        int numResults = resp.getNumResults();
        
        // get the QueryHandler and update the stats....
        DYNAMIC_QUERIER.updateLeafResultsForQuery(queryGUID, numResults);
    }


    /**
     * Sends the ping request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest request,
                                ManagedConnection connection) {
        if(request == null) {
            throw new NullPointerException("null ping");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _pingRouteTable.routeReply(request.getGUID(), forMeReplyHandler);
        connection.send(request);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest request,
                                 ManagedConnection connection) {        
        if(request == null) {
            throw new NullPointerException("null query");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _queryRouteTable.routeReply(request.getGUID(), forMeReplyHandler);
        connection.send(request);
    }

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastPingRequest(PingRequest ping) {
		if(ping == null) {
			throw new NullPointerException("null ping");
		}
        _pingRouteTable.routeReply(ping.getGUID(), forMeReplyHandler);
        broadcastPingRequest(ping, forMeReplyHandler, connectionManager);
    }

	/**
	 * Generates a new dynamic query.  This method is used to send a new 
	 * dynamic query from this host (the user initiated this query directly,
	 * so it's replies are intended for this node.
	 *
	 * @param query the <tt>QueryRequest</tt> instance that generates
	 *  queries for this dynamic query
	 * @throws <tt>NullPointerException</tt> if the <tt>QueryHandler</tt> 
	 *  argument is <tt>null</tt>
	 */
	public void sendDynamicQuery(QueryRequest query) {
		if(query == null) {
			throw new NullPointerException("null QueryHandler");
		}
		// get the result counter so we can track the number of results
		ResultCounter counter = 
			_queryRouteTable.routeReply(query.getGUID(), 
										forMeReplyHandler);
		if(RouterService.isSupernode()) {
			sendDynamicQuery(queryHandlerFactory.createHandlerForMe(query, 
                                                             counter), 
							 forMeReplyHandler);
		} else {
            originateLeafQuery(query);
		} 
		
		// always send the query to your multicast people
        originateMulticastQuery(query);
    }
    
    /**
     * Originates a multicast query from this host.
     * This will alter the GUID of the query and store it in a mapping
     * of new -> old GUID.  When replies come in, if they have the new GUID,
     * they are reset to be the old one and the multicast flag is allowed.
     * 
     * @param query
     * @return the newGUID that the multicast query is using.
     */
    protected void originateMulticastQuery(QueryRequest query) {
        byte[] newGUID = GUID.makeGuid();
        QueryRequest mquery = queryRequestFactory.createMulticastQuery(newGUID, query);
        _multicastGuidMap.addMapping(query.getGUID(), newGUID, MULTICAST_GUID_EXPIRE_TIME);
		multicastQueryRequest(mquery);
	}

	/**
	 * Initiates a dynamic query.  Only Ultrapeer should call this method,
	 * as this technique relies on fairly high numbers of connections to 
	 * dynamically adjust the TTL based on the number of results received, 
	 * the number of remaining connections, etc.
	 *
	 * @param qh the <tt>QueryHandler</tt> instance that generates
	 *  queries for this dynamic query
     * @param handler the <tt>ReplyHandler</tt> for routing replies for
     *  this query
	 * @throws <tt>NullPointerException</tt> if the <tt>ResultCounter</tt>
	 *  for the guid cannot be found -- this should never happen, or if any
	 *  of the arguments is <tt>null</tt>
	 */
	private void sendDynamicQuery(QueryHandler qh, ReplyHandler handler) {
		if(qh == null) {
			throw new NullPointerException("null QueryHandler");
		} else if(handler == null) {
			throw new NullPointerException("null ReplyHandler");
		} 
		DYNAMIC_QUERIER.addQuery(qh);
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
    private void broadcastPingRequest(PingRequest request,
                                      ReplyHandler receivingConnection,
                                      ConnectionManager manager) {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.

        //Broadcast the ping to other connected nodes (supernodes or older
        //nodes), but DON'T forward any ping not originating from me 
        //along leaf to ultrapeer connections.
        List<ManagedConnection> list = manager.getInitializedConnections();
        int size = list.size();

        boolean randomlyForward = false;
        if(size > 3) randomlyForward = true;
        double percentToIgnore;
        for(int i=0; i<size; i++) {
            ManagedConnection mc = list.get(i);
            if(!mc.isStable()) continue;
            if (receivingConnection == forMeReplyHandler || 
                (mc != receivingConnection && 
                 !mc.isClientSupernodeConnection())) {

                if(mc.supportsPongCaching()) {
                    percentToIgnore = 0.70;
                } else {
                    percentToIgnore = 0.90;
                }
                if(randomlyForward && 
                   (Math.random() < percentToIgnore)) {
                    continue;
                } else {
                    mc.send(request);
                }
            }
        }
    }

	/**
	 * Forwards the query request to any leaf connections.
	 *
	 * @param request the query to forward
	 * @param handler the <tt>ReplyHandler</tt> that responds to the
	 *  request appropriately
	 * @param manager the <tt>ConnectionManager</tt> that provides
	 *  access to any leaf connections that we should forward to
     */
	public final void forwardQueryRequestToLeaves(QueryRequest query,
                                                  ReplyHandler handler) {
		if(!RouterService.isSupernode()) return;
        //use query routing to route queries to client connections
        //send queries only to the clients from whom query routing 
        //table has been received
        List<ManagedConnection> list = connectionManager.getInitializedClientConnections();
        List<ManagedConnection> hitConnections = new ArrayList<ManagedConnection>();
        for(int i=0; i<list.size(); i++) {
            ManagedConnection mc = list.get(i);
            if(mc == handler) continue;
            if(mc.shouldForwardQuery(query)) {
                hitConnections.add(mc);
            }
        }
        //forward only to a quarter of the leaves in case the query is
        //very popular.
        if(list.size() > 8 && 
           (double)hitConnections.size()/(double)list.size() > .8) {
        	int startIndex = (int) Math.floor(
        			Math.random() * hitConnections.size() * 0.75);
            hitConnections = 
                hitConnections.subList(startIndex, startIndex+hitConnections.size()/4);
        }
        
        int notSent = list.size() - hitConnections.size();
        RoutedQueryStat.LEAF_DROP.addData(notSent);
        
        for(int i=0; i<hitConnections.size(); i++) {
            ManagedConnection mc = hitConnections.get(i);
            
            // sendRoutedQueryToHost is not called because 
            // we have already ensured it hits the routing table
            // by filling up the 'hitsConnection' list.
            mc.send(query);
            RoutedQueryStat.LEAF_SEND.incrementStat();
        }
	}

	/**
	 * Factored-out method that sends a query to a connection that supports
	 * query routing.  The query is only forwarded if there's a hit in the
	 * query routing entries.
	 *
	 * @param query the <tt>QueryRequest</tt> to potentially forward
	 * @param mc the <tt>ManagedConnection</tt> to forward the query to
	 * @param handler the <tt>ReplyHandler</tt> that will be entered into
	 *  the routing tables to handle any replies
	 * @return <tt>true</tt> if the query was sent, otherwise <tt>false</tt>
	 */
	private boolean sendRoutedQueryToHost(QueryRequest query, ManagedConnection mc,
										  ReplyHandler handler) {
		if (mc.shouldForwardQuery(query)) {
			//A new client with routing entry, or one that hasn't started
			//sending the patch.
			mc.send(query);
			return true;
		}
		return false;
	}

    /**
     * Adds the QueryRequest to the unicaster module.  Not much work done here,
     * see QueryUnicaster for more details.
     */
    protected void unicastQueryRequest(QueryRequest query,
                                       ReplyHandler conn) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
				
		queryUnicaster.addQuery(query, conn);
	}
	
    /**
     * Send the query to the multicast group.
     */
    protected void multicastQueryRequest(QueryRequest query) {
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
		// record the stat
		SentMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(query);
				
		multicastService.send(query);
	}	


    /**
     * Broadcasts the query request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated QueryReplyHandler.  This is called from teh default
     * handleQueryRequest and the default broadcastQueryRequest(QueryRequest)
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    private void forwardQueryToUltrapeers(QueryRequest query,
                                          ReplyHandler handler) {
		// Note the use of initializedConnections only.
		// Note that we have zero allocations here.
		
		//Broadcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forward any queries not originating from me 
		//along leaf to ultrapeer connections.
	 
		List<ManagedConnection> list = connectionManager.getInitializedConnections();
        int limit = list.size();

		for(int i=0; i<limit; i++) {
			ManagedConnection mc = list.get(i);      
            forwardQueryToUltrapeer(query, handler, mc);  
        }
    }

    /**
     * Performs a limited broadcast of the specified query.  This is
     * useful, for example, when receiving queries from old-style 
     * connections that we don't want to forward to all connected
     * Ultrapeers because we don't want to overly magnify the query.
     *
     * @param query the <tt>QueryRequest</tt> instance to forward
     * @param handler the <tt>ReplyHandler</tt> from which we received
     *  the query
     */
    private void forwardLimitedQueryToUltrapeers(QueryRequest query,
                                                 ReplyHandler handler) {
		//Broadcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forward any queries not originating from me 
		//along leaf to ultrapeer connections.
	 
		List<ManagedConnection> list = connectionManager.getInitializedConnections();
        int limit = list.size();

        int connectionsNeededForOld = OLD_CONNECTIONS_TO_USE;
		for(int i=0; i<limit; i++) {
            
            // if we've already queried enough old connections for
            // an old-style query, break out
            if(connectionsNeededForOld == 0) break;

			ManagedConnection mc = list.get(i);
            
            // if the query is comiing from an old connection, try to
            // send it's traffic to old connections.  Only send it to
            // new connections if we only have a minimum number left
            if(mc.isGoodUltrapeer() && 
               (limit-i) > connectionsNeededForOld) {
                continue;
            }
            forwardQueryToUltrapeer(query, handler, mc);
            
            // decrement the connections to use
            connectionsNeededForOld--;
		}    
    }

    /**
     * Forwards the specified query to the specified Ultrapeer.  This
     * encapsulates all necessary logic for forwarding queries to
     * Ultrapeers, for example handling last hop Ultrapeers specially
     * when the receiving Ultrapeer supports Ultrapeer query routing,
     * meaning that we check it's routing tables for a match before sending 
     * the query.
     *
     * @param query the <tt>QueryRequest</tt> to forward
     * @param handler the <tt>ReplyHandler</tt> that sent the query
     * @param ultrapeer the Ultrapeer to send the query to
     */
    private void forwardQueryToUltrapeer(QueryRequest query, 
                                         ReplyHandler handler,
                                         ManagedConnection ultrapeer) {    
        // don't send a query back to the guy who sent it
        if(ultrapeer == handler) return;

        // make double-sure we don't send a query received
        // by a leaf to other Ultrapeers
        if(ultrapeer.isClientSupernodeConnection()) return;

        // make sure that the ultrapeer understands feature queries.
        if(query.isFeatureQuery() && 
           !ultrapeer.getRemoteHostSupportsFeatureQueries())
             return;

        // is this the last hop for the query??
		boolean lastHop = query.getTTL() == 1; 
           
        // if it's the last hop to an Ultrapeer that sends
        // query route tables, route it.
        if(lastHop &&
           ultrapeer.isUltrapeerQueryRoutingConnection()) {
            boolean sent = sendRoutedQueryToHost(query, ultrapeer, handler);
            if(sent)
                RoutedQueryStat.ULTRAPEER_SEND.incrementStat();
            else
                RoutedQueryStat.ULTRAPEER_DROP.incrementStat();
        } else {
            // otherwise, just send it out
            ultrapeer.send(query);
        }
    }


    /**
     * Originate a new query from this leaf node.
     *
     * @param qr the <tt>QueryRequest</tt> to send
     */
    private void originateLeafQuery(QueryRequest qr) {
		List<ManagedConnection> list = connectionManager.getInitializedConnections();

        // only send to at most 4 Ultrapeers, as we could have more
        // as a result of race conditions - also, don't send what is new
        // requests down too many connections
        final int max = qr.isWhatIsNewRequest() ? 2 : 3;
	int start = !qr.isWhatIsNewRequest() ? 0 :
		(int) (Math.floor(Math.random()*(list.size()-1)));
        int limit = Math.min(max, list.size());
        final boolean wantsOOB = qr.desiresOutOfBandReplies();
        for(int i=start; i<start+limit; i++) {
			ManagedConnection mc = list.get(i);
            QueryRequest qrToSend = qr;
            if (wantsOOB && (mc.remoteHostSupportsLeafGuidance() < 0))
                qrToSend = queryRequestFactory.unmarkOOBQuery(qr);
            mc.originateQuery(qrToSend);
        }
    }
    
    /**
     * Originates a new query request to the ManagedConnection.
     *
     * @param request The query to send.
     * @param mc The ManagedConnection to send the query along
     * @return false if the query was not sent, true if so
     */
    public boolean originateQuery(QueryRequest query, ManagedConnection mc) {
        if( query == null )
            throw new NullPointerException("null query");
        if( mc == null )
            throw new NullPointerException("null connection");
    
        // if this is a feature query & the other side doesn't
        // support it, then don't send it
        // This is an optimization of network traffic, and doesn't
        // necessarily need to exist.  We could be shooting ourselves
        // in the foot by not sending this, rendering Feature Searches
        // inoperable for some users connected to bad Ultrapeers.
        if(query.isFeatureQuery() && !mc.getRemoteHostSupportsFeatureQueries())
            return false;
        
        mc.originateQuery(query);
        return true;
    }
    
    /**
     * Respond to the ping request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendPingReply(PingReply).
     * This method is called from the default handlePingRequest.
     */
    protected abstract void respondToPingRequest(PingRequest request,
                                                 ReplyHandler handler);

	/**
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
    protected abstract void respondToUDPPingRequest(PingRequest request, 
													InetSocketAddress addr,
                                                    ReplyHandler handler);


    /**
     * Respond to the query request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is called from the default handleQueryRequest.
     */
    protected abstract boolean respondToQueryRequest(QueryRequest queryRequest,
                                                     byte[] clientGUID,
                                                     ReplyHandler handler);

    /**
     * The default handler for PingRequests received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the ping route table to route a ping reply.  If an appropriate route
     * doesn't exist, records the error statistics.  On sucessful routing,
     * the PingReply count is incremented.<p>
     *
     * In all cases, the ping reply is recorded into the host catcher.<p>
     *
     * Override as desired, but you probably want to call super.handlePingReply
     * if you do.
     */
    protected void handlePingReply(PingReply reply,
                                   ReplyHandler handler) {
        //update hostcatcher (even if the reply isn't for me)
        boolean newAddress = hostCatcher.add(reply);

        if(newAddress && !reply.isUDPHostCache()) {
            ProviderHacks.getPongCacher().addPong(reply);
        }

        //First route to originator in usual manner.
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(reply.getGUID());

        if(replyHandler != null) {
            replyHandler.handlePingReply(reply, handler);
        }
        else {
            RouteErrorStat.PING_REPLY_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
		boolean supportsUnicast = reply.supportsUnicast();
        
        //Then, if a marked pong from an Ultrapeer that we've never seen before,
        //send to all leaf connections except replyHandler (which may be null),
        //irregardless of GUID.  The leafs will add the address then drop the
        //pong as they have no routing entry.  Note that if Ultrapeers are very
        //prevalent, this may consume too much bandwidth.
		//Also forward any GUESS pongs to all leaves.
        if (newAddress && (reply.isUltrapeer() || supportsUnicast)) {
            List<ManagedConnection> list=connectionManager.getInitializedClientConnections();
            for (int i=0; i<list.size(); i++) {
                ManagedConnection c = list.get(i);
                assert c != null : "null c.";
                if (c!=handler && c!=replyHandler && c.allowNewPongs()) {
                    c.handlePingReply(reply, handler);
                }
            }
        }
    }

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
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler handler) {
        if(queryReply == null) {
            throw new NullPointerException("null query reply");
        }
        if(handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getUniqueResultCount(),
                                             ByteOrder.beb2int(queryReply.getIPBytes(), 0));

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       handler);
            //Simple flow control: don't route this message along other
            //connections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.

            ReplyHandler rh = rrp.getReplyHandler();
            
            // remember more stats
            _queryRouteTable.countHopsTTLNet(queryReply);
            // if this reply is for us, remember even more stats
            if (rh == forMeReplyHandler)
                _queryRouteTable.timeStampResults(queryReply);
            
            if(!shouldDropReply(rrp, rh, queryReply)) {                
                rh.handleQueryReply(queryReply, handler);
                // also add to the QueryUnicaster for accounting - basically,
                // most results will not be relevant, but since it is a simple
                // HashSet lookup, it isn't a prohibitive expense...
                queryUnicaster.handleQueryReply(queryReply);

            } else {
				RouteErrorStat.HARD_LIMIT_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
                final byte ttl = queryReply.getTTL();
                if (ttl < RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length)
				    RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[ttl].incrementStat();
                else
				    RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length-1].incrementStat();
                handler.countDroppedMessage();
            }
        }
        else {
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
    private boolean shouldDropReply(RouteTable.ReplyRoutePair rrp,
                                    ReplyHandler rh,
                                    QueryReply qr) {
        int ttl = qr.getTTL();
                                           
        // Reason 2 --  The reply is meant for me, do not drop it.
        if( rh == forMeReplyHandler ) return false;
        
        // Reason 3 -- drop if TTL is 0.
        if( ttl == 0 ) return true;                

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

    /**
     *  If we get and SimppRequest, get the payload we need from the
     *  SimppManager and send the simpp bytes the the requestor in a SimppVM. 
     */
    private void handleSimppRequest(final SimppRequestVM simppReq, 
                                                  final ReplyHandler handler ) {
        if(simppReq.getVersion() > SimppRequestVM.VERSION)
            return; //we are not going to deal with these types of requests. 
        byte[] simppBytes = SimppManager.instance().getSimppBytes();
        if(simppBytes != null && simppBytes.length > 0 ) {
            SimppVM simppVM = new SimppVM(simppBytes);
            try {
                handler.handleSimppVM(simppVM);
            } catch(IOException iox) {//uanble to send the SimppVM. Nothing I can do
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
    private void handleSimppVM(SimppVM simppVM, ReplyHandler handler) {
        SimppManager.instance().checkAndUpdate(handler, simppVM.getPayload());
    }

    /**
     *  Handles an update request by sending a response.
     */
    private void handleUpdateRequest(UpdateRequest req, ReplyHandler handler ) {

        byte[] data = ProviderHacks.getUpdateHandler().getLatestBytes();
        if(data != null) {
            UpdateResponse msg = UpdateResponse.createUpdateResponse(data,req);
            handler.reply(msg);
        }
    }
    
    /** Handles a ContentResponse msg -- passing it to the ContentManager. */
    private void handleContentResponse(ContentResponse msg, ReplyHandler handler) {
        contentManager.handleContentResponse(msg);
    }

    /**
     * Passes the request onto the update manager.
     */
    private void handleUpdateResponse(UpdateResponse resp, ReplyHandler handler) {
        ProviderHacks.getUpdateHandler().handleNewData(resp.getUpdate(), handler);
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
    protected void handlePushRequest(PushRequest request,
                                  ReplyHandler handler) {
        if(request == null) {
            throw new NullPointerException("null request");
        }
        if(handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(request.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(request, handler);
        else {
			RouteErrorStat.PUSH_REQUEST_ROUTE_ERRORS.incrementStat();
            handler.countDroppedMessage();
        }
    }
    
    /**
     * Returns the appropriate handler from the _pushRouteTable.
     * This enforces that requests for my clientGUID will return
     * FOR_ME_REPLY_HANDLER, even if it's not in the table.
     */
    protected ReplyHandler getPushHandler(byte[] guid) {
        ReplyHandler replyHandler = _pushRouteTable.getReplyHandler(guid);
        if(replyHandler != null)
            return replyHandler;
        else if(Arrays.equals(_clientGUID, guid))
            return forMeReplyHandler;
        else
            return null;
    }

    /**
     * Uses the ping route table to send a PingReply to the appropriate
     * connection.  Since this is used for PingReplies orginating here, no
     * stats are updated.
     */
    protected void sendPingReply(PingReply pong, ReplyHandler handler) {
        if(pong == null) {
            throw new NullPointerException("null pong");
        }

        if(handler == null) {
            throw new NullPointerException("null reply handler");
        }
 
        handler.handlePingReply(pong, null);
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    protected void sendQueryReply(QueryReply queryReply)
        throws IOException {
        
        if(queryReply == null) {
            throw new NullPointerException("null reply");
        }
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount(),
                                             0);

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            rrp.getReplyHandler().handleQueryReply(queryReply, null);
        }
        else
            throw new IOException("no route for reply");
    }

    /**
     * Uses the push route table to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPushRequest(PushRequest push)
        throws IOException {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        

        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(push.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(push, forMeReplyHandler);
        else
            throw new IOException("no route for push");
    }
    
    /**
     * Sends a push request to the multicast network.  No lookups are
     * performed in the push route table, because the message will always
     * be broadcast to everyone.
     */
    public void sendMulticastPushRequest(PushRequest push) {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        
        // must have a TTL of 1
        assert push.getTTL() == 1 : "multicast push ttl not 1";
        
        multicastService.send(push);
        SentMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(push);
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
     * @return Iterable of QueryReply
     */
    public Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
                                            QueryRequest queryRequest) {
        return responsesToQueryReplies(responses, queryRequest, 10, null);
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
     * @param REPLY_LIMIT the maximum number of responses to have in each reply.
     * @param security token might be null
     * @return Iterable of QueryReply
     */
    private Iterable<QueryReply> responsesToQueryReplies(Response[] responses,
                                             QueryRequest queryRequest,
                                             final int REPLY_LIMIT, SecurityToken securityToken) {

        //List to store Query Replies
        List<QueryReply> queryReplies = new LinkedList<QueryReply>();
        
        // get the appropriate queryReply information
        byte[] guid = queryRequest.getGUID();
        byte ttl = (byte)(queryRequest.getHops() + 1);

        //Return measured speed if possible, or user's speed otherwise.
        long speed = uploadManager.measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=ConnectionSettings.CONNECTION_SPEED.getValue();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        // limit the responses if we're not delivering this 
        // out-of-band and we have a lot of responses
        if(REPLY_LIMIT > 1 && 
           numHops > 2 && 
           numResponses > HIGH_HOPS_RESPONSE_LIMIT) {
            int j = 
                (int)(Math.random() * numResponses) % 
                (numResponses - HIGH_HOPS_RESPONSE_LIMIT);

            Response[] newResponses = 
                new Response[HIGH_HOPS_RESPONSE_LIMIT];
            for(int i=0; i<10; i++, j++) {
                newResponses[i] = responses[j];
            }
            responses = newResponses;
            numResponses = responses.length;
        }
        while (numResponses > 0) {
            int arraySize;
            // if there are more than 255 responses,
            // create an array of 255 to send in the queryReply
            // otherwise, create an array of whatever size is left.
            if (numResponses < REPLY_LIMIT) {
                // break;
                arraySize = numResponses;
            }
            else
                arraySize = REPLY_LIMIT;

            Response[] res;
            // a special case.  in the common case where there
            // are less than 256 responses being sent, there
            // is no need to copy one array into another.
            if ( (index == 0) && (arraySize < REPLY_LIMIT) ) {
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

			// see if there are any open slots
            // Note: if we are busy, non-metafile results would be filtered.
            // by this point.
			boolean busy = !uploadManager.mayBeServiceable();
            boolean uploaded = uploadManager.hadSuccesfulUpload();
			
            // We only want to return a "reply to multicast query" QueryReply
            // if the request travelled a single hop.
			boolean mcast = queryRequest.isMulticast() && 
                (queryRequest.getTTL() + queryRequest.getHops()) == 1;
			
            // We should mark our hits if the remote end can do a firewalled
            // transfer AND so can we AND we don't accept tcp incoming AND our
            // external address is valid (needed for input into the reply)
            final boolean fwTransfer = 
                queryRequest.canDoFirewalledTransfer() && 
                networkManager.canDoFWT() &&
                !networkManager.acceptedIncomingConnection();
            
			if ( mcast ) {
                ttl = 1; // not strictly necessary, but nice.
            }
            
            List<QueryReply> replies =
                createQueryReply(guid, ttl, speed, res, 
                                 _clientGUID, busy, uploaded, 
                                 measuredSpeed, mcast,
                                 fwTransfer, securityToken);

            //add to the list
            queryReplies.addAll(replies);

        }//end of while
        
        return queryReplies;
    }

    /**
     * Abstract method for creating query hits.  Subclasses must specify
     * how this list is created.
     * 
     * @param securityToken might be null, otherwise must be sent in GGEP
     * of QHD with header "SO"
     *
     * @return a <tt>List</tt> of <tt>QueryReply</tt> instances
     */
    protected abstract List<QueryReply> createQueryReply(byte[] guid, byte ttl,
                                            long speed, 
                                             Response[] res, byte[] clientGUID, 
                                             boolean busy, 
                                             boolean uploaded, 
                                             boolean measuredSpeed, 
                                             boolean isFromMcast,
                                             boolean shouldMarkForFWTransfer,
                                             SecurityToken securityToken);

    /**
     * Handles a message to reset the query route table for the given
     * connection.
     *
     * @param rtm the <tt>ResetTableMessage</tt> for resetting the query
     *  route table
     * @param mc the <tt>ManagedConnection</tt> for which the query route
     *  table should be reset
     */
    private void handleResetTableMessage(ResetTableMessage rtm,
                                         ManagedConnection mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // reset the query route table for this connection
        synchronized (mc.getQRPLock()) {
            mc.resetQueryRouteTable(rtm);
        }

        // if this is coming from a leaf, make sure we update
        // our tables so that the dynamic querier has correct
        // data
        if(mc.isLeafConnection()) {
            _lastQueryRouteTable = createRouteTable();
        }
    }

    /**
     * Handles a message to patch the query route table for the given
     * connection.
     *
     * @param rtm the <tt>PatchTableMessage</tt> for patching the query
     *  route table
     * @param mc the <tt>ManagedConnection</tt> for which the query route
     *  table should be patched
     */
    private void handlePatchTableMessage(PatchTableMessage ptm,
                                         ManagedConnection mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // patch the query route table for this connection
        synchronized(mc.getQRPLock()) {
            mc.patchQueryRouteTable(ptm);
        }

        // if this is coming from a leaf, make sure we update
        // our tables so that the dynamic querier has correct
        // data
        if(mc.isLeafConnection()) {
            _lastQueryRouteTable = createRouteTable();
        }
    }

    private void updateMessage(QueryRequest request, ReplyHandler handler) {
        
        if (SearchSettings.SEND_LIME_RESPONSES.getBoolean() &&
                request.isQueryForLW() && staticMessages.getLimeReply() != null) {
            QueryReply qr = queryReplyFactory.createQueryReply(request.getGUID(), staticMessages.getLimeReply());
            qr.setHops((byte)0);
            qr.setTTL((byte)(request.getHops()+1));
            try {
                sendQueryReply(qr);
                LimeSentMessageStat.LIME_REPLIES_SENT.incrementStat();
            } catch (IOException ignored) {}
        }
        
        if (!(handler instanceof Connection)) 
            return;
        
        Connection c = (Connection) handler;
        QueryReply update = staticMessages.getUpdateReply();
        if (request.getHops() == 1 && c.isOldLimeWire()) {
            if (update != null) {
                QueryReply qr = queryReplyFactory.createQueryReply(request.getGUID(), update);
                try {
                    sendQueryReply(qr);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Utility method for checking whether or not the given connection
     * is able to pass QRP messages.
     *
     * @param c the <tt>Connection</tt> to check
     * @return <tt>true</tt> if this is a QRP-enabled connection,
     *  otherwise <tt>false</tt>
     */
    private static boolean isQRPConnection(Connection c) {
        if(c.isSupernodeClientConnection()) return true;
        if(c.isUltrapeerQueryRoutingConnection()) return true;
        return false;
    }

    /** Thread the processing of QRP Table delivery. */
    private class QRPPropagator extends ManagedThread {
        public QRPPropagator() {
            setName("QRPPropagator");
            setDaemon(true);
        }

        /** While the connection is not closed, sends all data delay. */
        public void run() {
            try {
                while (true) {
					// Check for any scheduled QRP table propagations
					// every 10 seconds
                    Thread.sleep(10*1000);
    				forwardQueryRouteTables();
                }
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }


    /**
     * Sends updated query routing tables to all connections which haven't
     * been updated in a while.  You can call this method as often as you want;
     * it takes care of throttling.
     *     @modifies connections
     */    
    private void forwardQueryRouteTables() {
		//Check the time to decide if it needs an update.
		long time = System.currentTimeMillis();

		//For all connections to new hosts c needing an update...
		List<ManagedConnection> list=connectionManager.getInitializedConnections();
		QueryRouteTable table = null;
		List<RouteTableMessage> patches = null;
		QueryRouteTable lastSent = null;
		
		for(int i=0; i<list.size(); i++) {                        
			ManagedConnection c = list.get(i);
			

			// continue if I'm an Ultrapeer and the node on the
			// other end doesn't support Ultrapeer-level query
			// routing
			if(RouterService.isSupernode()) { 
				// only skip it if it's not an Ultrapeer query routing
				// connection
				if(!c.isUltrapeerQueryRoutingConnection()) { 
					continue;
				}
			} 				
			// otherwise, I'm a leaf, and don't send routing
			// tables if it's not a connection to an Ultrapeer
			// or if query routing is not enabled on the connection
			else if (!(c.isClientSupernodeConnection() && 
					   c.isQueryRoutingEnabled())) {
				continue;
			}
			
			// See if it is time for this connections QRP update
			// This call is safe since only this thread updates time
			if (time<c.getNextQRPForwardTime())
				continue;

			c.incrementNextQRPForwardTime(time);
				
			// Create a new query route table if we need to
			if (table == null) {
				table = createRouteTable();     //  Ignores busy leaves
                _lastQueryRouteTable = table;
			} 

			//..and send each piece.
			
			// Because we tend to send the same list of patches to lots of
			// Connections, we can reuse the list of RouteTableMessages
			// between those connections if their last sent
			// table is exactly the same.
			// This allows us to only reduce the amount of times we have
			// to call encode.
			
			//  (This if works for 'null' sent tables too)
			if( lastSent == c.getQueryRouteTableSent() ) {
			    // if we have not constructed the patches yet, then do so.
			    if( patches == null )
			        patches = table.encode(lastSent, true);
			}
			// If they aren't the same, we have to encode a new set of
			// patches for this connection.
			else {
			    lastSent = c.getQueryRouteTableSent();
			    patches = table.encode(lastSent, true);
            }
            
            // If sending QRP tables is turned off, don't send them.  
            if(!ConnectionSettings.SEND_QRP.getValue()) {
                return;
            }
            
            for(RouteTableMessage next : patches)
		        c.send(next);
    	    
            c.setQueryRouteTableSent(table);
		}
    }

    /**
     * Accessor for the most recently calculated <tt>QueryRouteTable</tt>
     * for this node.  If this node is an Ultrapeer, the table will include
     * all data for leaf nodes in addition to data for this node's files.
     *
     * @return the <tt>QueryRouteTable</tt> for this node
     */
    public QueryRouteTable getQueryRouteTable() {
        return _lastQueryRouteTable;
    }

    /**
     * Creates a query route table appropriate for forwarding to connection c.
     * This will not include information from c.
     *     @requires queryUpdateLock held
     */
    private QueryRouteTable createRouteTable() {
        QueryRouteTable ret = fileManager.getQRT();
        
        // Add leaves' files if we're an Ultrapeer.
        if(RouterService.isSupernode()) {
            addQueryRoutingEntriesForLeaves(ret);
        }
        return ret;
    }


	/**
	 * Adds all query routing tables of leaves to the query routing table for
	 * this node for propagation to other Ultrapeers at 1 hop.
	 * 
	 * Added "busy leaf" support to prevent a busy leaf from having its QRT
	 * 	table added to the Ultrapeer's last-hop QRT table.  This should reduce
	 *  BW costs for UPs with busy leaves.  
	 *
	 * @param qrt the <tt>QueryRouteTable</tt> to add to
	 */
	private void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {
		List<ManagedConnection> leaves = connectionManager.getInitializedClientConnections();
		
		for(int i=0; i<leaves.size(); i++) {
			ManagedConnection mc = leaves.get(i);
        	synchronized (mc.getQRPLock()) {
        	    //	Don't include busy leaves
        	    if( !mc.isBusyLeaf() ){
                	QueryRouteTable qrtr = mc.getQueryRouteTableReceived();
					if(qrtr != null) {
						qrt.addAll(qrtr);
					}
        	    }
			}
		}
	}

    
    /**
     * Adds the specified MessageListener for messages with this GUID.
     * You must manually unregister the listener.
     *
     * This works by replacing the necessary maps & lists, so that 
     * notifying doesn't have to hold any locks.
     */
    public void registerMessageListener(byte[] guid, MessageListener ml) {
        ml.registered(guid);
        synchronized(MESSAGE_LISTENER_LOCK) {
            Map<byte[], List<MessageListener>> listeners =
                new TreeMap<byte[], List<MessageListener>>(GUID.GUID_BYTE_COMPARATOR);
            listeners.putAll(_messageListeners);
            List<MessageListener> all = listeners.get(guid);
            if(all == null) {
                all = new ArrayList<MessageListener>(1);
                all.add(ml);
            } else {
                List<MessageListener> temp = new ArrayList<MessageListener>(all.size() + 1);
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
        synchronized(MESSAGE_LISTENER_LOCK) {
            List<MessageListener> all = _messageListeners.get(guid);
            if(all != null) {
                all = new ArrayList<MessageListener>(all);
                if(all.remove(ml)) {
                    removed = true;
                    Map<byte[], List<MessageListener>> listeners =
                        new TreeMap<byte[], List<MessageListener>>(GUID.GUID_BYTE_COMPARATOR);
                    listeners.putAll(_messageListeners);
                    if(all.isEmpty())
                        listeners.remove(guid);
                    else
                        listeners.put(guid, Collections.unmodifiableList(all));
                    _messageListeners = Collections.unmodifiableMap(listeners);
                }
            }
        }
        if(removed)
            ml.unregistered(guid);
    }


    /**
     * Replies to a head ping sent from the given ReplyHandler.
     */
    private void handleHeadPing(HeadPing ping, ReplyHandler handler) {
        if (DownloadSettings.DROP_HEADPINGS.getValue())
            return;
        
        GUID clientGUID = ping.getClientGuid();
        ReplyHandler pingee;
        
        if(clientGUID != null)
            pingee = getPushHandler(clientGUID.bytes());
        else
            pingee = forMeReplyHandler; // handle ourselves.
        
        //drop the ping if no entry for the given clientGUID
        if (pingee == null) 
           return; 
        
        //don't bother routing if this is intended for me. 
        // TODO:  Clean up ReplyHandler interface so we aren't
        //        afraid to use it like it's intended.
        //        That way, we can do pingee.handleHeadPing(ping)
        //        and not need this anti-OO instanceof check.
        if (pingee instanceof ForMeReplyHandler) {
            // If it's for me, reply directly to the person who sent it.
            HeadPong pong = headPongFactory.create(ping);
            handler.reply(pong); // 
        } else {
            // Otherwise, remember who sent it and forward it on.
            //remember where to send the pong to. 
            //the pong will have the same GUID as the ping. 
            // Note that this uses the messageGUID, not the clientGUID
            _headPongRouteTable.routeReply(ping.getGUID(), handler); 
            
            //and send off the routed ping 
            if ( !(handler instanceof Connection) ||
                    ((Connection)handler).supportsVMRouting())
                pingee.reply(ping);
            else
                pingee.reply(new HeadPing(ping)); 
        }
   } 
    
    
    /** 
     * Handles a pong received from the given handler.
     */ 
    private void handleHeadPong(HeadPong pong, ReplyHandler handler) { 
        ReplyHandler forwardTo =  _headPongRouteTable.getReplyHandler(pong.getGUID()); 

        // TODO: Clean up ReplyHandler interface so we're not afraid
        //       to use it correctly.
        //       Ideally, we'd do forwardTo.handleHeadPong(pong)
        //       instead of this instanceof check
         
        // if this pong is for me, process it as usual (not implemented yet)
        if (forwardTo != null && !(forwardTo instanceof ForMeReplyHandler)) { 
            forwardTo.reply(pong); 
            _headPongRouteTable.removeReplyHandler(forwardTo); 
        } 
    } 
    
    private void handleDHTContactsMessage(DHTContactsMessage msg, ReplyHandler handler) {
        dhtManager.handleDHTContactsMessage(msg);
    }
    
    /**
     * Forwards an inspection request to leaf connections that 
     * support it.
     */
    public void forwardInspectionRequestToLeaves(InspectionRequest ir) {
        if (!connectionManager.isSupernode())
            return;
        // only inspection requests with return address are forwarded.
        if (ir.getReturnAddress() == null)
            return;
        
        for (ManagedConnection mc : connectionManager.getInitializedClientConnections()) {
            if (mc.remoteHostSupportsInspections() >= ir.getVersion())
                mc.send(ir);
        }
    }
    
    private static class QueryResponseBundle {
        public final QueryRequest _query;
        public final Response[] _responses;
        
        public QueryResponseBundle(QueryRequest query, Response[] responses) {
            _query = query;
            _responses = responses;
        }
    }

    /** Expires the UDP-Reply cache. */
    private class UDPReplyCleaner implements Runnable {
        public void run() {
            messageDispatcher.get().dispatch(new Runnable() {
                public void run() {
                    _udpReplyHandlerCache.clear();
                }
            });
        }
        
    }

    /** Can be run to invalidate out-of-band ACKs that we are waiting for....
     */
    private class Expirer implements Runnable {
        public void run() {
            Set<GUID.TimedGUID> toRemove = new HashSet<GUID.TimedGUID>();
            synchronized (_outOfBandReplies) {
                long now = System.currentTimeMillis();
                for(GUID.TimedGUID currQB : _outOfBandReplies.keySet()) {
                    if ((currQB != null) && (currQB.shouldExpire(now)))
                        toRemove.add(currQB);
                }
                // done iterating through _outOfBandReplies, remove the 
                // keys now...
                for(GUID.TimedGUID next : toRemove)
                    _outOfBandReplies.remove(next);
            }
        }
    }


    /** This is run to clear out the registry of connect back attempts...
     *  Made package access for easy test access.
     */
    static class ConnectBackExpirer implements Runnable {
        public void run() {
            _tcpConnectBacks.clear();
            _udpConnectBacks.clear();
        }
    }

    static class HopsFlowManager implements Runnable {
        private final UploadManager uploadManager;
        private final ConnectionManager connectionManager;
        
        public HopsFlowManager(UploadManager uploadManager, ConnectionManager connectionManager) {
            this.uploadManager = uploadManager;
            this.connectionManager = connectionManager;
        }
        
        /* in case we don't want any queries any more */
        private static final byte BUSY_HOPS_FLOW = 0;

    	/* in case we want to reenable queries */
    	private static final byte FREE_HOPS_FLOW = 5;

        /* small optimization:
           send only HopsFlowVendorMessages if the busy state changed */
        private static boolean _oldBusyState = false;
           
        public void run() {
            // only leafs should use HopsFlow
            if (RouterService.isSupernode())
                return;
            // busy hosts don't want to receive any queries, if this node is not
            // busy, we need to reset the HopsFlow value
            boolean isBusy = !uploadManager.mayBeServiceable();
            
            // state changed? don't bother the ultrapeer with information
            // that it already knows. we need to inform new ultrapeers, though.
            final List<ManagedConnection> connections = connectionManager.getInitializedConnections();
            final HopsFlowVendorMessage hops = 
                new HopsFlowVendorMessage(isBusy ? BUSY_HOPS_FLOW :
                                          FREE_HOPS_FLOW);
            if (isBusy == _oldBusyState) {
                for (int i = 0; i < connections.size(); i++) {
                    ManagedConnection c = connections.get(i);
                    // Yes, we may tell a new ultrapeer twice, but
                    // without a buffer of some kind, we might forget
                    // some ultrapeers. The clean solution would be
                    // to remember the hops-flow value in the connection.
                    if (c != null 
                        && c.getConnectionTime() + 1.25 * HOPS_FLOW_INTERVAL 
                            > System.currentTimeMillis()
                        && c.isClientSupernodeConnection() )
                        c.send(hops);
                }
            } else { 
                _oldBusyState = isBusy;
                for (int i = 0; i < connections.size(); i++) {
                    ManagedConnection c = connections.get(i);
                    if (c != null && c.isClientSupernodeConnection())
                        c.send(hops);
                }
            }
        }
    }
    
    /**
     * Time after which an OOB session should be expired.
     * @return
     */
    public long getOOBExpireTime() {
    	return OOB_SESSION_EXPIRE_TIME;
    }
    
    /*
     * ===================================================
     *                   "REGULAR" HANDLER
     * ===================================================
     */
    private class PingRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_PING_REQUESTS.addMessage(msg);
            handlePingRequestPossibleDuplicate((PingRequest)msg, handler);
        }
    }
    
    private class PingReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_PING_REPLIES.addMessage(msg);
            handlePingReply((PingReply)msg, handler);
        }
    }
    
    private class QueryRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(msg);
            handleQueryRequestPossibleDuplicate(
                (QueryRequest)msg, (ManagedConnection)handler);
        }
    }
    
    private class QueryReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_QUERY_REPLIES.addMessage(msg);
            // if someone sent a TCP QueryReply with the MCAST header,
            // that's bad, so ignore it.
            QueryReply qmsg = (QueryReply)msg;
            handleQueryReply(qmsg, handler);
        }
    }
    
    private class PushRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, handler);
        }
    }
    
    private class ResetTableHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handleResetTableMessage((ResetTableMessage)msg,
                    (ManagedConnection)handler);
        }
    }
    
    private class PatchTableHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handlePatchTableMessage((PatchTableMessage)msg,
                    (ManagedConnection)handler); 
        }
    }
    
    private class TCPConnectBackHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_TCP_CONNECTBACK.addMessage(msg);
            handleTCPConnectBackRequest((TCPConnectBackVendorMessage) msg,
                    (ManagedConnection)handler);
        }
    }
    
    private class UDPConnectBackHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.TCP_UDP_CONNECTBACK.addMessage(msg);
            handleUDPConnectBackRequest((UDPConnectBackVendorMessage) msg,
                    (ManagedConnection)handler);
        }
    }
    
    private class TCPConnectBackRedirectHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleTCPConnectBackRedirect((TCPConnectBackRedirect) msg,
                    (ManagedConnection)handler);
        }
    }
    
    private class UDPConnectBackRedirectHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUDPConnectBackRedirect((UDPConnectBackRedirect) msg,
                    (ManagedConnection)handler);
        }
    }
    
    private class PushProxyRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handlePushProxyRequest((PushProxyRequest) msg, (ManagedConnection)handler);
        }
    }
    
    private class QueryStatusResponseHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleQueryStatus((QueryStatusResponse) msg, (ManagedConnection)handler);
        }
    }
    
    private class HeadPingHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            //TODO: add the statistics recording code
            handleHeadPing((HeadPing)msg, handler);
        }
    }
    
    private class SimppRequestVMHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleSimppRequest((SimppRequestVM)msg, handler);
        }
    }
    
    private class SimppVMHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleSimppVM((SimppVM)msg, handler);
        }
    }
    
    private class UpdateRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUpdateRequest((UpdateRequest)msg, handler);
        }
    }
    
    private class UpdateResponseHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUpdateResponse((UpdateResponse)msg, handler);
        }
    }
    
    private class HeadPongHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleHeadPong((HeadPong)msg, handler); 
        }
    }
    
    private class DHTContactsMessageHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleDHTContactsMessage((DHTContactsMessage)msg, handler); 
        }
    }
    
    public class VendorMessageHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ManagedConnection c = (ManagedConnection)handler;
            c.handleVendorMessage((VendorMessage)msg);
        }
    }
    
    /*
     * ===================================================
     *                     UDP HANDLER
     * ===================================================
     */
    private class UDPQueryRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            InetAddress address = addr.getAddress();
            int port = addr.getPort();
            
            // TODO: compare AddressSecurityToken with old generation params.  if it matches
            //send a new one generated with current params 
            if (hasValidQueryKey(address, port, (QueryRequest) msg)) {
                sendAcknowledgement(addr, msg.getGUID());
                // a TTL above zero may indicate a malicious client, as UDP
                // messages queries should not be sent with TTL above 1.
                //if(msg.getTTL() > 0) return;
                if (!handleUDPQueryRequestPossibleDuplicate(
                  (QueryRequest)msg, handler) ) {
                    ReceivedMessageStatHandler.UDP_DUPLICATE_QUERIES.addMessage(msg);
                }  
            }
            ReceivedMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(msg);
        }
    }
    
    private class UDPPingRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.UDP_PING_REQUESTS.addMessage(msg);
            handleUDPPingRequestPossibleDuplicate((PingRequest)msg, handler, addr);
        }
    }
    
    private class UDPPingReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
            handleUDPPingReply((PingReply)msg, handler, addr.getAddress(), addr.getPort());
        }
    }
    
    private class UDPPushRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, handler);
        }
    }
    
    private class UDPLimeACKVendorMessageHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            ReceivedMessageStatHandler.UDP_LIME_ACK.addMessage(msg);
            handleLimeACKMessage((LimeACKVendorMessage)msg, addr);
        }
    }
    
    private class UDPHeadPingHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            //TODO: add the statistics recording code
            handleHeadPing((HeadPing)msg, handler);
        }
    }
    
    private class UDPUpdateRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleUpdateRequest((UpdateRequest)msg, handler);
        }
    }
    
    private class UDPContentResponseHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            handleContentResponse((ContentResponse)msg, handler);
        }
    }
    
    /*
     * ===================================================
     *                  MULTICAST HANDLER
     * ===================================================
     */
    public class MulticastQueryRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr,
                ReplyHandler handler) {
            if (!handleUDPQueryRequestPossibleDuplicate((QueryRequest) msg, handler)) {
                ReceivedMessageStatHandler.MULTICAST_DUPLICATE_QUERIES
                        .addMessage(msg);
            }
            ReceivedMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(msg);
        }
    }
    
    public class MulticastQueryReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, 
                ReplyHandler handler) {
            ReceivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
            handleQueryReply((QueryReply)msg, handler);
        }
    }
    
    public class MulticastPingRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, 
                ReplyHandler handler) {
            ReceivedMessageStatHandler.MULTICAST_PING_REQUESTS.addMessage(msg);
            handleUDPPingRequestPossibleDuplicate((PingRequest)msg, handler, addr);
        }
    }
    
    public class MulticastPingReplyHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, 
                ReplyHandler handler) {
            ReceivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
            handleUDPPingReply((PingReply)msg, handler, addr.getAddress(), addr.getPort());
        }
    }
    
    public class MulticastPushRequestHandler implements MessageHandler {
        public void handleMessage(Message msg, InetSocketAddress addr, 
                ReplyHandler handler) {
            ReceivedMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, handler);
        }
    }
 
    /**
     * This class handles UDP query replies and forwards them to the 
     * {@link OOBHandler} if they are not replies to multicast or unicast
     * queries.
     */
    public class UDPQueryReplyHandler implements MessageHandler {

        private final OOBHandler oobHandler;
        
        public UDPQueryReplyHandler(OOBHandler oobHandler) {
            this.oobHandler = oobHandler;
        }
        
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            QueryReply reply = (QueryReply)msg;
            if (reply.isReplyToMulticastQuery()
                    || isHostUnicastQueried(new GUID(reply.getGUID()), handler)) {
                handleQueryReply(reply, handler);
            }
            else {
                oobHandler.handleMessage(msg, addr, handler);
            }
        }
        
    }
}
