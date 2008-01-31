package com.limegroup.gnutella.search;


import java.util.Set;

import org.limewire.io.IpPort;

/**
 * This class contains data about a host that has returned a query hit,
 * as opposed to the data about the file itself, which is contained in
 * <tt>Response</tt>.
 */
public interface HostData {

	/**
	 * Accessor for the client guid for the host.
	 * 
	 * @return the host's client guid
	 */
	public byte[] getClientGUID();

	/**
	 * Accessor for the vendor code of the host.
	 * 
	 * @return the host's vendor code
	 */
	public String getVendorCode();

	/**
	 * Accessor for the message guid.
	 * 
	 * @return the message guid
	 */
	public byte[] getMessageGUID();

	/**
	 * Accessor for the speed (bandwidth) of the remote host.
	 * 
	 * @return the speed of the remote host
	 */
	public int getSpeed();

	/**
	 * Accessor for the quality of results returned from this host, based on
	 * firewalled status, whether or not it has upload slots, etc.
	 * 
	 * @return the quality of results returned from the remote host
	 */
	public int getQuality();

	/**
	 * Accessor for the ip address of the host sending the reply.
	 * 
	 * @return the ip address for the replying host
	 */
	public String getIP();

	/**
	 * Accessor for the port of the host sending the reply.
	 * 
	 * @return the port of the replying host
	 */
	public int getPort();

	/**
	 * Returns whether or not the remote host is firewalled.
	 *
	 * @return <tt>true</tt> if the remote host is firewalled,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isFirewalled();

	/**
	 * Returns whether or not the remote host is busy.
	 *
	 * @return <tt>true</tt> if the remote host is busy,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isBusy();

	/**
	 * Returns whether or not the remote host has browse host enabled.
	 *
	 * @return <tt>true</tt> if the remote host has browse host enabled,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isBrowseHostEnabled();

	/**
	 * Returns whether or not the remote host has chat enabled.
	 *
	 * @return <tt>true</tt> if the remote host has chat enabled,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isChatEnabled();

	/**
	 * Returns whether or not the remote host is reporting a speed that 
	 * has been measured by the application, as opposed to simply selected
	 * by the user..
	 *
	 * @return <tt>true</tt> if the remote host has as measured speed,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isMeasuredSpeed();
	
	/**
	 * Returns whether or not this was a response to a multicast query.
	 *
	 * @return <tt>true</tt> if this is a response to a multicast query,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isReplyToMulticastQuery();

    /**
     * Returns the <tt>Set</tt> of push proxies, which can be empty.
     *
     * @return a <tt>Set</tt> of push proxies, which can be empty
     */
    public Set<? extends IpPort> getPushProxies();

    /**
     * Returns whether or not this Host can do Firewalled Transfer.
     *
     */
    public boolean supportsFWTransfer();
    
    /**
     * 
     * @return the version of FWT protocol this host supports. 0 if none
     */
    public int getFWTVersionSupported();
    
    /** Returns true if the host supports TLS connections. */
    public boolean isTLSCapable();

}
