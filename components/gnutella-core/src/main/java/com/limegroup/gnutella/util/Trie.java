package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

/**
 * An information reTRIEval tree, a.k.a., a prefix tree.  A Trie is similar to a
 * dictionary in that it maps a key to a single value.  Unlike a dictionary,
 * keys must be strings.  Furthermore, Trie provides an efficient means
 * (getPrefixedBy()) to find all values given just a PREFIX of a key.<p>
 *
 * All retrieval operations run in O(nm) time, where n is the size of the
 * key/prefix and m is the size of the alphabet.  Some implementations may
 * reduce this to O(n log m) or even O(n) time.  Insertion operations are
 * assumed to be infrequent and may be slower.  The space required is roughly
 * linear with respect to the sum of the sizes of all keys in the tree, though
 * this may be reduced if many keys have common prefixes.<p>
 *
 * The Trie can be set to ignore case.  Doing so is the same as making all
 * keys and prefixes lower case.  That means the original keys cannot be
 * extracted from the Trie.<p>
 * 
 * Restrictions:
 * <ul>
 * <li><b>This class is not synchronized.</b>  Do that externally if you desire.
 * <li>Keys and values may not be null.
 * <li>The interface to this is not complete.  It would even be possible
 *   to make this implement Map, though the semantics would have to be bent
 *   somewhat.
 * </ul> 
 */
public class Trie {
    private TrieNode root;
    private boolean ignoreCase=false;    
    
    /**
     * Constructs a new, empty tree.
     * Case is ignored in storing and retrieving keys iff ignoreCase==true.     
     */
    public Trie(boolean ignoreCase) {
        this.ignoreCase=ignoreCase;
        clear();
    }

    /** 
     * Makes this empty.
     * @modifies this
     */
    public void clear() {
        this.root=new TrieNode();
    }
    
    /** Returns the canonical version of the given character. */
    private final char canonicalCase(char c) {
        return ignoreCase ? Character.toLowerCase(c) : c;
    }

    /**
     * Maps the given key to the given value.  Returns the old mapping
     * for key, if any, or null otherwise.  Key may be the empty string.
     *
     * @requires value!=null
     * @modifies this
     */
    public Object put(String key, Object value) {
        TrieNode node=root;
        int i=0;
        //1. Find the largest prefix of key, key[0..i-1], already in this.
        for ( ; i<key.length(); i++) {
            TrieNode child=node.get(canonicalCase(key.charAt(i)));
            if (child==null) 
                break;
            node=child;
        }
        //2. Insert additional new nodes (if any) for key[i..].
        for ( ; i<key.length(); i++) {
            TrieNode child=new TrieNode();
            node.put(canonicalCase(key.charAt(i)), child);
            node=child;
        }
        //3. Setup value and return old value, if any.
        Object ret=node.getValue();
        node.setValue(value);
        return ret;
    }

    /**
     * Returns the mapping for the given key, or null if no mapping.
     */
    public Object get(String key) {
        TrieNode node=root;
        for (int i=0; i<key.length(); i++) {
            node=node.get(canonicalCase(key.charAt(i)));
            if (node==null) 
                return null;
        }
        return node.getValue();
    }

    /**
     * Returns an iterator (of Object) of the values mapped by the given prefix,
     * in any order.  That is, the returned iterator contains exactly the values
     * v for which there exists a key k s.t. k.startsWith(prefix) and get(k)==v.
     *
     * @requires this not modified while iterator in use.  
     */
    public Iterator getPrefixedBy(String prefix) {
        return getPrefixedBy(prefix, 0, prefix.length());
    }

    /**
     * Exactly like getPrefixedBy(String), except that only
     * prefix[startsOffset...stopOffset-1] (inclusive) is considered the
     * prefix. This is useful as an optimization in certain applications to
     * avoid allocations.
     *
     * @requires 0<=startOffset<=stopOffset<=prefix.length
     */
    public Iterator getPrefixedBy(String prefix,
                                  int startOffset, int stopOffset) {
        TrieNode node=root;
        //1. Find first node start with prefix in tree.
        for (int i=startOffset; i<stopOffset; i++) {
            node=node.get(canonicalCase(prefix.charAt(i)));
            if (node==null) 
                return new EmptyIterator();
        }
        //2. Find all non-null values in children of node via DFS, putting them
        //into a temporary list.  Note: if we expect the caller to only iterate
        //through some of the values, we can do this work incrementally.
        //Otherwise, it's not worth the trouble.
        LinkedList /* of TrieNode */ queue=new LinkedList();
        LinkedList /* of Object */ values=new LinkedList();
        queue.addLast(node);
        while (queue.size() > 0) {
            node=(TrieNode)queue.removeLast();
            //TODO3: avoid allocating the iterator here.
            for (Iterator iter=node.children(); iter.hasNext(); ) {
                queue.addLast(iter.next());
            }
            Object value=node.getValue();
            if (value!=null)
                values.add(value);
        }
        //3. Return iterator of temporary list.
        return values.iterator();
    }   

