package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A class that generates all the integers from 0 to a certain limit in a 
 * random fashion where each number is generated only once per cycle.
 * 
 * It also implements the Iterable interface that iterates over a single cycle.
 * 
 * Logic: http://en.wikipedia.org/wiki/Linear_congruential_generator
 * Thx to Cyclonus for the pow2 hint
 */
public class RandomSequence {
    private final int end;
    private final long pow2, a;
    private long seed;
    
    public RandomSequence(int end) {
        this.end = end;
        
        // empty or single-element sequence
        if (end <= 1) {
            pow2 = 0; 
            a = 0;
            return;
        }
        
        long pow = 1;
        while (pow < end)
            pow <<= 1;
        pow2 = pow - 1;
        a = (((int)(Math.random() * (pow2 >> 2))) << 2) + 1;
        seed = (int)(Math.random() * end);
    }
    
    public int nextInt() {
        if (end < 1)
            throw new NoSuchElementException();
        
        if (end == 1)
            return 0;
        
        do {
            seed = (a * seed + 3 ) & pow2;
        } while (seed >= end || seed < 0);
        return (int)seed;
    }
    
    public Iterator iterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends  UnmodifiableIterator {
        private int given;
        public boolean hasNext() {
            return given < end;
        }
        
        public Object next() {
            if (!hasNext())
                throw new NoSuchElementException();
            given++;
            return new Integer(nextInt());
        }
    }
}