/**
 * Code taken freely from
 * http://www.java-engineer.com/java/auto-complete.html
 */

//------------------------------------------------------------------------------
// Copyright (c) 1999-2001 Matt Welsh.  All Rights Reserved.
//------------------------------------------------------------------------------

package com.limegroup.gnutella.util;

import java.util.Iterator;

/**
 * This interface defines the API that dictionaries for autocomplete components
 * must implement. Note that implementations of this interface should perform
 * look ups as quickly as possible to avoid delays as the user types.
 *
 * @author Matt Welsh (matt@matt-welsh.com)
 *
 * @modified David Soh (yunharla00@hotmail.com)
 *      added getIterator() & getIterator(String) for enhanced AutoCompleteTextField use.
 *
 */
public interface AutoCompleteDictionary extends Iterable<String> {
    /**
     * Adds an entry to the dictionary.
     *
     * @param s The string to add to the dictionary.
     */
    public void addEntry(String s);

    /**
     * Removes an entry from the dictionary.
     *
     * @param s The string to remove to the dictionary.
     * @return True if successful, false if the string is not contained or cannot
     *         be removed.
     */
    public boolean removeEntry(String s);

    /**
     * Perform a lookup and returns the closest matching string to the passed
     * string.
     *
     * @param s The string to use as the base for the lookup. How this routine
     *          is implemented determines the behaviour of the component.
     *          Typically, the closest matching string that completely contains
     *          the given string is returned.
     */
    public String lookup(String s);

    /**
     * Returns all available entries in dictionary
     *
     */
    public Iterator<String> iterator();

    /**
     * Returns an iterator of potential matches from the given string.
     *
     */
    public Iterator<String> iterator(String s);
    
    /**
     * Clears the dictionary.
     */
    public void clear();
}