    /** Yields nothing. */
    private static class EmptyIterator extends UnmodifiableIterator {
        public boolean hasNext() { return false; }
        public Object next() { throw new NoSuchElementException(); }
    }

    public String toString() {
        StringBuffer buf=new StringBuffer("[");
        /** INVARIANT: queue.size()==prefix.size(), and queue[i]
         *  is reachable by following the path prefixes[i] from root. */
        LinkedList /* of TrieNode */ queue=new LinkedList();
        LinkedList /* of String */ prefixes=new LinkedList();
        queue.addLast(root);
        prefixes.addLast("");
        /** True iff we've added one pair to buf.  Used to control ", " */
        boolean gotKey=false;
        while (queue.size() > 0) {
            TrieNode node=(TrieNode)queue.removeLast();
            String prefix=(String)prefixes.removeLast();
            for (Iterator iter=node.labels(); iter.hasNext(); ) {
                Character c=(Character)iter.next();
                TrieNode child=node.get(c.charValue());
                queue.addLast(child);
                prefixes.addLast(prefix+c);
            }
            Object value=node.getValue();
            if (value!=null) {
                if (gotKey) 
                    buf.append(", ");
                gotKey=true;
                buf.append(prefix.toString());
                buf.append("->");
                buf.append(value.toString());
            }
        }
        buf.append("]");
        return buf.toString();
    }
   
    /** Unit test. */
    public static void main(String args[]) {
        TrieNode.unitTest();

        Trie t=new Trie(false);
        Object anVal0="old value for an";
        Object anVal="value for an";
        Object antVal="value for ant";
        Object addVal="value for add";
        Object aVal="value for a";
        Iterator iter=null;
        Object tmp1=null, tmp2=null, tmp3=null, tmp4=null;

        Assert.that(t.get("a")==null);        

        Assert.that(t.put("an", anVal0)==null);
        Assert.that(t.put("an", anVal)==anVal0); //overwrite mapping
        Assert.that(t.put("ant", antVal)==null);
        Assert.that(t.put("add", addVal)==null);
        Assert.that(t.put("a", aVal)==null);
        System.out.println(t.toString());

        Assert.that(t.get("a")==aVal);
        Assert.that(t.get("an")==anVal);
        Assert.that(t.get("ant")==antVal);
        Assert.that(t.get("add")==addVal);        
        Assert.that(t.get("aDd")==null);        

        //Yield no elements...
        iter=t.getPrefixedBy("ab");
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) { }

        //Yield 1 element...
        iter=t.getPrefixedBy("ant");
        tmp1=iter.next();
        Assert.that(tmp1==antVal, tmp1.toString());
        Assert.that(! iter.hasNext());

        //Yield many elements...
        iter=t.getPrefixedBy("a");
        tmp1=iter.next();
        Assert.that(tmp1==aVal || tmp1==anVal || tmp1==addVal || tmp1==antVal);
        tmp2=iter.next();
        Assert.that(tmp2==aVal || tmp2==anVal || tmp2==addVal || tmp2==antVal);
        Assert.that(tmp2!=tmp1);
        tmp3=iter.next();
        Assert.that(tmp3==aVal || tmp3==anVal || tmp3==addVal || tmp3==antVal);
        Assert.that(tmp3!=tmp1);
        Assert.that(tmp3!=tmp2);
        tmp4=iter.next();
        Assert.that(tmp4==aVal || tmp4==anVal || tmp4==addVal || tmp4==antVal);
        Assert.that(tmp4!=tmp1);
        Assert.that(tmp4!=tmp2);
        Assert.that(tmp4!=tmp3);
        Assert.that(! iter.hasNext());

        //Empty string
        t=new Trie(false);
        Assert.that(t.get("")==null);
        t.put("", aVal);
        t.put("an", anVal);
        Assert.that(t.get("")==aVal);
        iter=t.getPrefixedBy("");
        tmp1=iter.next();
        Assert.that(tmp1==aVal || tmp1==anVal);
        tmp2=iter.next();
        Assert.that(tmp2==aVal || tmp2==anVal);
        Assert.that(tmp1!=tmp2);

        //Case insensitive tests
        t=new Trie(true);
        Assert.that(t.put("an", anVal)==null);
        Assert.that(t.put("An", anVal)==anVal);        
        Assert.that(t.put("aN", anVal)==anVal);        
        Assert.that(t.put("AN", anVal)==anVal);        
        Assert.that(t.get("an")==anVal);
        Assert.that(t.get("An")==anVal);
        Assert.that(t.get("aN")==anVal);
        Assert.that(t.get("AN")==anVal);
        Assert.that(t.put("ant", antVal)==null);
        Assert.that(t.get("ANT")==antVal);
        iter=t.getPrefixedBy("a");
        Assert.that(iter.next()==anVal);
        Assert.that(iter.next()==antVal);
        Assert.that(! iter.hasNext());      

