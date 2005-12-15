
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Not used now that LimeWire has switched to NIO.
 * Make an UncompressingInputStream with a source of compressed data, then call read(b) on it to have it write decompressed data into b.
 * 
 * Pass the constructor an InputStream the new object will read compressed data from.
 * Then, call read(destination) to have the object read data from its source, decompress it, and write it into the buffer you give it.
 * 
 * UncompressingInputStream extends Java's InflaterInputStream.
 * It catches exceptions on the read method, and repackages them as IOException objects.
 * If the stream is closed, a native inflateBytes call can cause a NullPointerException.
 * 
 * Java offers DeflaterOutputStream and InflaterInputStream classes.
 * The program extends both of these, naming them CompressingOutputStream and UncompressingInputStream.
 */
public final class UncompressingInputStream extends InflaterInputStream {

	/**
	 * Make a new UncompressingInputStream object, which works just like an InflaterInputStream to decompress the data you read from it.
	 * 
	 * @param stream   The stream this object will read from
	 * @param inflater A Java Inflater object that can actually decompress data
	 */
    public UncompressingInputStream(final InputStream stream, final Inflater inflater) {

    	// Call the InflaterInputStream constructor
        super(stream, inflater);
    }

    /**
     * Read compressed data from the InputStream source, decompress it, and write it into the given byte array b.
     * 
     * UncompressingInputStream extends Java's InflaterInputStream.
     * This read method overrides InflaterInputStream.read(b, off, len).
     * This read method calls that one, and then catches and repackages exceptions it might throw us.
     * 
     * @param b   The destination byte array where this object will put the decompressed data
     * @param off The index in b where this object can start writing
     * @param len The amount of space it has there
     * @return    The number of bytes it wrote
     */
    public int read(byte[] b, int off, int len) throws IOException {

        try {

        	// Try calling InflaterInputStream's read method
            return super.read(b, off, len);

        } catch (NullPointerException e) {

        	/*
        	 * This will happen if 'end' was called on the inflate while we were inflating.
        	 */

        	// Turn it into an IOException
            throw new IOException("inflater was ended");
            
        } catch (ArrayIndexOutOfBoundsException e2) {
        	
        	/*
        	 * This will happen occasionally on Windows machines when the underlying socket was closed/disconnected while the read reached the native socketRead.
        	 */

        	// Wrap it in an IOException
            throw new IOException(e2.getMessage());
            
        } catch (OutOfMemoryError e3) {

        	// Wrap it in an IOException
            throw new IOException(e3.getMessage());
        }
    }
}
