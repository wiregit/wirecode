/*
 * FixedSizeForgetfulHashMap.java
 *
 * Created on December 11, 2000, 2:08 PM
 */

package com.limegroup.gnutella.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.Assert;

/**
* A stronger version of ForgetfulHashMap.  Like ForgetfulHashMap, this is a
* mapping that "forgets" keys and values using a FIFO replacement policy, much
* like a cache.  Unlike ForgetfulHashMap, it has better-defined replacement
* policy.  Specifically, it allows a key to be remapped to a different value and
* then "renews" this key so it is the last key to be replaced.  All of this is
* done in constant time.<p>
*
* Restrictions:
* <ul>
* <li>The changes to this map should be made only thru the methods provided and not
*    thru any iterator/set of keys/values returned.
* <li>Values in the hash map may not be null.
* <li><b>This class is not thread safe.</b>  Synchronize externally if needed.
* </ul>
* 
* Note that <b>some methods of this are unimplemented</b>.  Also note that this
* implements Map but does not extend HashMap, unlike ForgetfulHashMap.
*  
* @author Anurag Singla -- initial version
* @author Christopher Rohrs -- cleaned up and added unit tests 
*/
public class FixedsizeForgetfulHashMap<K, V> implements Map<K, V>
{
    /* Implementation note:
     *
     * To avoid linear-time operations, this maintains an internal linked
     * list(removeList) to manage/figure-out which elements to remove when the
     * underlying hashMap reaches user defined size, and a new mapping needs to
     * be added.  Whenever we insert any thing to the underlying hashMap, we
     * also add an entry in the removeList (we add the entry at the last of the
     * list) When the underlying hashMap reaches the user defined size, we
     * remove an element from the underlying hashMap before inserting a new one.
     * The element removed is the one which is first in the removeList (ie the
     * element that was inserted first.)
     *
     * If we insert same 'key' twice to the underlying hashMap, we remove 
     * the previous entry in the removeList(if present) (its similar to
     * changing the remove timestamp for that entry). In other words, adding a
     * key again, removes the previous footprints (ie it again becomes the last
     * element to be removed, irrespective of the history(previous position)) 
     *
     * ABSTRACTION FUNCTION: a typical FixedsizeForgetfulHashMap is a list of
     * key value pairs [ (K1, V1), ... (KN, VN) ] ordered from oldest to
     * youngest where
     *         K_I=removeList.get(I)
     *         V_I=map.get(K_I).getValue()
     *  
     * INVARIANTS: here "a=b" is  shorthand for "a.equals(b)"
     *   +for all keys k in map, where ve==map.get(k),  
     *          ve.getListElement() is an element of list
     *          ve.getListElement().getKey()=k
     *          k!=null && ve!=null && ve.getValue()!=null  (no null values!)
     *   +for all elements l in removeList, where k=l.getKey() and ve=map.get(l)
     *          ve!=null (i.e., k is a key in map)
     *          ve.getListElement=l
     *
     * A corrolary of this invariant is that no duplicate keys may be stored in
     * removeList.
     */

    /** The underlying map from keys to [value, list element] pairs */
    private Map<K, ValueElement<K, V>> map;

    /**
     * A linked list of the keys in the hashMap. It is used to remove the 
     * elements from the underlying hashMap datastructure in FIFO order
     * Newer elements are stored in the tail.
     */
    private DoublyLinkedList<K> removeList = 
        new DoublyLinkedList<K>();

    /**
     * Maximum number of elements to be stored in the underlying hashMap
     */
    private int maxSize;

    /**
     * current number of elements in the underlying hashMap
     */
    private int currentSize;


    /**
     * class to store the value to be stored in the hashMap
     * It keeps both the actual value (that user wanted to insert), and the 
     * entry in the removeList that corresponds to this mapping.
     * This information is required so that when we overwrite the mapping (same key,
     * but different value), we should update the removeList entries accordingly.
     */
    private static class ValueElement<K, E>
    {
        /** The element in the remove list that corresponds to this mapping */
        DoublyLinkedList.ListElement<K> listElement;    
        /** The actual value (that user wanted to store in the hash map) */
        E value;
    
        /**
         * Creates a new instance with specified values
         * @param value The actual value (that user wanted to store in the hash map)
         * @param listElement The element in the remove list that corresponds 
         * to this mapping
         */
        public ValueElement(E value,
                            DoublyLinkedList.ListElement<K> listElement) {
            //update the member fields
            this.value = value;
            this.listElement = listElement;
        }
    