        //Prefix tests
        t=new Trie(false);
        Assert.that(t.put("an", anVal)==null);
        iter=t.getPrefixedBy("XanXX");
        Assert.that(! iter.hasNext());
        iter=t.getPrefixedBy("XanXX", 1, 4);
        Assert.that(! iter.hasNext());
        iter=t.getPrefixedBy("XanXX", 1, 3);
        Assert.that(iter.next()==anVal);
        Assert.that(! iter.hasNext());
        iter=t.getPrefixedBy("XanXX", 1, 2);
        Assert.that(iter.next()==anVal);
        Assert.that(! iter.hasNext());
    }
}



/** 
 * A node of the Trie.  Maps characters to children.  Different implementations
 * may trade space for time.<p>
 *
 * Design note: this is a "dumb" class.  It is <i>only</i> responsible for
 * managing its value and its children.  None of its operations are recursive;
 * that is Trie's job.  Nor does it deal with case.
 */
class TrieNode {
    private static class TrieNodePair implements Comparable {
        char c;
        TrieNode node;
        
        TrieNodePair(char c, TrieNode node) {
            this.c=c;
            this.node=node;
        }

        public int compareTo(Object other) {
            return this.c-((TrieNodePair)other).c;
        }

        public boolean equals(Object other) {
            if (! (other instanceof TrieNode))
                return false;
            return this.c==((TrieNodePair)other).c;
        }
    }

    /** The value of this node. */
    private Object value=null;
    /** The list of children.  INVARIANT: sorted by character. */
    private TrieNodePair[] children=new TrieNodePair[0];

    /** Gets the value associated with this node, or null if none. */
    Object getValue() {
        return value;
    }

    /** Sets the value associated with this node. */
    void setValue(Object value) {
        this.value=value;
    }

    /** Returns the child for the given character, or null if none. */
    TrieNode get(char c) {
        //TODO3: avoid allocation of dummy by implementing binary search here.
        TrieNodePair dummy=new TrieNodePair(c, null);
        int i=Arrays.binarySearch(children, dummy);
        if (i<0)
            return null;
        TrieNodePair pair=(TrieNodePair)children[i];
        Assert.that(pair.c==c);
        return pair.node;
    }        

    /** Maps the given character to the given node.
     *  This is much slower than get(char).
     *  @requires c not already mapped to a node. */
    void put(char c, TrieNode node) {
        //It's possible to simply insert the node into children instead of
        //sorting.  That reduces the time from O(n log n) to O(n), where 
        //n==children.size().  But that requires a more flexible data
        //structure to store the children. 
        TrieNodePair[] children2=new TrieNodePair[children.length+1];
        System.arraycopy(children, 0, children2, 0, children.length);
        children2[children2.length-1]=new TrieNodePair(c, node);
        Arrays.sort(children2);
        children=children2;
    }

    /** Returns the children of this in any order, as an iterator
     *  of TrieNode. */
    Iterator children() {
        return new ChildrenIterator();
    }

    /** Returns the labels of the children of this in any order, as an iterator
     *  of Character. */
    Iterator labels() {
        return new LabelIterator();
    }

    /** Maps (lambda (pair) pair.node) on children.iterator() */
    private class ChildrenIterator extends UnmodifiableIterator {
        int i=0;
        public boolean hasNext() {
            return i<children.length;
        }
        public Object next() {
            if (! hasNext())
                throw new NoSuchElementException();

            TrieNodePair pair=(TrieNodePair)children[i];
            i++;
            return pair.node;
        }
    }

    /** Maps (lambda (pair) pair.c) on children.iterator() */
    private class LabelIterator extends UnmodifiableIterator {
        int i=0;
        public boolean hasNext() {
            return i<children.length;
        }
        public Object next() {
            if (! hasNext())
                throw new NoSuchElementException();

            TrieNodePair pair=(TrieNodePair)children[i];
            i++;
            return new Character(pair.c);
        }
    }

    static void unitTest() {
        TrieNode node=new TrieNode();
        TrieNode childA=new TrieNode();
        TrieNode childB=new TrieNode();
        
        Assert.that(node.getValue()==null);
        String value="abc";
        node.setValue(value);
        Assert.that(node.getValue()==value);

        Assert.that(node.get('a')==null);
        Iterator iter=node.children();
        Assert.that(! iter.hasNext());

        node.put('b', childB);
        Assert.that(node.get('b')==childB);
        node.put('a', childA);
        Assert.that(node.get('a')==childA);
        Assert.that(node.get('b')==childB);
        Assert.that(node.get('c')==null);

        iter=node.children();
        TrieNode tmp=(TrieNode)iter.next();
        Assert.that(tmp==childA || tmp==childB);
        TrieNode tmp2=(TrieNode)iter.next();
        Assert.that(tmp2==childA || tmp2==childB);
        Assert.that(tmp2!=tmp);
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) {
        }
    }
}

