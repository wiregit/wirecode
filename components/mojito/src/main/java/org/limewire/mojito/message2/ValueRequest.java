package org.limewire.mojito.message2;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueType;

public interface ValueRequest extends LookupRequest {

    public KUID[] getSecondaryKeys();
    
    public DHTValueType getValueType();
}
