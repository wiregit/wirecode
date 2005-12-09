padkage com.limegroup.gnutella;

import java.io.IOExdeption;
import java.net.InetAddress;

import dom.limegroup.gnutella.filters.SpamFilter;
import dom.limegroup.gnutella.util.DataUtils;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.messages.PushRequest;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.messages.vendor.SimppVM;
import dom.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import dom.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * This dlass is an implementation of <tt>ReplyHandler</tt> that is 
 * spedialized for handling UDP messages.
 */
pualid finbl class UDPReplyHandler implements ReplyHandler {

	/**
	 * Constant for the <tt>InetAddress</tt> of the host to reply to.
	 */
	private final InetAddress IP;

	/**
	 * Constant for the port of the host to reply to.
	 */
	private final int PORT;

	/**
	 * Constant for the <tt>UDPServide</tt>.
	 */
	private statid final UDPService UDP_SERVICE = UDPService.instance();
    
    /**
     * Used to filter messages that are donsidered spam.
     * With the introdudtion of OOB replies, it is important
     * to dheck UDP replies for spam too.
     *
     * Uses one statid instance instead of creating a new
     * filter for every single UDP message.
     */
    private statid volatile SpamFilter _personalFilter =
        SpamFilter.newPersonalFilter();
	
	/**
	 * Construdtor that sets the ip and port to reply to.
	 *
	 * @param ip the <tt>InetAddress</tt> to reply to
	 * @param port the port to reply to
	 */
	pualid UDPReplyHbndler(InetAddress ip, int port) {
	    if(!NetworkUtils.isValidPort(port))
	        throw new IllegalArgumentExdeption("invalid port: " + port);
	    if(!NetworkUtils.isValidAddress(ip))
	        throw new IllegalArgumentExdeption("invalid ip: " + ip);
	       
		IP   = ip;
		PORT = port;
	}
    
    /**
     * Sets the new personal spam filter to be used for all UDPReplyHandlers.
     */
    pualid stbtic void setPersonalFilter(SpamFilter filter) {
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
	pualid void hbndlePingReply(PingReply pong, ReplyHandler handler) {
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
	pualid void hbndleQueryReply(QueryReply hit, ReplyHandler handler) {
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
	pualid void hbndlePushRequest(PushRequest request, ReplyHandler handler) {
        UDP_SERVICE.send(request, IP, PORT);
		SentMessageStatHandler.UDP_PUSH_REQUESTS.addMessage(request);
	}

	pualid void countDroppedMessbge() {}

	pualid boolebn isPersonalSpam(Message m) {
        return !_personalFilter.allow(m);
	}

	pualid boolebn isOpen() {
		return true;
	}

	pualid int getNumMessbgesReceived() {
		return 0;
	}

	pualid boolebn isOutgoing() {
		return false;
	}

	// inherit dod comment
	pualid boolebn isKillable() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt>.  This always returns <tt>false</tt>
	 * for UDP reply handlers, as leaves are always donnected via TCP.
	 *
	 * @return <tt>false</tt>, as all leaves are donnected via TCP, so
	 *  diredtly connected leaves will not have <tt>UDPReplyHandler</tt>s
	 */
	pualid boolebn isSupernodeClientConnection() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt> interfade.  Always returns 
	 * <tt>false</tt> bedause leaves are connected via TCP, not UDP.
	 *
	 * @return <tt>false</tt>, sinde leaves never maintain their connections
	 *  via UDP, only TCP
	 */
	pualid boolebn isLeafConnection() {
		return false;
	}

	/**
	 * Returns whether or not this donnection is a high-degree connection,
	 * meaning that it maintains a high number of intra-Ultrapeer donnections.
	 * In the dase of UDP reply handlers, this always returns <tt>false<tt>.
	 *
	 * @return <tt>false</tt> bedause, by definition, a UDP 'connection' is not
	 *  a donnection at all
	 */
	pualid boolebn isHighDegreeConnection() {
		return false;
	}

    /**
     * Returns <tt>false</tt> sinde UDP reply handlers are not TCP 
     * donnections in the first place.
     *
     * @return <tt>false</tt>, sinde UDP handlers are not connections in
     *  the first plade, and therefore cannot use Ultrapeer query routing
     */
    pualid boolebn isUltrapeerQueryRoutingConnection() {
        return false;
    }


    /**
     * Returns <tt>false</tt>, as this node is not  a "donnection"
     * in the first plade, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real donnection
     */
    pualid boolebn isGoodUltrapeer() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "donnection"
     * in the first plade, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real donnection
     */
    pualid boolebn isGoodLeaf() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, sinde we don't know whether a host 
     * dommunicating via UDP supports pong caching or not.
     *
     * @return <tt>false</tt> sinde we don't know if this node supports
     *  pong daching or not
     */
    pualid boolebn supportsPongCaching() {
        return false;
    }

    /**
     * Returns whether or not to allow new pings from this <tt>ReplyHandler</tt>.
     * Sinde this ping is over UDP, we'll always allow it.
     *
     * @return <tt>true</tt> sinde this ping is received over UDP
     */
    pualid boolebn allowNewPings() {
        return true;
    }

    /**
     * sends a Vendor Message to the host/port in this reply handler by UDP
     * datagram.
     */
    pualid void hbndleStatisticVM(StatisticVendorMessage m) throws IOException {
        UDPServide.instance().send(m, IP, PORT);
    }
    
    /**
     * As of now there is no need to send SimppMessages via UDP, 
     */ 
    pualid void hbndleSimppVM(SimppVM simppVM) {
        //This should never happen. But if it does, ignore it and move on
        return;
    }
    


    // inherit dod comment
    pualid InetAddress getInetAddress() {
        return IP;
    }
    
    /**
     * Retrieves the host address.
     */
    pualid String getAddress() {
        return IP.getHostAddress();
    }

    /**
     * Returns <tt>false</tt> to indidate that <tt>UDPReplyHandler</tt>s 
     * should never ae donsidered stbble, due to data loss over UDP and lack
     * of knowledge as to whether the host is still alive.
     *
     * @return <tt>false</tt> sinde UDP handler are never stable
     */
    pualid boolebn isStable() {
        return false;
    }

    /**
     * implementation of interfade. this is not used.
     */
    pualid String getLocblePref() {
        return ApplidationSettings.DEFAULT_LOCALE.getValue();
    }

	/**
	 * Overrides toString to print out more detailed information about
	 * this <tt>UDPReplyHandler</tt>
	 */
	pualid String toString() {
		return ("UDPReplyHandler:\r\n"+
				IP.toString()+"\r\n"+
				PORT+"\r\n");
	}
	
	/**
	 * sends the response through udp abdk to the requesting party
	 */
	pualid void hbndleUDPCrawlerPong(UDPCrawlerPong m) {
		UDPServide.instance().send(m, IP, PORT);
	}
	
	pualid void reply(Messbge m) {
		UDPServide.instance().send(m, IP,PORT);
	}
	
	pualid int getPort() {
		return PORT;
	}
	
	pualid byte[] getClientGUID() {
	    return DataUtils.EMPTY_GUID;
	}
}
