package com.limegroup.gnutella.util;

import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

/**
 * An information reTRIEval tree, a.k.a., a prefix tree.  A Trie is similar to a
 * dictionary, except that keys must be strings.  Furthermore, Trie provides an
 * efficient means (getPrefixedBy()) to find all values given just a PREFIX of a
 * key.<p>
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
 * Restrictions (not necessarily limitations!)
 * <ul>
 * <li><b>This class is not synchronized.</b>  Do that externally if you desire.
 * <li>Keys and values may not be null.
 * <li>The interface to this is not complete.
 * </ul>
 *
 * See http://www.csse.monash.edu.au/~lloyd/tildeAlgDS/Tree/Trie.html for a
 * discussion of Tries.
 */
public class Trie {
    /**
     * Our representation consists of a tree of nodes whose edges are labelled
     * by strings.  The first characters of all labels of all edges of a node
     * must be distinct.  Typically the edges are sorted, but this is determined
     * by TrieNode.<p>
     *
     * An abstract TrieNode is a mapping from String keys to values,
     * { <K1, V1>, ..., <KN, VN> }, where all Ki and Kj are distinct for i!=j.
     * For any node N, define KEY(N) to be the concatentation of all labels on
     * the edges from the root to that node.  Then the abstraction function is
     *       { <KEY(N), N.getValue() | N is a child of root
     *                                 and N.getValue()!=null}
     *
     * An earlier version used character labels on edges.  This made
     * implementation simpler but used more memory because one node would be
     * allocated to each character in long strings if that string had no
     * common prefixes with other elements of the Trie.
     *
     * INVARIANT: for any node N, for any edges Ei and Ej from N,
     *   i!=j ==> Ei.getLabel().getCharAt(0)!=Ej.getLabel().getCharAt(0)
     * Also, all invariants for TrieNode and TrieEdge must hold.
     */
    private TrieNode root;
    /**
     * True iff case should be ignored during comparisons.
     *
     * INVARIANT: if ignoreCase==true, then canonicalCase(c)==c for all
     *   edges labelled c.
     */
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
     *     @modifies this
     */
    public void clear() {
        this.root=new TrieNode();
    }

    /** Returns the canonical version of the given character. */
    private final char canonicalCase(char c) {
        return ignoreCase ? Character.toLowerCase(c) : c;
    }

    /** Returns the canonical version of the given character. */
    private final String canonicalCase(String s) {
        if (ignoreCase)
            return s.toLowerCase();
        else
            return s;
    }

    /**
     *  Matches the pattern b against the text a[startOffset...stopOffset-1].
     *  Ignores case of a when matching if ignoreCase==true.
     *
     *  Returns the first j s.t. 0<=i<b.length() AND
     *          a[startOffset+j]!=b[j]      [a and b differ]
     *       OR stopOffset==startOffset+j   [a is undefined]
     *  Returns -1 if no such j exists, i.e., there is a match.
     *
     *  Examples:
     *    1) a="abcde", startOffset=0, stopOffset=5, b="abc"
     *              abcde   ==> returns -1
     *              abc
     *    2) a="abcde", startOffset=1, stopOffset=5, b="bXd"
     *              abcde   ==> returns 1
     *               bXd
     *    3) a="abcde", startOffset=1, stopOffset=3, b="bcd"
     *              abc     ==> returns 2
     *               bcd
     *
     *  @requires 0<=startOffset<=stopOffset<=a.length()
     */
    private final int match(String a, int startOffset, int stopOffset,
                            String b) {
        //j is an index into b
        //i is a parallel index into a
        int i=startOffset;
        for (int j=0; j<b.length(); j++) {
            if ( i>=stopOffset )
                return j;
            if (canonicalCase(a.charAt(i))!=b.charAt(j))
                return j;
            i++;
        }
        return -1;
    }


