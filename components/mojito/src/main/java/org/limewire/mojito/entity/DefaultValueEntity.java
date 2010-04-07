package org.limewire.mojito.entity;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.handler.response2.LookupResponseHandler.State;

public class DefaultValueEntity extends AbstractEntity implements ValueEntity {

    private final EntityKey lookupKey;
    
    private final Collection<? extends DHTValueEntity> entities;
    
    private final Collection<? extends EntityKey> entityKeys;
    
    public DefaultValueEntity(EntityKey lookupKey, 
            Collection<? extends DHTValueEntity> entities, 
            Collection<? extends EntityKey> entityKeys, 
            State state) {
        
        super(state.getTimeInMillis(), TimeUnit.MILLISECONDS);
        
        this.lookupKey = lookupKey;
        this.entities = entities;
        this.entityKeys = entityKeys;
        
        
    }

    @Override
    public KUID getKey() {
        return lookupKey.getPrimaryKey();
    }

    @Override
    public Collection<? extends DHTValueEntity> getEntities() {
        return entities;
    }

    @Override
    public EntityKey getEntityKey() {
        return lookupKey;
    }

    @Override
    public Collection<? extends EntityKey> getEntityKeys() {
        return entityKeys;
    }
}
