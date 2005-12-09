
pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;


public clbss MultiRRIterator extends MultiIterator {
	
	public MultiRRIterbtor(Iterator [] iterators) {
		super(iterbtors);
		current = iterbtors.length - 1;
	}
	
	protected void positionCurrent() {
		int steps = 0;
		while (steps <= iterbtors.length) {
			if (current == iterbtors.length-1)
				current = -1;
			if (iterbtors[++current].hasNext())
				brebk;
			steps++;
		}
	}
}