    /**
     * Maps the given key (which may be empty) to the given value.  Returns the
     * old value associated with key, or null if none.
     *     @requires value!=null
     *     @modifies this
     */
    public Object add(String key, Object value) {
        //Find the largest prefix of key, key[0..i-1], already in this.
        TrieNode node=root;
        int i=0;
        while (i<key.length()) {
            //Find the edge whose label starts with key[i].
            TrieEdge edge=node.get(canonicalCase(key.charAt(i)));
            if (edge==null) {
                //1) Additive insert.
                TrieNode newNode=new TrieNode(value);
                node.put(canonicalCase(key.substring(i)), newNode);
                return null;
            }

            //Now check that rest of label matches
            String label=edge.getLabel();
            int j=match(key, i, key.length(), label);
            Assert.that(j!=0, "Label didn't start with prefix[0].");
            if (j>=0) {
                //2) Prefix overlaps perfectly with just part of edge label
                //   Do split insert as follows...
                //
                //   node          node         ab = label
                // ab |      =>   a |           a  = label[0...j-1] (inclusive)
                //  child      intermediate     b  = label[j...]    (inclusive)
                //              b /    \ c      c  = key[i+j...]    (inclusive)
                //             child  newNode
                //
                //   ...unless c="", in which case you just do a "splice insert"
                //   by ommiting newNew and setting intermediate's value.
                TrieNode child=edge.getChild();
                TrieNode intermediate=new TrieNode();
                String a=label.substring(0, j);
                //Assert.that(canonicalCase(a).equals(a), "Bad edge a");
                String b=label.substring(j);
                //Assert.that(canonicalCase(b).equals(b), "Bad edge a");
                String c=canonicalCase(key.substring(i+j));

                if (c.length() > 0) {
                    //Split.
                    TrieNode newNode=new TrieNode(value);
                    node.remove(label.charAt(0));
                    node.put(a, intermediate);
                    intermediate.put(b, child);
                    intermediate.put(c, newNode);
                } else {
                    //Splice.
                    node.remove(label.charAt(0));
                    node.put(a, intermediate);
                    intermediate.put(b, child);
                    intermediate.setValue(value);
                }
                return null;
            }

            //Prefix overlaps perfectly with all of edge label.  Keep searching.
            Assert.that(j==-1, "Bad return value from match: "+i);
            node=edge.getChild();
            i+=label.length();
        }

        //3. Relabel insert.  Prefix already in this, though not necessarily
        //associated with a value.
        Object ret=node.getValue();
        node.setValue(value);
        return ret;
    }


    /** Returns the node associated with prefix, or null if none. */
    private TrieNode fetch(String prefix) {
        TrieNode node=root;
        for (int i=0; i<prefix.length(); ) {
            //Find the edge whose label starts with prefix[i].
            TrieEdge edge=node.get(canonicalCase(prefix.charAt(i)));
            if (edge==null)
                return null;

            //Now check that rest of label matches
            String label=edge.getLabel();
            int j=match(prefix, i, prefix.length(), label);
            Assert.that(j!=0, "Label didn't start with prefix[0].");
            if (j!=-1)
                return null;

            i+=label.length();
            node=edge.getChild();
        }
        return node;
    }

    /**
     * Returns the value associated with the given key, or null if none.
     */
    public Object get(String key) {
        TrieNode node=fetch(key);
        if (node==null)
            return null;

        return node.getValue();
    }

    /**
     * Ensures no values are associated with the given key.  Returns true if
     * any values were actually removed.
     *     @modifies this
     */
    public boolean remove(String key) {
        //TODO2: prune unneeded nodes to save space
        TrieNode node=fetch(key);
        if (node==null)
            return false;

        boolean ret=node.getValue()!=null;
        node.setValue(null);
        return ret;
    }


    /**
     * Returns an iterator (of Object) of the values mapped by keys in this that
     * start with the given prefix, in any order.  That is, the returned
     * iterator contains exactly the values v for which there exists a key k
     * s.t. k.startsWith(prefix) and get(k)==v.  The remove() operation on the
     * iterator in unimplemented.
     *     @requires this not modified while iterator in use.
     */
    public Iterator getPrefixedBy(String prefix) {
        return getPrefixedBy(prefix, 0, prefix.length());
    }

    /**
     * Same as getPrefixedBy(prefix.substring(startOffset, stopOffset). This is
     * useful as an optimization in certain applications to avoid allocations.
     *
     * @requires 0<=startOffset<=stopOffset<=prefix.length
     */
    public Iterator getPrefixedBy(String prefix,
                                  int startOffset, int stopOffset) {
        //Find the first node for which "prefix" prefixes KEY(node).  (See the
        //implementation overview for a definition of KEY(node).) This code is
        //similar to fetch(prefix), except that if prefix extends into the
        //middle of an edge label, that edge's child is considered a match.
        TrieNode node=root;
        for (int i=startOffset; i<stopOffset; ) {
            //Find the edge whose label starts with prefix[i].
            TrieEdge edge=node.get(canonicalCase(prefix.charAt(i)));
            if (edge==null) {
                return new EmptyIterator();
            }

            //Now check that rest of label matches
            node=edge.getChild();
            String label=edge.getLabel();
            int j=match(prefix, i, stopOffset, label);
            Assert.that(j!=0, "Label didn't start with prefix[0].");
            if ((i+j)==stopOffset)
                //a) prefix overlaps perfectly with just part of edge label
                break;
            else if (j>=0) {
                //b) prefix and label differ at some point
                node=null;
                break;
            } else {
                //c) prefix overlaps perfectly with all of edge label.
                Assert.that(j==-1, "Bad return value from match: "+i);
            }

            i+=label.length();
        }

        //Yield all children of node, including node itself.
        if (node==null)
            return new EmptyIterator();
        else
            return new ValueIterator(node);
    }

