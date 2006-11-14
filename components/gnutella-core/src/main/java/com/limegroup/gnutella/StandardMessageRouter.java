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

import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.IPPortCombo;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.ReceivedMessageStat;
import com.limegroup.gnutella.statistics.RoutedQueryStat;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This class is the message routing implementation for TCP messages.
 */
public class StandardMessageRouter extends MessageRouter {
    /**
     * Responds to a Gnutella ping with cached pongs.  This does special 
     * handling for both "heartbeat" pings that were sent to ensure that
     * the connection is still live as well as for pings from a crawler.
     *
     * @param ping the <tt>PingRequest</tt> to respond to
     * @param handler the <tt>ReplyHandler</tt> to send any pongs to
     */
    protected void respondToPingRequest(PingRequest ping,
                                        ReplyHandler handler) {
        //If this wasn't a handshake or crawler ping, check if we can accept
        //incoming connection for old-style unrouted connections, ultrapeers, or
        //leaves.  TODO: does this mean leaves always respond to pings?
        int hops = ping.getHops();
        int ttl = ping.getTTL();
        if (   (hops+ttl > 2) 
            && !_manager.allowAnyConnection())
            return;
            
        // Only send pongs for ourself if we have a valid address & port.
        if(NetworkUtils.isValidAddress(RouterService.getAddress()) &&
           NetworkUtils.isValidPort(RouterService.getPort())) {    
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
                sendPingReply(PingReply.create(ping.getGUID(), (byte)1), 
                    handler);
                return;
            }
    
            //send its own ping in all the cases
            int newTTL = hops+1;
            if ( (hops+ttl) <=2)
                newTTL = 1;        
    
            // send our own pong if we have free slots or if our average
            // daily uptime is more than 1/2 hour
            if(RouterService.getConnectionManager().hasFreeSlots()  ||
               Statistics.instance().calculateDailyUptime() > 60*30) {
                PingReply pr = 
                    PingReply.create(ping.getGUID(), (byte)newTTL);
                
                sendPingReply(pr, handler);
            }
        }
        
