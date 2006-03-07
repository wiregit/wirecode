/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An optimized PATRICIA Trie for Kademlia. That means I
 * removed some unused stuff like support for 0bit Keys
 * which requires some extra steps.
 * 
 * @author Roger Kapsi
 */
public class PatriciaTrie {
    
    private static final int NULL_BIT_KEY = -1;
    private static final int EQUAL_BIT_KEY = -2;
    
    private final Entry root = new Entry(null, null, -1);
    
    private int size = 0;
    private int modCount = 0;
    
    private final KeyCreator keyCreator;
    
    public PatriciaTrie() {
        this(new KUIDKeyCreator());
    }
    
    public PatriciaTrie(KeyCreator keyCreator) {
        this.keyCreator = keyCreator;
    }
    
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
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
   
    private void incrementSize() {
        size++;
        incrementModCount();
    }
    
    private void decrementSize() {
        size--;
        incrementModCount();
    }
    
    private void incrementModCount() {
        modCount++;
    }
    
    public Object put(Object key, Object value) {
        
        Entry found = getR(root.left, -1, key);
        if (key.equals(found.key)) {
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
            throw new IndexOutOfBoundsException("Null bit keys are not supported");
        } else if (isEqualBitKey(bitIndex)) { // actually not possible 
            /* REPLACE OLD KEY+VALUE */
            if (found != root) {
                incrementModCount();
                return found.setKeyValue(key, value);
            }
        }
        
        throw new IndexOutOfBoundsException("Failed to put: " + key + " -> " + value + ", " + bitIndex);
    }
    
    private Entry putR(Entry h, Entry t, Entry p) {
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
    
    public Object get(Object key) {
        Entry entry = getR(root.left, -1, key);
        return (key.equals(entry.key) ? entry.value : null);
    }
    
    public Object getBest(Object key) {
        Entry entry = getR(root.left, -1, key);
        return (!isRoot(entry) ? entry.value : null);
    }
    
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
    
    /** 
     * Returns a List of buckts sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes by last-recently 
     * and most-recently seen.
     */
    public List getBest(Object key, int k) {
        return getBest(key, Collections.EMPTY_SET, k);
    }
    
    public List getBest(Object key, Collection exclude, int k) {
        int initialSize = (int)Math.min(k, size());
        List list = new ArrayList(initialSize);
        if (exclude == null) {
            exclude = Collections.EMPTY_SET;
        }
        getBestR(root.left, -1, key, exclude, list, k);
        return list;
    }
    
    private boolean getBestR(Entry h, int bitIndex, 
            final Object key, final Collection exclude, 
            final List list, final int k) {
        
        if (h.bitIndex <= bitIndex) {
            if (!isRoot(h) && !exclude.contains(h.key)) {
                list.add(h.value);
            }
            return list.size() < k;
        }

        if (!isBitSet(key, h.bitIndex)) {
            if (getBestR(h.left, h.bitIndex, key, exclude, list, k)) {
                return getBestR(h.right, h.bitIndex, key, exclude, list, k);
            }
        } else {
            if (getBestR(h.right, h.bitIndex, key, exclude, list, k)) {
                return getBestR(h.left, h.bitIndex, key, exclude, list, k);
            }
        }
        return false;
    }
    
    public boolean containsKey(Object key) {
        Entry entry = getR(root.left, -1, key);
        return key.equals(entry.key);
    }
    
    public Object remove(Object key) {
        return removeR(root.left, -1, key, root);
    }
    
    private Object removeR(Entry h, int bitIndex, Object key, Entry p) {
        if (h.bitIndex <= bitIndex) {
            if (key.equals(h.key)) {
                return removeNode(h, p);
            }
            return null;
        }
        
        if (!isBitSet(key, h.bitIndex)) {
            return removeR(h.left, h.bitIndex, key, h);
        } else {
            return removeR(h.right, h.bitIndex, key, h);
        }
    }
    
    private Object removeNode(Entry h, Entry p) {

        if (h.isInternalNode()) {
            removeInternalNode(h, p);
        } else {
            removeExternalNode(h);
        }
        
        decrementSize();
        return h.setKeyValue(null, null);
    }
    
    private void removeExternalNode(Entry h) {
        if (isRoot(h)) {
            throw new IllegalArgumentException("Cannot delete root Node!");
        } else if (!h.isExternalNode()) {
            throw new IllegalArgumentException(h + " is not an external Node!");
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
    
    private void removeInternalNode(Entry h, Entry p) {
        if (isRoot(h)) {
            throw new IllegalArgumentException("Cannot delete root Node!");
        } else if (!h.isInternalNode()) {
            throw new IllegalArgumentException(h + " is not an internal Node!");
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
        buffer.append("RoutingTable[").append(size()).append("]={\n");
        toStringR(root.left, -1, buffer);
        buffer.append("}\n");
        return buffer.toString();
    }
    
    private StringBuffer toStringR(Entry h, int bitIndex, final StringBuffer buffer) {

        if (h.bitIndex <= bitIndex) {
            if (!isRoot(h)) {
                buffer.append("  ").append(h.toString()).append("\n");
            }
            return buffer;
        }

        toStringR(h.left, h.bitIndex, buffer);
        return toStringR(h.right, h.bitIndex, buffer);
    }
    
    public List values() {
        return valuesR(root.left, -1, new ArrayList(size()));
    }
    
    private List valuesR(Entry h, int bitIndex, final List list) {
        if (h.bitIndex <= bitIndex) {
            if (!isRoot(h)) {
                list.add(h.value);
            }
            return list;
        }
        
        valuesR(h.left, h.bitIndex, list);
        return valuesR(h.right, h.bitIndex, list);
    }
    
    private static boolean isValidBitIndex(int bitIndex) {
        return 0 <= bitIndex && bitIndex <= Integer.MAX_VALUE;
    }
    
    private static boolean isNullBitKey(int bitIndex) {
        return bitIndex == NULL_BIT_KEY;
    }
    
    private static boolean isEqualBitKey(int bitIndex) {
        return bitIndex == EQUAL_BIT_KEY;
    }
    
    private boolean isBitSet(Object key, int bitIndex) {
        if (key == null) { // root's key is null!
            return false;
        }
        return keyCreator.isBitSet(key, bitIndex);
    }
    
    private int bitIndex(Object key, Object foundKey) {
        boolean nullKey = true;
        for(int bitIndex = 0; bitIndex < keyCreator.length(); bitIndex++) {
            boolean isBitSet = isBitSet(key, bitIndex);
            if (isBitSet != isBitSet(foundKey, bitIndex)) {
                return bitIndex;
            }

            if (isBitSet) {
                nullKey = false;
            }
        }
        
        if (nullKey) {
            return NULL_BIT_KEY;
        }
        return EQUAL_BIT_KEY;
    }
    
    private boolean isRoot(Entry entry) {
        return root == entry;
    }
    
    private final class Entry implements Map.Entry {
        
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
        
        private Object setKeyValue(Object key, Object value) {
            this.key = key;
            return setValue(value);
        }
        
        private boolean isInternalNode() {
            return left != this && right != this;
        }
        
        private boolean isExternalNode() {
            return !isInternalNode();
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            
            if (root == this) {
                buffer.append("RootNode(");
            } else {
                buffer.append("Node(");
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
    
    public static interface KeyCreator {
        public int length();
        public boolean isBitSet(Object key, int bitIndex);
    }
}