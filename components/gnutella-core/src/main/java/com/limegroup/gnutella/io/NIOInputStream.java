
// Edited for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Stack;

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
 * 
 * Extends and Implements
 * ReadObserver:                          NIO can tell this object to read now, handleRead().
 * java.nio.channels.ReadableByteChannel: Call read() to read data from this object.
 */
class NIOInputStream implements ReadObserver, ReadableByteChannel {

    /**
     * A stack of empty 8 KB ByteBuffer objects.
     * When you want an 8 KB ByteBuffer, don't make one yourself.
     * Instead, call the static method NIOInputStream.getBuffer().
     * It will remove one from this stack, and give it to you.
     * Keeping a stack of buffers this way is faster than allocating and garbage collecting them individually.
     * 
     * This stack is kept in NIOInputStream, and it's static.
     * Both NIOInputStream and NIOOutputStream use it.
     */
    static final Stack CACHE = new Stack();

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
        
        buffer = getBuffer(); 
        source = new BufferInputStream(buffer, handler, channel);
        bufferLock = source.getBufferLock();
        
        NIODispatcher.instance().interestRead(channel, true);
    }

    /**
     * Get an 8 KB ByteBuffer you can use.
     * BufferInputStream and BufferOutputStream objects get their buffer objects from this method.
     * 
     * @return An empty 8 KB ByteBuffer from our cache of them
     */
    static ByteBuffer getBuffer() {

        // Make sure only one thread can do this at a time
        synchronized(CACHE) {

            // The CACHE stack object doesn't have any 8 KB ByteBuffers in it yet
            if (CACHE.isEmpty()) {

                // Make a new one, and put it on the stack
                ByteBuffer buf = ByteBuffer.allocateDirect(8192); // Use allocateDirect for a large ByteBuffer you're going to keep around for awhile
                CACHE.push(buf); // Place the ByteBuffer on the top of the CACHE stack
            }

            // Remove an empty 8 KB ByteBuffer from the top of the CACHE stack, and return it
            return (ByteBuffer)CACHE.pop();
        }
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

            /*
             * Tour Point
             * 
             * This is where LimeWire actually reads data from the remote computer.
             * channel is a java.nio.channels.SocketChannel object.
             */

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

        // If we've already run this method for this object, don't do it again
        if (shutdown) return;

        // If this NIOInputStream object has a BufferInputStream source, tell it to shut itself down
        if (source != null) source.shutdown();

        // Mark this object as being shut down
        shutdown = true;

        try { close(); } catch (IOException ignored) {}
    }
    
    /** Unused */
    public void handleIOException(IOException iox) {
        throw new RuntimeException("unsupported operation", iox);
    }

    /**
     * Marks the 8 KB ByteBuffer this object was using as clear, and puts it back onto the stack of them.
     */
    public void close() throws IOException {

        /*
         * (do)
         * here's the comment that was here
         * it doesn't make any sense, because this method does do something
         * 
         * Does nothing, since this is implemented for ReadableByteChannel,
         * and that is used for reading from the temporary buffer --
         * there is no buffer to close in this case.
         */

        // If we made a ByteBuffer for this NIOInputStream
        if (buffer != null) {

            // Mark it as empty and put it on the top of the CACHE stack
            buffer.clear();     // Moves position to the start and limit to the end, doesn't actually free any memory
            CACHE.push(buffer); // Put the buffer on the top of the java.util.Stack object named CACHE
        }
    }

    /**
     * Always returns true, since this is implemented for ReadableByteChannel,
     * and the Buffer is always available for reading.
     */
    public boolean isOpen() {
        return true;
    }
}

        
    