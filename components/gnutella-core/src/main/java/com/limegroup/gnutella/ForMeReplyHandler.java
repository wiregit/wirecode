
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.FixedsizeForgetfulHashMap;
import com.limegroup.gnutella.util.IntWrapper;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * LimeWire puts the ForMeReplyHandler object in a RouteTable to get the pong and query hit packets that are responses to our pings and queries.
 * ForMeReplyHandler implements the ReplyHandler interface, just like ManagedConnection and UDPReplyHandler.
 * 
 * There is only one ForMeReplyHandler object as LimeWire runs.
 * The program saves it as MessageRouter.FOR_ME_REPLY_HANDLER, and uses it in the MessageRouter class.
 */
public final class ForMeReplyHandler implements ReplyHandler {

    /** Make a debugging log we can write lines of text to as the program runs. */
    private static final Log LOG = LogFactory.getLog(ForMeReplyHandler.class);

    /**
     * Counts how many times push packets have asked us to push open new connections to different IP addresses.
     * This lets handlePushRequest() notice if we get more than 5 requests to push open a connection to a single IP address in the same 30 second period.
     * 
     * PUSH_REQUESTS is a Map of String to IntWrapper objects.
     * Its String values are IP addresses, like "1.2.3.4".
     * Its LimeWire IntWrapper objects hold the number of times we've been asked to push open a connection to that IP address.
     * 
     * PUSH_REQUESTS is a FixedsizeForgetfulHashMap that holds up to 200 pairings.
     * When it overflows, it throws out the object that was added first.
     * 
     * The RouterService runs code in the constructor every 30 seconds that clears this list completely.
     */
    private final Map PUSH_REQUESTS = Collections.synchronizedMap(new FixedsizeForgetfulHashMap(200));

    /**
     * Keeps track of which push packets we've already pushed connections open for.
     * This lets handlePushRequest() notice if the same push packet comes in a second time.
     * 
     * GUID_REQUESTS is a Map of GUID to GUID objects.
     * handlePushRequest() uses it to make sure we don't push for the same packet twice.
     * Each push packet has a message GUID that marks the Gnutella packet unique.
     * handlePushRequest() adds the GUID as the key and value.
     * The put(guid, guid) method returns an object already stored under that GUID key.
     * If it gets an object and not null, we know we've seen this packet before.
     * 
     * GUID_REQUESTS is a FixedsizeForgetfulHashMap that holds up to 200 pairings.
     * When it overflows, it throws out the object that was added first.
     * This Map isn't cleared on any timed schedule, it just grows to hold 200 GUIDs, and then starts to overflow.
     */
    private final Map GUID_REQUESTS = Collections.synchronizedMap(new FixedsizeForgetfulHashMap(200));

	/** Make the program's one ForMeReplyHandler object. */
	private static final ReplyHandler INSTANCE = new ForMeReplyHandler();

	/**
     * Get a reference to the program's single ForMeReplyHandler object.
     * MessageRouter.FOR_ME_REPLY_HANDLER uses this method to reference the program's ForMeReplyHandler object.
	 * 
	 * @return A reference to the program's single ForMeReplyHandler object
	 */
	public static ReplyHandler instance() {

        // Return the object we made and saved
		return INSTANCE;
	}

	/**
     * Make the ForMeReplyHandler to represent us in a RouteTable.
     * The constructor is marked private to make sure external code can't make a second ForMeReplyHandler.
     * It contains code that will get the RouterService to clear the PUSH_REQUESTS list every 30 seconds.
	 */
	private ForMeReplyHandler() {

	    // Define a new class right here that doesn't have a name but implements Java's Runnable interface, requiring it to have a run() method
	    RouterService.schedule(new Runnable() { // Schedule the RouterService to call the run() method every 30 seconds

            // The RouterService will have a thread call this run() method every 30 seconds
	        public void run() {

                // Clear our record of the IP addresses we've been told to push open file delivering connections to
	            PUSH_REQUESTS.clear();
	        }
	    },

        // Call this 30 seconds from now
        30 * 1000,

        // And every 30 seconds after that
        30 * 1000);
    }

