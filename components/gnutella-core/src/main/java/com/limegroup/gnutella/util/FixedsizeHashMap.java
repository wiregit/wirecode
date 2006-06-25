package com.limegroup.gnutella.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements fixed size HashMap. If its get full, no new entry
 * can be inserted into it, except by removing some entry first.
 * An attempt to add new entry throws an NoMoreStorageException
 * @see NoMoreStorageException
 */
public class FixedsizeHashMap<K, V> {
    
    /**
     * The underlying storage
     */
    private final Map<K, V> hashMap;
    
    /**
     * The max number of elements that can be stored
     */
    private final int maxSize;
    
    /**
     * Create a new hashMap that stores only the specified number of entries
     *
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeHashMap(int size)
    {
        hashMap = new HashMap<K, V>(size * 4/3);
        this.maxSize = size;
    }
    
    /**
     * Maps the given key to the given value. If adding the key
     * would make this contain more elements than the size given at
     * construction, the passed entry is not stored and NoMoreStorageException
     * gets throwned.
     * @exception NoMoreStorageException when no more space left in the storage
     * ideally, before calling put method, it should be checked whether the map is
     * already full or not
     * @see isfull()
     */
    public synchronized V put(K key, V value) throws NoMoreStorageException
    {
        V retValue = null;
        
        //check if the count is less than size, in that case no problem
        //inserting this new entry
        if(hashMap.size() < maxSize) 
            retValue = hashMap.put(key,value);
        else {
            //if the entry already existed, we can safely add this new pair
            //without affecting the size
            retValue = hashMap.get(key);
            
            if(retValue != null) //mapping existed, so update the mapping 
                retValue = hashMap.put(key,value);
            else //no space to enter anything more 
                throw new NoMoreStorageException();
        }
        
        return retValue;
    }
    
    /**
     * Returns the value mapped to the given key
     * @param key The given key
     * @return the value given key maps to
     */
    public synchronized V get(K key) {
        return hashMap.get(key);
    }
    
    /**
     * clears all entries from the map.
     */
    public synchronized void clear() {
        hashMap.clear();
    }
    
    /**
     * Returns the string representation of the mappings
     * @return the string representation of the mappings
     */
    public synchronized String toString() {
        return hashMap.toString();
    }
    
    
}
