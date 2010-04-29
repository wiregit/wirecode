package org.limewire.mojito2.entity;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito2.storage.DHTValueEntity;

public interface ValueEntity extends LookupEntity {

    public EntityKey getEntityKey();
    
    public DHTValueEntity[] getEntities();
    
    public EntityKey[] getEntityKeys();
}
