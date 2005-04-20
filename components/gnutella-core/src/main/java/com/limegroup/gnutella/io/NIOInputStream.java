package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Manages reading data from the network & piping it to a blocking input stream.
 *
 * This uses a BufferInputStream that waits on a lock when no data is available.
 * The stream exposes a BufferLock that should be notified when data is available
 * to be read.
 */
class NIOInputStream {
    
    private static final Log LOG = LogFactory.getLog(NIOInputStream.class);
    
    private final NIOSocket handler;
    private final SocketChannel channel;
    private BufferInputStream source;
    private Object bufferLock;
    private ByteBuffer buffer;
    private boolean shutdown;
    
    /**
     * Constructs a new pipe to allow SocketChannel's reading to funnel
     * to a blocking InputStream.
     */
    NIOInputStream(NIOSocket handler, SocketChannel channel) throws IOException {
        this.handler = handler;
        this.channel = channel;
    }
    
    /**
     * Creates the pipes, buffer, and registers channels for interest.
     */
    synchronized void init() throws IOException {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("Already closed!");
        
        buffer = ByteBuffer.allocate(8192); // TODO: use a ByteBufferPool
        source = new BufferInputStream(buffer, handler, channel);
        bufferLock = source.getBufferLock();
        
        NIODispatcher.instance().interestRead(channel, true);
    }
    
    /**
     * Retrieves the InputStream to read from.
     */
    synchronized InputStream getInputStream() throws IOException {
        if(buffer == null)
            init();
        
        return source;
    }
    
    /**
     * Notification that a read can happen on the SocketChannel.
     */
    void readChannel() throws IOException {
        synchronized(bufferLock) {
            int read = 0;
            
            // read everything we can.
            while(buffer.hasRemaining() && (read = channel.read(buffer)) > 0);
            if(read == -1)
                source.finished();
            
            // If there's data in the buffer, we're interested in writing.
            if(buffer.position() > 0 || read == -1)
                bufferLock.notify();
    
            // if there's room in the buffer, we're interested in more reading ...
            // if not, we're not interested in more reading.
            if(!buffer.hasRemaining() || read == -1)
                NIODispatcher.instance().interestRead(channel, false);
        }
    }
    
    /**
     * Notification that an IOException has occurred on one of these channels.
     */
    public void handleIOException(IOException iox) {
        // Inform the NIOSocket that things are dead.  That will shut us down.
        handler.streamDied();
    }
    
    /**
     * Shuts down all internal channels.
     * The SocketChannel should be shut by NIOSocket.
     */
    synchronized void shutdown() {
        if(shutdown)
            return;
         
        if(source != null)
            source.shutdown();
        
        shutdown = true;
    }
}
                
        
    