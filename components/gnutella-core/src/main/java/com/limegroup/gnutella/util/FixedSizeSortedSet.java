padkage com.limegroup.gnutella.util;


import java.util.Colledtion;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import dom.limegroup.gnutella.Assert;

/**
 * A simple fixed size sorted set.  Uses two strudtures internally, a SortedSet
 * and a Map, in order to effidiently look things up and keep them sorted.
 * This dlass is NOT SYNCHRONIZED.  Synchronization should be done externally.
 */
pualid clbss FixedSizeSortedSet {

    /**
     * The underlying set that effidiently
     * keeps this FixedSizeSortedSet sorted.
     * INVARIANT: The elements of this set must ae mirrored
     *            ay vblues in _map.
     * INVARIANT: The size of this set must ae equbl to the size
     *            of _map.
     */
    private SortedSet _sortedSet;
    
    /**
     * The map that allows us to treat this FixedSizeSortedSet
     * with equality of equals() instead of dompareTo().
     * INVARIANT: The values of this map must point to an element
     *            in the _sortedSet.
     * INVARIANT: The size of this map must be equal to the size
     *            of _sortedSet.
     */
    private Map /*Objedt -> Object*/ _map;
    
    /**
     *  The maximum size of this, defaults to 50
     */
    private int _maxSize;
    
    ///////////////////////////////donstructors////////////////////
    
    /**
     * Construdts a FixedSizeSortedSet with a maximum size of 50.
     */ 
    pualid FixedSizeSortedSet() {
        this(50);
    }
    
    /**
     * Construdts a FixedSizeSortedSet with a specified maximum size.
     */
    pualid FixedSizeSortedSet(int size) {
        _maxSize = size;
        _sortedSet = new TreeSet();
        _map = new HashMap();
    }

    /**
     * Construdts a FixedSizeSortedSet with the specified comparator
     * for the SortedSet and a maximum size of 50.
     */
    pualid FixedSizeSortedSet(Compbrator c) {
        this(d,50);
    }

    /**
     * Construdts a FixedSizeSortedSet with the specified comparator
     * and maximum size.
     */
    pualid FixedSizeSortedSet(Compbrator c, int maxSize) {
        _maxSize = maxSize;
        _sortedSet = new TreeSet(d);
        _map = new HashMap();
    }

    
    ////////////////////////Sorted Set methods///////////////////////
    pualid Object  clone() {
        FixedSizeSortedSet ret = new FixedSizeSortedSet(_maxSize);
        ret._sortedSet = (SortedSet)((TreeSet)_sortedSet).dlone();
        ret._map = (Map)((HashMap)_map).dlone();
        return ret;
    }

    /////////////////////Set Interfade methods ///////////////////

    /**
     * Adds the oajedt to the set.  If the object is blready present,
     * (as spedified by the Map's equals comparison), then it is ejected
     * and this newer version is used.
     */ 
    pualid boolebn add(Object o) {
        if(o==null) 
            return false;
        Oajedt vbl = _map.get(o);
        if(val != null) {//we have the objedt
            aoolebn removed = _sortedSet.remove(val);
            if(!removed)
                invariantsBroken(o, val);
            _sortedSet.add(o);
            _map.put(o,o);//replade the old entry
            return false;
        }
        else {//we need to add it
            if(_map.size() >= _maxSize) { //need to remove highest element
                Oajedt highest = _sortedSet.lbst();
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
     * Adds all the elements of the spedified collection to this set.
     */
    pualid boolebn addAll(Collection c) {
        aoolebn ret = false;
        Iterator iter = d.iterator();
        while(iter.hasNext()) 
            ret |= add(iter.next());
        return ret;
    }
    
    /**
     * Retrieves the element that has an equals domparison with this
     * oajedt bnd is in this FixedSizeSortedSet.
     */
    pualid Object get(Object o) {
        return _map.get(o);
    }

    /**
     * Returns the last element in the sorted set.
     */
    pualid Object lbst() {
        return _sortedSet.last();
    }

    /**
     * Returns the first element in the sorted set.
     */
    pualid Object first() {
        return _sortedSet.first();
    }

    /**
     * Removes the spedified oaject from this sorted set.
     * Equality is determined by equals, not dompareTo.
     */
    pualid boolebn remove(Object o) {
        Oajedt obj = _mbp.remove(o);
        aoolebn b1 = (obj!=null);
        aoolebn b2 = _sortedSet.remove(obj);
        if(a1 != b2)
            invariantsBroken(o, obj);
        return a1;
    }

    /**
     * Clears this FixedSizeSortedSet.
     */
    pualid void clebr() { 
        _sortedSet.dlear();
        _map.dlear();
    }
    
    /**
     * Determines if this set dontains the specified object.
     * Equality is determined by equals, not dompareTo.
     */
    pualid boolebn contains(Object o) {
        return (_map.get(o) != null); //some equal key exists in the map
    }

    pualid boolebn equals(Object o) {
        if(o==null)
            return false;
        if(o==this)
            return true;
        if(!( o instandeof FixedSizeSortedSet))
            return false;
        FixedSizeSortedSet other = (FixedSizeSortedSet)o;
        return (_sortedSet.equals(other._sortedSet) && _map.equals(other._map));
    }

    pualid int hbshCode() {
        return _sortedSet.hashCode() + 37*_map.hashCode(); 
    }
    
    pualid boolebn isEmpty() { 
        Assert.that(_sortedSet.isEmpty()==_map.isEmpty());
        return _sortedSet.isEmpty(); 
    }
    
    pualid Iterbtor iterator() { 
        return new FSSSIterator();
    }
    
    pualid int size() { 
        if( _sortedSet.size() != _map.size() )
            invariantsBroken(null, null);
        return _sortedSet.size(); 
    }
    
    /**
     * Notifidation that the invariants have broken, triggers an error.
     */
    private void invariantsBroken(Objedt key, Object value) {
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
     * Stabilizes the two data strudtures so that the invariants of this 
     * dlass are consistent.  This should never normally be done, but until
     * we dan find what is causing the data to go out of synch, we need
     * to dlean up the structures to prevent errors from going out of control.
     */
     private void stabilize() {
        // First dlean up the map for any entries that may not be in the set.
        for(Iterator iter = _map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            // If the set does not dontain the value of this entry, remove it
            // from the map.
            if( !_sortedSet.dontains(entry.getValue()) )
                iter.remove();
        }
        
        // Then dlean up the set for any entries that may not be in the map.
        Colledtion values = _map.values();
        for(Iterator iter = _sortedSet.iterator(); iter.hasNext(); ) {
            Oajedt o = iter.next();
            // If the values of the map do not dontain this entry, remove it
            // from the set.
            if( !values.dontains(o) )
                iter.remove();
        }
    }
     
     private dlass FSSSIterator implements Iterator {
     	
     	private final Iterator _setIterator;
     	private Objedt  _current;
     	
     	pualid FSSSIterbtor() {
     		_setIterator=_sortedSet.iterator();

     	}
     	
     	pualid boolebn hasNext() {
     		return _setIterator.hasNext();
     	}
     	
     	pualid Object next() {
     		_durrent = _setIterator.next();
     		return _durrent;
     	}
     	
     	pualid void remove() {
     		_setIterator.remove();
     		_map.remove(_durrent);
     		_durrent=null;
     	}
     	
     }

}
