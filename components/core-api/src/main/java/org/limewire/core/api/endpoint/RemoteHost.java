package org.limewire.core.api.endpoint;

import java.net.UnknownHostException;

import org.limewire.io.Address;

public interface RemoteHost {
    
    Address getAddress() throws UnknownHostException;
    
    String getName();
    
    String getHostDescription();
    
    boolean isBrowseHostEnabled();
    
    boolean isChatEnabled();
    
    boolean isSharedFiles();
}
