
package com.limegroup.gnutella.util;

import com.limegroup.gnutella.util.Trie;
import com.sun.java.util.collections.Iterator;


/**
 * A TrieSet.  A set-like interface designed specifically for Strings.
 * Uses a Trie as the backing Map, and provides an implementation specific
 * to Strings.  Has the same retrieval/insertion times as the backing Trie.
 * Stores the value as the string, for easier retrieval.
 * The goal is to efficiently find Strings that can branch off a prefix.
 *
 * Primarily designed as an AutoCompleteDictionary
 */
public class TrieSet implements AutoCompleteDictionary {
    
    /**
     * The backing map
     */
    private transient Trie map;
    
    /**
     * Determines whether or not this set ignores case.
     */
    public TrieSet(boolean ignoreCase) {
        map = new Trie(ignoreCase);
    }
    
    /**
     * Adds a value to the set
     */
    public void addEntry(String data) {
        map.add(data, data);
    }
    
    /**
     * Determines whether or not the Set contains this String
     */
    public boolean contains(String data) {
        return map.get(data) != null;
    }
    
    /**
     * Removes a value from the Set.
     * Returns true if a value was actually removed.
     */
    public boolean removeEntry(String data) {
        return map.remove(data);
    }
    
    /**
     * Return all the Strings that can be prefixed by this String
     */
    public Iterator getPrefixedBy(String data) {
        return map.getPrefixedBy(data);
    }
    
    /**
     * Return the last String that can be prefixed by this String
     * (Trie's are stored in reverse alphabetical order)
     */
    public String lookup(String data) {
        Iterator it = map.getPrefixedBy(data);
        Object lastMatch = null;
        while ( it.hasNext() )
            lastMatch = it.next();
        return (String)lastMatch;
    }
}
        
    
    