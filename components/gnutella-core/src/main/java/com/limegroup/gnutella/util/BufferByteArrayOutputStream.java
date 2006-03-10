package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * A ByteArrayOutputStream that uses ByteBuffers internally and can optionally
 * grow or throw IOExceptions when the maximum size is reached and more is written.
 *
 * This exposes many methods to make using byte[]'s & ByteBuffers more efficient.
 */
public class BufferByteArrayOutputStream extends ByteArrayOutputStream {
    
    /** The backing ByteBuffer.  If growth is enabled, the buffer may change. */
    protected ByteBuffer buffer;
    
    /** Whether or not this can grow. */
    protected boolean grow;
    
    /**
     * Creates an OutputStream initially sized at 32 that can grow.
     */
    public BufferByteArrayOutputStream() {
	    this(32);
    }

    /**
     * Creates an OutputStream of the given size that can grow.
     */
    public BufferByteArrayOutputStream(int size) {
	    this(ByteBuffer.allocate(size), true);
    }
    
    /**
     * Creates an OutputStream of the given size that can grow as needed.
     */
    public BufferByteArrayOutputStream(int size, boolean grow) {
        this(ByteBuffer.allocate(size), grow);
    }
    
    /**
     * Creates an OutputStream with the given backing array that cannot grow.
     */
    public BufferByteArrayOutputStream(byte[] backing) {
        this(ByteBuffer.wrap(backing), false);
    }
    
    /**
     * Creates an OutputStream using the given backing array, starting at position
     * 'pos' and allowing writes for the given length.  The stream cannot grow.
     */
    public BufferByteArrayOutputStream(byte[] backing, int pos, int length) {
        this(ByteBuffer.wrap(backing, pos, length), false);
    }
    
    /**
     * Creates an OutputStream backed by the given ByteBuffer.  The stream cannot grow.
     */
    public BufferByteArrayOutputStream(ByteBuffer backing) {
        this(backing, false);
    }
    
    /**
     * Creates an OutputStream backed by the given ByteBuffer.  If 'grow' is true,
     * then the referenced ByteBuffer may change when the backing array is grown.
     */
    public BufferByteArrayOutputStream(ByteBuffer backing, boolean grow) {
        this.buffer = backing;
        this.grow = grow;
    }
    
    /** Does nothing. */
    public void close() throws IOException {}
    
    /** Resets the data so that the backing buffer can be reused. */
    public void reset() {
        buffer.clear();
    }
    
    /** Returns the amount of data currently stored. */
    public int size() {
        return buffer.position();
    }
    
    /**
     * Returns a byte[] of the valid bytes written to this stream.
     *
     * This _may_ return a reference to the backing array itself (but it is not
     * guaranteed to), so the BufferByteArrayOutputStream should not be used again
     * after this is called if you want to preserve the contents of the array.
     */
    public byte[] toByteArray() {
        byte[] arr = buffer.array();
        int offset = buffer.arrayOffset();
        int position = buffer.position();
        if(offset == 0 && position == arr.length)
            return arr; // no need to copy, the array is all filled up.
            
        byte[] out = new byte[position];
        System.arraycopy(arr, offset, out, 0, position);
        return out;
    }
    
    /**
     * Returns the backing buffer.
     */
    public ByteBuffer buffer() {
        return buffer;
    }
    
    /**
     * Writes the current data to the given buffer.
     * If the sink cannot hold all the data stored in this buffer,
     * nothing is written and a BufferOverflowException is thrown.
     * All written bytes are cleared.
     */
    public void writeTo(ByteBuffer sink) {
        buffer.flip();
        sink.put(buffer);
        buffer.compact();
    }
    
    /**
     * Writes the current data to the given byte[].
     * If the data is larger than the byte[], nothing is written
     * and a BufferOverflowException is thrown.
     * All written bytes are cleared.
     */
    public void writeTo(byte[] out) {
        writeTo(out, 0, out.length);
    }
    
    /**
     * Writes the current data to the given byte[], starting at offset off and going
     * for length len.  If the data is larger than the length, nothing is written and
     * a BufferOverflowException is thrown.
     * All written bytes are cleared.
     */
    public void writeTo(byte[] out, int off, int len) {
        buffer.flip();
        buffer.get(out, off, len);
        buffer.compact();
    }
    
    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the platform's default character encoding.
     */
    public String toString() {
        return new String(buffer.array(), buffer.arrayOffset(), buffer.position());
    }
    
    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the specified character encoding.
     */
    public String toString(String encoding) throws UnsupportedEncodingException {
        return new String(buffer.array(), buffer.arrayOffset(), buffer.position(), encoding);
    }
    
    /** Grows the buffer to accomodate the given size. */
    private void grow(int len) {
        int size = buffer.capacity();
        int newSize = Math.max(size << 1, size + len);
        ByteBuffer newBuffer = buffer.allocate(newSize);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }
    
    /**
     * Writes the byte[] to the buffer, starting at off for len bytes.
     * If the buffer cannot grow and this exceeds the size of the buffer, a
     * BufferOverflowException is thrown and no data is written. 
     * If the buffer can grow, a new buffer is created & data is written.
     */
    public void write(byte[] b, int off, int len) {
        if(grow && len > buffer.remaining())
            grow(len);
        
        buffer.put(b, off, len);
    }
    
    /**
     * Writes the given byte to the buffer.
     * If the buffer is already full and cannot grow, a BufferOverflowException is thrown
     * and no data is written. If the buffer can grow, a new buffer is created
     * & data is written.
     */
    public void write(int b) {
        if(grow && !buffer.hasRemaining())
            grow(1);
            
        buffer.put((byte)b);
    }
    
    /**
     * Writes the buffer to the given OutputStream.
     * All written bytes are cleared.
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(buffer.array(), buffer.arrayOffset(), buffer.position());
        buffer.clear();
    }
}