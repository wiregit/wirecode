package org.limewire.collection;

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
		if (bs.length() <= maxSize)
			return bs.cardinality();
		else
			return bs.get(0, maxSize).cardinality(); // expensive, avoid.
	}

	public boolean get(int i) {
		if (i > maxSize)
			throw new IndexOutOfBoundsException();
		return bs.get(i);
	}

	public int nextClearBit(int i) {
		int ret = bs.nextClearBit(i); 
		return ret >= maxSize ? -1 : ret;
	}

	public int nextSetBit(int i) {
		int ret = bs.nextSetBit(i);
		return ret >= maxSize ? -1 : ret;
	}

}
