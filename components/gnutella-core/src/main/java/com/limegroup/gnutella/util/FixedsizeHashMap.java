package com.limegroup.gnutella.util;

import java.util.*;
import com.limegroup.gnutella.util.*;

/**
 * This class implements fixed size HashMap. If its get full, no new entry
 * can be inserted into it, except by removing some entry first.
 * An attempt to add new entry throws an NoMoreStorageException
 * @see NoMoreStorageException
 */
public class FixedsizeHashMap
{

/**
* The number of elements in the underlying storage
*/
private int count = 0;

/**
* The max number of elements that can be stored
*/
private int size = 0;


private HashMap hashMap = null;

/**
 * Create a new hashMap that stores only the specified number of entries
 *
 * @param size the number of entries to hold
 * @exception IllegalArgumentException if size is less < 1.
 */
public FixedsizeHashMap(int size)
{
    hashMap = new HashMap(size * 4/3); //it might throw IllegalArgumentException
                       //in case the size is negative
    this.size = size;
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
public synchronized Object put(Object key, Object value) throws NoMoreStorageException
{
    Object retValue = null;
    
    //check if the count is less than size, in that case no problem
    //inserting this new entry
    if(count < size)
    {
        retValue = hashMap.put(key,value);
        if(retValue != null) //i.e if the mapping already existed
        {               //in that case no new mapping added
                        //and so no need to increment counts
        }
        else
        {
            //increment the count
            count++;
        }
    }
    else //if the hashmap is full
    {
        //if the entry already existed, we can safely add this new pair
        //without affecting the size
        retValue = hashMap.get(key);
        if(retValue != null)
        {
            //mapping existed, so update the mapping
            retValue = hashMap.put(key,value);
        }
        else //no space to enter anything more
        {
            //throw an exception
            throw new NoMoreStorageException();
        }
    }
    
    return retValue;
}

/**
* Returns the value mapped to the given key
* @param key The given key
* @return the value given key maps to
*/
public synchronized Object get(Object key)
{
    return hashMap.get(key);
}

/**
* Removes the mapping specified by given key from the underlying datastructure
* @param key The key to be removed
* @return the value associated with the key, or null if the key was not present
*/
public synchronized Object remove(Object key)
{
    //remove the mapping
    Object ret = hashMap.remove(key);
    
    //if the mapping existed
    if(ret != null)
    {
        //decrement the count
        count--;
    }
    //else do nothing
    
    //return the value the key mapped to (or else it'll be null)
    return ret;
    
}


/**
* Returns the Set that contains all the entries in the Map
* @return the Set that contains all the entries in the Map
*/
public synchronized Set entrySet()
{
    return hashMap.entrySet();
}

/**
 * checks if the hash Map is full or not (ie if for adding new item we need
 * to remove some item
 * This method is not synchronized (it doesnt matter much if the count is 1 off
 * or so)
 * @return true if the map is full, false otherwise
 */
public boolean isFull()

{
    return count >= size ;
}

/**
 * clears all entries from the map.
 */
public synchronized void clear()
{
    hashMap.clear();
}


/**
* Returns the string representation of the mappings
* @return the string representation of the mappings
*/
public synchronized String toString()
{
    return hashMap.toString();
}


}
