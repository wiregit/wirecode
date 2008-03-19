package org.limewire.collection;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a set of distinct integers. 
 * Like {@link Set}, <code>SparseIntSet</code> is <b>not synchronized</b>.
 * 
 * The integers in this set are sorted in ascending order.  It would be nice for it
 * to implement the SortedSet interface eventually.
 * 
 * Optimized to have compact representation when the set is "sparse". (For "dense" sets 
 * you should use IntSet.)  Integers are stored as primitives, so you're guaranteed 4*N bytes
 * of memory used after calling the compact() method.
 *  
 * All retrieval and insertion operations run in O(log n) time, where n is the size of the set. 
 * 
 * This class is not thread-safe.
 */
public class SparseIntSet extends AbstractSet<Integer> {

    private int[] list = new int[0];
    
    private int size;
    
    private int modCount;
    
    /**
     * compacts this set to occupy 4*size() bytes of memory.
     */
    public void compact() {
        int oldCapacity = list.length;
        if (size < oldCapacity) {
            int [] copy = new int[size];
            System.arraycopy(list,0,copy,0,size);
            list = copy;
        }
    }
    
    /**
     * @return the actual memory used, in bytes.
     */
    public int getActualMemoryUsed() {
        return list.length * 4;
    }
    
    @Override
    public boolean add(Integer i) {
        int point = binarySearch(i);
        if (point >= 0)
            return false;
        point = -(point + 1);
        ensureCapacity(size + 1);
        size++;
        System.arraycopy(list,point,list,point+1,size-point-1);
        list[point] = i;
        modCount++;
        return true;
    }
    
    private void ensureCapacity(int minCapacity) {

        int oldCapacity = list.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3)/2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            // minCapacity is usually close to size, so this is a win:
            int [] copy = new int[newCapacity];
            System.arraycopy(list,0,copy,0,size);
            list = copy;
        }
    }
    
    private int binarySearch(int key) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int midVal = list[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
    
    @Override
    public boolean remove(Object o) {
        if (! (o instanceof Integer))
            return false;
        int i = (Integer)o;
        int point = binarySearch(i);
        if (point < 0)
            return false;
        int numMoved = size - point - 1;
        if (numMoved > 0)
            System.arraycopy(list, point+1, list, point,
                     numMoved);
        size--;
        modCount++;
        return true;
    }
    
    @Override
    public boolean contains(Object o) {
        if (! (o instanceof Integer))
            return false;
        int i = ((Integer)o).intValue();
        int point = binarySearch(i);
        return point >= 0;
    }
    
    public Iterator<Integer> iterator() {
        return new ArrayIterator();
    }

    @Override
    public int size() {
        return size;
    }
    
    @Override
    public boolean retainAll(Collection<?> o) {
        
        SparseIntSet toRemove = new SparseIntSet();
        for (int contained : this) {
            if (! o.contains(contained))
                toRemove.add(contained);
        }
        return removeAll(toRemove);
    }
    
    private class ArrayIterator extends UnmodifiableIterator<Integer> {
        
        private int index;
        private final int mod = modCount;
        
        private void checkModification() {
            if (modCount != mod)
                throw new ConcurrentModificationException();
        }
        
        public boolean hasNext() {
            return index < size;
        }

        public Integer next() {
            checkModification();
            if (!hasNext())
                throw new NoSuchElementException();
            return list[index++];
        }
    }
}