    /** Returns all the (non-null) values associated with a given
     *  node and its children. */
    private class ValueIterator extends UnmodifiableIterator {
        /** Queue for DFS. Push and pop from back.  The last element
         *  of queue is the next node who's value will be returned.
         *  INVARIANT: queue.size()!=0 => queue.getLast().getValue()!=null */
        private LinkedList /* of TrieNode */ queue=new LinkedList();

        /** Creates a new iterator that yields all the node of start
         *  and its children. */
        ValueIterator(TrieNode start) {
            queue.add(start);
            advance();
        }

        /**
         * Performs DFS using the queue until the last node of the
         * queue has a non-null value or the queue is empty.
         *     @modifies this
         */
        private void advance() {
            while (queue.size()>0) {
                //Stop if tail contains non-null value
                TrieNode node=(TrieNode)queue.getLast();
                Object value=node.getValue();
                if (value!=null)
                    return;

                //Remove tail and expand its children.
                queue.removeLast();
                for (Iterator iter=node.children(); iter.hasNext(); )
                    queue.addLast(iter.next());
            }
        }

        public boolean hasNext() {
            return queue.size()>0;
        }

        public Object next() {
            if (! hasNext()) {
                throw new NoSuchElementException();
            }
            TrieNode node=(TrieNode)queue.removeLast();
            for (Iterator iter=node.children(); iter.hasNext(); )
                queue.addLast(iter.next());
            advance();
            return node.getValue();
        }
    }

    /** Yields nothing. */
    private static class EmptyIterator extends UnmodifiableIterator {
        public boolean hasNext() { return false; }
        public Object next() { throw new NoSuchElementException(); }
    }


    /** 
     * Ensures that this consumes the minimum amount of memory.  If
     * valueCompactor is not null, also sets each node's value to
     * valueCompactor.apply(node).  Any exceptions thrown by a call to
     * valueCompactor are thrown by this.
     *
     * This method should typically be called after add(..)'ing a number of
     * nodes.  Insertions can be done after the call to compact, but they might
     * be slower.  Because this method only affects the performance of this,
     * there is no modifies clause listed.  
     */
    public void trim(Function valueCompactor) 
            throws IllegalArgumentException, ClassCastException {
        //For each node in this...
        LinkedList queue=new LinkedList();
        queue.add(root);
        while (! queue.isEmpty()) {
            TrieNode node=(TrieNode)queue.removeLast();

            //1. Apply compactor to value (if any).
            if (valueCompactor!=null) {
                Object value=node.getValue();
                if (value!=null)
                    node.setValue(valueCompactor.apply(value));
            }

            //2. Compact node's children.
            node.trim();

            //3. Continue with DFS on children.
            for (Iterator iter=node.children(); iter.hasNext(); ) {
                queue.addLast(iter.next());
            }
        }
     }


    /** Returns a string representation of the tree state of this, i.e., the
     *  concrete state.  (The version of toString commented out below returns
     *  a representation of the abstract state of this. */
    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append("<root>");
        toStringHelper(root, buf, 1);
        return buf.toString();
    }

    /** Prints a description of the substree starting with start to
     *  buf.  The printing starts with the given indent level. */
    private void toStringHelper(TrieNode start, StringBuffer buf, int indent) {
        //Print value of node.
        if (start.getValue()==null)
            buf.append("\n");
        else
            buf.append(" -> "+start.getValue().toString()+"\n");

        //For each child...
        for (Iterator iter=start.labels(); iter.hasNext(); ) {
            //Indent child appropriately.
            for (int i=0; i<indent; i++)
                buf.append("  ");
            //Print edge.
            String label=(String)iter.next();
            buf.append(label);
            //Recurse to print value.
            TrieNode child=start.get(label.charAt(0)).getChild();
            toStringHelper(child, buf, indent+1);
        }
    }