    /**
     * By default, does nothing.
     * Only does something if SharingSettings.FREELOADER_ALLOWED is set to less than 100%.
     * If a remote computer sharing nothing connects to us, this method may close the connection.
     * The ReplyHandler interface requires this method.
     * 
     * @param pingReply A pong packet we received
     * @param handler   The ManagedConnection or UDPReplyHandler that represents the remote computer that sent us this pong
     */
	public void handlePingReply(PingReply pingReply, ReplyHandler handler) {

        /*
         * Kill incoming connections that don't share.  Note that we randomly
         * allow some freeloaders.  (Hopefully they'll get some stuff and then
         * share!)  Note that we only consider killing them on the first ping.
         * (Message 1 is their ping, message 2 is their reply to our ping.)
         */

	    /*
	     * By default, SharingSettings.FREELOADER_ALLOWED is 100.
	     * This turns the feature off, and makes this handlePingReply method do nothing.
	     */

        // If this remote computer connected to us and is sharing nothing, and settings allow it, disconnect from it
        if ((pingReply.getHops() <= 1)              && // The ping traveled one hop or less to get here, meaning the computer on the other end of this connection made it
            (handler.getNumMessagesReceived() <= 2) && // This remote computer hasn't sent us more than 2 packets yet, meaning this is their pong from our ping
            (!handler.isOutgoing())                 && // This remote computer connected to us
            (handler.isKillable())                  && // Our connection to this remote computer is a TCP socket Gnutella connection we could close
            (pingReply.getFiles() < SharingSettings.FREELOADER_FILES.getValue())           && // The pong says the remote computer is sharing 0 files
            ((int)(Math.random() * 100.f) > SharingSettings.FREELOADER_ALLOWED.getValue()) && // We've randomly selected to disconnect from this freeloader
            (handler instanceof ManagedConnection)  && // Make sure handler is actually a ManagedConnection object and not a UDPReplyHandler object
            (handler.isStable())) {                    // Only disconnect if we've been connected for more than 5 seconds

            // Disconnect from the remote computer that sent us this pong packet
			ConnectionManager cm = RouterService.getConnectionManager();
            cm.remove((ManagedConnection)handler);
        }
	}

    /**
     * Passes the packet to SearchResultHandler.handleQueryReply(reply) and then DownloadManager.handleQueryReply(reply).
     * The ReplyHandler interface requires this method.
     * 
     * @param reply   A query hit packet we received
     * @param handler The ManagedConnection or UDPReplyHandler that represents the remote computer that sent us this query hit
     */
	public void handleQueryReply(QueryReply reply, ReplyHandler handler) {

        // If the query hit packet doesn't make it through our personal SpamFilter, do nothing more here
		if (handler != null && handler.isPersonalSpam(reply)) return;

        // If this query hit is a response to a UDP multicast query we sent out on the LAN, make sure it's not on TCP and only hopped once
        if (reply.isReplyToMulticastQuery()) { // The GGEP block contains "MCAST"

            // And yet it made it here on a TCP Gnutella connection, leave now
            if (reply.isTCP()) return;

            // Make sure it only hopped once, right to here
            if (reply.getHops() != 1 || reply.getTTL() != 0) return;
        }

        // If this query hit arrived to us in a UDP packet
        if (reply.isUDP()) {

            // Get the UDPReplyHandler object that represents the computer that sent us this query hit packet
        	Assert.that(handler instanceof UDPReplyHandler);
        	UDPReplyHandler udpHandler = (UDPReplyHandler)handler;

            // Copy the remote computer's IP address from the UDPReplyHandler object into the query hit packet
        	reply.setOOBAddress(udpHandler.getInetAddress(), udpHandler.getPort()); // This overwrites information already in the query hit packet
        }

        /*
         * XML must be added to the response first, so that
         * whomever calls toRemoteFileDesc on the response
         * will create the cachedRFD with the correct XML.
         */

        // Add XML to the query hit packet about the shared files it describes (do)
        boolean validResponses = addXMLToResponses(reply);
        if (!validResponses) return; // Leave if addXMLToResponses() didn't like the packet (do)

        // Give the packet to SearchResultHandler.handleQueryReply(reply). (do)
		SearchResultHandler resultHandler = RouterService.getSearchResultHandler();
		resultHandler.handleQueryReply(reply);

        // Give the packet to DownloadManager.handleQueryReply(reply). (do)
		DownloadManager dm = RouterService.getDownloadManager();
		dm.handleQueryReply(reply);
	}

