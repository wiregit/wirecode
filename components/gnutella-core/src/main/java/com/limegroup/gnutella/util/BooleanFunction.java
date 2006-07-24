package com.limegroup.gnutella.util;

/**
 * A view over two or more <tt>BitField</tt> instances that represents a boolean function.
 * This class itself is a BitField so several may be chained
 * to form complex functions.
 */
abstract class BooleanFunction implements BitField {

	protected final BitField [] fields;
	
	protected BooleanFunction(BitField first, BitField... more) {
		this.fields = new BitField[more.length + 1];
		fields[0] = first;
		System.arraycopy(more,0,fields,1, more.length);
		
		int maxSize = fields[0].maxSize();
		for (BitField bf : fields) {
			if (bf.maxSize() != maxSize)
				throw new IllegalArgumentException("bitfield "+bf+" doesn't have size "+maxSize);
		}
	}
	
	public int maxSize() {
		return fields[0].maxSize();
	}
	
	public int cardinality() {
		int ret = 0;
		for (int i = 0; i < maxSize(); i++)
			ret += get(i) ? 1 : 0;
		return ret;
	}
}
