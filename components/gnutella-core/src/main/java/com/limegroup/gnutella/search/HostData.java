package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;

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
	 * Constructs a new <tt>HostData</tt> instance from a 
	 * <tt>QueryReply</tt>.
	 *
	 * @param reply the <tt>QueryReply</tt> instance from which
	 *  host data should be extracted.
	 */
	public HostData(QueryReply reply) {
		CLIENT_GUID = reply.getClientGUID();
		MESSAGE_GUID = reply.getGUID();
		SPEED = ByteOrder.long2int(reply.getSpeed()); //safe cast
		IP = reply.getIP();
		PORT = reply.getPort();

		boolean firewalled        = true;
		boolean busy              = true;
		boolean browseHostEnabled = false;
		boolean chatEnabled       = false;
		boolean measuredSpeed     = false;

		try {
			firewalled = reply.getNeedsPush();
		} catch(BadPacketException e) {
			firewalled = true;
		}
		try {
			browseHostEnabled = reply.getSupportsBrowseHost();
		} catch (BadPacketException e){ 
			browseHostEnabled = false;
		}                
		try {
			chatEnabled = reply.getSupportsChat() && !firewalled;			
		} catch (BadPacketException e){ 
			chatEnabled = false;
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

		FIREWALLED = firewalled;
		BUSY = busy;
		BROWSE_HOST_ENABLED = browseHostEnabled;
		CHAT_ENABLED = chatEnabled;
		MEASURED_SPEED = measuredSpeed;
		boolean ifirewalled = !RouterService.acceptedIncomingConnection();
        QUALITY = reply.calculateQualityOfService(ifirewalled);
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
}
