package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Manages writing data to the network from a piped blocking OutputStream.
 *
 * This uses a BufferOutputStream that waits on a lock when no data is available.
 * The stream exposes a BufferLock that should be notified when data is available
 * to be written.
 */
class NIOOutputStream {
    
    private static final Log LOG = LogFactory.getLog(NIOOutputStream.class);
    
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

        this.buffer = ByteBuffer.allocate(8192); // TODO: use a ByteBufferPool
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
    void writeChannel() throws IOException {// write everything we can.
        synchronized(bufferLock) {
            buffer.flip();
            while(buffer.hasRemaining() && channel.write(buffer) > 0);
            buffer.compact();
            
            // If there's room in the buffer, we're interested in reading.
            if(buffer.hasRemaining())
                bufferLock.notify();
                
            // if we were able to write everything, we're not interested in more writing.
            // otherwise, we are interested.
            if(buffer.position() == 0)
                NIODispatcher.instance().interestWrite(channel, false);
        }
    }
    
    /**
     * Notification that an IOException has occurred on one of these channels.
     */
    public void handleIOException(IOException iox) {
        handler.shutdown();
    }
    
    /**
     * Shuts down all internal channels.
     * The SocketChannel should be shut by NIOSocket.
     */
    synchronized void shutdown() {
        if(shutdown)
            return;

        if(sink != null)
            sink.shutdown();
            
        shutdown = true;
    }
}
