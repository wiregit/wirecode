package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Manages writing data to the network from a piped blocking OutputStream.
 *
 * This uses a BufferOutputStream that waits on a lock when no data is available.
 * The stream exposes a BufferLock that should be notified when data is available
 * to be written.
 */
class NIOOutputStream implements WriteObserver {
    
    private final NIOSocket handler;
    private final SocketChannel channel;
    private BufferOutputStream sink;
    private Object bufferLock;
    private ByteBuffer buffer;
    private boolean shutdown;
    
    /**
     * Constructs a new pipe to allow SocketChannel's reading to funnel
     * to a blocking InputStream.
     */
    NIOOutputStream(NIOSocket handler, SocketChannel channel) {
        this.handler = handler;
        this.channel = channel;
    }
    
    /**
     * Creates the pipes, buffer & registers channels for interest.
     */
    private synchronized NIOOutputStream init() throws IOException {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("already closed!");

        this.buffer = NIODispatcher.instance().getBufferCache().getHeap();
        sink = new BufferOutputStream(buffer, handler, channel);
        bufferLock = sink.getBufferLock();
        return this;
    }
    
    /**
     * Retrieves the OutputStream to write to.
     */
    synchronized OutputStream getOutputStream() throws IOException {
        if(buffer == null)
            init();
        
        return sink;
    }
    
    /**
     * Notification that a write can happen on the SocketChannel.
     */
    public boolean handleWrite() throws IOException {// write everything we can.
        if(buffer == null)
            return false;
        
        synchronized(bufferLock) {
            buffer.flip();
            while(buffer.hasRemaining() && channel.write(buffer) > 0);
            if (buffer.position() > 0) {
                if (buffer.hasRemaining()) 
                    buffer.compact();
                else 
                    buffer.clear();
            } else 
                buffer.position(buffer.limit()).limit(buffer.capacity());
            
            // If there's room in the buffer, we're interested in reading.
            if(buffer.hasRemaining())
                bufferLock.notify();
                
            // if we were able to write everything, we're not interested in more writing.
            // otherwise, we are interested.
            if(buffer.position() == 0) {
                NIODispatcher.instance().interestWrite(channel, false);
                return false;
            } else {
                return true;
            }
        }
    }
    
    /**
     * Shuts down all internal channels.
     * The SocketChannel should be shut by NIOSocket.
     */
    public synchronized void shutdown() {
        if(shutdown)
            return;
        
        if(buffer != null)
            NIODispatcher.instance().getBufferCache().release(buffer);

        if(sink != null)
            sink.shutdown();
            
        shutdown = true;
    }
    
    /** Unused */
    public void handleIOException(IOException iox) {
        throw new RuntimeException("unsupported operation", iox);
    }
    
}
