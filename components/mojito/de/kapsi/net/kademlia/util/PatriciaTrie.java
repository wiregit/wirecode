/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An optimized PATRICIA Trie for Kademlia. 
 * 
 * @author Roger Kapsi
 */
public class PatriciaTrie implements Serializable {
    
    private static final long serialVersionUID = 110232526181493307L;

    private final Entry root = new Entry(null, null, -1);
    
    private int size = 0;
    private transient int modCount = 0;
    
    private final KeyCreator keyCreator;
    
    public PatriciaTrie() {
        this(new KUIDKeyCreator());
    }
    
    public PatriciaTrie(KeyCreator keyCreator) {
        this.keyCreator = keyCreator;
    }
    
    /**
     * Clears the Trie (i.e. removes all emelemnts).
     */
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
    
    /**
     * Returns true if the Trie is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Returns the number items in the Trie
     */
    public int size() {
        return size;
    }
   
    /**
     * Increments both, the size and mod counter.
     */
    private void incrementSize() {
        size++;
        incrementModCount();
    }
    
    /**
     * Decrements the size and increments the mod
     * counter.
     */
    private void decrementSize() {
        size--;
        incrementModCount();
    }
    
    /**
     * Increments the mod counter. It's currently
     * unused but it will be useful to detect
     * concurrent modifications on the Trie (e.g.
     * as thrown by Iterators).
     */
    private void incrementModCount() {
        modCount++;
    }
    