//      public String toString() {
//          StringBuffer buf=new StringBuffer("[");
//          /** INVARIANT: queue.size()==prefix.size(), and queue[i]
//           *  is reachable by following the path prefixes[i] from root. */
//          LinkedList /* of TrieNode */ queue=new LinkedList();
//          LinkedList /* of String */ prefixes=new LinkedList();
//          queue.addLast(root);
//          prefixes.addLast("");
//          /** True iff we've added one pair to buf.  Used to control ", " */
//          boolean gotKey=false;
//          while (queue.size() > 0) {
//              TrieNode node=(TrieNode)queue.removeLast();
//              String prefix=(String)prefixes.removeLast();
//              for (Iterator iter=node.labels(); iter.hasNext(); ) {
//                  Character c=(Character)iter.next();
//                  TrieNode child=node.get(c.charValue());
//                  queue.addLast(child);
//                  prefixes.addLast(prefix+c);
//              }
//              Object value=node.getValue();
//              if (value!=null) {
//                  if (gotKey)
//                      buf.append(", ");
//                  gotKey=true;
//                  buf.append(prefix.toString());
//                  buf.append("->");
//                  buf.append(value.toString());
//              }
//          }
//          buf.append("]");
//          return buf.toString();
//      }

    /** Unit test. */
    /*
    public static void main(String args[]) {
        TrieNode.unitTest();
        Trie t=new Trie(false);

        Assert.that(t.match("abcde", 0, 5, "abc")==-1);
        Assert.that(t.match("abcde", 1, 5, "bXd")==1);
        Assert.that(t.match("abcde", 1, 3, "bcd")==2);

        Object anVal0="another value for an";
        Object anVal="value for an";
        Object antVal0="another value for ant";
        Object antVal="value for ant";
        Object addVal="value for add";
        Object aVal="value for a";
        Iterator iter=null;
        Object tmp1=null, tmp2=null, tmp3=null, tmp4=null;

        Assert.that(t.get("a")==null);

        Assert.that(t.add("ant", antVal0)==null);     //additive insert
        System.out.println(t.toString());
        Assert.that(t.add("an", anVal)==null);        //splice insert
        System.out.println(t.toString());
        Assert.that(t.add("add", addVal)==null);      //split insert
        System.out.println(t.toString());
        Assert.that(t.add("ant", antVal)==antVal0);   //relabel insert (old -> new)
        System.out.println(t.toString());
        Assert.that(t.add("a", aVal)==null);          //relabel insert (NULL -> new)
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
        iter=t.getPrefixedBy("ants");
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) { }

        //Yield 1 element...starting in middle of prefix
        iter=t.getPrefixedBy("ad");
        tmp1=iter.next();
        Assert.that(tmp1==addVal, tmp1.toString());
        Assert.that(! iter.hasNext());

        //Yield many elements...
        iter=t.getPrefixedBy("a");
        Assert.that(iter.hasNext());
        tmp1=iter.next();
        Assert.that(tmp1==aVal);
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
        t.add("", aVal);
        t.add("an", anVal);
        Assert.that(t.get("")==aVal);
        iter=t.getPrefixedBy("");
        tmp1=iter.next();
        Assert.that(tmp1==aVal || tmp1==anVal);
        tmp2=iter.next();
        Assert.that(tmp2==aVal || tmp2==anVal);
        Assert.that(tmp1!=tmp2);

        //Case insensitive tests
        t=new Trie(true);
        Assert.that(t.add("an", anVal)==null);
//          Assert.that(t.add("An", anVal)==anVal);
//          Assert.that(t.add("aN", anVal)==anVal);
//          Assert.that(t.add("AN", anVal)==anVal);
        Assert.that(t.get("an")==anVal);
        Assert.that(t.get("An")==anVal);
        Assert.that(t.get("aN")==anVal);
        Assert.that(t.get("AN")==anVal);
        Assert.that(t.add("ant", antVal)==null);
        Assert.that(t.get("ANT")==antVal);
        iter=t.getPrefixedBy("a");
        Assert.that(iter.next()==anVal);
        Assert.that(iter.next()==antVal);
        Assert.that(! iter.hasNext());

        //Prefix tests
        t=new Trie(false);
        Assert.that(t.add("an", anVal)==null);
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

        //Remove tests
        t=new Trie(false);
        t.add("an", anVal);
        t.add("ant", antVal);
        Assert.that(t.remove("x")==false);
        Assert.that(t.remove("a")==false);
        Assert.that(t.remove("an")==true);
        Assert.that(t.get("an")==null);
        Assert.that(t.get("ant")==antVal);
    }
    */
}



