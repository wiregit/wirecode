package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.ValueTuple;

public interface ValueResponse extends LookupResponse {

    public float getRequestLoad();
    
    public ValueTuple[] getValueEntities();
    
    public KUID[] getSecondaryKeys();
}
