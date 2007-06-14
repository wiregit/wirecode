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

public class History<T extends Number & Comparable<T>> implements Iterable<T>, Serializable {
    
    private final int historySize;
    
    private T[] history;
    
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
    
    @SuppressWarnings("unchecked")
    public synchronized void add(T sample) {
        if (sample == null) {
            throw new NullPointerException("Sample is null");
        }
        
        if (history == null) {
            history = (T[])new Object[historySize];
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
    
    public synchronized void incrementByOne() {
        totalCount++;
        modCount++;
    }
    
    public synchronized T get(int i) {
        if (size() < history.length) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return history[i];
        } else {
            return history[(index + i) % history.length];
        }
    }
    
    public synchronized T getMin() {
        return min;
    }
    
    public synchronized T getMax() {
        return max;
    }
    
    public synchronized T getCurrent() {
        if (isEmpty()) {
            return null;
        }
        
        int i = index - 1;
        if (i < 0) {
            i = history.length;
        }
        return history[i];
    }
    
    public synchronized long getTotalCount() {
        return totalCount;
    }
    
    public synchronized boolean isEmpty() {
        return size() == 0;
    }
    
    public synchronized int size() {
        return size;
    }
    
    public synchronized void clear() {
        index = 0;
        totalCount = 0;
        size = 0;
        min = null;
        max = null;
        modCount++;
    }
    
    public synchronized Iterator<T> iterator() {
        return new HistoryIterator();
    }
    
    private class HistoryIterator implements Iterator<T> {

        private int expectedModCount = modCount;
        
        private int pos = 0;
        
        public boolean hasNext() {
            return (pos < totalCount);
        }

        public T next() {
            if (pos >= totalCount) {
                throw new NoSuchElementException();
            }
            
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            
            return get(pos++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
