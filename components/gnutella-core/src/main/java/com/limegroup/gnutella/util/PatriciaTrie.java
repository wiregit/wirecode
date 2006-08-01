/*
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.gnutella.util;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

/**
 * A PATRICIA Trie.
 * 
 * PATRICIA = Practical Algorithm to Retrieve Information Coded in Alphanumeric
 * 
 * @author Roger Kapsi
 * @author Sam Berlin
 */
public class PatriciaTrie<K, V> extends AbstractMap<K, V> implements Trie<K, V>, Serializable {
    
    private static final long serialVersionUID = 110232526181493307L;

    private final TrieEntry<K, V> root = new TrieEntry<K, V>(null, null, -1);
    
    private int size = 0;
    private transient int modCount = 0;
    
    private final KeyAnalyzer<? super K> keyAnalyzer;
    
    public PatriciaTrie(KeyAnalyzer<? super K> keyAnalyzer) {
        this.keyAnalyzer = keyAnalyzer;
    }
    
    /** Returns the KeyAnalyzer */
    public KeyAnalyzer<? super K> getKeyAnalyzer() {
        return keyAnalyzer;
    }
    
    /** Clears the Trie (i.e. removes all emelemnts). */
    public void clear() {
        root.key = null;
        root.bitIndex = -1;
        root.value = null;
        
        root.parent = null;
        root.left = root;
        root.right = null;
        
        size = 0;
        incrementModCount();
    }
    
    /** Returns true if the Trie is empty */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /** Returns the number items in the Trie */
    public int size() {
        return size;
    }
   
    /** Increments both the size and mod counter. */
    private void incrementSize() {
        size++;
        incrementModCount();
    }
    
    /** Decrements the size and increments the mod counter. */
    private void decrementSize() {
        size--;
        incrementModCount();
    }
    
    /** Increments the mod counter. */
    private void incrementModCount() {
        modCount++;
    }
    
    /**
     * Adds a new <key, value> pair to the Trie and if a pair already
     * exists it will be replaced. In the latter case it will return
     * the old value.
     */
    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        int keyLength = length(key);
        
        // The only place to store a key with a length
        // of zero bits is the root node
        if (keyLength == 0) {
            if (root.isEmpty()) {
                incrementSize();
            } else {
                incrementModCount();
            }
            return root.setKeyValue(key, value);
        }
        
        TrieEntry<K, V> found = getR(root.left, -1, key, keyLength);
        if (key.equals(found.key)) {
            if (found.isEmpty()) { // <- must be the root
                incrementSize();
            } else {
                incrementModCount();
            }
            return found.setKeyValue(key, value);
        }
        
        int bitIndex = bitIndex(0, key, found.key);
        if (isValidBitIndex(bitIndex)) { // in 99.999...9% the case
            /* NEW KEY+VALUE TUPLE */
            TrieEntry<K, V> t = new TrieEntry<K, V>(key, value, bitIndex);
            root.left = putR(root.left, t, keyLength, root);
            incrementSize();
            return null;
        } else if (isNullBitKey(bitIndex)) {
            // A bits of the Key are zero. The only place to
            // store such a Key is the root Node!
            
            /* NULL BIT KEY */
            if (root.isEmpty()) {
                incrementSize();
            } else {
                incrementModCount();
            }
            return root.setKeyValue(key, value);
            
        } else if (isEqualBitKey(bitIndex)) {
            // This is a very special and rare case.
            
            /* REPLACE OLD KEY+VALUE */
            if (found != root) {
                incrementModCount();
                return found.setKeyValue(key, value);
            }
        }
        
