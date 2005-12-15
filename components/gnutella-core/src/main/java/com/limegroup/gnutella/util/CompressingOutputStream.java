
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Not used now that LimeWire has switched to NIO.
 * Make an CompressingOutputStream with a destination for compressed data, then call write(b) on it to have it take and compress b.
 * 
 * Pass the constructor an OutputStream the new object will write compressed data to.
 * Then, call write(source) to give the object some data it will compress and write to its output stream.
 * 
 * CompressingOutputStream wraps Java's DeflaterOutputStream class.
 * It changes two things:
 * (1) deflate() runs the deflater multiple times to make sure no data is left in DeflaterOutputStream's input buffer.
 * (2) flush() cycles the compression level to get the DeflaterOutputStream's Deflater object to release any data it's holding.
 * 
 * Java offers DeflaterOutputStream and InflaterInputStream classes.
 * The program extends both of these, naming them CompressingOutputStream and UncompressingInputStream.
 */
public final class CompressingOutputStream extends DeflaterOutputStream {

	/**
	 * Make a new CompressingOutputStream object.
	 * Passes the given arguments to the DeflaterOutputStream constructor.
	 * 
	 * @param out      The stream this object will write compressed data to
	 * @param deflater The Java Deflater object that can actually compress data
	 */
    public CompressingOutputStream(final OutputStream out, final Deflater deflater) {

    	// Call the DeflaterOutputStream constructor
    	super(out, deflater);
    }

    /**
     * Compresses data you wrote to this object, and writes it into this object's output stream.
     * 
     * Here's how this method gets called:
     * You have a CompressingOutputStream object, named c.
     * When you made it, you gave it an OutputStream that it will write to.
     * You call c.write(b), giving it a buffer of data to compress and write.
     * There is no write() method in this class, so control goes to DeflaterOutputStream's write method inside the Java platform.
     * That write() method copies b into an internal buffer, and points the Deflater object at it.
     * Then, it calls deflate().
     * CompressingOutputStream overrides DeflaterOutputStream's deflate() method with this one here.
     * This is how control gets here.
     * This method does the same thing that DeflaterOutputStream's deflate() method would, but fixing an important issue.
     * We extended DeflaterOutputStream to change the behavior of the deflate() method, and fix a problem in Java.<p>
     * 
     * We can't see the source code to DeflaterOutputStream's write(b) method, because it's inside the Java platform.
     * It must take the given data b, keep it in an internal buffer, and point the Deflater object at it.
     * When control gets here, the deflater already knows where to get data to compress.
     * The call def.deflate takes data from that location, and writes it into buf, the destination buffer.<p>
     * 
     * This object has two buffers inside it: one for incoming data that hasn't been compressed yet, and buf for compressed data.
     * This object also has out, the OutputStream that it writes to.
     * A call to deflate() compresses data from the incoming buffer to buf, and then writes it to out.
     * The mistake that DeflaterOutputStream's deflate() method makes, and that we correct here, deals with this process.
     * DeflaterOutputStream's deflate() method only does it one time.
     * If there's a lot of data in the incoming buffer that doesn't compress well, a single compressing operation will fill buf.
     * The deflate() method will then move the content of buf into out, and return.
     * But, there's still more data to compress in the incoming buffer.
     * So, this fixed version of deflate() repeats the process in a loop, only stopping when the incoming buffer is empty.
     * That way, data won't get stuck in this object.<p>
     */
    protected void deflate() throws IOException {

        try {

        	// The number of bytes of compressed data the Deflater object produced
            int deflated;

            // Loop to compress data
            while (true) {

            	/*
            	 * The call below runs the deflater, having it write compressed bytes into buf.
            	 * It doesn't show where def is getting its data from.
            	 * DeflaterOutputStream's write(b) method must have already pointed def at the input buffer.
            	 */

            	// Use the Deflater object stored in the DeflaterOutputStream to compress data
            	deflated = def.deflate( // Returns the number of bytes of compressed data it wrote
            			buf,            // Destination buffer where the deflater will write compressed data
            			0,              // Start writing at the beginning of buf
            			buf.length);    // The amount of free space in the buffer, buf is empty

            	/*
            	 * The deflater returns the number of bytes of compressed data it wrote to buf.
            	 * If it wrote 0, that means it's probably run out of data to compress.
            	 * This means it has emptied the input buffer, and we don't have to worry about data getting left there anymore.
            	 */

            	// The deflater ran out of data to compress, meaning the input buffer is empty and we are done.
            	if (deflated <= 0) break; // Leave the infinite while loop

            	// Move the contents of buf into the OutputStream out
            	out.write(buf, 0, deflated);

            	/*
            	 * Now buf is empty again.
            	 * But, there still might be some data in the input buffer.
            	 * Loop back to the top to call def.deflate again and see if it can get any more data from there and compress it.
            	 */
            }

        // This will happen if end is called on the Deflater while we are deflating
        } catch (NullPointerException e) {

        	// Throw an IOException instead
            throw new IOException("deflater was ended");
        }
    }

    /** An empty byte array used to flush the deflater. */
    private static final byte [] EMPTYBYTEARRAY = new byte [0];

    /**
     * Make the deflater write out any data it is holding.
     * 
     * The Deflater object takes in data, and puts out compressed data.
     * It might be holding on to some data, waiting for us to give it some more before it compresses all of it and gives it to us.
     * To make it give us everything it's got right now, call this flush() method.
     */
    public void flush() throws IOException {

    	// If the deflater has filled buf, leave now
        if (def.finished()) return;

        /*
         * We need to force the Deflater to flush its data.
         * We can trick it into doing this by telling it to stop compressing, and then start compressing again.
         * This simulates zlib's Z_PARTIAL_FLUSH and Z_SYNC_FLUSH behavior.
         * 
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4255743
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4206909
         */

        // Turn off compression, call deflate, and then turn it back on again
        def.setInput(EMPTYBYTEARRAY, 0, 0);         // Point the deflater at the empty byte array
        def.setLevel(Deflater.NO_COMPRESSION);      // Set it to process data with no compression at all
        deflate();                                  // Call the deflate method above, which will get the deflater to write its data into buf and then out
        def.setLevel(Deflater.DEFAULT_COMPRESSION); // Back here, set the deflater back to the default level of compression
        deflate();                                  // Call the deflate method above again (do)
        super.flush();                              // Call DeflaterOutputStream's flush method (do)

        // What points the deflater back at the internal buffer of data to compress? (do)
    }
}
