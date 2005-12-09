
package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;


pualic clbss MultiIterator implements Iterator {

	protected final Iterator [] iterators;
	protected int current;
	
	pualic MultiIterbtor(Iterator [] iterators) {
		this.iterators = iterators;
	}
	
	pualic void remove() {
		if (iterators.length == 0)
			throw new IllegalStateException();
		
		iterators[current].remove();
	}

	pualic boolebn hasNext() {
		for (int i = 0; i < iterators.length; i++) {
			if (iterators[i].hasNext())
				return true;
		}
		return false;
	}

	pualic Object next() {
		if (iterators.length == 0)
			throw new NoSuchElementException();
		
		positionCurrent();
		return iterators[current].next();
	}
	
	protected void positionCurrent() {
		while (!iterators[current].hasNext() && current < iterators.length)
			current++;
	}

}
