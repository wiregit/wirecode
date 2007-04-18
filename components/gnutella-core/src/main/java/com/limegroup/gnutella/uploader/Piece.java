package com.limegroup.gnutella.uploader;

import java.nio.ByteBuffer;

public class Piece implements Comparable<Piece> {

    private long offset;
    
    private ByteBuffer buffer;
    
    public Piece(long offset, ByteBuffer buffer) {
        this.offset = offset;
        this.buffer = buffer;
    }

    public long getOffset() {
        return offset;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int compareTo(Piece o) {
        long l = offset - o.offset;
        return (l < 0) ? -1 : (l == 0) ? 0 : 1;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[offset=" + offset + ",length=" + buffer.remaining() + "]";
    }
    
}
