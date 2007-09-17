package com.limegroup.gnutella.search;


import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * This class contains data about a host that has returned a query hit,
 * as opposed to the data about the file itself, which is contained in
 * <tt>Response</tt>.
 */
public final class HostData {

	/**
	 * Constant for the client guid.
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
	 * Constant for whether or not this is a reply to a multicast query
	 */
	private final boolean MULTICAST;

	/**
	 * Constant for whether or not chat is enabled.
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
	 * Constant for the search result "quality", based on whether or not
	 * the host is firewalled, has open upload slots, etc.
	 */
	private final int QUALITY;
		
    /** 
     * Constant for the Vendor code of the reply.
     */
    private final String VENDOR_CODE;
    

    /**
     * The <tt>Set</tt> of PushProxies for this host.
     */
    private final Set<? extends IpPort> PROXIES;

    /**
     * Constant for the Firewalled Transfer status of this badboy.
     */
    private final boolean CAN_DO_FWTRANSFER;
    
    /**
     * the version of the Firewall Transfer supported. 0 if not supported
     */
    private final int FWT_VERSION;

    /** If the host supports TLS connections. */
    private final boolean TLS_CAPABLE;
    
    /** Constructs a HostData with all the given fields. */
    public HostData(byte[] clientGuid, byte[] messageGuid, int speed, boolean firewalled,
            boolean busy, boolean multicast, boolean chatEnabled, boolean browseHostEnabled,
            boolean measuredSpeed, String ip, int port, int quality, String vendorCode,
            Set<? extends IpPort> proxies, boolean canDoFw2Fw, int fwtVersion, boolean tlsCapable) {
        this.CLIENT_GUID = clientGuid;
        this.MESSAGE_GUID = messageGuid;
        this.SPEED = speed;
        this.FIREWALLED = firewalled;
        this.BUSY = busy;
        this.MULTICAST = multicast;
        this.CHAT_ENABLED = chatEnabled;
        this.BROWSE_HOST_ENABLED = browseHostEnabled;
        this.MEASURED_SPEED = measuredSpeed;
        this.IP = ip;
        this.PORT = port;
        this.QUALITY = quality;
        this.VENDOR_CODE = vendorCode;
        this.PROXIES = proxies;
        this.CAN_DO_FWTRANSFER = canDoFw2Fw;
        this.FWT_VERSION = fwtVersion;
        this.TLS_CAPABLE = tlsCapable;
    }
         
	/**
     * Constructs a new <tt>HostData</tt> instance from a <tt>QueryReply</tt>.
     * 
     * @param reply the <tt>QueryReply</tt> instance from which host data
     *        should be extracted.
     */
	protected HostData(QueryReply reply, NetworkManager networkManager) {
		CLIENT_GUID = reply.getClientGUID();
		MESSAGE_GUID = reply.getGUID();
		IP = reply.getIP();
		PORT = reply.getPort();

		boolean firewalled        = true;
		boolean busy              = true;
		boolean browseHostEnabled = false;
		boolean chatEnabled       = false;
		boolean measuredSpeed     = false;
		boolean multicast         = false;
        String  vendor = "";

		try {
			firewalled = reply.getNeedsPush() || 
                NetworkUtils.isPrivateAddress(IP);
		} catch(BadPacketException e) {
			firewalled = true;
		}
		
		try { 
			measuredSpeed = reply.getIsMeasuredSpeed();
		} catch (BadPacketException e) { 
			measuredSpeed = false;
		}
		try {
			busy = reply.getIsBusy();
		} catch (BadPacketException bad) {
			busy = true;
		}
		
        
		try {
            vendor = reply.getVendor();
		} catch(BadPacketException bad) {
		}

    	browseHostEnabled = reply.getSupportsBrowseHost();
		chatEnabled = reply.getSupportsChat() && !firewalled;
		multicast = reply.isReplyToMulticastQuery();

		FIREWALLED = firewalled && !multicast;
		BUSY = busy;
		BROWSE_HOST_ENABLED = browseHostEnabled;
		CHAT_ENABLED = chatEnabled;
		MEASURED_SPEED = measuredSpeed || multicast;
		MULTICAST = multicast;
        VENDOR_CODE = vendor;
		boolean ifirewalled = !networkManager.acceptedIncomingConnection();
        QUALITY = reply.calculateQualityOfService(ifirewalled, networkManager);
        PROXIES = reply.getPushProxies();
        CAN_DO_FWTRANSFER = reply.getSupportsFWTransfer();
        FWT_VERSION = reply.getFWTransferVersion();
        TLS_CAPABLE = reply.isTLSCapable();

        if ( multicast )
            SPEED = Integer.MAX_VALUE;
        else
            SPEED = ByteOrder.long2int(reply.getSpeed()); //safe cast
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
	 * Accessor for the message guid.
	 * 
	 * @return the message guid
	 */
	public byte[] getMessageGUID() {
		return MESSAGE_GUID;
	}

	/**
	 * Accessor for the speed (bandwidth) of the remote host.
	 * 
	 * @return the speed of the remote host
	 */
	public int getSpeed() {
		return SPEED;
	}

	/**
	 * Accessor for the quality of results returned from this host, based on
	 * firewalled status, whether or not it has upload slots, etc.
	 * 
	 * @return the quality of results returned from the remote host
	 */
	public int getQuality() {
		return QUALITY;
	}

	/**
	 * Accessor for the ip address of the host sending the reply.
	 * 
	 * @return the ip address for the replying host
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
	 * Returns whether or not the remote host is firewalled.
	 *
	 * @return <tt>true</tt> if the remote host is firewalled,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isFirewalled() {
		return FIREWALLED;
	}

	/**
	 * Returns whether or not the remote host is busy.
	 *
	 * @return <tt>true</tt> if the remote host is busy,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isBusy() {
		return BUSY;
	}

	/**
	 * Returns whether or not the remote host has browse host enabled.
	 *
	 * @return <tt>true</tt> if the remote host has browse host enabled,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isBrowseHostEnabled() {
		return BROWSE_HOST_ENABLED;
	}

	/**
	 * Returns whether or not the remote host has chat enabled.
	 *
	 * @return <tt>true</tt> if the remote host has chat enabled,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isChatEnabled() {
		return CHAT_ENABLED;
	}

	/**
	 * Returns whether or not the remote host is reporting a speed that 
	 * has been measured by the application, as opposed to simply selected
	 * by the user..
	 *
	 * @return <tt>true</tt> if the remote host has as measured speed,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isMeasuredSpeed() {
		return MEASURED_SPEED;
	}
	
	/**
	 * Returns whether or not this was a response to a multicast query.
	 *
	 * @return <tt>true</tt> if this is a response to a multicast query,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isReplyToMulticastQuery() {
	    return MULTICAST;
	}

    /**
     * Returns the <tt>Set</tt> of push proxies, which can be empty.
     *
     * @return a <tt>Set</tt> of push proxies, which can be empty
     */
    public Set<? extends IpPort> getPushProxies() {
        return PROXIES;
    }

    /**
     * Returns whether or not this Host can do Firewalled Transfer.
     *
     */
    public boolean supportsFWTransfer() {
        return CAN_DO_FWTRANSFER;
    }
    
    /**
     * 
     * @return the version of FWT protocol this host supports. 0 if none
     */
    public int getFWTVersionSupported() {
    	return FWT_VERSION;
    }
    
    /** Returns true if the host supports TLS connections. */
    public boolean isTLSCapable() {
        return TLS_CAPABLE;
    }

}
