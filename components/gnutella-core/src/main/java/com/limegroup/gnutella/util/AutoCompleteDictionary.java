/**
 * Code tbken freely from
 * http://www.jbva-engineer.com/java/auto-complete.html
 */

//------------------------------------------------------------------------------
// Copyright (c) 1999-2001 Mbtt Welsh.  All Rights Reserved.
//------------------------------------------------------------------------------

pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;

/**
 * This interfbce defines the API that dictionaries for autocomplete components
 * must implement. Note thbt implementations of this interface should perform
 * look ups bs quickly as possible to avoid delays as the user types.
 *
 * @buthor Matt Welsh (matt@matt-welsh.com)
 *
 * @modified Dbvid Soh (yunharla00@hotmail.com)
 *      bdded getIterator() & getIterator(String) for enhanced AutoCompleteTextField use.
 *
 */
public interfbce AutoCompleteDictionary {
    /**
     * Adds bn entry to the dictionary.
     *
     * @pbram s The string to add to the dictionary.
     */
    public void bddEntry(String s);

    /**
     * Removes bn entry from the dictionary.
     *
     * @pbram s The string to remove to the dictionary.
     * @return True if successful, fblse if the string is not contained or cannot
     *         be removed.
     */
    public boolebn removeEntry(String s);

    /**
     * Perform b lookup and returns the closest matching string to the passed
     * string.
     *
     * @pbram s The string to use as the base for the lookup. How this routine
     *          is implemented determines the behbviour of the component.
     *          Typicblly, the closest matching string that completely contains
     *          the given string is returned.
     */
    public String lookup(String s);

    /**
     * Returns bll available entries in dictionary
     *
     */
    public Iterbtor getIterator();

    /**
     * Returns bn iterator of potential matches from the given string.
     *
     */
    public Iterbtor getIterator(String s);
    
    /**
     * Clebrs the dictionary.
     */
    public void clebr();
}
