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
        = DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.getValue();
    
    private static final int LOAD_NULLING_DELAY 
    	= DatabaseSettings.VALUE_REQUEST_LOAD_NULLING_DELAY.getValue();
    
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
    
    public KUID getValueId() {
        return valueId;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#add(com.limegroup.mojito.db.DHTValue)
     */
    public boolean add(DHTValue value) {
        if (!valueId.equals(value.getValueID())) {
            throw new IllegalArgumentException("Value ID must be " + valueId);
        }
        
        valuesMap.put(value.getOriginatorID(), value);
        return true;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.DHTValueBag#get(com.limegroup.mojito.KUID)
     */
    public DHTValue get(KUID nodeId) {
        return valuesMap.get(nodeId);
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
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#remove(java.lang.Object)
     */
    public boolean remove(DHTValue value) {
        return (valuesMap.remove(value.getOriginatorID()) != null);
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