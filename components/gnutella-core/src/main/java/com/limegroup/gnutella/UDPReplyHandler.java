
// Edited for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;

import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * This class is an implementation of <tt>ReplyHandler</tt> that is
 * specialized for handling UDP messages.
 */
public final class UDPReplyHandler implements ReplyHandler {

	/**
	 * Constant for the <tt>InetAddress</tt> of the host to reply to.
	 */
	private final InetAddress IP;

	/**
	 * Constant for the port of the host to reply to.
	 */
	private final int PORT;

	/**
	 * Constant for the <tt>UDPService</tt>.
	 */
	private static final UDPService UDP_SERVICE = UDPService.instance();
    
    /**
     * Used to filter messages that are considered spam.
     * With the introduction of OOB replies, it is important
     * to check UDP replies for spam too.
     *
     * Uses one static instance instead of creating a new
     * filter for every single UDP message.
     */
    private static volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();
	
	/**
	 * Constructor that sets the ip and port to reply to.
	 *
	 * @param ip the <tt>InetAddress</tt> to reply to
	 * @param port the port to reply to
	 */
	public UDPReplyHandler(InetAddress ip, int port) {
	    if(!NetworkUtils.isValidPort(port))
	        throw new IllegalArgumentException("invalid port: " + port);
	    if(!NetworkUtils.isValidAddress(ip))
	        throw new IllegalArgumentException("invalid ip: " + ip);
	       
		IP   = ip;
		PORT = port;
	}
    
    /**
     * Sets the new personal spam filter to be used for all UDPReplyHandlers.
     */
    public static void setPersonalFilter(SpamFilter filter) {
        _personalFilter = filter;
    }

    //done

	/**
     * Send the given pong in a UDP packet to this remote computer.
     * The ReplyHandler interface requires this method.
     * 
     * @param pong    The Gnutella pong packet to send
     * @param handler Not used
	 */
	public void handlePingReply(PingReply pong, ReplyHandler handler) {

        // Have the UDPService wrap the pong in a UDP packet and send it to the IP address and port number of this UDPReplyHandler
        UDP_SERVICE.send(pong, IP, PORT);
		SentMessageStatHandler.UDP_PING_REPLIES.addMessage(pong);
	}

	/**
     * Send the given query hit in a UDP packet to this remote computer.
     * The ReplyHandler interface requires this method.
     * 
     * @param pong    The Gnutella query hit packet to send
     * @param handler Not used
	 */
	public void handleQueryReply(QueryReply hit, ReplyHandler handler) {

        // Have the UDPService wrap the query hit in a UDP packet and send it to the IP address and port number of this UDPReplyHandler
        UDP_SERVICE.send(hit, IP, PORT);
		SentMessageStatHandler.UDP_QUERY_REPLIES.addMessage(hit);
	}

	/**
     * Send the given push request in a UDP packet to this remote computer.
     * The ReplyHandler interface requires this method.
     * 
     * @param request The Gnutella push request packet to send
     * @param handler Not used
	 */
	public void handlePushRequest(PushRequest request, ReplyHandler handler) {

        // Have the UDPService wrap the push in a UDP packet and send it to the IP address and port number of this UDPReplyHandler
        UDP_SERVICE.send(request, IP, PORT);
		SentMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(request);
	}

    /**
     * Does nothing.
     * Would count that we dropped this message.
     * The ReplyHandler interface requires this method.
     */
	public void countDroppedMessage() {}

    /**
     * Determine if the given Gnutella packet passes through our personal SpamFilter, letting us show its information to the user.
     * The ReplyHandler interface requires this method.
     * 
     * @param m A gnutella packet we've received
     * @return  True to hide it from the user, false if it's fine
     */
	public boolean isPersonalSpam(Message m) {

        // See if the Gnutella packet passes through our SpamFilter for messages to show the user
        return !_personalFilter.allow(m);
	}

    /**
     * Determine if we can still send a packet with this connection.
     * Returns true, because with UDP, there is no connection that can be lost or closed.
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
	public boolean isOpen() {

        // Yes, we can always send a UDP packet
		return true;
	}

    /**
     * The number of Gnutella packets this remote computer has sent us.
     * Returns 0 because with UDP, we don't keep count.
     * The ReplyHandler interface requires this method.
     * 
     * return 0
     */
	public int getNumMessagesReceived() {

        // Return 0 because we don't know
        return 0;
	}

