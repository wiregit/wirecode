package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.ValueKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.ValueTuple;
import org.limewire.mojito.io.LookupResponseHandler.State;

/**
 * The default implementation of {@link ValueEntity}.
 */
public class DefaultValueEntity extends AbstractEntity implements ValueEntity {

    private final ValueKey lookupKey;
    
    private final ValueTuple[] entities;
    
    private final ValueKey[] entityKeys;
    
    public DefaultValueEntity(ValueKey lookupKey, 
            ValueTuple[] entities, 
            ValueKey[] entityKeys, 
            State state) {
        this(lookupKey, entities, entityKeys, 
                state.getTimeInMillis(), TimeUnit.MILLISECONDS);
    }
    
    public DefaultValueEntity(ValueKey lookupKey, 
            ValueTuple[] entities, 
            ValueKey[] entityKeys, 
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
    public ValueTuple[] getValues() {
        return entities;
    }

    @Override
    public ValueKey getValueKey() {
        return lookupKey;
    }

    @Override
    public ValueKey[] getValueKeys() {
        return entityKeys;
    }
}
