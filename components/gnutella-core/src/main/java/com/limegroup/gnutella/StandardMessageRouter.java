package com.limegroup.gnutella;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.LimeACKHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.Message.MessageCounter;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This class is the message routing implementation for TCP messages.
 */
@Singleton
public class StandardMessageRouter extends MessageRouterImpl {
    
    private static final Log LOG = LogFactory.getLog(StandardMessageRouter.class);
    
    private final Statistics statistics;

    private final ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;
    
    @InspectionPoint("false positive queries")
    private final MessageCounter falsePositives = new Message.MessageCounter(50);
    @InspectionPoint("not serviced queries")
    private final MessageCounter notServiced = new Message.MessageCounter(500);
    @InspectionPoint("ignored busy queries")
    private final MessageCounter ignoredBusy = new Message.MessageCounter(500);
    
    @Inject
    public StandardMessageRouter(NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            QueryHandlerFactory queryHandlerFactory,
            OnDemandUnicaster onDemandUnicaster,
            HeadPongFactory headPongFactory, PingReplyFactory pingReplyFactory,
            ConnectionManager connectionManager, @Named("forMeReplyHandler")
            ReplyHandler forMeReplyHandler, QueryUnicaster queryUnicaster,
            FileManager fileManager, ContentManager contentManager,
            DHTManager dhtManager, UploadManager uploadManager,
            DownloadManager downloadManager, UDPService udpService,
            SearchResultHandler searchResultHandler,
            SocketsManager socketsManager, HostCatcher hostCatcher,
            QueryReplyFactory queryReplyFactory, StaticMessages staticMessages,
            Provider<MessageDispatcher> messageDispatcher,
            MulticastService multicastService, QueryDispatcher queryDispatcher,
            Provider<ActivityCallback> activityCallback,
            ConnectionServices connectionServices,
            ApplicationServices applicationServices,
            @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor,
            Provider<PongCacher> pongCacher,
            Provider<SimppManager> simppManager,
            Provider<UpdateHandler> updateHandler,
            GuidMapManager guidMapManager, 
            UDPReplyHandlerCache udpReplyHandlerCache,
            Provider<InspectionRequestHandler> inspectionRequestHandlerFactory,
            Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
            Statistics statistics,
            ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
            PingRequestFactory pingRequestFactory, MessageHandlerBinder messageHandlerBinder,
            Provider<OOBHandler> oobHandlerFactory,
            Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager,
            Provider<LimeACKHandler> limeACKHandler) {
        super(networkManager, queryRequestFactory, queryHandlerFactory,
                onDemandUnicaster, headPongFactory, pingReplyFactory,
                connectionManager, forMeReplyHandler, queryUnicaster,
                fileManager, contentManager, dhtManager, uploadManager,
                downloadManager, udpService, searchResultHandler,
                socketsManager, hostCatcher, queryReplyFactory, staticMessages,
                messageDispatcher, multicastService, queryDispatcher,
                activityCallback, connectionServices, applicationServices,
                backgroundExecutor, pongCacher, simppManager, updateHandler,
                guidMapManager, udpReplyHandlerCache, inspectionRequestHandlerFactory, 
                udpCrawlerPingHandlerFactory, 
                pingRequestFactory, messageHandlerBinder, oobHandlerFactory, 
                MACCalculatorRepositoryManager, limeACKHandler);
        this.statistics = statistics;
        this.replyNumberVendorMessageFactory = replyNumberVendorMessageFactory;
    }
    
