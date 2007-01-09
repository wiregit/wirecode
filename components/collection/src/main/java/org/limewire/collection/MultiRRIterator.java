
package org.limewire.collection;

import java.util.Iterator;


public class MultiRRIterator<T> extends MultiIterator<T> {
	
    public MultiRRIterator(Iterator<? extends T> i1) {
        super(i1);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
        super(i1, i2);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
        super(i1, i2, i3);
        current = iterators.length - 1;
    }
    
    public MultiRRIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3, Iterator<? extends T> i4) {
        super(i1, i2, i3, i4);
        current = iterators.length - 1;
    }
    
	public MultiRRIterator(Iterator<? extends T> ... iterators ) {
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
