package org.limewire.mojito2.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito2.storage.DHTValueEntity;

public interface ValueResponse extends LookupResponse {

    public float getRequestLoad();
    
    public DHTValueEntity[] getValueEntities();
    
    public KUID[] getSecondaryKeys();
}
