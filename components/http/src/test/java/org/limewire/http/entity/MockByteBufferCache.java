package org.limewire.http.entity;

import java.nio.ByteBuffer;

import org.limewire.nio.ByteBufferCacheImpl;

public class MockByteBufferCache extends ByteBufferCacheImpl {

    public int bytes;

    public int buffers;

    @Override
    public ByteBuffer get(int size) {
        bytes += size;
        buffers++;
        return super.get(size);
    }

    @Override
    public void release(ByteBuffer buffer) {
        bytes -= buffer.capacity();
        buffers--;
        super.release(buffer);
    }

}