	/**
     * Match each part of the XML in the given query hit packet with the correct file information block in it.
     * Extracts the XML, decompresses it, parses it, splits it, and inserts the parts by calling response.setDocument().
     * handleQueryReply() above calls this.
     * 
     * @param qr A query hit packet we've received
     * @return   true if it worked, false if there was an error
	 */
    private boolean addXMLToResponses(QueryReply qr) {

        /*
         * get xml collection string, then get dis-aggregated docs, then
         * in loop
         * you can match up metadata to responses
         */

        // Make a string for the XML we'll decompress from the given query hit packet
        String xmlCollectionString = "";

        try {

            // Get the bytes of the compressed XML from the end of the query hit packet
            LOG.trace("Trying to do uncompress XML.....");
            byte[] xmlCompressed = qr.getXMLBytes();
            if (xmlCompressed.length > 1) {

                // Decompess it into a String
                byte[] xmlUncompressed = LimeXMLUtils.uncompress(xmlCompressed);
                xmlCollectionString = new String(xmlUncompressed, "UTF-8"); // UTF-8 is ASCII
            }

        // Java didn't like the "UTF-8" ASCII encoding
        } catch (UnsupportedEncodingException use) {

            /*
             * b/c this should never happen, we will show and error
             * if it ever does for some reason.
             * we won't throw a BadPacketException here but we will show it.
             * the uee will effect the xml part of the reply but we could
             * still show the reply so there shouldn't be any ill effect if
             * xmlCollectionString is ""
             */

            // Log the error, and keep on going
            ErrorService.error(use);

        // Ignore the exception and keep on going
        } catch (IOException ignored) {}

        // If there's no XML in this query hit, leave now
        if (xmlCollectionString == null || xmlCollectionString.equals("")) return true;

        // Get the Response objects the QueryReply parsed file information blocks into
        Response[] responses; // An array of Response objects that represent the file information blocks in the query hit packet
        int responsesLength;  // The length of the array and the number of shared files the packet has information about

        try {

            // Get the array of Response objects from the query hit packet, and save the number of Response objects in it
            responses = qr.getResultsArray();
            responsesLength = responses.length;

        // The QueryReply class wasn't able to parse the packet for file information blocks
        } catch (BadPacketException bpe) {

            // Nothing more to do with XML, leave
            LOG.trace("Unable to get responses", bpe);
            return false;
        }

        // Save the XML into the debugging log
        if (LOG.isDebugEnabled()) LOG.debug("xmlCollectionString = " + xmlCollectionString);

        // Parse the XML into a List of LimeXMLDocument objects, with one object for each file information block
        List allDocsArray = LimeXMLDocumentHelper.getDocuments(xmlCollectionString, responsesLength);

        /*
         * A QueryHit packet has file information blocks, and then a big string of XML at the end.
         * The QueryReply class parsed each file information block into a Response object.
         * Here, responses[] has responsesLength number of Response objects.
         * The LimeXMLDocumentHelper split the XML into separate LimeXMLDocument objects.
         * Here, allDocsArray has allDocsArray.size() number of LimeXMLDocument objects.
         * 
         * For each Response, loop through all the LimeXMLDocument objects.
         * Stop when we find a match.
         * Call response.setDocument(LimeXMLDocument) to load the matching schema in the Response object.
         */

        // Loop through the Response objects in the responses array
        for (int i = 0; i < responsesLength; i++) {
            Response response = responses[i];

            // Make an array that can hold LimeXMLDocument objects
            LimeXMLDocument[] metaDocs;

            // Loop through the LimeXMLDocument objects for each schema
            for (int schema = 0; schema < allDocsArray.size(); schema++) {
                metaDocs = (LimeXMLDocument[])allDocsArray.get(schema);

                // If there are no documents in this schema, try another
                if (metaDocs == null) continue;

                // If this schema had a document for this response, use it
                if (metaDocs[i] != null) {

                    // Save the found schema in the Response object
                    response.setDocument(metaDocs[i]);
                    break; // Each Response only needs one schema, so move to the next Response object
                }
            }
        }

        // We made it
        return true;
    }

