package org.limewire.collection;

import java.util.Collection;
import java.util.Comparator;

public class FixedSizeSortedList<E> extends SortedList<E> {
    private final int capacity;

    public FixedSizeSortedList(int capacity) {
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Collection<? extends E> c, Comparator<? super E> comparator, int capacity) {
        super(c, comparator);
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Collection<? extends E> c, int capacity) {
        super(c);
        this.capacity = capacity;
    }

    public FixedSizeSortedList(Comparator<? super E> comparator, int capacity) {
        super(comparator);
        this.capacity = capacity;
    }
    
    public boolean add(E e) {
        if (size() == capacity)
            remove(last());
        return super.add(e);
    }
    
    public E insert(E e) {
        E ret = null;
        if (size() == capacity) {
            ret = last();
            if (comparator().compare(e, ret) < 0)
                return e;
            remove(ret);
        }
        add(e);
        return ret;
    }
}

