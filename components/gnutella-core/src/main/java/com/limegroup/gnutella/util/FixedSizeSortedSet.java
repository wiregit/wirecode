package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;

public class FixedSizeSortedSet implements Set {

    /**
     * The underlying set that this wraps
     */
    private SortedSet _delegate;
    
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
        _delegate = new TreeSet();
    }
    
    ////////////////////////Sorted Set methods///////////////////////

    /**
     * @return the lowest element in the set
     */
    public Object first() {
        return _delegate.first();
    }

    /**
     * @return the highest element in the set
     */ 
    public Object last() {
        return _delegate.last();
    }

    public SortedSet tailSet(Object o) {
        return _delegate.tailSet(o);
    }

    /////////////////////Set Interface methods ///////////////////
    public boolean add(Object o) {       
        if(_delegate.size()==_maxSize) { //Reached the max limit
            Object highest = _delegate.last();
            _delegate.remove(highest);
        }
        return _delegate.add(o);
    }
    
    public boolean addAll(Collection c) {
        boolean ret = false;
        Iterator iter = c.iterator();
        while(iter.hasNext()) 
            ret |= add(iter.next());
        return ret;
    }
    
    public void clear() { _delegate.clear();}
    
    public boolean contains(Object o) {return _delegate.contains(o); }

    public boolean containsAll(Collection c) {return _delegate.containsAll(c);}

    public boolean equals(Object o) {return _delegate.equals(o); }

    public int hashCode() {return _delegate.hashCode(); }
    
    public boolean isEmpty() { return _delegate.isEmpty(); }

    public Iterator iterator() { return _delegate.iterator(); }

    public boolean remove(Object o) { return _delegate.remove(o); }

    public boolean removeAll(Collection c) { return _delegate.removeAll(c); }

    public boolean retainAll(Collection c) { return _delegate.retainAll(c); }

    public int size() { return _delegate.size(); }

    public Object[] toArray() { return _delegate.toArray(); }

    public Object[] toArray(Object[] a) { return _delegate.toArray(a); }

}
