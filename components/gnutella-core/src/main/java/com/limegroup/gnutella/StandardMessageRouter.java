
// Commented for the Learning branch

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
 * The StandardMessageRouter class contains the code that gets ping and query packets, and makes and sends pong and query hits in response.
 * 
 * Pings and Pongs:
 * respondToPingRequest()     Responds to a ping a remote computer sent us with a pong about us, and 6 more pongs we've cached.
 * respondToUDPPingRequest()  When a remote computer sends a ping in a UDP packet, respond with a single pong about us.
 * 
 * The Crawler:
 * handleCrawlerPing()        Responds to a crawler ping with a pong about each one of our leaves.
 * 
 * Search:
 * respondToQueryRequest()    See which of our shared files match a given query packet, and generate and send query hit packets in response.
 * sendResponses()            Convert a query packet we received and our Response objects that match, generate and send query hit packets in response.
 * isConnectedTo()            Determine if the computer that sent us a query packet made it, or just forwarded it.
 * createQueryReply()         Given a list of Response objects, compose query hit packets with 32 KB or less XML in each.
 * 
 * StandardMessageRouter extends MessageRouter to provide code for the methods that MessageRouter marks abstract.
 * StandardMessageRouter is the only class in LimeWire which extends MessageRouter.
 * LimeWire never makes a MessageRouter object, and just makes one StandardMessageRouter.
 */
public class StandardMessageRouter extends MessageRouter {

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

    /**
     * See which of our shared files match a given query packet, and generate and send query hit packets in response.
     * 
     * Uses FileManager.query(queryRequest) to see which of our shared files match the given search.
     * Calls sendResponse() to generate and send query hit packets.
     * 
     * @param queryRequest A query packet that wants to search our shared files
     * @param clientGUID   Not used, our client ID GUID that uniquely identifies us on the Gnutella network
     * @param handler      The remote computer that sent it to us, a ManagedConnection or UDPReplyHandler object
     */
    protected boolean respondToQueryRequest(QueryRequest queryRequest, byte[] clientGUID, ReplyHandler handler) {

        // If the query has the "WH" feature query GGEP extension, make sure it's not later than 1, the highest feature we understand
        if (!FeatureSearchData.supportsFeature( // Returns true if given 0 or 1, false on 2 or higher
            queryRequest.getFeatureSelector())) // The byte value of this query's GGEP "WH" extension, or 0 if it doesn't have it
            return false;                       // This is a query from the future we don't understand, ignore it

        // If the query has GGEP "WH" with a value of 1, this is a What's New query
        if (queryRequest.isWhatIsNewRequest()) ReceivedMessageStat.WHAT_IS_NEW_QUERY_MESSAGES.incrementStat();

        /*
         * Only send results if we're not busy.  Note that this ignores
         * queue slots -- we're considered busy if all of our "normal"
         * slots are full.  This allows some spillover into our queue that
         * is necessary because we're always returning more total hits than
         * we have slots available.
         */

        // If we don't have any upload slots, don't return any query hits
        if (!RouterService.getUploadManager().isServiceable()) return false;

        /*
         * Ensure that we have a valid IP & Port before we send the response.
         * Otherwise the QueryReply will fail on creation.
         */

        // Make sure we know our IP address and port number
        if (!NetworkUtils.isValidPort(RouterService.getPort()) || !NetworkUtils.isValidAddress(RouterService.getAddress())) return false;

        /*
         * Tour Point
         * 
         * Here's where we see which of our shared files match a remote computer's search.
         */

        // Have the FileManager see which of our shared files match the search in the query packet
        Response[] responses = RouterService.getFileManager().query(queryRequest); // Returns an array of Response objects, each describing a file that matches

        // If we're a leaf, record some statistics
        if (RouterService.isShieldedLeaf() && // We have some connections up to ultrapeers, making us a leaf on the Gnutella network, and
            queryRequest.isTCP()) { // We got this query through a TCP socket Gnutella connection
            if (responses != null && responses.length > 0) RoutedQueryStat.LEAF_HIT.incrementStat(); // As a leaf, we got a query that matched some of our shared files
            else RoutedQueryStat.LEAF_FALSE_POSITIVE.incrementStat(); // As a leaf, we got a query that didn't match anything we're sharing
        }

        // Call the next method
        return sendResponses(responses, queryRequest, handler);
    }

