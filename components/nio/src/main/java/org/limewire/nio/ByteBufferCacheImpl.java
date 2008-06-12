package org.limewire.nio;

import java.nio.ByteBuffer;
/**
 * Provides both direct and non-direct caches of {@link ByteBuffer ByteBuffers}.
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/nio/ByteBuffer.html#direct">
 * direct vs. non-direct</a> buffers for more information. 

 */
public class ByteBufferCacheImpl implements ByteBufferCache {

    private final HeapByteBufferCache HEAP = new HeapByteBufferCache();
    
    /* (non-Javadoc)
     * @see org.limewire.nio.ByteBufferCache#get(int)
     */
    public ByteBuffer get(int size) {
        return HEAP.get(size);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.nio.ByteBufferCache#getCacheSize()
     */
    public long getCacheSize() {
        return HEAP.getByteSize();
    }
    
    /* (non-Javadoc)
     * @see org.limewire.nio.ByteBufferCache#release(java.nio.ByteBuffer)
     */
    public void release(ByteBuffer buffer) {
        HEAP.put(buffer);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.nio.ByteBufferCache#clearCache()
     */
    public void clearCache() {
        HEAP.clear();
    }

}
