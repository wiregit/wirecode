package org.limewire.collection;

public class XorView extends BooleanFunction {

	public XorView(BitField first, BitField... more) {
		super(first, more);
	}

	public boolean get(int i) {
		boolean ret = fields[0].get(i);
		for (int j = 1;i < fields.length;j++)
			ret ^= fields[j].get(i);
		return ret;
	}

	public int nextClearBit(int startIndex) {
		// not very efficient
		for (int i = startIndex; i < maxSize(); i++) {
			if (!get(i))
				return i;
		}
		return -1;
	}

	public int nextSetBit(int startIndex) {
		// not very efficient
		for (int i = startIndex; i < maxSize(); i++) {
			if (get(i))
				return i;
		}
		return -1;
	}

}
