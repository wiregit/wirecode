package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.statistics.RoutedQueryStat;
import com.sun.java.util.collections.*;

/**
 * This class is the message routing implementation for TCP messages.
 */
public class StandardMessageRouter extends MessageRouter {
    
    private final boolean RECORD_STATS = !CommonUtils.isJava118();

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
            if(hops == 1 && ttl == 0) {
                PingReply pr = 
                    PingReply.create(ping.getGUID(), (byte)1);
               
                sendPingReply(pr, handler);
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

        List pongs = PongCacher.instance().getBestPongs();
        Iterator iter = pongs.iterator();
        byte[] guid = ping.getGUID();

        String pingerIP = handler.getInetAddress().getHostAddress();
        while(iter.hasNext()) {
            PingReply pr = (PingReply)iter.next();
            if(pr.getIP().equals(pingerIP)) {
                continue;
            }
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
        sendPingReply(PingReply.create(request.getGUID(), (byte)1), handler);
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
                                         connection.getListeningPort(),
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
                                            byte[] clientGUID) {
                                                
        // Only send results if we're not busy.  Note that this ignores
        // queue slots -- we're considered busy if all of our "normal"
        // slots are full.  This allows some spillover into our queue that
        // is necessary because we're always returning more total hits than
        // we have slots available.
        if(RouterService.getUploadManager().isBusy())  {
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
            if( RECORD_STATS ) {
                if( responses != null && responses.length > 0 )
                    RoutedQueryStat.LEAF_HIT.incrementStat();
                else
                    RoutedQueryStat.LEAF_FALSE_POSITIVE.incrementStat();
            }
        }

        return sendResponses(responses, queryRequest);
        
    }

    //This method needs to be public because the Peer-Server code uses it.
    public boolean sendResponses(Response[] responses, 
                               QueryRequest query) {
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return false;

        
        // Here we can do a couple of things - if the query wants
        // out-of-band replies we should do things differently.  else just
        // send it off as usual.  only send out-of-band if you are GUESS-
        // capable (being GUESS capable implies that you can receive 
        // incoming TCP) AND not firewalled AND not servicing too many
        // uploads
        if (query.desiresOutOfBandReplies() && (query.getHops() > 1) &&
            !query.isFirewalledSource() &&
            RouterService.isGUESSCapable() && 
            RouterService.acceptedIncomingConnection() &&
            !RouterService.getUploadManager().isBusy()) {
            
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
                try {
                    int resultCount = 
                        (responses.length > 255) ? 255 : responses.length;
                    ReplyNumberVendorMessage vm = 
                        new ReplyNumberVendorMessage(new GUID(query.getGUID()),
                                                     resultCount);
                    UDPService.instance().send(vm, addr, port);
                }
                catch (BadPacketException bpe) {
                    // should NEVER happen
                    ErrorService.error(bpe);
                }
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
                                    boolean isFromMcast) {
        
        List queryReplies = new ArrayList();
        QueryReply queryReply = null;

        // if it is a multicasted response, use the non-forced address
        // and port
        int port = isFromMcast ?
            RouterService.getNonForcedPort() :
            RouterService.getPort();
        byte[] ip = isFromMcast ? 
            RouterService.getNonForcedAddress() :
            RouterService.getAddress();
        
        // get the xml collection string...
        String xmlCollectionString = 
        LimeXMLDocumentHelper.getAggregateString(res);
        if (xmlCollectionString == null)
            xmlCollectionString = "";

        byte[] xmlBytes = null;
        try {
            xmlBytes = xmlCollectionString.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ueex) {//no support for utf-8??
            xmlCollectionString = "";
            xmlBytes = xmlCollectionString.getBytes();
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
                                                isFromMcast, proxies);
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
                                        isFromMcast, proxies);
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
