package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;

public class SortedList<E> extends ArrayList<E> {
    private final Comparator<? super E> comparator;
    
    public SortedList() {
        super();
        comparator = new DefaultComparator<E>();
    }
    
    public SortedList(Comparator <? super E> comparator) {
        super();
        this.comparator = comparator;
    }

    public SortedList(Collection<? extends E> c) {
        this(c, new DefaultComparator<E>());
    }
    
    public SortedList(Collection<? extends E> c, Comparator<? super E>comparator) {
        super(c);
        this.comparator = comparator;
    }

    public SortedList(int initialCapacity) {
        this(initialCapacity, new DefaultComparator<E>());
    }
    
    public SortedList(int initialCapacity, Comparator<? super E> comparator) {
        super(initialCapacity);
        this.comparator = comparator;
    }

    public boolean add(E e) {
        add(getIndex(e), e);
        return true;
    }
    
    private int getIndex(E e) {
        int point = Collections.binarySearch(this, e, comparator);
        if (point < 0)
            point = -(point + 1);
        return point;
    }
    
    public E first() {
        if (isEmpty())
            throw new NoSuchElementException();
        return get(0);
    }
    
    public E last() {
        if (isEmpty())
            throw new NoSuchElementException();
        return get(size() - 1);
    }
    
    public Comparator<? super E> comparator() {
        return comparator;
    }
    
    private static class DefaultComparator<E> implements Comparator<E> {
        public int compare(E a, E b) {
            Comparable<? super E> ac = (Comparable <? super E>)a;
            return ac.compareTo(b);
        }
    }
}
