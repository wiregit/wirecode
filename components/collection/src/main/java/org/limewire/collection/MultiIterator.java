
package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class MultiIterator<T> implements Iterator<T> {

	protected final Iterator<? extends T> [] iterators;
	protected int current;
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1) {
        this.iterators = new Iterator[] { i1 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
        this.iterators = new Iterator[] { i1, i2 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
        this.iterators = new Iterator[] { i1, i2, i3 };
    }
    
    @SuppressWarnings("unchecked")
    public MultiIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3, Iterator<? extends T> i4) {
        this.iterators = new Iterator[] { i1, i2, i3, i4 };
    }
	
	public MultiIterator(Iterator<? extends T>... iterators) {
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
