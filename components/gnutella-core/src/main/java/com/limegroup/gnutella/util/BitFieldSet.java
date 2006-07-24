package com.limegroup.gnutella.util;

/**
 * A <tt>BitField</tt> view over a <tt>BitSet</tt> object.
 */
public class BitFieldSet implements BitField {

	private final int maxSize;
	private final BitSet bs;
	
	/**
	 * Constructs a BitField view over the passed bitset with the
	 * specified size. 
	 */
	public BitFieldSet(BitSet bs, int maxSize) {
		this.bs = bs;
		this.maxSize = maxSize;
	}
	
	public int maxSize() {
		return maxSize;
	}

	public int cardinality() {
		return bs.cardinality();
	}

	public boolean get(int i) {
		return bs.get(i);
	}

	public int nextClearBit(int i) {
		return bs.nextClearBit(i);
	}

	public int nextSetBit(int i) {
		return bs.nextSetBit(i);
	}

}