        List<PingReply> pongs = PongCacher.instance().getBestPongs(ping.getLocale());
        byte[] guid = ping.getGUID();
        InetAddress pingerIP = handler.getInetAddress();
        for(PingReply pr : pongs) {
            if(pr.getInetAddress().equals(pingerIP))
                continue;
            sendPingReply(pr.mutateGUID(guid), handler);
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
	protected void respondToUDPPingRequest(PingRequest request, 
										   InetSocketAddress addr,
                                           ReplyHandler handler) {
        if(!RouterService.isIpPortValid())
            return;
        
        IpPort ipport = null;
        if (request.requestsIP()) {
            try {
                ipport = new IPPortCombo(
                            addr.getAddress().getHostAddress(),
                            addr.getPort());
            } catch(IOException tooBad) { }
        }
        
        byte[] data = request.getSupportsCachedPongData();
        Collection<IpPort> hosts = Collections.emptyList();
        if(data != null) {
            boolean isUltrapeer =
                data.length >= 1 && 
		        (data[0] & PingRequest.SCP_ULTRAPEER_OR_LEAF_MASK) ==
		            PingRequest.SCP_ULTRAPEER;
            hosts = RouterService.getPreferencedHosts(
                        isUltrapeer, 
			            request.getLocale(),
			            ConnectionSettings.NUM_RETURN_PONGS.getValue());
        }        
        
        
        PingReply reply;
    	if (ipport != null)
    	    reply = PingReply.create(request.getGUID(), (byte)1, ipport, hosts);
    	else
    	    reply = PingReply.create(request.getGUID(), (byte)1, hosts);
        
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
        List<ManagedConnection> leafConnections = _manager.getInitializedClientConnections();
        
        for(ManagedConnection connection : leafConnections) {
            //create the pong for this connection
            PingReply pr = 
                PingReply.createExternal(m.getGUID(), (byte)2, 
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
    
    protected boolean respondToQueryRequest(QueryRequest queryRequest,
                                            byte[] clientGUID,
                                            ReplyHandler handler) {
        //Only respond if we understand the actual feature, if it had a feature.
        if(!FeatureSearchData.supportsFeature(queryRequest.getFeatureSelector()))
            return false;

        if (queryRequest.isWhatIsNewRequest())
            ReceivedMessageStat.WHAT_IS_NEW_QUERY_MESSAGES.incrementStat();
                                                
        // Only send results if we're not busy.  Note that this ignores
        // queue slots -- we're considered busy if all of our "normal"
        // slots are full.  This allows some spillover into our queue that
        // is necessary because we're always returning more total hits than
        // we have slots available.
        if(!RouterService.getUploadManager().mayBeServiceable() )  {
            return false;
        }
                                                
                                                
        // Ensure that we have a valid IP & Port before we send the response.
        // Otherwise the QueryReply will fail on creation.
        if( !NetworkUtils.isValidPort(RouterService.getPort()) ||
            !NetworkUtils.isValidAddress(RouterService.getAddress()))
            return false;
                                                     
        // Run the local query
        Response[] responses = 
            RouterService.getFileManager().query(queryRequest);
        
        if( RouterService.isShieldedLeaf() && queryRequest.isTCP() ) {
            if( responses != null && responses.length > 0 )
                RoutedQueryStat.LEAF_HIT.incrementStat();
            else
                RoutedQueryStat.LEAF_FALSE_POSITIVE.incrementStat();
        }

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
        // metafiles, if any.
        if (!RouterService.getUploadManager().isServiceable()) {
        	
        	List<Response> filtered = new ArrayList<Response>(responses.length);
        	for(Response r : responses) {
        		if (r.isMetaFile())
        			filtered.add(r);
        	}
        	
        	if (filtered.isEmpty()) // nothing to send..
        		return false;
        	
        	responses = filtered.toArray(responses);
        }
        
        // Here we can do a couple of things - if the query wants
        // out-of-band replies we should do things differently.  else just
        // send it off as usual.  only send out-of-band if you can
        // receive solicited udp AND not servicing too many
        // uploads AND not connected to the originator of the query
        if (query.desiresOutOfBandReplies() &&
            !isConnectedTo(query, handler) && 
			RouterService.canReceiveSolicited() &&
            NetworkUtils.isValidAddressAndPort(query.getReplyAddress(), query.getReplyPort())) {
            
            // send the replies out-of-band - we need to
            // 1) buffer the responses
            // 2) send a ReplyNumberVM with the number of responses
            if (bufferResponsesForLaterDelivery(query, responses)) {
                // special out of band handling....
                InetAddress addr = null;
                try {
                    addr = InetAddress.getByName(query.getReplyAddress());
                } catch (UnknownHostException uhe) {}
                int port = query.getReplyPort();
                
                if(addr != null) { 
                    // send a ReplyNumberVM to the host - he'll ACK you if he
                    // wants the whole shebang
                    int resultCount = 
                        (responses.length > 255) ? 255 : responses.length;
                    ReplyNumberVendorMessage vm = 
                        new ReplyNumberVendorMessage(new GUID(query.getGUID()),
                                                     resultCount);
                    UDPService.instance().send(vm, addr, port);
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
    protected List<QueryReply> createQueryReply(byte[] guid, byte ttl,
                                    long speed, Response[] res,
                                    byte[] clientGUID, 
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean isFromMcast,
                                    boolean isFWTransfer) {
        
        List<QueryReply> queryReplies = new ArrayList<QueryReply>();
        QueryReply queryReply = null;

        // pick the right address & port depending on multicast & fwtrans
        // if we cannot find a valid address & port, exit early.
        int port = -1;
        byte[] ip = null;
        // first try using multicast addresses & ports, but if they're
        // invalid, fallback to non multicast.
        if(isFromMcast) {
            ip = RouterService.getNonForcedAddress();
            port = RouterService.getNonForcedPort();
            if(!NetworkUtils.isValidPort(port) ||
               !NetworkUtils.isValidAddress(ip))
                isFromMcast = false;
        }
        
        if(!isFromMcast) {
            
            // see if we have a valid FWTrans address.  if not, fall back.
            if(isFWTransfer) {
                port = UDPService.instance().getStableUDPPort();
                ip = RouterService.getExternalAddress();
                if(!NetworkUtils.isValidAddress(ip) 
                        || !NetworkUtils.isValidPort(port))
                    isFWTransfer = false;
            }
            
            // if we still don't have a valid address here, exit early.
            if(!isFWTransfer) {
                ip = RouterService.getAddress();
                port = RouterService.getPort();
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
        } catch(UnsupportedEncodingException ueex) {//no support for utf-8??
            //all implementations of java must support utf8 encoding
            //here we will allow this QueryReply to be sent out 
            //with xml being empty rather than not allowing the
            //Query to be sent out 
            //therefore we won't throw a IllegalArgumentException but we will
            //show it so the error will be sent to Bug servlet
            ErrorService.error
                (ueex,
                 "encountered UnsupportedEncodingException in creation of QueryReply : xmlCollectionString : " 
                  + xmlCollectionString);
        }
        
        // get the *latest* push proxies if we have not accepted an incoming
        // connection in this session
        boolean notIncoming = !RouterService.acceptedIncomingConnection();
        Set<IpPort> proxies = notIncoming ? _manager.getPushProxies() : null;
        
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
                    //all implementations of java must support utf8 encoding
                    //so if we get here there was something really wrong
                    //we will show the error but treat as if the currXML was
                    //empty (see the try catch for uee earlier)
                    ErrorService.error
                        (ueex,
                         "encountered UnsupportedEncodingException : currXML " 
                          + currXML);
                    currXMLBytes = "".getBytes();
                }
                if ((currXMLBytes.length > QueryReply.XML_MAX_SIZE) &&
                                                        (currResps.length > 1)) 
                    splitAndAddResponses(splitResps, currResps);
                else {
                    // create xml bytes if possible...
                    byte[] xmlCompressed = null;
                    if ((currXML != null) && (!currXML.equals("")))
                        xmlCompressed = LimeXMLUtils.compress(currXMLBytes);
                    else //there is no XML
                        xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
                    
                    // create the new queryReply
                    queryReply = new QueryReply(guid, ttl, port, ip, speed, 
                                                currResps, _clientGUID, 
                                                xmlCompressed, notIncoming, 
                                                busy, uploaded, 
                                                measuredSpeed, 
                                                ChatSettings.CHAT_ENABLED.getValue(),
                                                isFromMcast, isFWTransfer,
                                                proxies);
                    queryReplies.add(queryReply);
                }
            }

        }
        else {  // xml is small enough, no problem.....
            // get xml bytes if possible....
            byte[] xmlCompressed = null;
            if (xmlCollectionString!=null && !xmlCollectionString.equals(""))
                xmlCompressed = 
                    LimeXMLUtils.compress(xmlBytes);
            else //there is no XML
                xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY;
            
            // create the new queryReply
            queryReply = new QueryReply(guid, ttl, port, ip, speed, res, 
                                        _clientGUID, xmlCompressed,
                                        notIncoming, busy, uploaded, 
                                        measuredSpeed, 
                                        ChatSettings.CHAT_ENABLED.getValue(),
                                        isFromMcast, isFWTransfer,
                                        proxies);
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
