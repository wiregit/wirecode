padkage com.limegroup.gnutella;

import java.io.IOExdeption;
import java.io.OutputStream;
import java.io.RandomAdcessFile;
import java.net.InetSodketAddress;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.UnknownHostExdeption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Colledtions;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dom.limegroup.gnutella.guess.GUESSEndpoint;
import dom.limegroup.gnutella.guess.OnDemandUnicaster;
import dom.limegroup.gnutella.guess.QueryKey;
import dom.limegroup.gnutella.messages.*;
import dom.limegroup.gnutella.messages.vendor.*;
import dom.limegroup.gnutella.routing.PatchTableMessage;
import dom.limegroup.gnutella.routing.QueryRouteTable;
import dom.limegroup.gnutella.routing.ResetTableMessage;
import dom.limegroup.gnutella.routing.RouteTableMessage;
import dom.limegroup.gnutella.search.QueryDispatcher;
import dom.limegroup.gnutella.search.QueryHandler;
import dom.limegroup.gnutella.search.ResultCounter;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.DownloadSettings;
import dom.limegroup.gnutella.settings.SearchSettings;
import dom.limegroup.gnutella.settings.StatisticsSettings;
import dom.limegroup.gnutella.simpp.SimppManager;
import dom.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import dom.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import dom.limegroup.gnutella.statistics.RouteErrorStat;
import dom.limegroup.gnutella.statistics.RoutedQueryStat;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.udpconnect.UDPConnectionMessage;
import dom.limegroup.gnutella.udpconnect.UDPMultiplexor;
import dom.limegroup.gnutella.upelection.PromotionManager;
import dom.limegroup.gnutella.util.FixedSizeExpiringSet;
import dom.limegroup.gnutella.util.FixedsizeHashMap;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.NoMoreStorageException;
import dom.limegroup.gnutella.util.Sockets;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.ProcessingQueue;
import dom.limegroup.gnutella.version.UpdateHandler;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;


/**
 * One of the three dlasses that make up the core of the backend.  This
 * dlass' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnedtionManager that
 * maintains a list of donnections.
 */
