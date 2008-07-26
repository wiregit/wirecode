package com.limegroup.gnutella;

import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

public interface HostDetails extends Connectable {
    /** Sets whether or not this host is TLS capable. */
    // TODO push up into Connectable?
    void setTLSCapable(boolean tlsCapable);

    /** 
     * Accessor for HTTP11.
     *
     * @return Whether or not we think this host supports HTTP11.
     */
    boolean isHTTP11();

    /**
     * Mutator for HTTP11.  Should be set after connecting.
     */
    void setHTTP11(boolean http11);

    /**
     * Accessor for the client guid for this file, which can be <tt>null</tt>.
     *
     * @return the client guid for this file, which can be <tt>null</tt>
     */
    byte[] getClientGUID();

    /**
     * Accessor for the speed of the host with this file, which can be 
     * <tt>null</tt>.
     *
     * @return the speed of the host with this file, which can be 
     *  <tt>null</tt>
     */
    int getSpeed();

    String getVendor();

    boolean isChatEnabled();

    boolean isBrowseHostEnabled();

    /**
     * Returns the "quality" of the remote file in terms of firewalled status,
     * whether or not the remote host has open slots, etc.
     * 
     * @return the current "quality" of the remote file in terms of the 
     *  determined likelihood of the request succeeding
     */
    int getQuality();

    /**
     * Determines whether or not this RFD was a reply to a multicast query.
     *
     * @return <tt>true</tt> if this RFD was in reply to a multicast query,
     *  otherwise <tt>false</tt>
     */
    boolean isReplyToMulticast();

    /**
     * Accessor for the <tt>Set</tt> of <tt>PushProxyInterface</tt>s for this
     * file -- can be empty, but is guaranteed to be non-null.
     *
     * @return the <tt>Set</tt> of proxy hosts that will accept push requests
     *  for this host -- can be empty
     */
    Set<? extends IpPort> getPushProxies();

    /**
     * @return whether this RFD supports firewall-to-firewall transfer.
     * For this to be true we need to have some push proxies, indication that
     * the host supports FWT and we need to know that hosts' external address.
     */
    boolean supportsFWTransfer();
    
    /**
	 * Returns whether or not the host that holds this file is firewalled.
	 * @return
	 */	
	boolean isFirewalled();
}
