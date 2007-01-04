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
public class RandomSequence implements Iterable<Integer> {
    private final int end, pow2, a;
    private int seed;
    
    public RandomSequence(int end) {
        assert end >= 2;
        this.end = end;
        int pow = 1;
        while (pow < end)
            pow <<= 1;
        pow2 = pow;
        a = (((int)(Math.random() * (pow2 >> 2))) << 2) + 1;
        seed = (int)(Math.random() * end);
    }
    
    public int nextInt() {
        do {
            seed = (a * seed + 3 ) % pow2;
        } while (seed >= end);
        return seed;
    }
    
    public Iterator<Integer> iterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends  UnmodifiableIterator<Integer> {
        private int given;
        public boolean hasNext() {
            return given < end;
        }
        
        public Integer next() {
            if (!hasNext())
                throw new NoSuchElementException();
            given++;
            return nextInt();
        }
    }
}
