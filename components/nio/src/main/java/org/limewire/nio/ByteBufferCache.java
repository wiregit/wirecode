package org.limewire.nio;

import java.nio.ByteBuffer;

public class ByteBufferCache {

    private final DirectByteBufferCache DIRECT = new DirectByteBufferCache();
    private final HeapByteBufferCache HEAP = new HeapByteBufferCache();
    
    public  ByteBuffer getDirect() {
        return DIRECT.get();
    }
    
    public ByteBuffer getHeap() {
        return HEAP.get();
    }
    
    public ByteBuffer getHeap(int size) {
        return HEAP.get(size);
    }
    
    public long getHeapCacheSize() {
        return HEAP.getByteSize();
    }
    
    public void release(ByteBuffer buffer) {
        if(buffer.isDirect())
            DIRECT.put(buffer);
        else
            HEAP.put(buffer);
    }
    
    public void clearCache() {
        DIRECT.clear();
        HEAP.clear();
    }

}