    /**
     * Checks the push packet, and calls PushManager.acceptPushUpload() to have us push open the connection.
     * The ReplyHandler interface requires this method.
     * 
     * @param pushRequest A push packet addressed to our client ID GUID, commanding us to push open a connection and deliver a file
     * @param handler     The remote computer that sent us this packet
     */
    public void handlePushRequest(PushRequest pushRequest, ReplyHandler handler) {

        /*
         * If there are problems with the request, just ignore it.
         * There's no point in sending them a GIV to have them send a GET
         * just to return a 404 or Busy or Malformed Request, etc..
         */

        // Make sure this push request packet makes it through our personal SpamFilter
        if (handler.isPersonalSpam(pushRequest)) return;

        // Get the IP address we're supposed to push open a connection to
        byte[] ip = pushRequest.getIP();
        String h = NetworkUtils.ip2string(ip);

        // Make sure we haven't done this push already
        GUID guid = new GUID(pushRequest.getGUID()); // Get the message GUID of the push packet that marks it unique, not the client ID GUID the push is addressed to
        if (GUID_REQUESTS.put(guid, guid) != null) { // put() adds the GUID to the list, and returns the same GUID if it was already there

            // The GUID was already in our list, we've already pushed for this packet before
            return; // Leave without doing it again
        }

        /*
         * make sure the guy isn't hammering us
         */

        // This is the first time we've been asked to push open a connection to this IP address in this 30 second period
        IntWrapper i = (IntWrapper)PUSH_REQUESTS.get(h); // Look up the IP address in the PUSH_REQUESTS list
        if (i == null) {

            // Add the IP address to the PUSH_REQUESTS list with the number 1
            i = new IntWrapper(1);
            PUSH_REQUESTS.put(h, i);

        // We've been asked to push open a connection to this IP address in this 30 second period before
        } else {

            // Increment the number stored in the list
            i.addInt(1);

            // If we've pushed open 5 connections to this IP address in this 30 second period already, leave without pushing open a 6th
            if (i.getInt() > UploadSettings.MAX_PUSHES_PER_HOST.getValue()) return;
        }

        // Make sure we don't push open a connection to a banned IP address
        if (RouterService.getAcceptor().isBannedIP(ip)) return;

        // Get the port number we'll push a new connection open to
        int port = pushRequest.getPort();
        if (!NetworkUtils.isValidPort(port)) return; // Make sure its not 0

        // Get the client ID GUID of the computer that will do the push, which should be our client ID GUID
        String req_guid_hexstring = (new GUID(pushRequest.getClientGUID())).toString(); // Get it as base 16 text

        // Call PushManager.acceptPushUpload() to initiate a new TCP socket connection to the IP address and port number, and send "GIV" and the file
        RouterService.getPushManager().acceptPushUpload(
            h,                                     // The IP address to push a new connection open to, like "72.139.164.14"
            port,                                  // The port number to push a new connection open to
            req_guid_hexstring,                    // The client ID GUID that addressed the push to us, our client ID GUID, as base 16 text
            pushRequest.isMulticast(),             // If the remote computer is on the same LAN as us, upload the file even if we don't have a free upload slot
            pushRequest.isFirewallTransferPush()); // The ID in the push packet is the special code indicating we should open a firewall-to-firewall connection
    }

    /**
     * Determine if we can still send a packet with this connection.
     * Returns true, because we're always ready to handle replies
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
	public boolean isOpen() {

        /*
         * I'm always ready to handle replies.
         */

