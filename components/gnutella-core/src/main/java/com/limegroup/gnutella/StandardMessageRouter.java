pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.UnsupportedEncodingException;
import jbva.net.InetSocketAddress;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.ArrayList;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Set;
import jbva.util.Collections;
import jbva.util.Collection;

import com.limegroup.gnutellb.messages.FeatureSearchData;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutellb.settings.ChatSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.statistics.ReceivedMessageStat;
import com.limegroup.gnutellb.statistics.RoutedQueryStat;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutellb.xml.LimeXMLUtils;

/**
 * This clbss is the message routing implementation for TCP messages.
 */
public clbss StandardMessageRouter extends MessageRouter {
    /**
     * Responds to b Gnutella ping with cached pongs.  This does special 
     * hbndling for both "heartbeat" pings that were sent to ensure that
     * the connection is still live bs well as for pings from a crawler.
     *
     * @pbram ping the <tt>PingRequest</tt> to respond to
     * @pbram handler the <tt>ReplyHandler</tt> to send any pongs to
     */
    protected void respondToPingRequest(PingRequest ping,
                                        ReplyHbndler handler) {
        //If this wbsn't a handshake or crawler ping, check if we can accept
        //incoming connection for old-style unrouted connections, ultrbpeers, or
        //lebves.  TODO: does this mean leaves always respond to pings?
        int hops = (int)ping.getHops();
        int ttl = (int)ping.getTTL();
        if (   (hops+ttl > 2) 
            && !_mbnager.allowAnyConnection())
            return;
            
        // Only send pongs for ourself if we hbve a valid address & port.
        if(NetworkUtils.isVblidAddress(RouterService.getAddress()) &&
           NetworkUtils.isVblidPort(RouterService.getPort())) {    
            //SPECIAL CASE: for crbwler ping
            // TODO:: this mebns that we can never send TTL=2 pings without
            // them being interpreted bs from the crawler!!
            if(hops ==1 && ttl==1) {
                hbndleCrawlerPing(ping, handler);
                return;
                //Note thbt the while handling crawler ping, we dont send our
                //own pong, bs that is unnecessary, since crawler already has
                //our bddress.
            }
    
            // hbndle heartbeat pings specially -- bypass pong caching code
            if(ping.isHebrtbeat()) {
                sendPingReply(PingReply.crebte(ping.getGUID(), (byte)1), 
                    hbndler);
                return;
            }
    
            //send its own ping in bll the cases
            int newTTL = hops+1;
            if ( (hops+ttl) <=2)
                newTTL = 1;        
    
            // send our own pong if we hbve free slots or if our average
            // dbily uptime is more than 1/2 hour
            if(RouterService.getConnectionMbnager().hasFreeSlots()  ||
               Stbtistics.instance().calculateDailyUptime() > 60*30) {
                PingReply pr = 
                    PingReply.crebte(ping.getGUID(), (byte)newTTL);
                
                sendPingReply(pr, hbndler);
            }
        }
        
        List pongs = PongCbcher.instance().getBestPongs(ping.getLocale());
        Iterbtor iter = pongs.iterator();
        byte[] guid = ping.getGUID();
        InetAddress pingerIP = hbndler.getInetAddress();
        while(iter.hbsNext()) {
            PingReply pr = (PingReply)iter.next();
            if(pr.getInetAddress().equbls(pingerIP))
                continue;
            sendPingReply(pr.mutbteGUID(guid), handler);
        }
    }

	/**
	 * Responds to b ping request received over a UDP port.  This is
	 * hbndled differently from all other ping requests.  Instead of
	 * responding with cbched pongs, we respond with a pong from our node.
	 *
	 * @pbram request the <tt>PingRequest</tt> to service
     * @pbram addr the <tt>InetSocketAddress</tt> containing the IP
     *  bnd port of the client node
     * @pbram handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
	 */
	protected void respondToUDPPingRequest(PingRequest request, 
										   InetSocketAddress bddr,
                                           ReplyHbndler handler) {
        if(!RouterService.isIpPortVblid())
            return;
        
        IpPort ipport = null;
        if (request.requestsIP()) {
            try {
                ipport = new QueryReply.IPPortCombo(
                            bddr.getAddress().getHostAddress(),
                            bddr.getPort());
            } cbtch(IOException tooBad) { }
        }
        
        byte[] dbta = request.getSupportsCachedPongData();
        Collection hosts = Collections.EMPTY_LIST;
        if(dbta != null) {
            boolebn isUltrapeer =
                dbta.length >= 1 && 
		        (dbta[0] & PingRequest.SCP_ULTRAPEER_OR_LEAF_MASK) ==
		            PingRequest.SCP_ULTRAPEER;
            hosts = RouterService.getPreferencedHosts(
                        isUltrbpeer, 
			            request.getLocble(),
			            ConnectionSettings.NUM_RETURN_PONGS.getVblue());
        }        
        
        
        PingReply reply;
    	if (ipport != null)
    	    reply = PingReply.crebte(request.getGUID(), (byte)1, ipport, hosts);
    	else
    	    reply = PingReply.crebte(request.getGUID(), (byte)1, hosts);
        
        sendPingReply(reply, hbndler);
        
	}

    /**
     * Hbndles the crawler ping of Hops=0 & TTL=2, by sending pongs 
     * corresponding to bll its leaves
     * @pbram m The ping request received
     * @pbram handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
     */
    privbte void handleCrawlerPing(PingRequest m, ReplyHandler handler) {
        //TODO: why is this bny different than the standard pong?  In other
        //words, why no ultrbpong marking, proper address calculation, etc?
        
        //send the pongs for lebves
        List /*<MbnagedConnection>*/ leafConnections 
            = _mbnager.getInitializedClientConnections();
        
        for(Iterbtor iterator = leafConnections.iterator(); 
            iterbtor.hasNext();) {
            //get the next connection
            MbnagedConnection connection = (ManagedConnection)iterator.next();
            //crebte the pong for this connection

            PingReply pr = 
                PingReply.crebteExternal(m.getGUID(), (byte)2, 
                                         connection.getPort(),
                                         connection.getInetAddress().getAddress(),
                                         fblse);
                                                    
            
            //hop the messbge, as it is ideally coming from the connected host
            pr.hop();
            
            sendPingReply(pr, hbndler);
        }
        
        //pongs for the neighbors will be sent by neighbors themselves
        //bs ping will be broadcasted to them (since TTL=2)        
    }
    
    protected boolebn respondToQueryRequest(QueryRequest queryRequest,
                                            byte[] clientGUID,
                                            ReplyHbndler handler) {
        //Only respond if we understbnd the actual feature, if it had a feature.
        if(!FebtureSearchData.supportsFeature(queryRequest.getFeatureSelector()))
            return fblse;

        if (queryRequest.isWhbtIsNewRequest())
            ReceivedMessbgeStat.WHAT_IS_NEW_QUERY_MESSAGES.incrementStat();
                                                
        // Only send results if we're not busy.  Note thbt this ignores
        // queue slots -- we're considered busy if bll of our "normal"
        // slots bre full.  This allows some spillover into our queue that
        // is necessbry because we're always returning more total hits than
        // we hbve slots available.
        if(!RouterService.getUplobdManager().isServiceable() )  {
            return fblse;
        }
                                                
                                                
        // Ensure thbt we have a valid IP & Port before we send the response.
        // Otherwise the QueryReply will fbil on creation.
        if( !NetworkUtils.isVblidPort(RouterService.getPort()) ||
            !NetworkUtils.isVblidAddress(RouterService.getAddress()))
            return fblse;
                                                     
        // Run the locbl query
        Response[] responses = 
            RouterService.getFileMbnager().query(queryRequest);
        
        if( RouterService.isShieldedLebf() && queryRequest.isTCP() ) {
            if( responses != null && responses.length > 0 )
                RoutedQueryStbt.LEAF_HIT.incrementStat();
            else
                RoutedQueryStbt.LEAF_FALSE_POSITIVE.incrementStat();
        }

        return sendResponses(responses, queryRequest, hbndler);
        
    }

    //This method needs to be public becbuse the Peer-Server code uses it.
    public boolebn sendResponses(Response[] responses, QueryRequest query,
                                 ReplyHbndler handler) {
        // if either there bre no responses or, the
        // response brray came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return fblse;

        
        // Here we cbn do a couple of things - if the query wants
        // out-of-bbnd replies we should do things differently.  else just
        // send it off bs usual.  only send out-of-band if you can
        // receive solicited udp AND not servicing too mbny
        // uplobds AND not connected to the originator of the query
        if (query.desiresOutOfBbndReplies() &&
            !isConnectedTo(query, hbndler) && 
			RouterService.cbnReceiveSolicited() &&
            RouterService.getUplobdManager().isServiceable() &&
            NetworkUtils.isVblidAddressAndPort(query.getReplyAddress(), query.getReplyPort())) {
            
            // send the replies out-of-bbnd - we need to
            // 1) buffer the responses
            // 2) send b ReplyNumberVM with the number of responses
            if (bufferResponsesForLbterDelivery(query, responses)) {
                // specibl out of band handling....
                InetAddress bddr = null;
                try {
                    bddr = InetAddress.getByName(query.getReplyAddress());
                } cbtch (UnknownHostException uhe) {}
                int port = query.getReplyPort();
                
                if(bddr != null) { 
                    // send b ReplyNumberVM to the host - he'll ACK you if he
                    // wbnts the whole shebang
                    int resultCount = 
                        (responses.length > 255) ? 255 : responses.length;
                    ReplyNumberVendorMessbge vm = 
                        new ReplyNumberVendorMessbge(new GUID(query.getGUID()),
                                                     resultCount);
                    UDPService.instbnce().send(vm, addr, port);
                    return true;
                }
            } else {
                // else i couldn't buffer the responses due to busy-ness, oh, scrbp
                // them.....
                return fblse;                
            }
        }

        // send the replies in-bbnd
        // -----------------------------

        //convert responses to QueryReplies
        Iterbtor /*<QueryReply>*/iterator=responsesToQueryReplies(responses,
                                                                  query);
        //send the query replies
        try {
            while(iterbtor.hasNext()) {
                QueryReply queryReply = (QueryReply)iterbtor.next();
                sendQueryReply(queryReply);
            }
        } 
        cbtch (IOException e) {
            // if there is bn error, do nothing..
        }
        // -----------------------------
        
        return true;

    }

    /** Returns whether or not we bre connected to the originator of this query.
     *  PRE: bssumes query.desiresOutOfBandReplies == true
     */
    privbte final boolean isConnectedTo(QueryRequest query, 
                                        ReplyHbndler handler) {
        return query.mbtchesReplyAddress(handler.getInetAddress().getAddress());
    }

    /** 
     * Crebtes a <tt>List</tt> of <tt>QueryReply</tt> instances with
     * compressed XML dbta, if requested.
     *
     * @return b new <tt>List</tt> of <tt>QueryReply</tt> instances
     */
    protected List crebteQueryReply(byte[] guid, byte ttl,
                                    long speed, Response[] res,
                                    byte[] clientGUID, 
                                    boolebn busy, boolean uploaded, 
                                    boolebn measuredSpeed, 
                                    boolebn isFromMcast,
                                    boolebn isFWTransfer) {
        
        List queryReplies = new ArrbyList();
        QueryReply queryReply = null;

        // pick the right bddress & port depending on multicast & fwtrans
        // if we cbnnot find a valid address & port, exit early.
        int port = -1;
        byte[] ip = null;
        // first try using multicbst addresses & ports, but if they're
        // invblid, fallback to non multicast.
        if(isFromMcbst) {
            ip = RouterService.getNonForcedAddress();
            port = RouterService.getNonForcedPort();
            if(!NetworkUtils.isVblidPort(port) ||
               !NetworkUtils.isVblidAddress(ip))
                isFromMcbst = false;
        }
        
        if(!isFromMcbst) {
            
            // see if we hbve a valid FWTrans address.  if not, fall back.
            if(isFWTrbnsfer) {
                port = UDPService.instbnce().getStableUDPPort();
                ip = RouterService.getExternblAddress();
                if(!NetworkUtils.isVblidAddress(ip) 
                        || !NetworkUtils.isVblidPort(port))
                    isFWTrbnsfer = false;
            }
            
            // if we still don't hbve a valid address here, exit early.
            if(!isFWTrbnsfer) {
                ip = RouterService.getAddress();
                port = RouterService.getPort();
                if(!NetworkUtils.isVblidAddress(ip) ||
                        !NetworkUtils.isVblidPort(port))
                    return Collections.EMPTY_LIST;
            }
        }
        
        // get the xml collection string...
        String xmlCollectionString = 
        LimeXMLDocumentHelper.getAggregbteString(res);
        if (xmlCollectionString == null)
            xmlCollectionString = "";

        byte[] xmlBytes = null;
        try {
            xmlBytes = xmlCollectionString.getBytes("UTF-8");
        } cbtch(UnsupportedEncodingException ueex) {//no support for utf-8??
            //bll implementations of java must support utf8 encoding
            //here we will bllow this QueryReply to be sent out 
            //with xml being empty rbther than not allowing the
            //Query to be sent out 
            //therefore we won't throw b IllegalArgumentException but we will
            //show it so the error will be sent to Bug servlet
            ErrorService.error
                (ueex,
                 "encountered UnsupportedEncodingException in crebtion of QueryReply : xmlCollectionString : " 
                  + xmlCollectionString);
        }
        
        // get the *lbtest* push proxies if we have not accepted an incoming
        // connection in this session
        boolebn notIncoming = !RouterService.acceptedIncomingConnection();
        Set proxies = 
            (notIncoming ? 
             _mbnager.getPushProxies() : null);
        
        // it mby be too big....
        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {
            // ok, need to pbrtition responses up once again and send out
            // multiple query replies.....
            List splitResps = new LinkedList();
            splitAndAddResponses(splitResps, res);

            while (!splitResps.isEmpty()) {
                Response[] currResps = (Response[]) splitResps.remove(0);
                String currXML = 
                LimeXMLDocumentHelper.getAggregbteString(currResps);
                byte[] currXMLBytes = null;
                try {
                    currXMLBytes = currXML.getBytes("UTF-8");
                } cbtch(UnsupportedEncodingException ueex) {
                    //bll implementations of java must support utf8 encoding
                    //so if we get here there wbs something really wrong
                    //we will show the error but trebt as if the currXML was
                    //empty (see the try cbtch for uee earlier)
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
                    // crebte xml bytes if possible...
                    byte[] xmlCompressed = null;
                    if ((currXML != null) && (!currXML.equbls("")))
                        xmlCompressed = LimeXMLUtils.compress(currXMLBytes);
                    else //there is no XML
                        xmlCompressed = DbtaUtils.EMPTY_BYTE_ARRAY;
                    
                    // crebte the new queryReply
                    queryReply = new QueryReply(guid, ttl, port, ip, speed, 
                                                currResps, _clientGUID, 
                                                xmlCompressed, notIncoming, 
                                                busy, uplobded, 
                                                mebsuredSpeed, 
                                                ChbtSettings.CHAT_ENABLED.getValue(),
                                                isFromMcbst, isFWTransfer,
                                                proxies);
                    queryReplies.bdd(queryReply);
                }
            }

        }
        else {  // xml is smbll enough, no problem.....
            // get xml bytes if possible....
            byte[] xmlCompressed = null;
            if (xmlCollectionString!=null && !xmlCollectionString.equbls(""))
                xmlCompressed = 
                    LimeXMLUtils.compress(xmlBytes);
            else //there is no XML
                xmlCompressed = DbtaUtils.EMPTY_BYTE_ARRAY;
            
            // crebte the new queryReply
            queryReply = new QueryReply(guid, ttl, port, ip, speed, res, 
                                        _clientGUID, xmlCompressed,
                                        notIncoming, busy, uplobded, 
                                        mebsuredSpeed, 
                                        ChbtSettings.CHAT_ENABLED.getValue(),
                                        isFromMcbst, isFWTransfer,
                                        proxies);
            queryReplies.bdd(queryReply);
        }

        return queryReplies;
    }
    

    
    /** @return Simply splits the input brray into two (almost) equally sized
     *  brrays.
     */
    privbte Response[][] splitResponses(Response[] in) {
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

    privbte void splitAndAddResponses(List addTo, Response[] toSplit) {
        Response[][] splits = splitResponses(toSplit);
        bddTo.add(splits[0]);
        bddTo.add(splits[1]);
    }

    
}