    /**
     * Responds to a Gnutella ping with cached pongs. This does special handling
     * for both "heartbeat" pings that were sent to ensure that the connection
     * is still live as well as for pings from a crawler.
     * 
     * @param ping the <tt>PingRequest</tt> to respond to
     * @param handler the <tt>ReplyHandler</tt> to send any pongs to
     */
    @Override
    protected void respondToPingRequest(PingRequest ping,
                                        ReplyHandler handler) {
        //If this wasn't a handshake or crawler ping, check if we can accept
        //incoming connection for old-style unrouted connections, ultrapeers, or
        //leaves.  TODO: does this mean leaves always respond to pings?
        int hops = ping.getHops();
        int ttl = ping.getTTL();
        if (   (hops+ttl > 2) 
            && !connectionManager.allowAnyConnection())
            return;
            
        // Only send pongs for ourself if we have a valid address & port.
        if(NetworkUtils.isValidAddress(networkManager.getAddress()) &&
           NetworkUtils.isValidPort(networkManager.getPort())) {    
            //SPECIAL CASE: for crawler ping
            // TODO:: this means that we can never send TTL=2 pings without
            // them being interpreted as from the crawler!!
            if(hops ==1 && ttl==1) {
                handleCrawlerPing(ping, handler);
                return;
                //Note that the while handling crawler ping, we dont send our
                //own pong, as that is unnecessary, since crawler already has
                //our address.
            }
    
            // handle heartbeat pings specially -- bypass pong caching code
            if(ping.isHeartbeat()) {
                sendPingReply(pingReplyFactory.create(ping.getGUID(), (byte)1), 
                    handler);
                return;
            }
    
            //send its own ping in all the cases
            int newTTL = hops+1;
            if ( (hops+ttl) <=2)
                newTTL = 1;        
    
            // send our own pong if we have free slots or if our average
            // daily uptime is more than 1/2 hour
            if(connectionManager.hasFreeSlots()  ||
               statistics.calculateDailyUptime() > 60*30) {
                PingReply pr = 
                    pingReplyFactory.create(ping.getGUID(), (byte)newTTL);
                
                sendPingReply(pr, handler);
            }
        }
        
        List<PingReply> pongs = pongCacher.get().getBestPongs(ping.getLocale());
        byte[] guid = ping.getGUID();
        InetAddress pingerIP = handler.getInetAddress();
        for(PingReply pr : pongs) {
            if(pr.getInetAddress().equals(pingerIP))
                continue;
            sendPingReply(pingReplyFactory.mutateGUID(pr, guid), handler);
        }
    }

