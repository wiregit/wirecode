package com.limegroup.gnutella.connection;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

class ReadBufferChannel implements ReadableByteChannel {
    private ByteBuffer buffer;
    
    public ReadBufferChannel(ByteBuffer source) {
        buffer = source;
    }
    
    public ReadBufferChannel(byte[] source) {
        this(ByteBuffer.wrap(source));
    }
    
    public ReadBufferChannel(byte[] source, int off, int len) {
        this(ByteBuffer.wrap(source, off, len));
    }
    
    public int read(ByteBuffer toBuffer) {
        int read = 0;
        
        int remaining = buffer.remaining();
        int toRemaining = toBuffer.remaining();
        if(toRemaining >= remaining) {
            toBuffer.put(buffer);
            read += remaining;
        } else {
            int limit = buffer.limit();
            int position = buffer.position();
            buffer.limit(position + toRemaining);
            toBuffer.put(buffer);
            read += toRemaining;
            buffer.limit(limit);
        }
        
        return read;
    }
    
    public boolean isOpen() {
        return true;
    }
    
    public void close() throws IOException { }
}