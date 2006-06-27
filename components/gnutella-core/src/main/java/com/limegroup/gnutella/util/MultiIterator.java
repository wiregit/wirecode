
package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class MultiIterator<T> implements Iterator<T> {

	protected final Iterator<T> [] iterators;
	protected int current;
	
	public MultiIterator(Iterator<T> [] iterators) {
		this.iterators = iterators;
	}
	
	public void remove() {
		if (iterators.length == 0)
			throw new IllegalStateException();
		
		iterators[current].remove();
	}

	public boolean hasNext() {
		for (int i = 0; i < iterators.length; i++) {
			if (iterators[i].hasNext())
				return true;
		}
		return false;
	}

	public T next() {
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
