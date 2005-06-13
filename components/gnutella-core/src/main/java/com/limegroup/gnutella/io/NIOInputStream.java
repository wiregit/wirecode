package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Manages reading data from the network & piping it to a blocking input stream.
 *
 * This uses a BufferInputStream that waits on a lock when no data is available.
 * The stream exposes a BufferLock that should be notified when data is available
 * to be read.
 *
 * ReadableByteChannel is implemented so that future ReadObservers can take over
 * reading and use this NIOInputStream as a source channel to read any buffered
 * data.
 */
class NIOInputStream implements ReadObserver, ReadableByteChannel {
    
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
    synchronized NIOInputStream init() throws IOException {
        if(buffer != null)
            throw new IllegalStateException("already init'd!");
            
        if(shutdown)
            throw new IOException("Already closed!");
        
        buffer = ByteBuffer.allocate(8192); // TODO: use a ByteBufferPool
        source = new BufferInputStream(buffer, handler, channel);
        bufferLock = source.getBufferLock();
        
        NIODispatcher.instance().interestRead(channel, true);
        return this;
    }
    
    /**
     * Reads from this' channel (which is the temporary ByteBuffer,
     * not the SocketChannel) into the given buffer.
     */
    public int read(ByteBuffer toBuffer) {
        if(buffer == null)
            return 0;
        
        int read = 0;

        if(buffer.position() > 0) {
            buffer.flip();
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
            buffer.compact();
        }
        
        return read;
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
    public void handleRead() throws IOException {
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
     * Shuts down all internal channels.
     * The SocketChannel should be shut by NIOSocket.
     */
    public synchronized void shutdown() {
        if(shutdown)
            return;
         
        if(source != null)
            source.shutdown();
        
        shutdown = true;
    }
    
    /** Unused */
    public void handleIOException(IOException iox) {
        throw new RuntimeException("unsupported operation", iox);
    }    
    
    /**
     * Does nothing, since this is implemented for ReadableByteChannel,
     * and that is used for reading from the temporary buffer --
     * there is no buffer to close in this case.
     */
    public void close() throws IOException {
    }
    
    /**
     * Always returns true, since this is implemented for ReadableByteChannel,
     * and the Buffer is always available for reading.
     */
    public boolean isOpen() {
        return true;
    }
}
                
        
    