        /** Returns the element in the remove list that corresponds to this
         *  mapping thats stored in this instance */
        public DoublyLinkedList.ListElement<K> getListElement() {
            return listElement;
        }
    
        /** Returns the value stored */
        public E getValue() {
            return value;
        }
        
        /**
         * Returns true if the value of these elements are equal.
         * Needed for map.equals(other.map) to work.
         */
        public boolean equals(Object o) {
            if ( o == this ) return true;
            if ( !(o instanceof ValueElement) )
                return false;
            ValueElement other = (ValueElement)o;
            return value.equals(other.value);
        }
        
        /**
         * Returns the hashcode of the value element.
         * Needed for map.hashCode() to work.
         */
        public int hashCode() {
            return value.hashCode();
        }
    }

    /**
     * Create a new instance that holds only the last "size" entries.
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeForgetfulHashMap(int size)
    {
        //allocate space in underlying hashMap
        map=new HashMap<K, ValueElement<K, V>>((size * 4)/3 + 10, 0.75f);
    
        //if size is < 1
        if (size < 1)
            throw new IllegalArgumentException();
    
        //no elements stored at present. Therefore, set the current size to zero
        currentSize = 0;
    
        //set the max size to the size specified
        maxSize = size;
    }

    /** Returns the value associated with this key. 
     *  @return the value associated with this key, or null if no association 
     *   (possibly because the key was expired)
     */
    public V get(Object key)
    {
        ValueElement<K, V> pair = map.get(key);
        return (pair==null) ? null : pair.getValue();
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced. Also if any of the key/value is null, the entry
     * is not inserted.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key, which must
     *         not be null
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key..
     */
    public V put(K key, V value)
    {
        //add the new mapping to the underlying hashmap data structure
        //add only if not null.  This isn't strictly needed our specification
        //disallows null keys (implicitly) and null values (explicitly).
        if(key == null || value == null)
            return null;
    
        //add the mapping
        //the method takes care of adding the information to the remove list
        //and other details (like updating current count)
        V oldValue = addMapping(key,value);   

        //return the old value
        return oldValue;
    }

    /**
     * Adds the specified key=>value mapping after wrapping the value to 
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
    private V addMapping(K key, V value)
    {
        //add the newly inserted element to the removeList
        DoublyLinkedList.ListElement<K> listElement = removeList.addLast(key);
            
        //insert the mapping in the hashmap (after wrapping the value properly)
        //save the element removed
        ValueElement<K, V> ret = map.put(key, new ValueElement<K, V>(value, listElement));
        
        //if a mapping already existed, remove the entry corresponding to 
        //the old value from the removeList
        if(ret != null) {
            removeList.remove(ret.getListElement());
        } else {
            //else increment the count of entries
            currentSize++;
        }
    
        //if the count is more than max, means we need to remove an entry
        if(currentSize > maxSize) {
            //get an element from the remove list to remove
            DoublyLinkedList.ListElement<K> toRemove = removeList.removeFirst();

            //remove it from the hashMap
            map.remove(toRemove.getKey());
        
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
     * Tests if the map is full
     * @return true, if the map is full (ie if adding any other entry will
     * lead to removal of some other entry to maintain the fixed-size property
     * of the map. Returns false, otherwise
     */
    public boolean isFull()
    {
        //if the count is more than max
        if(currentSize >= maxSize)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Removes the least recently used entry from the map
     * @return Value corresponding to the key-value removed from the map
     * @modifies this
     */
    public V removeLRUEntry()
    {
        //if there are no elements, return null.
        if(isEmpty())
            return null;
        
        //get an element from the remove list to remove
        DoublyLinkedList.ListElement<K> toRemove = removeList.removeFirst();

        //remove it from the hashMap
        ValueElement<K, V> removed = map.remove(toRemove.getKey());
        
        //decrement the count
        currentSize--;
        
        //return the removed element (value)
        return removed.getValue();
    }
    

    /**
     * Copies all of the mappings from the specified map to this one.
     * 
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified Map.
     * As this is fixed size mapping, some older entries may get removed
     *
     * @param t Mappings to be stored in this map.
     */
    public void putAll(Map<? extends K, ? extends V> t) {
        for(Map.Entry<? extends K, ? extends V> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Returns a shallow copy of this Map instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    public Object clone() {
        //create a clone map of required size
        Map<K, V> clone = new HashMap<K, V>((map.size() * 4)/3 + 10, 0.75f);
        for(Map.Entry<K, ValueElement<K, V>> entry : map.entrySet()) {
            //add it to the clone map
            //add only the value (and not the ValueElement wrapper instance
            //that is stored internally
            clone.put(entry.getKey(), entry.getValue().getValue());
        }
        
        //return the clone
        return clone;
        
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.
     */
    public V remove(Object key) {
        //save the element removed
        ValueElement<K, V> ret = map.remove(key);
    
        //if the mapping existed
        if(ret != null) {
            //decrement the current size
            currentSize--;
        
            //remove it from the removeList
            removeList.remove(ret.getListElement());
        
            return ret.getValue();
        } else {
            return null;
        }
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() 
    {
        //clear everything from the underlying data structure
        map.clear();
    
        //set the current size to zero
        currentSize = 0;
    
        //remove all the entries from remove list
        removeList.clear();
    }

    /////////////////////////// Implemented Map Methods ////////////////

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean equals(Object o) {
        if ( o == this ) return true;
        if(!(o instanceof FixedsizeForgetfulHashMap))
            return false;
        FixedsizeForgetfulHashMap other=(FixedsizeForgetfulHashMap)o;
        return map.equals(other.map);
    }
    
    public int hashCode() {
        return map.hashCode();
    }
            
    public boolean isEmpty() {
        return map.isEmpty();
    }


    public int size() {
        return map.size();
    }

    /////////////////////////// Unimplemented Map Methods //////////////

    /** <b>Partially implemented.</b>  
     *  Only keySet().iterator() is well defined. */
    public Set<K> keySet() {
        return new KeySet(map.keySet());
    }    
    
    class KeySet extends AbstractSet<K> {
        Set<K> real;
        
        KeySet(Set<K> real) {
            this.real=real;
        }
        public Iterator<K> iterator() {
            return new KeyIterator(real.iterator());
        }
        
        public int size() {
            return FixedsizeForgetfulHashMap.this.size();
        }
    }
    class KeyIterator implements Iterator<K> {
        Iterator<K> real;
        Object lastYielded=null;
        KeyIterator(Iterator<K> real) {
            this.real=real;
        }
        public K next() {
            K ret=real.next();
            lastYielded=ret;
            return ret;
        }
        public boolean hasNext() {
            return real.hasNext();
        }
        /** Same as Iterator.remove().  That means that calling remove()
         *  multiple times may have undefined results! */
        public void remove() {
            if (lastYielded==null)
                return;
            //Cleanup entry in removeList.  Note that we cannot simply call
            //FixedsizeForgetfulHashMap.this.remove(lastYielded) since that may
            //affect the underlying map--while iterating through it.
            ValueElement<K, V> ve = map.get(lastYielded);
            if (ve != null) { //not strictly needed by specification of remove.
                currentSize--;
                removeList.remove(ve.getListElement());      
            }
            //Cleanup entry in underlying map.  This MUST be done through
            //the iterator only, to prevent inconsistent state.
            real.remove();
        }
    }

    /** <b>Not implemented; behavior undefined</b> */
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    /** <b>Not implemented; behavior undefined</b> */
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /** <b>Not implemented; behavior undefined</b> */
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }
 
    //////////////////////////////////////////////////////////////////////

    /** Tests the invariants described above. */
    public void repOk() {
        for(K k : map.keySet()) {
            Assert.that(k!=null, "Null key (1)");
            ValueElement<K, V> ve = map.get(k);
            Assert.that(ve!=null, "Null value element (1)");
            Assert.that(ve.getValue()!=null, "Null real value (1)");
            Assert.that(removeList.contains(ve.getListElement()), 
                        "Invariant 1a failed");
            Assert.that(ve.getListElement().getKey().equals(k),
                        "Invariant 1b failed");
        }

        for(DoublyLinkedList.ListElement<K> l : removeList) {
            K k = l.getKey();
            Assert.that(k!=null, "Null key (2)");
            ValueElement<K, V> ve = map.get(k);
            Assert.that(ve!=null, "Null value element (2)");
            Assert.that(ve.getListElement().equals(l), "Invariant 2b failed");
        }
    }
}


