
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * A BufferByteArrayOutputStream holds some data in an ByteBuffer it keeps internally.
 * You can write more and more data to it, and it will trade in its ByteBuffer for a larger one when necessary.
 * Use the same methods on it you're familiar with from the ByteArrayOutputStream class.
 * 
 * The growth feature is optional, pass false to a constructor to turn it off.
 * 
 * This class extends ByteArrayOutputStream just to use its method names.
 * Methods here override the methods from ByteArrayOutputStream.
 * There is no IO ByteArrayOutputStream here at all, the data is kept in an NIO ByteBuffer.
 * 
 * Write data into a BufferByteArrayOutputStream like this:
 * 
 *     BufferByteArrayOutputStream buffer = new BufferByteArrayOutputStream();
 *     buffer.write(source);
 * 
 * Read data from a BufferByteArrayOutputStream like this:
 * 
 *     buffer.writeTo(destination);
 */
public class BufferByteArrayOutputStream extends ByteArrayOutputStream {

	/**
	 * The ByteBuffer that holds the data of BufferByteArrayOutputStream.
	 * If growth is enabled, this buffer will get switched with another of a different size.
	 */
    protected ByteBuffer buffer;

    /**
     * Whether or not the buffer in this object can grow.
     * True to let this object switch the buffer with a bigger one if necessary.
     * False to disable this feature.
     */
    protected boolean grow;

    /**
     * Make a new BufferByteArrayOutputStream object.
     * Its internal ByteBuffer will be able to grow, and will start out 32 bytes big.
     */
    public BufferByteArrayOutputStream() {

    	// Allocate a new 32 byte ByteBuffer, and setup this object so it can grow
	    this(32);
    }

    /**
     * Make a new BufferByteArrayOutputStream.
     * It will have an internal buffer of the given size.
     * The object will be able to switch that buffer with a larger one if necessary.
     * 
     * If you call a BufferByteArrayOutputSize constructor with 1 parameter, growth will be true.
     * This makes the default for BufferByteArrayOutputStream objects that they can grow.
     * 
     * @param size The ByteBuffer object in this one will start out this size
     */
    public BufferByteArrayOutputStream(int size) {
    	
    	// Allocate a new ByteBuffer of the given size, and setup this object to let it grow
	    this(ByteBuffer.allocate(size), true);
    }

    /**
     * Make a new BufferByteArrayOutputStream object.
     * Start it out with an internal ByteBuffer of the given size.
     * Make that buffer able to grow, or not.
     * 
     * @param size The ByteBuffer object in this one will start out this size
     * @param grow True to let this object change the buffer to let it grow, false to not allow this
     */
    public BufferByteArrayOutputStream(int size, boolean grow) {

    	// Allocate a new ByteBuffer of the given size, and pass through the growth option
        this(ByteBuffer.allocate(size), grow);
    }

    /**
     * Make a new BufferByteArrayOutputStream from the given array.
     * The new stream object won't be able to grow.
     * 
     * @param backing The byte array to wrap into a ByteBuffer object and use in this object.
     */
    public BufferByteArrayOutputStream(byte[] backing) {

    	// Wrap the given byte array into a ByteBuffer, and specify false to disable growing
        this(ByteBuffer.wrap(backing), false);
    }

    /**
     * Make a new BufferByteArrayOutputStream from the given array, position and length.
     * Just wraps the array, position, and length into a ByteBuffer.
     * The new stream object won't be able to grow.
     * 
     * @param backing The byte array to use in this BufferByteArrayOutputStream object
     * @param pos     The index in the array to start at
     * @param length  The length in the array to stop at
     */
    public BufferByteArrayOutputStream(byte[] backing, int pos, int length) {

    	// Wrap the given byte array into a ByteBuffer, and specify false to disable growing
    	this(ByteBuffer.wrap(backing, pos, length), false);
    }
    
    /**
     * Make a new BufferByteArrayOutputStream with by the given ByteBuffer.
     * The stream will not be able to grow.
     * 
     * @param backing The ByteBuffer to back the output stream
     */
    public BufferByteArrayOutputStream(ByteBuffer backing) {
    	
    	// Specify false to disable growing
    	this(backing, false);
    }
    
