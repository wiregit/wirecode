package com.limegroup.mojito.util;

import java.net.SocketAddress;

/**
 * An interface for filtering and banning hosts 
 */
public interface HostFilter {
    
    public boolean allow(SocketAddress addr);
    
    public void ban(SocketAddress addr);
    
}
