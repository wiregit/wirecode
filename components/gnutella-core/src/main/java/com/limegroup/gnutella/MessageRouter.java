pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.RandomAccessFile;
import jbva.net.InetSocketAddress;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.UnknownHostException;
import jbva.util.ArrayList;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.TreeMap;
import jbva.util.Map;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Hashtable;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Set;

import com.limegroup.gnutellb.guess.GUESSEndpoint;
import com.limegroup.gnutellb.guess.OnDemandUnicaster;
import com.limegroup.gnutellb.guess.QueryKey;
import com.limegroup.gnutellb.messages.*;
import com.limegroup.gnutellb.messages.vendor.*;
import com.limegroup.gnutellb.routing.PatchTableMessage;
import com.limegroup.gnutellb.routing.QueryRouteTable;
import com.limegroup.gnutellb.routing.ResetTableMessage;
import com.limegroup.gnutellb.routing.RouteTableMessage;
import com.limegroup.gnutellb.search.QueryDispatcher;
import com.limegroup.gnutellb.search.QueryHandler;
import com.limegroup.gnutellb.search.ResultCounter;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.DownloadSettings;
import com.limegroup.gnutellb.settings.SearchSettings;
import com.limegroup.gnutellb.settings.StatisticsSettings;
import com.limegroup.gnutellb.simpp.SimppManager;
import com.limegroup.gnutellb.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutellb.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutellb.statistics.RouteErrorStat;
import com.limegroup.gnutellb.statistics.RoutedQueryStat;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.udpconnect.UDPConnectionMessage;
import com.limegroup.gnutellb.udpconnect.UDPMultiplexor;
import com.limegroup.gnutellb.upelection.PromotionManager;
import com.limegroup.gnutellb.util.FixedSizeExpiringSet;
import com.limegroup.gnutellb.util.FixedsizeHashMap;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.NoMoreStorageException;
import com.limegroup.gnutellb.util.Sockets;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.ProcessingQueue;
import com.limegroup.gnutellb.version.UpdateHandler;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;


/**
 * One of the three clbsses that make up the core of the backend.  This
 * clbss' job is to direct the routing of messages and to count those message
 * bs they pass through.  To do so, it aggregates a ConnectionManager that
 * mbintains a list of connections.
 */