    /**
     * Make a new BufferByteArrayOutputStream with the given ByteBuffer and growth option.
     * If grow is true, this object will switch the ByteBuffer with a larger one when necessary.
kk     * 
     * @param backing The ByteBuffer to back the output stream
     * @param grow    True to let this object switch the ByteBuffer with a bigger one when necessary
     */
    public BufferByteArrayOutputStream(ByteBuffer backing, boolean grow) {

    	// Save the given buffer and growth option in this new object
    	this.buffer = backing;
        this.grow   = grow;
    }

    /**
     * Does nothing.
     * Closing a ByteArrayOutputStream has no effect, and there is no effect from closing a BufferByteArrayOutputStream either.
     * 
     * Overrides the method from ByteArrayOutputStream.
     */
    public void close() throws IOException {}

    /**
     * Clear the contents of this BufferByteArrayOutputStream.
     * Call this to erase the data and reuse the backing buffer.
     * This doesn't do anything with memory allocation or object creation, and is really fast.
     * 
     * Overrides the method from ByteArrayOutputStream.
     */
    public void reset() {

    	// Make the buffer appear empty again
        buffer.clear(); // Sets position at the start and limit at the end
    }

    /**
     * Find out how much data this BufferByteArrayOutputStream is holding.
     * Overrides the method from ByteArrayOutputStream.
     * 
     * @return The number of bytes this object has stored.
     */
    public int size() {

    	// The number of bytes in the buffer is the distance forward position has moved
    	return buffer.position(); // Each time we wrote to the buffer, the position moved forward
    }

    /**
     * Returns a byte array of the data stored in this BufferByteArrayOutputStream.
     * 
     * The data in the buffer is between the start and the position.
     * The position and limit clip out the free space beyond the data.
     * The byte array this method returns is sized to hold the data perfectly.
     * 
     * This may return a reference to the internal buffer.
     * So, if you want to preserve the contents of the array, don't use this object after you call this method.
     * 
     * Overrides the method from ByteArrayOutputStream.
     * 
     * @return A byte array with the data this BufferByteArrayOutputStream holds.
     */
    public byte[] toByteArray() {

    	// Get information from the buffer
        byte[] arr      = buffer.array();       // This doesn't allocate memory, and just returns a reference to the array that backs the ByteBuffer buffer
        int    offset   = buffer.arrayOffset(); // The offest in the array to the start of the ByteBuffer
        int    position = buffer.position();    // The index in the ByteBuffer where the data starts

        // If the position and limit in the ByteBuffer match the start and end of the array exactly, no need to copy, return a reference to the array
        if (offset == 0 && position == arr.length) return arr;

        // The array is bigger than the data
        byte[] out = new byte[position];                 // Make a new array as big as the data
        System.arraycopy(arr, offset, out, 0, position); // Copy the data into it
        return out;                                      // Return it
    }

    /**
     * Returns the backing buffer.
     * 
     * @return The ByteBuffer this object holds its data in.
     */
    public ByteBuffer buffer() {

    	// Return a reference to the ByteBuffer object inside this BufferByteArrayOutputStream
        return buffer;
    }
    
    /**
     * Writes the current data to the given buffer.
     * If the sink cannot hold all the data stored in this buffer,
     * nothing is written and a BufferOverflowException is thrown.
     * All written bytes are cleared.
     * 
     * @param sink Where this method will put the data.
     */
    public void writeTo(ByteBuffer sink) {
    	
    	// In buffer, position and limit clip out the free space
    	// Flip them so position is 0 and limit is where position was
    	// Now, position and limit clip out the data
        buffer.flip();

        // Move the data from buffer to sink
        sink.put(buffer); // This moves position forward in both buffers

        // Shift the data to the start of the buffer
        buffer.compact(); // This makes position 0 and limit smaller, and moves the data down to the start
    }

    /**
     * Writes the data in this object to the given byte array out.
     * If this object has more data than out has space, this method writes nothing and throws a BufferOverflowException
     * Moves all the data from this object to the given byte array.
     * Once this method has run, the buffer here is empty.
     * 
     * @param out Destination byte array
     */
    public void writeTo(byte[] out) {

    	// Call the next writeTo, letting it write into the whole array
        writeTo(out, 0, out.length);
    }