    /**
     * Adds a new <key, value> pair to the Trie and if a pair already
     * exists it will be replaced. In the latter case it will return
     * the old value.
     */
    public Object put(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        
        Entry found = getR(root.left, -1, key);
        if (key.equals(found.key)) {
            if (/*found == root */ found.isEmpty()) {
                incrementSize();
            } else {
                incrementModCount();
            }
            return found.setKeyValue(key, value);
        }
        
        int bitIndex = bitIndex(key, found.key);
        if (isValidBitIndex(bitIndex)) { // in 99.999...9% the case
            /* NEW KEY+VALUE TUPLE */
            Entry t = new Entry(key, value, bitIndex);
            root.left = putR(root.left, t, root);
            incrementSize();
            return null;
        } else if (isNullBitKey(bitIndex)) { // all 160bits are 0
            /* NULL BIT KEY */
            if (root.isEmpty()) {
                incrementSize();
            } else {
                incrementModCount();
            }
            return root.setKeyValue(key, value);
        } else if (isEqualBitKey(bitIndex)) { // actually not possible 
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
    private Entry putR(Entry h, final Entry t, Entry p) {
        if ((h.bitIndex >= t.bitIndex) || (h.bitIndex <= p.bitIndex)) {
            
            if (!isBitSet(t.key, t.bitIndex)) {
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
            
            return t;
        }

        if (!isBitSet(t.key, h.bitIndex)) {
            h.left = putR(h.left, t, h);
        } else {
            h.right = putR(h.right, t, h);
        }
        
        return h;
    }
    
    /**
     * Returns the Value whose Key equals our lookup Key
     * or null if no such key exists.
     */
    public Object get(Object key) {
        Entry entry = getR(root.left, -1, key);
        return (!entry.isEmpty() && key.equals(entry.key) ? entry.value : null);
    }
    
    /**
     * The actual get implementation. This is very similar to
     * selectR but with the exception that it might return the
     * root Entry even if it's empty.
     */
    private Entry getR(Entry h, int bitIndex, Object key) {
        if (h.bitIndex <= bitIndex) {
            return h;
        }

        if (!isBitSet(key, h.bitIndex)) {
            return getR(h.left, h.bitIndex, key);
        } else {
            return getR(h.right, h.bitIndex, key);
        }
    }
  
    public Object selectNextClosest(Object key) {
        List list = select(key, 2);
        return list.get(list.size()-1);
    }
    
    /**
     * Returns the Value whose Key has the longest prefix
     * in common with our lookup key.
     */
    public Object select(Object key) {
        List values = select(key, 1);
        return values.isEmpty() ? null : values.get(0);
    }
    
    /**
     * Selects a single best matching Entry. Very similar to getR
     * but we have to cheat if the root Entry has no <key,value>
     * associated with it.
     */
    /*private Entry selectR(Entry h, int bitIndex, final Object key, Entry p) {
        if (h.bitIndex <= bitIndex) {
            return (h.isEmpty() ? p : h);
        }

        if (!isBitSet(key, h.bitIndex)) {
            return selectR(h.left, h.bitIndex, key, h);
        } else {
            return selectR(h.right, h.bitIndex, key, h);
        }
    }*/
    
    /** 
     * Returns a List of buckts sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes by last-recently 
     * and most-recently seen.
     */
    public List select(Object key, int count) {
        return select(key, count, new KeySelector() {
            public boolean allow(Object key, Object value) {
                return true;
            }
        });
    }
    
    public List select(Object key, int count, KeySelector keySelector) {
        List list = new ArrayList(count);
        selectR(root.left, -1, key, list, count, keySelector);
        return list;
    }

    private boolean selectR(Entry h, int bitIndex, 
            final Object key, 
            final List list, 
            final int count, 
            final KeySelector keySelector) {
        
        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty() && keySelector.allow(h.key, h.value)) {
                list.add(h.value);
            }
            return list.size() < count;
        }

        if (!isBitSet(key, h.bitIndex)) {
            if (selectR(h.left, h.bitIndex, key, list, count, keySelector)) {
                return selectR(h.right, h.bitIndex, key, list, count, keySelector);
            }
        } else {
            if (selectR(h.right, h.bitIndex, key, list, count, keySelector)) {
                return selectR(h.left, h.bitIndex, key, list, count, keySelector);
            }
        }
        return false;
    }
    
    /**
     * Returns all values as List whose keys have the same
     * prefix as the provided key from the 0th bit to length-th bit
     * 
     * @param length (depth) in bits
     */
    public List range(Object key, int length) {
        return range(key, length, new KeySelector() {
            public boolean allow(Object key, Object value) {
                return true;
            }
        });
    }
    
    public List range(Object key, int length, KeySelector keySelector) {
        
        // If length is -1 then return everything!
        if (length == -1) {
            return values();
        }
        
        if (length >= keyCreator.length()) {
            throw new IllegalArgumentException(length + " >= " + keyCreator.length());
        }
        
        Entry entry = rangeR(root.left, -1, key, length, root);
        if (entry.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        
        // Found key's length-th bit differs from our
        // key which means it cannot be the prefix!
        if (isBitSet(key, length)
                != isBitSet(entry.key, length)) {
            return Collections.EMPTY_LIST;
        }
        
        if (length < entry.bitIndex) {
            //System.out.println("Has Subtree");
            return valuesInRangeR(entry, -1, keySelector, new ArrayList());
        } else {
            //System.out.println("Has No Subtree");
            if (keySelector.allow(entry.key, entry.value)) {
                return Arrays.asList(new Object[]{entry.value});
            } else {
                return Collections.EMPTY_LIST;
            }
        }
    }
    
    /**
     * This is very similar to getR but with the difference that
     * we stop the lookup if h.bitIndex > keyLength.
     */
    private Entry rangeR(Entry h, int bitIndex, 
            final Object key, final int keyLength, Entry p) {
        
        if (h.bitIndex <= bitIndex || keyLength < h.bitIndex) {
            return (h.isEmpty() ? p : h);
        }
        
        if (!isBitSet(key, h.bitIndex)) {
            return rangeR(h.left, h.bitIndex, key, keyLength, h);
        } else {
            return rangeR(h.right, h.bitIndex, key, keyLength, h);
        }
    }
    
    private List valuesInRangeR(Entry h, int bitIndex, 
            final KeySelector keySelector, final List values) {
        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty() 
                    && keySelector.allow(h.key, h.value)) {
                values.add(h.value);
            }
            return values;
        }
        
        valuesInRangeR(h.left, h.bitIndex, keySelector, values);
        return valuesInRangeR(h.right, h.bitIndex, keySelector, values);
    }
    
