package com.limegroup.gnutella;

import java.util.*;

/** An adaptor class to prevent an iterator from being modified. */
public class UnmodifiableIterator implements Iterator {
    private Iterator real;

    public UnmodifiableIterator(Iterator real) {
	this.real=real;
    }

    public boolean hasNext() {
	return real.hasNext();
    }

    public Object next() throws NoSuchElementException {
	return real.next();
    }

    /** Throws UnsupportedOperationException */
    public void remove() {
	throw new UnsupportedOperationException();
    }
}
