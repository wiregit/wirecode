package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.SortedSet;

/**
 * A BitField view over a collection of integers.
 *
 * Its up to the user to ensure that no element larger than the
 * max size is put in the collection.  Should that happen, the
 * cardinality() method will not return meaningful values.
 * 
 * The class performs best when the collection is a SortedSet.
 */
public class CollectionBitField implements BitField {

	private final Collection<Integer> collection;
	private final int maxSize;
	
	public CollectionBitField(Collection<Integer> collection, int maxSize) {
		this.collection = collection;
		this.maxSize = maxSize;
	}
	
	/**
	 * Warning: this cannot guarantee the return value 
	 * will be smaller than the maxSize() 
	 */
	public int cardinality() {
		return collection.size();
	}

	public boolean get(int i) {
		if (i > maxSize)
			throw new IndexOutOfBoundsException();
		return collection.contains(i);
	}

	public int maxSize() {
		return maxSize;
	}

	public int nextClearBit(int startIndex) {
		for (int i = startIndex; i < maxSize; i++) {
			if (!get(i))
				return i;
		}
		return -1;
	}

	public int nextSetBit(int startIndex) {
		if (get(startIndex))
			return startIndex;
		
		if (collection instanceof SortedSet) {
			// cool, do it fast
			SortedSet<Integer> set = (SortedSet<Integer>)collection;
			set = set.tailSet(startIndex);
			if (set.isEmpty())
				return -1;
			int ret = set.first();
			if (ret >= maxSize)
				return - 1;
			return ret;
		}
		
		// not a sorted set, do it slow :(
		long smallest = Long.MAX_VALUE;
		for (int i : collection) {
			if (i > startIndex && i < smallest && i < maxSize)
				smallest = i;
		}
		return smallest == Long.MAX_VALUE ? - 1 : (int)smallest;
	}

}
