package com.limegroup.gnutella;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.statistics.ReceivedMessageStat;
import com.limegroup.gnutella.statistics.RoutedQueryStat;
import com.limegroup.gnutella.util.DataUtils;
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
        int hops = (int)ping.getHops();
        int ttl = (int)ping.getTTL();
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
        
        List pongs = PongCacher.instance().getBestPongs(ping.getLocale());
        Iterator iter = pongs.iterator();
        byte[] guid = ping.getGUID();
        InetAddress pingerIP = handler.getInetAddress();
        while(iter.hasNext()) {
            PingReply pr = (PingReply)iter.next();
            if(pr.getInetAddress().equals(pingerIP))
                continue;
            sendPingReply(pr.mutateGUID(guid), handler);
        }
    }

	/**
	 * Responds to a ping request received over a UDP port.  This is
	 * handled differently from all other ping requests.  Instead of
	 * responding with a pong from this node, we respond with a pong
	 * from other UltraPeers supporting UDP from our cache.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param datagram the <tt>DatagramPacket</tt> containing the IP
     *  and port of the client node
     * @param handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
	 */
	protected void respondToUDPPingRequest(PingRequest request, 
										   DatagramPacket datagram,
                                           ReplyHandler handler) {
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();
        
        QueryReply.IPPortCombo ipport =null;
        if (request.requestsIP())
            try{
        	    
                ipport = 
                    new QueryReply.IPPortCombo(
                        datagram.getAddress().getHostAddress(),
                        datagram.getPort());
            }catch(IOException tooBad) {}
        
        PingReply reply=null;
        
        if(NetworkUtils.isValidAddress(addr) &&
           NetworkUtils.isValidPort(port)) 
        	if (ipport != null)
        	    reply = PingReply.create(request.getGUID(),
        	            (byte)1,ipport);
        	else
        	    reply = PingReply.create(request.getGUID(),
        	            (byte)1);
        
        if (reply!=null)
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
        List /*<ManagedConnection>*/ leafConnections 
            = _manager.getInitializedClientConnections();
        
        for(Iterator iterator = leafConnections.iterator(); 
            iterator.hasNext();) {
            //get the next connection
            ManagedConnection connection = (ManagedConnection)iterator.next();
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
    
    protected void handlePingReply(PingReply pingReply,
								   ReplyHandler receivingConnection) {
        //We override the super's method so the receiving connection's
        //statistics are updated whether or not this is for me.
		if(receivingConnection instanceof ManagedConnection) {
			ManagedConnection mc = (ManagedConnection)receivingConnection;
			mc.updateHorizonStats(pingReply);
		}
        super.handlePingReply(pingReply, receivingConnection);
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
        if(!RouterService.getUploadManager().isServiceable() )  {
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

    //This method needs to be public because the Peer-Server code uses it.
    public boolean sendResponses(Response[] responses, QueryRequest query,
                                 ReplyHandler handler) {
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return false;

        
        // Here we can do a couple of things - if the query wants
        // out-of-band replies we should do things differently.  else just
        // send it off as usual.  only send out-of-band if you can
        // receive solicited udp AND not servicing too many
        // uploads AND not connected to the originator of the query
        if (query.desiresOutOfBandReplies() &&
            !isConnectedTo(query, handler) && 
			RouterService.canReceiveSolicited() &&
            RouterService.getUploadManager().isServiceable()) {
            
            // send the replies out-of-band - we need to
            // 1) buffer the responses
            // 2) send a ReplyNumberVM with the number of responses
            if (bufferResponsesForLaterDelivery(query, responses)) {
                // special out of band handling....
                InetAddress addr = null;
                try {
                    addr = InetAddress.getByName(query.getReplyAddress());
                }
                catch (UnknownHostException uhe) {
                    // weird - just forget about it.....
                    return false;
                }
                int port = query.getReplyPort();
                
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
            // else i couldn't buffer the responses due to busy-ness, oh, scrap
            // them.....

            return false;
        }

        // send the replies in-band
        // -----------------------------

        //convert responses to QueryReplies
        Iterator /*<QueryReply>*/iterator=responsesToQueryReplies(responses,
                                                                  query);
        //send the query replies
        try {
            while(iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();
                sendQueryReply(queryReply);
            }
        } 
        catch (IOException e) {
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
    protected List createQueryReply(byte[] guid, byte ttl,
                                    long speed, Response[] res,
                                    byte[] clientGUID, 
                                    boolean busy, boolean uploaded, 
                                    boolean measuredSpeed, 
                                    boolean isFromMcast,
                                    boolean canFWTransfer) {
        
        List queryReplies = new ArrayList();
        QueryReply queryReply = null;

        // if it is a multicasted response, use the non-forced address
        // and port
        int port = isFromMcast ?
            RouterService.getNonForcedPort() :
            RouterService.getPort();
        byte[] ip = isFromMcast ? RouterService.getNonForcedAddress() :
                    (canFWTransfer ? RouterService.getExternalAddress() : 
                     RouterService.getAddress());


        
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
        Set proxies = 
            (notIncoming ? 
             _manager.getPushProxies() : null);
        
        // it may be too big....
        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {
            // ok, need to partition responses up once again and send out
            // multiple query replies.....
            List splitResps = new LinkedList();
            splitAndAddResponses(splitResps, res);

            while (!splitResps.isEmpty()) {
                Response[] currResps = (Response[]) splitResps.remove(0);
                String currXML = 
                LimeXMLDocumentHelper.getAggregateString(currResps);
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
                                                isFromMcast, canFWTransfer,
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
                                        isFromMcast, canFWTransfer,
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

    private void splitAndAddResponses(List addTo, Response[] toSplit) {
        Response[][] splits = splitResponses(toSplit);
        addTo.add(splits[0]);
        addTo.add(splits[1]);
    }

    
}