    /**
     * Given a query packet we've received and the Response objects of our shared files that match, generate and send query hit packets in response.
     * Uses responsesToQueryReplies(responses, query) to package the given Respone objects into new QueryReply packets with information about us.
     * 
     * There are 3 computers involved at this point:
     * The searching computer, which made the query packet and wants the results.
     * The relaying computer, which sent us the query packet.
     * Us, we got the query packet and have file hits to get back to the searching computer.
     * 
     * The searching computer hid its IP address and port number in the message GUID.
     * The relaying computer is the ReplyHandler handler.
     * 
     * There are 2 ways this method gets our hits back to the searching computer.
     * It buffers them for later and sends them out of band, using LimeWire's dynamic querying system and the ReplyNumberVendorMessage.
     * It sends them back through the Gnutella network the old fashioned way.
     * 
     * This method is public, but only the local method respondToQueryRequest() above calls it.
     * 
     * @param responses An array of Response objects representing the files we're sharing that match a query packet we received
     * @param query     The query packet we received
     * @param handler   The remote computer that sent it to us, and to which we'll send our reply query hit packets to
     * @return          True if we buffered or sent our reply, false if we couldn't buffer it and and dropped it
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

        // If FileManager.query(query) didn't return any Response objects, we have nothing to report, leave now
        if ((responses == null) || ((responses.length < 1))) return false;

        /*
         * Here we can do a couple of things - if the query wants
         * out-of-band replies we should do things differently.  else just
         * send it off as usual.  only send out-of-band if you can
         * receive solicited udp AND not servicing too many
         * uploads AND not connected to the originator of the query
         */

        // Buffer the responses for later, and then send them out of band
        if (query.desiresOutOfBandReplies()                  && // The searching computer is externally contactable for UDP, look for 0x04 in the speed flags bytes
            !isConnectedTo(query, handler)                   && // The computer that sent us this query isn't the computer that made it, so in band won't be fast
			RouterService.canReceiveSolicited()              && // We can receive solicited UDP packets
            RouterService.getUploadManager().isServiceable() && // We have a free upload slot
            NetworkUtils.isValidAddressAndPort(                 // The IP address and port number hidden in the query packet's GUID aren't 0
                query.getReplyAddress(),                        // The IP address hidden in the query packet's message GUID
                query.getReplyPort())) {                        // The port number hidden 13 bytes into the query packet's message GUID

            /*
             * send the replies out-of-band - we need to
             * 1) buffer the responses
             * 2) send a ReplyNumberVM with the number of responses
             */

            // Add the query and responses to the MessageRouter's buffer to send later
            if (bufferResponsesForLaterDelivery(query, responses)) {

                /*
                 * special out of band handling....
                 */

                // Get the searching computer's IP address and port number, which it hid in the GUID of the query packet it made
                InetAddress addr = null;
                try { addr = InetAddress.getByName(query.getReplyAddress()); } catch (UnknownHostException uhe) {}
                int port = query.getReplyPort();
                if (addr != null) { // We read the address from the GUID

                    /*
                     * send a ReplyNumberVM to the host - he'll ACK you if he
                     * wants the whole shebang
                     */

                    // Tell the searching computer how many file hits we have in a ReplyNumberVendorMessage
                    int resultCount = (responses.length > 255) ? 255 : responses.length; // Don't say more than 255
                    ReplyNumberVendorMessage vm = new ReplyNumberVendorMessage(new GUID(query.getGUID()), resultCount);
                    UDPService.instance().send(vm, addr, port);
                    
                    // Report true, we buffered our reply to send it later
                    return true;
                }

            // The MessageRouter's buffer is full, or we couldn't read the address
            } else {

                /*
                 * else i couldn't buffer the responses due to busy-ness, oh, scrap
                 * them.....
                 */

                // Report false, we dropped our replies
                return false;
            }
        }

        /*
         * send the replies in band
         */

        // Package the Response objects in groups of 10 into QueryReply packets with information about us
        Iterator iterator = responsesToQueryReplies(responses, query); // Returns an Iterator we can move over the QueryReply objects

        // Send the QueryReply packets back through the Gnutella network
        try {

            // Loop for each QueryReply packet we made
            while (iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();

                // Send it back to the computer that sent us the query it's in reply to
                sendQueryReply(queryReply);
            }

        // We couldn't find where to send it in the RouteTable, just keep going
        } catch (IOException e) {}

