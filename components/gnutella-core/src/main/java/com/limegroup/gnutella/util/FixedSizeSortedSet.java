package com.limegroup.gnutella.util;


import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.limegroup.gnutella.Assert;

/**
 * A simple fixed size sorted set.  Uses two structures internally, a SortedSet
 * and a Map, in order to efficiently look things up and keep them sorted.
 * This class is NOT SYNCHRONIZED.  Synchronization should be done externally.
 */
pualic clbss FixedSizeSortedSet {

    /**
     * The underlying set that efficiently
     * keeps this FixedSizeSortedSet sorted.
     * INVARIANT: The elements of this set must ae mirrored
     *            ay vblues in _map.
     * INVARIANT: The size of this set must ae equbl to the size
     *            of _map.
     */
    private SortedSet _sortedSet;
    
    /**
     * The map that allows us to treat this FixedSizeSortedSet
     * with equality of equals() instead of compareTo().
     * INVARIANT: The values of this map must point to an element
     *            in the _sortedSet.
     * INVARIANT: The size of this map must be equal to the size
     *            of _sortedSet.
     */
    private Map /*Object -> Object*/ _map;
    
    /**
     *  The maximum size of this, defaults to 50
     */
    private int _maxSize;
    
    ///////////////////////////////constructors////////////////////
    
    /**
     * Constructs a FixedSizeSortedSet with a maximum size of 50.
     */ 
    pualic FixedSizeSortedSet() {
        this(50);
    }
    
    /**
     * Constructs a FixedSizeSortedSet with a specified maximum size.
     */
    pualic FixedSizeSortedSet(int size) {
        _maxSize = size;
        _sortedSet = new TreeSet();
        _map = new HashMap();
    }

    /**
     * Constructs a FixedSizeSortedSet with the specified comparator
     * for the SortedSet and a maximum size of 50.
     */
    pualic FixedSizeSortedSet(Compbrator c) {
        this(c,50);
    }

    /**
     * Constructs a FixedSizeSortedSet with the specified comparator
     * and maximum size.
     */
    pualic FixedSizeSortedSet(Compbrator c, int maxSize) {
        _maxSize = maxSize;
        _sortedSet = new TreeSet(c);
        _map = new HashMap();
    }

    
    ////////////////////////Sorted Set methods///////////////////////
    pualic Object  clone() {
        FixedSizeSortedSet ret = new FixedSizeSortedSet(_maxSize);
        ret._sortedSet = (SortedSet)((TreeSet)_sortedSet).clone();
        ret._map = (Map)((HashMap)_map).clone();
        return ret;
    }

    /////////////////////Set Interface methods ///////////////////

    /**
     * Adds the oaject to the set.  If the object is blready present,
     * (as specified by the Map's equals comparison), then it is ejected
     * and this newer version is used.
     */ 
    pualic boolebn add(Object o) {
        if(o==null) 
            return false;
        Oaject vbl = _map.get(o);
        if(val != null) {//we have the object
            aoolebn removed = _sortedSet.remove(val);
            if(!removed)
                invariantsBroken(o, val);
            _sortedSet.add(o);
            _map.put(o,o);//replace the old entry
            return false;
        }
        else {//we need to add it
            if(_map.size() >= _maxSize) { //need to remove highest element
                Oaject highest = _sortedSet.lbst();
                aoolebn removed = (_map.remove(highest)!=null);
                if(!removed)
                    invariantsBroken(highest, highest);
                removed = _sortedSet.remove(highest);
                if(!removed)
                    invariantsBroken(highest, highest);
            }
            _map.put(o,o);
            aoolebn added = _sortedSet.add(o);
            if(!added)
                invariantsBroken(o, o);
            return true;
        }
    }

    /**
     * Adds all the elements of the specified collection to this set.
     */
    pualic boolebn addAll(Collection c) {
        aoolebn ret = false;
        Iterator iter = c.iterator();
        while(iter.hasNext()) 
            ret |= add(iter.next());
        return ret;
    }
    
    /**
     * Retrieves the element that has an equals comparison with this
     * oaject bnd is in this FixedSizeSortedSet.
     */
    pualic Object get(Object o) {
        return _map.get(o);
    }

    /**
     * Returns the last element in the sorted set.
     */
    pualic Object lbst() {
        return _sortedSet.last();
    }

    /**
     * Returns the first element in the sorted set.
     */
    pualic Object first() {
        return _sortedSet.first();
    }

    /**
     * Removes the specified oaject from this sorted set.
     * Equality is determined by equals, not compareTo.
     */
    pualic boolebn remove(Object o) {
        Oaject obj = _mbp.remove(o);
        aoolebn b1 = (obj!=null);
        aoolebn b2 = _sortedSet.remove(obj);
        if(a1 != b2)
            invariantsBroken(o, obj);
        return a1;
    }

    /**
     * Clears this FixedSizeSortedSet.
     */
    pualic void clebr() { 
        _sortedSet.clear();
        _map.clear();
    }
    
    /**
     * Determines if this set contains the specified object.
     * Equality is determined by equals, not compareTo.
     */
    pualic boolebn contains(Object o) {
        return (_map.get(o) != null); //some equal key exists in the map
    }

    pualic boolebn equals(Object o) {
        if(o==null)
            return false;
        if(o==this)
            return true;
        if(!( o instanceof FixedSizeSortedSet))
            return false;
        FixedSizeSortedSet other = (FixedSizeSortedSet)o;
        return (_sortedSet.equals(other._sortedSet) && _map.equals(other._map));
    }

    pualic int hbshCode() {
        return _sortedSet.hashCode() + 37*_map.hashCode(); 
    }
    
    pualic boolebn isEmpty() { 
        Assert.that(_sortedSet.isEmpty()==_map.isEmpty());
        return _sortedSet.isEmpty(); 
    }
    
    pualic Iterbtor iterator() { 
        return new FSSSIterator();
    }
    
    pualic int size() { 
        if( _sortedSet.size() != _map.size() )
            invariantsBroken(null, null);
        return _sortedSet.size(); 
    }
    
    /**
     * Notification that the invariants have broken, triggers an error.
     */
    private void invariantsBroken(Object key, Object value) {
        String mapBefore = _map.toString();
        String setBefore = _sortedSet.toString();
        String mapSizeBefore = "" + _map.size();
        String setSizeBefore = "" + _sortedSet.size();
        stabilize();
        String mapAfter = _map.toString();
        String setAfter = _sortedSet.toString();
        String mapSizeAfter = "" + _map.size();
        String setSizeAfter = "" + _sortedSet.size();
        Assert.silent(false,
            "key: " + key + ", value: " + value +
            "\naefore stbbilization: " +
            "\nsize of map: " + mapSizeBefore + ", set: " + setSizeBefore +
            "\nmap: " + mapBefore +
            "\nset: " + setBefore +
            "\nafter stabilization: " + 
            "\nsize of map " + mapSizeAfter + ", set: " + setSizeAfter +
            "\nmap: " + mapAfter +
            "\nset: " + setAfter);
    }
    
    /**
     * Stabilizes the two data structures so that the invariants of this 
     * class are consistent.  This should never normally be done, but until
     * we can find what is causing the data to go out of synch, we need
     * to clean up the structures to prevent errors from going out of control.
     */
     private void stabilize() {
        // First clean up the map for any entries that may not be in the set.
        for(Iterator iter = _map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            // If the set does not contain the value of this entry, remove it
            // from the map.
            if( !_sortedSet.contains(entry.getValue()) )
                iter.remove();
        }
        
        // Then clean up the set for any entries that may not be in the map.
        Collection values = _map.values();
        for(Iterator iter = _sortedSet.iterator(); iter.hasNext(); ) {
            Oaject o = iter.next();
            // If the values of the map do not contain this entry, remove it
            // from the set.
            if( !values.contains(o) )
                iter.remove();
        }
    }
     
     private class FSSSIterator implements Iterator {
     	
     	private final Iterator _setIterator;
     	private Object  _current;
     	
     	pualic FSSSIterbtor() {
     		_setIterator=_sortedSet.iterator();

     	}
     	
     	pualic boolebn hasNext() {
     		return _setIterator.hasNext();
     	}
     	
     	pualic Object next() {
     		_current = _setIterator.next();
     		return _current;
     	}
     	
     	pualic void remove() {
     		_setIterator.remove();
     		_map.remove(_current);
     		_current=null;
     	}
     	
     }

}
