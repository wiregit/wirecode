package com.limegroup.mojito.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.limegroup.mojito.KUID;

/**
 * A interface that represents a bag of DHT values that have the same ID. 
 * Used to represent multiple values coming from different originators.
 */
public interface DHTValueBag extends Serializable {

    /**
     * Adds a <tt>DHTValue</tt> to this value bag. The value must
     * have the same ID as this bag.
     * 
     * @return true if the value was added, false otherwise
     */
    public boolean add(DHTValue value);
    
    /**
     * Removes a <tt>DHTValue</tt> from this value bag.
     * 
     * @return true if the value was removed, false otherwise
     */
    public boolean remove(DHTValue value);
    
    /**
     * Return's this Bag's value ID.
     * 
     */
    public KUID getValueId();

    /**
     * Returns an unmodifiable map of values where the key is the originator's ID.
     * 
     */
    public Map<KUID, DHTValue> getValuesMap();

    /**
     * Returns the Bag of DHT values
     */
    public Collection<DHTValue> getAllValues();

    /**
     * Returns the request load associated with this value bag, i.e. the
     * approximate request popularity.
     * 
     */
    public float getRequestLoad();

    /**
     * Increments the request load of this bag and returns the new value
     * 
     * @return The updated request load
     */
    public float incrementRequestLoad();
    
    /**
     * Returns the number of values in this bag.
     */
    public int size();

    /**
     * Returns true if this bag is empty.
     */
    public boolean isEmpty();

    /**
     * 
     * @param nodeId
     * @return
     */
    public DHTValue get(KUID nodeId);
    
    /**
     * Returns true if this bag contains a value coming from the 
     * specified originator <tt>KUID</tt>.
     * 
     * @param nodeId The value's originator KUID
     */
    public boolean containsKey(KUID nodeId);
}