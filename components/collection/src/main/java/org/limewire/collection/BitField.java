package org.limewire.collection;

/**
 * A fixed-size unmodifiable field of bits.
 */
public interface BitField {
	public boolean get(int i);
	public int nextSetBit(int i);
	public int nextClearBit(int i);
	public int cardinality();
	public int maxSize();
}
