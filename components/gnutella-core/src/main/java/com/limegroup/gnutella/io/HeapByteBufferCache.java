package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class HeapByteBufferCache {

    private final TreeMap /* Integer -> List<ByteBuffer> */ CACHE = new TreeMap();
    
    private final Map /* ByteBuffer -> ByteBuffer */ SLICED = new IdentityHashMap();

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
        Integer key = new Integer(size);
        List l = (List) CACHE.get(key);

        // if yes, return it.
        if (l != null) {
            if (l.isEmpty())
                return ByteBuffer.allocate(size);
            
            return (ByteBuffer) l.remove(l.size() - 1);
        }
        
        // if not, try to get the next largest buffer
            
        // are there any larger buffers than this one at all?
        // if not, just allocate a new one
        Integer largest = (Integer) CACHE.lastKey();
        if (largest.compareTo(key) < 0) 
            return ByteBuffer.allocate(size);
        
        ByteBuffer larger = null;
        // otherwise find the next largest 
        // note: we could shorten the iteration using a tailMap, but its unclear if
        // creating a new object is justified.
        for (Iterator iter = CACHE.entrySet().iterator();iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (key.compareTo((Integer)entry.getKey()) > 0)
                continue;
            
            l = (List) entry.getValue();
            if (!l.isEmpty()) {
                larger = (ByteBuffer) l.remove(l.size() - 1);
                break;
            }
        }
        
        // still no larger buffer?
        if (larger == null) 
            return ByteBuffer.allocate(size);
        
        // slice!
        larger.limit(size);
        ByteBuffer ret = larger.slice();
        SLICED.put(ret, larger);
        return ret;
    }

    public synchronized void put(ByteBuffer buf) {
        
        // see if this buffer was sliced off of a bigger one
        ByteBuffer toReturn = (ByteBuffer) SLICED.remove(buf);
        if (toReturn == null)
            toReturn = buf;
        
        toReturn.clear();
        Integer size = new Integer(toReturn.capacity());
        List l = (List) CACHE.get(size);
        if (l == null) { 
            l = new ArrayList(1);
            CACHE.put(size, l);
        }
        l.add(toReturn);
    }
    
    public synchronized void clear() {
        CACHE.clear();
        // keep the SCLICED mappings around to allow returning of sliced buffers.
    }

}