	/**
	 * Responds to a ping request received over a UDP port.  This is
	 * handled differently from all other ping requests.  Instead of
	 * responding with cached pongs, we respond with a pong from our node.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param addr the <tt>InetSocketAddress</tt> containing the IP
     *  and port of the client node
     * @param handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
	 */
    @Override
	protected void respondToUDPPingRequest(PingRequest request, 
										   InetSocketAddress addr,
                                           ReplyHandler handler) {
        if(!networkManager.isIpPortValid())
            return;
        
        IpPort ipport = null;
        if (request.requestsIP()) {
            ipport = new IpPortImpl(addr);
        }
        
        List<IpPort> dhthosts = Collections.emptyList();
        int maxHosts = ConnectionSettings.NUM_RETURN_PONGS.getValue();
        
        if (request.requestsDHTIPP() && dhtManager.isRunning()) {
            dhthosts = dhtManager.getActiveDHTNodes(maxHosts);
        }
        
        int numDHTHosts = dhthosts.size();
        
        byte[] data = request.getSupportsCachedPongData();
        Collection<IpPort> gnuthosts = Collections.emptyList();
        if(data != null){
            boolean isUltrapeer =
                data.length >= 1 && 
                (data[0] & PingRequest.SCP_ULTRAPEER_OR_LEAF_MASK) ==
                    PingRequest.SCP_ULTRAPEER;
                
            int dhtFraction = ConnectionSettings.DHT_TO_GNUT_HOSTS_PONG.getValue();
            int maxDHTHosts = Math.round(((float)dhtFraction/100)*maxHosts);
            
            gnuthosts = connectionServices.getPreferencedHosts(
                        isUltrapeer, 
                        request.getLocale(),
                        maxHosts - Math.min(numDHTHosts, maxDHTHosts));
            //remove extra dht hosts
            dhthosts = dhthosts.subList(0, Math.min(numDHTHosts,maxHosts - gnuthosts.size()));
        } 
        
        PingReply reply;
    	if (ipport != null)
    	    reply = pingReplyFactory.create(request.getGUID(), (byte)1, ipport, gnuthosts, dhthosts);
    	else
    	    reply = pingReplyFactory.create(request.getGUID(), (byte)1, gnuthosts, dhthosts);
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Responding to UDPPingRequest "+(request.requestsDHTIPP()?"with DHTIPP ":"") +
                    "from : "+ addr + " with Gnutella hosts: \n"+ gnuthosts 
                    + "\n and DHT hosts: \n" + dhthosts);
        }
        
        sendPingReply(reply, handler);
        
	}

    /**
     * Handles the crawler ping of Hops=0 & TTL=2, by sending pongs 
     * corresponding to all its leaves
     * @param m The ping request received
     * @param handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
     */
    private void handleCrawlerPing(PingRequest m, ReplyHandler handler) {
        //TODO: why is this any different than the standard pong?  In other
        //words, why no ultrapong marking, proper address calculation, etc?
        
        //send the pongs for leaves
        List<RoutedConnection> leafConnections = connectionManager.getInitializedClientConnections();
        
        for(RoutedConnection connection : leafConnections) {
            //create the pong for this connection
            PingReply pr = 
                pingReplyFactory.createExternal(m.getGUID(), (byte)2, 
                                         connection.getPort(),
                                         connection.getInetAddress().getAddress(),
                                         false);
                                                    
            
            //hop the message, as it is ideally coming from the connected host
            pr.hop();
            
            sendPingReply(pr, handler);
        }
        
        //pongs for the neighbors will be sent by neighbors themselves
        //as ping will be broadcasted to them (since TTL=2)        
    }
    
    @Override
    protected boolean respondToQueryRequest(QueryRequest queryRequest,
                                            byte[] clientGUID,
                                            ReplyHandler handler) {
        //Only respond if we understand the actual feature, if it had a feature.
        if(!FeatureSearchData.supportsFeature(queryRequest.getFeatureSelector()))
            return false;
                                                
        // Only send results if we're not busy.  Note that this ignores
        // queue slots -- we're considered busy if all of our "normal"
        // slots are full.  This allows some spillover into our queue that
        // is necessary because we're always returning more total hits than
        // we have slots available.
        if(!uploadManager.mayBeServiceable() )  {
            ignoredBusy.countMessage(queryRequest);
            return false;
        }
                                                
                                                
        // Ensure that we have a valid IP & Port before we send the response.
        // Otherwise the QueryReply will fail on creation.
        if( !NetworkUtils.isValidPort(networkManager.getPort()) ||
            !NetworkUtils.isValidAddress(networkManager.getAddress()))
            return false;
                                                     
        // Run the local query
        Response[] responses = fileManager.query(queryRequest);
        if (responses.length == 0)
            falsePositives.countMessage(queryRequest);
        return sendResponses(responses, queryRequest, handler);
        
    }

    private boolean sendResponses(Response[] responses, QueryRequest query,
                                 ReplyHandler handler) {
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return false;

        // if we cannot service a regular query, only send back results for
        // application-shared metafiles, if any.
        if (!uploadManager.isServiceable()) {
        	
        	List<Response> filtered = new ArrayList<Response>(responses.length);
        	for(Response r : responses) {
        		if (r.isMetaFile() && 
        				fileManager.isFileApplicationShared(r.getName()))
        			filtered.add(r);
        	}
        	
        	if (filtered.isEmpty()) {// nothing to send..
        	    notServiced.countMessage(query);
        		return false;
        	}
        	
        	if (filtered.size() != responses.length)
        		responses = filtered.toArray(new Response[filtered.size()]);
        }
        
        // Here we can do a couple of things - if the query wants
        // out-of-band replies we should do things differently.  else just
        // send it off as usual.  only send out-of-band if you can
        // receive solicited udp AND not servicing too many
        // uploads AND not connected to the originator of the query
        if (query.desiresOutOfBandReplies() &&
            !isConnectedTo(query, handler) && 
			networkManager.canReceiveSolicited() &&
            NetworkUtils.isValidAddressAndPort(query.getReplyAddress(), query.getReplyPort())) {
            
            // send the replies out-of-band - we need to
            // 1) buffer the responses
            // 2) send a ReplyNumberVM with the number of responses
            if (limeAckHandler.get().bufferResponsesForLaterDelivery(query, responses)) {
                // special out of band handling....
                InetAddress addr = null;
                try {
                    addr = InetAddress.getByName(query.getReplyAddress());
                } catch (UnknownHostException uhe) {}
                final int port = query.getReplyPort();
                
                if(addr != null) { 
                    // send a ReplyNumberVM to the host - he'll ACK you if he
                    // wants the whole shebang
                    int resultCount = 
                        (responses.length > 255) ? 255 : responses.length;
                    final ReplyNumberVendorMessage vm = query.desiresOutOfBandRepliesV3() ?
                            replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(query.getGUID()), resultCount) :
                                replyNumberVendorMessageFactory.createV2ReplyNumberVendorMessage(new GUID(query.getGUID()), resultCount);
                    udpService.send(vm, addr, port);
                    if (MessageSettings.OOB_REDUNDANCY.getValue() && 
                            query.desiresOutOfBandRepliesV3()) {
                        final InetAddress addrf = addr;
                        backgroundExecutor.schedule(new Runnable() {
                            public void run () {
                                udpService.send(vm, addrf, port);
                            }
                        }, 100, TimeUnit.MILLISECONDS);
                    }
                    return true;
                }
            } else {
                // else i couldn't buffer the responses due to busy-ness, oh, scrap
                // them.....
                return false;                
            }
        }

        // send the replies in-band
        // -----------------------------

        //convert responses to QueryReplies
        Iterable<QueryReply> iterable = responsesToQueryReplies(responses,
                                                                  query);
        //send the query replies
        try {
            for(QueryReply queryReply : iterable)
                sendQueryReply(queryReply);
        }  catch (IOException e) {
            // if there is an error, do nothing..
        }
        // -----------------------------
        
        return true;

    }

    /** Returns whether or not we are connected to the originator of this query.
     *  PRE: assumes query.desiresOutOfBandReplies == true
     */
    private final boolean isConnectedTo(QueryRequest query, 
                                        ReplyHandler handler) {
        return query.matchesReplyAddress(handler.getInetAddress().getAddress());
    }

    /** 
     * Creates a <tt>List</tt> of <tt>QueryReply</tt> instances with
     * compressed XML data, if requested.
     *
     * @return a new <tt>List</tt> of <tt>QueryReply</tt> instances
     */
    @Override
    protected List<QueryReply> createQueryReply(byte[] guid, byte ttl,
                                    long speed, Response[] res,
                                    byte[] clientGUID, 
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean isFromMcast,
                                    boolean isFWTransfer,
                                    SecurityToken securityToken) {
        
        List<QueryReply> queryReplies = new ArrayList<QueryReply>();
        QueryReply queryReply = null;

        // pick the right address & port depending on multicast & fwtrans
        // if we cannot find a valid address & port, exit early.
        int port = -1;
        byte[] ip = null;
        // first try using multicast addresses & ports, but if they're
        // invalid, fallback to non multicast.
        if(isFromMcast) {
            ip = networkManager.getNonForcedAddress();
            port = networkManager.getNonForcedPort();
            if(!NetworkUtils.isValidPort(port) ||
               !NetworkUtils.isValidAddress(ip))
                isFromMcast = false;
        }
        
        if(!isFromMcast) {
            
            // see if we have a valid FWTrans address.  if not, fall back.
            if(isFWTransfer) {
                port = udpService.getStableUDPPort();
                ip = networkManager.getExternalAddress();
                if(!NetworkUtils.isValidAddress(ip) 
                        || !NetworkUtils.isValidPort(port))
                    isFWTransfer = false;
            }
            
            // if we still don't have a valid address here, exit early.
            if(!isFWTransfer) {
                ip = networkManager.getAddress();
                port = networkManager.getPort();
                if(!NetworkUtils.isValidAddress(ip) ||
                        !NetworkUtils.isValidPort(port))
                    return Collections.emptyList();
            }
        }
        
        // get the xml collection string...
        String xmlCollectionString = 
        LimeXMLDocumentHelper.getAggregateString(res);
        if (xmlCollectionString == null)
            xmlCollectionString = "";

        byte[] xmlBytes = null;
        try {
            xmlBytes = xmlCollectionString.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ueex) {
            throw new IllegalStateException(ueex);
        }
        
        // get the *latest* push proxies if we have not accepted an incoming
        // connection in this session
        boolean notIncoming = !networkManager.acceptedIncomingConnection();
        Set<? extends IpPort> proxies = notIncoming ? connectionManager.getPushProxies() : null;
        
        // it may be too big....
        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {
            // ok, need to partition responses up once again and send out
            // multiple query replies.....
            List<Response[]> splitResps = new LinkedList<Response[]>();
            splitAndAddResponses(splitResps, res);

            while (!splitResps.isEmpty()) {
                Response[] currResps = splitResps.remove(0);
                String currXML = LimeXMLDocumentHelper.getAggregateString(currResps);
                byte[] currXMLBytes = null;
                try {
                    currXMLBytes = currXML.getBytes("UTF-8");
                } catch(UnsupportedEncodingException ueex) {
                    throw new IllegalStateException(ueex);
                }
                if ((currXMLBytes.length > QueryReply.XML_MAX_SIZE) &&
                                                        (currResps.length > 1)) 
                    splitAndAddResponses(splitResps, currResps);
                else {
                    // create xml bytes if possible...
                    byte[] xmlCompressed = null;
                    if (!currXML.equals(""))
                        xmlCompressed = LimeXMLUtils.compress(currXMLBytes);
                    else //there is no XML
                        xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
                    
                    // create the new queryReply
                    queryReply = queryReplyFactory.createQueryReply(guid, ttl,
                            port, ip, speed, currResps, _clientGUID,
                            xmlCompressed, notIncoming, busy, uploaded, measuredSpeed,
                            ChatSettings.CHAT_ENABLED.getValue(), isFromMcast, isFWTransfer, proxies, securityToken);
                    queryReplies.add(queryReply);
                }
            }

        }
        else {  // xml is small enough, no problem.....
            // get xml bytes if possible....
            byte[] xmlCompressed = null;
            if (!xmlCollectionString.equals(""))
                xmlCompressed = 
                    LimeXMLUtils.compress(xmlBytes);
            else //there is no XML
                xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
            
            // create the new queryReply
            queryReply = queryReplyFactory.createQueryReply(guid, ttl, port,
                    ip, speed, res, _clientGUID, xmlCompressed, notIncoming,
                    busy, uploaded, measuredSpeed, ChatSettings.CHAT_ENABLED.getValue(), isFromMcast, isFWTransfer,
                    proxies, securityToken);
            queryReplies.add(queryReply);
        }

        return queryReplies;
    }
    

    
    /** @return Simply splits the input array into two (almost) equally sized
     *  arrays.
     */
    private Response[][] splitResponses(Response[] in) {
        int middle = in.length/2;
        Response[][] retResps = new Response[2][];
        retResps[0] = new Response[middle];
        retResps[1] = new Response[in.length-middle];
        for (int i = 0; i < middle; i++)
            retResps[0][i] = in[i];
        for (int i = 0; i < (in.length-middle); i++)
            retResps[1][i] = in[i+middle];
        return retResps;
    }

    private void splitAndAddResponses(List<Response[]> addTo, Response[] toSplit) {
        Response[][] splits = splitResponses(toSplit);
        addTo.add(splits[0]);
        addTo.add(splits[1]);
    }

    
}