        // Report true, we sent our reply
        return true;
    }

    /**
     * Determine if the computer that sent us a query packet made it, or just forwarded it.
     * 
     * We got a query packet from a remote computer.
     * But, we don't know if that computer made the packet and sent it to us, or received the packet and forwarded it to us.
     * This method will tell us which one it was.
     * 
     * Looks in the first 4 bytes of the query packet's message GUID.
     * An IP address may be hidden there, the IP address of the computer that made the packet.
     * If the IP addresses match, the computer made the packet.
     * If they don't match, the computer forwarded the packet.
     * 
     * If the remote computer that sent us this packet also made it, we can respond without worrying about using a lot of bandwidth through multiple hops home.
     * 
     * Only sendResponses() above calls this.
     * 
     * @param query   A query packet
     * @param handler The remote computer that sent it to us
     * @return        True if the remote computer's IP address is hidden in the first 4 bytes of the query packet's message GUID, indicating it made it.
     *                False if the data doesn't match, indicating the computer received and forwarded the query from another computer to us.
     */
    private final boolean isConnectedTo(QueryRequest query, ReplyHandler handler) {

        // Look for the remote computer's IP address in the first 4 bytes of the query packet's message GUID
        return                                      // (3) Return true
            query.matchesReplyAddress(              // (2) If that IP address is hidden in the first 4 byts of the query's message GUID
            handler.getInetAddress().getAddress()); // (1) Get the IP address of the remote computer that sent us this query packet
    }

    /**
     * Given a list of Response objects, compose QueryResponse packets with 32 KB or less XML in each.
     * Only MessageRouter.responsesToQueryReplies() calls this.
     * 
     * Takes information to put in the query hit packet.
     * Determines our IP address and port number to write into the packet.
     * Composes XML based on all the files.
     * If the XML is larger than 32 KB, loops to group fewer and fewer results into more and more packets to get under this limit.
     * 
     * @param guid          For the header, the message GUID
     * @param ttl           For the header, the message TTL
     * @param speed         For the payload, our upload speed
     * @param res           An array of Response objects that each describe a file we're sharing that matches the search we received
     * @param clientGUID    For the end, our client ID GUID that will let a downloader get a push packet to us, not used, we just use _clientGUID instead
     * @param busy          Sets 0x04 in the flags and controls bytes, all our upload slots are full right now
     * @param uploaded      Sets 0x08 in the flags and controls bytes, we have actually uploaded a file
     * @param measuredSpeed Sets 0x10 in the flags and controls bytes, the given upload speed is from real measured data, not just a setting the user entered
     * @param isFromMcast   Makes "MCAST" in the GGEP block, this query hit is responding to a multicast query
     * @param isFWTransfer  Makes "FW" in the GGEP block, we can do a firewall-to-firewall file transfer
     * @return              A list of query reply packets to send
     */
    protected List createQueryReply(byte[] guid, byte ttl, long speed, Response[] res, byte[] clientGUID, boolean busy, boolean uploaded, boolean measuredSpeed, boolean isFromMcast, boolean isFWTransfer) {

        // A List and a reference for the query reply packets we make
        List queryReplies = new ArrayList(); // We'll keep the QueryReply objects we make in this List
        QueryReply queryReply = null;        // We'll point queryReply at the one we're working on

        /*
         * pick the right address & port depending on multicast & fwtrans
         * if we cannot find a valid address & port, exit early.
         */

        // Choose the IP address and port number we'll tell recipients of these query hit packets they can contact us at
        int port = -1;
        byte[] ip = null;

        /*
         * first try using multicast addresses & ports, but if they're
         * invalid, fallback to non multicast.
         */

        // The query we're preparing replies for arrived in a multicast UDP packet from another computer on our LAN
        if (isFromMcast) {

            // Get our internal LAN IP address and port number
            ip = RouterService.getNonForcedAddress();
            port = RouterService.getNonForcedPort();
            if (!NetworkUtils.isValidPort(port) || !NetworkUtils.isValidAddress(ip)) isFromMcast = false; // Make sure they're not 0
        }

        // This is a regular query from the Internet
        if (!isFromMcast) { // Or, we didn't know our LAN address above

            /*
             * see if we have a valid FWTrans address.  if not, fall back.
             */

            // The caller requested that we indicate firewall-to-firewall transfer support in the query hits we make here
            if (isFWTransfer) {

                // Get our external Internet IP address and port number
                port = UDPService.instance().getStableUDPPort(); // Is this different than RouterService.getPort() (ask)
                ip = RouterService.getExternalAddress();
                if (!NetworkUtils.isValidAddress(ip) || !NetworkUtils.isValidPort(port)) isFWTransfer = false; // Make sure they're not 0
            }

            /*
             * if we still don't have a valid address here, exit early.
             */

            // The caller doesn't want to advertise our firewall-to-firewall ability
            if (!isFWTransfer) {

                // Get our external Internet IP address and port number
                ip = RouterService.getAddress();
                port = RouterService.getPort();
                if (!NetworkUtils.isValidAddress(ip) || !NetworkUtils.isValidPort(port)) return Collections.EMPTY_LIST; // Make sure they're not 0
            }
        }

        /*
         * get the xml collection string...
         */

        // Compose XML based on all the given Response objects we'll include in the query hit packet
        String xmlCollectionString = LimeXMLDocumentHelper.getAggregateString(res); // Takes the array of Response objects and returns a String of XML
        if (xmlCollectionString == null) xmlCollectionString = ""; // Replace null with a blank String
        byte[] xmlBytes = null;
        try {

            // Express the text as ASCII bytes
            xmlBytes = xmlCollectionString.getBytes("UTF-8"); // UTF-8 is ASCII

        // The computer the program is running on doesn't support UTF-8 encoding
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

            // Give the error to the ErrorService, but keep going with xmlBytes null
            ErrorService.error(ueex, "encountered UnsupportedEncodingException in creation of QueryReply : xmlCollectionString : " + xmlCollectionString);
        }

        /*
         * get the *latest* push proxies if we have not accepted an incoming
         * connection in this session
         */

        // Determine if we can accept incoming TCP socket connections, or we can't and will need a push message
        boolean notIncoming = !RouterService.acceptedIncomingConnection(); // Will set 0x01 in the flags and controls bytes
        Set proxies = (                 // Will set "PUSH" in the GGEP block
            notIncoming ?               // If we're firewalled,
            _manager.getPushProxies() : // Get a list of up to 4 of our push proxies, ultrapeers we're connected up to that sent us a PushProxyAcknowledgement vendor message
            null);                      // Otherwise, leave proxies null

        /*
         * it may be too big....
         */

        // Generating XML for all of the Response objects made more than 32 KB of it
        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {

            /*
             * ok, need to partition responses up once again and send out
             * multiple query replies.....
             */

            // Divide the Response objects in res into 2 equally sized lists, and add the 2 lists to splitResps
            List splitResps = new LinkedList();
            splitAndAddResponses(splitResps, res);

            // Loop until splitResps doesn't have any more arrays of Response objects left
            while (!splitResps.isEmpty()) {

                // Get the first array of Response objects from the splitResps list, and generate XML for just those responses
                Response[] currResps = (Response[])splitResps.remove(0);              // Take out the first list
                String currXML = LimeXMLDocumentHelper.getAggregateString(currResps); // Generate XML based on it
                byte[] currXMLBytes = null;
                try {
                    currXMLBytes = currXML.getBytes("UTF-8");                         // Convert the String to ASCII bytes 
                } catch (UnsupportedEncodingException ueex) {                         // The computer can't do ASCII encoding
                    ErrorService.error(ueex, "encountered UnsupportedEncodingException : currXML " + currXML);
                    currXMLBytes = "".getBytes();                                     // Just leave the XML out
                }

                // We still have more than 32 KB of XML generated for 2 or more Response objects
                if ((currXMLBytes.length > QueryReply.XML_MAX_SIZE) && (currResps.length > 1)) {

                    // Divide the Response objects in currResps into 2 equally sized lists, and add the 2 lists to splitResps
                    splitAndAddResponses(splitResps, currResps); // Adds 2 more arrays of Response objects to splitResps, keeping the loop going 2 more times

                // The XML is small enough now
                } else {

                    /*
                     * create xml bytes if possible...
                     */

                    // Compress the XML
                    byte[] xmlCompressed = null;
                    if ((currXML != null) && (!currXML.equals(""))) xmlCompressed = LimeXMLUtils.compress(currXMLBytes);
                    else                                            xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY; // There is no XML

                    // Make a new query hit packet for us to send about files we're sharing with the subset of responses and the XML they generated
                    queryReply = new QueryReply(
                        guid,          // Passed in, for the header, the message GUID
                        ttl,           // Passed in, for the header, the message TTL
                        port,          // For the payload, our port number
                        ip,            // For the payload, our IP address
                        speed,         // Passed in, for the payload, our upload speed
                        currResps,     // For the payload, the number of file hit results in this packet
                        _clientGUID,   // Write our client ID GUID into the last 16 bytes of the query hit packet to let downloaders get push requests to us
                        xmlCompressed, // For the QHD, the bytes of compressed XML
                        notIncoming,   // Sets 0x01 in the flags and controls bytes, we can't accept TCP connections and will need a push message
                        busy,          // Sets 0x04 in the flags and controls bytes, all our upload slots are full right now
                        uploaded,      // Sets 0x08 in the flags and controls bytes, we have actually uploaded a file
                        measuredSpeed, // Sets 0x10 in the flags and controls bytes, the speed in this packet is from real measured data, not just a setting the user entered
                        ChatSettings.CHAT_ENABLED.getValue(), // For the chat byte, we can chat, makes the chat byte 1 and not 0
                        isFromMcast,   // Makes "MCAST" in the GGEP block, this query hit is responding to a multicast query
                        isFWTransfer,  // Makes "FW" in the GGEP block, we can do a firewall-to-firewall file transfer
                        proxies);      // Makes "PUSH" in the GGEP block, a Set of objects that implement IpPort with the addresses of our push proxies

                    // Add the query hit packet to the list we'll send
                    queryReplies.add(queryReply);
                }
            }

        // The XML is less than 32 KB
        } else {

            /*
             * xml is small enough, no problem.....
             */

            /*
             * get xml bytes if possible....
             */

            // Compress the XML
            byte[] xmlCompressed = null;
            if (xmlCollectionString != null && !xmlCollectionString.equals("")) xmlCompressed = LimeXMLUtils.compress(xmlBytes);
            else                                                                xmlCompressed = DataUtils.EMPTY_BYTE_ARRAY; // There is no XML

            // Make a new query hit packet for us to send about files we're sharing
            queryReply = new QueryReply(
                guid,          // Passed in, for the header, the message GUID
                ttl,           // Passed in, for the header, the message TTL
                port,          // For the payload, our port number
                ip,            // For the payload, our IP address
                speed,         // Passed in, for the payload, our upload speed
                res,           // The number of file hit results in this packet
                _clientGUID,   // Write our client ID GUID into the last 16 bytes of the query hit packet to let downloaders get push requests to us
                xmlCompressed, // For the QHD, the bytes of compressed XML
                notIncoming,   // Sets 0x01 in the flags and controls bytes, we can't accept TCP connections and will need a push message
                busy,          // Sets 0x04 in the flags and controls bytes, all our upload slots are full right now
                uploaded,      // Sets 0x08 in the flags and controls bytes, we have actually uploaded a file
                measuredSpeed, // Sets 0x10 in the flags and controls bytes, the upload speed is from real measured data, not just a setting the user entered
                ChatSettings.CHAT_ENABLED.getValue(), // For the chat byte, we can chat, makes the chat byte 1 and not 0
                isFromMcast,   // Makes "MCAST" in the GGEP block, this query hit is responding to a multicast query
                isFWTransfer,  // Makes "FW" in the GGEP block, we can do a firewall-to-firewall file transfer
                proxies);      // Makes "PUSH" in the GGEP block, a Set of objects that implement IpPort with the addresses of our push proxies

            // Add the query hit packet to the list we'll send
            queryReplies.add(queryReply);
        }

        // Return the list of query hit packets we made
        return queryReplies;
    }

    /**
     * Split the given array into 2 equally sized arrays.
     * Only splitAndAddResponses() below calls this.
     * 
     * @param in An array of Response objects that represent file information blocks in a query hit packet
     * @return   An array of 2 Response object arrays
     */
    private Response[][] splitResponses(Response[] in) {

        // Make an array of 2 arrays to hold the given Response objects
        int middle = in.length / 2;                     // Calculate how many Response objects to put in the first of our 2 arrays
        Response[][] retResps = new Response[2][];      // Make an array that holds 2 arrays
        retResps[0] = new Response[middle];             // Make the first array able to hold the calculated half of the number of items
        retResps[1] = new Response[in.length - middle]; // Set up the second array to hold the rest of the items

        // Copy in the Response object
        for (int i = 0; i < middle; i++) retResps[0][i] = in[i];
        for (int i = 0; i < (in.length - middle); i++) retResps[1][i] = in[i + middle];

        // Return the array of 2 arrays we made
        return retResps;
    }

    /**
     * Splits toSplit into 2 arrays, and adds them to addTo.
     * 
     * @param addTo   A List of arrays of Response objects
     * @param toSplit An array of Response objects
     */
    private void splitAndAddResponses(List addTo, Response[] toSplit) {

        // Split the given toSplit array of Response objects into 2 arrays
        Response[][] splits = splitResponses(toSplit); // Returns an array of 2 arrays

        // Add the 2 arrays to the addTo List
        addTo.add(splits[0]);
        addTo.add(splits[1]);
    }
}
