package org.limewire.mojito.message2;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;

public interface ValueResponse extends LookupResponse {

    public float getRequestLoad();
    
    public DHTValueEntity[] getValueEntities();
    
    public KUID[] getSecondaryKeys();
}
