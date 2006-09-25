package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Yields nothing. (internal)
 */
class EmptyIterator extends UnmodifiableIterator {
    /** A constant EmptyIterator. */
    public final static Iterator EMPTY_ITERATOR = new EmptyIterator();

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> emptyIterator() {
        return EMPTY_ITERATOR;
    }

    // inherits javadoc comment
    public boolean hasNext() {
        return false;
    }

    // inherits javadoc comment
    public Object next() {
        throw new NoSuchElementException();
    }
}