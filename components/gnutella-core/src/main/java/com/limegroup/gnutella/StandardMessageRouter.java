
// Edited for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Collection;

import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.ReceivedMessageStat;
import com.limegroup.gnutella.statistics.RoutedQueryStat;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This class is the message routing implementation for TCP messages.
 * 
 */
public class StandardMessageRouter extends MessageRouter {

    /**
     * 
     * 
     * 
     * 
     * 
     * Called when the computer represented by handler has sent us a ping.
     * Here's what's happened so far:
     * The MessageReader sliced data from a remote computer into a Gnutella packet, and parsed it into a new Message object.
     * The message went to ManagedConnection.processReadMessage(m) which called ManagedConnection.handleMessageInternal(m).
     * That method called MessageDispatcher.dispatchTCP(m, managedConnection), which packaged the message and connection objects for an asynchronous call.
     * The "MessageDispatch" thread picked up the message and connection, and called MessageRouter.handleMessage(Message, ManagedConnection).
     * MessageRouter.handleMessage() hopped the packet, moving 1 from TTL to hops, and sorted it to the next method.
     * MessageRouter.handlePingRequestPossibleDuplicate() added the ping's GUID to the MessageRouter's RouteTable for pings and pongs.
     * MessageRouter.handlePingRequest() made sure the remote computer that sent us this ping isn't pinging us too frequently.
     * Control then reaches here.
     * 
     * Responds to a Gnutella ping with cached pongs.  This does special
     * handling for both "heartbeat" pings that were sent to ensure that
     * the connection is still live as well as for pings from a crawler.
     * 
     * @param request A ping packet we received
     * @param handler The remote computer that sent it to us over a TCP socket Gnutella connection
     */
    protected void respondToPingRequest(PingRequest ping, ReplyHandler handler) {

        /*
         * If this wasn't a handshake or crawler ping, check if we can accept
         * incoming connection for old-style unrouted connections, ultrapeers, or
         * leaves.  TODO: does this mean leaves always respond to pings?
         */

        // Get the hops and TTL from the packet, this is after MessageRouter.handleMessage() hopped it
        int hops = (int)ping.getHops(); // The number of times the ping traveled between ultrapeers to get here
        int ttl  = (int)ping.getTTL();  // The number of additional trips between ultrapeers the ping can make

        /*
         * We should only reply to this ping with a pong about us if we're an ultrapeer still hoping more computers will connect to us.
         * Returning a pong about us will advertise us on the network, encouraging computers to try to connect.
         * If all our connection slots are full, we don't want to advertise ourselves with a pong.
         */

        // Only respond with a pong if this ping was made with a TTL of 2 or less, or we have an available slot of any kind
        if ((hops + ttl > 2) &&             // If this ping was made with a TTL of 3 or more, and
            !_manager.allowAnyConnection()) // We're a leaf, or we're an ultrapeer with all the ultrapeer and leaf connections we need
            return;                         // Leave without responding to this ping

        // Before we make a a pong about us, make sure our externally contactable IP address and port number look right
        if (NetworkUtils.isValidAddress(RouterService.getAddress()) && // Make sure our IP address we'll advertise doesn't start 0 or 255
            NetworkUtils.isValidPort(RouterService.getPort())) {       // Make sure our port number we'll advertise isn't 0 or too big to fit in 2 bytes

            /*
             * SPECIAL CASE: for crawler ping
             * TODO:: this means that we can never send TTL=2 pings without
             * them being interpreted as from the crawler!!
             */

            // If this ping has a hops and TTL of 1, it's a special ping from the crawler
            if (hops == 1 && ttl == 1) {

                // Handle it seprately, and leave
                handleCrawlerPing(ping, handler);
                return;

                /*
                 * Note that the while handling crawler ping, we dont send our
                 * own pong, as that is unnecessary, since crawler already has
                 * our address.
                 */
            }

            // If this ping has 1 hop and 0 TTL, it's just for keeping the TCP connection from closing because it's been quiet too long
            if (ping.isHeartbeat()) {

                /*
                 * handle heartbeat pings specially -- bypass pong caching code
                 */

                sendPingReply(
                    PingReply.create(ping.getGUID(),
                    (byte)1),
                    handler);

                return;
            }

            // Compute the TTL for the pong we'll send in reply to this ping
            int newTTL = hops + 1;             // Give the pong a TTL of the ping's hops and one more, making sure it can make it back to the computer that sent the ping
            if ((hops + ttl) <= 2) newTTL = 1; // If the ping's original TTL was 1 or 2, only let our pong travel one hop

            /*
             * send our own pong if we have free slots or if our average
             * daily uptime is more than 1/2 hour
             */

            // Only send a pong if we're an ultrapeer with free slots, or we're a leaf right now but have an average daily uptime of over a half hour
            if (RouterService.getConnectionManager().hasFreeSlots() ||    // Only send a pong if we want more computers to connect to us, or
                Statistics.instance().calculateDailyUptime() > 60 * 30) { // On an average day, we're online more than 30 minutes

                // Make a new pong packet about us
                PingReply pr = PingReply.create(
                    ping.getGUID(), // Give the pong the same message GUID as the ping, letting the Gnutella network route the pong back to the compuer that made the ping
                    (byte)newTTL);  // The TTL for the pong we computed above

                /*
                 * Tour Point: Inside a packet.
                 * 
                 * Here's what this pong looks like:
                 * 
                 * 74 73 28 3d 74 3a 8b f9  ts(=t:--  aaaaaaaa
                 * c0 33 87 6f 76 39 e9 00  -3-ov9--  aaaaaaaa
                 * 01 03 00 2d 00 00 00 e7  --------  bcdeeeef
                 * 18 d8 1b 9e 4a 01 00 00  ----J---  fgggghhh
                 * 00 00 20 00 00 c3 02 44  -------D  hiiiijkk
                 * 55 42 35 07 03 4c 4f 43  UB5--LOC  kkkkllll
                 * 43 65 6e 02 02 55 50 43  Cen--UPC  llllmmmm
                 * 01 1c 1b 82 56 43 45 4c  ----VCEL  mmmnnnnn
                 * 49 4d 45 49              IMEI      nnnn
                 * 
                 * a is the 16 byte message GUID from the ping.
                 * b is 0x01, the byte code for a pong.
                 * c is the TTL, here shown as 3.
                 * d is the hops, 0.
                 * 
                 * e is the length of the payload, 0x2d, which is 45 bytes.
                 * The length is 4 bytes in little endian order, like 2d 00 00 00.
                 * 
                 * f is the port number this computer is listening on right now, e7 18, little endian 0x18e7, port number 6375.
                 * g is this computer's IP address, d8 1b 9e 4a, 216.27.158.74, with the bytes in the same order as the text.
                 * h is the number of files this computer is sharing, 1, in little endian order 01 00 00 00.
                 * 
                 * i is total size in KB of shared data here, adjusted to the nearest power of 2.
                 * The little endian 00 20 00 00 is 0x2000, 8192 KB, which is bigger than the one file I'm sharing.
                 * 
                 * j is 0xC3, the byte that begins a GGEP block, and the remaining lettered regions are GGEP extensions.
                 * k is 0000 0010 "DU"  0100 0010 35 07.
                 * l is 0000 0011 "LOC" 0100 0011 "en" 02.
                 * m is 0000 0010 "UP"  0100 0011 01 1c 1b.
                 * n is 1000 0010 "VC"  0100 0101 "LIME" 27.
                 * 
                 * The first byte in each extension contains flags and a length.
                 * The first bit marks the last extension, and is set only in "VC".
                 * The second bit is 0 because none of the extension values are COBS encoded.
                 * We don't need to hide 0 values in a GGEP block in a pong.
                 * The third bit is 0 because none of the extension values are deflate compressed.
                 * The right 4 bits are the length of the extension name, like 0010 2 for "DU" and 0011 3 for "LOC".
                 * 
                 * The byte after the tag holds the length of the value.
                 * The bytes all start 01 because the lengths all fit in 1 byte.
                 * The remianing 6 bits hold the length, like 10 2 for "35 07" and "101" 5 for "LIME 27".
                 * 
                 * k is 0000 0010 "DU" 0100 0010 35 07, Daily Uptime.
                 * The value is the number of seconds this computer is online in an average day.
                 * The two bytes in the value 35 07 are little endian 0x00000735, 1845 seconds, a little over 30 minutes.
                 * 
                 * l is 0000 0011 "LOC" 0100 0011 "en" 02, Locale preference.
                 * "en" is for English, and 2 is the number of additional ultrapeers I want that also prefer English.
                 * 
                 * m is 0000 0010 "UP" 0100 0011 01 1c 1b, Ultrapeer.
                 * I'm sending this tag because LimeWire is running as an ultrapeer right now.
                 * The value is 3 bytes, "01 1c 1b".
                 * The first byte is the version of the ultrapeer protocol the computer supports, 0.1, squashed into a single byte.
                 * The second byte is the number of free leaf slots I have, 28.
                 * The third byte is the number of free ultrapeer slots I have, 27.
                 * 
                 * n is 1000 0010 "VC" 0100 0101 "LIME" 49, Vendor Code.
                 * The first 4 bytes are "LIME" for LimeWire.
                 * The last byte is 0x49 0100 1001 4 and 9 for LimeWire version 4.9.
                 * 
                 * Once we realize we're externally contactable for UDP, we'll also include the "GUE" extension.
                 */

                // Send it to the remote computer that sent us this ping
                sendPingReply(pr, handler);
            }
        }

        List pongs = PongCacher.instance().getBestPongs(ping.getLocale());
        Iterator iter = pongs.iterator();
        byte[] guid = ping.getGUID();
        InetAddress pingerIP = handler.getInetAddress();

        while (iter.hasNext()) {

            PingReply pr = (PingReply)iter.next();
            if (pr.getInetAddress().equals(pingerIP)) continue;
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
                ipport = new QueryReply.IPPortCombo(
                            addr.getAddress().getHostAddress(),
                            addr.getPort());
            } catch(IOException tooBad) { }
        }
        
