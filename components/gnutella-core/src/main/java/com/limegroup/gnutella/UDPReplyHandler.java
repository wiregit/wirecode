pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.net.InetAddress;

import com.limegroup.gnutellb.filters.SpamFilter;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PushRequest;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.vendor.SimppVM;
import com.limegroup.gnutellb.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutellb.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * This clbss is an implementation of <tt>ReplyHandler</tt> that is 
 * speciblized for handling UDP messages.
 */
public finbl class UDPReplyHandler implements ReplyHandler {

	/**
	 * Constbnt for the <tt>InetAddress</tt> of the host to reply to.
	 */
	privbte final InetAddress IP;

	/**
	 * Constbnt for the port of the host to reply to.
	 */
	privbte final int PORT;

	/**
	 * Constbnt for the <tt>UDPService</tt>.
	 */
	privbte static final UDPService UDP_SERVICE = UDPService.instance();
    
    /**
     * Used to filter messbges that are considered spam.
     * With the introduction of OOB replies, it is importbnt
     * to check UDP replies for spbm too.
     *
     * Uses one stbtic instance instead of creating a new
     * filter for every single UDP messbge.
     */
    privbte static volatile SpamFilter _personalFilter =
        SpbmFilter.newPersonalFilter();
	
	/**
	 * Constructor thbt sets the ip and port to reply to.
	 *
	 * @pbram ip the <tt>InetAddress</tt> to reply to
	 * @pbram port the port to reply to
	 */
	public UDPReplyHbndler(InetAddress ip, int port) {
	    if(!NetworkUtils.isVblidPort(port))
	        throw new IllegblArgumentException("invalid port: " + port);
	    if(!NetworkUtils.isVblidAddress(ip))
	        throw new IllegblArgumentException("invalid ip: " + ip);
	       
		IP   = ip;
		PORT = port;
	}
    
    /**
     * Sets the new personbl spam filter to be used for all UDPReplyHandlers.
     */
    public stbtic void setPersonalFilter(SpamFilter filter) {
        _personblFilter = filter;
    }

	
	/**
	 * Sends the <tt>PingReply</tt> vib a UDP datagram to the IP and port
	 * for this hbndler.<p>
	 *
	 * Implements <tt>ReplyHbndler</tt>.
	 *
	 * @pbram hit the <tt>PingReply</tt> to send
	 * @pbram handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void hbndlePingReply(PingReply pong, ReplyHandler handler) {
        UDP_SERVICE.send(pong, IP, PORT);
		SentMessbgeStatHandler.UDP_PING_REPLIES.addMessage(pong);
	}

	/**
	 * Sends the <tt>QueryReply</tt> vib a UDP datagram to the IP and port
	 * for this hbndler.<p>
	 *
	 * Implements <tt>ReplyHbndler</tt>.
	 *
	 * @pbram hit the <tt>QueryReply</tt> to send
	 * @pbram handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void hbndleQueryReply(QueryReply hit, ReplyHandler handler) {
        UDP_SERVICE.send(hit, IP, PORT);
		SentMessbgeStatHandler.UDP_QUERY_REPLIES.addMessage(hit);
	}

	/**
	 * Sends the <tt>QueryRequest</tt> vib a UDP datagram to the IP and port
	 * for this hbndler.<p>
	 *
	 * Implements <tt>ReplyHbndler</tt>.
	 *
	 * @pbram request the <tt>QueryRequest</tt> to send
	 * @pbram handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void hbndlePushRequest(PushRequest request, ReplyHandler handler) {
        UDP_SERVICE.send(request, IP, PORT);
		SentMessbgeStatHandler.UDP_PUSH_REQUESTS.addMessage(request);
	}

	public void countDroppedMessbge() {}

	public boolebn isPersonalSpam(Message m) {
        return !_personblFilter.allow(m);
	}

	public boolebn isOpen() {
		return true;
	}

	public int getNumMessbgesReceived() {
		return 0;
	}

	public boolebn isOutgoing() {
		return fblse;
	}

	// inherit doc comment
	public boolebn isKillable() {
		return fblse;
	}

	/**
	 * Implements <tt>ReplyHbndler</tt>.  This always returns <tt>false</tt>
	 * for UDP reply hbndlers, as leaves are always connected via TCP.
	 *
	 * @return <tt>fblse</tt>, as all leaves are connected via TCP, so
	 *  directly connected lebves will not have <tt>UDPReplyHandler</tt>s
	 */
	public boolebn isSupernodeClientConnection() {
		return fblse;
	}

	/**
	 * Implements <tt>ReplyHbndler</tt> interface.  Always returns 
	 * <tt>fblse</tt> because leaves are connected via TCP, not UDP.
	 *
	 * @return <tt>fblse</tt>, since leaves never maintain their connections
	 *  vib UDP, only TCP
	 */
	public boolebn isLeafConnection() {
		return fblse;
	}

	/**
	 * Returns whether or not this connection is b high-degree connection,
	 * mebning that it maintains a high number of intra-Ultrapeer connections.
	 * In the cbse of UDP reply handlers, this always returns <tt>false<tt>.
	 *
	 * @return <tt>fblse</tt> because, by definition, a UDP 'connection' is not
	 *  b connection at all
	 */
	public boolebn isHighDegreeConnection() {
		return fblse;
	}

    /**
     * Returns <tt>fblse</tt> since UDP reply handlers are not TCP 
     * connections in the first plbce.
     *
     * @return <tt>fblse</tt>, since UDP handlers are not connections in
     *  the first plbce, and therefore cannot use Ultrapeer query routing
     */
    public boolebn isUltrapeerQueryRoutingConnection() {
        return fblse;
    }


    /**
     * Returns <tt>fblse</tt>, as this node is not  a "connection"
     * in the first plbce, and so could never have sent the requisite
     * hebders.
     *
     * @return <tt>fblse</tt>, as this node is not a real connection
     */
    public boolebn isGoodUltrapeer() {
        return fblse;
    }

    /**
     * Returns <tt>fblse</tt>, as this node is not  a "connection"
     * in the first plbce, and so could never have sent the requisite
     * hebders.
     *
     * @return <tt>fblse</tt>, as this node is not a real connection
     */
    public boolebn isGoodLeaf() {
        return fblse;
    }

    /**
     * Returns <tt>fblse</tt>, since we don't know whether a host 
     * communicbting via UDP supports pong caching or not.
     *
     * @return <tt>fblse</tt> since we don't know if this node supports
     *  pong cbching or not
     */
    public boolebn supportsPongCaching() {
        return fblse;
    }

    /**
     * Returns whether or not to bllow new pings from this <tt>ReplyHandler</tt>.
     * Since this ping is over UDP, we'll blways allow it.
     *
     * @return <tt>true</tt> since this ping is received over UDP
     */
    public boolebn allowNewPings() {
        return true;
    }

    /**
     * sends b Vendor Message to the host/port in this reply handler by UDP
     * dbtagram.
     */
    public void hbndleStatisticVM(StatisticVendorMessage m) throws IOException {
        UDPService.instbnce().send(m, IP, PORT);
    }
    
    /**
     * As of now there is no need to send SimppMessbges via UDP, 
     */ 
    public void hbndleSimppVM(SimppVM simppVM) {
        //This should never hbppen. But if it does, ignore it and move on
        return;
    }
    


    // inherit doc comment
    public InetAddress getInetAddress() {
        return IP;
    }
    
    /**
     * Retrieves the host bddress.
     */
    public String getAddress() {
        return IP.getHostAddress();
    }

    /**
     * Returns <tt>fblse</tt> to indicate that <tt>UDPReplyHandler</tt>s 
     * should never be considered stbble, due to data loss over UDP and lack
     * of knowledge bs to whether the host is still alive.
     *
     * @return <tt>fblse</tt> since UDP handler are never stable
     */
    public boolebn isStable() {
        return fblse;
    }

    /**
     * implementbtion of interface. this is not used.
     */
    public String getLocblePref() {
        return ApplicbtionSettings.DEFAULT_LOCALE.getValue();
    }

	/**
	 * Overrides toString to print out more detbiled information about
	 * this <tt>UDPReplyHbndler</tt>
	 */
	public String toString() {
		return ("UDPReplyHbndler:\r\n"+
				IP.toString()+"\r\n"+
				PORT+"\r\n");
	}
	
	/**
	 * sends the response through udp bbck to the requesting party
	 */
	public void hbndleUDPCrawlerPong(UDPCrawlerPong m) {
		UDPService.instbnce().send(m, IP, PORT);
	}
	
	public void reply(Messbge m) {
		UDPService.instbnce().send(m, IP,PORT);
	}
	
	public int getPort() {
		return PORT;
	}
	
	public byte[] getClientGUID() {
	    return DbtaUtils.EMPTY_GUID;
	}
}
