package com.limegroup.gnutella.util;

import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;

/**
 * Map that is backed by two hash maps, which are occasionally
 * swapped and the older one cleared.
 */
public class DualMap extends AbstractMap {
    
    /**
     * The primary backing map.
     */
    protected Map _m1;
    
    /**
     * The secondary backing map.
     */
    protected Map _m2;
    
    /**
     * Constructs a new DualMap backed by two Maps created from createMap.
     */
    public DualMap() {
        _m1 = createMap();
        _m2 = createMap();
    }
    
    /**
     * Creates a new DualMap initially based off of maps a & b.
     */
    public DualMap(Map a, Map b) {
        _m1 = a;
        _m2 = b;
    }
    
    /**
     * Expires the eldest map.
     */
    public void expire() {
        _m2 = _m1;
        _m1 = createMap();
    }
    
    /**
     * Creates a new map.
     * Subclasses can override this method to have DualMap
     * act on different kinds of Map implementations, such as 
     * HashMap, LinkedHashMap, etc..
     * The default implementation returns a HashMap.
     */
    protected Map createMap() {
        return new HashMap();
    }
    
    /**
     * Clears both the primary & secondary map.
     */
    public void clear()  {
        _m1.clear(); _m2.clear();
    }
    
    /**
     * Returns true if either the primary or secondary map contain the key.
     */
    public boolean containsKey(Object key) {
        return _m1.containsKey(key) || _m2.containsKey(key);
    }
    
    /**
     * Returns true if either primary or second map contain the value.
     */
    public boolean containsValue(Object value)  {
        return _m1.containsValue(value) || _m2.containsValue(value);
    }
    
    /**
     * Returns a DualSet backed by the primary & secondary map's entrySet.
     */
    public Set entrySet() {
        return new DualSet(_m1.entrySet(), _m2.entrySet());
    }
    
    /**
     * Returns true if this map equals another map.
     */
    public boolean equals(Object o) {
        return super.equals(o);
    }
    
    /**
     * Gets the object associated with this key.
     */
    public Object get(Object key) {
        Object got = _m1.get(key);
        return got == null ? _m2.get(key) : got;
    }
    
    /**
     * Returns the hashcode.
     */
    public int hashCode() {
        return super.hashCode();
    }
    
    /**
     * Determines if the primary & secondary maps are empty.
     */
    public boolean isEmpty()  {
        return super.isEmpty();
    }
    
    /**
     * Returns a DualSet backed by the primary & secondary map's keySets.
     */
    public Set keySet() {
        return new DualSet(_m1.keySet(), _m2.keySet());
    }
    
    /**
     * Puts a key->value into the primary map, removing it if it exists in the
     * secondary map.
     */
    public Object put(Object key, Object value) {
        Object removed = _m2.remove(key);
        if(removed != null) {
            _m1.put(key, value);
            return removed;
        }
        
        return _m1.put(key, value);
    }
    
    /**
     * Puts all entries into the map.
     */
    public void putAll(Map t) {
        super.putAll(t);
    }

    /**
     * Attempts to remove the key from first the primary map and then the secondary map.
     */
    public Object remove(Object key) {
        Object removed = _m1.remove(key);
        if(removed != null)
            removed = _m2.remove(key);
        return removed;
    }
 
    /**
     * Returns the size of the primary & secondary map combined.
     */
    public int size() {
        return _m1.size() + _m2.size();
    }
    
    /**
     * Returns a DualCollection backed by the primary & secondary map's values.
     */
    public Collection values() {
        return new DualCollection(_m1.values(), _m2.values());
    }
}