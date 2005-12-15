
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import com.limegroup.gnutella.io.ChannelReader;

/**
 * An InflaterReader object can read compressed data from a channel and decompress it all at the same time.
 * 
 * Make an InflateReader and give it a source channel of compressed data.
 * Then, call read(ByteBuffer b) on it to have it read data from the channel and decompress it into the given buffer.
 * 
 * Extends and Implements
 * ChannelReader:                         This object has a source channel it reads from, setReadChannel() and getReadChannel().
 * java.nio.channels.ReadableByteChannel: Call read() to get data from this object.
 */
public class InflaterReader implements ChannelReader, ReadableByteChannel {

    /** The Java Inflater object that actually decompresses the data. */
    private Inflater inflater; // A java.util.zip.Inflater is an object that can do decompression with the deflate algorithm

    /**
     * The channel we read the compressed data from.
     * 
     * Any object that implements ReadableByteChannel can go here.
     * NIOSocket.setReadObserver() gives us the actual Java SocketChannel object from inside a NIOSocket object.
     * When read() calls channel.read(data), it will actually be reading data from the Java platform, not just another LimeWire object.
     */
    private ReadableByteChannel channel;

    /** A 512 byte buffer where compressed data from the channel waits to be decompressed. */
    private ByteBuffer data; // A java.nio.ByteBuffer is a buffer that can hold bytes

    /**
     * Make a new InflateReader object without a source of compressed data.
     * Call setReadChannel(ReadableByteChannel) to give it a source of compressed data later.
     * Then, you can call read(ByteBuffer) to have the object decompress the data.
     * 
     * @param inflater The java.util.zip.Inflater object to keep in this one that will actually decompress the data
     */
    public InflaterReader(Inflater inflater) {

    	// Call the main constructor without giving it a readable byte channel
    	this(null, inflater);
    }

    /**
     * Make a new InflateReader object from the given source channel and inflater object.
     * 
     * @param channel  An object we can call read(ByteBuffer) on to read compressed data from
     * @param inflater The Java Inflater object that can actually decompress data
     */
    public InflaterReader(ReadableByteChannel channel, Inflater inflater) {

    	// Make sure we got a Java Inflater object
        if (inflater == null) throw new NullPointerException("null inflater!");

        // Save the given readable byte channel and Java zip inflater in this object
        this.channel  = channel;  // This is where we'll get the compressed data
        this.inflater = inflater; // This is how we'll decompress it

        // Setup this object's temporary buffer
        this.data = ByteBuffer.allocate(512); // Make it able to hold 512 bytes
    }

    /**
     * You may have made a new InflateReader object without giving the constructor a channel to read from.
     * If you did, call this method to tell this InflaterReader where to get the compressed data.
     * 
     * NIOSocket.setReadObserver() will call this, and give us the actual Java SocketChannel object from inside a NIOSocket object.
     * 
     * @param channel An object we can call read(ByteBuffer) on to read compressed data from
     */
    public void setReadChannel(ReadableByteChannel channel) {

    	// Make sure the caller is actually giving us an object
        if (channel == null) throw new NullPointerException("cannot set null channel!");

        // Save the channel in this object
        this.channel = channel;
    }

    /**
     * Gets the channel this object has been reading compressed data from.
     * This is an object that implements Java's ReadableByteChannel interface.
     * This means you can call read(ByteBuffer) on it to read compressed data from it.
     * We saved it in the member variable named channel.
     * 
     * @return The object that you can call read(ByteBuffer) on to read compressed data from
     */
    public ReadableByteChannel getReadChannel() {

    	// We saved it in channel
        return channel;
    }
    
