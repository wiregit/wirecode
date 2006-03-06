
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
 * 
 * 
 * 
 * 
 */
public class StandardMessageRouter extends MessageRouter {

    //done

    /**
     * Responds to a ping a remote computer sent us with a pong about us, and 6 more pongs we've cached.
     * The cached pongs all have different hops counts, providing a healthy variety of nearby and exotic pongs.
     * 
     * Uses the ping's message GUID for all the pongs, and sends them all back to the comptuer that sent us the ping.
     * This routes the in the direction of the computer that created the ping.
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

                // Reply with a pong about each of our leaves
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

                // Send our pong back
                sendPingReply(
                    PingReply.create(ping.getGUID(), (byte)1), // Make a new pong about us, using the ping's message GUID and only letting it travel 1 hop
                    handler);                                  // Send our pong back to the computer that pinged us
                return;
            }

            // Compute the TTL for the pong we'll send in reply to this ping
            int newTTL = hops + 1;             // Give the pong a TTL of the ping's hops and one more, making sure it can make it back to the computer that sent the ping
            if ((hops + ttl) <= 2) newTTL = 1; // If the ping's original TTL was 1 or 2, only let our pong travel one hop

            /*
             * send our own pong if we have free slots or if our average
             * daily uptime is more than 1/2 hour
             */

