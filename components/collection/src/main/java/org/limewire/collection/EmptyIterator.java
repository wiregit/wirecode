package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * Provides an unmodifiable empty iterator. <code>EmptyIterator</code> always
 * returns that there aren't any more items and throws a 
 * {@link NoSuchElementException} when attempting to move to the next item.
 * 
 <pre>
    try{
        EmptyIterator ei = new EmptyIterator();     
        ei.next();      
    } catch (Exception e) {
        System.out.println("Expected to get NoSuchElementException exception: " + e.toString());
    }

    Output:
        Expected to get NoSuchElementException exception: java.util.NoSuchElementException
 </pre>
 */
public class EmptyIterator extends UnmodifiableIterator {
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