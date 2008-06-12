package org.limewire.nio;

import java.nio.ByteBuffer;

/** A cache of {@link ByteBuffer ByteBuffers}. */
public interface ByteBufferCache {

    /** Retrieves a new ByteBuffer of the given size. */
    public ByteBuffer get(int size);

    /** Returns the total number of bytes this cache has available. */
    public long getCacheSize();

    /** Returns a ByteBuffer back to the cache. */
    public void release(ByteBuffer buffer);

    /** Erases all cached ByteBuffers. */
    public void clearCache();

}