        throw new IndexOutOfBoundsException("Failed to put: " + key + " -> " + value + ", " + bitIndex);
    }
    
    /**
     * The actual put implementation. Entry t is the new Entry we're
     * gonna add.
     */
    private TrieEntry<K, V> putR(TrieEntry<K, V> h, 
            final TrieEntry<K, V> t, final int keyLength, TrieEntry<K, V> p) {
        
        if ((h.bitIndex >= t.bitIndex) || (h.bitIndex <= p.bitIndex)) {
                
            t.predecessor = t;
            
            if (!isBitSet(t.key, keyLength, t.bitIndex)) {
                t.left = t;
                t.right = h;
            } else {
                t.left = h;
                t.right = t;
            }
           
            t.parent = p;
            if (h.bitIndex >= t.bitIndex) {
                h.parent = t;
            }
            
            // if we inserted an uplink, set the predecessor on it
            if(h.bitIndex <= p.bitIndex) {
                h.predecessor = t;
            }
            
            return t;
        }

        if (!isBitSet(t.key, keyLength, h.bitIndex)) {
            h.left = putR(h.left, t, keyLength, h);
        } else {
            h.right = putR(h.right, t, keyLength, h);
        }
        
        return h;
    }
    
    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es = entrySet;
        return (es != null ? es : (entrySet = new EntrySet()));
    }
    
    /**
     * Returns the Value whose Key equals our lookup Key
     * or null if no such key exists.
     */
    public V get(Object k) {
        TrieEntry<K, V> entry = getEntry(k);
        return entry != null ? entry.getValue() : null;
    }

    /**
     * Returns the entry associated with the specified key in the
     * PatriciaTrie.  Returns null if the map contains no mapping
     * for this key.
     */
    TrieEntry<K,V> getEntry(Object k) {
        K key = asKey(k);
        if(key == null) {
            return null;
        }
        
        int keyLength = length(key);
        TrieEntry<K,V> entry = getR(root.left, -1, key, keyLength);
        return !entry.isEmpty() && key.equals(entry.key) ? entry : null;
    }
    
    /** Casts the key to K.  TODO: this doesn't work */
    @SuppressWarnings("unchecked")
    protected final K asKey(Object key) {
        try {
            return (K)key;
        } catch(ClassCastException cce) {
            // Because the type is erased, the cast & return are
            // actually doing nothing, making this CCE impossible.
            // However, it's still here on the off-chance it may
            // work.
            return null;
        }
    }
    
    /**
     * The actual get implementation. This is very similar to
     * selectR but with the exception that it might return the
     * root Entry even if it's empty.
     */
    private TrieEntry<K, V> getR(TrieEntry<K, V> h, int bitIndex, 
            final K key, final int keyLength) {
        if (h.bitIndex <= bitIndex) {
            return h;
        }

        if (!isBitSet(key, keyLength, h.bitIndex)) {
            return getR(h.left, h.bitIndex, key, keyLength);
        } else {
            return getR(h.right, h.bitIndex, key, keyLength);
        }
    }
    
    /**
     * Returns the Value whose Key has the longest prefix
     * in common with our lookup key.
     */
    @SuppressWarnings("unchecked")
    public V select(K key) {
        int keyLength = length(key);
        TrieEntry[] result = new TrieEntry[1];
        if (!selectR(root.left, -1, key, keyLength, result)) {
            TrieEntry<K, V> e = result[0];
            return e.getValue();
        }
        return null;
    }
    
    /**
     * This is eqivalent to the other selectR() method but without
     * its overhead because we're selecting only one best matching
     * Entry from the Trie.
     */
    private boolean selectR(TrieEntry<K, V> h, int bitIndex, 
            final K key, final int keyLength, final TrieEntry[] result) {
        
        if (h.bitIndex <= bitIndex) {
            // If we hit the root Node and it is empty
            // we have to look for an alternative best
            // mathcing node.
            if (!h.isEmpty()) {
                result[0] = h;
                return false;
            }
            return true;
        }

        if (!isBitSet(key, keyLength, h.bitIndex)) {
            if (selectR(h.left, h.bitIndex, key, keyLength, result)) {
                return selectR(h.right, h.bitIndex, key, keyLength, result);
            }
        } else {
            if (selectR(h.right, h.bitIndex, key, keyLength, result)) {
                return selectR(h.left, h.bitIndex, key, keyLength, result);
            }
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public Map.Entry<K,V> select(K key, Cursor<? super K, ? super V> cursor) {
        int keyLength = length(key);
        TrieEntry[] result = new TrieEntry[]{ null };
        selectR(root.left, -1, key, keyLength, cursor, result);
        return result[0];
    }

    private boolean selectR(TrieEntry<K,V> h, int bitIndex, 
            final K key, 
            final int keyLength,
            final Cursor<? super K, ? super V> cursor,
            final TrieEntry[] result) {

        if (h.bitIndex <= bitIndex) {
            if(!h.isEmpty()) {
                Cursor.SelectStatus ret = cursor.select(h);
                if(ret == Cursor.SelectStatus.REMOVE) {
                    throw new UnsupportedOperationException("cannot remove during select");
                    //remove(h.key);
                    //return true; // continue
                } else if(ret == Cursor.SelectStatus.EXIT) {
                    result[0] = h;
                    return false; // exit
                } // else if (ret == Cursor.SelectStatus.CONTINUE), fall through
            }
            return true; // continue
        }

        if (!isBitSet(key, keyLength, h.bitIndex)) {
            if (selectR(h.left, h.bitIndex, key, keyLength, cursor, result)) {
                return selectR(h.right, h.bitIndex, key, keyLength, cursor, result);
            }
        } else {
            if (selectR(h.right, h.bitIndex, key, keyLength, cursor, result)) {
                return selectR(h.left, h.bitIndex, key, keyLength, cursor, result);
            }
        }
        
        return false;
    }

    public SortedMap<K, V> getPrefixedBy(K key) {
        return getPrefixedByBits(key, 0, keyAnalyzer.length(key));
    }

    public SortedMap<K, V> getPrefixedBy(K key, int length) {
        return getPrefixedByBits(key, 0, length * keyAnalyzer.bitsPerElement());
    }

    public SortedMap<K, V> getPrefixedBy(K key, int offset, int length) {
        return getPrefixedByBits(key, offset * keyAnalyzer.bitsPerElement(), length * keyAnalyzer.bitsPerElement());
    }

    public SortedMap<K, V> getPrefixedByBits(K key, int bitLength) {
        return getPrefixedByBits(key, 0, bitLength);
    }
    
    /**
     * Returns a view of this map, with entries containing only those that
     * are prefixed by a value whose bits matches the bits between 'offset'
     * and 'length' in the given key.
     * 
     * @param key
     * @param offset
     * @param length
     * @return
     */
    private SortedMap<K, V> getPrefixedByBits(K key, int offset, int length) {
        int offsetLength = offset + length;
        if (offsetLength > length(key)) {
            throw new IllegalArgumentException(offset + " + " + length + " > " + length(key));
        }
        
        if(offsetLength == 0)
            return this;
        
        return new PrefixSubMap(key, offset, length);
    }
    
    /** Returns true if this trie contains the specified Key */
    public boolean containsKey(Object k) {
        K key = asKey(k);
        if(key == null) {
            return false;
        }
        
        int keyLength = length(key);
        TrieEntry entry = getR(root.left, -1, key, keyLength);
        return !entry.isEmpty() && key.equals(entry.key);
    }
    
    /** Returns true if this Trie contains the specified value. */
    public boolean containsValue(Object o) {
        for(V v : values())
            if(valEquals(v, o))
                return true;
        return false;
    }
    
    
    /**
     * Removes a Key from the Trie if one exists
     * 
     * @param key the Key to delete
     * @return Returns the deleted Value
     */
    public V remove(Object k) {
        K key = asKey(k);
        if(key == null) {
            return null;
        }
        
        int keyLength = length(key);
        return removeR(root.left, -1, key, keyLength);
    }
    
    /**
     * Part 1
     * 
     * Serach for the Key
     */
    private V removeR(TrieEntry<K, V> h, int bitIndex, 
            final K key, final int keyLength) {
        
        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty() && key.equals(h.key)) {
                return removeEntry(h);
            }
            return null;
        }
        
        if (!isBitSet(key, keyLength, h.bitIndex)) {
            return removeR(h.left, h.bitIndex, key, keyLength);
        } else {
            return removeR(h.right, h.bitIndex, key, keyLength);
        }
    }
    
    /**
     * Part 2
     * 
     * If we found a Key (Entry h) then figure out if it's
     * an internal (hard to remove) or external Entry (easy 
     * to remove)
     */
    private V removeEntry(TrieEntry<K, V> h) {
        if (h != root) {
            if (h.isInternalNode()) {
                removeInternalEntry(h);
            } else {
                removeExternalEntry(h);
            }
        }
        
        decrementSize();
        return h.setKeyValue(null, null);
    }
    
    /**
     * Part 3
     * 
     * If it's an external Entry then just remove it.
     * This is very easy and straight forward.
     */
    private void removeExternalEntry(TrieEntry<K, V> h) {
        if (h == root) {
            throw new IllegalArgumentException("Cannot delete root Entry!");
        } else if (!h.isExternalNode()) {
            throw new IllegalArgumentException(h + " is not an external Entry!");
        } 
        
        TrieEntry<K, V> parent = h.parent;
        TrieEntry<K, V> child = (h.left == h) ? h.right : h.left;
        
        if (parent.left == h) {
            parent.left = child;
        } else {
            parent.right = child;
        }
        
        // either the parent is changing, or the predecessor is changing.
        if (child.bitIndex > parent.bitIndex) {
            child.parent = parent;
        } else {
            child.predecessor = parent;
        }
        
    }
    
    /**
     * Part 4
     * 
     * If it's an internal Entry then "good luck" with understanding
     * this code. The Idea is essentially that Entry p takes Entry h's
     * place in the trie which requires some re-wireing.
     */
    private void removeInternalEntry(TrieEntry<K, V> h) {
        if (h == root) {
            throw new IllegalArgumentException("Cannot delete root Entry!");
        } else if (!h.isInternalNode()) {
            throw new IllegalArgumentException(h + " is not an internal Entry!");
        } 
        
        TrieEntry<K, V> p = h.predecessor;
        
        // Set P's bitIndex
        p.bitIndex = h.bitIndex;
        
        // Fix P's parent, predecessor and child Nodes
        {
            TrieEntry<K, V> parent = p.parent;
            TrieEntry<K, V> child = (p.left == h) ? p.right : p.left;
            
            // if it was looping to itself previously,
            // it will now be pointed from it's parent
            // (if we aren't removing it's parent --
            //  in that case, it remains looping to itself).
            // otherwise, it will continue to have the same
            // predecessor.
            if(p.predecessor == p && p.parent != h)
                p.predecessor = p.parent;
            
            if (parent.left == p) {
                parent.left = child;
            } else {
                parent.right = child;
            }
            
            if (child.bitIndex > parent.bitIndex) {
                child.parent = parent;
            }
        };
        
        // Fix H's parent and child Nodes
        {         
            // If H is a parent of its left and right childs 
            // then change them to P
            if (h.left.parent == h) {
                h.left.parent = p;
            }
            
            if (h.right.parent == h) {
                h.right.parent = p;
            }
            
            // Change H's parent
            if (h.parent.left == h) {
                h.parent.left = p;
            } else {
                h.parent.right = p;
            }
        };
        
        // Copy the remaining fields from H to P
        //p.bitIndex = h.bitIndex;
        p.parent = h.parent;
        p.left = h.left;
        p.right = h.right;
        
        // Make sure that if h was pointing to any uplinks,
        // p now points to them.
        if(isValidUplink(p.left, p))
            p.left.predecessor = p;
        if(isValidUplink(p.right, p))
            p.right.predecessor = p;
            
    }
    
    /**
     * Returns the node lexigraphically before the given node (or null if none).
     * 
     * This follows four simple branches:
     *  - If the uplink that returned us was a right uplink:
     *      - If predecessor's left is a valid uplink from predecessor, return it.
     *      - Else, follow the right path from the predecessor's left.
     *  - If the uplink that returned us was a left uplink:
     *      - Loop back through parents until we encounter a node where 
     *        node != node.parent.left.
     *          - If node.parent.left is uplink from node.parent:
     *              - If node.parent.left is not root, return it.
     *              - If it is root & root isEmpty, return null.
     *              - If it is root & root !isEmpty, return root.
     *          - If node.parent.left is not uplink from node.parent:
     *              - Follow right path for first right child from node.parent.left   
     * 
     * @param start
     * @return
     */
    private TrieEntry<K, V> previousEntry(TrieEntry<K, V> start) {
        if(start.predecessor == null)
            throw new IllegalArgumentException("must have come from somewhere!");
        
        if(start.predecessor.right == start) {
            if(isValidUplink(start.predecessor.left, start.predecessor)) {
                return start.predecessor.left;
            } else {
                return followRight(start.predecessor.left);
            }
        } else {
            TrieEntry<K, V> node = start.predecessor;
            while(node.parent != null && node == node.parent.left)
                node = node.parent;
            if(node.parent == null) // can be null if we're looking up root.
                return null;
            if(isValidUplink(node.parent.left, node.parent)) {
                if(node.parent.left == root) {
                    if(root.isEmpty())
                        return null;
                    else
                        return root;
                } else {
                    return node.parent.left;
                }
            } else {
                return followRight(node.parent.left);
            }
        }
    }
    
    /**
     * Returns the entry lexigraphically after the given entry.
     * If the given entry is null, returns the first node.
     */
    private TrieEntry<K, V> nextEntry(TrieEntry<K, V> node) {
        if(node == null) {
            return firstEntry();
        } else {
            return nextEntryImpl(node.predecessor, node, null);
        }
    }
    
    /**
     * Returns the entry lexigraphically after the given entry.
     * If the given entry is null, returns the first node.
     * 
     * This will traverse only within the subtree.  If the given node
     * is not within the subtree, this will have undefined results.
     */
    private TrieEntry<K, V> nextEntryInSubtree(TrieEntry<K, V> node, TrieEntry<K, V> parentOfSubtree) {
        if(node == null) {
            return firstEntry();
        } else {
            return nextEntryImpl(node.predecessor, node, parentOfSubtree);
        }
    }
    
    /**
     * Scans for the next node, starting at the specified point, and using 'previous'
     * as a hint that the last node we returned was 'previous' (so we know not to return
     * it again).  If 'tree' is non-null, this will limit the search to the given tree.
     * 
     * The basic premise is that each iteration can follow the following steps:
     * 
     * 1) Scan all the way to the left.
     *   a) If we already started from this node last time, proceed to Step 2.
     *   b) If a valid uplink is found, use it.
     *   c) If the result is an empty node (root not set), break the scan.
     *   d) If we already returned the left node, break the scan.
     *   
     * 2) Check the right.
     *   a) If we already returned the right node, proceed to Step 3.
     *   b) If it is a valid uplink, use it.
     *   c) Do Step 1 from the right node.
     *   
     * 3) Back up through the parents until we encounter find a parent
     *    that we're not the right child of.
     *    
     * 4) If there's no right child of that parent, the iteration is finished.
     *    Otherwise continue to Step 5.
     * 
     * 5) Check to see if the right child is a valid uplink.
     *    a) If we already returned that child, proceed to Step 6.
     *       Otherwise, use it.
     *    
     * 6) If the right child of the parent is the parent itself, we've
     *    already found & returned the end of the Trie, so exit.
     *    
     * 7) Do Step 1 on the parent's right child.
     */
    private TrieEntry<K, V> nextEntryImpl(TrieEntry<K, V> start, TrieEntry<K, V> previous, TrieEntry<K, V> tree) {
        TrieEntry<K, V> current = start;

        // Only look at the left if this was a recursive or
        // the first check, otherwise we know we've already looked
        // at the left.
        if(previous == null || start != previous.predecessor) {
            while(!current.left.isEmpty()) {
                // stop traversing if we've already
                // returned the left of this node.
                if(previous == current.left) {
                    break;
                }
                
                if(isValidUplink(current.left, current)) {
                    return current.left;
                }
                
                current = current.left;
            }
        }
        
        // If there's no data at all, exit.
        if(current.isEmpty()) {
            return null;
        }
        
        // If nothing valid on the left, try the right.
        if(previous != current.right) {
            // See if it immediately is valid.
            if(isValidUplink(current.right, current)) {
                return current.right;
            }
            
            // Must search on the right's side if it wasn't initially valid.
            return nextEntryImpl(current.right, previous, tree);
        }
        
        // Neither left nor right are valid, find the first parent
        // whose child did not come from the right & traverse it.
        while(current == current.parent.right) {
            // If we're going to traverse to above the subtree, stop.
            if(current == tree)
                return null;
            
            current = current.parent;
        }

        // If we're on the top of the subtree, we can't go any higher.
        if(current == tree)
            return null;
        
        
        // If there's no right, the parent must be root, so we're done.
        if(current.parent.right == null) {
            return null;
        }

        
        // If the parent's right points to itself, we've found one.
        if(previous != current.parent.right && isValidUplink(current.parent.right, current.parent)) {
            return current.parent.right;
        }
        
        // If the parent's right is itself, there can't be any more nodes.
        if(current.parent.right == current.parent) {
            return null;
        }
        
        // We need to traverse down the parent's right's path.
        return nextEntryImpl(current.parent.right, previous, tree);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Trie[").append(size()).append("]={\n");
        toStringR(root.left, -1, buffer);
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private StringBuilder toStringR(TrieEntry<K, V> h, int bitIndex, 
            final StringBuilder buffer) {

        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty()) {
                buffer.append("  ").append(h.toString()).append("\n");
            }
            return buffer;
        }

        toStringR(h.left, h.bitIndex, buffer);
        return toStringR(h.right, h.bitIndex, buffer);
    }
    
    /**
     * Traverses through the trie, passing each entry to the cursor.
     * This will return the element that the cursor returned EXIT on,
     * or null if the trie runs out of elements.  Any elements the cursor
     * returns REMOVE on will be removed.  The cursor should return 
     * CONTINUE when it wishes to continue looking at more elements.
     */
    public Map.Entry<K, V> traverse(Cursor<? super K, ? super V> cursor) {
        TrieEntry<K, V> entry = nextEntry(null);
        while(entry != null) {
            TrieEntry<K, V> current = entry;
            Cursor.SelectStatus ret = cursor.select(current);
            entry = nextEntry(current);
            switch(ret) {
            case EXIT:
                return current;
            case REMOVE:
                removeEntry(current);
                break;
            case CONTINUE: // do nothing.
            }
        }
        
        return null;
    }
    
    /** Returns true if 'next' is a valid uplink coming from 'from'. */
    private boolean isValidUplink(TrieEntry<K, V> next, TrieEntry<K, V> from) {            
        return next != null && next.bitIndex <= from.bitIndex && !next.isEmpty();
    }
    
    /** Returns true if bitIndex is a valid index */
    private static boolean isValidBitIndex(int bitIndex) {
        return 0 <= bitIndex && bitIndex <= Integer.MAX_VALUE;
    }
    
    /** Returns true if bitIndex is a NULL_BIT_KEY */
    private static boolean isNullBitKey(int bitIndex) {
        return bitIndex == KeyAnalyzer.NULL_BIT_KEY;
    }
    
    /** Returns true if bitIndex is a EQUAL_BIT_KEY */
    private static boolean isEqualBitKey(int bitIndex) {
        return bitIndex == KeyAnalyzer.EQUAL_BIT_KEY;
    }
    
    private int length(K key) {
        if (key == null) {
            return 0;
        }
        
        return keyAnalyzer.length(key);
    }
    
    private boolean isBitSet(K key, int keyLength, int bitIndex) {
        if (key == null) { // root's might be null!
            return false;
        }
        return keyAnalyzer.isBitSet(key, keyLength, bitIndex);
    }
    
    private int bitIndex(int startAt, K key, K foundKey) {
        return keyAnalyzer.bitIndex(startAt, key, foundKey);
    }
    
    /**
     * The actual Trie nodes.
     */
    private static class TrieEntry<K,V> implements Map.Entry<K,V>, Serializable {
        
        private static final long serialVersionUID = 4596023148184140013L;
        
        private K key;
        private V value;
        
        private int bitIndex;
        
        /** The parent of this entry. */
        private TrieEntry<K,V> parent;
        /** The left child of this entry. */
        private TrieEntry<K,V> left;
        /** The right child of this entry. */
        private TrieEntry<K,V> right;
        /** The entry who uplinks to this entry. */ 
        private TrieEntry<K,V> predecessor;
        
        private TrieEntry(K key, V value, int bitIndex) {
            this.key = key;
            this.value = value;
            
            this.bitIndex = bitIndex;
            
            this.parent = null;
            this.left = this;
            this.right = null;
        }
        
        public boolean equals(Object o) {
            if(o == this) {
                return true;
            } else if(o instanceof Map.Entry) {
                Map.Entry e = (Map.Entry)o;
                Object k1 = getKey();
                Object k2 = e.getKey();
                if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                    Object v1 = getValue();
                    Object v2 = e.getValue();
                    if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                        return true;
                }
                return false;
            } else {
                return false;
            }
        }
        
        /**
         * This can be only the case with the root node!
         */
        public boolean isEmpty() {
            return key == null;
        }
        
        public K getKey() {
            return key;
        }
        
        public V getValue() {
            return value;
        }
        
        public V setValue(V value) {
            V o = this.value;
            this.value = value;
            return o;
        }
        
        /** 
         * Replaces the old key and value with the new ones.
         * Returns the old vlaue.
         */
        private V setKeyValue(K key, V value) {
            this.key = key;
            return setValue(value);
        }
        
        /** Neither the left nor right child is a loopback */
        private boolean isInternalNode() {
            return left != this && right != this;
        }
        
        /** Either the left or right child is a loopback */
        private boolean isExternalNode() {
            return !isInternalNode();
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            
            if (bitIndex == -1) {
                buffer.append("RootEntry(");
            } else {
                buffer.append("Entry(");
            }
            
            buffer.append("key=").append(getKey()).append(" [").append(bitIndex).append("], ");
            buffer.append("value=").append(getValue()).append(", ");
            //buffer.append("bitIndex=").append(bitIndex).append(", ");
            
            if (parent != null) {
                if (parent.bitIndex == -1) {
                    buffer.append("parent=").append("ROOT");
                } else {
                    buffer.append("parent=").append(parent.getKey()).append(" [").append(parent.bitIndex).append("]");
                }
            } else {
                buffer.append("parent=").append("null");
            }
            buffer.append(", ");
            
            if (left != null) {
                if (left.bitIndex == -1) {
                    buffer.append("left=").append("ROOT");
                } else {
                    buffer.append("left=").append(left.getKey()).append(" [").append(left.bitIndex).append("]");
                }
            } else {
                buffer.append("left=").append("null");
            }
            buffer.append(", ");
            
            if (right != null) {
                if (right.bitIndex == -1) {
                    buffer.append("right=").append("ROOT");
                } else {
                    buffer.append("right=").append(right.getKey()).append(" [").append(right.bitIndex).append("]");
                }
            } else {
                buffer.append("right=").append("null");
            }
            buffer.append(", ");
            
            if(predecessor != null) {
                if(predecessor.bitIndex == -1) {
                    buffer.append("predecessor=").append("ROOT");
                } else {
                    buffer.append("predecessor=").append(predecessor.getKey()).append(" [").append(predecessor.bitIndex).append("]");
                }
            }
            
            buffer.append(")");
            return buffer.toString();
        }
    }
    
    /**
     * The interface used by PatriciaTrie to access the Keys
     * on bit level.
     */
    public static interface KeyAnalyzer<K> extends Comparator<K>, Serializable {
        
        /** Returned by bitIndex if key's bits are all 0 */
        public static final int NULL_BIT_KEY = -1;
        
        /** 
         * Returned by bitIndex if key and found key are
         * equal. This is a very very specific case and
         * shouldn't happen on a regular basis
         */
        public static final int EQUAL_BIT_KEY = -2;
        
        /** 
         * Returns the length of the Key in bits. 
         */
        public int length(K key);
        
        /** Returns whether or not a bit is set */
        public boolean isBitSet(K key, int keyLength, int bitIndex);
        
        /**
         * Returns the n-th different bit between key and found,
         * starting the comparison at startAt.
         */
        public int bitIndex(int startAt, K key, K found);
        
        /**
         * Returns the number of bits per element in the key.
         * This is only useful for variable-length keys, such as Strings.
         */
        public int bitsPerElement();
    }
    
    /**
     * An iterator for the entries.
     */
    private abstract class NodeIterator<E> implements Iterator<E> {
        protected int expectedModCount = modCount;   // For fast-fail 
        protected TrieEntry<K, V> next; // the next node to return
        protected TrieEntry<K, V> current; // the current entry we're on
        
        // Starts iteration from the beginning.
        protected NodeIterator() {
            next = PatriciaTrie.this.nextEntry(null);
        }
        
        // Starts iteration at the given entry.
        protected NodeIterator(TrieEntry<K, V> firstEntry) {
            next = firstEntry;
        }
        
        public boolean hasNext() {
            return next != null;
        }
        
        TrieEntry<K,V> nextEntry() { 
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            TrieEntry<K,V> e = next;
            if (e == null) 
                throw new NoSuchElementException();
            
            next = findNext(e);
            current = e;
            return e;
        }
        
        protected TrieEntry<K, V> findNext(TrieEntry<K, V> prior) {
            return PatriciaTrie.this.nextEntry(prior);
        }
        
        public void remove() {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            
            TrieEntry<K, V> node = current;
            current = null;
            PatriciaTrie.this.removeEntry(node);
            
            expectedModCount = modCount;
        }
    }

    private class ValueIterator extends NodeIterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends NodeIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends NodeIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }
    
    /** An iterator for iterating over a prefix search. */
    private class PrefixIterator extends NodeIterator<Map.Entry<K, V>> {
        protected final TrieEntry<K, V> subtree; // the subtree to search within
        
        // Starts iteration at the given entry & search only within the given subtree.
        PrefixIterator(TrieEntry<K, V> startScan) {
            subtree = startScan;
            next = PatriciaTrie.this.followLeft(startScan);
        }

        public Map.Entry<K,V> next() {
            return nextEntry();
        }
        
        @Override
        protected TrieEntry<K, V> findNext(TrieEntry<K, V> prior) {
            return PatriciaTrie.this.nextEntryInSubtree(prior, subtree);
        }
    }
    
    private class SubMapEntryIterator extends NodeIterator<Map.Entry<K,V>> {
        private final K firstExcludedKey;

        SubMapEntryIterator(TrieEntry<K,V> first, TrieEntry<K,V> firstExcluded) {
            super(first);
            firstExcludedKey = 
              (firstExcluded == null ? null : firstExcluded.key);
        }

        public boolean hasNext() {
            return next != null && next.key != firstExcludedKey;
        }

        public Map.Entry<K,V> next() {
            if (next == null || next.key == firstExcludedKey)
                throw new NoSuchElementException();
            return nextEntry();
        }
    }

    Iterator<K> newKeyIterator()   {
        return new KeyIterator();
    }
    
    Iterator<V> newValueIterator()   {
        return new ValueIterator();
    }
    
    Iterator<Map.Entry<K,V>> newEntryIterator()   {
        return new EntryIterator();
    }
    
    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     */
    private transient volatile Set<K>               keySet = null;
    private transient volatile Collection<V>        values = null;
    private transient volatile Set<Map.Entry<K,V>>  entrySet = null;
    
    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return newEntryIterator();
        }
        
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            
            TrieEntry<K,V> candidate = getEntry(((Map.Entry)o).getKey());
            return candidate != null && candidate.equals(o);
        }
        
        public boolean remove(Object o) {
            int size = size();
            PatriciaTrie.this.remove(o);
            return size != size();
        }
        
        public int size() {
            return size;
        }
        
        public void clear() {
            PatriciaTrie.this.clear();
        }
    }
    
    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return newKeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            int size = size();
            PatriciaTrie.this.remove(o);
            return size != size();
        }
        public void clear() {
            PatriciaTrie.this.clear();
        }
    }
    
    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }
    
    /** Test two values for equality.  Works with null values. */
    private static boolean valEquals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return newValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            PatriciaTrie.this.clear();
        }
        public boolean remove(Object o) {
            for(Iterator<V> i =  iterator(); i.hasNext(); ) {
                V v = i.next();
                if(valEquals(v, o)) {
                    i.remove();
                    return true;
                }
            }
            return false;
        }
    }

    public Comparator<? super K> comparator() {
        return keyAnalyzer;
    }
    
    /**
     * Returns the first entry the Trie is storing.
     * 
     * This is implemented by going always to the left until
     * we encounter a valid uplink.  That uplink is the first key.
     */
    private TrieEntry<K, V> firstEntry() {
        // if Trie is empty, no first node.
        if(isEmpty())
            return null;
        
        return followLeft(root);
    }
    
    /**
     * Goes left through the tree until it finds a valid node.
     */
    private TrieEntry<K, V> followLeft(TrieEntry<K, V> node) {
        while(true) {
            TrieEntry<K, V> child = node.left;
            // if we hit root and it didn't have a node, go right instead.
            if(child.isEmpty())
                child = node.right;
            
            if(child.bitIndex <= node.bitIndex)
                return child;
            
            node = child;
        }
    }
    
    /**
     * Returns the last entry the Trie is storing.
     * 
     * This is implemented by going always to the right until
     * we encounter a valid uplink.  That uplink is the last key.
     */
    private TrieEntry<K, V> lastEntry() {
        return followRight(root.left);
    }
    
    /** Traverses down the right path until it finds an uplink. */
    protected TrieEntry<K, V> followRight(TrieEntry<K, V> node) {
        // if Trie is empty, no last entry.
        if(node.right == null)
            return null;
        
        // Go as far right as possible, until we encounter an uplink.
        while(node.right.bitIndex > node.bitIndex)
            node = node.right;
        
        return node.right;
    }

    public K firstKey() {
        return firstEntry().getKey();
    }

    public SortedMap<K, V> headMap(K toKey) {
        return new SubMap(null, toKey);
    }

    public K lastKey() {
        TrieEntry<K, V> entry = lastEntry();
        if(entry != null)
            return entry.getKey();
        else
            return null;
    }

    
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        return new SubMap(fromKey, toKey);
    }

    public SortedMap<K, V> tailMap(K fromKey) {
        return new SubMap(fromKey, null);
    } 
    
    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the least key greater than the specified
     * key; if no such entry exists (i.e., the greatest key in the Tree is less
     * than the specified key), returns <tt>null</tt>.
     */
    protected TrieEntry<K,V> getCeilEntry(K key) {
        // Basically:
        // Follow the steps of adding an entry, but instead...
        //
        // - If we ever encounter a situation where we found an equal
        //   key, we return it immediately.
        //
        // - If we hit an empty root, return the first iterable item.
        //
        // - If we have to add a new item, we temporarily add it,
        //   find the successor to it, then remove the added item.
        //
        // These steps ensure that the returned value is either the
        // entry for the key itself, or the first entry directly after
        // the key.
        
        // TODO: Cleanup so that we don't actually have to add/remove from the
        //       tree.  (We do it here because there are other well-defined 
        //       functions to perform the search.)
        int keyLength = length(key);
        
        if (keyLength == 0) {
            if(!root.isEmpty())
                return root;
            else
                return firstEntry();
        }
        
        TrieEntry<K, V> found = getR(root.left, -1, key, keyLength);
        if (key.equals(found.key))
            return found;
        
        int bitIndex = bitIndex(0, key, found.key);
        if (isValidBitIndex(bitIndex)) {
            TrieEntry<K, V> added = new TrieEntry<K, V>(key, null, bitIndex);
            root.left = putR(root.left, added, keyLength, root);
            incrementSize(); // must increment because remove will decrement
            TrieEntry<K, V> ceil = nextEntry(added);
            removeEntry(added);
            modCount -= 2; // we didn't really modify it.
            return ceil;
        } else if (isNullBitKey(bitIndex)) {
            if (!root.isEmpty())
                return root;
            else
                return firstEntry();
        } else if (isEqualBitKey(bitIndex)) {
            return found;
        }

        // we should have exited above.
        throw new IllegalStateException("invalid lookup: " + key);
    }
    
    /**
     * Returns the entry for the greatest key less than the specified key; if no such entry
     * exists (i.e., the least key in the Tree is greater than the specified key), returns
     * <tt>null</tt>.
     */
    protected TrieEntry<K,V> getPrecedingEntry(K key) {
        // Basically:
        // Follow the steps of adding an entry, but instead...
        //
        // - If we ever encounter a situation where we found an equal
        //   key, we return it's previousEntry immediately.
        //
        // - If we hit root (empty or not), return null.
        //
        // - If we have to add a new item, we temporarily add it,
        //   find the previousEntry to it, then remove the added item.
        //
        // These steps ensure that the returned value is always just before
        // the key or null (if there was nothing before it).
        
        // TODO: Cleanup so that we don't actually have to add/remove from the
        //       tree.  (We do it here because there are other well-defined 
        //       functions to perform the search.)
        int keyLength = length(key);
        
        if (keyLength == 0) {
            return null; // there can never be anything before root.
        }
        
        TrieEntry<K, V> found = getR(root.left, -1, key, keyLength);
        if (key.equals(found.key))
            return previousEntry(found);
        
        int bitIndex = bitIndex(0, key, found.key);
        if (isValidBitIndex(bitIndex)) {
            TrieEntry<K, V> added = new TrieEntry<K, V>(key, null, bitIndex);
            root.left = putR(root.left, added, keyLength, root);
            incrementSize(); // must increment because remove will decrement
            TrieEntry<K, V> prior = previousEntry(added);
            removeEntry(added);
            modCount -= 2; // we didn't really modify it.
            return prior;
        } else if (isNullBitKey(bitIndex)) {
            return null;
        } else if (isEqualBitKey(bitIndex)) {
            return previousEntry(found);
        }

        // we should have exited above.
        throw new IllegalStateException("invalid lookup: " + key);
    }
    
    
    /** A submap used for prefix views over the Trie. */
    private class PrefixSubMap extends SubMap {
        protected final K prefix;
        protected final int offset;
        protected final int length;
        protected final int offsetLength;
        protected int size;
        
        PrefixSubMap(K prefix, int offset, int length) {
            this.prefix = prefix;
            this.offset = offset;
            this.length = length;
            offsetLength = offset + length;
        }
        
        private transient int keyModCount = 0;
        
        @Override
        public Set<Entry<K, V>> entrySet() {
            return super.entrySet();
        }

        @Override
        public K firstKey() {
            fixup();
            return super.firstKey();
        }

        @Override
        protected boolean inRange(K key) {
            fixup();
            return super.inRange(key);
        }

        @Override
        protected boolean inRange2(K key) {
            fixup();
            return super.inRange2(key);
        }

        @Override
        public K lastKey() {
            fixup();
            return super.lastKey();
        }
        
        private void fixup() {
            // The trie has changed since we last
            // found our toKey / fromKey
            if(modCount != keyModCount) {
                Iterator<Map.Entry<K, V>> iter = entrySet().iterator();
                size = 0;
                
                Map.Entry<K, V> entry = null;
                if(iter.hasNext()) {
                    entry = iter.next();
                    size = 1;
                }
                
                fromKey = entry == null ? null : entry.getKey();
                toKey = fromKey;
                
                while(iter.hasNext()) {
                    size++;
                    entry = iter.next();
                }
                
                toKey = entry == null ? null : entry.getKey();
                
                if(toKey != null) {
                    entry = nextEntry((TrieEntry<K, V>)entry);
                    toKey = entry == null ? null : entry.getKey();
                }
            }
        }
        
        protected Set<Map.Entry<K, V>> newSubMapEntrySet() {
            return new PrefixEntrySetView();
        }

        private class PrefixEntrySetView extends SubMap.EntrySetView {
            private TrieEntry<K, V> prefixStart;
            private boolean empty; 
            
            public int size() {
                fixup();
                return PrefixSubMap.this.size;
            }

            public boolean isEmpty() {
                return !iterator().hasNext();
            }

            public Iterator<Map.Entry<K,V>> iterator() {
                if(modCount != keyModCount) {
                    fixupIterator();
                }
                
                if(empty) {
                    return EmptyIterator.emptyIterator();
                } else if(length >= prefixStart.bitIndex){
                    return Collections.singletonList((Map.Entry<K, V>)prefixStart).iterator();
                } else {
                    return new PrefixIterator(prefixStart);
                }
            }
            
            /**
             * Finds the subtree that contains the prefix.
             * 
             * This is very similar to getR but with the difference that
             * we stop the lookup if h.bitIndex > keyLength.
             */
            private TrieEntry<K, V> subtree() {
                TrieEntry<K, V> current = root.left;
                TrieEntry<K, V> path = root;
                while(true) {
                    if(current.bitIndex <= path.bitIndex || length < current.bitIndex)
                        return current.isEmpty() ? path : current;
                    
                    path = current;
                    if(!isBitSet(prefix, length, current.bitIndex))
                        current = current.left;
                    else
                        current = current.right;
                }
            }
            
            private void fixupIterator() {
                TrieEntry<K, V> entry = subtree();
                empty = true;
                prefixStart = null;
                
                // entry isn't root
                if (entry.isEmpty())
                    return;
                
                // Found key's length-th bit differs from our key
                // which means it cannot be the prefix...
                if(isBitSet(prefix, offsetLength, offsetLength) != 
                  isBitSet(entry.key, length(entry.key), offsetLength))
                    return;
                
                // ... or there are less than 'length' equal bits
                // TODO: must offset the bitIndex lookup in key
                int bitIndex = bitIndex(0, prefix, entry.key);
                if (bitIndex >= 0 && bitIndex < length)
                    return;
                
                // Otherwise, the entry is the start of the tree.
                empty = false;
                prefixStart = entry;
            }
        }
    }
    
    private class SubMap extends AbstractMap<K,V> implements SortedMap<K,V>, java.io.Serializable {

        // TODO: add serialVersionUID
        
        /** The key to start from, null if the beginning. */
        protected K fromKey;
        
        /** The key to end at, null if till the end. */
        protected K toKey;
        
        /**
         * Constructs a blank SubMap -- this should ONLY be used
         * by subclasses that wish to lazily construct their
         * fromKey or toKey
         */
        protected SubMap() {
        }

        SubMap(K fromKey, K toKey) {
            if(fromKey == null && toKey == null)
                throw new IllegalArgumentException("must have a from or to!");
            if(fromKey != null && toKey != null && keyAnalyzer.compare(fromKey, toKey) > 0)
                throw new IllegalArgumentException("fromKey > toKey");
            this.fromKey = fromKey;
            this.toKey = toKey;
        }
        
        public boolean isEmpty() {
            return entrySet.isEmpty();
        }

        @SuppressWarnings("unchecked")
        public boolean containsKey(Object key) {
            return inRange((K) key) && PatriciaTrie.this.containsKey(key);
        }

        @SuppressWarnings("unchecked")
        public V get(Object key) {
            if (!inRange((K) key))
                return null;
            return PatriciaTrie.this.get(key);
        }

        public V put(K key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("key out of range");
            return PatriciaTrie.this.put(key, value);
        }

        public Comparator<? super K> comparator() {
            return keyAnalyzer;
        }

        public K firstKey() {
            TrieEntry<K,V> e = fromKey == null ? firstEntry() : getCeilEntry(fromKey);
            K first = e.getKey();
            if (toKey != null && keyAnalyzer.compare(first, toKey) >= 0)
                throw new NoSuchElementException();
            return first;
        }

        public K lastKey() {
            TrieEntry<K,V> e = toKey == null ? lastEntry() : getPrecedingEntry(toKey);
            K last = e.getKey();
            if (fromKey != null && keyAnalyzer.compare(last, fromKey) < 0)
                throw new NoSuchElementException();
            return last;
        }

        private transient Set<Map.Entry<K,V>> entrySet;

        public Set<Map.Entry<K,V>> entrySet() {
            if(entrySet == null)
                entrySet = newSubMapEntrySet();
            return entrySet;
        }
        
        protected Set<Map.Entry<K, V>> newSubMapEntrySet() {
            return new EntrySetView();
        }

        private class EntrySetView extends AbstractSet<Map.Entry<K,V>> {
            private transient int size = -1, sizeModCount;

            public int size() {
                if (size == -1 || sizeModCount != PatriciaTrie.this.modCount) {
                    size = 0;  sizeModCount = PatriciaTrie.this.modCount;
                    Iterator i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            public boolean isEmpty() {
                return !iterator().hasNext();
            }

            @SuppressWarnings("unchecked")
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
                K key = entry.getKey();
                if (!inRange(key))
                    return false;
                TrieEntry<K, V> node = getEntry(key);
                return node != null && 
                       valEquals(node.getValue(), entry.getValue());
            }

            @SuppressWarnings("unchecked")
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
                K key = entry.getKey();
                if (!inRange(key))
                    return false;
                TrieEntry<K,V> node = getEntry(key);
                if (node!=null && valEquals(node.getValue(),entry.getValue())){
                    removeEntry(node);
                    return true;
                }
                return false;
            }

            public Iterator<Map.Entry<K,V>> iterator() {
                return new SubMapEntryIterator(
                    (fromKey == null ? firstEntry() : getCeilEntry(fromKey)),
                    (toKey   == null ? null         : getCeilEntry(toKey)));
            }
        }

        public SortedMap<K,V> subMap(K fromKey, K toKey) {
            if (!inRange2(fromKey))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange2(toKey))
                throw new IllegalArgumentException("toKey out of range");
            return new SubMap(fromKey, toKey);
        }

        public SortedMap<K,V> headMap(K toKey) {
            if (!inRange2(toKey))
                throw new IllegalArgumentException("toKey out of range");
            return new SubMap(fromKey, toKey);
        }

        public SortedMap<K,V> tailMap(K fromKey) {
            if (!inRange2(fromKey))
                throw new IllegalArgumentException("fromKey out of range");
            return new SubMap(fromKey, toKey);
        }

        protected boolean inRange(K key) {
            return (fromKey == null || keyAnalyzer.compare(key, fromKey) >= 0) &&
                   (toKey   == null || keyAnalyzer.compare(key, toKey)   <  0);
        }

        // This form allows the high endpoint (as well as all legit keys)
        protected boolean inRange2(K key) {
            return (fromKey == null || keyAnalyzer.compare(key, fromKey) >= 0) &&
                   (toKey   == null || keyAnalyzer.compare(key, toKey)   <= 0);
        }
    }
}
