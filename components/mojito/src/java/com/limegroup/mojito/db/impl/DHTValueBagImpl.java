package com.limegroup.mojito.db.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValueBag;
import com.limegroup.mojito.db.DHTValueEntity;
import com.limegroup.mojito.settings.DatabaseSettings;

/**
 * This class allows to store multiple DHT values for the same key,
 * under the condition that the originators of those values are different.
 * It is backed by a <tt>HashMap</tt>, which maps originator <tt>KUID</tt>s 
 * to <tt>DHTValue</tt>s
 * 
 */
class DHTValueBagImpl implements DHTValueBag {
    
    private static final long serialVersionUID = 3677203930910637434L;
    
    private static final float SMOOTHING_FACTOR 
        = DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.getValue();
    
    private static final int LOAD_NULLING_DELAY 
    	= DatabaseSettings.VALUE_REQUEST_LOAD_NULLING_DELAY.getValue();
    
    /**
     * The key if this value
     */
    private final KUID primaryKey;

    /**
     * The request load associated with this DHT value bag
     * LOCKING: this
     */
    private float requestLoad = 0f;
    
    /**
     * The time the request load was last updated
     * LOCKING: this
     */
    private long lastRequestTime;
    
    /**
     * The Map of <OriginatorID,DHTValue>
     */
    private final Map<KUID, DHTValueEntity> entityMap;
    
    public DHTValueBagImpl(KUID primaryKey) {
        this.primaryKey = primaryKey;
        entityMap = Collections.synchronizedMap(new HashMap<KUID, DHTValueEntity>());
    }
    
    /**
     * 
     */
    public KUID getPrimaryKey() {
        return primaryKey;
    }
    
    /**
     * 
     */
    public Object getValuesLock() {
        return entityMap;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#add(com.limegroup.mojito.db.DHTValueEntity)
     */
    public boolean add(DHTValueEntity entity) {
        if (!primaryKey.equals(entity.getKey())) {
            throw new IllegalArgumentException("Key must be " + primaryKey);
        }
        
        entityMap.put(entity.getSecondaryKey(), entity);
        return true;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#get(com.limegroup.mojito.KUID)
     */
    public DHTValueEntity get(KUID secondaryKey) {
        return entityMap.get(secondaryKey);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#getValuesMap()
     */
    public Map<KUID, DHTValueEntity> getValuesMap() {
        return Collections.unmodifiableMap(entityMap);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#getAllValues()
     */
    public Collection<DHTValueEntity> getAllValues() {
        return entityMap.values();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getRequestLoad()
     */
    public synchronized float getRequestLoad() {
        return requestLoad;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#incrementRequestLoad()
     */
    public synchronized float incrementRequestLoad() {
        
        //Use Exponentially weighted moving average (EMA) to compute load
        //on this particular value bag. 
        
        long now = System.currentTimeMillis();
        
        if(lastRequestTime == 0L) {
            lastRequestTime = now;
            return 0f;
        }
        
        //we don't want to skew the results with delays that are too small!
        float delay = Math.max((now - lastRequestTime)/1000f, 0.01f); //in sec
        
        if (delay <= 0f || delay > LOAD_NULLING_DELAY) {
            lastRequestTime = now;
            requestLoad = 0f; //we can't trust the value anymore
            return requestLoad;
        }
        
        //The new requests per second is always a percentage of the 
        //increase over the old one:
        //newValue = SF*newLoad + (1 - SF)*previousValue
        //Which is equal to:
        //newValue = previousValue + SF * (newLoad - previousValue)
        requestLoad = requestLoad 
            + SMOOTHING_FACTOR*((1f/delay) - requestLoad);
        lastRequestTime = now;
        
        return requestLoad;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#remove(com.limegroup.mojito.db.DHTValueEntity)
     */
    public boolean remove(DHTValueEntity entity) {
        return (entityMap.remove(entity.getSecondaryKey()) != null);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#size()
     */
    public int size() {
        return entityMap.size();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#isEmpty()
     */
    public boolean isEmpty() {
        return entityMap.isEmpty();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#containsKey(com.limegroup.mojito.KUID)
     */
    public boolean containsKey(KUID key) {
        return entityMap.containsKey(key);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Bag: ").append(getPrimaryKey()).append("\n");
        buffer.append("Load: ").append(getRequestLoad()).append("\n");
        buffer.append("Values:").append("\n");
        
        synchronized(getValuesLock()) {
            for(DHTValueEntity entity : getAllValues()) {    
                buffer.append(entity).append("\n");  
            }
        }
        
        return buffer.toString();
    }
}