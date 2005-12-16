
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.io.IOException;
import java.util.zip.Deflater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.WriteObserver;

/**
 * DeflaterWriter implements ChannelWriter and InterestWriteChannel.
 * This means that it can be a part of a chain of writers.
 * The chain begins with one object that is a source of data.
 * The chain ends with the socket channel that sends data to the remote computer.
 * 
 * In DeflaterWriter, the reference that points to the next object in the chain is named channel.
 * The reference that points back to the previous object is named observer.
 * 
 * The channel is the sink, it's the channel we can write to by calling channel.write(ByteBuffer b).
 * The observer is the source, it's the channel that gives us data.
 * When we want some data, we call observer.handleWrite().
 * It then calls our write method, where we copy the data from the buffer it gave us into our incoming buffer.
 * 
 * The object that writes to this one calls interest(this, true) to get us to link back to them.
 * With this link established, this object can ask the source to write to us.
 * 
 * Extends and Implements
 * ChannelWriter:        This object has a sink channel it writes to, setWriteChannel() and getWriteChannel().
 * InterestWriteChannel: The object that gives us data can tell us it has some, interest().
 * WriteObserver:        NIO can tell this object to get data and write now, handleWrite().
 */
public class DeflaterWriter implements ChannelWriter, InterestWriteChannel {

	/** A log that we can write lines of text into as the code here runs */
    private static final Log LOG = LogFactory.getLog(DeflaterWriter.class);

    /** The channel this DeflaterWriter writes compresed data into, the sink */    
    private volatile InterestWriteChannel channel;

    /** The object that writes data to this DeflaterWriter, the source */
    private volatile WriteObserver observer;

    /** When you call write, we put your data here */
    private ByteBuffer incoming; // 4 KB, position and limit identify free space, holds data that is not compressed

    /** Then, we compress data from incoming to outgoing */
    private ByteBuffer outgoing; // 1/2 KB, position and limit identify written data, holds compressed data

    /** The Java deflater object that actually compresses the data */
    private Deflater deflater;

    /** Sometimes, we have to flush the deflater. This flag tells us what stage of that process we're on. */
    private int sync = 0;

    /** An empty byte array we use when flushing the deflater */
    private static final byte[] EMPTY = new byte[0];

    /**
     * Make a new DeflaterWriter that will use the given Deflater object.
     * The new DeflaterWriter doesn't have a channel to write to yet, though.
     * You must call setWriteChannel on this new object before you call handleWrite.
     * 
     * @param deflater Give this new DeflaterWriter a Java Deflater object so it can actually compress data
     */
    public DeflaterWriter(Deflater deflater) {
    	
    	// Call the constructor that takes a deflater and a channel
        this(deflater, null); // We don't have a channel to give it next
    }

    /**
     * Make a new DeflaterWriter with the given Java Deflater and a channel to write to.
     * 
     * @param deflater The Java Deflater object that can actually compress data
     * @param channel  The channel this new DeflaterWriter will send compressed data into, the sink
     */
    public DeflaterWriter(Deflater deflater, InterestWriteChannel channel) {

    	// Save the deflater object the caller gave us, we'll use it to actually compress data
        this.deflater = deflater;

        // Setup incoming buffer to hold 4 KB, and leave position and limit identifying the free space
        this.incoming = ByteBuffer.allocate(4 * 1024); // 4 KB of space, position at start and limit and end

        // The outgoing buffer holds 1/2 KB, and position and limit identify the compressed bytes we wrote there
        this.outgoing = ByteBuffer.allocate(512); // 512 bytes is 1/2 KB
        outgoing.flip();                          // Both position and limit at the start

        // Save the channel we'll write to, the sink
        this.channel = channel;
    }