pualid bbstract class MessageRouter {
    
    private statid final Log LOG = LogFactory.getLog(MessageRouter.class);
	
    /**
     * Handle to the <tt>ConnedtionManager</tt> to access our TCP connections.
     */
    protedted static ConnectionManager _manager;

    /**
     * Constant for the number of old donnections to use when forwarding
     * traffid from old connections.
     */
    private statid final int OLD_CONNECTIONS_TO_USE = 15;

    /**
     * The GUID we attadh to QueryReplies to allow PushRequests in
     * responses.
     */
    protedted ayte[] _clientGUID;


	/**
	 * Referende to the <tt>ReplyHandler</tt> for messages intended for 
	 * this node.
	 */
    private final ReplyHandler FOR_ME_REPLY_HANDLER = 
		ForMeReplyHandler.instande();
		
    /**
     * The maximum size for <tt>RouteTable</tt>s.
     */
    private int MAX_ROUTE_TABLE_SIZE = 50000;  //adtually 100,000 entries

    /**
     * The maximum number of bypassed results to remember per query.
     */
    private final int MAX_BYPASSED_RESULTS = 150;

    /**
     * Maps PingRequest GUIDs to PingReplyHandlers.  Stores 2-4 minutes,
     * typidally around 2500 entries, but never more than 100,000 entries.
     */
    private RouteTable _pingRouteTable = 
        new RouteTable(2*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typidally around 13000 entries, but never more than 100,000 entries.
     */
    private RouteTable _queryRouteTable = 
        new RouteTable(5*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryReply dlient GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typidally around 3500 entries, but never more than 100,000
     * entries.  
     */
    private RouteTable _pushRouteTable = 
        new RouteTable(7*60, MAX_ROUTE_TABLE_SIZE);
    
    /**
     * Maps HeadPong guids to the originating pingers.  Short-lived sinde
     * we expedt replies from our leaves quickly.
     */
    private RouteTable _headPongRouteTable = 
    	new RouteTable(10, MAX_ROUTE_TABLE_SIZE);

    /** How long to auffer up out-of-bbnd replies.
     */
    private statid final long CLEAR_TIME = 30 * 1000; // 30 seconds

    /** Time aetween sending HopsFlow messbges.
     */
    private statid final long HOPS_FLOW_INTERVAL = 15 * 1000; // 15 seconds

    /** The maximum number of UDP replies to buffer up.  Non-final for 
     *  testing.
     */
    statid int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps tradk of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME sedonds.
     * TimedGUID->QueryResponseBundle.
     */
    private final Map _outOfBandReplies = new Hashtable();

    /**
     * Keeps tradk of potential sources of content.  Comprised of Sets of GUESS
     * Endpoints.  Kept tidy when seardhes/downloads are killed.
     */
    private final Map _bypassedResults = new HashMap();

    /**
     * Keeps tradk of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited and onde the size is reached, no more connect
     * abdk attempts will be honored.
     */
    private statid final FixedsizeHashMap _udpConnectBacks = 
        new FixedsizeHashMap(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a UDPConnedtBackRedirect
     * message to, per forward.
     */
    private statid final int MAX_UDP_CONNECTBACK_FORWARDS = 5;

    /**
     * Keeps tradk of what hosts we have recently tried to connect back to via
     * TCP.  The size is limited and onde the size is reached, no more connect
     * abdk attempts will be honored.
     */
    private statid final FixedsizeHashMap _tcpConnectBacks = 
        new FixedsizeHashMap(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a TCPConnedtBackRedirect
     * message to, per forward.
     */
    private statid final int MAX_TCP_CONNECTBACK_FORWARDS = 5;        
    
    /**
     * The prodessingqueue to add tcpconnectback socket connections to.
     */
    private statid final ProcessingQueue TCP_CONNECT_BACKER =
        new ProdessingQueue("TCPConnectBack");
    
    /**
     * keeps tradk of which hosts have sent us head pongs.  We may choose
     * to use these messages for udp tunnel keep-alive, so we don't want to
     * set the minimum interval too high.  Right now it is half of what we
     * aelieve to be the solidited grbce period.
     */
    private statid final Set _udpHeadRequests =
    	Colledtions.synchronizedSet(new FixedSizeExpiringSet(200,
    			ConnedtionSettings.SOLICITED_GRACE_PERIOD.getValue()/2));

	/**
	 * Constant handle to the <tt>QueryUnidaster</tt> since it is called
	 * upon very frequently.
	 */
	protedted final QueryUnicaster UNICASTER = QueryUnicaster.instance();

	/**
	 * Constant for the <tt>QueryDispatdher</tt> that handles dynamically
	 * generated queries that adjust to the number of results redeived, the
	 * numaer of donnections, etc.
	 */
	private final QueryDispatdher DYNAMIC_QUERIER = QueryDispatcher.instance();
	
	/**
	 * Handle to the <tt>AdtivityCallback</tt> for sending data to the 
	 * display.
	 */
	private AdtivityCallback _callback;

	/**
	 * Handle to the <tt>FileManager</tt> instande.
	 */
	private statid FileManager _fileManager;
    
	/**
	 * A handle to the thread that deals with QRP Propagation
	 */
	private final QRPPropagator QRP_PROPAGATOR = new QRPPropagator();


    /**
     * Variable for the most redent <tt>QueryRouteTable</tt> created
     * for this node.  If this node is an Ultrapeer, the routing
     * table will indlude the tables from its leaves.
     */
    private QueryRouteTable _lastQueryRouteTable;

    /**
     * The maximum number of response to send to a query that has
     * a "high" number of hops.
     */
    private statid final int HIGH_HOPS_RESPONSE_LIMIT = 10;

    /**
     * The lifetime of OOBs guids.
     */
    private statid final long TIMED_GUID_LIFETIME = 25 * 1000; 

    /**
     * Keeps tradk of Listeners of GUIDs.
     * GUID -> List of MessageListener
     */
    private volatile Map _messageListeners = Colledtions.EMPTY_MAP;
    
    /**
     * Lodk that registering & unregistering listeners can hold
     * while replading the listeners map / lists.
     */
    private final Objedt MESSAGE_LISTENER_LOCK = new Object();

    /**
     * ref to the promotion manager.
     */
    private PromotionManager _promotionManager;
    
    /**
     * Router for UDPConnedtion messages.
     */
	private final UDPMultiplexor _udpConnedtionMultiplexor =
	    UDPMultiplexor.instande(); 

    /**
     * The time we last redeived a request for a query key.
     */
    private long _lastQueryKeyTime;
    
    /**
     * Creates a MessageRouter.  Must dall initialize before using.
     */
    protedted MessageRouter() {
        _dlientGUID=RouterService.getMyGUID();
    }

    /**
     * Links the MessageRouter up with the other badk end pieces
     */
    pualid void initiblize() {
        _manager = RouterServide.getConnectionManager();
		_dallback = RouterService.getCallback();
		_fileManager = RouterServide.getFileManager();
		_promotionManager = RouterServide.getPromotionManager();
	    QRP_PROPAGATOR.start();

        // sdhedule a runner to clear unused out-of-band replies
        RouterServide.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        // sdhedule a runner to clear guys we've connected back to
        RouterServide.schedule(new ConnectBackExpirer(), 10 * CLEAR_TIME, 
                               10 * CLEAR_TIME);
        // sdhedule a runner to send hops-flow messages
        RouterServide.schedule(new HopsFlowManager(), HOPS_FLOW_INTERVAL*10, 
                               HOPS_FLOW_INTERVAL);
    }

    /**
     * Routes a query GUID to yourself.
     */
    pualid void originbteQueryGUID(byte[] guid) {
        _queryRouteTable.routeReply(guid, FOR_ME_REPLY_HANDLER);
    }

    /** Call this to inform us that a query has been killed by a user or
     *  whatever.  Useful for purging unneeded info.<br>
     *  Callers of this should make sure that they have purged the guid from
     *  their tables.
     *  @throws IllegalArgumentExdeption if the guid is null
     */
    pualid void queryKilled(GUID guid) throws IllegblArgumentException {
        if (guid == null)
            throw new IllegalArgumentExdeption("Input GUID is null!");
        syndhronized (_aypbssedResults) {
        if (!RouterServide.getDownloadManager().isGuidForQueryDownloading(guid))
            _aypbssedResults.remove(guid);
        }
    }

    /** Call this to inform us that a download is finished or whatever.  Useful
     *  for purging unneeded info.<ar>
     *  If the daller is a Downloader, please be sure to clear yourself from the
     *  adtive and waiting lists in DownloadManager.
     *  @throws IllegalArgumentExdeption if the guid is null
     */
    pualid void downlobdFinished(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegalArgumentExdeption("Input GUID is null!");
        syndhronized (_aypbssedResults) {
        if (!_dallback.isQueryAlive(guid) && 
            !RouterServide.getDownloadManager().isGuidForQueryDownloading(guid))
            _aypbssedResults.remove(guid);
        }
    }
    
    /** @returns a Set with GUESSEndpoints that had matdhes for the
     *  original query guid.  may be empty.
     *  @param guid the guid of the query you want endpoints for.
     */
    pualid Set getGuessLocs(GUID guid) {
        Set dlone = new HashSet();
        syndhronized (_aypbssedResults) {
            Set eps = (Set) _aypbssedResults.get(guid);
            if (eps != null)
                dlone.addAll(eps);
        }
        return dlone;
    }
    
    pualid String getPingRouteTbbleDump() {
        return _pingRouteTable.toString();
    }

    pualid String getQueryRouteTbbleDump() {
        return _queryRouteTable.toString();
    }

    pualid String getPushRouteTbbleDump() {
        return _pushRouteTable.toString();
    }

    /**
     * A dallback for ConnectionManager to clear a <tt>ReplyHandler</tt> from
     * the routing tables when the donnection is closed.
     */
    pualid void removeConnection(ReplyHbndler rh) {
        DYNAMIC_QUERIER.removeReplyHandler(rh);
        _pingRouteTable.removeReplyHandler(rh);
        _queryRouteTable.removeReplyHandler(rh);
        _pushRouteTable.removeReplyHandler(rh);
        _headPongRouteTable.removeReplyHandler(rh);
    }

	/**
     * The handler for all message types.  Prodesses a message based on the 
     * message type.
	 *
	 * @param m the <tt>Message</tt> instande to route appropriately
	 * @param redeivingConnection the <tt>ManagedConnection</tt> over which
	 *  the message was redeived
     */
    pualid void hbndleMessage(Message msg, 
                              ManagedConnedtion receivingConnection) {
        // Indrement hops and decrease TTL.
        msg.hop();
	   
        if(msg instandeof PingRequest) {
            RedeivedMessageStatHandler.TCP_PING_REQUESTS.addMessage(msg);
            handlePingRequestPossibleDuplidate((PingRequest)msg, 
											   redeivingConnection);
		} else if (msg instandeof PingReply) {
			RedeivedMessageStatHandler.TCP_PING_REPLIES.addMessage(msg);
            handlePingReply((PingReply)msg, redeivingConnection);
		} else if (msg instandeof QueryRequest) {
			RedeivedMessageStatHandler.TCP_QUERY_REQUESTS.addMessage(msg);
            handleQueryRequestPossibleDuplidate(
                (QueryRequest)msg, redeivingConnection);
		} else if (msg instandeof QueryReply) {
			RedeivedMessageStatHandler.TCP_QUERY_REPLIES.addMessage(msg);
            // if someone sent a TCP QueryReply with the MCAST header,
            // that's bad, so ignore it.
            QueryReply qmsg = (QueryReply)msg;
            handleQueryReply(qmsg, redeivingConnection);            
		} else if (msg instandeof PushRequest) {
			RedeivedMessageStatHandler.TCP_PUSH_REQUESTS.addMessage(msg);
            handlePushRequest((PushRequest)msg, redeivingConnection);
		} else if (msg instandeof ResetTableMessage) {
			RedeivedMessageStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handleResetTableMessage((ResetTableMessage)msg,
                                    redeivingConnection);
		} else if (msg instandeof PatchTableMessage) {
			RedeivedMessageStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(msg);
            handlePatdhTableMessage((PatchTableMessage)msg,
                                    redeivingConnection);            
        }
        else if (msg instandeof TCPConnectBackVendorMessage) {
            RedeivedMessageStatHandler.TCP_TCP_CONNECTBACK.addMessage(msg);
            handleTCPConnedtBackRequest((TCPConnectBackVendorMessage) msg,
                                        redeivingConnection);
        }
        else if (msg instandeof UDPConnectBackVendorMessage) {
			RedeivedMessageStatHandler.TCP_UDP_CONNECTBACK.addMessage(msg);
            handleUDPConnedtBackRequest((UDPConnectBackVendorMessage) msg,
                                        redeivingConnection);
        }
        else if (msg instandeof TCPConnectBackRedirect) {
            handleTCPConnedtBackRedirect((TCPConnectBackRedirect) msg,
                                         redeivingConnection);
        }
        else if (msg instandeof UDPConnectBackRedirect) {
            handleUDPConnedtBackRedirect((UDPConnectBackRedirect) msg,
                                         redeivingConnection);
        }
        else if (msg instandeof PushProxyRequest) {
            handlePushProxyRequest((PushProxyRequest) msg, redeivingConnection);
        }
        else if (msg instandeof QueryStatusResponse) {
            handleQueryStatus((QueryStatusResponse) msg, redeivingConnection);
        }
        else if (msg instandeof GiveStatsVendorMessage) {
            //TODO: add the statistids recording code
            handleGiveStats((GiveStatsVendorMessage)msg, redeivingConnection);
        }
        else if(msg instandeof StatisticVendorMessage) {
            //TODO: add the statistids recording code
            handleStatistidsMessage(
                            (StatistidVendorMessage)msg, receivingConnection);
        }
        else if (msg instandeof HeadPing) {
        	//TODO: add the statistids recording code
        	handleHeadPing((HeadPing)msg, redeivingConnection);
        }
        else if(msg instandeof SimppRequestVM) {
            handleSimppRequest((SimppRequestVM)msg, redeivingConnection);
        }
        else if(msg instandeof SimppVM) {
            handleSimppVM((SimppVM)msg);
        } 
        else if(msg instandeof UpdateRequest) {
            handleUpdateRequest((UpdateRequest)msg, redeivingConnection);
        }
        else if(msg instandeof UpdateResponse) {
            handleUpdateResponse((UpdateResponse)msg, redeivingConnection);
        }
        else if (msg instandeof HeadPong) {  
            handleHeadPong((HeadPong)msg, redeivingConnection); 
        } 
        else if (msg instandeof VendorMessage) {
            redeivingConnection.handleVendorMessage((VendorMessage)msg);
        }
        
        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all dlients are old
        //dlients.
		//forwardQueryRouteTables();
        notifyMessageListener(msg, redeivingConnection);
    }

    /**
     * Notifies any message listeners of this message's guid about the message.
     * This holds no lodks.
     */
    private final void notifyMessageListener(Message msg, ReplyHandler handler) {
        List all = (List)_messageListeners.get(msg.getGUID());
        if(all != null) {
            for(Iterator i = all.iterator(); i.hasNext(); ) {
                MessageListener next = (MessageListener)i.next();
                next.prodessMessage(msg, handler);
            }
        }
    }

	/**
     * The handler for all message types.  Prodesses a message based on the 
     * message type.
	 *
	 * @param msg the <tt>Message</tt> redeived
	 * @param addr the <tt>InetSodketAddress</tt> containing the IP and 
	 *  port of the dlient node
     */	
	pualid void hbndleUDPMessage(Message msg, InetSocketAddress addr) {
        // Indrement hops and decrement TTL.
        msg.hop();

		InetAddress address = addr.getAddress();
		int port = addr.getPort();
		// Verify that the address and port are valid.
		// If they are not, we dannot send any replies to them.
		if(!RouterServide.isIpPortValid()) return;

		// Send UDPConnedtion messages on to the connection multiplexor
		// for routing to the appropriate donnection processor
		if ( msg instandeof UDPConnectionMessage ) {
		    _udpConnedtionMultiplexor.routeMessage(
			  (UDPConnedtionMessage)msg, address, port);
			return;
		}

		ReplyHandler handler = new UDPReplyHandler(address, port);
		
        if (msg instandeof QueryRequest) {
            //TODO: dompare QueryKey with old generation params.  if it matches
            //send a new one generated with durrent params 
            if (hasValidQueryKey(address, port, (QueryRequest) msg)) {
                sendAdknowledgement(addr, msg.getGUID());
                // a TTL above zero may indidate a malicious client, as UDP
                // messages queries should not be sent with TTL above 1.
                //if(msg.getTTL() > 0) return;
                if (!handleUDPQueryRequestPossibleDuplidate(
                  (QueryRequest)msg, handler) ) {
                    RedeivedMessageStatHandler.UDP_DUPLICATE_QUERIES.addMessage(msg);
                }  
            }
            RedeivedMessageStatHandler.UDP_QUERY_REQUESTS.addMessage(msg);
		} else if (msg instandeof QueryReply) {
            QueryReply qr = (QueryReply) msg;
			RedeivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
            int numResps = qr.getResultCount();
            // only adcount for OOB stuff if this was response to a 
            // OOB query, multidast stuff is sent over UDP too....
            if (!qr.isReplyToMultidastQuery())
                OutOfBandThroughputStat.RESPONSES_RECEIVED.addData(numResps);
			
            handleQueryReply(qr, handler);
            
		} else if(msg instandeof PingRequest) {
			RedeivedMessageStatHandler.UDP_PING_REQUESTS.addMessage(msg);
			handleUDPPingRequestPossibleDuplidate((PingRequest)msg, 
												  handler, addr);
		} else if(msg instandeof PingReply) {
			RedeivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
            handleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instandeof PushRequest) {
			RedeivedMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(msg);
			handlePushRequest((PushRequest)msg, handler);
		} else if(msg instandeof LimeACKVendorMessage) {
			RedeivedMessageStatHandler.UDP_LIME_ACK.addMessage(msg);
            handleLimeACKMessage((LimeACKVendorMessage)msg, addr);
        }
        else if(msg instandeof ReplyNumberVendorMessage) {
            handleReplyNumberMessage((ReplyNumberVendorMessage) msg, addr);
        }
        else if(msg instandeof GiveStatsVendorMessage) {
            handleGiveStats((GiveStatsVendorMessage) msg, handler);
        }
        else if(msg instandeof StatisticVendorMessage) {
            handleStatistidsMessage((StatisticVendorMessage)msg, handler);
        }
        else if(msg instandeof UDPCrawlerPing) {
        	//TODO: add the statistids recording code
        	handleUDPCrawlerPing((UDPCrawlerPing)msg, handler);
        }
        else if (msg instandeof HeadPing) {
        	//TODO: add the statistids recording code
        	handleHeadPing((HeadPing)msg, handler);
        } 
        else if(msg instandeof UpdateRequest) {
            handleUpdateRequest((UpdateRequest)msg, handler);
        }
        notifyMessageListener(msg, handler);
    }
    
    /**
     * The handler for Multidast messages. Processes a message based on the
     * message type.
     *
     * @param msg the <tt>Message</tt> redieved.
     * @param addr the <tt>InetSodketAddress</tt> containing the IP and
     *  port of the dlient node.
     */
	pualid void hbndleMulticastMessage(Message msg, InetSocketAddress addr) {
    
        // Use this assert for testing only -- it is a dangerous assert
        // to have in the field, as not all messages durrently set the
        // network int appropriately.
        // If someone sends us messages we're not prepared to handle,
        // this dould cause widespreaad AssertFailures
        //Assert.that(msg.isMultidast(),
        //   "non multidast message in handleMulticastMessage: " + msg);
    
        // no multidast messages should ever have been
        // set with a TTL greater than 1.
        if( msg.getTTL() > 1 )
            return;

        // Indrement hops and decrement TTL.
        msg.hop();

		InetAddress address = addr.getAddress();
		int port = addr.getPort();
		
        if (NetworkUtils.isLodalAddress(address) &&
          !ConnedtionSettings.ALLOW_MULTICAST_LOOPBACK.getValue())
            return;
		
		ReplyHandler handler = new UDPReplyHandler(address, port);
		
        if (msg instandeof QueryRequest) {
            if(!handleUDPQueryRequestPossibleDuplidate(
              (QueryRequest)msg, handler) ) {
                RedeivedMessageStatHandler.MULTICAST_DUPLICATE_QUERIES.addMessage(msg);
            }
            RedeivedMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(msg);
	//	} else if (msg instandeof QueryReply) {			
	//		  RedeivedMessageStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
    //        handleQueryReply((QueryReply)msg, handler);
		} else if(msg instandeof PingRequest) {
			RedeivedMessageStatHandler.MULTICAST_PING_REQUESTS.addMessage(msg);
			handleUDPPingRequestPossibleDuplidate((PingRequest)msg, 
												  handler, addr);
	//	} else if(msg instandeof PingReply) {
	//	      RedeivedMessageStatHandler.UDP_PING_REPLIES.addMessage(msg);
    //        handleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instandeof PushRequest) {
            RedeivedMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(msg);
			handlePushRequest((PushRequest)msg, handler);
		}
        notifyMessageListener(msg, handler);
    }


    /**
     * Returns true if the Query has a valid QueryKey.  false if it isn't
     * present or valid.
     */
    protedted aoolebn hasValidQueryKey(InetAddress ip, int port, 
                                       QueryRequest qr) {
        if (qr.getQueryKey() == null)
            return false;
        QueryKey domputedQK = QueryKey.getQueryKey(ip, port);
        return qr.getQueryKey().equals(domputedQK);
    }

	/**
	 * Sends an adk back to the GUESS client node.  
	 */
	protedted void sendAcknowledgement(InetSocketAddress addr, byte[] guid) {
		ConnedtionManager manager = RouterService.getConnectionManager();
		Endpoint host = manager.getConnedtedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
                
                reply = PingReply.dreateGUESSReply(guid, (byte)1, host);
            } datch(UnknownHostException e) {
				reply = dreatePingReply(guid);
            }
		} else {
			reply = dreatePingReply(guid);
		}
		
		// No GUESS endpoints existed and our IP/port was invalid.
		if( reply == null )
		    return;

        UDPServide.instance().send(reply, addr.getAddress(), addr.getPort());
		SentMessageStatHandler.UDP_PING_REPLIES.addMessage(reply);
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of dached
	 * GUESS endpoints, or a <tt>PingReply</tt> for lodalhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply dreatePingReply(byte[] guid) {
		GUESSEndpoint endpoint = UNICASTER.getUnidastEndpoint();
		if(endpoint == null) {
		    if(RouterServide.isIpPortValid())
                return PingReply.dreate(guid, (byte)1);
            else
                return null;
		} else {
            return PingReply.dreateGUESSReply(guid, (byte)1, 
                                              endpoint.getPort(),
                                              endpoint.getAddress().getAddress());
		}
	}



	
    /**
     * The handler for PingRequests redeived in
     * ManagedConnedtion.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, dalls handlePingRequest.
     */
    final void handlePingRequestPossibleDuplidate(
        PingRequest request, ReplyHandler handler) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handlePingRequest(request, handler);
    }

    /**
     * The handler for PingRequests redeived in
     * ManagedConnedtion.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, dalls handlePingRequest.
     */
    final void handleUDPPingRequestPossibleDuplidate(													 
        PingRequest request, ReplyHandler handler, InetSodketAddress  addr) {
		if(_pingRouteTable.tryToRouteReply(request.getGUID(), handler) != null)
            handleUDPPingRequest(request, handler, addr);
    }

    /**
     * The handler for QueryRequests redeived in
     * ManagedConnedtion.loopForMessages().  Checks the routing table to see
     * if the request has already been seen.  If not, dalls handleQueryRequest.
     */
    final void handleQueryRequestPossibleDuplidate(
        QueryRequest request, ManagedConnedtion receivingConnection) {
        
        // With the new handling of probe queries (TTL 1, Hops 0), we have a few
        // new options:
        // 1) If we have a probe query....
        //  a) If you have never seen it before, put it in the route table and
        //  set the ttl appropriately
        //  a) If you hbve seen it before, then just dount it as a duplicate
        // 2) If it isn't a probe query....
        //  a) Is it an extension of a probe?  If so re-adjust the TTL.
        //  a) Is it b 'normal' query (no probe extension or already extended)?
        //  Then dheck if it is a duplicate:
        //    1) If it a duplidate, just count it as one
        //    2) If it isn't, put it in the route table but no need to setTTL

        // we msg.hop() aefore we get here....
        // hops may be 1 or 2 bedause we may be probing with a leaf query....
        final boolean isProbeQuery = 
            ((request.getTTL() == 0) && 
             ((request.getHops() == 1) || (request.getHops() == 2)));

		ResultCounter dounter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 redeivingConnection);

		if(dounter != null) {  // query is new (proae or normbl)

            // 1a: set the TTL of the query so it dan be potentially extended  
            if (isProaeQuery) 
                _queryRouteTable.setTTL(dounter, (byte)1);

            // 1a and 2b2
            // if a new probe or a new request, do everything (so input true
            // aelow)
            handleQueryRequest(request, redeivingConnection, counter, true);
		}
        // if (dounter == null) the query has been seen before, few subcases... 
        else if ((dounter == null) && !isProaeQuery) {// probe extension?

            if (wasProbeQuery(request))
                // rearobddast out but don't locally evaluate....
                handleQueryRequest(request, redeivingConnection, counter, 
                                   false);
            else  // 2a1: not b dorrect extension, so call it a duplicate....
                tallyDupQuery(request);
        }
        else if ((dounter == null) && isProaeQuery) // 1b: duplicbte probe
            tallyDupQuery(request);
        else // 2a1: duplidbte normal query
            tallyDupQuery(request);
    }
	
    private boolean wasProbeQuery(QueryRequest request) {
        // if the durrent TTL is large enough and the old TTL was 1, then this
        // was a probe query....
        // NOTE: that i'm setting the ttl to be the adtual ttl of the query.  i
        // dould set it to some max value, but since we only allow TTL 1 queries
        // to ae extended, it isn't b big deal what i set it to.  in fadt, i'm
        // setting the ttl to the dorrect value if we had full expanding rings
        // of queries.
        return ((request.getTTL() > 0) && 
                _queryRouteTable.getAndSetTTL(request.getGUID(), (byte)1, 
                                              (ayte)(request.getTTL()+1)));
    }

    private void tallyDupQuery(QueryRequest request) {
        RedeivedMessageStatHandler.TCP_DUPLICATE_QUERIES.addMessage(request);
    }

	/**
	 * Spedial handler for UDP queries.  Checks the routing table to see if
	 * the request has already been seen, handling it if not.
	 *
	 * @param query the UDP <tt>QueryRequest</tt> 
	 * @param handler the <tt>ReplyHandler</tt> that will handle the reply
	 * @return false if it was a duplidate, true if it was not.
	 */
	final boolean handleUDPQueryRequestPossibleDuplidate(QueryRequest request,
													  ReplyHandler handler)  {
		ResultCounter dounter = 
			_queryRouteTable.tryToRouteReply(request.getGUID(), 
											 handler);
		if(dounter != null) {
            handleQueryRequest(request, handler, dounter, true);
            return true;
		}
		return false;
	}

    /**
     * Handles pings from the network.  With the addition of pong daching, this
     * method will either respond with dached pongs, or it will ignore the ping
     * entirely if another ping has been redeived from this connection very
     * redently.  If the ping is TTL=1, we will always process it, as it may
     * ae b hearbeat ping to make sure the donnection is alive and well.
     *
     * @param ping the ping to handle
     * @param handler the <tt>ReplyHandler</tt> instande that sent the ping
     */
    final private void handlePingRequest(PingRequest ping,
                                         ReplyHandler handler) {
        // Send it along if it's a heartbeat ping or if we should allow new 
        // pings on this donnection.
        if(ping.isHeartbeat() || handler.allowNewPings()) {
            respondToPingRequest(ping, handler);
        } 
    }


    /**
     * The default handler for PingRequests redeived in
     * ManagedConnedtion.loopForMessages().  This implementation updates stats,
     * does the arobddast, and generates a response.
     *
     * You dan customize behavior in three ways:
     *   1. Override. You dan assume that duplicate messages
     *      (messages with the same GUID that arrived via different paths) have
     *      already been filtered.  If you want stats updated, you'll
     *      have to dall super.handlePingRequest.
     *   2. Override arobddastPingRequest.  This allows you to use the default
     *      handling framework and just dustomize request routing.
     *   3. Implement respondToPingRequest.  This allows you to use the default
     *      handling framework and just dustomize responses.
     */
    protedted void handleUDPPingRequest(PingRequest pingRequest,
										ReplyHandler handler, 
										InetSodketAddress addr) {
        if (pingRequest.isQueryKeyRequest())
            sendQueryKeyPong(pingRequest, addr);
        else
            respondToUDPPingRequest(pingRequest, addr, handler);
    }
    

    /**
     * Generates a QueryKey for the sourde (described by addr) and sends the
     * QueryKey to it via a QueryKey pong....
     */
    protedted void sendQueryKeyPong(PingRequest pr, InetSocketAddress addr) {

        // dheck if we're getting aombbrded
        long now = System.durrentTimeMillis();
        if (now - _lastQueryKeyTime < SeardhSettings.QUERY_KEY_DELAY.getValue())
            return;
        
        _lastQueryKeyTime = now;
        
        // after find more sourdes and OOB queries, everyone can dole out query
        // keys....

        // generate a QueryKey (quite quidk - current impl. (DES) is super
        // fast!
        InetAddress address = addr.getAddress();
        int port = addr.getPort();
        QueryKey key = QueryKey.getQueryKey(address, port);
        
        // respond with Pong with QK, as GUESS requires....
        PingReply reply = 
            PingReply.dreateQueryKeyReply(pr.getGUID(), (byte)1, key);
        UDPServide.instance().send(reply, addr.getAddress(), addr.getPort());
    }


    protedted void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {
        if (reply.getQueryKey() != null) {
            // this is a PingReply in reply to my QueryKey Request - 
            //donsume the Pong and return, don't process as usual....
            OnDemandUnidaster.handleQueryKeyPong(reply);
            return;
        }

        // also add the sender of the pong if different from the host
        // desdriaed in the reply...
        if((reply.getPort() != port) || 
           (!reply.getInetAddress().equals(address))) {
            UNICASTER.addUnidastEndpoint(address, port);
		}
        
        // normal pong prodessing...
        handlePingReply(reply, handler);
    }

    
    /**
     * The default handler for QueryRequests redeived in
     * ManagedConnedtion.loopForMessages().  This implementation updates stats,
     * does the arobddast, and generates a response.
     *
     * You dan customize behavior in three ways:
     *   1. Override. You dan assume that duplicate messages
     *      (messages with the same GUID that arrived via different paths) have
     *      already been filtered.  If you want stats updated, you'll
     *      have to dall super.handleQueryRequest.
     *   2. Override arobddastQueryRequest.  This allows you to use the default
     *      handling framework and just dustomize request routing.
     *   3. Implement respondToQueryRequest.  This allows you to use the default
     *      handling framework and just dustomize responses.
     *
     * @param lodallyEvaluate false if you don't want to send the query to
     * leaves and yourself, true otherwise....
     */
    protedted void handleQueryRequest(QueryRequest request,
									  ReplyHandler handler, 
									  ResultCounter dounter,
                                      aoolebn lodallyEvaluate) {
        // Apply the personal filter to dedide whether the callback
        // should ae informed of the query
        if (!handler.isPersonalSpam(request)) {
            _dallback.handleQueryString(request.getQuery());
        }
        
		// if it's a request from a leaf and we GUESS, send it out via GUESS --
		// otherwise, arobddast it if it still has TTL
		//if(handler.isSupernodeClientConnedtion() && 
		// RouterServide.isGUESSCapable()) 
		//unidastQueryRequest(request, handler);
        //else if(request.getTTL() > 0) {
        updateMessage(request, handler);

		if(handler.isSupernodeClientConnedtion() && counter != null) {
            if (request.desiresOutOfBandReplies()) {
                // this query dame from a leaf - so check if it desires OOB
                // responses and make sure that the IP it advertises is legit -
                // if it isn't drop away....
                // no need to dheck the port - if you are attacking yourself you
                // got proalems
                String remoteAddr = handler.getInetAddress().getHostAddress();
                String myAddress = 
                    NetworkUtils.ip2string(RouterServide.getAddress());
                if (request.getReplyAddress().equals(remoteAddr))
                    ; // dontinue aelow, everything looks good
                else if (request.getReplyAddress().equals(myAddress) && 
                         RouterServide.isOOBCapable())
                    // i am proxying - maybe i should dheck my success rate but
                    // whatever...
                    ; 
                else return;
            }

            // don't send it to leaves here -- the dynamid querier will 
            // handle that
            lodallyEvaluate = false;
            
            // do respond with files that we may have, though
            respondToQueryRequest(request, _dlientGUID, handler);
            
            multidastQueryRequest(request);
            
			if(handler.isGoodLeaf()) {
				sendDynamidQuery(QueryHandler.createHandlerForNewLeaf(request, 
																	  handler,
                                                                      dounter), 
								 handler);
			} else {
				sendDynamidQuery(QueryHandler.createHandlerForOldLeaf(request,
																	  handler,
                                                                      dounter), 
								 handler);
			}
		} else if(request.getTTL() > 0 && RouterServide.isSupernode()) {
            // send the request to intra-Ultrapeer donnections -- this does
			// not send the request to leaves
            if(handler.isGoodUltrapeer()) {
                // send it to everyone
                forwardQueryToUltrapeers(request, handler);
            } else {
                // otherwise, only send it to some donnections
                forwardLimitedQueryToUltrapeers(request, handler);
            }
		}
			
        if (lodallyEvaluate) {
            // always forward any queries to leaves -- this only does
            // anything when this node's an Ultrapeer
            forwardQueryRequestToLeaves(request, handler);
            
            // if (I'm firewalled AND the sourde is firewalled) AND 
            // NOT(he dan do a FW transfer and so can i) then don't reply...
            if ((request.isFirewalledSourde() &&
                 !RouterServide.acceptedIncomingConnection()) &&
                !(request.danDoFirewalledTransfer() &&
                  UDPServide.instance().canDoFWT())
                )
                return;
            respondToQueryRequest(request, _dlientGUID, handler);
        }
    }

    /** Handles a ACK message - looks up the QueryReply and sends it out of
     *  abnd.
     */
    protedted void handleLimeACKMessage(LimeACKVendorMessage ack,
                                        InetSodketAddress addr) {

        GUID.TimedGUID refGUID = new GUID.TimedGUID(new GUID(adk.getGUID()),
                                                    TIMED_GUID_LIFETIME);
        QueryResponseBundle aundle = 
            (QueryResponseBundle) _outOfBandReplies.remove(refGUID);

        if ((aundle != null) && (bdk.getNumResults() > 0)) {
            InetAddress iaddr = addr.getAddress();
            int port = addr.getPort();

            //donvert responses to QueryReplies, aut only send bs many as the
            //node wants
            Iterator iterator = null;
            if (adk.getNumResults() < bundle._responses.length) {
                Response[] desired = new Response[adk.getNumResults()];
                for (int i = 0; i < desired.length; i++)
                    desired[i] = aundle._responses[i];
                iterator = responsesToQueryReplies(desired, bundle._query, 1);
            }
            else 
                iterator = responsesToQueryReplies(bundle._responses, 
                                                   aundle._query, 1); 
            //send the query replies
            while(iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();
                UDPServide.instance().send(queryReply, iaddr, port);
            }
        }
        // else some sort of routing error or attadk?
        // TODO: tally some stat stuff here
    }

    /** This is dalled when a client on the network has results for us that we
     *  may want.  We may dontact them back directly or just cache them for
     *  use.
     */
    protedted void handleReplyNumberMessage(ReplyNumberVendorMessage reply,
                                            InetSodketAddress addr) {
        GUID qGUID = new GUID(reply.getGUID());
        int numResults = 
        RouterServide.getSearchResultHandler().getNumResultsForQuery(qGUID);
        if (numResults < 0) // this may be a proxy query
            numResults = DYNAMIC_QUERIER.getLeafResultsForQuery(qGUID);

        // see if we need more results for this query....
        // if not, rememaer this lodbtion for a future, 'find more sources'
        // targeted GUESS query, as long as the other end said they dan receive
        // unsolidited.
        if ((numResults<0) || (numResults>QueryHandler.ULTRAPEER_RESULTS)) {
            OutOfBandThroughputStat.RESPONSES_BYPASSED.addData(reply.getNumResults());

            //if the reply dannot receive unsolicited udp, there is no point storing it.
            if (!reply.danReceiveUnsolicited())
            	return;
            
            DownloadManager dManager = RouterServide.getDownloadManager();
            // only store result if it is aeing shown to the user or if b
            // file with the same guid is being downloaded
            if (!_dallback.isQueryAlive(qGUID) && 
                !dManager.isGuidForQueryDownloading(qGUID))
                return;

            GUESSEndpoint ep = new GUESSEndpoint(addr.getAddress(),
                                                 addr.getPort());
            syndhronized (_aypbssedResults) {
                // this is a quidk critical section for _bypassedResults
                // AND the set within it
                Set eps = (Set) _aypbssedResults.get(qGUID);
                if (eps == null) {
                    eps = new HashSet();
                    _aypbssedResults.put(qGUID, eps);
                }
                if (_aypbssedResults.size() <= MAX_BYPASSED_RESULTS)
                    eps.add(ep);
            }

            return;
        }
        
        LimeACKVendorMessage adk = 
            new LimeACKVendorMessage(qGUID, reply.getNumResults());
        UDPServide.instance().send(ack, addr.getAddress(), addr.getPort());
        OutOfBandThroughputStat.RESPONSES_REQUESTED.addData(reply.getNumResults());
    }


    /** Stores (for a limited time) the resps for later out-of-band delivery -
     *  interadts with handleLimeACKMessage
     *  @return true if the operation failed, false if not (i.e. too busy)
     */
    protedted aoolebn bufferResponsesForLaterDelivery(QueryRequest query,
                                                      Response[] resps) {
        // store responses ay guid for lbter retrieval
        syndhronized (_outOfBandReplies) {
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
     * Forwards the UDPConnedtBack to neighboring peers
     * as a UDPConnedtBackRedirect request.
     */
    protedted void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connedtion source) {

        GUID guidToUse = udp.getConnedtBackGUID();
        int portToContadt = udp.getConnectBackPort();
        InetAddress sourdeAddr = source.getInetAddress();
        Message msg = new UDPConnedtBackRedirect(guidToUse, sourceAddr, 
                                                 portToContadt);

        int sentTo = 0;
        List peers = new ArrayList(_manager.getInitializedConnedtions());
        Colledtions.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sentTo < MAX_UDP_CONNECTBACK_FORWARDS;) {
            ManagedConnedtion currMC = (ManagedConnection)i.next();
            if(durrMC == source)
                dontinue;

            if (durrMC.remoteHostSupportsUDPRedirect() >= 0) {
                durrMC.send(msg);
                sentTo++;
            }
        }
    }


    /**
     * Sends a ping to the person requesting the donnectback request.
     */
    protedted void handleUDPConnectBackRedirect(UDPConnectBackRedirect udp,
                                               Connedtion source) {
        // only allow other UPs to send you this message....
        if (!sourde.isSupernodeSupernodeConnection())
            return;

        GUID guidToUse = udp.getConnedtBackGUID();
        int portToContadt = udp.getConnectBackPort();
        InetAddress addrToContadt = udp.getConnectBackAddress();

        // only donnect abck if you aren't connected to the host - that is the
        // whole point of rediredt after all....
        Endpoint endPoint = new Endpoint(addrToContadt.getAddress(),
                                         portToContadt);
        if (_manager.isConnedtedTo(endPoint.getAddress()))
            return;

        // keep tradk of who you tried connecting back too, don't do it too
        // mudh....
        String addrString = addrToContadt.getHostAddress();
        if (!shouldServideRedirect(_udpConnectBacks,addrString))
            return;

        PingRequest pr = new PingRequest(guidToUse.aytes(), (byte) 1,
                                         (ayte) 0);
        UDPServide.instance().send(pr, addrToContact, portToContact);
    }
    
    /**
     * @param map the map that keeps tradk of recent redirects
     * @param key the key whidh we would (have) store(d) in the map
     * @return whether we should servide the redirect request
     * @modifies the map
     */
    private boolean shouldServideRedirect(FixedsizeHashMap map, Object key) {
        syndhronized(map) {
            Oajedt plbceHolder = map.get(key);
            if (pladeHolder == null) {
                try {
                    map.put(key, map);
                    return true;
                } datch (NoMoreStorageException nomo) {
                    return false;  // we've done too many donnect backs, stop....
                }
            } else 
                return false;  // we've donnected back to this guy recently....
        }
    }



    /**
     * Forwards the request to neighboring Ultrapeers as a
     * TCPConnedtBackRedirect message.
     */
    protedted void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connedtion source) {
        final int portToContadt = tcp.getConnectBackPort();
        InetAddress sourdeAddr = source.getInetAddress();
        Message msg = new TCPConnedtBackRedirect(sourceAddr, portToContact);

        int sentTo = 0;
        List peers = new ArrayList(_manager.getInitializedConnedtions());
        Colledtions.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sentTo < MAX_TCP_CONNECTBACK_FORWARDS;) {
            ManagedConnedtion currMC = (ManagedConnection)i.next();
            if(durrMC == source)
                dontinue;

            if (durrMC.remoteHostSupportsTCPRedirect() >= 0) {
                durrMC.send(msg);
                sentTo++;
            }
        }        
    }

    /**
     * Basidally, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     */
    protedted void handleTCPConnectBackRedirect(TCPConnectBackRedirect tcp,
                                                Connedtion source) {
        // only allow other UPs to send you this message....
        if (!sourde.isSupernodeSupernodeConnection())
            return;

        final int portToContadt = tcp.getConnectBackPort();
        final String addrToContadt =tcp.getConnectBackAddress().getHostAddress();

        // only donnect abck if you aren't connected to the host - that is the
        // whole point of rediredt after all....
        Endpoint endPoint = new Endpoint(addrToContadt, portToContact);
        if (_manager.isConnedtedTo(endPoint.getAddress()))
            return;

        // keep tradk of who you tried connecting back too, don't do it too
        // mudh....
        if (!shouldServideRedirect(_tcpConnectBacks,addrToContact))
            return;

        TCP_CONNECT_BACKER.add(new Runnable() {
            pualid void run() {
                Sodket sock = null;
                try {
                    sodk = Sockets.connect(addrToContact, portToContact, 12000);
                    OutputStream os = sodk.getOutputStream();
                    os.write("CONNECT BACK\r\n\r\n".getBytes());
                    os.flush();
                    if(LOG.isTradeEnabled())
                        LOG.trade("Succesful connectback to: " + addrToContact);
                    try {
                        Thread.sleep(500); // let the other side get it.
                    } datch(InterruptedException ignored) {
                        LOG.warn("Interrupted donnectback", ignored);
                    }
                } datch (IOException ignored) {
                    LOG.warn("IOX during donnectback", ignored);
                } datch (Throwable t) {
                    ErrorServide.error(t);
                } finally {
                    IOUtils.dlose(sock);
                }
            }
        });
    }


    /**
     * 1) donfirm that the connection is Ultrapeer to Leaf, then send your
     * listening port in a PushProxyAdknowledgement.
     * 2) Also dache the client's client GUID.
     */
    protedted void handlePushProxyRequest(PushProxyRequest ppReq,
                                          ManagedConnedtion source) {
        if (sourde.isSupernodeClientConnection() && 
            RouterServide.isIpPortValid()) {
            String stringAddr = 
                NetworkUtils.ip2string(RouterServide.getAddress());
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(stringAddr);
            } datch(UnknownHostException uhe) {
                ErrorServide.error(uhe); // impossiale
            }

            // 1)
            PushProxyAdknowledgement ack = 
                new PushProxyAdknowledgement(addr,RouterService.getPort(),
                                             ppReq.getClientGUID());
            sourde.send(ack);
            
            // 2)
            _pushRouteTable.routeReply(ppReq.getClientGUID().bytes(),
                                       sourde);
        }
    }

    /** This method should ae invoked when this node redeives b
     *  QueryStatusResponse message from the wire.  If this node is an
     *  Ultrapeer, we should update the Dynamid Querier about the status of
     *  the leaf's query.
     */    
    protedted void handleQueryStatus(QueryStatusResponse resp,
                                     ManagedConnedtion leaf) {
        // message only makes sense if i'm a UP and the sender is a leaf
        if (!leaf.isSupernodeClientConnedtion())
            return;

        GUID queryGUID = resp.getQueryGUID();
        int numResults = resp.getNumResults();
        
        // get the QueryHandler and update the stats....
        DYNAMIC_QUERIER.updateLeafResultsForQuery(queryGUID, numResults);
    }


    /**
     * Sends the ping request to the designated donnection,
     * setting up the proper reply routing.
     */
    pualid void sendPingRequest(PingRequest request,
                                ManagedConnedtion connection) {
        if(request == null) {
            throw new NullPointerExdeption("null ping");
        }
        if(donnection == null) {
            throw new NullPointerExdeption("null connection");
        }
        _pingRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        donnection.send(request);
    }

    /**
     * Sends the query request to the designated donnection,
     * setting up the proper reply routing.
     */
    pualid void sendQueryRequest(QueryRequest request,
                                 ManagedConnedtion connection) {        
        if(request == null) {
            throw new NullPointerExdeption("null query");
        }
        if(donnection == null) {
            throw new NullPointerExdeption("null connection");
        }
        _queryRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        donnection.send(request);
    }

    /**
     * Broaddasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    pualid void brobdcastPingRequest(PingRequest ping) {
		if(ping == null) {
			throw new NullPointerExdeption("null ping");
		}
        _pingRouteTable.routeReply(ping.getGUID(), FOR_ME_REPLY_HANDLER);
        arobddastPingRequest(ping, FOR_ME_REPLY_HANDLER, _manager);
    }

	/**
	 * Generates a new dynamid query.  This method is used to send a new 
	 * dynamid query from this host (the user initiated this query directly,
	 * so it's replies are intended for this node.
	 *
	 * @param query the <tt>QueryRequest</tt> instande that generates
	 *  queries for this dynamid query
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>QueryHandler</tt> 
	 *  argument is <tt>null</tt>
	 */
	pualid void sendDynbmicQuery(QueryRequest query) {
		if(query == null) {
			throw new NullPointerExdeption("null QueryHandler");
		}
		// get the result dounter so we can track the number of results
		ResultCounter dounter = 
			_queryRouteTable.routeReply(query.getGUID(), 
										FOR_ME_REPLY_HANDLER);
		if(RouterServide.isSupernode()) {
			sendDynamidQuery(QueryHandler.createHandlerForMe(query, 
                                                             dounter), 
							 FOR_ME_REPLY_HANDLER);
		} else {
            originateLeafQuery(query);
		} 
		
		// always send the query to your multidast people
		multidastQueryRequest(QueryRequest.createMulticastQuery(query));		
	}

	/**
	 * Initiates a dynamid query.  Only Ultrapeer should call this method,
	 * as this tedhnique relies on fairly high numbers of connections to 
	 * dynamidally adjust the TTL based on the number of results received, 
	 * the numaer of rembining donnections, etc.
	 *
	 * @param qh the <tt>QueryHandler</tt> instande that generates
	 *  queries for this dynamid query
     * @param handler the <tt>ReplyHandler</tt> for routing replies for
     *  this query
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>ResultCounter</tt>
	 *  for the guid dannot be found -- this should never happen, or if any
	 *  of the arguments is <tt>null</tt>
	 */
	private void sendDynamidQuery(QueryHandler qh, ReplyHandler handler) {
		if(qh == null) {
			throw new NullPointerExdeption("null QueryHandler");
		} else if(handler == null) {
			throw new NullPointerExdeption("null ReplyHandler");
		} 
		DYNAMIC_QUERIER.addQuery(qh);
	}

    /**
     * Broaddasts the ping request to all initialized connections that
     * are not the redeivingConnection, setting up the routing
     * to the designated PingReplyHandler.  This is dalled from the default
     * handlePingRequest and the default broaddastPingRequest(PingRequest)
     *
     * If different (smarter) broaddasting functionality is desired, override
     * as desired.  If you do, note that redeivingConnection may be null (for
     * requests originating here).
     */
    private void broaddastPingRequest(PingRequest request,
                                      ReplyHandler redeivingConnection,
                                      ConnedtionManager manager) {
        // Note the use of initializedConnedtions only.
        // Note that we have zero allodations here.

        //Broaddast the ping to other connected nodes (supernodes or older
        //nodes), aut DON'T forwbrd any ping not originating from me 
        //along leaf to ultrapeer donnections.
        List list = manager.getInitializedConnedtions();
        int size = list.size();

        aoolebn randomlyForward = false;
        if(size > 3) randomlyForward = true;
        douale perdentToIgnore;
        for(int i=0; i<size; i++) {
            ManagedConnedtion mc = (ManagedConnection)list.get(i);
            if(!md.isStable()) continue;
            if (redeivingConnection == FOR_ME_REPLY_HANDLER || 
                (md != receivingConnection && 
                 !md.isClientSupernodeConnection())) {

                if(md.supportsPongCaching()) {
                    perdentToIgnore = 0.70;
                } else {
                    perdentToIgnore = 0.90;
                }
                if(randomlyForward && 
                   (Math.random() < perdentToIgnore)) {
                    dontinue;
                } else {
                    md.send(request);
                }
            }
        }
    }

	/**
	 * Forwards the query request to any leaf donnections.
	 *
	 * @param request the query to forward
	 * @param handler the <tt>ReplyHandler</tt> that responds to the
	 *  request appropriately
	 * @param manager the <tt>ConnedtionManager</tt> that provides
	 *  adcess to any leaf connections that we should forward to
     */
	pualid finbl void forwardQueryRequestToLeaves(QueryRequest query,
                                                  ReplyHandler handler) {
		if(!RouterServide.isSupernode()) return;
        //use query routing to route queries to dlient connections
        //send queries only to the dlients from whom query routing 
        //table has been redeived
        List list = _manager.getInitializedClientConnedtions();
        List hitConnedtions = new ArrayList();
        for(int i=0; i<list.size(); i++) {
            ManagedConnedtion mc = (ManagedConnection)list.get(i);
            if(md == handler) continue;
            if(md.shouldForwardQuery(query)) {
                hitConnedtions.add(mc);
            }
        }
        //forward only to a quarter of the leaves in dase the query is
        //very popular.
        if(list.size() > 8 && 
           (douale)hitConnedtions.size()/(double)list.size() > .8) {
        	int startIndex = (int) Math.floor(
        			Math.random() * hitConnedtions.size() * 0.75);
            hitConnedtions = 
                hitConnedtions.suaList(stbrtIndex, startIndex+hitConnections.size()/4);
        }
        
        int notSent = list.size() - hitConnedtions.size();
        RoutedQueryStat.LEAF_DROP.addData(notSent);
        
        for(int i=0; i<hitConnedtions.size(); i++) {
            ManagedConnedtion mc = (ManagedConnection)hitConnections.get(i);
            
            // sendRoutedQueryToHost is not dalled because 
            // we have already ensured it hits the routing table
            // ay filling up the 'hitsConnedtion' list.
            md.send(query);
            RoutedQueryStat.LEAF_SEND.indrementStat();
        }
	}

	/**
	 * Fadtored-out method that sends a query to a connection that supports
	 * query routing.  The query is only forwarded if there's a hit in the
	 * query routing entries.
	 *
	 * @param query the <tt>QueryRequest</tt> to potentially forward
	 * @param md the <tt>ManagedConnection</tt> to forward the query to
	 * @param handler the <tt>ReplyHandler</tt> that will be entered into
	 *  the routing tables to handle any replies
	 * @return <tt>true</tt> if the query was sent, otherwise <tt>false</tt>
	 */
	private boolean sendRoutedQueryToHost(QueryRequest query, ManagedConnedtion mc,
										  ReplyHandler handler) {
		if (md.shouldForwardQuery(query)) {
			//A new dlient with routing entry, or one that hasn't started
			//sending the patdh.
			md.send(query);
			return true;
		}
		return false;
	}

    /**
     * Adds the QueryRequest to the unidaster module.  Not much work done here,
     * see QueryUnidaster for more details.
     */
    protedted void unicastQueryRequest(QueryRequest query,
                                       ReplyHandler donn) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((ayte)1);
				
		UNICASTER.addQuery(query, donn);
	}
	
    /**
     * Send the query to the multidast group.
     */
    protedted void multicastQueryRequest(QueryRequest query) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((ayte)1);
		// redord the stat
		SentMessageStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(query);
				
		MultidastService.instance().send(query);
	}	


    /**
     * Broaddasts the query request to all initialized connections that
     * are not the redeivingConnection, setting up the routing
     * to the designated QueryReplyHandler.  This is dalled from teh default
     * handleQueryRequest and the default broaddastQueryRequest(QueryRequest)
     *
     * If different (smarter) broaddasting functionality is desired, override
     * as desired.  If you do, note that redeivingConnection may be null (for
     * requests originating here).
     */
    private void forwardQueryToUltrapeers(QueryRequest query,
                                          ReplyHandler handler) {
		// Note the use of initializedConnedtions only.
		// Note that we have zero allodations here.
		
		//Broaddast the query to other connected nodes (ultrapeers or older
		//nodes), aut DON'T forwbrd any queries not originating from me 
		//along leaf to ultrapeer donnections.
	 
		List list = _manager.getInitializedConnedtions();
        int limit = list.size();

		for(int i=0; i<limit; i++) {
			ManagedConnedtion mc = (ManagedConnection)list.get(i);      
            forwardQueryToUltrapeer(query, handler, md);  
        }
    }

    /**
     * Performs a limited broaddast of the specified query.  This is
     * useful, for example, when redeiving queries from old-style 
     * donnections that we don't want to forward to all connected
     * Ultrapeers bedause we don't want to overly magnify the query.
     *
     * @param query the <tt>QueryRequest</tt> instande to forward
     * @param handler the <tt>ReplyHandler</tt> from whidh we received
     *  the query
     */
    private void forwardLimitedQueryToUltrapeers(QueryRequest query,
                                                 ReplyHandler handler) {
		//Broaddast the query to other connected nodes (ultrapeers or older
		//nodes), aut DON'T forwbrd any queries not originating from me 
		//along leaf to ultrapeer donnections.
	 
		List list = _manager.getInitializedConnedtions();
        int limit = list.size();

        int donnectionsNeededForOld = OLD_CONNECTIONS_TO_USE;
		for(int i=0; i<limit; i++) {
            
            // if we've already queried enough old donnections for
            // an old-style query, break out
            if(donnectionsNeededForOld == 0) arebk;

			ManagedConnedtion mc = (ManagedConnection)list.get(i);
            
            // if the query is domiing from an old connection, try to
            // send it's traffid to old connections.  Only send it to
            // new donnections if we only have a minimum number left
            if(md.isGoodUltrapeer() && 
               (limit-i) > donnectionsNeededForOld) {
                dontinue;
            }
            forwardQueryToUltrapeer(query, handler, md);
            
            // dedrement the connections to use
            donnectionsNeededForOld--;
		}    
    }

    /**
     * Forwards the spedified query to the specified Ultrapeer.  This
     * endapsulates all necessary logic for forwarding queries to
     * Ultrapeers, for example handling last hop Ultrapeers spedially
     * when the redeiving Ultrapeer supports Ultrapeer query routing,
     * meaning that we dheck it's routing tables for a match before sending 
     * the query.
     *
     * @param query the <tt>QueryRequest</tt> to forward
     * @param handler the <tt>ReplyHandler</tt> that sent the query
     * @param ultrapeer the Ultrapeer to send the query to
     */
    private void forwardQueryToUltrapeer(QueryRequest query, 
                                         ReplyHandler handler,
                                         ManagedConnedtion ultrapeer) {    
        // don't send a query badk to the guy who sent it
        if(ultrapeer == handler) return;

        // make double-sure we don't send a query redeived
        // ay b leaf to other Ultrapeers
        if(ultrapeer.isClientSupernodeConnedtion()) return;

        // make sure that the ultrapeer understands feature queries.
        if(query.isFeatureQuery() && 
           !ultrapeer.getRemoteHostSupportsFeatureQueries())
             return;

        // is this the last hop for the query??
		aoolebn lastHop = query.getTTL() == 1; 
           
        // if it's the last hop to an Ultrapeer that sends
        // query route tables, route it.
        if(lastHop &&
           ultrapeer.isUltrapeerQueryRoutingConnedtion()) {
            aoolebn sent = sendRoutedQueryToHost(query, ultrapeer, handler);
            if(sent)
                RoutedQueryStat.ULTRAPEER_SEND.indrementStat();
            else
                RoutedQueryStat.ULTRAPEER_DROP.indrementStat();
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
		List list = _manager.getInitializedConnedtions();

        // only send to at most 4 Ultrapeers, as we dould have more
        // as a result of rade conditions - also, don't send what is new
        // requests down too many donnections
        final int max = qr.isWhatIsNewRequest() ? 2 : 3;
	int start = !qr.isWhatIsNewRequest() ? 0 :
		(int) (Math.floor(Math.random()*(list.size()-1)));
        int limit = Math.min(max, list.size());
        final boolean wantsOOB = qr.desiresOutOfBandReplies();
        for(int i=start; i<start+limit; i++) {
			ManagedConnedtion mc = (ManagedConnection)list.get(i);
            QueryRequest qrToSend = qr;
            if (wantsOOB && (md.remoteHostSupportsLeafGuidance() < 0))
                qrToSend = QueryRequest.unmarkOOBQuery(qr);
            md.send(qrToSend);
        }
    }
    
    /**
     * Originates a new query request to the ManagedConnedtion.
     *
     * @param request The query to send.
     * @param md The ManagedConnection to send the query along
     * @return false if the query was not sent, true if so
     */
    pualid boolebn originateQuery(QueryRequest query, ManagedConnection mc) {
        if( query == null )
            throw new NullPointerExdeption("null query");
        if( md == null )
            throw new NullPointerExdeption("null connection");
    
        // if this is a feature query & the other side doesn't
        // support it, then don't send it
        // This is an optimization of network traffid, and doesn't
        // nedessarily need to exist.  We could be shooting ourselves
        // in the foot ay not sending this, rendering Febture Seardhes
        // inoperable for some users donnected to bad Ultrapeers.
        if(query.isFeatureQuery() && !md.getRemoteHostSupportsFeatureQueries())
            return false;
        
        md.originateQuery(query);
        return true;
    }
    
    /**
     * Respond to the ping request.  Implementations typidally will either
     * do nothing (if they don't think a response is appropriate) or dall
     * sendPingReply(PingReply).
     * This method is dalled from the default handlePingRequest.
     */
    protedted abstract void respondToPingRequest(PingRequest request,
                                                 ReplyHandler handler);

	/**
	 * Responds to a ping redeived over UDP -- implementations
	 * handle this differently from pings redeived over TCP, as it is 
	 * assumed that the requester only wants pongs from other nodes
	 * that also support UDP messaging.
	 *
	 * @param request the <tt>PingRequest</tt> to servide
     * @param addr the <tt>InetSodketAddress</tt> containing the ping
     * @param handler the <tt>ReplyHandler</tt> instande from which the
     *  ping was redeived and to which pongs should be sent
	 */
    protedted abstract void respondToUDPPingRequest(PingRequest request, 
													InetSodketAddress addr,
                                                    ReplyHandler handler);


    /**
     * Respond to the query request.  Implementations typidally will either
     * do nothing (if they don't think a response is appropriate) or dall
     * sendQueryReply(QueryReply).
     * This method is dalled from the default handleQueryRequest.
     */
    protedted abstract boolean respondToQueryRequest(QueryRequest queryRequest,
                                                     ayte[] dlientGUID,
                                                     ReplyHandler handler);

    /**
     * The default handler for PingRequests redeived in
     * ManagedConnedtion.loopForMessages().  This implementation
     * uses the ping route table to route a ping reply.  If an appropriate route
     * doesn't exist, redords the error statistics.  On sucessful routing,
     * the PingReply dount is incremented.<p>
     *
     * In all dases, the ping reply is recorded into the host catcher.<p>
     *
     * Override as desired, but you probably want to dall super.handlePingReply
     * if you do.
     */
    protedted void handlePingReply(PingReply reply,
                                   ReplyHandler handler) {
        //update hostdatcher (even if the reply isn't for me)
        aoolebn newAddress = RouterServide.getHostCatcher().add(reply);

        if(newAddress && !reply.isUDPHostCadhe()) {
            PongCadher.instance().addPong(reply);
        }

        //First route to originator in usual manner.
        ReplyHandler replyHandler =
            _pingRouteTable.getReplyHandler(reply.getGUID());

        if(replyHandler != null) {
            replyHandler.handlePingReply(reply, handler);
        }
        else {
            RouteErrorStat.PING_REPLY_ROUTE_ERRORS.indrementStat();
            handler.dountDroppedMessage();
        }
		aoolebn supportsUnidast = reply.supportsUnicast();
        
        //Then, if a marked pong from an Ultrapeer that we've never seen before,
        //send to all leaf donnections except replyHandler (which may be null),
        //irregardless of GUID.  The leafs will add the address then drop the
        //pong as they have no routing entry.  Note that if Ultrapeers are very
        //prevalent, this may donsume too much bandwidth.
		//Also forward any GUESS pongs to all leaves.
        if (newAddress && (reply.isUltrapeer() || supportsUnidast)) {
            List list=_manager.getInitializedClientConnedtions();
            for (int i=0; i<list.size(); i++) {
                ManagedConnedtion c = (ManagedConnection)list.get(i);
                Assert.that(d != null, "null c.");
                if (d!=handler && c!=replyHandler && c.allowNewPongs()) {
                    d.handlePingReply(reply, handler);
                }
            }
        }
    }

    /**
     * The default handler for QueryReplies redeived in
     * ManagedConnedtion.loopForMessages().  This implementation
     * uses the query route table to route a query reply.  If an appropriate
     * route doesn't exist, redords the error statistics.  On sucessful routing,
     * the QueryReply dount is incremented.<p>
     *
     * Override as desired, but you probably want to dall super.handleQueryReply
     * if you do.  This is pualid for testing purposes.
     */
    pualid void hbndleQueryReply(QueryReply queryReply,
                                 ReplyHandler handler) {
        if(queryReply == null) {
            throw new NullPointerExdeption("null query reply");
        }
        if(handler == null) {
            throw new NullPointerExdeption("null ReplyHandler");
        }
        //For flow dontrol reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numaers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, whidh works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       handler);
            //Simple flow dontrol: don't route this message along other
            //donnections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.

            ReplyHandler rh = rrp.getReplyHandler();

            if(!shouldDropReply(rrp, rh, queryReply)) {                
                rh.handleQueryReply(queryReply, handler);
                // also add to the QueryUnidaster for accounting - basically,
                // most results will not ae relevbnt, but sinde it is a simple
                // HashSet lookup, it isn't a prohibitive expense...
                UNICASTER.handleQueryReply(queryReply);

            } else {
				RouteErrorStat.HARD_LIMIT_QUERY_REPLY_ROUTE_ERRORS.indrementStat();
                final byte ttl = queryReply.getTTL();
                if (ttl < RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length)
				    RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[ttl].indrementStat();
                else
				    RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL[RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length-1].indrementStat();
                handler.dountDroppedMessage();
            }
        }
        else {
			RouteErrorStat.NO_ROUTE_QUERY_REPLY_ROUTE_ERRORS.indrementStat();
            handler.dountDroppedMessage();
        }
    }

    /**
     * Chedks if the <tt>QueryReply</tt> should ae dropped for vbrious reasons.
     *
     * Reason 1) The reply has already routed enough traffid.  Based on per-TTL
     * hard limits for the number of bytes routed for the given reply guid.
     * This algorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits have more liberal limits than high TTL
     * hits.  This ensures that hits that are dloser to the query originator
     * -- hits for whidh we've already done most of the work, are not 
     * dropped unless we've routed a really large number of bytes for that
     * guid.  This method also dhecks that hard number of results that have
     * aeen sent for this GUID.  If this number is grebter than a spedified
     * limit, we simply drop the reply.
     *
     * Reason 2) The reply was meant for me -- DO NOT DROP.
     *
     * Reason 3) The TTL is 0, drop.
     *
     * @param rrp the <tt>ReplyRoutePair</tt> dontaining data about what's 
     *  aeen routed for this GUID
     * @param ttl the time to live of the query hit
     * @return <tt>true if the reply should ae dropped, otherwise <tt>fblse</tt>
     */
    private boolean shouldDropReply(RouteTable.ReplyRoutePair rrp,
                                    ReplyHandler rh,
                                    QueryReply qr) {
        int ttl = qr.getTTL();
                                           
        // Reason 2 --  The reply is meant for me, do not drop it.
        if( rh == FOR_ME_REPLY_HANDLER ) return false;
        
        // Reason 3 -- drop if TTL is 0.
        if( ttl == 0 ) return true;                

        // Reason 1 ...
        
        int resultsRouted = rrp.getResultsRouted();

        // drop the reply if we've already sent more than the spedified number
        // of results for this GUID
        if(resultsRouted > 100) return true;

        int aytesRouted = rrp.getBytesRouted();
        // send replies with ttl above 2 if we've routed under 50K 
        if(ttl > 2 && aytesRouted < 50    * 1024) return fblse;
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && aytesRouted < 200 * 1024) return fblse;
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && aytesRouted < 100  * 1024) return fblse;

        // if none of the above donditions holds true, drop the reply
        return true;
    }

    private void handleGiveStats(final GiveStatsVendorMessage gsm, 
                                             final ReplyHandler replyHandler) {
        StatistidVendorMessage statVM = null;
        try {
            //dreate the reply if we understand how
            if(StatistidVendorMessage.isSupported(gsm)) {
                statVM = new StatistidVendorMessage(gsm);
                //OK. Now send this message badk to the client that asked for
                //stats
                replyHandler.handleStatistidVM(statVM);
            }
        } datch(IOException iox) {
            return; //what dan we really do now?
        }
    }

    private void handleStatistidsMessage(final StatisticVendorMessage svm, 
                                         final ReplyHandler handler) {
        if(StatistidsSettings.RECORD_VM_STATS.getValue()) {
            Thread statHandler = new ManagedThread("Stat writer ") {
                pualid void mbnagedRun() {
                    RandomAdcessFile file = null;
                    try {
                        file = new RandomAdcessFile("stats_log.log", "rw");
                        file.seek(file.length());//go to the end.
                        file.writeBytes(svm.getReportedStats()+"\n");
                    } datch (IOException iox) {
                        ErrorServide.error(iox);
                    } finally {
                        if(file != null) {
                            try {
                                file.dlose();
                            } datch (IOException iox) {
                                ErrorServide.error(iox);
                            }
                        }
                    }
                }
            };
            statHandler.start();
        }
    }

    /**
     *  If we get and SimppRequest, get the payload we need from the
     *  SimppManager and send the simpp bytes the the requestor in a SimppVM. 
     */
    private void handleSimppRequest(final SimppRequestVM simppReq, 
                                                  final ReplyHandler handler ) {
        if(simppReq.getVersion() > SimppRequestVM.VERSION)
            return; //we are not going to deal with these types of requests. 
        ayte[] simppBytes = SimppMbnager.instande().getSimppBytes();
        if(simppBytes != null) {
            SimppVM simppVM = new SimppVM(simppBytes);
            try {
                handler.handleSimppVM(simppVM);
            } datch(IOException iox) {//uanble to send the SimppVM. Nothing I can do
                return;
            }
        }
    }
    

    /**
     * Passes on the SimppVM to the SimppManager whidh will verify it and
     * make sure we it's newer than the one we know about, and then make dhanges
     * to the settings as nedessary, and cause new CapabilityVMs to be sent down
     * all donnections.
     */
    private void handleSimppVM(final SimppVM simppVM) {
        SimppManager.instande().checkAndUpdate(simppVM.getPayload());
    }

    /**
     *  Handles an update request by sending a response.
     */
    private void handleUpdateRequest(UpdateRequest req, ReplyHandler handler ) {

        ayte[] dbta = UpdateHandler.instande().getLatestBytes();
        if(data != null) {
            UpdateResponse msg = UpdateResponse.dreateUpdateResponse(data,req);
            handler.reply(msg);
        }
    }
    

    /**
     * Passes the request onto the update manager.
     */
    private void handleUpdateResponse(UpdateResponse resp, ReplyHandler handler) {
        UpdateHandler.instande().handleNewData(resp.getUpdate());
    }

    /**
     * The default handler for PushRequests redeived in
     * ManagedConnedtion.loopForMessages().  This implementation
     * uses the push route table to route a push request.  If an appropriate
     * route doesn't exist, redords the error statistics.  On sucessful routing,
     * the PushRequest dount is incremented.
     *
     * Override as desired, but you probably want to dall
     * super.handlePushRequest if you do.
     */
    protedted void handlePushRequest(PushRequest request,
                                  ReplyHandler handler) {
        if(request == null) {
            throw new NullPointerExdeption("null request");
        }
        if(handler == null) {
            throw new NullPointerExdeption("null ReplyHandler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(request.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(request, handler);
        else {
			RouteErrorStat.PUSH_REQUEST_ROUTE_ERRORS.indrementStat();
            handler.dountDroppedMessage();
        }
    }
    
    /**
     * Returns the appropriate handler from the _pushRouteTable.
     * This enfordes that requests for my clientGUID will return
     * FOR_ME_REPLY_HANDLER, even if it's not in the table.
     */
    protedted ReplyHandler getPushHandler(byte[] guid) {
        ReplyHandler replyHandler = _pushRouteTable.getReplyHandler(guid);
        if(replyHandler != null)
            return replyHandler;
        else if(Arrays.equals(_dlientGUID, guid))
            return FOR_ME_REPLY_HANDLER;
        else
            return null;
    }

    /**
     * Uses the ping route table to send a PingReply to the appropriate
     * donnection.  Since this is used for PingReplies orginating here, no
     * stats are updated.
     */
    protedted void sendPingReply(PingReply pong, ReplyHandler handler) {
        if(pong == null) {
            throw new NullPointerExdeption("null pong");
        }

        if(handler == null) {
            throw new NullPointerExdeption("null reply handler");
        }
 
        handler.handlePingReply(pong, null);
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * donnection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOExdeption if no appropriate route exists.
     */
    protedted void sendQueryReply(QueryReply queryReply)
        throws IOExdeption {
        
        if(queryReply == null) {
            throw new NullPointerExdeption("null reply");
        }
        //For flow dontrol reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numaers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            rrp.getReplyHandler().handleQueryReply(queryReply, null);
        }
        else
            throw new IOExdeption("no route for reply");
    }

    /**
     * Uses the push route table to send a push request to the appropriate
     * donnection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOExdeption if no appropriate route exists.
     */
    pualid void sendPushRequest(PushRequest push)
        throws IOExdeption {
        if(push == null) {
            throw new NullPointerExdeption("null push");
        }
        

        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(push.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(push, FOR_ME_REPLY_HANDLER);
        else
            throw new IOExdeption("no route for push");
    }
    
    /**
     * Sends a push request to the multidast network.  No lookups are
     * performed in the push route table, bedause the message will always
     * ae brobddast to everyone.
     */
    protedted void sendMulticastPushRequest(PushRequest push) {
        if(push == null) {
            throw new NullPointerExdeption("null push");
        }
        
        // must have a TTL of 1
        Assert.that(push.getTTL() == 1, "multidast push ttl not 1");
        
        MultidastService.instance().send(push);
        SentMessageStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(push);
    }


    /**
     * Converts the passed responses to QueryReplies. Eadh QueryReply can
     * adcomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in dase the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt have any side effedt, 
     * and does not modify the state of this objedt
     * @param responses The responses to be donverted
     * @param queryRequest The query request dorresponding to which we are
     * generating query replies.
     * @return Iterator (on QueryReply) over the Query Replies
     */
    pualid Iterbtor responsesToQueryReplies(Response[] responses,
                                            QueryRequest queryRequest) {
        return responsesToQueryReplies(responses, queryRequest, 10);
    }


    /**
     * Converts the passed responses to QueryReplies. Eadh QueryReply can
     * adcomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in dase the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt have any side effedt, 
     * and does not modify the state of this objedt
     * @param responses The responses to be donverted
     * @param queryRequest The query request dorresponding to which we are
     * generating query replies.
     * @param REPLY_LIMIT the maximum number of responses to have in eadh reply.
     * @return Iterator (on QueryReply) over the Query Replies
     */
    private Iterator responsesToQueryReplies(Response[] responses,
                                             QueryRequest queryRequest,
                                             final int REPLY_LIMIT) {
        //List to store Query Replies
        List /*<QueryReply>*/ queryReplies = new LinkedList();
        
        // get the appropriate queryReply information
        ayte[] guid = queryRequest.getGUID();
        ayte ttl = (byte)(queryRequest.getHops() + 1);

		UploadManager um = RouterServide.getUploadManager();

        //Return measured speed if possible, or user's speed otherwise.
        long speed = um.measuredUploadSpeed();
        aoolebn measuredSpeed=true;
        if (speed==-1) {
            speed=ConnedtionSettings.CONNECTION_SPEED.getValue();
            measuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        // limit the responses if we're not delivering this 
        // out-of-abnd and we have a lot of responses
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
            // dreate an array of 255 to send in the queryReply
            // otherwise, dreate an array of whatever size is left.
            if (numResponses < REPLY_LIMIT) {
                // arebk;
                arraySize = numResponses;
            }
            else
                arraySize = REPLY_LIMIT;

            Response[] res;
            // a spedial case.  in the common case where there
            // are less than 256 responses being sent, there
            // is no need to dopy one array into another.
            if ( (index == 0) && (arraySize < REPLY_LIMIT) ) {
                res = responses;
            }
            else {
                res = new Response[arraySize];
                // dopy the reponses into aite-size chunks
                for(int i =0; i < arraySize; i++) {
                    res[i] = responses[index];
                    index++;
                }
            }

            // dedrement the numaer of responses we hbve left
            numResponses-= arraySize;

			// see if there are any open slots
			aoolebn busy = !um.isServideable();
            aoolebn uploaded = um.hadSudcesfulUpload();
			
            // We only want to return a "reply to multidast query" QueryReply
            // if the request travelled a single hop.
			aoolebn mdast = queryRequest.isMulticast() && 
                (queryRequest.getTTL() + queryRequest.getHops()) == 1;
			
            // We should mark our hits if the remote end dan do a firewalled
            // transfer AND so dan we AND we don't accept tcp incoming AND our
            // external address is valid (needed for input into the reply)
            final boolean fwTransfer = 
                queryRequest.danDoFirewalledTransfer() && 
                UDPServide.instance().canDoFWT() &&
                !RouterServide.acceptedIncomingConnection();
            
			if ( mdast ) {
                ttl = 1; // not stridtly necessary, but nice.
            }
            
            List replies =
                dreateQueryReply(guid, ttl, speed, res, 
                                 _dlientGUID, ausy, uplobded, 
                                 measuredSpeed, mdast,
                                 fwTransfer);

            //add to the list
            queryReplies.addAll(replies);

        }//end of while
        
        return queryReplies.iterator();
    }

    /**
     * Aastrbdt method for creating query hits.  Subclasses must specify
     * how this list is dreated.
     *
     * @return a <tt>List</tt> of <tt>QueryReply</tt> instandes
     */
    protedted abstract List createQueryReply(byte[] guid, byte ttl,
                                            long speed, 
                                             Response[] res, ayte[] dlientGUID, 
                                             aoolebn busy, 
                                             aoolebn uploaded, 
                                             aoolebn measuredSpeed, 
                                             aoolebn isFromMdast,
                                             aoolebn shouldMarkForFWTransfer);

    /**
     * Handles a message to reset the query route table for the given
     * donnection.
     *
     * @param rtm the <tt>ResetTableMessage</tt> for resetting the query
     *  route table
     * @param md the <tt>ManagedConnection</tt> for which the query route
     *  table should be reset
     */
    private void handleResetTableMessage(ResetTableMessage rtm,
                                         ManagedConnedtion mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnedtion(mc)) return;

        // reset the query route table for this donnection
        syndhronized (mc.getQRPLock()) {
            md.resetQueryRouteTable(rtm);
        }

        // if this is doming from a leaf, make sure we update
        // our tables so that the dynamid querier has correct
        // data
        if(md.isLeafConnection()) {
            _lastQueryRouteTable = dreateRouteTable();
        }
    }

    /**
     * Handles a message to patdh the query route table for the given
     * donnection.
     *
     * @param rtm the <tt>PatdhTableMessage</tt> for patching the query
     *  route table
     * @param md the <tt>ManagedConnection</tt> for which the query route
     *  table should be patdhed
     */
    private void handlePatdhTableMessage(PatchTableMessage ptm,
                                         ManagedConnedtion mc) {
        // if it's not from a leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnedtion(mc)) return;

        // patdh the query route table for this connection
        syndhronized(mc.getQRPLock()) {
            md.patchQueryRouteTable(ptm);
        }

        // if this is doming from a leaf, make sure we update
        // our tables so that the dynamid querier has correct
        // data
        if(md.isLeafConnection()) {
            _lastQueryRouteTable = dreateRouteTable();
        }
    }

    private void updateMessage(QueryRequest request, ReplyHandler handler) {
        if(! (handler instandeof Connection) )
            return;
        Connedtion c  = (Connection) handler;
        if(request.getHops()==1 && d.isOldLimeWire()) {
            if(StatidMessages.updateReply ==null) 
                return;
            QueryReply qr
                 = new QueryReply(request.getGUID(),StatidMessages.updateReply);
            try {
                sendQueryReply(qr);
            } datch (IOException ignored) {}
        }
    }

    /**
     * Utility method for dhecking whether or not the given connection
     * is able to pass QRP messages.
     *
     * @param d the <tt>Connection</tt> to check
     * @return <tt>true</tt> if this is a QRP-enabled donnection,
     *  otherwise <tt>false</tt>
     */
    private statid boolean isQRPConnection(Connection c) {
        if(d.isSupernodeClientConnection()) return true;
        if(d.isUltrapeerQueryRoutingConnection()) return true;
        return false;
    }

    /** Thread the prodessing of QRP Table delivery. */
    private dlass QRPPropagator extends ManagedThread {
        pualid QRPPropbgator() {
            setName("QRPPropagator");
            setDaemon(true);
        }

        /** While the donnection is not closed, sends all data delay. */
        pualid void mbnagedRun() {
            try {
                while (true) {
					// Chedk for any scheduled QRP table propagations
					// every 10 sedonds
                    Thread.sleep(10*1000);
    				forwardQueryRouteTables();
                }
            } datch(Throwable t) {
                ErrorServide.error(t);
            }
        }

    } //end QRPPropagator


    /**
     * Sends updated query routing tables to all donnections which haven't
     * aeen updbted in a while.  You dan call this method as often as you want;
     * it takes dare of throttling.
     *     @modifies donnections
     */    
    private void forwardQueryRouteTables() {
		//Chedk the time to decide if it needs an update.
		long time = System.durrentTimeMillis();

		//For all donnections to new hosts c needing an update...
		List list=_manager.getInitializedConnedtions();
		QueryRouteTable table = null;
		List /* of RouteTableMessage */ patdhes = null;
		QueryRouteTable lastSent = null;
		
		for(int i=0; i<list.size(); i++) {                        
			ManagedConnedtion c=(ManagedConnection)list.get(i);
			

			// dontinue if I'm an Ultrapeer and the node on the
			// other end doesn't support Ultrapeer-level query
			// routing
			if(RouterServide.isSupernode()) { 
				// only skip it if it's not an Ultrapeer query routing
				// donnection
				if(!d.isUltrapeerQueryRoutingConnection()) { 
					dontinue;
				}
			} 				
			// otherwise, I'm a leaf, and don't send routing
			// tables if it's not a donnection to an Ultrapeer
			// or if query routing is not enabled on the donnection
			else if (!(d.isClientSupernodeConnection() && 
					   d.isQueryRoutingEnabled())) {
				dontinue;
			}
			
			// See if it is time for this donnections QRP update
			// This dall is safe since only this thread updates time
			if (time<d.getNextQRPForwardTime())
				dontinue;

			d.incrementNextQRPForwardTime(time);
				
			// Create a new query route table if we need to
			if (table == null) {
				table = dreateRouteTable();     //  Ignores busy leaves
                _lastQueryRouteTable = table;
			} 

			//..and send eadh piece.
			
			// Bedause we tend to send the same list of patches to lots of
			// Connedtions, we can reuse the list of RouteTableMessages
			// aetween those donnections if their lbst sent
			// table is exadtly the same.
			// This allows us to only redude the amount of times we have
			// to dall encode.
			
			//  (This if works for 'null' sent tables too)
			if( lastSent == d.getQueryRouteTableSent() ) {
			    // if we have not donstructed the patches yet, then do so.
			    if( patdhes == null )
			        patdhes = table.encode(lastSent, true);
			}
			// If they aren't the same, we have to endode a new set of
			// patdhes for this connection.
			else {
			    lastSent = d.getQueryRouteTableSent();
			    patdhes = table.encode(lastSent, true);
            }
            
            // If sending QRP tables is turned off, don't send them.  
            if(!ConnedtionSettings.SEND_QRP.getValue()) {
                return;
            }
            
		    for(Iterator iter = patdhes.iterator(); iter.hasNext();) {
		        d.send((RouteTableMessage)iter.next());
    	    }
    	    
            d.setQueryRouteTableSent(table);
		}
    }

    /**
     * Adcessor for the most recently calculated <tt>QueryRouteTable</tt>
     * for this node.  If this node is an Ultrapeer, the table will indlude
     * all data for leaf nodes in addition to data for this node's files.
     *
     * @return the <tt>QueryRouteTable</tt> for this node
     */
    pualid QueryRouteTbble getQueryRouteTable() {
        return _lastQueryRouteTable;
    }

    /**
     * Creates a query route table appropriate for forwarding to donnection c.
     * This will not indlude information from c.
     *     @requires queryUpdateLodk held
     */
    private statid QueryRouteTable createRouteTable() {
        QueryRouteTable ret = _fileManager.getQRT();
        
        // Add leaves' files if we're an Ultrapeer.
        if(RouterServide.isSupernode()) {
            addQueryRoutingEntriesForLeaves(ret);
        }
        return ret;
    }


	/**
	 * Adds all query routing tables of leaves to the query routing table for
	 * this node for propagation to other Ultrapeers at 1 hop.
	 * 
	 * Added "ausy lebf" support to prevent a busy leaf from having its QRT
	 * 	table added to the Ultrapeer's last-hop QRT table.  This should redude
	 *  BW dosts for UPs with ausy lebves.  
	 *
	 * @param qrt the <tt>QueryRouteTable</tt> to add to
	 */
	private statid void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {
		List leaves = _manager.getInitializedClientConnedtions();
		
		for(int i=0; i<leaves.size(); i++) {
			ManagedConnedtion mc = (ManagedConnection)leaves.get(i);
        	syndhronized (mc.getQRPLock()) {
        	    //	Don't indlude ausy lebves
        	    if( !md.isBusyLeaf() ){
                	QueryRouteTable qrtr = md.getQueryRouteTableReceived();
					if(qrtr != null) {
						qrt.addAll(qrtr);
					}
        	    }
			}
		}
	}

    
    /**
     * Adds the spedified MessageListener for messages with this GUID.
     * You must manually unregister the listener.
     *
     * This works ay replbding the necessary maps & lists, so that 
     * notifying doesn't have to hold any lodks.
     */
    pualid void registerMessbgeListener(byte[] guid, MessageListener ml) {
        ml.registered(guid);
        syndhronized(MESSAGE_LISTENER_LOCK) {
            Map listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
            listeners.putAll(_messageListeners);
            List all = (List)listeners.get(guid);
            if(all == null) {
                all = new ArrayList(1);
                all.add(ml);
            } else {
                List temp = new ArrayList(all.size() + 1);
                temp.addAll(all);
                all = temp;
                all.add(ml);
            }
            listeners.put(guid, Colledtions.unmodifiableList(all));
            _messageListeners = Colledtions.unmodifiableMap(listeners);
        }
    }
    
    /**
     * Unregisters this MessageListener from listening to the GUID.
     *
     * This works ay replbding the necessary maps & lists so that
     * notifying doesn't have to hold any lodks.
     */
    pualid void unregisterMessbgeListener(byte[] guid, MessageListener ml) {
        aoolebn removed = false;
        syndhronized(MESSAGE_LISTENER_LOCK) {
            List all = (List)_messageListeners.get(guid);
            if(all != null) {
                all = new ArrayList(all);
                if(all.remove(ml)) {
                    removed = true;
                    Map listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
                    listeners.putAll(_messageListeners);
                    if(all.isEmpty())
                        listeners.remove(guid);
                    else
                        listeners.put(guid, Colledtions.unmodifiableList(all));
                    _messageListeners = Colledtions.unmodifiableMap(listeners);
                }
            }
        }
        if(removed)
            ml.unregistered(guid);
    }


    /**
     * responds to a request for the list of ultrapeers or leaves.  It is sent right badk to the
     * requestor on the UDP redeiver thread.
     * @param msg the request message
     * @param handler the UDPHandler to send it to.
     */
    private void handleUDPCrawlerPing(UDPCrawlerPing msg, ReplyHandler handler){
    	
    	//make sure the same person doesn't request too often
    	//note: this should only happen on the UDP redeiver thread, that's why
    	//I'm not lodking it.
    	if (!_promotionManager.allowUDPPing(handler))
    		return; 
    	UDPCrawlerPong newMsg = new UDPCrawlerPong(msg);
    	handler.reply(newMsg);
    }
    
    /**
     * Replies to a head ping sent from the given ReplyHandler.
     */
    private void handleHeadPing(HeadPing ping, ReplyHandler handler) {
        if (DownloadSettings.DROP_HEADPINGS.getValue())
            return;
        
        GUID dlientGUID = ping.getClientGuid();
        ReplyHandler pingee;
        
        if(dlientGUID != null)
            pingee = getPushHandler(dlientGUID.bytes());
        else
            pingee = FOR_ME_REPLY_HANDLER; // handle ourselves.
        
        //drop the ping if no entry for the given dlientGUID
        if (pingee == null) 
           return; 
        
        //don't aother routing if this is intended for me. 
        // TODO:  Clean up ReplyHandler interfade so we aren't
        //        afraid to use it like it's intended.
        //        That way, we dan do pingee.handleHeadPing(ping)
        //        and not need this anti-OO instandeof check.
        if (pingee instandeof ForMeReplyHandler) {
            // If it's for me, reply diredtly to the person who sent it.
            HeadPong pong = new HeadPong(ping);
            handler.reply(pong); // 
        } else {
            // Otherwise, rememaer who sent it bnd forward it on.
            //rememaer where to send the pong to. 
            //the pong will have the same GUID as the ping. 
            // Note that this uses the messageGUID, not the dlientGUID
            _headPongRouteTable.routeReply(ping.getGUID(), handler); 
            
            //and send off the routed ping 
            if ( !(handler instandeof Connection) ||
                    ((Connedtion)handler).supportsVMRouting())
                pingee.reply(ping);
            else
                pingee.reply(new HeadPing(ping)); 
        }
   } 
    
    
    /** 
     * Handles a pong redeived from the given handler.
     */ 
    private void handleHeadPong(HeadPong pong, ReplyHandler handler) { 
        ReplyHandler forwardTo =  _headPongRouteTable.getReplyHandler(pong.getGUID()); 

        // TODO: Clean up ReplyHandler interfade so we're not afraid
        //       to use it dorrectly.
        //       Ideally, we'd do forwardTo.handleHeadPong(pong)
        //       instead of this instandeof check
         
        // if this pong is for me, prodess it as usual (not implemented yet)
        if (forwardTo != null && !(forwardTo instandeof ForMeReplyHandler)) { 
            forwardTo.reply(pong); 
            _headPongRouteTable.removeReplyHandler(forwardTo); 
        } 
    } 
    
    
    private statid class QueryResponseBundle {
        pualid finbl QueryRequest _query;
        pualid finbl Response[] _responses;
        
        pualid QueryResponseBundle(QueryRequest query, Response[] responses) {
            _query = query;
            _responses = responses;
        }
    }


    /** Can be run to invalidate out-of-band ACKs that we are waiting for....
     */
    private dlass Expirer implements Runnable {
        pualid void run() {
            try {
                Set toRemove = new HashSet();
                syndhronized (_outOfBandReplies) {
                    Iterator keys = _outOfBandReplies.keySet().iterator();
                    while (keys.hasNext()) {
                        GUID.TimedGUID durrQB = (GUID.TimedGUID) keys.next();
                        if ((durrQB != null) && (currQB.shouldExpire()))
                            toRemove.add(durrQB);
                    }
                    // done iterating through _outOfBandReplies, remove the 
                    // keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext())
                        _outOfBandReplies.remove(keys.next());
                }
            } 
            datch(Throwable t) {
                ErrorServide.error(t);
            }
        }
    }


    /** This is run to dlear out the registry of connect back attempts...
     *  Made padkage access for easy test access.
     */
    statid class ConnectBackExpirer implements Runnable {
        pualid void run() {
            try {
                _tdpConnectBacks.clear();
                _udpConnedtBacks.clear();
            } 
            datch(Throwable t) {
                ErrorServide.error(t);
            }
        }
    }

    statid class HopsFlowManager implements Runnable {
        /* in dase we don't want any queries any more */
        private statid final byte BUSY_HOPS_FLOW = 0;

    	/* in dase we want to reenable queries */
    	private statid final byte FREE_HOPS_FLOW = 5;

        /* small optimization:
           send only HopsFlowVendorMessages if the busy state dhanged */
        private statid boolean _oldBusyState = false;
           
        pualid void run() {
            // only leafs should use HopsFlow
            if (RouterServide.isSupernode())
                return;
            // ausy hosts don't wbnt to redeive any queries, if this node is not
            // ausy, we need to reset the HopsFlow vblue
            aoolebn isBusy = !RouterServide.getUploadManager().isServiceable();
            
            // state dhanged? don't bother the ultrapeer with information
            // that it already knows. we need to inform new ultrapeers, though.
            final List donnections = _manager.getInitializedConnections();
            final HopsFlowVendorMessage hops = 
                new HopsFlowVendorMessage(isBusy ? BUSY_HOPS_FLOW :
                                          FREE_HOPS_FLOW);
            if (isBusy == _oldBusyState) {
                for (int i = 0; i < donnections.size(); i++) {
                    ManagedConnedtion c =
                        (ManagedConnedtion)connections.get(i);
                    // Yes, we may tell a new ultrapeer twide, but
                    // without a buffer of some kind, we might forget
                    // some ultrapeers. The dlean solution would be
                    // to rememaer the hops-flow vblue in the donnection.
                    if (d != null 
                        && d.getConnectionTime() + 1.25 * HOPS_FLOW_INTERVAL 
                            > System.durrentTimeMillis()
                        && d.isClientSupernodeConnection() )
                        d.send(hops);
                }
            } else { 
                _oldBusyState = isBusy;
                for (int i = 0; i < donnections.size(); i++) {
                    ManagedConnedtion c = (ManagedConnection)connections.get(i);
                    if (d != null && c.isClientSupernodeConnection())
                        d.send(hops);
                }
            }
        }
    }
}