    /**
     * Reads compressed data from the channel, decompresses it, and writes it to the given buffer decompressed.
     * 
     * Each time you call read, the method will try to get data from the channel and decompress it.
     * If there's no data waiting on our end of the channel, it won't decompress anything, and will return 0.
     * If the channel has a lot of data, it will fill the buffer you give it.
     * If the buffer isn't big enough, call it multiple times to pull everything through.
     * 
     * If the inflater object or channel breaks, read returns -1.
     * 
     * This method doesn't block because it never waits for data to arrive on the channel, it just decompresses what's already there.
     * 
     * This object implements ReadableByteChannel just like channel does.
     * This means we have to implement a read(ByteBuffer) method here too.
     * 
     * @param buffer A ByteBuffer to write the decompressed data into
     * @return       The number of bytes we wrote there, 0 if none, -1 if we hit the end and should stop
     */
    public int read(ByteBuffer buffer) throws IOException {

        int written = 0; // The number of bytes of decompressed data we'll write to the given output buffer
        int read    = 0;

        // This loop moves data from the channel to the temporary buffer, and inflates it to the given output buffer
        // Loop until
        // (a) Our output buffer fills up
        // (b) The inflater finishes or needs a dictionary
        // (c) There's no more compressed data in the channel or temporary buffer
        while (buffer.hasRemaining()) { // Break for (a) the output buffer fills up

        	// Have the inflate object inflate any compressed data sitting in the data buffer
            int inflated = inflate(buffer); // setInput below told it where to get the data, buffer is where inflate will write
            written += inflated;            // Add the number of decompressed bytes it wrote to written

            // The inflater couldn't inflate anything
            if (inflated == 0) {

            	// The inflater is done, or it needs a dictionary
        		if (inflater.finished() || inflater.needsDictionary()) {

        			// We have to stop now and return -1
                    read = -1;
                    break; // Break for (b) the inflater finished or needs a dictionary
        		}

        		// The buffer needs some compressed data to decompress
                if (inflater.needsInput()) {

                    // Move as much compressed data from the channel to the buffer as we can
                    while (true) {

                    	// Stop because the temporary buffer is out of room
                    	if (!data.hasRemaining()) break;

                    	/*
                    	 * Tour Point
                    	 * 
                    	 * This is where LimeWire actually reads data from the remote computer.
                    	 * channel is a java.nio.channels.SocketChannel object.
                    	 */

                    	// Move compressed data from the channel into the temporary buffer
                    	read = channel.read(data); // Sets read to the number of bytes moved

                    	// The read method returns 0 for nothing read, or -1 if it reached the end of the channel
                    	if (read <= 0) break; // Break for (c) there's no more compressed data in the channel or temporary buffer
                    }

                    // If the temporary buffer has no data to decompress, leave the loop
                    if (data.position() == 0) break;

                    // Tell the inflater where it can find the compressed data
                    inflater.setInput(
                        data.array(),     // The compressed data is in the temporary buffer
                        0,                // At the start of the buffer
                        data.position()); // The length of the input data is the position where we could write more into the buffer

                    // Clear the buffer
                    data.clear(); // Sets the position back to 0, letting us treat it as an empty buffer again
                }
            }
        }

        // We decompressed all the data we could
        if      (written > 0) return written; // Say how many bytes of decompressed data we wrote to the given buffer
        else if (read == -1)  return -1;      // The inflater finished, needs a dictionary, or we hit the very end of the channel
        else                  return 0;       // The channel just doesn't have any compressed bytes for us right now
    }

    /**
     * This method only gets called by read, the one above.
     * Runs the inflater, which read set on the decompressed data in the temporary buffer.
     * 
     * @param buffer The same buffer passed to read, where we write the decompressed data
     * @return       The number of bytes we wrote there
     */
    private int inflate(ByteBuffer buffer) throws IOException {

    	// Record how many bytes we wrote
        int written = 0;

        // A buffer keeps a position, get it
        int position = buffer.position(); // This is where we can write next

        try {
        	
        	// The inflater already knows where to read the compressed data
        	// We gave it the temporary buffer in the read method, with the call inflater.setInput above

        	// This is the line of code that actually decompresses the data
            written = inflater.inflate(  // Returns the number of decompressed bytes written to the buffer
            		buffer.array(),      // The buffer to write the decompressed bytes into
            		position,            // Don't start writing at the start of that buffer, write this far into it
            		buffer.remaining()); // The buffer has this much space left at that point

        // The inflater threw a data format exception
        } catch (DataFormatException dfe) {

        	// Wrap it with a new io exception, and throw that instead
            IOException x = new IOException();
            x.initCause(dfe);
            throw x;

        // If a separate thread closed the inflater, it might throw a null pointer exception
        } catch (NullPointerException npe) {

        	// Wrap it with a new io exception, and throw that instead
            IOException x = new IOException();
            x.initCause(npe);
            throw x;
        }

        // Move the position forward in the buffer over the bytes we wrote, and return the number we wrote
        buffer.position(position + written);
        return written;
    }

    /**
     * Determines if this reader is open.
     * Checks if the channel we've been reading compressed data from is open.
     * 
     * @return True if the channel we've been getting compressed data from is open, false if it's closed
     */
    public boolean isOpen() {
    	
    	// Return true or false if the compressed channel is open or closed
        return channel.isOpen();
    }
    
    /**
     * Close the channel we've been reading compressed data from.
     */
    public void close() throws IOException {

    	// Close the channel we've been reading compressed data from
        channel.close();
    }
}
