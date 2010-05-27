package org.limewire.mojito.entity;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.storage.DHTValueEntity;

public interface ValueEntity extends LookupEntity {

    public EntityKey getEntityKey();
    
    public DHTValueEntity[] getEntities();
    
    public EntityKey[] getEntityKeys();
}
