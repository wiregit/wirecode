package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;

/**
 * A miniature version of the Collection interface for fixed-size
 * data structures.
 */
public interface FixedSizeCollection {        
    /** 
     * Adds the object x to this, ejecting several other items if needed.
     *
     * @param x the object to add
     * @return the number of older elements removed in the process
     */
    public int addR(Object x);

    /**
     * Removes and returns an element from this.
     *
     * @throws NoSuchElementException this.isEmpty()
     */
    public Object remove() throws NoSuchElementException;

    /** 
     * @see java.util.Collection.size 
     */
    public int size();

    /** 
     * @see java.util.Collection.isEmpty 
     */
    public boolean isEmpty();

}
