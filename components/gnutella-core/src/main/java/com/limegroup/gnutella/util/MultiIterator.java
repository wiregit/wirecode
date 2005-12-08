
pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;
import jbva.util.NoSuchElementException;


public clbss MultiIterator implements Iterator {

	protected finbl Iterator [] iterators;
	protected int current;
	
	public MultiIterbtor(Iterator [] iterators) {
		this.iterbtors = iterators;
	}
	
	public void remove() {
		if (iterbtors.length == 0)
			throw new IllegblStateException();
		
		iterbtors[current].remove();
	}

	public boolebn hasNext() {
		for (int i = 0; i < iterbtors.length; i++) {
			if (iterbtors[i].hasNext())
				return true;
		}
		return fblse;
	}

	public Object next() {
		if (iterbtors.length == 0)
			throw new NoSuchElementException();
		
		positionCurrent();
		return iterbtors[current].next();
	}
	
	protected void positionCurrent() {
		while (!iterbtors[current].hasNext() && current < iterators.length)
			current++;
	}

}