    /**
     * You may have made a DeflaterWriter without giving the constructor a channel to write to.
     * If so, you have to call setWriteChannel on it to give it the channel later.
     * 
     * @param channel The channel this DeflaterWriter will send compressed data into, the sink
     */
    public void setWriteChannel(InterestWriteChannel channel) {

    	// Link this object to the next one in the writing chain
    	this.channel = channel; // This is the object we will write data to

    	// Have the next one link back to us
    	channel.interest(this, true);
    }

    /**
     * Gets the channel that this DeflaterWriter writes to.
     * This is an object that implements the InterestWriteChannel interface.
     * This means it can be a part of a writing chain.
     * 
     * @param return The channel this object writes to
     */
    public InterestWriteChannel getWriteChannel() {
    	
    	// Return this channel that this object has been writing to
        return channel;
    }

    /**
     * The object that writes to this one calls interest(this, true) to get us to link back to them.
     * With this link established, this object can ask the source to write to us.
     * 
     * Or, if you're at the end of the chain, call interest(this, false) to get us to null our link to you.
     * This method will still make sure all the remaining objects in the chain have backlinks.
     * 
     * The link forward is named channel, and is the sink we write to.
     * The link back is named observer, and is the source that writes to us.
     * 
     * Links this object back to the caller behind.
     * Then, links the object this one writes to back to this one.
     * Keeps going forward in the write list, linking all the objects back.
     * 
     * The InterestWriteChannel interface requires this method.
     * Use to get the next object in the writing chain to link back to you.
     * 
     * @param observer The object in the write chain that gives us data
     * @param status   True to have us link back to it, false to null our backlink
     */
    public synchronized void interest(WriteObserver observer, boolean status) {

    	// Point our observer reference back at the given object
        if (status) this.observer = observer;
        else        this.observer = null; // Remove our backlink

        // Have the next object in the chain link back to us
        // If all the objects in the chain implement interest this way, this creates backlinks all down the chain from here
        InterestWriteChannel c = channel;      // Copy the reference because another thread may change channel at any time
        if (c != null) c.interest(this, true); // Link the whole rest of the list even if the caller unlinked itself from here
    }

    /**
     * Moves data from the given buffer to this object's incoming buffer.
     * A call like buffer2.put(buffer1) changes the position both buffers keep.
     * 
     * Before this method runs:
     * The position and limit in the given buffer clip around the data the caller wants to write.
     * The position and limit in the object's incoming buffer clip around the free space there.
     * 
     * After this method runs:
     * The position and limit in the given buffer clip around any data we didn't have room to write.
     * The position and limit in the object's incoming buffer clip around the remaining free space there.
     * 
     * This method just moves data from one buffer to another.
     * It doesn't really have anything to do with writing or deflating.
     * It should be refactored to a class of buffer utility methods.
     * 
     * Java's WritableByteChannel interface requires this method.
     * Call this to write data into this object.
     * 
     * @param buffer The source data the caller wants to write
     * @return       The number of bytes written
     */
    public int write(ByteBuffer buffer) throws IOException {

    	// Keep track of how many bytes we copy from buffer to incoming
        int wrote = 0;

        // This object's incoming buffer still has some free space left
        if (incoming.hasRemaining()) {

        	// Find out how much data buffer has, and how much space incoming has
        	int adding    = buffer.remaining();   // Adding is the number of bytes to copy from buffer
        	int remaining = incoming.remaining(); // Remaining is the free space in the destination buffer

            // There's enough room to move all the data across
            if (remaining >= adding) {

            	// Move it all across
            	incoming.put(buffer); // Calling put moves the position forward in both buffers
                wrote = adding;       // Record how many bytes we moved

            // Not enough room, we'll move as much as we can
            } else {

            	// Get the buffer's current position and limit
            	int position = buffer.position(); // The index in the given buffer where the data we should read starts
                int oldLimit = buffer.limit();    // The index where the data ends

                // Make the limit smaller to match the free space in destination
                buffer.limit(position + remaining); // Now the position and limit in buffer clip out exactly as much data as will fit

                // Copy the data in buffer between position and limit into incoming
                incoming.put(buffer); // This moves position forward in both buffers

                // Restore the buffer's limit again
                buffer.limit(oldLimit); // Now position and limit clip out the portion we didn't have space to write
                
                // Report we wrote as much as we could
                wrote = remaining;
            }
        }

        // Return the number of bytes we moved
        return wrote;
    }

