package com.limegroup.gnutella.util;

import java.util.*;

/**
 * a HashSet which allows us to use custom hashCode and equals methods for
 * the objects contained inside.
 */
public class HasherSet extends HashSet {

    private static final Hasher DEFAULT = new DefaultHasher();

    /** the <tt> Hasher </tt> instance this set will use for its elements */
    private Hasher _hasher;
    
    /** creates a new instance with the provided Hasher */
    public HasherSet(Hasher h){
        _hasher =h;
    }
    
    /** creates a new instance with the default hasher, which does nothing */
    public HasherSet() {
        _hasher=DEFAULT;
    }
    
    public HasherSet(Hasher h, Collection c) {
        _hasher = h;
        addAll(c);
    }
    
    public HasherSet(Collection c) {
        this(DEFAULT,c);
    }
    
    //TODO: override the other constructors - with size, existing collection, etc.
    
    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object arg0) {
        return super.add(wrap(arg0));
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection arg0) {
        return super.addAll(wrap(arg0));
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return super.contains(wrap(o));
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection arg0) {
        return super.containsAll(wrap(arg0));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator iterator() {
        return new UnwrapIterator();
    }
    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return super.remove(wrap(o));
    }
    /* (non-Javadoc)
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection arg0) {
        return super.retainAll(wrap(arg0));
    }
    
    /**
     * @param c a collection that will interact with this
     * @return a collection of Wrapper objects, wrapping each element in the
     * original collection
     */
    private Collection wrap(Collection c) {
        if (c instanceof HasherSet)
            return c;
        
        HashSet tmp = new HashSet();
        
        for (Iterator i = c.iterator();i.hasNext();){
            Object next = i.next();
            tmp.add(wrap(next));
        }
        
        return tmp;
    }
    
    private Wrapper wrap(Object o) {
        if (o instanceof Wrapper)
            return ((Wrapper)o);
        else
            return new Wrapper(o);
    }
    
    private final class Wrapper {
        private final Object _obj;
        
        public Wrapper(Object o) {
            _obj = o;
        }
        
        public int hashCode() {
            return _hasher.hash(_obj);
        }
        
        public boolean equals(Object other) {
            if (other instanceof Wrapper)
                return _hasher.areEqual(_obj,((Wrapper)other).getObj());
            
            return false;
        }
        
        public Object getObj() {
            return _obj;
        }
    }
    
    private final class UnwrapIterator implements Iterator {
        
        private final Iterator _iter;
        
        public UnwrapIterator() {
            _iter = HasherSet.super.iterator();
        }
        
        public boolean hasNext() {
            return _iter.hasNext();
        }
        
        public Object next() {
            Wrapper wr = (Wrapper)_iter.next();
            return wr.getObj();
        }
        
        public void remove() {
            _iter.remove();
        }
    }
    
    /**
     * a default hasher which delegates to the object's methods.
     */
    private static final class DefaultHasher implements Hasher {
        public int hash(Object o) {
            return o.hashCode();
        }
        
        public boolean areEqual(Object a, Object b) {
            return a.equals(b);
        }
    }
}
