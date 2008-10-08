package org.limewire.core.api.endpoint;

import org.limewire.io.Address;

public interface RemoteHost {
    
    String getId();
    
    Address getAddress();
    
    String getName();
    
    String getHostDescription();
    
    boolean isBrowseHostEnabled();
    
    boolean isChatEnabled();
    
    boolean isSharedFiles();
}