    /**
     * Closes the channel this object writes into.
     * This call will propegate down the chain until it gets to the last object, the one that really writes to the remote computer.
     * That object actually has the real writing channel, and the close method there will close it.
     * 
     * Java's Channel interface requires this method.
     * Call it to close the channel in this object.
     */
    public void close() throws IOException {

    	// This call will spread down the chain to the last object, which will close the real write channel
        Channel c = channel; // Copy the reference because another thread may change it at any time
        if (c != null) c.close();
    }

    /**
     * Determines if the channel this object writes to is open.
     * This call will propegate down the chain until it gets to the last object, the one that really writes to the remote computer.
     * That object actually has the real writing channel, and the close method there will close it.
     * 
     * Java's Channel interface requires this method.
     * Determines if the channel in this object is open or closed.
     */
    public boolean isOpen() {

    	// Copy the reference because another thread may change it at any time
    	Channel c = channel;

    	// If we have the reference, ask the channel if it is open
        if (c != null) return c.isOpen();
        else           return false; // We have no channel reference, say the channel is closed
    }

    /**
     * Moves as much data as possible from the source, through this object, to the next object in the chain.
     * 
     * Compresses any data held here and writes it into the channel.
     * Calls observer.handleWrite to get the object behind us to call our write method to give us more.
     * Loops to keep doing this, drawing data forward throughout the chain.
     * Stops when the sink fills up, or there is no more data to move along.
     * 
     * If handleWrite fills the channel this object writes to, it returns true.
     * This means the caller should stop writing data to us.
     * If handleWrite runs out of data, it returns false.
     * This means a source that has some more data could send it now.
     * 
     * The WriteObserver interface requires this method.
     * Moves data forward all along the write chain.
     * 
     * @return True if the call filled the channel's sending buffer, false if the channel still has room for more
     */
    public boolean handleWrite() throws IOException {

    	// Copy our reference to the sink channel because another thread could change channel at any time
        InterestWriteChannel sink = channel;
        if (sink == null) throw new IllegalStateException("writing with no sink.");

        while (true) {

        	// Step 1
        	// Move data we've already compressed from outgoing to channel

        	// The outgoing buffer contains data we've already compressed
        	// Write it into the channel
        	// The channel probably isn't the socket channel, but another object in the write chain
            channel.write(outgoing); // Moves the position in outgoing forward
            
            // The channel at the end of the chain is a socket channel in non-blocking mode
            // This means it won't be able to take more bytes than are free in its own output buffer
            // When this happens, the channel will fill up, and the object we're writing to won't be able to take any more
            // If the outgoing buffer still has data, this is what happened
            if (outgoing.hasRemaining()) return true; // We filled the channel's output buffer, return true to try again later

            while (true) {

            	// Step 2
            	// Run the deflater to compress data from incoming to outgoing

            	// The number of bytes of comrpessed data that deflate wrote to the outgoing buffer
                int deflated;

                try {

                	// We tell the deflater where to get the data it will compress with the call to setInput below
                	// Now, have it compress from there into the outgoing buffer
                    deflated = deflater.deflate(outgoing.array()); // Returns the number of compressed bytes it wrote

                // This can happen because the deflater doesn't support asynchronous ends
                } catch (NullPointerException npe) { throw (IOException) new IOException().initCause(npe); }

                // The deflater wrote something
                if (deflated > 0) {

                	// Since the deflater looked at the outgoing buffer as an array, it couldn't edit the buffer's position and limit
                	// Set the outgoing buffer's position and limit to clip around the compressed data the deflater wrote
                    outgoing.position(0);
                    outgoing.limit(deflated);

                    // We compressed some data, go back to Step 1 to write it into the channel
                    break;
                }

                // If the computer gets here, the deflater wasn't able to produce any compressed data

                // Step 3
                // Flush the deflater

                // Normal deflate didn't work, so we'll simulate a Z_SYNC_FLUSH
                // Only do this after deflate returned 0 bytes
                // Otherwise, we'll loose data still in the deflater when we call setInput

                try {

                	// This is the first flush, or we came from the bottom of the while loop
                    if (sync == 0) {

                    	// Point the deflater at an empty byte array and change its level to no compression
                    	deflater.setInput(EMPTY);
                        deflater.setLevel(Deflater.NO_COMPRESSION);

                        // Go back to the start of the while loop, try deflate again, and then go to the next step here
                        sync = 1;
                        continue;

                    // Last time, we emptied the input and set the level to no compression
                    } else if (sync == 1) {

                    	// The deflater is still pointed at the empty array, turn compression back on
                    	deflater.setLevel(Deflater.DEFAULT_COMPRESSION);

                    	// Go back to the start of the while loop, try deflate again, and skip these flush steps to make it to the bottom of the loop
                    	sync = 2;
                        continue;
                    }

                // This can happen because the deflater doesn't support asynchronous ends
                } catch(NullPointerException npe) { throw (IOException) new IOException().initCause(npe); }

                // Step 4
                // If we have no data, tell our write observers to give us some
                // They will call our write method, which puts data in our incoming buffer

                // The incoming buffer is empty
                if (incoming.position() == 0) {

                	// Tell the object behind us in the chain to write more data to us now
                	// This call will propegate all the way back in the chain to the original data source
                	WriteObserver o = observer; // Copy the reference because another thread could change observer at any time
                    if (o != null) o.handleWrite();

                    // The incoming buffer is still empty
                    // None of the objects behind us in the chain could write any data to us
                    if (incoming.position() == 0) {

                    	// We have nothing left to write
                    	// However, it's possible that between the o.handleWrite() check and here, another thread added a source to the chain

                    	// Only allow one thread to run this code for this object at a time
                        synchronized (this) {
                        	
                        	// If there is no object behind us, tell the next object to stop linking back to us
                            if (observer == null) sink.interest(this, false);

                            // Otherwise, we have nothing to write, but our observer might call our write method and give us some more data at any point
                        }

                        // We did not fill the channel's sending buffer, return false
                        return false;
                    }
                }

                // Step 5
                // We've got new data to compress, point the deflater at it

                try {

                	// Tell the deflater to get data to compress from the incoming array
                    deflater.setInput(incoming.array(), 0, incoming.position());

                // This can happen because the deflater doesn't support asynchronous ends
                } catch (NullPointerException npe) { throw (IOException) new IOException().initCause(npe); }

                // Mark the incoming buffer as empty and set sync to start the deflater flush process at the beginning again
                incoming.clear(); // Set position and limit in the incoming buffer so the whole thing is free space
                sync = 0;         // Flush the deflater the next time it returns 0 bytes
            }
        }
    }

    /**
     * Shuts down the original data source for this write chain.
     * This call propegates backwards in the list, all the way to the first object that calls write to give the second one data.
     * 
     * The Shutdownable interface requires this method.
     * Call it to have this object and contained objects free their resources.
     */
    public void shutdown() {

    	// Call shutdown on the object that is giving us data
    	Shutdownable listener = observer; // Copy the reference because another thread might change it at any time
        if (listener != null) listener.shutdown();
    }

    /**
     * This method is not used or supported.
     * The IOErrorObserver interface requires that we have it, so here it is.
     * 
     * The IOErrorObserver interface requires this method.
     * It makes this object able to handle IO exceptions.
     */
    public void handleIOException(IOException x) {

    	// Wrap the given IOException as a RuntimeException named "Unsupported", and throw it again
        throw new RuntimeException("Unsupported", x);
    }
}