/**
 * A node of the Trie.  Each Trie has a list of children, labelled by strings.
 * Each of these [String label, TrieNode child] pairs is considered an
 * "edge".  The first character of each label must be distinct.  When managing
 * children, different implementations may trade space for time.  Each node also
 * stores an arbitrary Object value.<p>
 *
 * Design note: this is a "dumb" class.  It is <i>only</i> responsible for
 * managing its value and its children.  None of its operations are recursive;
 * that is Trie's job.  Nor does it deal with case.
 */
final class TrieNode {
    /** The value of this node. */
    private Object value=null;
    /** The list of children.  Children are stored as a sorted Vector because
     *  it is a more compact than a tree or linked lists.  Insertions and 
     *  deletions are more expensive, but they are rare compared to searching.
     *
     *  INVARIANT: children are sorted by distinct first characters of edges,
     *  i.e., for all i<j,
     *          children[i].edge.charAt(0)<children[j].edge.charAt(0)
     */
    private Vector /* of TrieEdge */ children=new Vector(0);

    /** Creates a trie with no children and no value. */
    public TrieNode() { }

     /** Creates a trie with no children and the given value. */
    public TrieNode(Object value) { 
        this.value=value;
    }


    /** Gets the value associated with this node, or null if none. */
    public Object getValue() {
        return value;
    }

    /** Sets the value associated with this node. */
    public void setValue(Object value) {
        this.value=value;
    }

    private final TrieEdge get(int i) {
        return (TrieEdge)children.get(i);
    }

    /**
     * If exact,  returns the unique  i s.t. children[i].getLabelStart()==c
     * If !exact, returns the largest i s.t. children[i].getLabelStart()<=c
     * In either case, returns -1 if no such i exists.
     *
     * This method uses binary search and runs in O(log N) time, where
     * N=children.size().  The standard Java binary search methods could not
     * be used because they only return exact matches.  Also, they require
     * allocating a dummy Trie.
     */
    private final int search(char c, boolean exact) {
        //This code is stolen from IntSet.search.
        int low=0;
        int high=children.size()-1;

        while (low<=high) {
            int i=(low+high)/2;
            char ci=get(i).getLabelStart();

            if (ci<c)
                low=i+1;
            else if (c<ci)
                high=i-1;
            else
                return i;
        }

        if (exact)
            return -1;        //Return no match.
        else
            return high;      //Return closes match.  (This works!)
    } 

    /** Returns the edge (at most one) whose label starts with the given
     *  character, or null if no such edge.  */
    public TrieEdge get(char labelStart) {
        int i=search(labelStart, true);
        if (i<0)
            return null;
        TrieEdge ret=get(i);
        Assert.that(ret.getLabelStart()==labelStart);
        return ret;
    }

    /** Adds an edge with the given label to the given child to this.
     *     @requires for all edges E in this, label.getLabel[0]!=E not already
     *      mapped to a node.
     *     @modifies this */
    public void put(String label, TrieNode child) {
        char labelStart=label.charAt(0);
        int i=search(labelStart, false);   //find closest match
        if (i>=0) 
            Assert.that(get(i).getLabelStart()!=labelStart,
                        "Precondition of TrieNode.put violated.");
        children.add(i+1, new TrieEdge(label, child));
    }

    /** Removes the edge (at most one) whose label starts with the given
     *  character.  Returns true if any edges where actually removed. */
    public boolean remove(char labelStart) {
        int i=search(labelStart, true);
        if (i==-1)
            return false;
        Assert.that(get(i).getLabelStart()==labelStart);
        children.remove(i);
        return true;
    }

    /** Ensures that this's children take a minimal amount of storage.  This
     *  should be called after numberous calls to add().
     *      @modifies this */
    public void trim() {
        children.trimToSize();
    }

    /** Returns the children of this in any order, as an iterator
     *  of TrieNode. */
    public Iterator children() {
        return new ChildrenIterator();
    }

    /** Returns the labels of the children of this in any order, as an iterator
     *  of Strings. */
    public Iterator labels() {
        return new LabelIterator();
    }

    /** Maps (lambda (pair) pair.node) on children.iterator() */
    private class ChildrenIterator extends UnmodifiableIterator {
        int i=0;
        public boolean hasNext() {
            return i<children.size();
        }
        public Object next() {
            if (! hasNext())
                throw new NoSuchElementException();

            TrieEdge edge=get(i);
            i++;
            return edge.getChild();
        }
    }

