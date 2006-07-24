package com.limegroup.gnutella.util;

public class AndView extends BooleanFunction {

	public AndView(BitField first, BitField... more) {
		super(first, more);
	}

	public boolean get(int i) {
		for (BitField bf : fields) {
			if (!bf.get(i))
				return false;
		}
		return true;
	}

	public int nextClearBit(int startIndex) {
		long smallest = Long.MAX_VALUE;
		for (int i = 0; i < fields.length; i++) {
			int current = fields[i].nextClearBit(startIndex);
			if (current == -1)
				continue;
			smallest = Math.min(current, smallest);
		}
		return smallest == Long.MAX_VALUE ? -1 : (int)smallest;
	}

	public int nextSetBit(int startIndex) {
		int currentIndex = startIndex;
		while(currentIndex < maxSize()) {
			boolean allSame = true;
			int largest = -1;
			int current = -1;
			for (int i = 0; i < fields.length; i++) {
				current = fields[i].nextSetBit(currentIndex);
				if (current == -1)
					return -1; // shortcut
				if (i == 0)
					largest = current;
				else if (current != largest) {
					allSame = false;
					largest = Math.max(largest,current);
				}
			}
			if (allSame)
				return largest;
			currentIndex = largest;
		}
		return -1;
	}

}
