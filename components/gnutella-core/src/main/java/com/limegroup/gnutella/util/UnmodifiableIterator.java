package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;

/** An convenience class to aid in writing iterators that cannot be modified. */
public abstract class UnmodifiableIterator implements Iterator {
    /** Throws UnsupportedOperationException */
    public void remove() {
	throw new com.sun.java.util.collections.UnsupportedOperationException();
    }
}
