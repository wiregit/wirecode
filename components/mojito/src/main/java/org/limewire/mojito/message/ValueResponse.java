package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.DHTValueEntity;

public interface ValueResponse extends LookupResponse {

    public float getRequestLoad();
    
    public DHTValueEntity[] getValueEntities();
    
    public KUID[] getSecondaryKeys();
}
