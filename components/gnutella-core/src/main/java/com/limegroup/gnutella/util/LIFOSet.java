package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * 
 * A Set ordered by Last-In-First-Out. 
 * 
 */
public class LIFOSet<E> implements Set<E>{
    
    private Set<E> set;
    
    private List<E> list;
    
    public LIFOSet() {
        this(50, 0.75F);
    }
    
    public LIFOSet(int initialCapacity){
        this(initialCapacity, 0.75F);
    }
    
    public LIFOSet(int initialCapacity, float loadFactor){
        set = new HashSet<E>(initialCapacity, loadFactor);
        list = new ArrayList<E>(initialCapacity);
    }
    
    /**
     * Adds the given element to the head of the set.
     * 
     * @return true
     * 
     */
    public boolean add(E o) {
        if(set.add(o)) {
            list.add(o);
            return true;
        }
        list.remove(o);
        list.add(o);
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean ret = false;
        for(E e : c) {
            ret |= add(e);
        }
        return ret;
    }

    public void clear() {
        set.clear();
        list.clear();
    }

    public boolean contains(Object o) {
        return set.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public Iterator<E> iterator() {
        return new LIFOSetIterator();
    }

    public boolean remove(Object o) {
        return set.remove(o);
    }
    
    /**
     * Removes the last element from the ordered set
     * 
     * @return true if the set was changed
     */
    public boolean removeLast() {
        if(list.isEmpty()) {
            return false;
        }
        return set.remove(list.remove(list.size()-1));
    }

    public boolean removeAll(Collection<?> c) {
        list.removeAll(c);
        return set.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        list.retainAll(c);
        return set.retainAll(c);
    }

    public int size() {
        return set.size();
    }

    public Object[] toArray() {
        return set.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return set.toArray(a);
    }
    
    @Override
    public String toString() {
        List<E> reverse = new ArrayList<E>(list);
        Collections.reverse(reverse);
        return reverse.toString();
    }



    private class LIFOSetIterator implements Iterator<E> {
        
        private E  current;
        private int index = 0;
        
        public LIFOSetIterator() {
            index = list.size() - 1;
        }
        
        public boolean hasNext() {
            return index >= 0;
        }
        
        public E next() {
            if(index < 0) {
                throw new NoSuchElementException();
            }
            current = list.get(index);
            --index;
            return current;
        }
        
        public void remove() {
            set.remove(current);
            list.remove(current);
            current=null;
        }
     }
}
