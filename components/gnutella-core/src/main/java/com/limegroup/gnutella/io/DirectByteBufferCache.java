package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;
import java.util.Stack;

public class DirectByteBufferCache {

    private final Stack<ByteBuffer> CACHE = new Stack<ByteBuffer>();

    public ByteBuffer get() {
        synchronized (CACHE) {
            if (CACHE.isEmpty()) {
                ByteBuffer buf = ByteBuffer.allocateDirect(8192);
                return buf;
            } else {
                return CACHE.pop();
            }
        }
    }

    public void put(ByteBuffer buf) {
        buf.clear();
        CACHE.push(buf);
    }
    
    public void clear() {
        CACHE.clear();
    }
    
}
