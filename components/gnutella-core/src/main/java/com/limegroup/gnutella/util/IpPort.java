package com.limegroup.gnutella.util;

import java.net.InetAddress;

/**
 * Generic interface for classes containing IP/port pairs.
 */
public interface IpPort {
    
    /**
     * Accessor for the <tt>InetAddress</tt> for this class.
     * 
     * @return the <tt>InetAddress</tt> for this class
     */
    InetAddress getAddress();
    
    /**
     * Accessor for the port for this host.
     * 
     * @return the port for this host
     */
    int getPort();
}