    /** Maps (lambda (pair) pair.c) on children.iterator() */
    private class LabelIterator extends UnmodifiableIterator {
        int i=0;
        public boolean hasNext() {
            return i<children.size();
        }
        public Object next() {
            if (! hasNext())
                throw new NoSuchElementException();

            TrieEdge edge=get(i);
            i++;
            return edge.getLabel();
        }
    }

    public String toString() {
        Object val=getValue();
        if (val==null)
            return "NULL";
        else
            return val.toString();
    }

    /*
    static void unitTest() {
        TrieNode node=new TrieNode();
        TrieNode childA=new TrieNode();
        TrieNode childB=new TrieNode();
        TrieNode childC=new TrieNode();

        Assert.that(node.getValue()==null);
        String value="abc";
        node.setValue(value);
        Assert.that(node.getValue()==value);

        Assert.that(node.get('a')==null);
        Iterator iter=node.children();
        Assert.that(! iter.hasNext());

        //Test put/get.  Note we insert at beginning and end of list.
        node.put("b", childB);
        Assert.that(node.get('a')==null);
        Assert.that(node.get('b').getChild()==childB);
        Assert.that(node.get('c')==null);
        Assert.that(node.get('d')==null);
        node.put("a very long key", childA);
        Assert.that(node.get('a').getChild()==childA);
        Assert.that(node.get('b').getChild()==childB);
        Assert.that(node.get('c')==null);
        Assert.that(node.get('d')==null);
        node.put("c is also a key", childC);
        Assert.that(node.get('a').getChild()==childA);
        Assert.that(node.get('b').getChild()==childB);
        Assert.that(node.get('c').getChild()==childC);
        Assert.that(node.get('d')==null);

        //Test child iterator
        iter=node.children();
        Object tmp=(TrieNode)iter.next();
        Assert.that(tmp==childA || tmp==childB || tmp==childC);
        Object tmp2=(TrieNode)iter.next();
        Assert.that(tmp2==childA || tmp2==childB  || tmp2==childC);
        Assert.that(tmp2!=tmp);
        Object tmp3=(TrieNode)iter.next();
        Assert.that(tmp3==childA || tmp3==childB  || tmp3==childC);
        Assert.that(tmp2!=tmp);
        Assert.that(tmp3!=tmp);
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) {
        }

        //Test label iterator
        iter=node.labels();
        tmp=(String)iter.next();
        Assert.that(tmp.equals("b") 
            || tmp.equals("a very long key") 
            || tmp.equals("c is also a key"));
        tmp2=(String)iter.next();
        Assert.that(tmp2.equals("b") 
            || tmp2.equals("a very long key") 
            || tmp2.equals("c is also a key"));
        Assert.that(! tmp2.equals(tmp));
        tmp3=(String)iter.next();
        Assert.that(tmp3.equals("b") 
            || tmp3.equals("a very long key") 
            || tmp3.equals("c is also a key"));
        Assert.that(! tmp2.equals(tmp));
        Assert.that(! tmp3.equals(tmp));
        Assert.that(! iter.hasNext());
        try {
            iter.next();
            Assert.that(false);
        } catch (NoSuchElementException e) {
        }

        //Test remove operations.
        node.remove('a');
        Assert.that(node.get('a')==null);
        Assert.that(node.get('b').getChild()==childB);
        Assert.that(node.get('c').getChild()==childC);
        node.remove('c');
        Assert.that(node.get('a')==null);
        Assert.that(node.get('b').getChild()==childB);
        Assert.that(node.get('c')==null);
        node.remove('b');
        Assert.that(node.get('a')==null);
        Assert.that(node.get('b')==null);
        Assert.that(node.get('c')==null);
    }
    */
}

/**
 * A labelled edge, i.e., a String label and a TrieNode endpoint.
 */
final class TrieEdge {
    private String label;
    private TrieNode child;

    /** @requires label.size()>0
     *  @requires child!=null */
    TrieEdge(String label, TrieNode child) {
        this.label=label;
        this.child=child;
    }

    public String getLabel() {
        return label;
    }

    /** Returns the first character of the label, i.e., getLabel().charAt(0). */
    public char getLabelStart() {
        //You could store this char as an optimization if needed.
        return label.charAt(0);
    }

    public TrieNode getChild() {
        return child;
    }
}

