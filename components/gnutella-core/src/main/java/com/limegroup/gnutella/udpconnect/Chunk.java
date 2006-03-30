
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

/**
 * A Chunk object keeps a byte array with start and length indices that clip out a part of it you can read.
 * 
 * A Chunk looks like this:
 * 
 * chunk.data    [array - - datadatadatadatadata - - - - - - - ]
 * chunk.start   ---------->
 * chunk.length             ------------------->
 * 
 * chunk.data is the byte array.
 * Read chunk.length bytes from chunk.start.
 */
public class Chunk {

    /** The byte array with the data in the middle. */
	public byte[] data;

    /** The index in the data byte array where the data starts. */
    public int start;

    /** The number of bytes of data. */
	public int length;

    /**
     * Express this Chunk object as text.
     * Composes text like " dl: 100 start:0 len:100".
     * 
     * @return A String
     */
	public String toString() {

        // Compose and return the String
	    return " dl: " + data.length + " start:" + start + " len:" + length;
	}
}
