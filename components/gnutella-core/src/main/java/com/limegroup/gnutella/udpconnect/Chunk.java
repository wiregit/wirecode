package com.limegroup.gnutella.udpconnect;

/**
 *  A container for a chunk of byte information.
 */
public class Chunk {
	public byte[] data;
    public int    start;
	public int    length;

	public String toString() {
	    return " dl: "+data.length+" start:"+start+" len:"+length;
	}
}
