package com.limegroup.gnutella;

import com.limegroup.gnutella.util.IpPort;

/**
 * Interface for any class that wishes to be an oberver/listener of new host
 * data.
 */
public interface HostListener {

    /**
     * Adds the specified host to the listener.
     * 
     * @param host the host to add
     */
    void addHost(IpPort host);
}
