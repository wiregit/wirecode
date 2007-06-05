package org.limewire.collection;

/**
 * Defines the interface to manipulate a fixed size field of bits. 
 * <code>BitField</code> declares methods to return the location where a bit
 * is either set (equal to 1) or clear (equal to 0). The <code>BitField</code>
 * interface has a methods for returning the bit value at a particular location
 * and the maximum size of the field of bits.
 * 
 * Also, <code>BitField</code> has a <a href="http://en.wikipedia.org/wiki/Cardinality">
 * cardinality</a> method for working with sets.
 * 
 */
public interface BitField {
	public boolean get(int i);
	public int nextSetBit(int i);
	public int nextClearBit(int i);
	public int cardinality();
	public int maxSize();
}
