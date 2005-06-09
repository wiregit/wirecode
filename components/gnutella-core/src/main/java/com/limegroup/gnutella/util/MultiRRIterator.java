
package com.limegroup.gnutella.util;

import java.util.Iterator;


public class MultiRRIterator extends MultiIterator {
	
	public MultiRRIterator(Iterator [] iterators) {
		super(iterators);
		current = iterators.length - 1;
	}
	
	protected void positionCurrent() {
		int steps = 0;
		while (steps <= iterators.length) {
			if (current == iterators.length-1)
				current = -1;
			if (iterators[++current].hasNext())
				break;
			steps++;
		}
	}
}
