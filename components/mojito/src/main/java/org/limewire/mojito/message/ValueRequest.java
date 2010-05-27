package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.DHTValueType;

public interface ValueRequest extends LookupRequest {

    public KUID[] getSecondaryKeys();
    
    public DHTValueType getValueType();
}
