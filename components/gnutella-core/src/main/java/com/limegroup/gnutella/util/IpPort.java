package com.limegroup.gnutella.util;

import java.net.InetAddress;

/**
 * Utility interface that allows class containing host information to be 
 * used generically.
 */
public interface IpPort {

    /**
     * Accessor for the <tt>InetAddress</tt> for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */
    InetAddress getInetAddress();
    
    /**
     * Accessor for the port this host is listening on.
     * 
     * @return the port this host is listening on
     */
    int getPort();

    /**
     * Accessor for the address string.
     * 
     * @return the address of this host as a string
     */
    String getAddress();
}
