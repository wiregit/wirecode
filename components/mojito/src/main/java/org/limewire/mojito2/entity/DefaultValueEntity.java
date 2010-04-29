package org.limewire.mojito2.entity;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.io.LookupResponseHandler.State;
import org.limewire.mojito2.storage.DHTValueEntity;

public class DefaultValueEntity extends AbstractEntity implements ValueEntity {

    private final EntityKey lookupKey;
    
    private final DHTValueEntity[] entities;
    
    private final EntityKey[] entityKeys;
    
    public DefaultValueEntity(EntityKey lookupKey, 
            DHTValueEntity[] entities, 
            EntityKey[] entityKeys, 
            State state) {
        this(lookupKey, entities, entityKeys, 
                state.getTimeInMillis(), TimeUnit.MILLISECONDS);
    }
    
    public DefaultValueEntity(EntityKey lookupKey, 
            DHTValueEntity[] entities, 
            EntityKey[] entityKeys, 
            long time, TimeUnit unit) {
        
        super(time, unit);
        
        this.lookupKey = lookupKey;
        this.entities = entities;
        this.entityKeys = entityKeys;
    }

    @Override
    public KUID getKey() {
        return lookupKey.getPrimaryKey();
    }

    @Override
    public DHTValueEntity[] getEntities() {
        return entities;
    }

    @Override
    public EntityKey getEntityKey() {
        return lookupKey;
    }

    @Override
    public EntityKey[] getEntityKeys() {
        return entityKeys;
    }
}
