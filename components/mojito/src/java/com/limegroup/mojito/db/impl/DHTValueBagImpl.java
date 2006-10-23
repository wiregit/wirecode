package com.limegroup.mojito.db.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueBag;
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
        = DatabaseSettings.VALUE_LOAD_SMOOTHING_FACTOR.getValue();
    
    /**
     * The key if this value
     */
    private KUID valueId;

    /**
     * The request load associated with this DHT value bag
     */
    private float requestLoad = 0f;
    
    /**
     * The time the request load was last updated
     */
    private long lastRequestTime;
    
    /**
     * The Map of <OriginatorID,DHTValue>
     */
    private Map<KUID, DHTValue> valuesMap;
    
    public DHTValueBagImpl(KUID valueId) {
        this.valueId = valueId;
        valuesMap = new HashMap<KUID, DHTValue>();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#add(com.limegroup.mojito.db.DHTValue)
     */
    public boolean add(DHTValue value) {
        if (!valueId.equals(value.getValueID())) {
            throw new IllegalArgumentException("Value ID must be " + valueId);
        }
        
        KUID key = value.getOriginatorID();
        if ((value.isDirect() && putDirectDHTValue(key, value)) 
                || (!value.isDirect() && putIndirectDHTValue(key, value))) {
            
            valuesMap.put(key, value);
            return true;
        }
        
        return false;
    }
    
    public KUID getValueId() {
        return valueId;
    }

    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getDHTValuesMap()
     */
    public Map<KUID, DHTValue> getValuesMap() {
        return Collections.unmodifiableMap(valuesMap);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getDHTValues()
     */
    public Collection<DHTValue> getAllValues() {
        return valuesMap.values();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getRequestLoad()
     */
    public float getRequestLoad() {
        return requestLoad;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#incrementRequestLoad()
     */
    public float incrementRequestLoad() {
        
        //Use Exponentially weighted moving average (EMA) to compute load
        //on this particular value bag. 
        
        long now = System.currentTimeMillis();
        
        if(lastRequestTime == 0L) {
            lastRequestTime = now;
            return 0f;
        }
        
        float delay = (now - lastRequestTime)/1000f; //in sec
        
        if (delay <= 0f) {
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
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#remove(java.lang.Object)
     */
    public DHTValue remove(DHTValue value, boolean isRemoteRemove) {
        if(isRemoteRemove) {
            if(!value.isDirect() && !value.isLocalValue()) {
                return null;
            }
            
            KUID originatorId = value.getOriginatorID();
            DHTValue current = valuesMap.get(originatorId);
            if (current == null 
                    || (current.isLocalValue() && !value.isLocalValue())) {
                return null;
            }
        }
        
        return valuesMap.remove(value.getOriginatorID());
    }
    
    /**
     * Returns whether or not the given (direct) DHTValue can
     * be added to the bag
     */
    private boolean putDirectDHTValue(KUID key, DHTValue value) {
        assert (value.isDirect());
        
        // Do not replace local with non-local values
        DHTValue current = valuesMap.get(key);
        if (current != null 
                && current.isLocalValue() 
                && !value.isLocalValue()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns whether or not the given (indirect) DHTValue can
     * be added to the bag
     */
    private boolean putIndirectDHTValue(KUID key, DHTValue value) {
        assert (!value.isDirect());
        assert (!value.isLocalValue()); // cannot be a local value
        
        // Do not overwrite an directly stored value
        DHTValue current = valuesMap.get(key);
        if (current != null 
                && current.isDirect()) {
            return false;
        }
        
        return true;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#size()
     */
    public int size() {
        return valuesMap.size();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#isEmpty()
     */
    public boolean isEmpty() {
        return valuesMap.isEmpty();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#containsKey(com.limegroup.mojito.KUID)
     */
    public boolean containsKey(KUID key) {
        return valuesMap.containsKey(key);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Bag: ").append(getValueId()).append("\n");
        buffer.append("Load: ").append(getRequestLoad()).append("\n");
        buffer.append("Values:").append("\n");
        
        for(DHTValue value : getAllValues()) {    
            buffer.append(value).append("\n\n");
        }
        
        return buffer.toString();
    }
    
    
}