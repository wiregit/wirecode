
// Edited for the Learning branch

package com.limegroup.gnutella.udpconnect;

/**
 * A container for a chunk of byte information.
 * 
 * 
 * DataMessage.getData1Chunk() and getData2Chunk() make Chunk objects.
 * UDPBufferedOutputStream
 * 
 */
public class Chunk {

    /**
     * A byte array that holds the data.
     */
	public byte[] data;
    
    /**
     * The index in the data byte array where the data starts.
     */
    public int    start;
    
    /**
     * 
     */
	public int    length;

    /**
     * Express this Chunk object as text.
     * Composes text like " dl: 100 start:0 len:100".
     * 
     * @return A String
     */
	public String toString() {

	    return " dl: " + data.length + " start:" + start + " len:" + length;
	}
}
