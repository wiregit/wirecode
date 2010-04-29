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

package org.limewire.mojito2.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * TimeAwareIterable has a maximum time for which it's valid
 * and it takes the average time between next() and hasNext()
 * calls respectively into consideration to select and return 
 * elements. The hasNext() method returns false if all elements
 * have been returned or if there's not enough time remaining
 * (i.e. if the average time between next()/hasNext() calls is
 * higher than the remaining time).
 * <p>
 * The selection of elements looks about this. The gap between
 * selected elements gets bigger and bigger as we're approaching
 * the maximum time for which this Iterable is valid.
 * <pre>
 * a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z
 * ^ ^   ^     ^       ^         ^           ^
 * 0 ------------------- time --------------------> max
 * </pre>
 * Implementation detail: Unlike with regular Iterators where you 
 * may call next() multiple times in a row without checking whether 
 * or not it has more elements you must check on every iteration
 * if there're more elements left.
 */
public class TimeAwareIterable<E> implements Iterable<E> {
    
    private final int sampleSize;
    
    private final long maxTime;

    private final TimeUnit unit;
    
    private final E[] elements;
    
    /**
     * Creates a TimeAwareIterable with a default sample size of 10.
     * 
     * @param elements the elements to process
     * @param maxTime the maximum time this {@link Iterable} is valid
     * @param unit the {@link TimeUnit} of maxTime
     */
    public TimeAwareIterable(E[] elements, 
            long maxTime, TimeUnit unit) {
        this(10, elements, maxTime, unit);
    }
    
    /**
     * Creates a TimeAwareIterable.
     * 
     * @param sampleSize the sample size to compute the average time between calls
     * @param elements the elements to process
     * @param maxTime the maximum time this {@link Iterable} is valid
     * @param unit the {@link TimeUnit} of maxTime
     */
    public TimeAwareIterable(int sampleSize, 
            E[] elements, long maxTime, TimeUnit unit) {
        this.sampleSize = sampleSize;
        this.elements = elements;
        this.maxTime = maxTime;
        this.unit = unit;
    }
    
    /**
     * Returns the maxTime in the given {@link TimeUnit}
     */
    public long getMaxTime(TimeUnit unit) {
        return unit.convert(maxTime, this.unit);
    }
    
    /**
     * Returns the maxTime in milliseconds
     */
    public long getMaxTimeInMillis() {
        return getMaxTime(TimeUnit.MILLISECONDS);
    }
    
    @Override
    public Iterator<E> iterator() {
        return new TimeAwareIterator();
    }

    /**
     * The actual Iterator.
     */
    private class TimeAwareIterator implements Iterator<E> {
        
        private final long[] samples = new long[sampleSize];
        
        private final long maxTime = getMaxTimeInMillis();
        
        private int sampleCount = 0;
        
        private int sampleIndex = 0;
        
        private long startTime = -1L;
        
        private long lastTime = -1L;
        
        private int currentIndex = -1;
        
        private int nextIndex = 0;
        
        @Override
        public boolean hasNext() {
            if (currentIndex == -1 && nextIndex < elements.length) {
                long currentTime = System.currentTimeMillis();
                long average = 0L;
                
                if (startTime == -1L) {
                    startTime = currentTime;
                }
                
                if (lastTime != -1L) {
                    average = addSample(currentTime - lastTime);
                }
                lastTime = currentTime;
                
                // If there's not enough time left then exit
                long timeRemaining = maxTime - (currentTime - startTime);
                
                if (timeRemaining >= average) {
                    currentIndex = nextIndex;
                    nextIndex++;
                    int elementsRemaining = elements.length - nextIndex;
                    assert (elementsRemaining >= 0);
                    nextIndex += (int)(average*elementsRemaining/timeRemaining);
                }
            }
            
            return currentIndex != -1;
        }
        
        @Override
        public E next() {
            if (startTime == -1L || currentIndex == -1) {
                throw new NoSuchElementException();
            }
            
            int index = currentIndex;
            currentIndex = -1;
            return elements[index];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        private long addSample(long timeInMillis) {
            if (timeInMillis >= 0L) {
                samples[sampleIndex] = timeInMillis;
                sampleIndex = (sampleIndex + 1) % samples.length;
                
                if (sampleCount < samples.length) {
                    sampleCount++;
                }
            }
            
            return getAverage();
        }
        
        private long getAverage() {
            if (sampleCount == 0) {
                return 0L;
            }
            
            long sum = 0L;
            for (long time : samples) {
                sum += time;
            }
            return sum/sampleCount;
        }
    }
}
