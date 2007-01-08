package org.limewire.collection;

import java.util.Iterator;

/** An convenience class to aid in writing iterators that cannot be modified. */
public abstract class UnmodifiableIterator<E> implements Iterator<E> {
    /** Throws UnsupportedOperationException */
    public final void remove() {
		throw new UnsupportedOperationException();
    }
}
