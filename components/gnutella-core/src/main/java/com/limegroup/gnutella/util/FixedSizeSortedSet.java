package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;

public class FixedSizeSortedSet {

    /**
     * The underlying set that this wraps
     */
    private TreeSet _sortedSet;
    
    private HashMap /*Object -> Object*/ _map;
    
    /**
     *  The maximum size of this, defaults to 50
     */
    private int _maxSize;

    
    ///////////////////////////////constructors////////////////////
    /**
     * constructor for default size
     */ 
    public FixedSizeSortedSet() {
        this(50);
    }
    
    public FixedSizeSortedSet(int size) {
        _maxSize = size;
        _sortedSet = new TreeSet();
        _map = new HashMap();
    }
    
    ////////////////////////Sorted Set methods///////////////////////
    public Object  clone() {
        FixedSizeSortedSet ret = new FixedSizeSortedSet(_maxSize);
        ret._sortedSet = (TreeSet)_sortedSet.clone();
        ret._map = (HashMap)_map.clone();
        return ret;
    }

    /////////////////////Set Interface methods ///////////////////

    /**
     * Adding an item that is already in map will replace it with the new Object
     */ 
    public boolean add(Object o) {
        if(o==null) 
            System.out.println("Sumeet:adding null element");
        Object val = _map.get(o);
        if(val != null) {//we have the object
            boolean removed = _sortedSet.remove(val);
            Assert.that(removed);
            _sortedSet.add(o);
            _map.put(o,o);//replace the old entry
            return false;
        }
        else {//we need to add it
            if(_map.size() >= _maxSize) { //need to remove highest element
                Object highest = _sortedSet.last();
                boolean removed = (_map.remove(highest)!=null);
                Assert.that(removed);
                removed = _sortedSet.remove(highest);
                Assert.that(removed);
            }
            _map.put(o,o);
            boolean added = _sortedSet.add(o);
            Assert.that(added);
            return true;
        }
    }

    public boolean addAll(Collection c) {
        boolean ret = false;
        Iterator iter = c.iterator();
        while(iter.hasNext()) 
            ret |= add(iter.next());
        return ret;
    }
    
    public Object get(Object o) {
        return _map.get(o);
    }

    public Object last() {
        return _sortedSet.last();
    }

    public Object first() {
        return _sortedSet.first();
    }

    public boolean remove(Object o) {
        Object obj = _map.remove(o);
        boolean b1 = (obj!=null);
        boolean b2 = _sortedSet.remove(obj);
        Assert.that(b1==b2);
        return b1;
    }

    public void clear() { 
        _sortedSet.clear();
        _map.clear();
    }
    
    public boolean contains(Object o) {
        return (_map.get(o) != null); //some equal key exists in the map
    }

    public boolean equals(Object o) {
        if(o==null)
            return false;
        if(o==this)
            return true;
        if(!( o instanceof FixedSizeSortedSet))
            return false;
        FixedSizeSortedSet other = (FixedSizeSortedSet)o;
        return (_sortedSet.equals(other._sortedSet) && _map.equals(other._map));
    }

    public int hashCode() {
        return _sortedSet.hashCode() + 37*_map.hashCode(); 
    }
    
    public boolean isEmpty() { 
        Assert.that(_sortedSet.isEmpty()==_map.isEmpty());
        return _sortedSet.isEmpty(); 
    }
    
    public Iterator iterator() { 
        return _sortedSet.iterator(); 
    }
    
    public int size() { 
        Assert.that(_sortedSet.size() == _map.size());
        return _sortedSet.size(); 
    }

}
