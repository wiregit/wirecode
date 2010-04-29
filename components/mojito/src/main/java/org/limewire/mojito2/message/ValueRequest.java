package org.limewire.mojito2.message;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.storage.DHTValueType;

public interface ValueRequest extends LookupRequest {

    public KUID[] getSecondaryKeys();
    
    public DHTValueType getValueType();
}