        byte[] data = request.getSupportsCachedPongData();
        Collection hosts = Collections.EMPTY_LIST;
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

    /**
     * 
     * @param queryRequest
     * @param clientGUID   Not used, our client ID GUID that uniquely identifies us on the Gnutella network
     * @param handler
     */
    protected boolean respondToQueryRequest(QueryRequest queryRequest, byte[] clientGUID, ReplyHandler handler) {

        //Only respond if we understand the actual feature, if it had a feature.
        if (!FeatureSearchData.supportsFeature(queryRequest.getFeatureSelector())) return false;

        if (queryRequest.isWhatIsNewRequest()) ReceivedMessageStat.WHAT_IS_NEW_QUERY_MESSAGES.incrementStat();

        /*
         * Only send results if we're not busy.  Note that this ignores
         * queue slots -- we're considered busy if all of our "normal"
         * slots are full.  This allows some spillover into our queue that
         * is necessary because we're always returning more total hits than
         * we have slots available.
         */

        if (!RouterService.getUploadManager().isServiceable()) return false;

        /*
         * Ensure that we have a valid IP & Port before we send the response.
         * Otherwise the QueryReply will fail on creation.
         */

        if (!NetworkUtils.isValidPort(RouterService.getPort()) ||
            !NetworkUtils.isValidAddress(RouterService.getAddress()))
            return false;

        // Run the local query
        Response[] responses = RouterService.getFileManager().query(queryRequest);

        if (RouterService.isShieldedLeaf() && queryRequest.isTCP()) {

            if (responses != null && responses.length > 0)
                RoutedQueryStat.LEAF_HIT.incrementStat();
            else
                RoutedQueryStat.LEAF_FALSE_POSITIVE.incrementStat();
        }

        return sendResponses(responses, queryRequest, handler);
    }

