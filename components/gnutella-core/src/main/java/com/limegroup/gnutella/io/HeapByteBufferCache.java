package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.util.IntHashMap;

public class HeapByteBufferCache {

    // Store up to 1MB of byte[] here.
    private static final int MAX_SIZE = 1024 * 1024;
    
    private final IntHashMap /* int -> List<ByteBuffer> */ CACHE = new IntHashMap();
        
    /** The total size of bytes stored in cache. */
    private long totalCacheSize;

    public ByteBuffer get() {
        return get(8192);
    }
    
    public synchronized ByteBuffer get(int size) {
        // trivial case - cache is empty
        if (CACHE.isEmpty()) { 
            ByteBuffer buf = ByteBuffer.allocate(size);
            return buf;
        }
        
        // if not, see if we have a buffer of the exact size
        List l = (List) CACHE.get(size);
        // if yes, return it.
        if (l != null && !l.isEmpty()) {
            ByteBuffer buf = (ByteBuffer)l.remove(l.size() -1);
            totalCacheSize -= buf.capacity();
            return buf;
        } else {
            return ByteBuffer.allocate(size);
        }
    }

    public synchronized void put(ByteBuffer toReturn) {
        if(totalCacheSize > MAX_SIZE)
            return;
        
        int size = toReturn.capacity();
        toReturn.clear();
        List l = (List) CACHE.get(size);
        if (l == null) { 
            l = new ArrayList(1);
            CACHE.put(size, l);
        }
        l.add(toReturn);
        totalCacheSize += toReturn.capacity();
    }
    
    public synchronized void clear() {
        CACHE.clear();
        totalCacheSize = 0;
    }
    
    public synchronized long getByteSize() {
        return totalCacheSize;
    }
}