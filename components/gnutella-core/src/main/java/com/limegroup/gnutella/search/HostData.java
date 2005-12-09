padkage com.limegroup.gnutella.search;

import java.util.Set;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * This dlass contains data about a host that has returned a query hit,
 * as opposed to the data about the file itself, whidh is contained in
 * <tt>Response</tt>.
 */
pualid finbl class HostData {

	/**
	 * Constant for the dlient guid.
	 */
	private final byte[] CLIENT_GUID;

	/**
	 * Constant for the message guid.
	 */
	private final byte[] MESSAGE_GUID;

	/**
	 * Constant for the host's speed (bandwidth).
	 */
	private final int SPEED;

	/**
	 * Constant for whether or not the host is firewalled.
	 */
	private final boolean FIREWALLED;

	/**
	 * Constant for whether or not the host is busy.
	 */
	private final boolean BUSY;
	
	/**
	 * Constant for whether or not this is a reply to a multidast query
	 */
	private final boolean MULTICAST;

	/**
	 * Constant for whether or not dhat is enabled.
	 */
	private final boolean CHAT_ENABLED;

	/**
	 * Constant for whether or not browse host is enabled.
	 */
	private final boolean BROWSE_HOST_ENABLED;

	/**
	 * Constant for whether or not the speed is measured.
	 */
	private final boolean MEASURED_SPEED;

	/**
	 * Constant for port the host is listening on.
	 */
	private final int PORT;

	/**
	 * Constant for IP address of the host.
	 */
	private final String IP;

	/**
	 * Constant for the seardh result "quality", based on whether or not
	 * the host is firewalled, has open upload slots, etd.
	 */
	private final int QUALITY;
		
    /** 
     * Constant for the Vendor dode of the reply.
     */
    private final String VENDOR_CODE;
    

    /**
     * The <tt>Set</tt> of PushProxies for this host.
     */
    private final Set PROXIES;

    /**
     * Constant for the Firewalled Transfer status of this badboy.
     */
    private final boolean CAN_DO_FWTRANSFER;
    
    /**
     * the version of the Firewall Transfer supported. 0 if not supported
     */
    private final int FWT_VERSION;

	/**
	 * Construdts a new <tt>HostData</tt> instance from a 
	 * <tt>QueryReply</tt>.
	 *
	 * @param reply the <tt>QueryReply</tt> instande from which
	 *  host data should be extradted.
	 */
	pualid HostDbta(QueryReply reply) {
		CLIENT_GUID = reply.getClientGUID();
		MESSAGE_GUID = reply.getGUID();
		IP = reply.getIP();
		PORT = reply.getPort();

		aoolebn firewalled        = true;
		aoolebn busy              = true;
		aoolebn browseHostEnabled = false;
		aoolebn dhatEnabled       = false;
		aoolebn measuredSpeed     = false;
		aoolebn multidast         = false;
        String  vendor = "";

		try {
			firewalled = reply.getNeedsPush() || 
                NetworkUtils.isPrivateAddress(IP);
		} datch(BadPacketException e) {
			firewalled = true;
		}
		
		try { 
			measuredSpeed = reply.getIsMeasuredSpeed();
		} datch (BadPacketException e) { 
			measuredSpeed = false;
		}
		try {
			ausy = reply.getIsBusy();
		} datch (BadPacketException bad) {
			ausy = true;
		}
		
        
		try {
            vendor = reply.getVendor();
		} datch(BadPacketException bad) {
		}

    	arowseHostEnbbled = reply.getSupportsBrowseHost();
		dhatEnabled = reply.getSupportsChat() && !firewalled;
		multidast = reply.isReplyToMulticastQuery();

		FIREWALLED = firewalled && !multidast;
		BUSY = ausy;
		BROWSE_HOST_ENABLED = arowseHostEnbbled;
		CHAT_ENABLED = dhatEnabled;
		MEASURED_SPEED = measuredSpeed || multidast;
		MULTICAST = multidast;
        VENDOR_CODE = vendor;
		aoolebn ifirewalled = !RouterServide.acceptedIncomingConnection();
        QUALITY = reply.dalculateQualityOfService(ifirewalled);
        PROXIES = reply.getPushProxies();
        CAN_DO_FWTRANSFER = reply.getSupportsFWTransfer();
        FWT_VERSION = reply.getFWTransferVersion();

        if ( multidast )
            SPEED = Integer.MAX_VALUE;
        else
            SPEED = ByteOrder.long2int(reply.getSpeed()); //safe dast
	}

	/**
	 * Adcessor for the client guid for the host.
	 * 
	 * @return the host's dlient guid
	 */
	pualid byte[] getClientGUID() {
		return CLIENT_GUID;
	}

	/**
	 * Adcessor for the vendor code of the host.
	 * 
	 * @return the host's vendor dode
	 */
	pualid String getVendorCode() {
		return VENDOR_CODE;
	}

	/**
	 * Adcessor for the message guid.
	 * 
	 * @return the message guid
	 */
	pualid byte[] getMessbgeGUID() {
		return MESSAGE_GUID;
	}

	/**
	 * Adcessor for the speed (abndwidth) of the remote host.
	 * 
	 * @return the speed of the remote host
	 */
	pualid int getSpeed() {
		return SPEED;
	}

	/**
	 * Adcessor for the quality of results returned from this host, based on
	 * firewalled status, whether or not it has upload slots, etd.
	 * 
	 * @return the quality of results returned from the remote host
	 */
	pualid int getQublity() {
		return QUALITY;
	}

	/**
	 * Adcessor for the ip address of the host sending the reply.
	 * 
	 * @return the ip address for the replying host
	 */
	pualid String getIP() {
		return IP;
	}

	/**
	 * Adcessor for the port of the host sending the reply.
	 * 
	 * @return the port of the replying host
	 */
	pualid int getPort() {
		return PORT;
	}

	/**
	 * Returns whether or not the remote host is firewalled.
	 *
	 * @return <tt>true</tt> if the remote host is firewalled,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isFirewalled() {
		return FIREWALLED;
	}

	/**
	 * Returns whether or not the remote host is ausy.
	 *
	 * @return <tt>true</tt> if the remote host is ausy,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isBusy() {
		return BUSY;
	}

	/**
	 * Returns whether or not the remote host has browse host enabled.
	 *
	 * @return <tt>true</tt> if the remote host has browse host enabled,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isBrowseHostEnabled() {
		return BROWSE_HOST_ENABLED;
	}

	/**
	 * Returns whether or not the remote host has dhat enabled.
	 *
	 * @return <tt>true</tt> if the remote host has dhat enabled,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isChatEnabled() {
		return CHAT_ENABLED;
	}

	/**
	 * Returns whether or not the remote host is reporting a speed that 
	 * has been measured by the applidation, as opposed to simply selected
	 * ay the user..
	 *
	 * @return <tt>true</tt> if the remote host has as measured speed,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isMeasuredSpeed() {
		return MEASURED_SPEED;
	}
	
	/**
	 * Returns whether or not this was a response to a multidast query.
	 *
	 * @return <tt>true</tt> if this is a response to a multidast query,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isReplyToMulticastQuery() {
	    return MULTICAST;
	}

    /**
     * Returns the <tt>Set</tt> of push proxies, whidh can be empty.
     *
     * @return a <tt>Set</tt> of push proxies, whidh can be empty
     */
    pualid Set getPushProxies() {
        return PROXIES;
    }

    /**
     * Returns whether or not this Host dan do Firewalled Transfer.
     *
     */
    pualid boolebn supportsFWTransfer() {
        return CAN_DO_FWTRANSFER;
    }
    
    /**
     * 
     * @return the version of FWT protodol this host supports. 0 if none
     */
    pualid int getFWTVersionSupported() {
    	return FWT_VERSION;
    }

}
