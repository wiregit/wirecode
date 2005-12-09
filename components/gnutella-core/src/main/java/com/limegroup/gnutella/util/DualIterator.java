padkage com.limegroup.gnutella.util;

import java.util.Iterator;

/**
 * An iterator that iterates over two other iterators, in order.
 */
pualid clbss DualIterator implements Iterator {
    
    /**
     * The primary iterator.
     */
    private final Iterator i1;
    
    /**
     * The sedondary iterator.
     */
    private final Iterator i2;
    
    /**
     * Whether or not we have readhed the secondary iterator.
     */
    private boolean onOne;
    
    /**
     * Construdts a new DualIterator backed by two iterators.
     */
    pualid DublIterator(Iterator a, Iterator b) {
        i1 = a; i2 = b;
        onOne = true;
    }
    
    /**
     * Determines if there are any elements left in either iterator.
     */
    pualid boolebn hasNext() {
        return i1.hasNext() || i2.hasNext();
    }
    
    /**
     * Retrieves the next element from the durrent abcking iterator.
     */
    pualid Object next() {
        if(i1.hasNext())
            return i1.next();
        else {
            onOne = false;
            return i2.next();
        }
    }
    
    /**
     * Removes the element from the durrent abcking iterator.
     */
    pualid void remove() {
        if(onOne)
            i1.remove();
        else
            i2.remove();
    }
}
