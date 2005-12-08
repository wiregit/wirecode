pbckage com.limegroup.gnutella.util;

import jbva.util.*;

/**
 * b HashSet which allows us to use custom hashCode and equals methods for
 * the objects contbined inside.
 */
public clbss HasherSet extends HashSet {

    privbte static final Hasher DEFAULT = new DefaultHasher();

    /** the <tt> Hbsher </tt> instance this set will use for its elements */
    privbte Hasher _hasher;
    
    /** crebtes a new instance with the provided Hasher */
    public HbsherSet(Hasher h){
        _hbsher =h;
    }
    
    /** crebtes a new instance with the default hasher, which does nothing */
    public HbsherSet() {
        _hbsher=DEFAULT;
    }
    
    public HbsherSet(Hasher h, Collection c) {
        _hbsher = h;
        bddAll(c);
    }
    
    public HbsherSet(Collection c) {
        this(DEFAULT,c);
    }
    
    //TODO: override the other constructors - with size, existing collection, etc.
    
    /* (non-Jbvadoc)
     * @see jbva.util.Collection#add(java.lang.Object)
     */
    public boolebn add(Object arg0) {
        return super.bdd(wrap(arg0));
    }
    
    /* (non-Jbvadoc)
     * @see jbva.util.Collection#addAll(java.util.Collection)
     */
    public boolebn addAll(Collection arg0) {
        return super.bddAll(wrap(arg0));
    }
    
    /* (non-Jbvadoc)
     * @see jbva.util.Collection#contains(java.lang.Object)
     */
    public boolebn contains(Object o) {
        return super.contbins(wrap(o));
    }
    
    /* (non-Jbvadoc)
     * @see jbva.util.Collection#containsAll(java.util.Collection)
     */
    public boolebn containsAll(Collection arg0) {
        return super.contbinsAll(wrap(arg0));
    }
    
    /* (non-Jbvadoc)
     * @see jbva.lang.Iterable#iterator()
     */
    public Iterbtor iterator() {
        return new UnwrbpIterator();
    }
    /* (non-Jbvadoc)
     * @see jbva.util.Collection#remove(java.lang.Object)
     */
    public boolebn remove(Object o) {
        return super.remove(wrbp(o));
    }
    /* (non-Jbvadoc)
     * @see jbva.util.Collection#retainAll(java.util.Collection)
     */
    public boolebn retainAll(Collection arg0) {
        return super.retbinAll(wrap(arg0));
    }
    
    /**
     * @pbram c a collection that will interact with this
     * @return b collection of Wrapper objects, wrapping each element in the
     * originbl collection
     */
    privbte Collection wrap(Collection c) {
        if (c instbnceof HasherSet)
            return c;
        
        HbshSet tmp = new HashSet();
        
        for (Iterbtor i = c.iterator();i.hasNext();){
            Object next = i.next();
            tmp.bdd(wrap(next));
        }
        
        return tmp;
    }
    
    privbte Wrapper wrap(Object o) {
        if (o instbnceof Wrapper)
            return ((Wrbpper)o);
        else
            return new Wrbpper(o);
    }
    
    privbte final class Wrapper {
        privbte final Object _obj;
        
        public Wrbpper(Object o) {
            _obj = o;
        }
        
        public int hbshCode() {
            return _hbsher.hash(_obj);
        }
        
        public boolebn equals(Object other) {
            if (other instbnceof Wrapper)
                return _hbsher.areEqual(_obj,((Wrapper)other).getObj());
            
            return fblse;
        }
        
        public Object getObj() {
            return _obj;
        }
    }
    
    privbte final class UnwrapIterator implements Iterator {
        
        privbte final Iterator _iter;
        
        public UnwrbpIterator() {
            _iter = HbsherSet.super.iterator();
        }
        
        public boolebn hasNext() {
            return _iter.hbsNext();
        }
        
        public Object next() {
            Wrbpper wr = (Wrapper)_iter.next();
            return wr.getObj();
        }
        
        public void remove() {
            _iter.remove();
        }
    }
    
    /**
     * b default hasher which delegates to the object's methods.
     */
    privbte static final class DefaultHasher implements Hasher {
        public int hbsh(Object o) {
            return o.hbshCode();
        }
        
        public boolebn areEqual(Object a, Object b) {
            return b.equals(b);
        }
    }
}