            // Only send a pong about us if we're an ultrapeer with free slots, or we're a leaf right now but have an average daily uptime of over a half hour
            if (RouterService.getConnectionManager().hasFreeSlots() ||    // Only send a pong if we want more computers to connect to us, or
                Statistics.instance().calculateDailyUptime() > 60 * 30) { // On an average day, we're online more than 30 minutes

                // Make a new pong packet about us
                PingReply pr = PingReply.create(
                    ping.getGUID(), // Give the pong the same message GUID as the ping, letting the Gnutella network route the pong back to the compuer that made the ping
                    (byte)newTTL);  // The TTL for the pong we computed above

                // Send it to the remote computer that sent us this ping
                sendPingReply(pr, handler);
            }
        }

        // Get 6 pongs from the PongCacher that traveled a varity of distances to get to us
        List pongs = PongCacher.instance().getBestPongs(ping.getLocale()); // Get pongs that match the ping's language preference

        // Loop through the pongs, sending each to the remote computer that sent us the ping
        Iterator    iter     = pongs.iterator();         // iter is an Iterator that will loop through the 6 pongs the PongCacher gave us
        byte[]      guid     = ping.getGUID();           // guid is the ping's message GUID, we'll use it for the pongs we send back
        InetAddress pingerIP = handler.getInetAddress(); // pingerIP is the IP address of the computer that sent us the ping
        while (iter.hasNext()) { // Loop through the pongs the PongCacher gave us
            PingReply pr = (PingReply)iter.next();

            // Make sure this pong isn't about the computer that sent us the ping
            if (pr.getInetAddress().equals(pingerIP)) continue; // We don't need to tell it about itself

            /*
             * Tour Point
             * 
             * Gnutella routes response packets back to the computer that sent the original request packet.
             * It does this by setting the respone packet's message GUID to the same GUID as the request packet, and then sending it back to the same computer.
             * This code shows us setting a pong's GUID to match that of a ping, and sending it back to the computer that sent us the ping.
             */

            // Send the pong back to the remote computer that sent us the ping
            sendPingReply(           // Leads to ManagedConnection.send(pong)
                pr.mutateGUID(guid), // Set the pong's message GUID to match the ping's message GUID
                handler);            // Send the pong back to the computer that sent us the ping
        }
    }

	/**
     * When a remote computer sends a ping in a UDP packet, respond with a single pong about us.
     * If the ping has the GGEP "IP" extension, tell the remote computer it's external IP address and port number with "IP" in the pong.
     * If the ping has "SCP", give the remote computer some addresses to try to connect to with "IPP".
	 * 
     * @param request A ping packet we received
     * @param addr    The IP address and port number of the remote computer that sent us the ping in a UDP packet
     * @param handler The UDPReplyHandler object that represents the remote computer
	 */
	protected void respondToUDPPingRequest(PingRequest request, InetSocketAddress addr, ReplyHandler handler) {

        // Make sure we don't think our IP address starts 0 or 255 or our port is 0
        if (!RouterService.isIpPortValid()) return;

        // This ping has a GGEP block with the "IP" extension, meaning the remote computer just wants to know what its IP address looks like from where we are
        IpPort ipport = null; // If the ping doesn't have "IP", ipport will still be null
        if (request.requestsIP()) {

            try {

                // We got the UDP packet from the addr InetSocketAddress, wrap the IP address and port number into a IPPortCombo object
                ipport = new QueryReply.IPPortCombo(addr.getAddress().getHostAddress(), addr.getPort());

            } catch (IOException tooBad) {}
        }

        // The ping has a GGEP block with the "SCP" extension and a value
        byte[] data = request.getSupportsCachedPongData(); // Get the value of the GGEP "SCP" Supports Cached Pongs extension
        Collection hosts = Collections.EMPTY_LIST;
        if (data != null) {

            // Determine if the computer that pinged is an ultrapeer or a leaf
            boolean isUltrapeer =   // The ping wants the addresses of ultrapeers with free ultrapeer slots, true, or free leaf slots, false
                data.length >= 1 && // Make sure the "SCP" value is at least 1 byte
		        (data[0] & PingRequest.SCP_ULTRAPEER_OR_LEAF_MASK) == PingRequest.SCP_ULTRAPEER; // If the lowest bit is 1, the ping wants ultrapeers seeking ultrapeers

            // Get the addresses of computers the computer that pinged us can try connecting to
            hosts = RouterService.getPreferencedHosts( // We'll put these addresses in our pong's "IPP" header
                isUltrapeer,         // Tell getPreferencedHosts to get ultrapeers with free ultrapeer slots, true, or with free leaf slots, false
			    request.getLocale(), // Have it match the ping's language preference, specified by "LOC"
			    ConnectionSettings.NUM_RETURN_PONGS.getValue()); // Get a maximum of 10 addresses, which will take up 60 bytes
        }

        // The ping has "IP", so our pong should tell the remote computer what its IP address looks like from here
        PingReply reply;
    	if (ipport != null) {

            // Make our pong response
    	    reply = PingReply.create( // Most of the information in the pong will be about us
                request.getGUID(),    // Make the pong's message GUID match the ping's
                (byte)1,              // TTL 1, the pong will only need to travel 1 hop back over UDP
                ipport,               // Include the GGEP "IP" extension to tell the remote computer that pinged us what its IP address is
                hosts);               // If the ping included a "SCP" request, add the addresses we collected under "IPP"

        // The ping doesn't have "IP"
        } else {

            // Make our pong response
    	    reply = PingReply.create( // Most of the information in the pong will be about us
                request.getGUID(),    // Make the pong's message GUID match the ping's
                (byte)1,              // TTL 1, the pong will only need to travel 1 hop back over UDP
                hosts);               // If the ping included a "SCP" request, add the addresses we collected under "IPP"
        }

        // Send our pong response in a UDP packet back to the remote computer that pinged us
        sendPingReply(reply, handler);
	}

    /**
     * Responds to a crawler ping with a pong about each one of our leaves.
     * 
     * A crawler ping arrived with 2 TTL and 0 hops.
     * Then, we hopped it, so now it has 1 TTL and 1 hop.
     * 
     * @param request A ping packet we received
     * @param handler The remote computer that sent it to us over a TCP socket Gnutella connection, and will get the pongs in response
     */
    private void handleCrawlerPing(PingRequest m, ReplyHandler handler) {

        /*
         * TODO: why is this any different than the standard pong?  In other
         * words, why no ultrapong marking, proper address calculation, etc?
         */

        /*
         * send the pongs for leaves
         */

        // Loop through all of our leaves
        List leafConnections = _manager.getInitializedClientConnections(); // Returns a List of ManagedConnection objects
        for (Iterator iterator = leafConnections.iterator(); iterator.hasNext(); ) {
            ManagedConnection connection = (ManagedConnection)iterator.next();

            // Make a pong to describe the leaf
            PingReply pr = PingReply.createExternal(      // Call createExternal() to make a pong that doesn't have information about us
                m.getGUID(),                              // Give the pong the ping's GUID to route it back
                (byte)2,                                  // Set the TTL to 2
                connection.getPort(),                     // The leaf's port number
                connection.getInetAddress().getAddress(), // The leaf's IP address
                false);                                   // This is a leaf, so don't include the GGEP "UP" extension

            // Hop the pong to make it look like it came from our leaf
            pr.hop(); // Now it has 1 TTL and 1 hop

            // Send the pong back to the computer we got the ping from
            sendPingReply(pr, handler);
        }

        /*
         * pongs for the neighbors will be sent by neighbors themselves
         * as ping will be broadcasted to them (since TTL=2)
         */
    }

    //do

    /**
     * 
     * 
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

    /**
     * 
     * @param responses
     * @param query
     * @param handler
     * @return
     */
    public boolean sendResponses(Response[] responses, QueryRequest query, ReplyHandler handler) {

        /*
         * This method needs to be public because the Peer-Server code uses it.
         */

        /*
         * if either there are no responses or, the
         * response array came back null for some reason,
         * exit this method
         */

        if ((responses == null) || ((responses.length < 1))) return false;

        /*
         * Here we can do a couple of things - if the query wants
         * out-of-band replies we should do things differently.  else just
         * send it off as usual.  only send out-of-band if you can
         * receive solicited udp AND not servicing too many
         * uploads AND not connected to the originator of the query
         */

        if (query.desiresOutOfBandReplies() &&
            !isConnectedTo(query, handler) &&
			RouterService.canReceiveSolicited() &&
            RouterService.getUploadManager().isServiceable() &&
            NetworkUtils.isValidAddressAndPort(query.getReplyAddress(), query.getReplyPort())) {

            /*
             * send the replies out-of-band - we need to
             * 1) buffer the responses
             * 2) send a ReplyNumberVM with the number of responses
             */

            if (bufferResponsesForLaterDelivery(query, responses)) {

                // special out of band handling....
                InetAddress addr = null;
                try {

                    addr = InetAddress.getByName(query.getReplyAddress());

                } catch (UnknownHostException uhe) {}

                int port = query.getReplyPort();

                if (addr != null) {

                    /*
                     * send a ReplyNumberVM to the host - he'll ACK you if he
                     * wants the whole shebang
                     */

                    int resultCount = (responses.length > 255) ? 255 : responses.length;
                    ReplyNumberVendorMessage vm = new ReplyNumberVendorMessage(new GUID(query.getGUID()), resultCount);
                    UDPService.instance().send(vm, addr, port);
                    return true;
                }

            } else {

                /*
                 * else i couldn't buffer the responses due to busy-ness, oh, scrap
                 * them.....
                 */

                return false;
            }
        }

        /*
         * send the replies in-band
         * -----------------------------
         */

        /*
         * convert responses to QueryReplies
         */

        Iterator iterator=responsesToQueryReplies(responses, query); // Iterator over QueryReply objects

        /*
         * send the query replies
         */

        try {

            while (iterator.hasNext()) {

                QueryReply queryReply = (QueryReply)iterator.next();
                sendQueryReply(queryReply);
            }

        } catch (IOException e) {

            /*
             * if there is an error, do nothing..
             */
        }

        /*
         * -----------------------------
         */

        return true;
    }

    /**
     * 
     * Returns whether or not we are connected to the originator of this query.
     * PRE: assumes query.desiresOutOfBandReplies == true
     * 
     * @param query
     * @param handler
     * @return
     */
    private final boolean isConnectedTo(QueryRequest query, ReplyHandler handler) {

        return query.matchesReplyAddress(handler.getInetAddress().getAddress());
    }

    /**
     * 
     * 
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

        /*
         * pick the right address & port depending on multicast & fwtrans
         * if we cannot find a valid address & port, exit early.
         */

        int port = -1;
        byte[] ip = null;

        /*
         * first try using multicast addresses & ports, but if they're
         * invalid, fallback to non multicast.
         */

        if (isFromMcast) {

            ip = RouterService.getNonForcedAddress();
            port = RouterService.getNonForcedPort();

            if (!NetworkUtils.isValidPort(port) || !NetworkUtils.isValidAddress(ip)) isFromMcast = false;
        }

        if (!isFromMcast) {

            /*
             * see if we have a valid FWTrans address.  if not, fall back.
             */

            if (isFWTransfer) {

                port = UDPService.instance().getStableUDPPort();
                ip = RouterService.getExternalAddress();
                if (!NetworkUtils.isValidAddress(ip) || !NetworkUtils.isValidPort(port)) isFWTransfer = false;
            }

            /*
             * if we still don't have a valid address here, exit early.
             */

            if (!isFWTransfer) {

                ip = RouterService.getAddress();
                port = RouterService.getPort();

                if (!NetworkUtils.isValidAddress(ip) || !NetworkUtils.isValidPort(port)) return Collections.EMPTY_LIST;
            }
        }

        /*
         * get the xml collection string...
         */

        String xmlCollectionString = LimeXMLDocumentHelper.getAggregateString(res);

        if (xmlCollectionString == null) xmlCollectionString = "";

        byte[] xmlBytes = null;
        try {

            xmlBytes = xmlCollectionString.getBytes("UTF-8");

        } catch (UnsupportedEncodingException ueex) {

            /*
             * no support for utf-8??
             * all implementations of java must support utf8 encoding
             * here we will allow this QueryReply to be sent out 
             * with xml being empty rather than not allowing the
             * Query to be sent out 
             * therefore we won't throw a IllegalArgumentException but we will
             * show it so the error will be sent to Bug servlet
             */

            ErrorService.error(ueex, "encountered UnsupportedEncodingException in creation of QueryReply : xmlCollectionString : " + xmlCollectionString);
        }

        /*
         * get the *latest* push proxies if we have not accepted an incoming
         * connection in this session
         */

        boolean notIncoming = !RouterService.acceptedIncomingConnection();
        Set proxies = (notIncoming ? _manager.getPushProxies() : null);

        /*
         * it may be too big....
         */

        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {

            /*
             * ok, need to partition responses up once again and send out
             * multiple query replies.....
             */

            List splitResps = new LinkedList();
            splitAndAddResponses(splitResps, res);

            while (!splitResps.isEmpty()) {

                Response[] currResps = (Response[])splitResps.remove(0);
                String currXML = LimeXMLDocumentHelper.getAggregateString(currResps);

                byte[] currXMLBytes = null;
                try {

                    currXMLBytes = currXML.getBytes("UTF-8");

                } catch (UnsupportedEncodingException ueex) {

                    /*
                     * all implementations of java must support utf8 encoding
                     * so if we get here there was something really wrong
                     * we will show the error but treat as if the currXML was
                     * empty (see the try catch for uee earlier)
                     */

                    ErrorService.error(ueex, "encountered UnsupportedEncodingException : currXML " + currXML);
                    currXMLBytes = "".getBytes();
                }

                if ((currXMLBytes.length > QueryReply.XML_MAX_SIZE) && (currResps.length > 1)) splitAndAddResponses(splitResps, currResps);

                else {

                    /*
                     * create xml bytes if possible...
                     */

                    byte[] xmlCompressed = null;

                    if ((currXML != null) && (!currXML.equals(""))) xmlCompressed = LimeXMLUtils.compress(currXMLBytes);
                    else                                            xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY; //there is no XML

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

        } else {

            /*
             * xml is small enough, no problem.....
             */

            /*
             * get xml bytes if possible....
             */

            byte[] xmlCompressed = null;
            if (xmlCollectionString != null && !xmlCollectionString.equals("")) xmlCompressed = LimeXMLUtils.compress(xmlBytes);
            else xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY; //there is no XML

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

    /**
     * 
     * @return Simply splits the input array into two (almost) equally sized
     * arrays.
     * 
     * @param in
     * @return
     */
    private Response[][] splitResponses(Response[] in) {

        int middle = in.length / 2;
        Response[][] retResps = new Response[2][];
        retResps[0] = new Response[middle];
        retResps[1] = new Response[in.length - middle];
        for (int i = 0; i < middle; i++) retResps[0][i] = in[i];
        for (int i = 0; i < (in.length - middle); i++) retResps[1][i] = in[i+middle];
        return retResps;
    }

    /**
     * 
     * @param addTo
     * @param toSplit
     */
    private void splitAndAddResponses(List addTo, Response[] toSplit) {

        Response[][] splits = splitResponses(toSplit);
        addTo.add(splits[0]);
        addTo.add(splits[1]);
    }
}
