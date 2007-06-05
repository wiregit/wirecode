package org.limewire.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.RandomAccess;

/**
 * Provides a fixed size {@link HashSet} with index access. The replacement
 * policy is FIFO (First in, First out) and the iteration order is from newest 
 * to oldest.
 * <p>
 * Adding an existing element resets the FIFO order; the newly added element is
 * now the "last in" element.  
 * <p>
 * <code>FixedSizeArrayHashSet</code> does not support the null element.
 * <pre> 
    public class MyObjectHash{
        public String s;
        public int item;
        public MyObjectHash(String s, int item){
            this.s = s;
            this.item = item;
        }       

        public String toString(){
            return s + "=" + item;
        }
        
        public boolean equals(Object obj) {
            MyObjectHash other = (MyObjectHash)obj;
            return (this.s.equals(other.s) && this.item == other.item);         
        }
                
        public int hashCode() {
            return this.item * 31 + s.hashCode();
        }
    }   

    void sampleCodeFixedSizeArrayHashSet(){

        FixedSizeArrayHashSet&lt;MyObjectHash&gt; fsah = new FixedSizeArrayHashSet&lt;MyObjectHash&gt;(4);
    
        fsah.add(new MyObjectHash("a", 1));
        fsah.add(new MyObjectHash("b", 2));
        fsah.add(new MyObjectHash("c", 3));
        fsah.add(new MyObjectHash("d", 4));
        fsah.add(new MyObjectHash("e", 5));
        fsah.add(new MyObjectHash("f", 6));

        for (MyObjectHash iO : fsah)
            System.out.println(iO);
        
        if(!fsah.add(new MyObjectHash("c", 3)))
            System.out.println("Tried to add 3=c again, but it failed (though 3=c was put to the first item).");

        System.out.println("Index access: " + fsah.get(0));

        for (MyObjectHash iO : fsah)
            System.out.println(iO);

        System.out.println("Remove 3=c.");
        if(!fsah.remove(new MyObjectHash("c", 3)))
            System.out.println("Remove failed.");

        for (MyObjectHash iO : fsah)
            System.out.println(iO);
    }
    Output:
        f=6
        e=5
        d=4
        c=3
        Tried to add 3=c again, but it failed (though 3=c was put to the first item).
        Index access: c=3
        c=3
        f=6
        e=5
        d=4
        Remove 3=c.
        f=6
        e=5
        d=4
   </pre>    
 */
public class FixedSizeArrayHashSet<T> extends HashSet<T> implements RandomAccess {

    private Buffer<T> buf;
    
    /**
     * creates a FixedSizeArrayHashSet with the specified maximum capacity.
     */
    public FixedSizeArrayHashSet(int maxCapacity) {
        buf = new Buffer<T>(maxCapacity);
    }

    /**
     * Creates a <code>FixedSizeArrayHashSet</code> with maximum capacity the 
     * size of the provided collection and adds all the elements of that 
     * collection.
     */
    public FixedSizeArrayHashSet(Collection<? extends T> c) {
        this(c.size(),c);
    }
    
    /**
     * Creates a <code>FixedSizeArrayHashSet</code> with the provided maximum capacity and
     * adds elements from the provided collection.  If the capacity is less than
     * the size of the collection, elements will get ejected with FIFO policy.
     */
    public FixedSizeArrayHashSet(int maxCapacity, Collection<? extends T> c) {
        buf = new Buffer<T>(maxCapacity);
        addAll(c);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        buf = new Buffer<T>(maxCapacity);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity) {
        super(initialCapacity);
        buf = new Buffer<T>(maxCapacity);
    }

    @Override
    public boolean add(T e) {
        if (e == null)
            throw new IllegalArgumentException("null element not supported");
        
        boolean ret = super.add(e);
        
        if (ret) {
            // eject oldest element if size reached
           T removed = buf.add(e);
           if (removed != null)
               super.remove(removed);
        } else {
            // refresh this element
            buf.remove(e);
            buf.add(e);
        }
        
        return ret;
    }
    
    @Override
    public void clear() {
        buf.clear();
        super.clear();
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Object clone() {
        FixedSizeArrayHashSet<T> newSet = (FixedSizeArrayHashSet<T>)super.clone();
        newSet.buf = buf.clone();
        return newSet;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayHashSetIterator();
    }
    
    public T get(int i) {
        return buf.get(i);
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = super.remove(o);
        if (ret)
            buf.remove(o);
        return ret;
    }
    
    private class ArrayHashSetIterator extends UnmodifiableIterator<T> {
        private final Iterator<T> iter = buf.iterator();
        private T current;
        public boolean hasNext() {
            return iter.hasNext();
        }
        
        public T next() {
            current = iter.next();
            return current;
        }
    }
}
