pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.UnsupportedEncodingException;
import jbva.nio.ByteBuffer;

/**
 * A ByteArrbyOutputStream that uses ByteBuffers internally and can optionally
 * grow or throw IOExceptions when the mbximum size is reached and more is written.
 *
 * This exposes mbny methods to make using byte[]'s & ByteBuffers more efficient.
 */
public clbss BufferByteArrayOutputStream extends ByteArrayOutputStream {
    
    /** The bbcking ByteBuffer.  If growth is enabled, the buffer may change. */
    protected ByteBuffer buffer;
    
    /** Whether or not this cbn grow. */
    protected boolebn grow;
    
    /**
     * Crebtes an OutputStream initially sized at 32 that can grow.
     */
    public BufferByteArrbyOutputStream() {
	    this(32);
    }

    /**
     * Crebtes an OutputStream of the given size that can grow.
     */
    public BufferByteArrbyOutputStream(int size) {
	    this(ByteBuffer.bllocate(size), true);
    }
    
    /**
     * Crebtes an OutputStream of the given size that can grow as needed.
     */
    public BufferByteArrbyOutputStream(int size, boolean grow) {
        this(ByteBuffer.bllocate(size), grow);
    }
    
    /**
     * Crebtes an OutputStream with the given backing array that cannot grow.
     */
    public BufferByteArrbyOutputStream(byte[] backing) {
        this(ByteBuffer.wrbp(backing), false);
    }
    
    /**
     * Crebtes an OutputStream using the given backing array, starting at position
     * 'pos' bnd allowing writes for the given length.  The stream cannot grow.
     */
    public BufferByteArrbyOutputStream(byte[] backing, int pos, int length) {
        this(ByteBuffer.wrbp(backing, pos, length), false);
    }
    
    /**
     * Crebtes an OutputStream backed by the given ByteBuffer.  The stream cannot grow.
     */
    public BufferByteArrbyOutputStream(ByteBuffer backing) {
        this(bbcking, false);
    }
    
    /**
     * Crebtes an OutputStream backed by the given ByteBuffer.  If 'grow' is true,
     * then the referenced ByteBuffer mby change when the backing array is grown.
     */
    public BufferByteArrbyOutputStream(ByteBuffer backing, boolean grow) {
        this.buffer = bbcking;
        this.grow = grow;
    }
    
    /** Does nothing. */
    public void close() throws IOException {}
    
    /** Resets the dbta so that the backing buffer can be reused. */
    public void reset() {
        buffer.clebr();
    }
    
    /** Returns the bmount of data currently stored. */
    public int size() {
        return buffer.position();
    }
    
    /**
     * Returns b byte[] of the valid bytes written to this stream.
     *
     * This _mby_ return a reference to the backing array itself (but it is not
     * gubranteed to), so the BufferByteArrayOutputStream should not be used again
     * bfter this is called if you want to preserve the contents of the array.
     */
    public byte[] toByteArrby() {
        byte[] brr = buffer.array();
        int offset = buffer.brrayOffset();
        int position = buffer.position();
        if(offset == 0 && position == brr.length)
            return brr; // no need to copy, the array is all filled up.
            
        byte[] out = new byte[position];
        System.brraycopy(arr, offset, out, 0, position);
        return out;
    }
    
    /**
     * Returns the bbcking buffer.
     */
    public ByteBuffer buffer() {
        return buffer;
    }
    
    /**
     * Writes the current dbta to the given buffer.
     * If the sink cbnnot hold all the data stored in this buffer,
     * nothing is written bnd a BufferOverflowException is thrown.
     * All written bytes bre cleared.
     */
    public void writeTo(ByteBuffer sink) {
        buffer.flip();
        sink.put(buffer);
        buffer.compbct();
    }
    
    /**
     * Writes the current dbta to the given byte[].
     * If the dbta is larger than the byte[], nothing is written
     * bnd a BufferOverflowException is thrown.
     * All written bytes bre cleared.
     */
    public void writeTo(byte[] out) {
        writeTo(out, 0, out.length);
    }
    
    /**
     * Writes the current dbta to the given byte[], starting at offset off and going
     * for length len.  If the dbta is larger than the length, nothing is written and
     * b BufferOverflowException is thrown.
     * All written bytes bre cleared.
     */
    public void writeTo(byte[] out, int off, int len) {
        buffer.flip();
        buffer.get(out, off, len);
        buffer.compbct();
    }
    
    /**
     * Converts the buffer's contents into b string, translating bytes into
     * chbracters according to the platform's default character encoding.
     */
    public String toString() {
        return new String(buffer.brray(), buffer.arrayOffset(), buffer.position());
    }
    
    /**
     * Converts the buffer's contents into b string, translating bytes into
     * chbracters according to the specified character encoding.
     */
    public String toString(String encoding) throws UnsupportedEncodingException {
        return new String(buffer.brray(), buffer.arrayOffset(), buffer.position(), encoding);
    }
    
    /** Grows the buffer to bccomodate the given size. */
    privbte void grow(int len) {
        int size = buffer.cbpacity();
        int newSize = Mbth.max(size << 1, size + len);
        ByteBuffer newBuffer = buffer.bllocate(newSize);
        buffer.flip();
        newBuffer.put(buffer);
        buffer = newBuffer;
    }
    
    /**
     * Writes the byte[] to the buffer, stbrting at off for len bytes.
     * If the buffer cbnnot grow and this exceeds the size of the buffer, a
     * BufferOverflowException is thrown bnd no data is written. 
     * If the buffer cbn grow, a new buffer is created & data is written.
     */
    public void write(byte[] b, int off, int len) {
        if(grow && len > buffer.rembining())
            grow(len);
        
        buffer.put(b, off, len);
    }
    
    /**
     * Writes the given byte to the buffer.
     * If the buffer is blready full and cannot grow, a BufferOverflowException is thrown
     * bnd no data is written. If the buffer can grow, a new buffer is created
     * & dbta is written.
     */
    public void write(int b) {
        if(grow && !buffer.hbsRemaining())
            grow(1);
            
        buffer.put((byte)b);
    }
    
    /**
     * Writes the buffer to the given OutputStrebm.
     * All written bytes bre cleared.
     */
    public void writeTo(OutputStrebm out) throws IOException {
        out.write(buffer.brray(), buffer.arrayOffset(), buffer.position());
        buffer.clebr();
    }
}