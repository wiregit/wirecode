package com.limegroup.mojito.db.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueBag;

/**
 * This class allows to store multiple DHT values for the same key,
 * under the condition that the originators of those values are different.
 * It is backed by a <tt>HashMap</tt>, which maps originator <tt>KUID</tt>s 
 * to <tt>DHTValue</tt>s
 * 
 */
class DHTValueBagImpl implements DHTValueBag{
    
    private static final long serialVersionUID = 3677203930910637434L;
    
    /**
     * The key if this value
     */
    private KUID valueId;

    /**
     * The request load associated with this DHT value bag
     */
    private float requestLoad = 0f;
    
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
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getDHTValuesMap()
     */
    public Map<KUID, DHTValue> getValuesMap(){
        return valuesMap;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getDHTValues()
     */
    public Collection<DHTValue> getAllValues(){
        return valuesMap.values();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.mojito.db.impl.DHTValueBag#getRequestLoad()
     */
    public float getRequestLoad() {
        return requestLoad;
    }

//    /**
//     * Removes the given DHTValue from the bag if certain
//     * conditions met like it must be a direct remove
//     * request.
//     */
//    public DHTValue remoteRemove(DHTValue value) {
//        if (value.isDirect() 
//                || value.isLocalValue()) {
//            
//            KUID originatorId = value.getOriginatorID();
//            DHTValue current = valuesMap.get(originatorId);
//            if (current != null) {
//                if (!current.isLocalValue()
//                        || (current.isLocalValue() && value.isLocalValue())) {
//                    return remove(current, false);
//                }
//            }
//        }
//        
//        return null;
//    }
    
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
    public boolean containsKey(KUID key){
        return valuesMap.containsKey(key);
    }
}