    /**
     * Returns true if this trie contains the specified Key
     */
    public boolean containsKey(Object key) {
        Entry entry = getR(root.left, -1, key);
        return !entry.isEmpty() && key.equals(entry.key);
    }
    
    /**
     * Removes a Key from the Trie if one exists
     * 
     * @param key the Key to delete
     * @return Returns the deleted Value
     */
    public Object remove(Object key) {
        return removeR(root.left, -1, key, root);
    }
    
    /**
     * Part 1
     * 
     * Serach for the Key
     */
    private Object removeR(Entry h, int bitIndex, Object key, Entry p) {
        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty() && key.equals(h.key)) {
                return removeEntry(h, p);
            }
            return null;
        }
        
        if (!isBitSet(key, h.bitIndex)) {
            return removeR(h.left, h.bitIndex, key, h);
        } else {
            return removeR(h.right, h.bitIndex, key, h);
        }
    }
    
    /**
     * Part 2
     * 
     * If we found a Key (Entry h) then figure out if it's
     * an internal (hard to remove) or external Entry (easy 
     * to remove)
     */
    private Object removeEntry(Entry h, Entry p) {
        
        if (h != root) {
            if (h.isInternalNode()) {
                removeInternalEntry(h, p);
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
    private void removeExternalEntry(Entry h) {
        if (h == root) {
            throw new IllegalArgumentException("Cannot delete root Entry!");
        } else if (!h.isExternalNode()) {
            throw new IllegalArgumentException(h + " is not an external Entry!");
        } 
        
        Entry parent = h.parent;
        Entry child = (h.left == h) ? h.right : h.left;
        
        if (parent.left == h) {
            h.parent.left = child;
        } else {
            h.parent.right = child;
        }
        
        if (child.bitIndex > parent.bitIndex) {
            child.parent = parent;
        }
    }
    
    /**
     * Part 4
     * 
     * If it's an internal Entry then "good luck" with understanding
     * this code. The Idea is essentially that Entry p takes Entry h's
     * place in the trie which requires some re-wireing.
     */
    private void removeInternalEntry(Entry h, Entry p) {
        if (h == root) {
            throw new IllegalArgumentException("Cannot delete root Entry!");
        } else if (!h.isInternalNode()) {
            throw new IllegalArgumentException(h + " is not an internal Entry!");
        } 
        
        // Set P's bitIndex
        p.bitIndex = h.bitIndex;
        
        // Fix P's parent and child Nodes
        {
            Entry parent = p.parent;
            Entry child = (p.left == h) ? p.right : p.left;
            
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
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Trie[").append(size()).append("]={\n");
        toStringR(root.left, -1, buffer);
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private StringBuffer toStringR(Entry h, int bitIndex, 
            final StringBuffer buffer) {

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
     * Returns all Keys as List. You can think of it as
     * a Set since there're no duplicate keys.
     */
    public List keys() {
        return keysR(root.left, -1, new ArrayList(size()));
    }
    
    /**
     * The actual keys() implementation. Just an inorder traverse
     */
    private List keysR(Entry h, int bitIndex, final List keys) {
        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty()) {
                keys.add(h.key);
            }
            return keys;
        }
        
        keysR(h.left, h.bitIndex, keys);
        return keysR(h.right, h.bitIndex, keys);
    }
    
    /**
     * Returns all Values
     */
    public List values() {
        return valuesR(root.left, -1, new ArrayList(size()));
    }
    
    /**
     * The actual values() implementation. Just an inorder traverse
     */
    private List valuesR(Entry h, int bitIndex, final List values) {
        if (h.bitIndex <= bitIndex) {
            if (!h.isEmpty()) {
                values.add(h.value);
            }
            return values;
        }
        
        valuesR(h.left, h.bitIndex, values);
        return valuesR(h.right, h.bitIndex, values);
    }
    
    /** Helper method. Returns true if bitIndex is a valid index */
    private static boolean isValidBitIndex(int bitIndex) {
        return 0 <= bitIndex && bitIndex <= Integer.MAX_VALUE;
    }
    
    /** Helper method. Returns true if bitIndex is a NULL_BIT_KEY */
    private static boolean isNullBitKey(int bitIndex) {
        return bitIndex == KeyCreator.NULL_BIT_KEY;
    }
    
    /** Helper method. Returns true if bitIndex is a EQUAL_BIT_KEY */
    private static boolean isEqualBitKey(int bitIndex) {
        return bitIndex == KeyCreator.EQUAL_BIT_KEY;
    }
    
    private boolean isBitSet(Object key, int bitIndex) {
        if (key == null) { // root's might be null!
            return false;
        }
        return keyCreator.isBitSet(key, bitIndex);
    }
    
    private int bitIndex(Object key, Object foundKey) {
        return keyCreator.bitIndex(key, foundKey);
    }
    
    /**
     * The actual Trie nodes.
     */
    private final class Entry implements Map.Entry, Serializable {
        
        private static final long serialVersionUID = 4596023148184140013L;
        
        private Object key;
        private Object value;
        
        private int bitIndex;
        
        private Entry parent;
        private Entry left;
        private Entry right;
        
        private Entry(Object key, Object value, int bitIndex) {
            this.key = key;
            this.value = value;
            
            this.bitIndex = bitIndex;
            
            this.parent = null;
            this.left = this;
            this.right = null;
        }
        
        /**
         * This can be only the case with the root node!
         */
        public boolean isEmpty() {
            return key == null;
        }
        
        public Object getKey() {
            return key;
        }
        
        public Object getValue() {
            return value;
        }
        
        public Object setValue(Object value) {
            Object o = this.value;
            this.value = value;
            return o;
        }
        
        /** 
         * Replaces the old key and value with the new ones.
         * Returns the old vlaue.
         */
        private Object setKeyValue(Object key, Object value) {
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
            StringBuffer buffer = new StringBuffer();
            
            if (root == this) {
                buffer.append("RootEntry(");
            } else {
                buffer.append("Entry(");
            }
            
            buffer.append("key=").append(key).append(" [").append(bitIndex).append("], ");
            buffer.append("value=").append(value).append(", ");
            //buffer.append("bitIndex=").append(bitIndex).append(", ");
            
            if (parent != null) {
                if (parent == root) {
                    buffer.append("parent=").append("ROOT");
                } else {
                    buffer.append("parent=").append(parent.key).append(" [").append(parent.bitIndex).append("]");
                }
            } else {
                buffer.append("parent=").append("null");
            }
            buffer.append(", ");
            
            if (left != null) {
                if (root == left) {
                    buffer.append("left=").append("ROOT");
                } else {
                    buffer.append("left=").append(left.key).append(" [").append(left.bitIndex).append("]");
                }
            } else {
                buffer.append("left=").append("null");
            }
            buffer.append(", ");
            
            if (right != null) {
                if (root == right) {
                    buffer.append("right=").append("ROOT");
                } else {
                    buffer.append("right=").append(right.key).append(" [").append(right.bitIndex).append("]");
                }
            } else {
                buffer.append("right=").append("null");
            }
            
            buffer.append(")");
            return buffer.toString();
        }
    }
    
    /**
     * The interface used by PatriciaTrie to access the Keys
     * on bit level.
     */
    public static interface KeyCreator extends Serializable {
        
        /** Returned by bitIndex if key's bits are all 0 */
        public static final int NULL_BIT_KEY = -1;
        
        /** 
         * Returned by bitIndex if key and found key are
         * equal. This is a very very specific case and
         * shouldn't happen on a regular basis
         */
        public static final int EQUAL_BIT_KEY = -2;
        
        /** Returns the length of the Key in bits. */
        public int length();
        
        /** Returns weather or not a bit is set */
        public boolean isBitSet(Object key, int bitIndex);
        
        /** Returns the n-th different bit between key and found */
        public int bitIndex(Object key, Object found);
    }
    
    public static interface KeySelector {
        public boolean allow(Object key, Object value);
    }
}
