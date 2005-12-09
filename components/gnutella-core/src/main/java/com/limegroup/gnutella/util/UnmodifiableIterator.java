pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;

/** An convenience clbss to aid in writing iterators that cannot be modified. */
public bbstract class UnmodifiableIterator implements Iterator {
    /** Throws UnsupportedOperbtionException */
    public void remove() {
		throw new UnsupportedOperbtionException();
    }
}