    /**
     * Determine if this is an outgoing connection we initiated.
     * Returns false, as UDP doesn't have connections.
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
     * Returns false, there is no connection with UDP.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
    public boolean isKillable() {

        // There is no connection to close
        return false;
    }

	/**
     * Determine if we are an ultrapeer and this remote computer is a leaf.
     * Returns false, because leaves and ultrapeers only with TCP socket Gnutella connections, not UDP.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
	 */
	public boolean isSupernodeClientConnection() {

        // Only TCP socket connections can set up this relationship
		return false;
	}

	/**
     * Determine if our connection to this remote computer is down to a leaf.
     * Returns false because leaf connections only exist on TCP.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
	 */
	public boolean isLeafConnection() {

        // The leaf role only happens on TCP connections, not UDP packets we're sending a remote computer
        return false;
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

        // This is just for TCP Gnutella connections
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

        // This is just for TCP Gnutella connections
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
     * Returns false because we never did the Gnutella handshake with this remote computer, so we don't know.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
    public boolean supportsPongCaching() {

        // We don't know because there is no Gnutella handshake with UDP
        return false;
    }

    /**
     * Determine if we'll let this remote computer ping us right now.
     * Returns true, not placing the restriction on UDP pings that we place on pings we get through TCP Gnutella connections.
     * The ReplyHandler interface requires this method.
     * 
     * @return true
     */
    public boolean allowNewPings() {

        // Always allow UDP pings
        return true;
    }

    /**
     * Send the given StatisticVendorMessage to this remote computer.
     * Uses the IP address and port number saved in this UDPReplyHandler object.
     * The ReplyHandler interface requires this method.
     * 
     * @param m The StatisticVendorMessage to send to this remote computer
     */
    public void handleStatisticVM(StatisticVendorMessage m) throws IOException {

        // Get the UDPService object, and have it send the StatisticVendorMessage in a UDP packet to this UDPReplyHandler's IP address and port number
        UDPService.instance().send(m, IP, PORT);
    }

    /**
     * Does nothing.
     * SIMPP vendor messages are only sent through TCP Gnutella connections, not UDP.
     * The ReplyHandler interface requires this method.
     * 
     * @param simppVM The SIMPP vendor message the program asked us to send to this remote computer by UDP
     */
    public void handleSimppVM(SimppVM simppVM) {}

    //do
    
    // inherit doc comment
    public InetAddress getInetAddress() {
        return IP;
    }
    
    /**
     * Retrieves the host address.
     */
    public String getAddress() {
        return IP.getHostAddress();
    }

    //done
    
    /**
     * Determine if our connection with this computer is stable.
     * Returns false because UDP doesn't make any guarantee the computer will still be there.
     * The ReplyHandler interface requires this method.
     * 
     * @return false
     */
    public boolean isStable() {

        // Report not stable
        return false;
    }

    /**
     * Get the language preference of this packet handling computer.
     * This is information we get in the Gnutella handshake, so for UDP, we don't know.
     * Always returns "en" for English, because most computers on the Gnutella network have that default.
     * The ReplyHandler interface requires this method.
     * 
     * This method is implemented to meet the requirements of the ReplyHandler interface.
     * It is not used.
     * 
     * @return "en"
     */
    public String getLocalePref() {

        // Get the default locale for the entire Gnutella network, "en" for English
        return ApplicationSettings.DEFAULT_LOCALE.getValue();
    }

    //do
    
	/**
	 * Overrides toString to print out more detailed information about
	 * this <tt>UDPReplyHandler</tt>
	 */
	public String toString() {
		return ("UDPReplyHandler:\r\n"+
				IP.toString()+"\r\n"+
				PORT+"\r\n");
	}
	
	/**
	 * sends the response through udp back to the requesting party
	 */
	public void handleUDPCrawlerPong(UDPCrawlerPong m) {
		UDPService.instance().send(m, IP, PORT);
	}

    //done
    
    /**
     * Send the given Gnutella packet to this remote computer by UDP.
     * The ReplyHandler interface requires this method.
     * 
     * @param m The Gnutella packet to send
     */
	public void reply(Message m) {

        // Have the UDPService object send the Gnutella message in a UDP packet to the IP address and port number of this UDPReplyHandler
		UDPService.instance().send(m, IP, PORT);
	}

    //do
    
	public int getPort() {
		return PORT;
	}

    //done
    
    /**
     * Get the remote computer's Gnutella client ID GUID.
     * Returns 0s because we don't know what it is.
     * The ReplyHandler interface requires this method.
     * 
     * @return A 16 byte array filled with 0s, because we don't know
     */
	public byte[] getClientGUID() {

        // Return a 16 byte array filled with 0s
	    return DataUtils.EMPTY_GUID;
	}
}
