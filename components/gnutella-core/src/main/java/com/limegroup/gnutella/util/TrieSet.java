package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * A TrieSet.  A set-like interface designed specifically for Strings.
 * Uses a Trie as the backing Map, and provides an implementation specific to
 * Strings.  Has the same retrieval/insertion times as the backing Trie.
 * Stores the value as the string, for easier retrieval.
 * The goal is to efficiently find Strings that can branch off a prefix.
 *
 * Primarily designed as an AutoCompleteDictionary
 *
 * @modified David Soh (yunharla00@hotmail.com)
 *      1. added getIterator() & getIterator(String) for enhanced AutoCompleteTextField use.
 *      2. disallowed adding duplicates
 *
 */
public class TrieSet implements AutoCompleteDictionary {
    /**
     * The backing map. A binary-sorted Trie.
     */
    private transient Trie map;

    /**
     * This constuctor sets up a dictionary where case IS significant
     * but whose sort order is binary based.
     * All Strings are stored with the case of the last entry added.
     */
    public TrieSet(boolean caseSensitive) {
        map = new Trie(caseSensitive);
    }

    /**
     * Adds a value to the set.  Different letter case of values is always
     * kept and significant.  If the TrieSet is made case-insensitive,
     * it will not store two Strings with different case but will update
     * the stored values with the case of the last entry.
     */
    public void addEntry(String data) {
        if (!contains(data))    //disallow adding duplicates
            map.add(data, data);
    }

    /**
     * Determines whether or not the Set contains this String.
     */
    public boolean contains(String data) {
        return map.get(data) != null;
    }

    /**
     * Removes a value from the Set.
     *
     * @return <tt>true</tt> if a value was actually removed.
     */
    public boolean removeEntry(String data) {
        return map.remove(data);
    }

    /**
     * Return all the Strings that can be prefixed by this String.
     * All values returned by the iterator have their case preserved.
     */
    public Iterator getPrefixedBy(String data) {
        return map.getPrefixedBy(data);
    }

    /**
     * Return the last String in the set that can be prefixed by this String
     * (Trie's are stored in alphabetical order).
     * Return null if no such String exist in the current set.
     */
    public String lookup(String data) {
        Iterator it = map.getPrefixedBy(data);
        if (!it.hasNext())
            return null;
        return (String)it.next();
    }

    /**
     * Returns all values (entire TrieSet)
     */
    public Iterator getIterator() {
        return map.getIterator();
    }

    /**
     * Returns all potential matches off the given String.
     */
    public Iterator getIterator(String s) {
        return map.getPrefixedBy(s);
    }
    
    /**
     * Clears all items in the dictionary.
     */
    public void clear() {
        List l = new LinkedList();
        for(Iterator i = getIterator(); i.hasNext(); )
            l.add(i.next());
        for(Iterator i = l.iterator(); i.hasNext(); )
            removeEntry((String)i.next());
    }
}

