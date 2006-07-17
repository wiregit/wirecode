/*
 * FixedSizeForgetfulHashMap.java
 *
 * Created on December 11, 2000, 2:08 PM
 */

package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
* A stronger version of ForgetfulHashMap.  Like ForgetfulHashMap, this is a
* mapping that "forgets" keys and values using a FIFO replacement policy, much
* like a cache.  Unlike ForgetfulHashMap, it has better-defined replacement
* policy.  Specifically, it allows a key to be remapped to a different value and
* then "renews" this key so it is the last key to be replaced.  All of this is
* done in constant time.<p>
*  
* @author Anurag Singla -- initial version
* @author Christopher Rohrs -- cleaned up and added unit tests
* @author Sam Berlin -- extend LinkedHashMap (adds unimplemented methods, simplifies) 
*/
public class FixedsizeForgetfulHashMap<K, V> extends LinkedHashMap<K, V> {

    /**  Maximum number of elements to be stored in the underlying hashMap */
    private final int MAXIMUM_SIZE;

    /**
     * Create a new instance that holds only the last "size" entries.
     * 
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size) {
        this(size, (size * 4)/3 + 10, 0.75f);
    }
    
    /**
     * Create a new instance that holds only the last "size" entries,
     * using the given initialCapacity and a loadFactor of 0.75.
     * 
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size, int initialCapacity) {
        this(size, initialCapacity, 0.75f);
    }
    
    /**
     * Create a new instance that holds only the last "size" entries, using
     * the given initialCapacity & loadFactor.
     * 
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        
        //if size is < 1
        if (size < 1)
            throw new IllegalArgumentException("invalid size: " + size);
    
        //set the max size to the size specified
        MAXIMUM_SIZE = size;
    }

    /**
     * Tests if the map is full
     * 
     * @return true, if the map is full (ie if adding any other entry will
     * lead to removal of some other entry to maintain the fixed-size property
     * of the map. Returns false, otherwise
     */
    public boolean isFull() {
        return size() >= MAXIMUM_SIZE;
    }
    
    /**
     * Removes the least recently used entry from the map
     * @return Value corresponding to the key-value removed from the map
     * @modifies this
     */
    public V removeLRUEntry() {
        //if there are no elements, return null.
        if(isEmpty())
            return null;
        
        Iterator<V> i = values().iterator();
        V value = i.next();
        i.remove();
        return value;
    }
    
    /**
     * Returns a shallow copy of this Map instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    @SuppressWarnings("unchecked")
    public FixedsizeForgetfulHashMap<K, V> clone() {
        return (FixedsizeForgetfulHashMap<K, V>)super.clone();
    }

    /**
     * Returns true if the eldest entry should be removed.
     */
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > MAXIMUM_SIZE;
    }

    /**
     * Overriden to ensure that remapping a key renews the value in the
     * linked list.
     */
    @Override
    public V put(K key, V value) {
        V ret = null;
        if(containsKey(key))
            ret = remove(key);
        
        super.put(key, value);
        
        return ret;
    }
}


