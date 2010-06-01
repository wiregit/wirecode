package org.limewire.mojito.message;

import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.ValueType;

public interface ValueRequest extends LookupRequest {

    public KUID[] getSecondaryKeys();
    
    public ValueType getValueType();
}