    //This method needs to be public because the Peer-Server code uses it.
    public boolean sendResponses(Response[] responses, QueryRequest query, ReplyHandler handler) {

        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ((responses == null) || ((responses.length < 1))) return false;

        
        // Here we can do a couple of things - if the query wants
        // out-of-band replies we should do things differently.  else just
        // send it off as usual.  only send out-of-band if you can
        // receive solicited udp AND not servicing too many
        // uploads AND not connected to the originator of the query
        if (query.desiresOutOfBandReplies() &&
            !isConnectedTo(query, handler) && 
			RouterService.canReceiveSolicited() &&
            RouterService.getUploadManager().isServiceable() &&
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
     * 
     * 
     * 
     * @param guid
     * @param ttl
     * @param speed
     * @param res
     * @param clientGUID Our client ID GUID, not used, we just use _clientGUID instead
     * @param busy
     * @param uploaded
     * @param measuredSpeed
     * @param isFromMcast
     * @param isFWTransfer
     * @return
     */
    protected List createQueryReply(byte[] guid, byte ttl, long speed, Response[] res, byte[] clientGUID, boolean busy, boolean uploaded, boolean measuredSpeed, boolean isFromMcast, boolean isFWTransfer) {

        List queryReplies = new ArrayList();
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
                    return Collections.EMPTY_LIST;
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
                    queryReply = new QueryReply(
                        guid,
                        ttl,
                        port,
                        ip,
                        speed,
                        currResps,
                        _clientGUID, // Write our client ID GUID into the last 16 bytes of the query hit packet to let downloaders get push requests to us 
                        xmlCompressed,
                        notIncoming,
                        busy,
                        uploaded,
                        measuredSpeed,
                        ChatSettings.CHAT_ENABLED.getValue(),
                        isFromMcast,
                        isFWTransfer,
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
            queryReply = new QueryReply(
                guid,
                ttl,
                port,
                ip,
                speed,
                res,
                _clientGUID, // Write our client ID GUID into the last 16 bytes of the query hit packet to let downloaders get push requests to us
                xmlCompressed,
                notIncoming,
                busy,
                uploaded,
                measuredSpeed,
                ChatSettings.CHAT_ENABLED.getValue(),
                isFromMcast,
                isFWTransfer,
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
