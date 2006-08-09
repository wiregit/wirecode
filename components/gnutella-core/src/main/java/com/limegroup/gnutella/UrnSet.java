package com.limegroup.gnutella;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Set specifically for URNs.
 * This is not backed by a HashSet because there are
 * so few URN types that it doesn't need to be.
 * 
 * This currently only supports SHA1 URNs, and thus will
 * only allow a single URN to be added.  If further
 * UrnTypes are created, this class will have to be updated.
 * (Note that there'll have to be MANY changes.)
 * 
 * If the set already has a URN of the specified type added to it,
 * further additions of that type will be rejected.
 */
public class UrnSet implements Set<URN>, Iterable<URN>, Cloneable, Serializable {
    
    private static final long serialVersionUID = -1065284624401321676L;

    /** The sole URN this knows about. */
    private URN sha1;
    
    /** Constructs an empty UrnSet. */
    public UrnSet() {}
    
    /** Constructs a UrnSet with the given URN. */
    public UrnSet(URN urn) {
        add(urn);
    }
    
    /** Constructs a UrnSet with URNs from the given collection. */
    public UrnSet(Collection<? extends URN> c) {
        addAll(c);
    }
    
    public String toString() {
        return isEmpty() ? "{Empty UrnSet}" : "UrnSet of: " + sha1;
    }
    
    /** Clones this set. */
    public UrnSet clone() {
        UrnSet c = new UrnSet();
        c.sha1 = sha1;
        return c;
    }
    
    /** Returns the hashcode for this UrnSet. */
    public int hashCode() {
        return sha1 == null ? 0 : sha1.hashCode();
    }
    
    
    /**
     * Determines if this set contains all the same objects
     * as another set.
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;

        Collection c = (Collection) o;
        if (c.size() != size())
            return false;
        
        try {
            return containsAll(c);
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    /**
     * Attempts to add the given URN into this set.
     * If the set already contained a URN of the same type,
     * the new URN is rejected and this returns false.
     * 
     * If the URN's type is not supported, the URN is rejected
     * and this returns false.
     * 
     * @param o
     * @return
     */
    public boolean add(URN o) {
        if(o.isSHA1() && sha1 == null) {
            sha1 = o;
            return true;
        }
        
        return false;
    }

    /**
     * Attempts to add all the URNs in the given collection into this
     * set.  If the set was modified as a result of any of the additions,
     * this will return true.  Otherwise, it will return false.
     * 
     * @param c
     * @return
     */
    public boolean addAll(Collection<? extends URN> c) {
        boolean ret = false;
        for(URN urn : c)
            ret |= add(urn);
        return ret;
    }

    public void clear() {
        sha1 = null;       
    }

    public boolean contains(Object o) {
        return o.equals(sha1);
    }

    public boolean containsAll(Collection<?> c) {
        if(c.size() > 1)
            return false;
        if(c.isEmpty())
            return true;
        if(sha1 == null)
            return false;
        return sha1.equals(c.iterator().next());
    }

    public boolean isEmpty() {
        return sha1 == null;
    }

    public Iterator<URN> iterator() {
        return new UrnIterator();
    }

    public boolean remove(Object o) {
        if(sha1 != null && o.equals(sha1)) {
            sha1 = null;
            return true;
        }
        
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        if(sha1 == null)
            return false;
        
        for(Object o : c) {
            remove(o);
            if(sha1 == null)
                return true;
        }
        
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        if(sha1 != null && !c.contains(sha1)) {
            sha1 = null;
            return true;
        }
        
        return false;
    }

    public int size() {
        return sha1 == null ? 0 : 1;
    }

    public Object[] toArray() {
        return sha1 == null ? new Object[0] : new Object[] { sha1 };
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size)
            a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
        
        if(size == 1)
            a[0] = (T)sha1;
        
        if(a.length > size)
            a[size] = null;
        return a;
    }
    
    /** Iterator that returns each of the Urn Types in turn. */
    private class UrnIterator implements Iterator<URN> {
        private int idx = 0;

        public boolean hasNext() {
            return idx == 0 && sha1 != null;
        }

        public URN next() {
            if(idx != 0 || sha1 == null)
                throw new NoSuchElementException();
            idx++;
            return sha1;
        }

        public void remove() {
            if(idx == 0)
                throw new IllegalStateException();
            
            if(idx == 1)
                sha1 = null;
        }

    }

}