public bbstract class MessageRouter {
    
    privbte static final Log LOG = LogFactory.getLog(MessageRouter.class);
	
    /**
     * Hbndle to the <tt>ConnectionManager</tt> to access our TCP connections.
     */
    protected stbtic ConnectionManager _manager;

    /**
     * Constbnt for the number of old connections to use when forwarding
     * trbffic from old connections.
     */
    privbte static final int OLD_CONNECTIONS_TO_USE = 15;

    /**
     * The GUID we bttach to QueryReplies to allow PushRequests in
     * responses.
     */
    protected byte[] _clientGUID;


	/**
	 * Reference to the <tt>ReplyHbndler</tt> for messages intended for 
	 * this node.
	 */
    privbte final ReplyHandler FOR_ME_REPLY_HANDLER = 
		ForMeReplyHbndler.instance();
		
    /**
     * The mbximum size for <tt>RouteTable</tt>s.
     */
    privbte int MAX_ROUTE_TABLE_SIZE = 50000;  //actually 100,000 entries

    /**
     * The mbximum number of bypassed results to remember per query.
     */
    privbte final int MAX_BYPASSED_RESULTS = 150;

    /**
     * Mbps PingRequest GUIDs to PingReplyHandlers.  Stores 2-4 minutes,
     * typicblly around 2500 entries, but never more than 100,000 entries.
     */
    privbte RouteTable _pingRouteTable = 
        new RouteTbble(2*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Mbps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typicblly around 13000 entries, but never more than 100,000 entries.
     */
    privbte RouteTable _queryRouteTable = 
        new RouteTbble(5*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Mbps QueryReply client GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typicblly around 3500 entries, but never more than 100,000
     * entries.  
     */
    privbte RouteTable _pushRouteTable = 
        new RouteTbble(7*60, MAX_ROUTE_TABLE_SIZE);
    
    /**
     * Mbps HeadPong guids to the originating pingers.  Short-lived since
     * we expect replies from our lebves quickly.
     */
    privbte RouteTable _headPongRouteTable = 
    	new RouteTbble(10, MAX_ROUTE_TABLE_SIZE);

    /** How long to buffer up out-of-bbnd replies.
     */
    privbte static final long CLEAR_TIME = 30 * 1000; // 30 seconds

    /** Time between sending HopsFlow messbges.
     */
    privbte static final long HOPS_FLOW_INTERVAL = 15 * 1000; // 15 seconds

    /** The mbximum number of UDP replies to buffer up.  Non-final for 
     *  testing.
     */
    stbtic int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps trbck of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wbnts them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryResponseBundle.
     */
    privbte final Map _outOfBandReplies = new Hashtable();

    /**
     * Keeps trbck of potential sources of content.  Comprised of Sets of GUESS
     * Endpoints.  Kept tidy when sebrches/downloads are killed.
     */
    privbte final Map _bypassedResults = new HashMap();

    /**
     * Keeps trbck of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited bnd once the size is reached, no more connect
     * bbck attempts will be honored.
     */
    privbte static final FixedsizeHashMap _udpConnectBacks = 
        new FixedsizeHbshMap(200);
        
    /**
     * The mbximum numbers of ultrapeers to forward a UDPConnectBackRedirect
     * messbge to, per forward.
     */
    privbte static final int MAX_UDP_CONNECTBACK_FORWARDS = 5;

    /**
     * Keeps trbck of what hosts we have recently tried to connect back to via
     * TCP.  The size is limited bnd once the size is reached, no more connect
     * bbck attempts will be honored.
     */
    privbte static final FixedsizeHashMap _tcpConnectBacks = 
        new FixedsizeHbshMap(200);
        
    /**
     * The mbximum numbers of ultrapeers to forward a TCPConnectBackRedirect
     * messbge to, per forward.
     */
    privbte static final int MAX_TCP_CONNECTBACK_FORWARDS = 5;        
    
    /**
     * The processingqueue to bdd tcpconnectback socket connections to.
     */
    privbte static final ProcessingQueue TCP_CONNECT_BACKER =
        new ProcessingQueue("TCPConnectBbck");
    
    /**
     * keeps trbck of which hosts have sent us head pongs.  We may choose
     * to use these messbges for udp tunnel keep-alive, so we don't want to
     * set the minimum intervbl too high.  Right now it is half of what we
     * believe to be the solicited grbce period.
     */
    privbte static final Set _udpHeadRequests =
    	Collections.synchronizedSet(new FixedSizeExpiringSet(200,
    			ConnectionSettings.SOLICITED_GRACE_PERIOD.getVblue()/2));

	/**
	 * Constbnt handle to the <tt>QueryUnicaster</tt> since it is called
	 * upon very frequently.
	 */
	protected finbl QueryUnicaster UNICASTER = QueryUnicaster.instance();

	/**
	 * Constbnt for the <tt>QueryDispatcher</tt> that handles dynamically
	 * generbted queries that adjust to the number of results received, the
	 * number of connections, etc.
	 */
	privbte final QueryDispatcher DYNAMIC_QUERIER = QueryDispatcher.instance();
	
	/**
	 * Hbndle to the <tt>ActivityCallback</tt> for sending data to the 
	 * displby.
	 */
	privbte ActivityCallback _callback;

	/**
	 * Hbndle to the <tt>FileManager</tt> instance.
	 */
	privbte static FileManager _fileManager;
    
	/**
	 * A hbndle to the thread that deals with QRP Propagation
	 */
	privbte final QRPPropagator QRP_PROPAGATOR = new QRPPropagator();


    /**
     * Vbriable for the most recent <tt>QueryRouteTable</tt> created
     * for this node.  If this node is bn Ultrapeer, the routing
     * tbble will include the tables from its leaves.
     */
    privbte QueryRouteTable _lastQueryRouteTable;

    /**
     * The mbximum number of response to send to a query that has
     * b "high" number of hops.
     */
    privbte static final int HIGH_HOPS_RESPONSE_LIMIT = 10;

    /**
     * The lifetime of OOBs guids.
     */
    privbte static final long TIMED_GUID_LIFETIME = 25 * 1000; 

    /**
     * Keeps trbck of Listeners of GUIDs.
     * GUID -> List of MessbgeListener
     */
    privbte volatile Map _messageListeners = Collections.EMPTY_MAP;
    
    /**
     * Lock thbt registering & unregistering listeners can hold
     * while replbcing the listeners map / lists.
     */
    privbte final Object MESSAGE_LISTENER_LOCK = new Object();

    /**
     * ref to the promotion mbnager.
     */
    privbte PromotionManager _promotionManager;
    
    /**
     * Router for UDPConnection messbges.
     */
	privbte final UDPMultiplexor _udpConnectionMultiplexor =
	    UDPMultiplexor.instbnce(); 

    /**
     * The time we lbst received a request for a query key.
     */
    privbte long _lastQueryKeyTime;
    
    /**
     * Crebtes a MessageRouter.  Must call initialize before using.
     */
    protected MessbgeRouter() {
        _clientGUID=RouterService.getMyGUID();
    }

    /**
     * Links the MessbgeRouter up with the other back end pieces
     */
    public void initiblize() {
        _mbnager = RouterService.getConnectionManager();
		_cbllback = RouterService.getCallback();
		_fileMbnager = RouterService.getFileManager();
		_promotionMbnager = RouterService.getPromotionManager();
	    QRP_PROPAGATOR.stbrt();

        // schedule b runner to clear unused out-of-band replies
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        // schedule b runner to clear guys we've connected back to
        RouterService.schedule(new ConnectBbckExpirer(), 10 * CLEAR_TIME, 
                               10 * CLEAR_TIME);
        // schedule b runner to send hops-flow messages
        RouterService.schedule(new HopsFlowMbnager(), HOPS_FLOW_INTERVAL*10, 
                               HOPS_FLOW_INTERVAL);
    }

    /**
     * Routes b query GUID to yourself.
     */
    public void originbteQueryGUID(byte[] guid) {
        _queryRouteTbble.routeReply(guid, FOR_ME_REPLY_HANDLER);
    }

    /** Cbll this to inform us that a query has been killed by a user or
     *  whbtever.  Useful for purging unneeded info.<br>
     *  Cbllers of this should make sure that they have purged the guid from
     *  their tbbles.
     *  @throws IllegblArgumentException if the guid is null
     */
    public void queryKilled(GUID guid) throws IllegblArgumentException {
        if (guid == null)
            throw new IllegblArgumentException("Input GUID is null!");
        synchronized (_bypbssedResults) {
        if (!RouterService.getDownlobdManager().isGuidForQueryDownloading(guid))
            _bypbssedResults.remove(guid);
        }
    }

    /** Cbll this to inform us that a download is finished or whatever.  Useful
     *  for purging unneeded info.<br>
     *  If the cbller is a Downloader, please be sure to clear yourself from the
     *  bctive and waiting lists in DownloadManager.
     *  @throws IllegblArgumentException if the guid is null
     */
    public void downlobdFinished(GUID guid) throws IllegalArgumentException {
        if (guid == null)
            throw new IllegblArgumentException("Input GUID is null!");
        synchronized (_bypbssedResults) {
        if (!_cbllback.isQueryAlive(guid) && 
            !RouterService.getDownlobdManager().isGuidForQueryDownloading(guid))
            _bypbssedResults.remove(guid);
        }
    }
    
    /** @returns b Set with GUESSEndpoints that had matches for the
     *  originbl query guid.  may be empty.
     *  @pbram guid the guid of the query you want endpoints for.
     */
    public Set getGuessLocs(GUID guid) {
        Set clone = new HbshSet();
        synchronized (_bypbssedResults) {
            Set eps = (Set) _bypbssedResults.get(guid);
            if (eps != null)
                clone.bddAll(eps);
        }
        return clone;
    }
    
    public String getPingRouteTbbleDump() {
        return _pingRouteTbble.toString();
    }

    public String getQueryRouteTbbleDump() {
        return _queryRouteTbble.toString();
    }

    public String getPushRouteTbbleDump() {
        return _pushRouteTbble.toString();
    }

    /**
     * A cbllback for ConnectionManager to clear a <tt>ReplyHandler</tt> from
     * the routing tbbles when the connection is closed.
     */
    public void removeConnection(ReplyHbndler rh) {
        DYNAMIC_QUERIER.removeReplyHbndler(rh);
        _pingRouteTbble.removeReplyHandler(rh);
        _queryRouteTbble.removeReplyHandler(rh);
        _pushRouteTbble.removeReplyHandler(rh);
        _hebdPongRouteTable.removeReplyHandler(rh);
    }

	/**
     * The hbndler for all message types.  Processes a message based on the 
     * messbge type.
	 *
	 * @pbram m the <tt>Message</tt> instance to route appropriately
	 * @pbram receivingConnection the <tt>ManagedConnection</tt> over which
	 *  the messbge was received
     */
    public void hbndleMessage(Message msg, 
                              MbnagedConnection receivingConnection) {
        // Increment hops bnd decrease TTL.
        msg.hop();
	   
        if(msg instbnceof PingRequest) {
            ReceivedMessbgeStatHandler.TCP_PING_REQUESTS.addMessage(msg);
            hbndlePingRequestPossibleDuplicate((PingRequest)msg, 
											   receivingConnection);
		} else if (msg instbnceof PingReply) {
			ReceivedMessbgeStatHandler.TCP_PING_REPLIES.addMessage(msg);
            hbndlePingReply((PingReply)msg, receivingConnection);
		} else if (msg instbnceof QueryRequest) {
			ReceivedMessbgeStatHandler.TCP_QUERY_REQUESTS.addMessage(msg);
            hbndleQueryRequestPossibleDuplicate(
                (QueryRequest)msg, receivingConnection);
		} else if (msg instbnceof QueryReply) {
			ReceivedMessbgeStatHandler.TCP_QUERY_REPLIES.addMessage(msg);
            // if someone sent b TCP QueryReply with the MCAST header,
            // thbt's bad, so ignore it.
            QueryReply qmsg = (QueryReply)msg;
            hbndleQueryReply(qmsg, receivingConnection);            
		} else if (msg instbnceof PushRequest) {
			ReceivedMessbgeStatHandler.TCP_PUSH_REQUESTS.addMessage(msg);
            hbndlePushRequest((PushRequest)msg, receivingConnection);
		} else if (msg instbnceof ResetTableMessage) {
			ReceivedMessbgeStatHandler.TCP_RESET_ROUTE_TABLE_MESSAGES.addMessage(msg);
            hbndleResetTableMessage((ResetTableMessage)msg,
                                    receivingConnection);
		} else if (msg instbnceof PatchTableMessage) {
			ReceivedMessbgeStatHandler.TCP_PATCH_ROUTE_TABLE_MESSAGES.addMessage(msg);
            hbndlePatchTableMessage((PatchTableMessage)msg,
                                    receivingConnection);            
        }
        else if (msg instbnceof TCPConnectBackVendorMessage) {
            ReceivedMessbgeStatHandler.TCP_TCP_CONNECTBACK.addMessage(msg);
            hbndleTCPConnectBackRequest((TCPConnectBackVendorMessage) msg,
                                        receivingConnection);
        }
        else if (msg instbnceof UDPConnectBackVendorMessage) {
			ReceivedMessbgeStatHandler.TCP_UDP_CONNECTBACK.addMessage(msg);
            hbndleUDPConnectBackRequest((UDPConnectBackVendorMessage) msg,
                                        receivingConnection);
        }
        else if (msg instbnceof TCPConnectBackRedirect) {
            hbndleTCPConnectBackRedirect((TCPConnectBackRedirect) msg,
                                         receivingConnection);
        }
        else if (msg instbnceof UDPConnectBackRedirect) {
            hbndleUDPConnectBackRedirect((UDPConnectBackRedirect) msg,
                                         receivingConnection);
        }
        else if (msg instbnceof PushProxyRequest) {
            hbndlePushProxyRequest((PushProxyRequest) msg, receivingConnection);
        }
        else if (msg instbnceof QueryStatusResponse) {
            hbndleQueryStatus((QueryStatusResponse) msg, receivingConnection);
        }
        else if (msg instbnceof GiveStatsVendorMessage) {
            //TODO: bdd the statistics recording code
            hbndleGiveStats((GiveStatsVendorMessage)msg, receivingConnection);
        }
        else if(msg instbnceof StatisticVendorMessage) {
            //TODO: bdd the statistics recording code
            hbndleStatisticsMessage(
                            (StbtisticVendorMessage)msg, receivingConnection);
        }
        else if (msg instbnceof HeadPing) {
        	//TODO: bdd the statistics recording code
        	hbndleHeadPing((HeadPing)msg, receivingConnection);
        }
        else if(msg instbnceof SimppRequestVM) {
            hbndleSimppRequest((SimppRequestVM)msg, receivingConnection);
        }
        else if(msg instbnceof SimppVM) {
            hbndleSimppVM((SimppVM)msg);
        } 
        else if(msg instbnceof UpdateRequest) {
            hbndleUpdateRequest((UpdateRequest)msg, receivingConnection);
        }
        else if(msg instbnceof UpdateResponse) {
            hbndleUpdateResponse((UpdateResponse)msg, receivingConnection);
        }
        else if (msg instbnceof HeadPong) {  
            hbndleHeadPong((HeadPong)msg, receivingConnection); 
        } 
        else if (msg instbnceof VendorMessage) {
            receivingConnection.hbndleVendorMessage((VendorMessage)msg);
        }
        
        //This mby trigger propogation of query route tables.  We do this AFTER
        //bny handshake pings.  Otherwise we'll think all clients are old
        //clients.
		//forwbrdQueryRouteTables();
        notifyMessbgeListener(msg, receivingConnection);
    }

    /**
     * Notifies bny message listeners of this message's guid about the message.
     * This holds no locks.
     */
    privbte final void notifyMessageListener(Message msg, ReplyHandler handler) {
        List bll = (List)_messageListeners.get(msg.getGUID());
        if(bll != null) {
            for(Iterbtor i = all.iterator(); i.hasNext(); ) {
                MessbgeListener next = (MessageListener)i.next();
                next.processMessbge(msg, handler);
            }
        }
    }

	/**
     * The hbndler for all message types.  Processes a message based on the 
     * messbge type.
	 *
	 * @pbram msg the <tt>Message</tt> received
	 * @pbram addr the <tt>InetSocketAddress</tt> containing the IP and 
	 *  port of the client node
     */	
	public void hbndleUDPMessage(Message msg, InetSocketAddress addr) {
        // Increment hops bnd decrement TTL.
        msg.hop();

		InetAddress bddress = addr.getAddress();
		int port = bddr.getPort();
		// Verify thbt the address and port are valid.
		// If they bre not, we cannot send any replies to them.
		if(!RouterService.isIpPortVblid()) return;

		// Send UDPConnection messbges on to the connection multiplexor
		// for routing to the bppropriate connection processor
		if ( msg instbnceof UDPConnectionMessage ) {
		    _udpConnectionMultiplexor.routeMessbge(
			  (UDPConnectionMessbge)msg, address, port);
			return;
		}

		ReplyHbndler handler = new UDPReplyHandler(address, port);
		
        if (msg instbnceof QueryRequest) {
            //TODO: compbre QueryKey with old generation params.  if it matches
            //send b new one generated with current params 
            if (hbsValidQueryKey(address, port, (QueryRequest) msg)) {
                sendAcknowledgement(bddr, msg.getGUID());
                // b TTL above zero may indicate a malicious client, as UDP
                // messbges queries should not be sent with TTL above 1.
                //if(msg.getTTL() > 0) return;
                if (!hbndleUDPQueryRequestPossibleDuplicate(
                  (QueryRequest)msg, hbndler) ) {
                    ReceivedMessbgeStatHandler.UDP_DUPLICATE_QUERIES.addMessage(msg);
                }  
            }
            ReceivedMessbgeStatHandler.UDP_QUERY_REQUESTS.addMessage(msg);
		} else if (msg instbnceof QueryReply) {
            QueryReply qr = (QueryReply) msg;
			ReceivedMessbgeStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
            int numResps = qr.getResultCount();
            // only bccount for OOB stuff if this was response to a 
            // OOB query, multicbst stuff is sent over UDP too....
            if (!qr.isReplyToMulticbstQuery())
                OutOfBbndThroughputStat.RESPONSES_RECEIVED.addData(numResps);
			
            hbndleQueryReply(qr, handler);
            
		} else if(msg instbnceof PingRequest) {
			ReceivedMessbgeStatHandler.UDP_PING_REQUESTS.addMessage(msg);
			hbndleUDPPingRequestPossibleDuplicate((PingRequest)msg, 
												  hbndler, addr);
		} else if(msg instbnceof PingReply) {
			ReceivedMessbgeStatHandler.UDP_PING_REPLIES.addMessage(msg);
            hbndleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instbnceof PushRequest) {
			ReceivedMessbgeStatHandler.UDP_PUSH_REQUESTS.addMessage(msg);
			hbndlePushRequest((PushRequest)msg, handler);
		} else if(msg instbnceof LimeACKVendorMessage) {
			ReceivedMessbgeStatHandler.UDP_LIME_ACK.addMessage(msg);
            hbndleLimeACKMessage((LimeACKVendorMessage)msg, addr);
        }
        else if(msg instbnceof ReplyNumberVendorMessage) {
            hbndleReplyNumberMessage((ReplyNumberVendorMessage) msg, addr);
        }
        else if(msg instbnceof GiveStatsVendorMessage) {
            hbndleGiveStats((GiveStatsVendorMessage) msg, handler);
        }
        else if(msg instbnceof StatisticVendorMessage) {
            hbndleStatisticsMessage((StatisticVendorMessage)msg, handler);
        }
        else if(msg instbnceof UDPCrawlerPing) {
        	//TODO: bdd the statistics recording code
        	hbndleUDPCrawlerPing((UDPCrawlerPing)msg, handler);
        }
        else if (msg instbnceof HeadPing) {
        	//TODO: bdd the statistics recording code
        	hbndleHeadPing((HeadPing)msg, handler);
        } 
        else if(msg instbnceof UpdateRequest) {
            hbndleUpdateRequest((UpdateRequest)msg, handler);
        }
        notifyMessbgeListener(msg, handler);
    }
    
    /**
     * The hbndler for Multicast messages. Processes a message based on the
     * messbge type.
     *
     * @pbram msg the <tt>Message</tt> recieved.
     * @pbram addr the <tt>InetSocketAddress</tt> containing the IP and
     *  port of the client node.
     */
	public void hbndleMulticastMessage(Message msg, InetSocketAddress addr) {
    
        // Use this bssert for testing only -- it is a dangerous assert
        // to hbve in the field, as not all messages currently set the
        // network int bppropriately.
        // If someone sends us messbges we're not prepared to handle,
        // this could cbuse widespreaad AssertFailures
        //Assert.thbt(msg.isMulticast(),
        //   "non multicbst message in handleMulticastMessage: " + msg);
    
        // no multicbst messages should ever have been
        // set with b TTL greater than 1.
        if( msg.getTTL() > 1 )
            return;

        // Increment hops bnd decrement TTL.
        msg.hop();

		InetAddress bddress = addr.getAddress();
		int port = bddr.getPort();
		
        if (NetworkUtils.isLocblAddress(address) &&
          !ConnectionSettings.ALLOW_MULTICAST_LOOPBACK.getVblue())
            return;
		
		ReplyHbndler handler = new UDPReplyHandler(address, port);
		
        if (msg instbnceof QueryRequest) {
            if(!hbndleUDPQueryRequestPossibleDuplicate(
              (QueryRequest)msg, hbndler) ) {
                ReceivedMessbgeStatHandler.MULTICAST_DUPLICATE_QUERIES.addMessage(msg);
            }
            ReceivedMessbgeStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(msg);
	//	} else if (msg instbnceof QueryReply) {			
	//		  ReceivedMessbgeStatHandler.UDP_QUERY_REPLIES.addMessage(msg);
    //        hbndleQueryReply((QueryReply)msg, handler);
		} else if(msg instbnceof PingRequest) {
			ReceivedMessbgeStatHandler.MULTICAST_PING_REQUESTS.addMessage(msg);
			hbndleUDPPingRequestPossibleDuplicate((PingRequest)msg, 
												  hbndler, addr);
	//	} else if(msg instbnceof PingReply) {
	//	      ReceivedMessbgeStatHandler.UDP_PING_REPLIES.addMessage(msg);
    //        hbndleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instbnceof PushRequest) {
            ReceivedMessbgeStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(msg);
			hbndlePushRequest((PushRequest)msg, handler);
		}
        notifyMessbgeListener(msg, handler);
    }


    /**
     * Returns true if the Query hbs a valid QueryKey.  false if it isn't
     * present or vblid.
     */
    protected boolebn hasValidQueryKey(InetAddress ip, int port, 
                                       QueryRequest qr) {
        if (qr.getQueryKey() == null)
            return fblse;
        QueryKey computedQK = QueryKey.getQueryKey(ip, port);
        return qr.getQueryKey().equbls(computedQK);
    }

	/**
	 * Sends bn ack back to the GUESS client node.  
	 */
	protected void sendAcknowledgement(InetSocketAddress bddr, byte[] guid) {
		ConnectionMbnager manager = RouterService.getConnectionManager();
		Endpoint host = mbnager.getConnectedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
                
                reply = PingReply.crebteGUESSReply(guid, (byte)1, host);
            } cbtch(UnknownHostException e) {
				reply = crebtePingReply(guid);
            }
		} else {
			reply = crebtePingReply(guid);
		}
		
		// No GUESS endpoints existed bnd our IP/port was invalid.
		if( reply == null )
		    return;

        UDPService.instbnce().send(reply, addr.getAddress(), addr.getPort());
		SentMessbgeStatHandler.UDP_PING_REPLIES.addMessage(reply);
	}

	/**
	 * Crebtes a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or b <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints bre available.
	 */
	privbte PingReply createPingReply(byte[] guid) {
		GUESSEndpoint endpoint = UNICASTER.getUnicbstEndpoint();
		if(endpoint == null) {
		    if(RouterService.isIpPortVblid())
                return PingReply.crebte(guid, (byte)1);
            else
                return null;
		} else {
            return PingReply.crebteGUESSReply(guid, (byte)1, 
                                              endpoint.getPort(),
                                              endpoint.getAddress().getAddress());
		}
	}



	
    /**
     * The hbndler for PingRequests received in
     * MbnagedConnection.loopForMessages().  Checks the routing table to see
     * if the request hbs already been seen.  If not, calls handlePingRequest.
     */
    finbl void handlePingRequestPossibleDuplicate(
        PingRequest request, ReplyHbndler handler) {
		if(_pingRouteTbble.tryToRouteReply(request.getGUID(), handler) != null)
            hbndlePingRequest(request, handler);
    }

    /**
     * The hbndler for PingRequests received in
     * MbnagedConnection.loopForMessages().  Checks the routing table to see
     * if the request hbs already been seen.  If not, calls handlePingRequest.
     */
    finbl void handleUDPPingRequestPossibleDuplicate(													 
        PingRequest request, ReplyHbndler handler, InetSocketAddress  addr) {
		if(_pingRouteTbble.tryToRouteReply(request.getGUID(), handler) != null)
            hbndleUDPPingRequest(request, handler, addr);
    }

    /**
     * The hbndler for QueryRequests received in
     * MbnagedConnection.loopForMessages().  Checks the routing table to see
     * if the request hbs already been seen.  If not, calls handleQueryRequest.
     */
    finbl void handleQueryRequestPossibleDuplicate(
        QueryRequest request, MbnagedConnection receivingConnection) {
        
        // With the new hbndling of probe queries (TTL 1, Hops 0), we have a few
        // new options:
        // 1) If we hbve a probe query....
        //  b) If you have never seen it before, put it in the route table and
        //  set the ttl bppropriately
        //  b) If you hbve seen it before, then just count it as a duplicate
        // 2) If it isn't b probe query....
        //  b) Is it an extension of a probe?  If so re-adjust the TTL.
        //  b) Is it b 'normal' query (no probe extension or already extended)?
        //  Then check if it is b duplicate:
        //    1) If it b duplicate, just count it as one
        //    2) If it isn't, put it in the route tbble but no need to setTTL

        // we msg.hop() before we get here....
        // hops mby be 1 or 2 because we may be probing with a leaf query....
        finbl boolean isProbeQuery = 
            ((request.getTTL() == 0) && 
             ((request.getHops() == 1) || (request.getHops() == 2)));

		ResultCounter counter = 
			_queryRouteTbble.tryToRouteReply(request.getGUID(), 
											 receivingConnection);

		if(counter != null) {  // query is new (probe or normbl)

            // 1b: set the TTL of the query so it can be potentially extended  
            if (isProbeQuery) 
                _queryRouteTbble.setTTL(counter, (byte)1);

            // 1b and 2b2
            // if b new probe or a new request, do everything (so input true
            // below)
            hbndleQueryRequest(request, receivingConnection, counter, true);
		}
        // if (counter == null) the query hbs been seen before, few subcases... 
        else if ((counter == null) && !isProbeQuery) {// probe extension?

            if (wbsProbeQuery(request))
                // rebrobdcast out but don't locally evaluate....
                hbndleQueryRequest(request, receivingConnection, counter, 
                                   fblse);
            else  // 2b1: not b correct extension, so call it a duplicate....
                tbllyDupQuery(request);
        }
        else if ((counter == null) && isProbeQuery) // 1b: duplicbte probe
            tbllyDupQuery(request);
        else // 2b1: duplicbte normal query
            tbllyDupQuery(request);
    }
	
    privbte boolean wasProbeQuery(QueryRequest request) {
        // if the current TTL is lbrge enough and the old TTL was 1, then this
        // wbs a probe query....
        // NOTE: thbt i'm setting the ttl to be the actual ttl of the query.  i
        // could set it to some mbx value, but since we only allow TTL 1 queries
        // to be extended, it isn't b big deal what i set it to.  in fact, i'm
        // setting the ttl to the correct vblue if we had full expanding rings
        // of queries.
        return ((request.getTTL() > 0) && 
                _queryRouteTbble.getAndSetTTL(request.getGUID(), (byte)1, 
                                              (byte)(request.getTTL()+1)));
    }

    privbte void tallyDupQuery(QueryRequest request) {
        ReceivedMessbgeStatHandler.TCP_DUPLICATE_QUERIES.addMessage(request);
    }

	/**
	 * Specibl handler for UDP queries.  Checks the routing table to see if
	 * the request hbs already been seen, handling it if not.
	 *
	 * @pbram query the UDP <tt>QueryRequest</tt> 
	 * @pbram handler the <tt>ReplyHandler</tt> that will handle the reply
	 * @return fblse if it was a duplicate, true if it was not.
	 */
	finbl boolean handleUDPQueryRequestPossibleDuplicate(QueryRequest request,
													  ReplyHbndler handler)  {
		ResultCounter counter = 
			_queryRouteTbble.tryToRouteReply(request.getGUID(), 
											 hbndler);
		if(counter != null) {
            hbndleQueryRequest(request, handler, counter, true);
            return true;
		}
		return fblse;
	}

    /**
     * Hbndles pings from the network.  With the addition of pong caching, this
     * method will either respond with cbched pongs, or it will ignore the ping
     * entirely if bnother ping has been received from this connection very
     * recently.  If the ping is TTL=1, we will blways process it, as it may
     * be b hearbeat ping to make sure the connection is alive and well.
     *
     * @pbram ping the ping to handle
     * @pbram handler the <tt>ReplyHandler</tt> instance that sent the ping
     */
    finbl private void handlePingRequest(PingRequest ping,
                                         ReplyHbndler handler) {
        // Send it blong if it's a heartbeat ping or if we should allow new 
        // pings on this connection.
        if(ping.isHebrtbeat() || handler.allowNewPings()) {
            respondToPingRequest(ping, hbndler);
        } 
    }


    /**
     * The defbult handler for PingRequests received in
     * MbnagedConnection.loopForMessages().  This implementation updates stats,
     * does the brobdcast, and generates a response.
     *
     * You cbn customize behavior in three ways:
     *   1. Override. You cbn assume that duplicate messages
     *      (messbges with the same GUID that arrived via different paths) have
     *      blready been filtered.  If you want stats updated, you'll
     *      hbve to call super.handlePingRequest.
     *   2. Override brobdcastPingRequest.  This allows you to use the default
     *      hbndling framework and just customize request routing.
     *   3. Implement respondToPingRequest.  This bllows you to use the default
     *      hbndling framework and just customize responses.
     */
    protected void hbndleUDPPingRequest(PingRequest pingRequest,
										ReplyHbndler handler, 
										InetSocketAddress bddr) {
        if (pingRequest.isQueryKeyRequest())
            sendQueryKeyPong(pingRequest, bddr);
        else
            respondToUDPPingRequest(pingRequest, bddr, handler);
    }
    

    /**
     * Generbtes a QueryKey for the source (described by addr) and sends the
     * QueryKey to it vib a QueryKey pong....
     */
    protected void sendQueryKeyPong(PingRequest pr, InetSocketAddress bddr) {

        // check if we're getting bombbrded
        long now = System.currentTimeMillis();
        if (now - _lbstQueryKeyTime < SearchSettings.QUERY_KEY_DELAY.getValue())
            return;
        
        _lbstQueryKeyTime = now;
        
        // bfter find more sources and OOB queries, everyone can dole out query
        // keys....

        // generbte a QueryKey (quite quick - current impl. (DES) is super
        // fbst!
        InetAddress bddress = addr.getAddress();
        int port = bddr.getPort();
        QueryKey key = QueryKey.getQueryKey(bddress, port);
        
        // respond with Pong with QK, bs GUESS requires....
        PingReply reply = 
            PingReply.crebteQueryKeyReply(pr.getGUID(), (byte)1, key);
        UDPService.instbnce().send(reply, addr.getAddress(), addr.getPort());
    }


    protected void hbndleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress bddress, int port) {
        if (reply.getQueryKey() != null) {
            // this is b PingReply in reply to my QueryKey Request - 
            //consume the Pong bnd return, don't process as usual....
            OnDembndUnicaster.handleQueryKeyPong(reply);
            return;
        }

        // blso add the sender of the pong if different from the host
        // described in the reply...
        if((reply.getPort() != port) || 
           (!reply.getInetAddress().equbls(address))) {
            UNICASTER.bddUnicastEndpoint(address, port);
		}
        
        // normbl pong processing...
        hbndlePingReply(reply, handler);
    }

    
    /**
     * The defbult handler for QueryRequests received in
     * MbnagedConnection.loopForMessages().  This implementation updates stats,
     * does the brobdcast, and generates a response.
     *
     * You cbn customize behavior in three ways:
     *   1. Override. You cbn assume that duplicate messages
     *      (messbges with the same GUID that arrived via different paths) have
     *      blready been filtered.  If you want stats updated, you'll
     *      hbve to call super.handleQueryRequest.
     *   2. Override brobdcastQueryRequest.  This allows you to use the default
     *      hbndling framework and just customize request routing.
     *   3. Implement respondToQueryRequest.  This bllows you to use the default
     *      hbndling framework and just customize responses.
     *
     * @pbram locallyEvaluate false if you don't want to send the query to
     * lebves and yourself, true otherwise....
     */
    protected void hbndleQueryRequest(QueryRequest request,
									  ReplyHbndler handler, 
									  ResultCounter counter,
                                      boolebn locallyEvaluate) {
        // Apply the personbl filter to decide whether the callback
        // should be informed of the query
        if (!hbndler.isPersonalSpam(request)) {
            _cbllback.handleQueryString(request.getQuery());
        }
        
		// if it's b request from a leaf and we GUESS, send it out via GUESS --
		// otherwise, brobdcast it if it still has TTL
		//if(hbndler.isSupernodeClientConnection() && 
		// RouterService.isGUESSCbpable()) 
		//unicbstQueryRequest(request, handler);
        //else if(request.getTTL() > 0) {
        updbteMessage(request, handler);

		if(hbndler.isSupernodeClientConnection() && counter != null) {
            if (request.desiresOutOfBbndReplies()) {
                // this query cbme from a leaf - so check if it desires OOB
                // responses bnd make sure that the IP it advertises is legit -
                // if it isn't drop bway....
                // no need to check the port - if you bre attacking yourself you
                // got problems
                String remoteAddr = hbndler.getInetAddress().getHostAddress();
                String myAddress = 
                    NetworkUtils.ip2string(RouterService.getAddress());
                if (request.getReplyAddress().equbls(remoteAddr))
                    ; // continue below, everything looks good
                else if (request.getReplyAddress().equbls(myAddress) && 
                         RouterService.isOOBCbpable())
                    // i bm proxying - maybe i should check my success rate but
                    // whbtever...
                    ; 
                else return;
            }

            // don't send it to lebves here -- the dynamic querier will 
            // hbndle that
            locbllyEvaluate = false;
            
            // do respond with files thbt we may have, though
            respondToQueryRequest(request, _clientGUID, hbndler);
            
            multicbstQueryRequest(request);
            
			if(hbndler.isGoodLeaf()) {
				sendDynbmicQuery(QueryHandler.createHandlerForNewLeaf(request, 
																	  hbndler,
                                                                      counter), 
								 hbndler);
			} else {
				sendDynbmicQuery(QueryHandler.createHandlerForOldLeaf(request,
																	  hbndler,
                                                                      counter), 
								 hbndler);
			}
		} else if(request.getTTL() > 0 && RouterService.isSupernode()) {
            // send the request to intrb-Ultrapeer connections -- this does
			// not send the request to lebves
            if(hbndler.isGoodUltrapeer()) {
                // send it to everyone
                forwbrdQueryToUltrapeers(request, handler);
            } else {
                // otherwise, only send it to some connections
                forwbrdLimitedQueryToUltrapeers(request, handler);
            }
		}
			
        if (locbllyEvaluate) {
            // blways forward any queries to leaves -- this only does
            // bnything when this node's an Ultrapeer
            forwbrdQueryRequestToLeaves(request, handler);
            
            // if (I'm firewblled AND the source is firewalled) AND 
            // NOT(he cbn do a FW transfer and so can i) then don't reply...
            if ((request.isFirewblledSource() &&
                 !RouterService.bcceptedIncomingConnection()) &&
                !(request.cbnDoFirewalledTransfer() &&
                  UDPService.instbnce().canDoFWT())
                )
                return;
            respondToQueryRequest(request, _clientGUID, hbndler);
        }
    }

    /** Hbndles a ACK message - looks up the QueryReply and sends it out of
     *  bbnd.
     */
    protected void hbndleLimeACKMessage(LimeACKVendorMessage ack,
                                        InetSocketAddress bddr) {

        GUID.TimedGUID refGUID = new GUID.TimedGUID(new GUID(bck.getGUID()),
                                                    TIMED_GUID_LIFETIME);
        QueryResponseBundle bundle = 
            (QueryResponseBundle) _outOfBbndReplies.remove(refGUID);

        if ((bundle != null) && (bck.getNumResults() > 0)) {
            InetAddress ibddr = addr.getAddress();
            int port = bddr.getPort();

            //convert responses to QueryReplies, but only send bs many as the
            //node wbnts
            Iterbtor iterator = null;
            if (bck.getNumResults() < bundle._responses.length) {
                Response[] desired = new Response[bck.getNumResults()];
                for (int i = 0; i < desired.length; i++)
                    desired[i] = bundle._responses[i];
                iterbtor = responsesToQueryReplies(desired, bundle._query, 1);
            }
            else 
                iterbtor = responsesToQueryReplies(bundle._responses, 
                                                   bundle._query, 1); 
            //send the query replies
            while(iterbtor.hasNext()) {
                QueryReply queryReply = (QueryReply)iterbtor.next();
                UDPService.instbnce().send(queryReply, iaddr, port);
            }
        }
        // else some sort of routing error or bttack?
        // TODO: tblly some stat stuff here
    }

    /** This is cblled when a client on the network has results for us that we
     *  mby want.  We may contact them back directly or just cache them for
     *  use.
     */
    protected void hbndleReplyNumberMessage(ReplyNumberVendorMessage reply,
                                            InetSocketAddress bddr) {
        GUID qGUID = new GUID(reply.getGUID());
        int numResults = 
        RouterService.getSebrchResultHandler().getNumResultsForQuery(qGUID);
        if (numResults < 0) // this mby be a proxy query
            numResults = DYNAMIC_QUERIER.getLebfResultsForQuery(qGUID);

        // see if we need more results for this query....
        // if not, remember this locbtion for a future, 'find more sources'
        // tbrgeted GUESS query, as long as the other end said they can receive
        // unsolicited.
        if ((numResults<0) || (numResults>QueryHbndler.ULTRAPEER_RESULTS)) {
            OutOfBbndThroughputStat.RESPONSES_BYPASSED.addData(reply.getNumResults());

            //if the reply cbnnot receive unsolicited udp, there is no point storing it.
            if (!reply.cbnReceiveUnsolicited())
            	return;
            
            DownlobdManager dManager = RouterService.getDownloadManager();
            // only store result if it is being shown to the user or if b
            // file with the sbme guid is being downloaded
            if (!_cbllback.isQueryAlive(qGUID) && 
                !dMbnager.isGuidForQueryDownloading(qGUID))
                return;

            GUESSEndpoint ep = new GUESSEndpoint(bddr.getAddress(),
                                                 bddr.getPort());
            synchronized (_bypbssedResults) {
                // this is b quick critical section for _bypassedResults
                // AND the set within it
                Set eps = (Set) _bypbssedResults.get(qGUID);
                if (eps == null) {
                    eps = new HbshSet();
                    _bypbssedResults.put(qGUID, eps);
                }
                if (_bypbssedResults.size() <= MAX_BYPASSED_RESULTS)
                    eps.bdd(ep);
            }

            return;
        }
        
        LimeACKVendorMessbge ack = 
            new LimeACKVendorMessbge(qGUID, reply.getNumResults());
        UDPService.instbnce().send(ack, addr.getAddress(), addr.getPort());
        OutOfBbndThroughputStat.RESPONSES_REQUESTED.addData(reply.getNumResults());
    }


    /** Stores (for b limited time) the resps for later out-of-band delivery -
     *  interbcts with handleLimeACKMessage
     *  @return true if the operbtion failed, false if not (i.e. too busy)
     */
    protected boolebn bufferResponsesForLaterDelivery(QueryRequest query,
                                                      Response[] resps) {
        // store responses by guid for lbter retrieval
        synchronized (_outOfBbndReplies) {
            if (_outOfBbndReplies.size() < MAX_BUFFERED_REPLIES) {
                GUID.TimedGUID tGUID = 
                    new GUID.TimedGUID(new GUID(query.getGUID()),
                                       TIMED_GUID_LIFETIME);
                _outOfBbndReplies.put(tGUID, new QueryResponseBundle(query, 
                                                                     resps));
                return true;
            }
            return fblse;
        }
    }


    /**
     * Forwbrds the UDPConnectBack to neighboring peers
     * bs a UDPConnectBackRedirect request.
     */
    protected void hbndleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connection source) {

        GUID guidToUse = udp.getConnectBbckGUID();
        int portToContbct = udp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Messbge msg = new UDPConnectBackRedirect(guidToUse, sourceAddr, 
                                                 portToContbct);

        int sentTo = 0;
        List peers = new ArrbyList(_manager.getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterbtor i = peers.iterator(); i.hasNext() && sentTo < MAX_UDP_CONNECTBACK_FORWARDS;) {
            MbnagedConnection currMC = (ManagedConnection)i.next();
            if(currMC == source)
                continue;

            if (currMC.remoteHostSupportsUDPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }
    }


    /**
     * Sends b ping to the person requesting the connectback request.
     */
    protected void hbndleUDPConnectBackRedirect(UDPConnectBackRedirect udp,
                                               Connection source) {
        // only bllow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection())
            return;

        GUID guidToUse = udp.getConnectBbckGUID();
        int portToContbct = udp.getConnectBackPort();
        InetAddress bddrToContact = udp.getConnectBackAddress();

        // only connect bbck if you aren't connected to the host - that is the
        // whole point of redirect bfter all....
        Endpoint endPoint = new Endpoint(bddrToContact.getAddress(),
                                         portToContbct);
        if (_mbnager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep trbck of who you tried connecting back too, don't do it too
        // much....
        String bddrString = addrToContact.getHostAddress();
        if (!shouldServiceRedirect(_udpConnectBbcks,addrString))
            return;

        PingRequest pr = new PingRequest(guidToUse.bytes(), (byte) 1,
                                         (byte) 0);
        UDPService.instbnce().send(pr, addrToContact, portToContact);
    }
    
    /**
     * @pbram map the map that keeps track of recent redirects
     * @pbram key the key which we would (have) store(d) in the map
     * @return whether we should service the redirect request
     * @modifies the mbp
     */
    privbte boolean shouldServiceRedirect(FixedsizeHashMap map, Object key) {
        synchronized(mbp) {
            Object plbceHolder = map.get(key);
            if (plbceHolder == null) {
                try {
                    mbp.put(key, map);
                    return true;
                } cbtch (NoMoreStorageException nomo) {
                    return fblse;  // we've done too many connect backs, stop....
                }
            } else 
                return fblse;  // we've connected back to this guy recently....
        }
    }



    /**
     * Forwbrds the request to neighboring Ultrapeers as a
     * TCPConnectBbckRedirect message.
     */
    protected void hbndleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connection source) {
        finbl int portToContact = tcp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Messbge msg = new TCPConnectBackRedirect(sourceAddr, portToContact);

        int sentTo = 0;
        List peers = new ArrbyList(_manager.getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterbtor i = peers.iterator(); i.hasNext() && sentTo < MAX_TCP_CONNECTBACK_FORWARDS;) {
            MbnagedConnection currMC = (ManagedConnection)i.next();
            if(currMC == source)
                continue;

            if (currMC.remoteHostSupportsTCPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }        
    }

    /**
     * Bbsically, just get the correct parameters, create a Socket, and
     * send b "/n/n".
     */
    protected void hbndleTCPConnectBackRedirect(TCPConnectBackRedirect tcp,
                                                Connection source) {
        // only bllow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection())
            return;

        finbl int portToContact = tcp.getConnectBackPort();
        finbl String addrToContact =tcp.getConnectBackAddress().getHostAddress();

        // only connect bbck if you aren't connected to the host - that is the
        // whole point of redirect bfter all....
        Endpoint endPoint = new Endpoint(bddrToContact, portToContact);
        if (_mbnager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep trbck of who you tried connecting back too, don't do it too
        // much....
        if (!shouldServiceRedirect(_tcpConnectBbcks,addrToContact))
            return;

        TCP_CONNECT_BACKER.bdd(new Runnable() {
            public void run() {
                Socket sock = null;
                try {
                    sock = Sockets.connect(bddrToContact, portToContact, 12000);
                    OutputStrebm os = sock.getOutputStream();
                    os.write("CONNECT BACK\r\n\r\n".getBytes());
                    os.flush();
                    if(LOG.isTrbceEnabled())
                        LOG.trbce("Succesful connectback to: " + addrToContact);
                    try {
                        Threbd.sleep(500); // let the other side get it.
                    } cbtch(InterruptedException ignored) {
                        LOG.wbrn("Interrupted connectback", ignored);
                    }
                } cbtch (IOException ignored) {
                    LOG.wbrn("IOX during connectback", ignored);
                } cbtch (Throwable t) {
                    ErrorService.error(t);
                } finblly {
                    IOUtils.close(sock);
                }
            }
        });
    }


    /**
     * 1) confirm thbt the connection is Ultrapeer to Leaf, then send your
     * listening port in b PushProxyAcknowledgement.
     * 2) Also cbche the client's client GUID.
     */
    protected void hbndlePushProxyRequest(PushProxyRequest ppReq,
                                          MbnagedConnection source) {
        if (source.isSupernodeClientConnection() && 
            RouterService.isIpPortVblid()) {
            String stringAddr = 
                NetworkUtils.ip2string(RouterService.getAddress());
            InetAddress bddr = null;
            try {
                bddr = InetAddress.getByName(stringAddr);
            } cbtch(UnknownHostException uhe) {
                ErrorService.error(uhe); // impossible
            }

            // 1)
            PushProxyAcknowledgement bck = 
                new PushProxyAcknowledgement(bddr,RouterService.getPort(),
                                             ppReq.getClientGUID());
            source.send(bck);
            
            // 2)
            _pushRouteTbble.routeReply(ppReq.getClientGUID().bytes(),
                                       source);
        }
    }

    /** This method should be invoked when this node receives b
     *  QueryStbtusResponse message from the wire.  If this node is an
     *  Ultrbpeer, we should update the Dynamic Querier about the status of
     *  the lebf's query.
     */    
    protected void hbndleQueryStatus(QueryStatusResponse resp,
                                     MbnagedConnection leaf) {
        // messbge only makes sense if i'm a UP and the sender is a leaf
        if (!lebf.isSupernodeClientConnection())
            return;

        GUID queryGUID = resp.getQueryGUID();
        int numResults = resp.getNumResults();
        
        // get the QueryHbndler and update the stats....
        DYNAMIC_QUERIER.updbteLeafResultsForQuery(queryGUID, numResults);
    }


    /**
     * Sends the ping request to the designbted connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest request,
                                MbnagedConnection connection) {
        if(request == null) {
            throw new NullPointerException("null ping");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _pingRouteTbble.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Sends the query request to the designbted connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest request,
                                 MbnagedConnection connection) {        
        if(request == null) {
            throw new NullPointerException("null query");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _queryRouteTbble.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Brobdcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void brobdcastPingRequest(PingRequest ping) {
		if(ping == null) {
			throw new NullPointerException("null ping");
		}
        _pingRouteTbble.routeReply(ping.getGUID(), FOR_ME_REPLY_HANDLER);
        brobdcastPingRequest(ping, FOR_ME_REPLY_HANDLER, _manager);
    }

	/**
	 * Generbtes a new dynamic query.  This method is used to send a new 
	 * dynbmic query from this host (the user initiated this query directly,
	 * so it's replies bre intended for this node.
	 *
	 * @pbram query the <tt>QueryRequest</tt> instance that generates
	 *  queries for this dynbmic query
	 * @throws <tt>NullPointerException</tt> if the <tt>QueryHbndler</tt> 
	 *  brgument is <tt>null</tt>
	 */
	public void sendDynbmicQuery(QueryRequest query) {
		if(query == null) {
			throw new NullPointerException("null QueryHbndler");
		}
		// get the result counter so we cbn track the number of results
		ResultCounter counter = 
			_queryRouteTbble.routeReply(query.getGUID(), 
										FOR_ME_REPLY_HANDLER);
		if(RouterService.isSupernode()) {
			sendDynbmicQuery(QueryHandler.createHandlerForMe(query, 
                                                             counter), 
							 FOR_ME_REPLY_HANDLER);
		} else {
            originbteLeafQuery(query);
		} 
		
		// blways send the query to your multicast people
		multicbstQueryRequest(QueryRequest.createMulticastQuery(query));		
	}

	/**
	 * Initibtes a dynamic query.  Only Ultrapeer should call this method,
	 * bs this technique relies on fairly high numbers of connections to 
	 * dynbmically adjust the TTL based on the number of results received, 
	 * the number of rembining connections, etc.
	 *
	 * @pbram qh the <tt>QueryHandler</tt> instance that generates
	 *  queries for this dynbmic query
     * @pbram handler the <tt>ReplyHandler</tt> for routing replies for
     *  this query
	 * @throws <tt>NullPointerException</tt> if the <tt>ResultCounter</tt>
	 *  for the guid cbnnot be found -- this should never happen, or if any
	 *  of the brguments is <tt>null</tt>
	 */
	privbte void sendDynamicQuery(QueryHandler qh, ReplyHandler handler) {
		if(qh == null) {
			throw new NullPointerException("null QueryHbndler");
		} else if(hbndler == null) {
			throw new NullPointerException("null ReplyHbndler");
		} 
		DYNAMIC_QUERIER.bddQuery(qh);
	}

    /**
     * Brobdcasts the ping request to all initialized connections that
     * bre not the receivingConnection, setting up the routing
     * to the designbted PingReplyHandler.  This is called from the default
     * hbndlePingRequest and the default broadcastPingRequest(PingRequest)
     *
     * If different (smbrter) broadcasting functionality is desired, override
     * bs desired.  If you do, note that receivingConnection may be null (for
     * requests originbting here).
     */
    privbte void broadcastPingRequest(PingRequest request,
                                      ReplyHbndler receivingConnection,
                                      ConnectionMbnager manager) {
        // Note the use of initiblizedConnections only.
        // Note thbt we have zero allocations here.

        //Brobdcast the ping to other connected nodes (supernodes or older
        //nodes), but DON'T forwbrd any ping not originating from me 
        //blong leaf to ultrapeer connections.
        List list = mbnager.getInitializedConnections();
        int size = list.size();

        boolebn randomlyForward = false;
        if(size > 3) rbndomlyForward = true;
        double percentToIgnore;
        for(int i=0; i<size; i++) {
            MbnagedConnection mc = (ManagedConnection)list.get(i);
            if(!mc.isStbble()) continue;
            if (receivingConnection == FOR_ME_REPLY_HANDLER || 
                (mc != receivingConnection && 
                 !mc.isClientSupernodeConnection())) {

                if(mc.supportsPongCbching()) {
                    percentToIgnore = 0.70;
                } else {
                    percentToIgnore = 0.90;
                }
                if(rbndomlyForward && 
                   (Mbth.random() < percentToIgnore)) {
                    continue;
                } else {
                    mc.send(request);
                }
            }
        }
    }

	/**
	 * Forwbrds the query request to any leaf connections.
	 *
	 * @pbram request the query to forward
	 * @pbram handler the <tt>ReplyHandler</tt> that responds to the
	 *  request bppropriately
	 * @pbram manager the <tt>ConnectionManager</tt> that provides
	 *  bccess to any leaf connections that we should forward to
     */
	public finbl void forwardQueryRequestToLeaves(QueryRequest query,
                                                  ReplyHbndler handler) {
		if(!RouterService.isSupernode()) return;
        //use query routing to route queries to client connections
        //send queries only to the clients from whom query routing 
        //tbble has been received
        List list = _mbnager.getInitializedClientConnections();
        List hitConnections = new ArrbyList();
        for(int i=0; i<list.size(); i++) {
            MbnagedConnection mc = (ManagedConnection)list.get(i);
            if(mc == hbndler) continue;
            if(mc.shouldForwbrdQuery(query)) {
                hitConnections.bdd(mc);
            }
        }
        //forwbrd only to a quarter of the leaves in case the query is
        //very populbr.
        if(list.size() > 8 && 
           (double)hitConnections.size()/(double)list.size() > .8) {
        	int stbrtIndex = (int) Math.floor(
        			Mbth.random() * hitConnections.size() * 0.75);
            hitConnections = 
                hitConnections.subList(stbrtIndex, startIndex+hitConnections.size()/4);
        }
        
        int notSent = list.size() - hitConnections.size();
        RoutedQueryStbt.LEAF_DROP.addData(notSent);
        
        for(int i=0; i<hitConnections.size(); i++) {
            MbnagedConnection mc = (ManagedConnection)hitConnections.get(i);
            
            // sendRoutedQueryToHost is not cblled because 
            // we hbve already ensured it hits the routing table
            // by filling up the 'hitsConnection' list.
            mc.send(query);
            RoutedQueryStbt.LEAF_SEND.incrementStat();
        }
	}

	/**
	 * Fbctored-out method that sends a query to a connection that supports
	 * query routing.  The query is only forwbrded if there's a hit in the
	 * query routing entries.
	 *
	 * @pbram query the <tt>QueryRequest</tt> to potentially forward
	 * @pbram mc the <tt>ManagedConnection</tt> to forward the query to
	 * @pbram handler the <tt>ReplyHandler</tt> that will be entered into
	 *  the routing tbbles to handle any replies
	 * @return <tt>true</tt> if the query wbs sent, otherwise <tt>false</tt>
	 */
	privbte boolean sendRoutedQueryToHost(QueryRequest query, ManagedConnection mc,
										  ReplyHbndler handler) {
		if (mc.shouldForwbrdQuery(query)) {
			//A new client with routing entry, or one thbt hasn't started
			//sending the pbtch.
			mc.send(query);
			return true;
		}
		return fblse;
	}

    /**
     * Adds the QueryRequest to the unicbster module.  Not much work done here,
     * see QueryUnicbster for more details.
     */
    protected void unicbstQueryRequest(QueryRequest query,
                                       ReplyHbndler conn) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
				
		UNICASTER.bddQuery(query, conn);
	}
	
    /**
     * Send the query to the multicbst group.
     */
    protected void multicbstQueryRequest(QueryRequest query) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
		// record the stbt
		SentMessbgeStatHandler.MULTICAST_QUERY_REQUESTS.addMessage(query);
				
		MulticbstService.instance().send(query);
	}	


    /**
     * Brobdcasts the query request to all initialized connections that
     * bre not the receivingConnection, setting up the routing
     * to the designbted QueryReplyHandler.  This is called from teh default
     * hbndleQueryRequest and the default broadcastQueryRequest(QueryRequest)
     *
     * If different (smbrter) broadcasting functionality is desired, override
     * bs desired.  If you do, note that receivingConnection may be null (for
     * requests originbting here).
     */
    privbte void forwardQueryToUltrapeers(QueryRequest query,
                                          ReplyHbndler handler) {
		// Note the use of initiblizedConnections only.
		// Note thbt we have zero allocations here.
		
		//Brobdcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forwbrd any queries not originating from me 
		//blong leaf to ultrapeer connections.
	 
		List list = _mbnager.getInitializedConnections();
        int limit = list.size();

		for(int i=0; i<limit; i++) {
			MbnagedConnection mc = (ManagedConnection)list.get(i);      
            forwbrdQueryToUltrapeer(query, handler, mc);  
        }
    }

    /**
     * Performs b limited broadcast of the specified query.  This is
     * useful, for exbmple, when receiving queries from old-style 
     * connections thbt we don't want to forward to all connected
     * Ultrbpeers because we don't want to overly magnify the query.
     *
     * @pbram query the <tt>QueryRequest</tt> instance to forward
     * @pbram handler the <tt>ReplyHandler</tt> from which we received
     *  the query
     */
    privbte void forwardLimitedQueryToUltrapeers(QueryRequest query,
                                                 ReplyHbndler handler) {
		//Brobdcast the query to other connected nodes (ultrapeers or older
		//nodes), but DON'T forwbrd any queries not originating from me 
		//blong leaf to ultrapeer connections.
	 
		List list = _mbnager.getInitializedConnections();
        int limit = list.size();

        int connectionsNeededForOld = OLD_CONNECTIONS_TO_USE;
		for(int i=0; i<limit; i++) {
            
            // if we've blready queried enough old connections for
            // bn old-style query, break out
            if(connectionsNeededForOld == 0) brebk;

			MbnagedConnection mc = (ManagedConnection)list.get(i);
            
            // if the query is comiing from bn old connection, try to
            // send it's trbffic to old connections.  Only send it to
            // new connections if we only hbve a minimum number left
            if(mc.isGoodUltrbpeer() && 
               (limit-i) > connectionsNeededForOld) {
                continue;
            }
            forwbrdQueryToUltrapeer(query, handler, mc);
            
            // decrement the connections to use
            connectionsNeededForOld--;
		}    
    }

    /**
     * Forwbrds the specified query to the specified Ultrapeer.  This
     * encbpsulates all necessary logic for forwarding queries to
     * Ultrbpeers, for example handling last hop Ultrapeers specially
     * when the receiving Ultrbpeer supports Ultrapeer query routing,
     * mebning that we check it's routing tables for a match before sending 
     * the query.
     *
     * @pbram query the <tt>QueryRequest</tt> to forward
     * @pbram handler the <tt>ReplyHandler</tt> that sent the query
     * @pbram ultrapeer the Ultrapeer to send the query to
     */
    privbte void forwardQueryToUltrapeer(QueryRequest query, 
                                         ReplyHbndler handler,
                                         MbnagedConnection ultrapeer) {    
        // don't send b query back to the guy who sent it
        if(ultrbpeer == handler) return;

        // mbke double-sure we don't send a query received
        // by b leaf to other Ultrapeers
        if(ultrbpeer.isClientSupernodeConnection()) return;

        // mbke sure that the ultrapeer understands feature queries.
        if(query.isFebtureQuery() && 
           !ultrbpeer.getRemoteHostSupportsFeatureQueries())
             return;

        // is this the lbst hop for the query??
		boolebn lastHop = query.getTTL() == 1; 
           
        // if it's the lbst hop to an Ultrapeer that sends
        // query route tbbles, route it.
        if(lbstHop &&
           ultrbpeer.isUltrapeerQueryRoutingConnection()) {
            boolebn sent = sendRoutedQueryToHost(query, ultrapeer, handler);
            if(sent)
                RoutedQueryStbt.ULTRAPEER_SEND.incrementStat();
            else
                RoutedQueryStbt.ULTRAPEER_DROP.incrementStat();
        } else {
            // otherwise, just send it out
            ultrbpeer.send(query);
        }
    }


    /**
     * Originbte a new query from this leaf node.
     *
     * @pbram qr the <tt>QueryRequest</tt> to send
     */
    privbte void originateLeafQuery(QueryRequest qr) {
		List list = _mbnager.getInitializedConnections();

        // only send to bt most 4 Ultrapeers, as we could have more
        // bs a result of race conditions - also, don't send what is new
        // requests down too mbny connections
        finbl int max = qr.isWhatIsNewRequest() ? 2 : 3;
	int stbrt = !qr.isWhatIsNewRequest() ? 0 :
		(int) (Mbth.floor(Math.random()*(list.size()-1)));
        int limit = Mbth.min(max, list.size());
        finbl boolean wantsOOB = qr.desiresOutOfBandReplies();
        for(int i=stbrt; i<start+limit; i++) {
			MbnagedConnection mc = (ManagedConnection)list.get(i);
            QueryRequest qrToSend = qr;
            if (wbntsOOB && (mc.remoteHostSupportsLeafGuidance() < 0))
                qrToSend = QueryRequest.unmbrkOOBQuery(qr);
            mc.send(qrToSend);
        }
    }
    
    /**
     * Originbtes a new query request to the ManagedConnection.
     *
     * @pbram request The query to send.
     * @pbram mc The ManagedConnection to send the query along
     * @return fblse if the query was not sent, true if so
     */
    public boolebn originateQuery(QueryRequest query, ManagedConnection mc) {
        if( query == null )
            throw new NullPointerException("null query");
        if( mc == null )
            throw new NullPointerException("null connection");
    
        // if this is b feature query & the other side doesn't
        // support it, then don't send it
        // This is bn optimization of network traffic, and doesn't
        // necessbrily need to exist.  We could be shooting ourselves
        // in the foot by not sending this, rendering Febture Searches
        // inoperbble for some users connected to bad Ultrapeers.
        if(query.isFebtureQuery() && !mc.getRemoteHostSupportsFeatureQueries())
            return fblse;
        
        mc.originbteQuery(query);
        return true;
    }
    
    /**
     * Respond to the ping request.  Implementbtions typically will either
     * do nothing (if they don't think b response is appropriate) or call
     * sendPingReply(PingReply).
     * This method is cblled from the default handlePingRequest.
     */
    protected bbstract void respondToPingRequest(PingRequest request,
                                                 ReplyHbndler handler);

	/**
	 * Responds to b ping received over UDP -- implementations
	 * hbndle this differently from pings received over TCP, as it is 
	 * bssumed that the requester only wants pongs from other nodes
	 * thbt also support UDP messaging.
	 *
	 * @pbram request the <tt>PingRequest</tt> to service
     * @pbram addr the <tt>InetSocketAddress</tt> containing the ping
     * @pbram handler the <tt>ReplyHandler</tt> instance from which the
     *  ping wbs received and to which pongs should be sent
	 */
    protected bbstract void respondToUDPPingRequest(PingRequest request, 
													InetSocketAddress bddr,
                                                    ReplyHbndler handler);


    /**
     * Respond to the query request.  Implementbtions typically will either
     * do nothing (if they don't think b response is appropriate) or call
     * sendQueryReply(QueryReply).
     * This method is cblled from the default handleQueryRequest.
     */
    protected bbstract boolean respondToQueryRequest(QueryRequest queryRequest,
                                                     byte[] clientGUID,
                                                     ReplyHbndler handler);

    /**
     * The defbult handler for PingRequests received in
     * MbnagedConnection.loopForMessages().  This implementation
     * uses the ping route tbble to route a ping reply.  If an appropriate route
     * doesn't exist, records the error stbtistics.  On sucessful routing,
     * the PingReply count is incremented.<p>
     *
     * In bll cases, the ping reply is recorded into the host catcher.<p>
     *
     * Override bs desired, but you probably want to call super.handlePingReply
     * if you do.
     */
    protected void hbndlePingReply(PingReply reply,
                                   ReplyHbndler handler) {
        //updbte hostcatcher (even if the reply isn't for me)
        boolebn newAddress = RouterService.getHostCatcher().add(reply);

        if(newAddress && !reply.isUDPHostCbche()) {
            PongCbcher.instance().addPong(reply);
        }

        //First route to originbtor in usual manner.
        ReplyHbndler replyHandler =
            _pingRouteTbble.getReplyHandler(reply.getGUID());

        if(replyHbndler != null) {
            replyHbndler.handlePingReply(reply, handler);
        }
        else {
            RouteErrorStbt.PING_REPLY_ROUTE_ERRORS.incrementStat();
            hbndler.countDroppedMessage();
        }
		boolebn supportsUnicast = reply.supportsUnicast();
        
        //Then, if b marked pong from an Ultrapeer that we've never seen before,
        //send to bll leaf connections except replyHandler (which may be null),
        //irregbrdless of GUID.  The leafs will add the address then drop the
        //pong bs they have no routing entry.  Note that if Ultrapeers are very
        //prevblent, this may consume too much bandwidth.
		//Also forwbrd any GUESS pongs to all leaves.
        if (newAddress && (reply.isUltrbpeer() || supportsUnicast)) {
            List list=_mbnager.getInitializedClientConnections();
            for (int i=0; i<list.size(); i++) {
                MbnagedConnection c = (ManagedConnection)list.get(i);
                Assert.thbt(c != null, "null c.");
                if (c!=hbndler && c!=replyHandler && c.allowNewPongs()) {
                    c.hbndlePingReply(reply, handler);
                }
            }
        }
    }

    /**
     * The defbult handler for QueryReplies received in
     * MbnagedConnection.loopForMessages().  This implementation
     * uses the query route tbble to route a query reply.  If an appropriate
     * route doesn't exist, records the error stbtistics.  On sucessful routing,
     * the QueryReply count is incremented.<p>
     *
     * Override bs desired, but you probably want to call super.handleQueryReply
     * if you do.  This is public for testing purposes.
     */
    public void hbndleQueryReply(QueryReply queryReply,
                                 ReplyHbndler handler) {
        if(queryReply == null) {
            throw new NullPointerException("null query reply");
        }
        if(hbndler == null) {
            throw new NullPointerException("null ReplyHbndler");
        }
        //For flow control rebsons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume hbve higher priorities (i.e., lower
        //numbers).
        RouteTbble.ReplyRoutePair rrp =
            _queryRouteTbble.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotblLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepbre a routing for a PushRequest, which works
            // here like b QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTbble.routeReply(queryReply.getClientGUID(),
                                       hbndler);
            //Simple flow control: don't route this messbge along other
            //connections if we've blready routed too many replies for this
            //GUID.  Note thbt replies destined for me all always delivered to
            //the GUI.

            ReplyHbndler rh = rrp.getReplyHandler();

            if(!shouldDropReply(rrp, rh, queryReply)) {                
                rh.hbndleQueryReply(queryReply, handler);
                // blso add to the QueryUnicaster for accounting - basically,
                // most results will not be relevbnt, but since it is a simple
                // HbshSet lookup, it isn't a prohibitive expense...
                UNICASTER.hbndleQueryReply(queryReply);

            } else {
				RouteErrorStbt.HARD_LIMIT_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
                finbl byte ttl = queryReply.getTTL();
                if (ttl < RouteErrorStbt.HARD_LIMIT_QUERY_REPLY_TTL.length)
				    RouteErrorStbt.HARD_LIMIT_QUERY_REPLY_TTL[ttl].incrementStat();
                else
				    RouteErrorStbt.HARD_LIMIT_QUERY_REPLY_TTL[RouteErrorStat.HARD_LIMIT_QUERY_REPLY_TTL.length-1].incrementStat();
                hbndler.countDroppedMessage();
            }
        }
        else {
			RouteErrorStbt.NO_ROUTE_QUERY_REPLY_ROUTE_ERRORS.incrementStat();
            hbndler.countDroppedMessage();
        }
    }

    /**
     * Checks if the <tt>QueryReply</tt> should be dropped for vbrious reasons.
     *
     * Rebson 1) The reply has already routed enough traffic.  Based on per-TTL
     * hbrd limits for the number of bytes routed for the given reply guid.
     * This blgorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits hbve more liberal limits than high TTL
     * hits.  This ensures thbt hits that are closer to the query originator
     * -- hits for which we've blready done most of the work, are not 
     * dropped unless we've routed b really large number of bytes for that
     * guid.  This method blso checks that hard number of results that have
     * been sent for this GUID.  If this number is grebter than a specified
     * limit, we simply drop the reply.
     *
     * Rebson 2) The reply was meant for me -- DO NOT DROP.
     *
     * Rebson 3) The TTL is 0, drop.
     *
     * @pbram rrp the <tt>ReplyRoutePair</tt> containing data about what's 
     *  been routed for this GUID
     * @pbram ttl the time to live of the query hit
     * @return <tt>true if the reply should be dropped, otherwise <tt>fblse</tt>
     */
    privbte boolean shouldDropReply(RouteTable.ReplyRoutePair rrp,
                                    ReplyHbndler rh,
                                    QueryReply qr) {
        int ttl = qr.getTTL();
                                           
        // Rebson 2 --  The reply is meant for me, do not drop it.
        if( rh == FOR_ME_REPLY_HANDLER ) return fblse;
        
        // Rebson 3 -- drop if TTL is 0.
        if( ttl == 0 ) return true;                

        // Rebson 1 ...
        
        int resultsRouted = rrp.getResultsRouted();

        // drop the reply if we've blready sent more than the specified number
        // of results for this GUID
        if(resultsRouted > 100) return true;

        int bytesRouted = rrp.getBytesRouted();
        // send replies with ttl bbove 2 if we've routed under 50K 
        if(ttl > 2 && bytesRouted < 50    * 1024) return fblse;
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && bytesRouted < 200 * 1024) return fblse;
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && bytesRouted < 100  * 1024) return fblse;

        // if none of the bbove conditions holds true, drop the reply
        return true;
    }

    privbte void handleGiveStats(final GiveStatsVendorMessage gsm, 
                                             finbl ReplyHandler replyHandler) {
        StbtisticVendorMessage statVM = null;
        try {
            //crebte the reply if we understand how
            if(StbtisticVendorMessage.isSupported(gsm)) {
                stbtVM = new StatisticVendorMessage(gsm);
                //OK. Now send this messbge back to the client that asked for
                //stbts
                replyHbndler.handleStatisticVM(statVM);
            }
        } cbtch(IOException iox) {
            return; //whbt can we really do now?
        }
    }

    privbte void handleStatisticsMessage(final StatisticVendorMessage svm, 
                                         finbl ReplyHandler handler) {
        if(StbtisticsSettings.RECORD_VM_STATS.getValue()) {
            Threbd statHandler = new ManagedThread("Stat writer ") {
                public void mbnagedRun() {
                    RbndomAccessFile file = null;
                    try {
                        file = new RbndomAccessFile("stats_log.log", "rw");
                        file.seek(file.length());//go to the end.
                        file.writeBytes(svm.getReportedStbts()+"\n");
                    } cbtch (IOException iox) {
                        ErrorService.error(iox);
                    } finblly {
                        if(file != null) {
                            try {
                                file.close();
                            } cbtch (IOException iox) {
                                ErrorService.error(iox);
                            }
                        }
                    }
                }
            };
            stbtHandler.start();
        }
    }

    /**
     *  If we get bnd SimppRequest, get the payload we need from the
     *  SimppMbnager and send the simpp bytes the the requestor in a SimppVM. 
     */
    privbte void handleSimppRequest(final SimppRequestVM simppReq, 
                                                  finbl ReplyHandler handler ) {
        if(simppReq.getVersion() > SimppRequestVM.VERSION)
            return; //we bre not going to deal with these types of requests. 
        byte[] simppBytes = SimppMbnager.instance().getSimppBytes();
        if(simppBytes != null) {
            SimppVM simppVM = new SimppVM(simppBytes);
            try {
                hbndler.handleSimppVM(simppVM);
            } cbtch(IOException iox) {//uanble to send the SimppVM. Nothing I can do
                return;
            }
        }
    }
    

    /**
     * Pbsses on the SimppVM to the SimppManager which will verify it and
     * mbke sure we it's newer than the one we know about, and then make changes
     * to the settings bs necessary, and cause new CapabilityVMs to be sent down
     * bll connections.
     */
    privbte void handleSimppVM(final SimppVM simppVM) {
        SimppMbnager.instance().checkAndUpdate(simppVM.getPayload());
    }

    /**
     *  Hbndles an update request by sending a response.
     */
    privbte void handleUpdateRequest(UpdateRequest req, ReplyHandler handler ) {

        byte[] dbta = UpdateHandler.instance().getLatestBytes();
        if(dbta != null) {
            UpdbteResponse msg = UpdateResponse.createUpdateResponse(data,req);
            hbndler.reply(msg);
        }
    }
    

    /**
     * Pbsses the request onto the update manager.
     */
    privbte void handleUpdateResponse(UpdateResponse resp, ReplyHandler handler) {
        UpdbteHandler.instance().handleNewData(resp.getUpdate());
    }

    /**
     * The defbult handler for PushRequests received in
     * MbnagedConnection.loopForMessages().  This implementation
     * uses the push route tbble to route a push request.  If an appropriate
     * route doesn't exist, records the error stbtistics.  On sucessful routing,
     * the PushRequest count is incremented.
     *
     * Override bs desired, but you probably want to call
     * super.hbndlePushRequest if you do.
     */
    protected void hbndlePushRequest(PushRequest request,
                                  ReplyHbndler handler) {
        if(request == null) {
            throw new NullPointerException("null request");
        }
        if(hbndler == null) {
            throw new NullPointerException("null ReplyHbndler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHbndler replyHandler = getPushHandler(request.getClientGUID());

        if(replyHbndler != null)
            replyHbndler.handlePushRequest(request, handler);
        else {
			RouteErrorStbt.PUSH_REQUEST_ROUTE_ERRORS.incrementStat();
            hbndler.countDroppedMessage();
        }
    }
    
    /**
     * Returns the bppropriate handler from the _pushRouteTable.
     * This enforces thbt requests for my clientGUID will return
     * FOR_ME_REPLY_HANDLER, even if it's not in the tbble.
     */
    protected ReplyHbndler getPushHandler(byte[] guid) {
        ReplyHbndler replyHandler = _pushRouteTable.getReplyHandler(guid);
        if(replyHbndler != null)
            return replyHbndler;
        else if(Arrbys.equals(_clientGUID, guid))
            return FOR_ME_REPLY_HANDLER;
        else
            return null;
    }

    /**
     * Uses the ping route tbble to send a PingReply to the appropriate
     * connection.  Since this is used for PingReplies orginbting here, no
     * stbts are updated.
     */
    protected void sendPingReply(PingReply pong, ReplyHbndler handler) {
        if(pong == null) {
            throw new NullPointerException("null pong");
        }

        if(hbndler == null) {
            throw new NullPointerException("null reply hbndler");
        }
 
        hbndler.handlePingReply(pong, null);
    }

    /**
     * Uses the query route tbble to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginbting here, no
     * stbts are updated.
     * @throws IOException if no bppropriate route exists.
     */
    protected void sendQueryReply(QueryReply queryReply)
        throws IOException {
        
        if(queryReply == null) {
            throw new NullPointerException("null reply");
        }
        //For flow control rebsons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume hbve higher priorities (i.e., lower
        //numbers).
        RouteTbble.ReplyRoutePair rrp =
            _queryRouteTbble.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotblLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            rrp.getReplyHbndler().handleQueryReply(queryReply, null);
        }
        else
            throw new IOException("no route for reply");
    }

    /**
     * Uses the push route tbble to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginbting here, no
     * stbts are updated.
     * @throws IOException if no bppropriate route exists.
     */
    public void sendPushRequest(PushRequest push)
        throws IOException {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        

        // Note the use of getClientGUID() here, not getGUID()
        ReplyHbndler replyHandler = getPushHandler(push.getClientGUID());

        if(replyHbndler != null)
            replyHbndler.handlePushRequest(push, FOR_ME_REPLY_HANDLER);
        else
            throw new IOException("no route for push");
    }
    
    /**
     * Sends b push request to the multicast network.  No lookups are
     * performed in the push route tbble, because the message will always
     * be brobdcast to everyone.
     */
    protected void sendMulticbstPushRequest(PushRequest push) {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        
        // must hbve a TTL of 1
        Assert.thbt(push.getTTL() == 1, "multicast push ttl not 1");
        
        MulticbstService.instance().send(push);
        SentMessbgeStatHandler.MULTICAST_PUSH_REQUESTS.addMessage(push);
    }


    /**
     * Converts the pbssed responses to QueryReplies. Each QueryReply can
     * bccomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in cbse the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt hbve any side effect, 
     * bnd does not modify the state of this object
     * @pbram responses The responses to be converted
     * @pbram queryRequest The query request corresponding to which we are
     * generbting query replies.
     * @return Iterbtor (on QueryReply) over the Query Replies
     */
    public Iterbtor responsesToQueryReplies(Response[] responses,
                                            QueryRequest queryRequest) {
        return responsesToQueryReplies(responses, queryRequest, 10);
    }


    /**
     * Converts the pbssed responses to QueryReplies. Each QueryReply can
     * bccomodate atmost 255 responses. Not all the responses may get included
     * in QueryReplies in cbse the query request came from a far away host.
     * <p>
     * NOTE: This method doesnt hbve any side effect, 
     * bnd does not modify the state of this object
     * @pbram responses The responses to be converted
     * @pbram queryRequest The query request corresponding to which we are
     * generbting query replies.
     * @pbram REPLY_LIMIT the maximum number of responses to have in each reply.
     * @return Iterbtor (on QueryReply) over the Query Replies
     */
    privbte Iterator responsesToQueryReplies(Response[] responses,
                                             QueryRequest queryRequest,
                                             finbl int REPLY_LIMIT) {
        //List to store Query Replies
        List /*<QueryReply>*/ queryReplies = new LinkedList();
        
        // get the bppropriate queryReply information
        byte[] guid = queryRequest.getGUID();
        byte ttl = (byte)(queryRequest.getHops() + 1);

		UplobdManager um = RouterService.getUploadManager();

        //Return mebsured speed if possible, or user's speed otherwise.
        long speed = um.mebsuredUploadSpeed();
        boolebn measuredSpeed=true;
        if (speed==-1) {
            speed=ConnectionSettings.CONNECTION_SPEED.getVblue();
            mebsuredSpeed=false;
        }

        int numResponses = responses.length;
        int index = 0;

        int numHops = queryRequest.getHops();

        // limit the responses if we're not delivering this 
        // out-of-bbnd and we have a lot of responses
        if(REPLY_LIMIT > 1 && 
           numHops > 2 && 
           numResponses > HIGH_HOPS_RESPONSE_LIMIT) {
            int j = 
                (int)(Mbth.random() * numResponses) % 
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
            int brraySize;
            // if there bre more than 255 responses,
            // crebte an array of 255 to send in the queryReply
            // otherwise, crebte an array of whatever size is left.
            if (numResponses < REPLY_LIMIT) {
                // brebk;
                brraySize = numResponses;
            }
            else
                brraySize = REPLY_LIMIT;

            Response[] res;
            // b special case.  in the common case where there
            // bre less than 256 responses being sent, there
            // is no need to copy one brray into another.
            if ( (index == 0) && (brraySize < REPLY_LIMIT) ) {
                res = responses;
            }
            else {
                res = new Response[brraySize];
                // copy the reponses into bite-size chunks
                for(int i =0; i < brraySize; i++) {
                    res[i] = responses[index];
                    index++;
                }
            }

            // decrement the number of responses we hbve left
            numResponses-= brraySize;

			// see if there bre any open slots
			boolebn busy = !um.isServiceable();
            boolebn uploaded = um.hadSuccesfulUpload();
			
            // We only wbnt to return a "reply to multicast query" QueryReply
            // if the request trbvelled a single hop.
			boolebn mcast = queryRequest.isMulticast() && 
                (queryRequest.getTTL() + queryRequest.getHops()) == 1;
			
            // We should mbrk our hits if the remote end can do a firewalled
            // trbnsfer AND so can we AND we don't accept tcp incoming AND our
            // externbl address is valid (needed for input into the reply)
            finbl boolean fwTransfer = 
                queryRequest.cbnDoFirewalledTransfer() && 
                UDPService.instbnce().canDoFWT() &&
                !RouterService.bcceptedIncomingConnection();
            
			if ( mcbst ) {
                ttl = 1; // not strictly necessbry, but nice.
            }
            
            List replies =
                crebteQueryReply(guid, ttl, speed, res, 
                                 _clientGUID, busy, uplobded, 
                                 mebsuredSpeed, mcast,
                                 fwTrbnsfer);

            //bdd to the list
            queryReplies.bddAll(replies);

        }//end of while
        
        return queryReplies.iterbtor();
    }

    /**
     * Abstrbct method for creating query hits.  Subclasses must specify
     * how this list is crebted.
     *
     * @return b <tt>List</tt> of <tt>QueryReply</tt> instances
     */
    protected bbstract List createQueryReply(byte[] guid, byte ttl,
                                            long speed, 
                                             Response[] res, byte[] clientGUID, 
                                             boolebn busy, 
                                             boolebn uploaded, 
                                             boolebn measuredSpeed, 
                                             boolebn isFromMcast,
                                             boolebn shouldMarkForFWTransfer);

    /**
     * Hbndles a message to reset the query route table for the given
     * connection.
     *
     * @pbram rtm the <tt>ResetTableMessage</tt> for resetting the query
     *  route tbble
     * @pbram mc the <tt>ManagedConnection</tt> for which the query route
     *  tbble should be reset
     */
    privbte void handleResetTableMessage(ResetTableMessage rtm,
                                         MbnagedConnection mc) {
        // if it's not from b leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // reset the query route tbble for this connection
        synchronized (mc.getQRPLock()) {
            mc.resetQueryRouteTbble(rtm);
        }

        // if this is coming from b leaf, make sure we update
        // our tbbles so that the dynamic querier has correct
        // dbta
        if(mc.isLebfConnection()) {
            _lbstQueryRouteTable = createRouteTable();
        }
    }

    /**
     * Hbndles a message to patch the query route table for the given
     * connection.
     *
     * @pbram rtm the <tt>PatchTableMessage</tt> for patching the query
     *  route tbble
     * @pbram mc the <tt>ManagedConnection</tt> for which the query route
     *  tbble should be patched
     */
    privbte void handlePatchTableMessage(PatchTableMessage ptm,
                                         MbnagedConnection mc) {
        // if it's not from b leaf or an Ultrapeer advertising 
        // QRP support, ignore it
        if(!isQRPConnection(mc)) return;

        // pbtch the query route table for this connection
        synchronized(mc.getQRPLock()) {
            mc.pbtchQueryRouteTable(ptm);
        }

        // if this is coming from b leaf, make sure we update
        // our tbbles so that the dynamic querier has correct
        // dbta
        if(mc.isLebfConnection()) {
            _lbstQueryRouteTable = createRouteTable();
        }
    }

    privbte void updateMessage(QueryRequest request, ReplyHandler handler) {
        if(! (hbndler instanceof Connection) )
            return;
        Connection c  = (Connection) hbndler;
        if(request.getHops()==1 && c.isOldLimeWire()) {
            if(StbticMessages.updateReply ==null) 
                return;
            QueryReply qr
                 = new QueryReply(request.getGUID(),StbticMessages.updateReply);
            try {
                sendQueryReply(qr);
            } cbtch (IOException ignored) {}
        }
    }

    /**
     * Utility method for checking whether or not the given connection
     * is bble to pass QRP messages.
     *
     * @pbram c the <tt>Connection</tt> to check
     * @return <tt>true</tt> if this is b QRP-enabled connection,
     *  otherwise <tt>fblse</tt>
     */
    privbte static boolean isQRPConnection(Connection c) {
        if(c.isSupernodeClientConnection()) return true;
        if(c.isUltrbpeerQueryRoutingConnection()) return true;
        return fblse;
    }

    /** Threbd the processing of QRP Table delivery. */
    privbte class QRPPropagator extends ManagedThread {
        public QRPPropbgator() {
            setNbme("QRPPropagator");
            setDbemon(true);
        }

        /** While the connection is not closed, sends bll data delay. */
        public void mbnagedRun() {
            try {
                while (true) {
					// Check for bny scheduled QRP table propagations
					// every 10 seconds
                    Threbd.sleep(10*1000);
    				forwbrdQueryRouteTables();
                }
            } cbtch(Throwable t) {
                ErrorService.error(t);
            }
        }

    } //end QRPPropbgator


    /**
     * Sends updbted query routing tables to all connections which haven't
     * been updbted in a while.  You can call this method as often as you want;
     * it tbkes care of throttling.
     *     @modifies connections
     */    
    privbte void forwardQueryRouteTables() {
		//Check the time to decide if it needs bn update.
		long time = System.currentTimeMillis();

		//For bll connections to new hosts c needing an update...
		List list=_mbnager.getInitializedConnections();
		QueryRouteTbble table = null;
		List /* of RouteTbbleMessage */ patches = null;
		QueryRouteTbble lastSent = null;
		
		for(int i=0; i<list.size(); i++) {                        
			MbnagedConnection c=(ManagedConnection)list.get(i);
			

			// continue if I'm bn Ultrapeer and the node on the
			// other end doesn't support Ultrbpeer-level query
			// routing
			if(RouterService.isSupernode()) { 
				// only skip it if it's not bn Ultrapeer query routing
				// connection
				if(!c.isUltrbpeerQueryRoutingConnection()) { 
					continue;
				}
			} 				
			// otherwise, I'm b leaf, and don't send routing
			// tbbles if it's not a connection to an Ultrapeer
			// or if query routing is not enbbled on the connection
			else if (!(c.isClientSupernodeConnection() && 
					   c.isQueryRoutingEnbbled())) {
				continue;
			}
			
			// See if it is time for this connections QRP updbte
			// This cbll is safe since only this thread updates time
			if (time<c.getNextQRPForwbrdTime())
				continue;

			c.incrementNextQRPForwbrdTime(time);
				
			// Crebte a new query route table if we need to
			if (tbble == null) {
				tbble = createRouteTable();     //  Ignores busy leaves
                _lbstQueryRouteTable = table;
			} 

			//..bnd send each piece.
			
			// Becbuse we tend to send the same list of patches to lots of
			// Connections, we cbn reuse the list of RouteTableMessages
			// between those connections if their lbst sent
			// tbble is exactly the same.
			// This bllows us to only reduce the amount of times we have
			// to cbll encode.
			
			//  (This if works for 'null' sent tbbles too)
			if( lbstSent == c.getQueryRouteTableSent() ) {
			    // if we hbve not constructed the patches yet, then do so.
			    if( pbtches == null )
			        pbtches = table.encode(lastSent, true);
			}
			// If they bren't the same, we have to encode a new set of
			// pbtches for this connection.
			else {
			    lbstSent = c.getQueryRouteTableSent();
			    pbtches = table.encode(lastSent, true);
            }
            
            // If sending QRP tbbles is turned off, don't send them.  
            if(!ConnectionSettings.SEND_QRP.getVblue()) {
                return;
            }
            
		    for(Iterbtor iter = patches.iterator(); iter.hasNext();) {
		        c.send((RouteTbbleMessage)iter.next());
    	    }
    	    
            c.setQueryRouteTbbleSent(table);
		}
    }

    /**
     * Accessor for the most recently cblculated <tt>QueryRouteTable</tt>
     * for this node.  If this node is bn Ultrapeer, the table will include
     * bll data for leaf nodes in addition to data for this node's files.
     *
     * @return the <tt>QueryRouteTbble</tt> for this node
     */
    public QueryRouteTbble getQueryRouteTable() {
        return _lbstQueryRouteTable;
    }

    /**
     * Crebtes a query route table appropriate for forwarding to connection c.
     * This will not include informbtion from c.
     *     @requires queryUpdbteLock held
     */
    privbte static QueryRouteTable createRouteTable() {
        QueryRouteTbble ret = _fileManager.getQRT();
        
        // Add lebves' files if we're an Ultrapeer.
        if(RouterService.isSupernode()) {
            bddQueryRoutingEntriesForLeaves(ret);
        }
        return ret;
    }


	/**
	 * Adds bll query routing tables of leaves to the query routing table for
	 * this node for propbgation to other Ultrapeers at 1 hop.
	 * 
	 * Added "busy lebf" support to prevent a busy leaf from having its QRT
	 * 	tbble added to the Ultrapeer's last-hop QRT table.  This should reduce
	 *  BW costs for UPs with busy lebves.  
	 *
	 * @pbram qrt the <tt>QueryRouteTable</tt> to add to
	 */
	privbte static void addQueryRoutingEntriesForLeaves(QueryRouteTable qrt) {
		List lebves = _manager.getInitializedClientConnections();
		
		for(int i=0; i<lebves.size(); i++) {
			MbnagedConnection mc = (ManagedConnection)leaves.get(i);
        	synchronized (mc.getQRPLock()) {
        	    //	Don't include busy lebves
        	    if( !mc.isBusyLebf() ){
                	QueryRouteTbble qrtr = mc.getQueryRouteTableReceived();
					if(qrtr != null) {
						qrt.bddAll(qrtr);
					}
        	    }
			}
		}
	}

    
    /**
     * Adds the specified MessbgeListener for messages with this GUID.
     * You must mbnually unregister the listener.
     *
     * This works by replbcing the necessary maps & lists, so that 
     * notifying doesn't hbve to hold any locks.
     */
    public void registerMessbgeListener(byte[] guid, MessageListener ml) {
        ml.registered(guid);
        synchronized(MESSAGE_LISTENER_LOCK) {
            Mbp listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
            listeners.putAll(_messbgeListeners);
            List bll = (List)listeners.get(guid);
            if(bll == null) {
                bll = new ArrayList(1);
                bll.add(ml);
            } else {
                List temp = new ArrbyList(all.size() + 1);
                temp.bddAll(all);
                bll = temp;
                bll.add(ml);
            }
            listeners.put(guid, Collections.unmodifibbleList(all));
            _messbgeListeners = Collections.unmodifiableMap(listeners);
        }
    }
    
    /**
     * Unregisters this MessbgeListener from listening to the GUID.
     *
     * This works by replbcing the necessary maps & lists so that
     * notifying doesn't hbve to hold any locks.
     */
    public void unregisterMessbgeListener(byte[] guid, MessageListener ml) {
        boolebn removed = false;
        synchronized(MESSAGE_LISTENER_LOCK) {
            List bll = (List)_messageListeners.get(guid);
            if(bll != null) {
                bll = new ArrayList(all);
                if(bll.remove(ml)) {
                    removed = true;
                    Mbp listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
                    listeners.putAll(_messbgeListeners);
                    if(bll.isEmpty())
                        listeners.remove(guid);
                    else
                        listeners.put(guid, Collections.unmodifibbleList(all));
                    _messbgeListeners = Collections.unmodifiableMap(listeners);
                }
            }
        }
        if(removed)
            ml.unregistered(guid);
    }


    /**
     * responds to b request for the list of ultrapeers or leaves.  It is sent right back to the
     * requestor on the UDP receiver threbd.
     * @pbram msg the request message
     * @pbram handler the UDPHandler to send it to.
     */
    privbte void handleUDPCrawlerPing(UDPCrawlerPing msg, ReplyHandler handler){
    	
    	//mbke sure the same person doesn't request too often
    	//note: this should only hbppen on the UDP receiver thread, that's why
    	//I'm not locking it.
    	if (!_promotionMbnager.allowUDPPing(handler))
    		return; 
    	UDPCrbwlerPong newMsg = new UDPCrawlerPong(msg);
    	hbndler.reply(newMsg);
    }
    
    /**
     * Replies to b head ping sent from the given ReplyHandler.
     */
    privbte void handleHeadPing(HeadPing ping, ReplyHandler handler) {
        if (DownlobdSettings.DROP_HEADPINGS.getValue())
            return;
        
        GUID clientGUID = ping.getClientGuid();
        ReplyHbndler pingee;
        
        if(clientGUID != null)
            pingee = getPushHbndler(clientGUID.bytes());
        else
            pingee = FOR_ME_REPLY_HANDLER; // hbndle ourselves.
        
        //drop the ping if no entry for the given clientGUID
        if (pingee == null) 
           return; 
        
        //don't bother routing if this is intended for me. 
        // TODO:  Clebn up ReplyHandler interface so we aren't
        //        bfraid to use it like it's intended.
        //        Thbt way, we can do pingee.handleHeadPing(ping)
        //        bnd not need this anti-OO instanceof check.
        if (pingee instbnceof ForMeReplyHandler) {
            // If it's for me, reply directly to the person who sent it.
            HebdPong pong = new HeadPong(ping);
            hbndler.reply(pong); // 
        } else {
            // Otherwise, remember who sent it bnd forward it on.
            //remember where to send the pong to. 
            //the pong will hbve the same GUID as the ping. 
            // Note thbt this uses the messageGUID, not the clientGUID
            _hebdPongRouteTable.routeReply(ping.getGUID(), handler); 
            
            //bnd send off the routed ping 
            if ( !(hbndler instanceof Connection) ||
                    ((Connection)hbndler).supportsVMRouting())
                pingee.reply(ping);
            else
                pingee.reply(new HebdPing(ping)); 
        }
   } 
    
    
    /** 
     * Hbndles a pong received from the given handler.
     */ 
    privbte void handleHeadPong(HeadPong pong, ReplyHandler handler) { 
        ReplyHbndler forwardTo =  _headPongRouteTable.getReplyHandler(pong.getGUID()); 

        // TODO: Clebn up ReplyHandler interface so we're not afraid
        //       to use it correctly.
        //       Ideblly, we'd do forwardTo.handleHeadPong(pong)
        //       instebd of this instanceof check
         
        // if this pong is for me, process it bs usual (not implemented yet)
        if (forwbrdTo != null && !(forwardTo instanceof ForMeReplyHandler)) { 
            forwbrdTo.reply(pong); 
            _hebdPongRouteTable.removeReplyHandler(forwardTo); 
        } 
    } 
    
    
    privbte static class QueryResponseBundle {
        public finbl QueryRequest _query;
        public finbl Response[] _responses;
        
        public QueryResponseBundle(QueryRequest query, Response[] responses) {
            _query = query;
            _responses = responses;
        }
    }


    /** Cbn be run to invalidate out-of-band ACKs that we are waiting for....
     */
    privbte class Expirer implements Runnable {
        public void run() {
            try {
                Set toRemove = new HbshSet();
                synchronized (_outOfBbndReplies) {
                    Iterbtor keys = _outOfBandReplies.keySet().iterator();
                    while (keys.hbsNext()) {
                        GUID.TimedGUID currQB = (GUID.TimedGUID) keys.next();
                        if ((currQB != null) && (currQB.shouldExpire()))
                            toRemove.bdd(currQB);
                    }
                    // done iterbting through _outOfBandReplies, remove the 
                    // keys now...
                    keys = toRemove.iterbtor();
                    while (keys.hbsNext())
                        _outOfBbndReplies.remove(keys.next());
                }
            } 
            cbtch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }


    /** This is run to clebr out the registry of connect back attempts...
     *  Mbde package access for easy test access.
     */
    stbtic class ConnectBackExpirer implements Runnable {
        public void run() {
            try {
                _tcpConnectBbcks.clear();
                _udpConnectBbcks.clear();
            } 
            cbtch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

    stbtic class HopsFlowManager implements Runnable {
        /* in cbse we don't want any queries any more */
        privbte static final byte BUSY_HOPS_FLOW = 0;

    	/* in cbse we want to reenable queries */
    	privbte static final byte FREE_HOPS_FLOW = 5;

        /* smbll optimization:
           send only HopsFlowVendorMessbges if the busy state changed */
        privbte static boolean _oldBusyState = false;
           
        public void run() {
            // only lebfs should use HopsFlow
            if (RouterService.isSupernode())
                return;
            // busy hosts don't wbnt to receive any queries, if this node is not
            // busy, we need to reset the HopsFlow vblue
            boolebn isBusy = !RouterService.getUploadManager().isServiceable();
            
            // stbte changed? don't bother the ultrapeer with information
            // thbt it already knows. we need to inform new ultrapeers, though.
            finbl List connections = _manager.getInitializedConnections();
            finbl HopsFlowVendorMessage hops = 
                new HopsFlowVendorMessbge(isBusy ? BUSY_HOPS_FLOW :
                                          FREE_HOPS_FLOW);
            if (isBusy == _oldBusyStbte) {
                for (int i = 0; i < connections.size(); i++) {
                    MbnagedConnection c =
                        (MbnagedConnection)connections.get(i);
                    // Yes, we mby tell a new ultrapeer twice, but
                    // without b buffer of some kind, we might forget
                    // some ultrbpeers. The clean solution would be
                    // to remember the hops-flow vblue in the connection.
                    if (c != null 
                        && c.getConnectionTime() + 1.25 * HOPS_FLOW_INTERVAL 
                            > System.currentTimeMillis()
                        && c.isClientSupernodeConnection() )
                        c.send(hops);
                }
            } else { 
                _oldBusyStbte = isBusy;
                for (int i = 0; i < connections.size(); i++) {
                    MbnagedConnection c = (ManagedConnection)connections.get(i);
                    if (c != null && c.isClientSupernodeConnection())
                        c.send(hops);
                }
            }
        }
    }
}
