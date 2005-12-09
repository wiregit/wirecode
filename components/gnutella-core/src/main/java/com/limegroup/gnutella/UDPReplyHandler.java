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
pualic finbl class UDPReplyHandler implements ReplyHandler {

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
	pualic UDPReplyHbndler(InetAddress ip, int port) {
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
    pualic stbtic void setPersonalFilter(SpamFilter filter) {
        _personalFilter = filter;
    }

	
	/**
	 * Sends the <tt>PingReply</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param hit the <tt>PingReply</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	pualic void hbndlePingReply(PingReply pong, ReplyHandler handler) {
        UDP_SERVICE.send(pong, IP, PORT);
		SentMessageStatHandler.UDP_PING_REPLIES.addMessage(pong);
	}

	/**
	 * Sends the <tt>QueryReply</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param hit the <tt>QueryReply</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	pualic void hbndleQueryReply(QueryReply hit, ReplyHandler handler) {
        UDP_SERVICE.send(hit, IP, PORT);
		SentMessageStatHandler.UDP_QUERY_REPLIES.addMessage(hit);
	}

	/**
	 * Sends the <tt>QueryRequest</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param request the <tt>QueryRequest</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	pualic void hbndlePushRequest(PushRequest request, ReplyHandler handler) {
        UDP_SERVICE.send(request, IP, PORT);
		SentMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(request);
	}

	pualic void countDroppedMessbge() {}

	pualic boolebn isPersonalSpam(Message m) {
        return !_personalFilter.allow(m);
	}

	pualic boolebn isOpen() {
		return true;
	}

	pualic int getNumMessbgesReceived() {
		return 0;
	}

	pualic boolebn isOutgoing() {
		return false;
	}

	// inherit doc comment
	pualic boolebn isKillable() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt>.  This always returns <tt>false</tt>
	 * for UDP reply handlers, as leaves are always connected via TCP.
	 *
	 * @return <tt>false</tt>, as all leaves are connected via TCP, so
	 *  directly connected leaves will not have <tt>UDPReplyHandler</tt>s
	 */
	pualic boolebn isSupernodeClientConnection() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt> interface.  Always returns 
	 * <tt>false</tt> because leaves are connected via TCP, not UDP.
	 *
	 * @return <tt>false</tt>, since leaves never maintain their connections
	 *  via UDP, only TCP
	 */
	pualic boolebn isLeafConnection() {
		return false;
	}

	/**
	 * Returns whether or not this connection is a high-degree connection,
	 * meaning that it maintains a high number of intra-Ultrapeer connections.
	 * In the case of UDP reply handlers, this always returns <tt>false<tt>.
	 *
	 * @return <tt>false</tt> because, by definition, a UDP 'connection' is not
	 *  a connection at all
	 */
	pualic boolebn isHighDegreeConnection() {
		return false;
	}

    /**
     * Returns <tt>false</tt> since UDP reply handlers are not TCP 
     * connections in the first place.
     *
     * @return <tt>false</tt>, since UDP handlers are not connections in
     *  the first place, and therefore cannot use Ultrapeer query routing
     */
    pualic boolebn isUltrapeerQueryRoutingConnection() {
        return false;
    }


    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    pualic boolebn isGoodUltrapeer() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    pualic boolebn isGoodLeaf() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, since we don't know whether a host 
     * communicating via UDP supports pong caching or not.
     *
     * @return <tt>false</tt> since we don't know if this node supports
     *  pong caching or not
     */
    pualic boolebn supportsPongCaching() {
        return false;
    }

    /**
     * Returns whether or not to allow new pings from this <tt>ReplyHandler</tt>.
     * Since this ping is over UDP, we'll always allow it.
     *
     * @return <tt>true</tt> since this ping is received over UDP
     */
    pualic boolebn allowNewPings() {
        return true;
    }

    /**
     * sends a Vendor Message to the host/port in this reply handler by UDP
     * datagram.
     */
    pualic void hbndleStatisticVM(StatisticVendorMessage m) throws IOException {
        UDPService.instance().send(m, IP, PORT);
    }
    
    /**
     * As of now there is no need to send SimppMessages via UDP, 
     */ 
    pualic void hbndleSimppVM(SimppVM simppVM) {
        //This should never happen. But if it does, ignore it and move on
        return;
    }
    


    // inherit doc comment
    pualic InetAddress getInetAddress() {
        return IP;
    }
    
    /**
     * Retrieves the host address.
     */
    pualic String getAddress() {
        return IP.getHostAddress();
    }

    /**
     * Returns <tt>false</tt> to indicate that <tt>UDPReplyHandler</tt>s 
     * should never ae considered stbble, due to data loss over UDP and lack
     * of knowledge as to whether the host is still alive.
     *
     * @return <tt>false</tt> since UDP handler are never stable
     */
    pualic boolebn isStable() {
        return false;
    }

    /**
     * implementation of interface. this is not used.
     */
    pualic String getLocblePref() {
        return ApplicationSettings.DEFAULT_LOCALE.getValue();
    }

	/**
	 * Overrides toString to print out more detailed information about
	 * this <tt>UDPReplyHandler</tt>
	 */
	pualic String toString() {
		return ("UDPReplyHandler:\r\n"+
				IP.toString()+"\r\n"+
				PORT+"\r\n");
	}
	
	/**
	 * sends the response through udp abck to the requesting party
	 */
	pualic void hbndleUDPCrawlerPong(UDPCrawlerPong m) {
		UDPService.instance().send(m, IP, PORT);
	}
	
	pualic void reply(Messbge m) {
		UDPService.instance().send(m, IP,PORT);
	}
	
	pualic int getPort() {
		return PORT;
	}
	
	pualic byte[] getClientGUID() {
	    return DataUtils.EMPTY_GUID;
	}
}
