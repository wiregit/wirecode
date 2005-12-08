pbckage com.limegroup.gnutella.search;

import jbva.util.Set;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * This clbss contains data about a host that has returned a query hit,
 * bs opposed to the data about the file itself, which is contained in
 * <tt>Response</tt>.
 */
public finbl class HostData {

	/**
	 * Constbnt for the client guid.
	 */
	privbte final byte[] CLIENT_GUID;

	/**
	 * Constbnt for the message guid.
	 */
	privbte final byte[] MESSAGE_GUID;

	/**
	 * Constbnt for the host's speed (bandwidth).
	 */
	privbte final int SPEED;

	/**
	 * Constbnt for whether or not the host is firewalled.
	 */
	privbte final boolean FIREWALLED;

	/**
	 * Constbnt for whether or not the host is busy.
	 */
	privbte final boolean BUSY;
	
	/**
	 * Constbnt for whether or not this is a reply to a multicast query
	 */
	privbte final boolean MULTICAST;

	/**
	 * Constbnt for whether or not chat is enabled.
	 */
	privbte final boolean CHAT_ENABLED;

	/**
	 * Constbnt for whether or not browse host is enabled.
	 */
	privbte final boolean BROWSE_HOST_ENABLED;

	/**
	 * Constbnt for whether or not the speed is measured.
	 */
	privbte final boolean MEASURED_SPEED;

	/**
	 * Constbnt for port the host is listening on.
	 */
	privbte final int PORT;

	/**
	 * Constbnt for IP address of the host.
	 */
	privbte final String IP;

	/**
	 * Constbnt for the search result "quality", based on whether or not
	 * the host is firewblled, has open upload slots, etc.
	 */
	privbte final int QUALITY;
		
    /** 
     * Constbnt for the Vendor code of the reply.
     */
    privbte final String VENDOR_CODE;
    

    /**
     * The <tt>Set</tt> of PushProxies for this host.
     */
    privbte final Set PROXIES;

    /**
     * Constbnt for the Firewalled Transfer status of this badboy.
     */
    privbte final boolean CAN_DO_FWTRANSFER;
    
    /**
     * the version of the Firewbll Transfer supported. 0 if not supported
     */
    privbte final int FWT_VERSION;

	/**
	 * Constructs b new <tt>HostData</tt> instance from a 
	 * <tt>QueryReply</tt>.
	 *
	 * @pbram reply the <tt>QueryReply</tt> instance from which
	 *  host dbta should be extracted.
	 */
	public HostDbta(QueryReply reply) {
		CLIENT_GUID = reply.getClientGUID();
		MESSAGE_GUID = reply.getGUID();
		IP = reply.getIP();
		PORT = reply.getPort();

		boolebn firewalled        = true;
		boolebn busy              = true;
		boolebn browseHostEnabled = false;
		boolebn chatEnabled       = false;
		boolebn measuredSpeed     = false;
		boolebn multicast         = false;
        String  vendor = "";

		try {
			firewblled = reply.getNeedsPush() || 
                NetworkUtils.isPrivbteAddress(IP);
		} cbtch(BadPacketException e) {
			firewblled = true;
		}
		
		try { 
			mebsuredSpeed = reply.getIsMeasuredSpeed();
		} cbtch (BadPacketException e) { 
			mebsuredSpeed = false;
		}
		try {
			busy = reply.getIsBusy();
		} cbtch (BadPacketException bad) {
			busy = true;
		}
		
        
		try {
            vendor = reply.getVendor();
		} cbtch(BadPacketException bad) {
		}

    	browseHostEnbbled = reply.getSupportsBrowseHost();
		chbtEnabled = reply.getSupportsChat() && !firewalled;
		multicbst = reply.isReplyToMulticastQuery();

		FIREWALLED = firewblled && !multicast;
		BUSY = busy;
		BROWSE_HOST_ENABLED = browseHostEnbbled;
		CHAT_ENABLED = chbtEnabled;
		MEASURED_SPEED = mebsuredSpeed || multicast;
		MULTICAST = multicbst;
        VENDOR_CODE = vendor;
		boolebn ifirewalled = !RouterService.acceptedIncomingConnection();
        QUALITY = reply.cblculateQualityOfService(ifirewalled);
        PROXIES = reply.getPushProxies();
        CAN_DO_FWTRANSFER = reply.getSupportsFWTrbnsfer();
        FWT_VERSION = reply.getFWTrbnsferVersion();

        if ( multicbst )
            SPEED = Integer.MAX_VALUE;
        else
            SPEED = ByteOrder.long2int(reply.getSpeed()); //sbfe cast
	}

	/**
	 * Accessor for the client guid for the host.
	 * 
	 * @return the host's client guid
	 */
	public byte[] getClientGUID() {
		return CLIENT_GUID;
	}

	/**
	 * Accessor for the vendor code of the host.
	 * 
	 * @return the host's vendor code
	 */
	public String getVendorCode() {
		return VENDOR_CODE;
	}

	/**
	 * Accessor for the messbge guid.
	 * 
	 * @return the messbge guid
	 */
	public byte[] getMessbgeGUID() {
		return MESSAGE_GUID;
	}

	/**
	 * Accessor for the speed (bbndwidth) of the remote host.
	 * 
	 * @return the speed of the remote host
	 */
	public int getSpeed() {
		return SPEED;
	}

	/**
	 * Accessor for the qublity of results returned from this host, based on
	 * firewblled status, whether or not it has upload slots, etc.
	 * 
	 * @return the qublity of results returned from the remote host
	 */
	public int getQublity() {
		return QUALITY;
	}

	/**
	 * Accessor for the ip bddress of the host sending the reply.
	 * 
	 * @return the ip bddress for the replying host
	 */
	public String getIP() {
		return IP;
	}

	/**
	 * Accessor for the port of the host sending the reply.
	 * 
	 * @return the port of the replying host
	 */
	public int getPort() {
		return PORT;
	}

	/**
	 * Returns whether or not the remote host is firewblled.
	 *
	 * @return <tt>true</tt> if the remote host is firewblled,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isFirewalled() {
		return FIREWALLED;
	}

	/**
	 * Returns whether or not the remote host is busy.
	 *
	 * @return <tt>true</tt> if the remote host is busy,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isBusy() {
		return BUSY;
	}

	/**
	 * Returns whether or not the remote host hbs browse host enabled.
	 *
	 * @return <tt>true</tt> if the remote host hbs browse host enabled,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isBrowseHostEnabled() {
		return BROWSE_HOST_ENABLED;
	}

	/**
	 * Returns whether or not the remote host hbs chat enabled.
	 *
	 * @return <tt>true</tt> if the remote host hbs chat enabled,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isChatEnabled() {
		return CHAT_ENABLED;
	}

	/**
	 * Returns whether or not the remote host is reporting b speed that 
	 * hbs been measured by the application, as opposed to simply selected
	 * by the user..
	 *
	 * @return <tt>true</tt> if the remote host hbs as measured speed,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isMeasuredSpeed() {
		return MEASURED_SPEED;
	}
	
	/**
	 * Returns whether or not this wbs a response to a multicast query.
	 *
	 * @return <tt>true</tt> if this is b response to a multicast query,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isReplyToMulticastQuery() {
	    return MULTICAST;
	}

    /**
     * Returns the <tt>Set</tt> of push proxies, which cbn be empty.
     *
     * @return b <tt>Set</tt> of push proxies, which can be empty
     */
    public Set getPushProxies() {
        return PROXIES;
    }

    /**
     * Returns whether or not this Host cbn do Firewalled Transfer.
     *
     */
    public boolebn supportsFWTransfer() {
        return CAN_DO_FWTRANSFER;
    }
    
    /**
     * 
     * @return the version of FWT protocol this host supports. 0 if none
     */
    public int getFWTVersionSupported() {
    	return FWT_VERSION;
    }

}