        // Yes, we can send a packet to ourselves
        return true;
	}

    /**
     * The number of Gnutella packets the computer this ReplyHandler represents has sent us.
     * Returns 0 because this makes no sense for the ForMeReplyHandler ReplyHandler, which is us.
     * The ReplyHandler interface requires this method.
     * 
     * return 0
     */
    public int getNumMessagesReceived() {

        // Return 0 because this ReplyHandler is us
        return 0;
    }

    /**
     * Does nothing.
     * Would count that we dropped this message.
     * The ReplyHandler interface requires this method.
     */
	public void countDroppedMessage() {}

    /**
     * Determine if we are an ultrapeer and this remote computer is a leaf.
     * Returns false, because ForMeReplyHandler represents ourselves.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
	public boolean isSupernodeClientConnection() {

        // This kind of relationship only happens between two different computers
        return false;
	}

    /**
     * Always returns false to let us show the given message to the user.
     * isPersonalSpam() is supposed to determine if the given Gnutella packet passes through our personal SpamFilter, letting us show its information to the user.
     * The ReplyHandler interface requires this method.
     * 
     * @param m A gnutella packet we've received
     * @return  True to hide it from the user, false if it's fine
     */
	public boolean isPersonalSpam(Message m) {

        // Always say it's fine
        return false;
	}

    /**
     * Not used.
     * Does nothing.
     * 
     * @param A PingReply object we could record in statistics
     */
	public void updateHorizonStats(PingReply pingReply) {

        /*
         * TODO:: we should probably actually update the stats with this pong
         */
    }

    /**
     * Determine if this is an outgoing connection we initiated.
     * Returns false, as a ForMeReplyHandler object represents ourselves.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
	public boolean isOutgoing() {

        // This is not an outgoing connection we initiated
		return false;
	}

    /**
     * Determine if you are allowed to close this connection.
     * Returns false, a ForMeReplyHandler object represents ourselves, so there is no connection to open or close.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
	public boolean isKillable() {

        // There is no connection to close
		return false;
	}

	/**
     * Determine if we are a leaf.
     * The ReplyHandler interface requires this method.
     * 
     * ForMeReplyHandler represents ourselves.
     * When we have the ForMeReplyHandler object handle a packet, we're sending it to ourselves.
     * isLeafConnection() determines if the computer on the other end of the connection is a leaf.
     * In this case, it determines if we are a leaf.
     * 
     * @return True if we are a leaf, false if we are an ultrapeer
	 */
	public boolean isLeafConnection() {

        // Return false if we're connected or trying to connect as an ultrapeer
        return !RouterService.isSupernode();
	}

	/**
     * Determine if this is a high degree connection, meaning it's an ultrapeer with 15 or more ultrapeer connections.
     * True when the remote compueter says "X-Degree: 15" or higher.
     * Returns false because this only makes sense for TCP Gnutella connections to remote computers.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
	 */
	public boolean isHighDegreeConnection() {

        // This only applies to Gnutella connections on TCP
		return false;
    }

    /**
     * Determine if this is a connection to an ultrapeer that can exchange query routing tables with other ultrapeers.
     * True when the remote computer says "X-Ultrapeer-Query-Routing: 0.1".
     * Returns false because this only makes sense for TCP Gnutella connections to remote computers.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
    public boolean isUltrapeerQueryRoutingConnection() {

        // This only applies to Gnutella connections on TCP
        return false;
    }

    /**
     * Determine if this is a connection to a computer that supports the advanced Gnutella features good ultrapeers need.
     * Returns false because this only makes sense for TCP Gnutella connections to remote computers.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
    public boolean isGoodUltrapeer() {

        // This only applies to Gnutella connections on TCP
        return false;
    }

    /**
     * Determine if this is a connection to a computer that supports the advanced Gnutella features good leaves need.
     * Returns false because this only makes sense for TCP Gnutella connections to remote computers.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
    public boolean isGoodLeaf() {

        // This only applies to Gnutella connections on TCP
        return false;
    }

    /**
     * Determine if this connection is to a computer that supports pong caching.
     * Returns true because we do.
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
    public boolean supportsPongCaching() {

        // We will cache the pongs we receive
        return true;
    }

    /**
     * Determine if we'll let this computer ping us right now.
     * Returns true, not placing a restriction on how frequently we can ping ourselves.
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
    public boolean allowNewPings() {

        // Always allow pings from ourselves
        return true;
    }

    /**
     * Get our Internet IP address.
     * Calling getInetAddress() on a ReplyHandler object will usually tell you the IP address of the remote computer the ReplyHandler sends packets to.
     * The ForMeReplyHandler represents us, so getInetAddress() returns our IP address.
     * 
     * @return Our IP address as a Java InetAddress object
     */
    public InetAddress getInetAddress() {

        try {

            // Ask the RouterService what our IP address is, convert it into a String, convert that into a InetAddress object, and return it
            return InetAddress.getByName(NetworkUtils.ip2string(RouterService.getAddress()));

        // Converting it into a InetAddress object didn't work
        } catch (UnknownHostException e) {

            /*
             * may want to do something else here if we ever use this!
             */

            // Return null instead of a InetAddress object
            return null;
        }
    }

    /**
     * Get our port number.
     * Calling getPort() on a ReplyHandler object will usually tell you the port number of the remote computer the ReplyHandler sends packets to.
     * The ForMeReplyHandler represents us, so getPort() returns our port.
     * 
     * @return Our port number
     */
    public int getPort() {

        // Ask the RouterService for our port number, and return it
        return RouterService.getPort();
    }

    /**
     * Get our Internet IP address.
     * Calling getInetAddress() on a ReplyHandler object will usually tell you the IP address of the remote computer the ReplyHandler sends packets to.
     * The ForMeReplyHandler represents us, so getInetAddress() returns our IP address.
     * 
     * @return Our IP address as a String like "216.27.158.74"
     */
    public String getAddress() {

        // Ask the RouterService for our IP address, convert it into a String, and return it
        return NetworkUtils.ip2string(RouterService.getAddress());
    }

    /**
     * Log an error because we were asked to send a packet to ourselves
     * The ReplyHandler interface requires this method.
     * 
     * @param vm The StatisticVendorMessge the program asked us to send to ourselves
     */
    public void handleStatisticVM(StatisticVendorMessage vm) {

        // Make a false assertion to record a line of error text (do)
        Assert.that(false, "ForMeReplyHandler asked to send vendor message");
    }

    /**
     * Log an error because we were asked to send a packet to ourselves
     * The ReplyHandler interface requires this method.
     * 
     * @param simppVM The SIMPP vendor message the program asked us to send to ourselves
     */
    public void handleSimppVM(SimppVM simppVM) {

        // Make a false assertion to record a line of error text (do)
        Assert.that(false, "ForMeReplyHandler asked to send vendor message");
    }

    /**
     * Determine if our connection with this computer is stable.
     * Returns true because the computer is us, so we should always be able to reach it.
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
    public boolean isStable() {

        // Report stable
        return true;
    }

    /**
     * Get the language preference of this packet handling computer.
     * Returns our language preference, because this ReplyHandler is us.
     * The ReplyHandler interface requires this method.
     * 
     * @return Our language preference from ApplicationSettings, like "en" for English.
     */
    public String getLocalePref() {

        // Get our language preference from ApplicationSettings
        return ApplicationSettings.LANGUAGE.getValue();
    }

    /**
     * Drop the message.
     * It doesn't make sense to send a message to ourselves.
     * The ReplyHandler interface requires this method.
     * 
     * @param m A Gnutella packet the caller wants us to send to ourselves
     */
    public void reply(Message m) {}

    /**
     * Get our Gnutella client ID GUID.
     * The ReplyHandler interface requires this method.
     * Call getClientGUID() on a ReplyHandler to find the client ID GUID of the remote computer the ReplyHandler represents.
     * This ForMeReplyHandler represents us, so getClientGUID() returns our client ID GUID from settings.
     * 
     * @return A 16 byte array with our Gnutella client ID GUID
     */
    public byte[] getClientGUID() {

        // Return our client ID GUID that uniquely identifies us on the Gnutella network
        return RouterService.getMyGUID();
    }
}
