package org.limewire.mojito.entity;

import java.util.Collection;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.db.DHTValueEntity;

public interface ValueEntity extends LookupEntity {

    public EntityKey getEntityKey();
    
    public Collection<? extends DHTValueEntity> getEntities();
    
    public Collection<? extends EntityKey> getEntityKeys();
}
