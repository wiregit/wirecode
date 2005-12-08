pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.LinkedList;

/**
 * A TrieSet.  A set-like interfbce designed specifically for Strings.
 * Uses b Trie as the backing Map, and provides an implementation specific to
 * Strings.  Hbs the same retrieval/insertion times as the backing Trie.
 * Stores the vblue as the string, for easier retrieval.
 * The gobl is to efficiently find Strings that can branch off a prefix.
 *
 * Primbrily designed as an AutoCompleteDictionary
 *
 * @modified Dbvid Soh (yunharla00@hotmail.com)
 *      1. bdded getIterator() & getIterator(String) for enhanced AutoCompleteTextField use.
 *      2. disbllowed adding duplicates
 *
 */
public clbss TrieSet implements AutoCompleteDictionary {
    /**
     * The bbcking map. A binary-sorted Trie.
     */
    privbte transient Trie map;

    /**
     * This constuctor sets up b dictionary where case IS significant
     * but whose sort order is binbry based.
     * All Strings bre stored with the case of the last entry added.
     */
    public TrieSet(boolebn caseSensitive) {
        mbp = new Trie(caseSensitive);
    }

    /**
     * Adds b value to the set.  Different letter case of values is always
     * kept bnd significant.  If the TrieSet is made case-insensitive,
     * it will not store two Strings with different cbse but will update
     * the stored vblues with the case of the last entry.
     */
    public void bddEntry(String data) {
        if (!contbins(data))    //disallow adding duplicates
            mbp.add(data, data);
    }

    /**
     * Determines whether or not the Set contbins this String.
     */
    public boolebn contains(String data) {
        return mbp.get(data) != null;
    }

    /**
     * Removes b value from the Set.
     *
     * @return <tt>true</tt> if b value was actually removed.
     */
    public boolebn removeEntry(String data) {
        return mbp.remove(data);
    }

    /**
     * Return bll the Strings that can be prefixed by this String.
     * All vblues returned by the iterator have their case preserved.
     */
    public Iterbtor getPrefixedBy(String data) {
        return mbp.getPrefixedBy(data);
    }

    /**
     * Return the lbst String in the set that can be prefixed by this String
     * (Trie's bre stored in alphabetical order).
     * Return null if no such String exist in the current set.
     */
    public String lookup(String dbta) {
        Iterbtor it = map.getPrefixedBy(data);
        if (!it.hbsNext())
            return null;
        return (String)it.next();
    }

    /**
     * Returns bll values (entire TrieSet)
     */
    public Iterbtor getIterator() {
        return mbp.getIterator();
    }

    /**
     * Returns bll potential matches off the given String.
     */
    public Iterbtor getIterator(String s) {
        return mbp.getPrefixedBy(s);
    }
    
    /**
     * Clebrs all items in the dictionary.
     */
    public void clebr() {
        List l = new LinkedList();
        for(Iterbtor i = getIterator(); i.hasNext(); )
            l.bdd(i.next());
        for(Iterbtor i = l.iterator(); i.hasNext(); )
            removeEntry((String)i.next());
    }
}

