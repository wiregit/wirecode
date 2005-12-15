
// Edited for the Learning branch

package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.WriteObserver;

/**
 * WriteBufferChannel is a test class.
 * It implements ChannelWriter and InterestWriteChannel, just like DeflaterWriter
 * This means it can be part of a write chain.
 * It shows how to implement write, handleWrite, and interest.
 * 
 * When we're testing DeflaterWriter, for instance, we build a little write chain.
 * We make a DeflaterWriter, and then put it inbetween two WriteBufferChannel objects.
 */
public class WriteBufferChannel implements ChannelWriter, InterestWriteChannel {

	private ByteBuffer buffer; // position and limit clip out the free space
    private boolean closed = false;
    public InterestWriteChannel channel;
    public WriteObserver observer;
    public boolean status;
    private boolean shutdown;

    // Constructors

    /**
     * Make a new WriteBufferChannel with an empty buffer and no channel.
     */
    public WriteBufferChannel() {

    	// Call the constructor that takes a size for the buffer
    	this(0); // Allocate a buffer that can't hold anything
    }

    /**
     * Make a new WriteBufferChannel object, given a size for the buffer.
     * 
     * @param size How big to allocate the buffer in this class
     */
    public WriteBufferChannel(int size) {

    	// Make the buffer in this object the given size
        buffer = ByteBuffer.allocate(size); // The position is at the start and the limit is at the end
    }
    
    /**
     * Make a new WriteBufferChannel object, given a buffer and the channel we'll write to.
     * 
     * @param buffer  A ByteBuffer to load in this class
     * @param channel The InterestWriteChannel this object will write to
     */
    public WriteBufferChannel(ByteBuffer buffer, InterestWriteChannel channel) {
    	
    	// Keep the given buffer and channel here
        this.buffer  = buffer;
        this.channel = channel;
        
        // Tell the given channel we are interested in it
        channel.interest(this, true); // True to add this to it, linking us into the chain
    }

    /**
     * Make a new WriteBufferChannel, given a byte array to use as the buffer, and the channel we'll write to.
     * 
     * @param data    The byte array we'll wrap into a buffer and keep in this object
     * @param channel The InterestWriteChannel this object will write to
     */
    public WriteBufferChannel(byte[] data, InterestWriteChannel channel) {

    	// Wrap the array into a ByteBuffer, and call the constructor that takes a buffer and channel
        this(ByteBuffer.wrap(data), channel);
    }
    
    /**
     * Make a new WriteBufferChannel, given a byte array and bounds within it, and the channel we'll write to.
     * 
     * @param data    The byte array we'll wrap into a buffer and keep in this object
     * @param off     Set the position in the buffer to this value
     * @param len     Set the limit in the buffer this far beyond the position
     * @param channel The InterestWriteChannel this object will write to
     */
    public WriteBufferChannel(byte[] data, int off, int len, InterestWriteChannel channel) {

    	// Wrap the array into a ByteBuffer with the specified position and limit, and call the constructor that takes a buffer and channel
        this(ByteBuffer.wrap(data, off, len), channel);
    }

    /**
     * Make a new WriteBufferChannel with a 0 size buffer that will write to the given channel.
     * 
     * @param channel The InterestWriteChannel this object will write to
     */
    public WriteBufferChannel(InterestWriteChannel channel) {

    	// Allocate a new no size ByteBuffer for this object, and write to the given channel
        this(ByteBuffer.allocate(0), channel);
    }
    
    /**
     * Call this method to write data to this object.
     * Give it a ByteBuffer with the data you want to write between position and limit.
     * It will move position forward past the data it writes, and return the number of bytes it wrote.
     * 
     * @param write The data you want to write
     * @return      The number of bytes this method wrote
     */
    public int write(ByteBuffer source) throws IOException {

    	// Keep track of how many bytes we write
    	int wrote = 0;

    	// The buffer in this object has some free space
        if (buffer.hasRemaining()) {

            int remaining = buffer.remaining(); // The amount of free space in this object's destination buffer
            int adding    = source.remaining(); // The amount of data in the given source buffer

            // The buffer has enough free space to hold the data in source
            if (remaining >= adding) {

            	// Move it across and record how much we wrote
            	buffer.put(source); // This moves position forward in both buffers
                wrote = adding;

            // We don't have enough room to take all the data
            } else {

            	// Save the source buffer's current position and limit
            	int position = source.position();
            	int oldLimit = source.limit();

            	// Adjust the source buffer's limit to clip the amount of data we can take
                source.limit(position + remaining);

                // Move that much data across
                buffer.put(source); // Moves position forward in both buffers

                // Put back the source buffer's limit and record how much we wrote
                source.limit(oldLimit);
                wrote = remaining;
            }
        }

        // Return how much we wrote
        return wrote; // This is also how much we moved forward the source buffer's position
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
    
    public int written() {
        return buffer.position();
    }
    
    public int remaining() {
        return buffer.remaining();
    }
    
    public ByteBuffer getBuffer() {
        return (ByteBuffer)buffer.flip();
    }
    
    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
        channel.interest(this, true);
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
    
    public void setWriteChannel(InterestWriteChannel chan) {
        channel = chan;
    }
    
    public InterestWriteChannel getWriteChannel() {
        return channel;
    }
    
    /**
     * 
     * Call interest(this, true) to have this object link to you.
     * 
     * @param observer A reference to you, a write observer
     * @param interest True to have us link to you, false to have us not link to you
     */
    public void interest(WriteObserver observer, boolean status) {

    	// Just save both things in the object
    	this.observer = observer;
        this.status   = status; // What about unlinking write observers with false?
    }

    /**
     * 
     * Call handleWrite to have this object send everything it's holding into the channel
     * 
     * @return True if handleWrite filled the channel, false if it didn't
     */
    public boolean handleWrite() throws IOException {

        while (true) {

        	// If the buffer is empty, quit
        	if (!buffer.hasRemaining()) break;

        	// Move data from the buffer to the channel
        	// Moves the position in the buffer forward past the bytes it writes
        	// Returns the number of bytes written
        	int written = channel.write(buffer);

        	// If the channel is full, quit
        	if (written <= 0) break;
        }

        // If we emptied our whole buffer into the channel, ask it to stop linking to us
        if (!buffer.hasRemaining()) channel.interest(this, false);

        // Return true if we filled the channel, false if we emptied our whole buffer into it
        return buffer.hasRemaining();
    }

    public void shutdown() {
        shutdown = true;
    }
    
    public void handleIOException(IOException iox) {
        throw (RuntimeException)new UnsupportedOperationException("not implemented").initCause(iox);
    }
    
    public int position() {
        return buffer.position();
    }
    
    public int limit() {
        return buffer.limit();
    }
}