    /**
     * Writes the data in this object to the given byte array out.
     * Starts at the offset off and won't go as far as len.
     * If this object has more data than out off and len allow, this method writes nothing and throws a BufferOverflowException
     * Moves all the data from this object to the given byte array.
     * Once this method has run, the buffer here is empty.
     * 
     * @param out Destination byte array
     * @param off This method will start writing at this offset in the given array
     * @param len Length of out, this method will make sure it has enough room
     */
    public void writeTo(byte[] out, int off, int len) {

    	// Swith position and limit in the buffer from clipping around space to clipping around data
        buffer.flip(); // Makes position 0 and limit the position

        // Move data from the buffer to the out byte array
        buffer.get(out, off, len); // If there isn't enough room, the get method writes nothing and throws a BufferOverflowException

        // Shift the data to the start of the buffer
        buffer.compact();
    }

    /**
     * Expresses the data in the buffer as a string.
     * Uses the default character encoding to turn bytes into characters.
     * 
     * Overrides the method from ByteArrayOutputStream.
     */
    public String toString() {
    	
        return new String(buffer.array(), buffer.arrayOffset(), buffer.position());
    }
    
    /**
     * Expresses the data in the buffer as a string.
     * Turns bytes into characters according to the specified character encoding.
     * 
     * Overrides the method from ByteArrayOutputStream.
     * 
     * @param encoding The kind of encoding to use.
     */
    public String toString(String encoding) throws UnsupportedEncodingException {

        return new String(buffer.array(), buffer.arrayOffset(), buffer.position(), encoding);
    }
    
    /**
     * Makes the ByteBuffer bigger to hold the given number of additional bytes.
     * Switches the ByteBuffer this object keeps with a bigger one, and copies the data across.
     * 
     * @param len The additional number of bytes you need to store here
     */
    private void grow(int len) {

    	// Get the size of the current buffer
        int size = buffer.capacity();
        
        // Calculate the new size as whicever is bigger
        int newSize = Math.max(
        		size << 1,   // Twice the current size, or
        		size + len); // The current size and the requested additional length

        // Make a new buffer that big
        ByteBuffer newBuffer = buffer.allocate(newSize); // TODO:kfaaborg change to ByteBuffer.allocate(newSize);
        
        // Copy the data from the current buffer to the new one, and switch to the new one
        buffer.flip();         // Flip position and length in buffer to clip around written data, not remaining space
        newBuffer.put(buffer); // Put the contents of buffer into newBuffer
        buffer = newBuffer;    // Point buffer at newBuffer, disconnecting the old one for garbage collection
    }

    /**
     * Takes a byte array with offset and length indices.
     * Writes the data between them into this BufferByteArrayOutputStream object.
     * 
     * If this BufferByteArrayOutputStream is not allowed to grow, it might throw a BufferOverflowException.
     * The method checks the length first and doesn't write anything unless there is enough room.
     * 
     * Overrides the method from ByteArrayOutputStream.
     * 
     * @param b   A byte array to take in
     * @param off The offset in it where the data starts
     * @param len The length from the beginning of the array where the data ends
     */
    public void write(byte[] b, int off, int len) {

    	// If growing is enabled and the new data is bigger than the buffer's free space, switch to a new buffer that's big enough
        if (grow && len > buffer.remaining()) grow(len);

        // Put the data from the given byte array in this objects's buffer
        buffer.put(b, off, len);
    }

    /**
     * Takes a number, like 5.
     * Casts it into a single byte, and appends it to the end of the data stored here.
     * If growing is disabled and the buffer is full, this method will throw a BufferOverflowException.
     * 
     * Overrides the method from ByteArrayOutputStream.
     * 
     * @param b A number that will be cast into a byte and added to the data this object keeps
     */
    public void write(int b) {

    	// If growing is enabled and the buffer is full, switch to a new buffer that's big enough
        if (grow && !buffer.hasRemaining()) grow(1); // We need space for just one more byte

        // Read the given number as a single byte, and add it to the end of the data in this object's buffer
        buffer.put((byte)b);
    }
    
    /**
     * Moves all the data here into the given OutputStream.
     * Marks the buffer this object keeps as empty.
     * 
     * Overrides the method from ByteArrayOutputStream.
     */
    public void writeTo(OutputStream out) throws IOException {

    	// Get the array under this object's ByteBuffer, and write its contents into the given OutputStream
        out.write(buffer.array(), buffer.arrayOffset(), buffer.position());

        // Mark the buffer here as empty
        buffer.clear(); // Sets position at the start and length at the end
    }
}
