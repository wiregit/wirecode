/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FixedSizeHashMap extends LinkedHashMap {
    
    protected final int maxSize;

    public FixedSizeHashMap(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(int initialCapacity, float loadFactor, 
            boolean accessOrder, int maxSize) {
        super(initialCapacity, loadFactor, accessOrder);
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(int initialCapacity, float loadFactor, int maxSize) {
        super(initialCapacity, loadFactor);
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(int initialCapacity, int maxSize) {
        super(initialCapacity);
        this.maxSize = maxSize;
    }

    public FixedSizeHashMap(Map m, int maxSize) {
        super(m);
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }
    
    public boolean isFull() {
        return size() >= maxSize-1;
    }
    
    protected boolean removeEldestEntry(Entry eldest) {
        return size() >= maxSize;
    }
}
