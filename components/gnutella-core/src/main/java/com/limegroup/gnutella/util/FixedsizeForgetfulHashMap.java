/*
 * FixedSizeForgetfulHashMap.java
 *
 * Created on December 11, 2000, 2:08 PM
 */

package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;

/**
 *
 * @author  Anurag Singla
 * @version
 */

/**
* It is an extension of hash map that stores only fixed
* number of entries. Only the last 'n' (user-defined size) entries
* are stored.
* <br>
* Operation:
* It maintains an internal linked list(removeList) to manage/figure-out which 
* elements to remove when the underlying hashMap reaches user defined size, and
* a new mapping needs to be added.
* When ever we insert any thing to the underlying hashMap, we also add 
* an entry in the removeList (we add the entry at the last of the list)
* When the underlying hashMap reaches the userdefined size, we remove 
* an element from the underlying hashMap before inserting a new one.
* The element removed is the one which is first in the removeList (ie the
* element that was inserted first (inserted earliest in time) with an 
* exception:
*   If we insert same 'key' twice to the underlying hashMap, we remove 
* the previous entry in the removeList(if present) (its similar to
* changing the remove timestamp for that entry). In other words, adding a
* key again, removes the previous footprints (ie it again becomes the last
* element to be removed, irrespective of the history(previous position))
* <br>
* Note: The changes to this map should be made only thru the methods provided
* and not thru any iterator/set of keys/values returned.
* Also note that this implementation of hash map doesnt store any null
* values
* <br>
* Note: All the access to this datastructure should be synchronized 
* externally(if required)
*/
public class FixedsizeForgetfulHashMap extends HashMap
{

/**
* A linked list of the keys in the hashMap. It is used to remove the 
* elements from the underlying hashMap datastructure in FIFO order
*/
private DoublyLinkedList removeList = new DoublyLinkedList();

/**
* Maximum number of elements to be stored in the underlying hashMap
*/
private int maxSize;

/**
* current number of elements in the underlying hashMap
*/
private int currentSize;

/**
* Create a new instance that holds only the last "size" entries.
* @param size the number of entries to hold
* @exception IllegalArgumentException if size is less < 1.
*/
public FixedsizeForgetfulHashMap(int size)
{
    //allocate space in underlying hashMap
    super(size * 4/3 + 10, 0.75f);
    
    //if size is < 1
    if (size < 1)
        throw new IllegalArgumentException();
    
    //no elements stored at present. Therefore, set the current size to zero
    currentSize = 0;
    
    //set the max size to the size specified
    maxSize = size;
}


/**
* Associates the specified value with the specified key in this map.
* If the map previously contained a mapping for this key, the old
* value is replaced. Also if any of the key/value is null, the entry
* is not inserted.
*
* @param key key with which the specified value is to be associated.
* @param value value to be associated with the specified key.
* @return previous value associated with specified key, or <tt>null</tt>
*	       if there was no mapping for key.  A <tt>null</tt> return can
*	       also indicate that the HashMap previously associated
*	       <tt>null</tt> with the specified key.
*/
public Object put(Object key, Object value)
{
    //add the new mapping to the underlying hashmap data structure
    //add only if not null
    if(key == null || value == null)
        return null;
    
    //add the mapping
    //the method takes care of adding the information to the remove list
    //and other details (like updating current count)
    Object oldValue = addMapping(key,value);
    
    //return the old value
    return oldValue;
}

/**
* It adds the specified key=>value mapping after wrapping the value to 
* maintain additional information. If an entry needs to be removed to 
* accomodate this new mapping (as it can increase the max number of elements 
* to be retained, as specified by the user), it removes the earliest element
* enetred, as explained in the class description. It updates various counts, 
* as well as the removeList to reflect the updates
* @param key key with which the specified value is to be associated.
* @param value value to be associated with the specified key.
* @return previous value associated with specified key, or <tt>null</tt>
*	       if there was no mapping for key.  A <tt>null</tt> return can
*	       also indicate that the HashMap previously associated
*	       <tt>null</tt> with the specified key.
* @modifies currentCount, 'this', removeList
*/
private Object addMapping(Object key, Object value)
{
    //create a list element to be stored
    DoublyLinkedList.ListElement listElement 
                    = DoublyLinkedList.getANewListElement(key);
    
    //insert the mapping in the hashmap (after wrapping the value properly)
    //save the element removed
    ValueElement ret = (ValueElement)super.put(key, new ValueElement(value, 
                                                                listElement));
    
    //add the newly inserted element to the removeList
    removeList.addLast(listElement);
    
    //if a mapping already existed, remove the entry corresponding to 
    //the old value from the removeList
    if(ret != null)
    {
        removeList.remove(ret.getListElement());
    }
    else
    {
        //else increment the count of entries
        currentSize++;
    }
    
    //if the count is more than max, means we need to remove an entry
    if(currentSize > maxSize)
    {
        //get an element from the remove list to remove
        DoublyLinkedList.ListElement toRemove = removeList.removeFirst();
        
        //remove it from the hashMap
        super.remove(toRemove.getKey());
        
        //decrement the count
        currentSize--;
    }
    
    //return the previous mapping
    if(ret == null)
        return null;
    else
        return ret.getValue();
}


/**
* Copies all of the mappings from the specified map to this one.
* 
* These mappings replace any mappings that this map had for any of the
* keys currently in the specified Map.
* As this is fixed siz emapping, some older entries may get removed
*
* @param t Mappings to be stored in this map.
*/
public void putAll(Map t)
{
    Iterator iter=t.keySet().iterator();
    while (iter.hasNext())
    {
        Object key=iter.next();
        put(key,t.get(key));
    }
}

/**
* Removes the mapping for this key from this map if present.
*
* @param key key whose mapping is to be removed from the map.
* @return previous value associated with specified key, or <tt>null</tt>
*	       if there was no mapping for key.  A <tt>null</tt> return can
*	       also indicate that the map previously associated <tt>null</tt>
*	       with the specified key.
*/
public Object remove(Object key) 
{
    //save the element removed
    ValueElement ret = (ValueElement)super.remove(key);
    
    //if the mapping existed
    if(ret != null)
    {
        //decrement the current size
        currentSize--;
        
        //remove it from the removeList
        removeList.remove(ret.getListElement());
        
        return ret.getValue();
    }
    else
    {
        return null;
    }
}

/**
* Removes all mappings from this map.
*/
public void clear() 
{
    //clear everything from the underlying data structure
    super.clear();
    
    //set the current size to zero
    currentSize = 0;
    
    //remove all the entries from remove list
    removeList.clear();
}

/**
* class to store the value to be stored in the hashMap
* It keeps both the actual value (that user wanted to insert), and the 
* entry in the removeList that corresponds to this mapping.
* This information is required so that when we overwrite the mapping (same key,
* but different value), we should update the removeList entries accordingly.
*/
private static class ValueElement
{
    /**
    * The element in the remove list that corresponds to this mapping
    */
    DoublyLinkedList.ListElement listElement;
    
    /**
    * The actual value (that user wanted to store in the hash map)
    */
    Object value;
    
    /**
    * Creates a new instance with specified values
    * @param value The actual value (that user wanted to store in the hash map)
    * @param listElement The element in the remove list that corresponds 
    * to this mapping
    */
    public ValueElement(Object value, DoublyLinkedList.ListElement listElement)
    {
        //update the member fields
        this.value = value;
        this.listElement = listElement;
    }
    
    /**
    * Returns the element in the remove list that corresponds to this mapping
    * thats stored in this instance
    * @return the element in the remove list that corresponds to this mapping
    * thats stored in this instance
    */
    public DoublyLinkedList.ListElement getListElement()
    {
        return listElement;
    }
    
    /**
    * Returns the value stored
    * @return the value stored
    */
    public Object getValue()
    {
        return value;
    }
}

}


