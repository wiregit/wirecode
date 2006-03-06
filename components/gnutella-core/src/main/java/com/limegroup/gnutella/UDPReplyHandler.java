
// Commented for the Learning branch

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
 * A UDPReplyHandler object represents a remote computer on the Internet running Gnutella software that we can send UDP packets to.
 * We'll wrap each Gnutella message in a UDP packet, and send it directly there.
 * When you make a UDPReplyHandler object, the constructor takes a the IP address and port number of the remote computer.
 * Call a method like handlePingReply(pong), and the method will send the remote computer the given pong packet.
 * 
 * This class implements the ReplyHandler interface.
 * Another important class that implements ReplyHandler is ManagedConnection.
 * A ManagedConnection object represents a remote computer on the Internet we can send packets to through our TCP socket Gnutella connection with it.
 * A UDPReplyHandler object represents a remote computer on the Internet we can send UDP packets to.
 * 
 * ManagedConnection and UDPReplyHandler implement the ReplyHandler interface so they can be placed in a RouteTable.
 */
public final class UDPReplyHandler implements ReplyHandler {

	/** The IP address of the remote computer this UDPReplyHandler will send packets to. */
	private final InetAddress IP;

	/** The port number of the remote computer this UDPReplyHandler will send packets to. */
	private final int PORT;

	/** A reference to the program's single UDPService object, which we'll use to send UDP packets. */
	private static final UDPService UDP_SERVICE = UDPService.instance();

    /** All UDPReplyHandler objects use this one static personal SpamFilter to check packets before sending them. */
    private static volatile SpamFilter _personalFilter = SpamFilter.newPersonalFilter();

	/**
     * Make a new UDPReplyHandler object that can wrap Gnutella packets into UDP packets and send them to an IP address and port number.
     * MessageRouter.handleMulticastMessage(Message, InetSocketAddress) and MessageRouter.handleUDPMessage(Message, InetSocketAddress) make UDPReplyHandler objects.
     * The given IP address and port number is the address we just got a UDP packet from.
     * The UDPReplyHandler this constructor makes will be ablet to send a reply packet back to the same address.
     * 
     * @param ip   The IP address to send packets to
     * @param port The port number to send packets to
	 */
	public UDPReplyHandler(InetAddress ip, int port) {

        // Make sure the port isn't 0 and the IP address doesn't start 0 or 255
	    if (!NetworkUtils.isValidPort(port))  throw new IllegalArgumentException("invalid port: " + port);
	    if (!NetworkUtils.isValidAddress(ip)) throw new IllegalArgumentException("invalid ip: "   + ip);

        // Save the address information in this new object
		IP   = ip;
		PORT = port;
	}

    /**
     * Set the personal SpamFilter that all UDPReplyHandler objects use to check packets before sending them.
     * _personalFilter is static, so every UDPReplyHandler object is using the same SpamFilter.
     * RouterService.adjustSpamFilters() calls this when the program's spam filters have changed. 
     * 
     * @param filter The new SpamFilter to use
     */
    public static void setPersonalFilter(SpamFilter filter) {

        // Save the given SpamFilter in the static member
        _personalFilter = filter;
    }

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
     * _personalFilter is static, so every UDPReplyHandler uses the same one.
     * Having the method here makes it look like it does checks specific to this remote computer, but it doesn't.
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

    /**
     * Get the IP address this UDPReplyHandler sends packets to.
     * 
     * @return The IP address as a Java InetAddress object
     */
    public InetAddress getInetAddress() {

        // Return the IP address the constructor saved
        return IP;
    }

    /**
     * Get the IP address this UDPReplyHandler sends packets to.
     * 
     * @return The IP address as a String like "1.2.3.4"
     */
    public String getAddress() {

        // Express the IP address as text and return it
        return IP.getHostAddress();
    }

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

	/**
     * Express this UDPReplyHandler object as 3 lines of text with the IP address and port number it sends packets to.
     * Composes a String like:
     * 
     * UDPReplyHandler:
     * 220.237.253.112
     * 6346
     * 
     * @return A String
	 */
	public String toString() {

        // Compose 3 lines of text with the IP address and port number
		return ("UDPReplyHandler:\r\n" + IP.toString() + "\r\n" + PORT + "\r\n");
	}

	/**
     * Send the given UDPCrawlerPong packet to this remote computer.
     * 
     * @param m The UDPCrawlerPong to send
	 */
	public void handleUDPCrawlerPong(UDPCrawlerPong m) {

        // Have the UDPService object send the Gnutella message in a UDP packet to the IP address and port number of this UDPReplyHandler
		UDPService.instance().send(m, IP, PORT);
	}

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

    /**
     * Get the port number this UDPReplyHandler sends packets to.
     * 
     * @return The port number
     */
	public int getPort() {

        // Return the port number the constructor saved
		return PORT;
	}

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
