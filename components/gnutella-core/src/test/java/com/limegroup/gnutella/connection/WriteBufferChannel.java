package com.limegroup.gnutella.connection;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.WriteObserver;

class WriteBufferChannel implements InterestWriteChannel {
    private ByteBuffer buffer;
    private boolean closed = false;
    private InterestWriteChannel channel;
    private WriteObserver observer;
    private boolean status;
    private boolean shutdown;
    
    public WriteBufferChannel(int size) {
        buffer = ByteBuffer.allocate(size);
    }
    
    public WriteBufferChannel(ByteBuffer buffer, InterestWriteChannel channel) {
        this.buffer = buffer;
        this.channel = channel;
        channel.interest(this, true);
    }
    
    public WriteBufferChannel(byte[] data, InterestWriteChannel channel) {
        this(ByteBuffer.wrap(data), channel);
    }
    
    public WriteBufferChannel(byte[] data, int off, int len, InterestWriteChannel channel) {
        this(ByteBuffer.wrap(data, off, len), channel);
    }
    
    public WriteBufferChannel() {
        this(0);
    }
    
    public WriteBufferChannel(InterestWriteChannel channel) {
        this(ByteBuffer.allocate(0), channel);
    }
    
    public int write(ByteBuffer source) throws IOException {
        int wrote = 0;
        
        if(buffer.hasRemaining()) {
            int remaining = buffer.remaining();
            int adding = source.remaining();
            if(remaining >= adding) {
                buffer.put(source);
                wrote = adding;
            } else {
                int oldLimit = source.limit();
                int position = source.position();
                source.limit(position + remaining);
                buffer.put(source);
                source.limit(oldLimit);
                wrote = remaining;
            }
        }
        
        return wrote;
    }
    
    public boolean isOpen() {
        return !closed;
    }
    
    public void close() throws IOException {
        closed = true;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
    public ByteBuffer getBuffer() {
        return (ByteBuffer)buffer.flip();
    }
    
    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    public void resize(int size) {
        buffer = ByteBuffer.allocate(size);
    }
    
    public void clear() {
        buffer.clear();
    }
    
    public boolean interested() {
        return status;
    }
    
    public void interest(WriteObserver observer, boolean status) {
        this.observer = observer;
        this.status = status;
    }
    
    public boolean handleWrite() throws IOException {
        while(buffer.hasRemaining() && channel.write(buffer) > 0);
        return buffer.hasRemaining();
    }
    
    public void shutdown() {
        shutdown = true;
    }
    
    public void handleIOException(IOException iox) {
        throw (RuntimeException)new UnsupportedOperationException("not implemented").initCause(iox);
    }
}