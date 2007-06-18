/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.statistics;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple ring buffer that keeps track of the most recently added
 * element, the min and max value as well as the number of times 
 * {@link #add(Number)} and {@link #incrementByOne()} was invoked 
 * respectively.
 */
class History<T extends Number & Comparable<T>> implements Iterable<T>, Serializable {
    
    private final int historySize;
    
    private Object[] history;
    
    private int index = 0;
    
    private T min = null;
    
    private T max = null;
    
    private int size = 0;
    
    private long totalCount = 0;
    
    private transient int modCount = 0;
    
    public History(int historySize) {
        if (historySize < 0) {
            throw new IllegalArgumentException("historySize: " + historySize);
        }
        
        this.historySize = historySize;
    }
    
    /**
     * Adds the given sample to the History
     */
    public synchronized void add(T sample) {
        if (sample == null) {
            throw new NullPointerException("Sample is null");
        }
        
        if (history == null) {
            history = new Object[historySize];
        }
        
        history[index] = sample;
        index = (index + 1) % history.length;
        
        if (min == null || sample.compareTo(min) < 0) {
            min = sample;
        }
        
        if (max == null || sample.compareTo(max) > 0) {
            max = sample;
        }
        
        if (size < history.length) {
            size++;
        }
        
        incrementByOne();
    }
    
    /**
     * Increments the inner counter by one.
     * 
     * Note: You cannot mix individual {@link #add(Number)}
     * and {@link #incrementByOne()} calls!
     */
    public synchronized void incrementByOne() {
        totalCount++;
        modCount++;
    }
    
    /**
     * 
     */
    public synchronized T get(int i) {
        if (size() < history.length) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return innerGet(i);
        } else {
            return innerGet((index + i) % history.length);
        }
    }
    
    /**
     * Returns the min element or null if the History is empty
     */
    public synchronized T getMin() {
        return min;
    }
    
    /**
     * Returns the max element of null if the History is empty
     */
    public synchronized T getMax() {
        return max;
    }
    
    /**
     * Returns the most recently added element or null if the
     * History is empty
     */
    public synchronized T getCurrent() {
        if (isEmpty()) {
            return null;
        }
        
        int i = index - 1;
        if (i < 0) {
            i = history.length;
        }
        return innerGet(i);
    }
    
    @SuppressWarnings("unchecked")
    private synchronized T innerGet(int index) {
        return (T)history[index];
    }
    
    /**
     * Returns the total number of times {@link #add(Number)} or 
     * {@link #incrementByOne()} was called
     */
    public synchronized long getTotalCount() {
        return totalCount;
    }
    
    /**
     * Returns true if the History is empty
     */
    public synchronized boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Returns the current size of the History
     */
    public synchronized int size() {
        return size;
    }
    
    /**
     * Clears the history
     */
    public synchronized void clear() {
        index = 0;
        totalCount = 0;
        size = 0;
        min = null;
        max = null;
        modCount++;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public synchronized Iterator<T> iterator() {
        return new HistoryIterator();
    }
    
    private class HistoryIterator implements Iterator<T> {

        private final int expectedModCount = modCount;
        
        private int position = 0;
        
        public boolean hasNext() {
            return (position < size());
        }

        public T next() {
            if (position >= size()) {
                throw new NoSuchElementException();
            }
            
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            return get(position++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
