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
    NIOOutputStream(NIOSocket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.channel = channel;
    }
    
    /**
     * Creates the pipes, buffer & registers channels for interest.
     */
    synchronized void init() throws IOException {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("already closed!");

        this.buffer = NIOInputStream.getBuffer();
        sink = new BufferOutputStream(buffer, handler, channel);
        bufferLock = sink.getBufferLock();
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

        if(sink != null)
            sink.shutdown();
            
        shutdown = true;
        if (buffer != null) {
            buffer.clear();
            NIOInputStream.CACHE.push(buffer);
        }
    }
    
    /** Unused */
    public void handleIOException(IOException iox) {
        throw new RuntimeException("unsupported operation", iox);
    }
    
}
