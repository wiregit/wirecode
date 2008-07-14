package org.limewire.core.api.endpoint;

import java.util.List;

public interface RemoteHost {
    
    String getHostDescription();
    
    List<RemoteHostAction> getHostActions();

}
