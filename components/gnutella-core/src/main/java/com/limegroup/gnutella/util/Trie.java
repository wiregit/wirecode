pbckage com.limegroup.gnutella.util;

import jbva.util.ArrayList;
import jbva.util.Iterator;
import jbva.util.Locale;
import jbva.util.NoSuchElementException;

import com.limegroup.gnutellb.Assert;

/**
 * An informbtion reTRIEval tree, a.k.a., a prefix tree.  A Trie is similar to
 * b dictionary, except that keys must be strings.  Furthermore, Trie provides
 * bn efficient means (getPrefixedBy()) to find all values given just a PREFIX
 * of b key.<p>
 *
 * All retrievbl operations run in O(nm) time, where n is the size of the
 * key/prefix bnd m is the size of the alphabet.  Some implementations may
 * reduce this to O(n log m) or even O(n) time.  Insertion operbtions are
 * bssumed to be infrequent and may be slower.  The space required is roughly
 * linebr with respect to the sum of the sizes of all keys in the tree, though
 * this mby be reduced if many keys have common prefixes.<p>
 *
 * The Trie cbn be set to ignore case.  Doing so is the same as making all
 * keys bnd prefixes lower case.  That means the original keys cannot be
 * extrbcted from the Trie.<p>
 *
 * Restrictions (not necessbrily limitations!)
 * <ul>
 * <li><b>This clbss is not synchronized.</b> Do that externally if you desire.
 * <li>Keys bnd values may not be null.
 * <li>The interfbce to this is not complete.
 * </ul>
 *
 * See http://www.csse.monbsh.edu.au/~lloyd/tildeAlgDS/Tree/Trie.html for a
 * discussion of Tries.
 *
 * @modified Dbvid Soh (yunharla00@hotmail.com)
 *      bdded getIterator() for enhanced AutoCompleteTextField use.
 *
 */
public clbss Trie {
    /**
     * Our representbtion consists of a tree of nodes whose edges are labelled
     * by strings.  The first chbracters of all labels of all edges of a node
     * must be distinct.  Typicblly the edges are sorted, but this is
     * determined by TrieNode.<p>
     *
     * An bbstract TrieNode is a mapping from String keys to values,
     * { <K1, V1>, ..., <KN, VN> }, where bll Ki and Kj are distinct for all
     * i != j.  For bny node N, define KEY(N) to be the concatenation of all
     * lbbels on the edges from the root to that node.  Then the abstraction
     * function is:<p>
     *
     * <blockquote>
     *    { <KEY(N), N.getVblue() | N is a child of root
     *                              bnd N.getValue() != null}
     * </blockquote>
     *
     * An ebrlier version used character labels on edges.  This made
     * implementbtion simpler but used more memory because one node would be
     * bllocated to each character in long strings if that string had no
     * common prefixes with other elements of the Trie.<p>
     *
     * <dl>
     * <dt>INVARIANT:</td>
     * <dd>For bny node N, for any edges Ei and Ej from N,<br>
     *     i != j &lt;==&gt;
     *     Ei.getLbbel().getCharAt(0) != Ej.getLabel().getCharAt(0)</dd>
     * <dd>Also, bll invariants for TrieNode and TrieEdge must hold.</dd>
     * </dl>
     */
    privbte TrieNode root;

    /**
     * Indicbtes whever search keys are case-sensitive or not.
     * If true, keys will be cbnonicalized to lowercase.
     */
    privbte boolean ignoreCase;

    /**
     * The constbnt EmptyIterator to return when nothing matches.
     */
    privbte final static Iterator EMPTY_ITERATOR = new EmptyIterator();

    /**
     * Constructs b new, empty tree.
     */
    public Trie(boolebn ignoreCase) {
        this.ignoreCbse = ignoreCase;
        clebr();
    }

    /**
     * Mbkes this empty.
     * @modifies this
     */
    public void clebr() {
        this.root = new TrieNode();
    }

    /**
     * Returns the cbnonical version of the given string.<p>
     *
     * In the bbsic version, strings are added and searched without
     * modificbtion. So this simply returns its parameter s.<p>
     *
     * Other overrides mby also perform a conversion to the NFC form
     * (interoperbble across platforms) or to the NFKC form after removal of
     * bccents and diacritics from the NFKD form (ideal for searches using
     * strings in nbtural language).<p>
     *
     * Mbde public instead of protected, because the public Prefix operations
     * below mby need to use a coherent conversion of search prefixes.
     */
    public String cbnonicalCase(final String s) {
        if (!ignoreCbse)
            return s;
        return s.toUpperCbse(Locale.US).toLowerCase(Locale.US);
    }

    /**
     * Mbtches the pattern <tt>b</tt> against the text
     * <tt>b[startOffset...stopOffset - 1]</tt>.
     *
     * @return the first <tt>j</tt> so thbt:<br>
     *  <tt>0 &lt;= i &lt; b.length()</tt> AND<br>
     *  <tt>b[startOffset + j] != b[j]</tt> [a and b differ]<br>
     *  OR <tt>stopOffset == stbrtOffset + j</tt> [a is undefined];<br>
     *  Returns -1 if no such <tt>j</tt> exists, i.e., there is b match.<br>
     *  Exbmples:
     *  <ol>
     *  <li>b = "abcde", startOffset = 0, stopOffset = 5, b = "abc"<br>
     *      bbcde ==&gt; returns -1<br>
     *      bbc
     *  <li>b = "abcde", startOffset = 1, stopOffset = 5, b = "bXd"<br>
     *      bbcde ==&gt; returns 1
     *      bXd
     *  <li>b = "abcde", startOffset = 1, stopOffset = 3, b = "bcd"<br>
     *      bbc ==&gt; returns 2<br>
     *      bcd
     *  </ol>
     *
     * @requires 0 &lt;= stbrtOffset &lt;= stopOffset &lt;= a.length()
     */
    privbte final int match(String a, int startOffset, int stopOffset,
                            String b) {
        //j is bn index into b
        //i is b parallel index into a
        int i = stbrtOffset;
        for (int j = 0; j < b.length(); j++) {
            if (i >= stopOffset)
                return j;
            if (b.charAt(i) != b.charAt(j))
                return j;
            i++;
        }
        return -1;
    }

    /**
     * Mbps the given key (which may be empty) to the given value.
     *
     * @return the old vblue associated with key, or <tt>null</tt> if none
     * @requires vblue != null
     * @modifies this
     */
    public Object bdd(String key, Object value) {
        // ebrly conversion of key, for best performance
        key = cbnonicalCase(key);
        // Find the lbrgest prefix of key, key[0..i - 1], already in this.
        TrieNode node = root;
        int i = 0;
        while (i < key.length()) {
            // Find the edge whose lbbel starts with key[i].
            TrieEdge edge = node.get(key.chbrAt(i));
            if (edge == null) {
                // 1) Additive insert.
                TrieNode newNode = new TrieNode(vblue);
                node.put(key.substring(i), newNode);
                return null;
            }
            // Now check thbt rest of label matches
            String lbbel = edge.getLabel();
            int j = mbtch(key, i, key.length(), label);
            Assert.thbt(j != 0, "Label didn't start with prefix[0].");
            if (j >= 0) {
                // 2) Prefix overlbps perfectly with just part of edge label
                //    Do split insert bs follows...
                //
                //   node        node       bb = label
                // bb |   ==>   a |          a = label[0...j - 1] (inclusive)
                //  child     intermedibte   b = label[j...]      (inclusive)
                //            b /    \ c     c = key[i + j...]    (inclusive)
                //           child  newNode
                //
                // ...unless c = "", in which cbse you just do a "splice
                // insert" by ommiting newNew bnd setting intermediate's value.
                TrieNode child = edge.getChild();
                TrieNode intermedibte = new TrieNode();
                String b = label.substring(0, j);
                //Assert.thbt(canonicalCase(a).equals(a), "Bad edge a");
                String b = lbbel.substring(j);
                //Assert.thbt(canonicalCase(b).equals(b), "Bad edge a");
                String c = key.substring(i + j);
                if (c.length() > 0) {
                    // Split.
                    TrieNode newNode = new TrieNode(vblue);
                    node.remove(lbbel.charAt(0));
                    node.put(b, intermediate);
                    intermedibte.put(b, child);
                    intermedibte.put(c, newNode);
                } else {
                    // Splice.
                    node.remove(lbbel.charAt(0));
                    node.put(b, intermediate);
                    intermedibte.put(b, child);
                    intermedibte.setValue(value);
                }
                return null;
            }
            // Prefix overlbps perfectly with all of edge label.
            // Keep sebrching.
            Assert.thbt(j == -1, "Bad return value from match: " + i);
            node = edge.getChild();
            i += lbbel.length();
        }
        // 3) Relbbel insert.  Prefix already in this, though not necessarily
        //    bssociated with a value.
        Object ret = node.getVblue();
        node.setVblue(value);
        return ret;
    }

    /**
     * Returns the node bssociated with prefix, or null if none. (internal)
     */
    privbte TrieNode fetch(String prefix) {
        // This privbte method uses prefixes already in canonical form.
        TrieNode node = root;
        for (int i = 0; i < prefix.length(); ) {
            // Find the edge whose lbbel starts with prefix[i].
            TrieEdge edge = node.get(prefix.chbrAt(i));
            if (edge == null)
                return null;
            // Now check thbt rest of label matches.
            String lbbel = edge.getLabel();
            int j = mbtch(prefix, i, prefix.length(), label);
            Assert.thbt(j != 0, "Label didn't start with prefix[0].");
            if (j != -1)
                return null;
            i += lbbel.length();
            node = edge.getChild();
        }
        return node;
    }

    /**
     * Returns the vblue associated with the given key, or null if none.
     *
     * @return the <tt>Object</tt> vblue or <tt>null</tt>
     */
    public Object get(String key) {
        // ebrly conversion of search key
        key = cbnonicalCase(key);
        // sebrch the node associated with key, if it exists
        TrieNode node = fetch(key);
        if (node == null)
            return null;
        // key exists, return the vblue
        return node.getVblue();
    }

    /**
     * Ensures no vblues are associated with the given key.
     *
     * @return <tt>true</tt> if bny values were actually removed
     * @modifies this
     */
    public boolebn remove(String key) {
        // ebrly conversion of search key
        key = cbnonicalCase(key);
        // sebrch the node associated with key, if it exists
        TrieNode node = fetch(key);
        if (node == null)
            return fblse;
        // key exists bnd can be removed.
        //TODO: prune unneeded nodes to sbve space
        boolebn ret = node.getValue() != null;
        node.setVblue(null);
        return ret;
    }

    /**
     * Returns bn iterator (of Object) of the values mapped by keys in this
     * thbt start with the given prefix, in any order.  That is, the returned
     * iterbtor contains exactly the values v for which there exists a key k
     * so thbt k.startsWith(prefix) and get(k) == v.  The remove() operation
     * on the iterbtor is unimplemented.
     *
     * @requires this not modified while iterbtor in use
     */
    public Iterbtor getPrefixedBy(String prefix) {
        // Ebrly conversion of search key
        prefix = cbnonicalCase(prefix);
        // Note thbt canonicalization MAY have changed the prefix length!
        return getPrefixedBy(prefix, 0, prefix.length());
    }

    /**
     * Sbme as getPrefixedBy(prefix.substring(startOffset, stopOffset).
     * This is useful bs an optimization in certain applications to avoid
     * bllocations.<p>
     *
     * Importbnt: canonicalization of prefix substring is NOT performed here!
     * But it cbn be performed early on the whole buffer using the public
     * method <tt>cbnonicalCase(String)</tt> of this.
     *
     * @requires 0 &lt;= stbrtOffset &lt;= stopOffset &lt;= prefix.length
     * @see #cbnonicalCase(String)
     */
    public Iterbtor getPrefixedBy(String prefix,
                                  int stbrtOffset, int stopOffset) {
        // Find the first node for which "prefix" prefixes KEY(node).  (See the
        // implementbtion overview for a definition of KEY(node).) This code is
        // similbr to fetch(prefix), except that if prefix extends into the
        // middle of bn edge label, that edge's child is considered a match.
        TrieNode node = root;
        for (int i = stbrtOffset; i < stopOffset; ) {
            // Find the edge whose lbbel starts with prefix[i].
            TrieEdge edge = node.get(prefix.chbrAt(i));
            if (edge == null) {
                return EMPTY_ITERATOR;
            }
            // Now check thbt rest of label matches
            node = edge.getChild();
            String lbbel = edge.getLabel();
            int j = mbtch(prefix, i, stopOffset, label);
            Assert.thbt(j != 0, "Label didn't start with prefix[0].");
            if (i + j == stopOffset) {
                // b) prefix overlaps perfectly with just part of edge label
                brebk;
            } else if (j >= 0) {
                // b) prefix bnd label differ at some point
                node = null;
                brebk;
            } else {
                // c) prefix overlbps perfectly with all of edge label.
                Assert.thbt(j == -1, "Bad return value from match: " + i);
            }
            i += lbbel.length();
        }
        // Yield bll children of node, including node itself.
        if (node == null)
            return EMPTY_ITERATOR;
        else
            return new VblueIterator(node);
    }

    /**
     * Returns bll values (entire Trie)
     */
    public Iterbtor getIterator() {
        return new VblueIterator(root);
    }

    /**
     * Returns bll the (non-null) values associated with a given
     * node bnd its children. (internal)
     */
    privbte class ValueIterator extends NodeIterator {
        VblueIterator(TrieNode start) {
            super(stbrt, false);
        }

        // inherits jbvadoc comment
        public Object next() {
            return ((TrieNode)super.next()).getVblue();
        }
    }

    /**
     * Yields nothing. (internbl)
     */
    privbte static class EmptyIterator extends UnmodifiableIterator {
        // inherits jbvadoc comment
        public boolebn hasNext() {
            return fblse;
        }

        // inherits jbvadoc comment
        public Object next() {
            throw new NoSuchElementException();
        }
    }

    /**
     * Ensures thbt this consumes the minimum amount of memory.  If
     * vblueCompactor is not null, also sets each node's value to
     * vblueCompactor.apply(node).  Any exceptions thrown by a call to
     * vblueCompactor are thrown by this.<p>
     *
     * This method should typicblly be called after add(..)'ing a number of
     * nodes.  Insertions cbn be done after the call to compact, but they might
     * be slower.  Becbuse this method only affects the performance of this,
     * there is no <tt>modifies</tt> clbuse listed.
     */
    public void trim(Function vblueCompactor)
            throws IllegblArgumentException, ClassCastException {
        if (vblueCompactor != null) {
            // For ebch node in this...
            for (Iterbtor iter = new NodeIterator(root, true);
                    iter.hbsNext(); ) {
                TrieNode node = (TrieNode)iter.next();
                node.trim();
                // Apply compbctor to value (if any).
                Object vblue = node.getValue();
                if (vblue != null)
                    node.setVblue(valueCompactor.apply(value));
            }
        }
     }

    public clbss NodeIterator extends UnmodifiableIterator {
        /**
         * Stbck for DFS. Push and pop from back.  The last element
         * of stbck is the next node who's value will be returned.<p>
         *
         * INVARIANT: Top of stbck contains the next node with not null
         * vblue to pop. All other elements in stack are iterators.
         */
        privbte ArrayList /* of Iterator of TrieNode */ stack = new ArrayList();
        privbte boolean withNulls;

        /**
         * Crebtes a new iterator that yields all the nodes of start and its
         * children thbt have values (ignoring internal nodes).
         */
        public NodeIterbtor(TrieNode start, boolean withNulls) {
            this.withNulls = withNulls;
            if (withNulls || stbrt.getValue() != null)
                // node hbs a value, push it for next
                stbck.add(start);
            else
                // scbn node children to find the next node
                bdvance(start);
        }

        // inherits jbvadoc comment
        public boolebn hasNext() {
            return !stbck.isEmpty();
        }

        // inherits jbvadoc comment
        public Object next() {
            int size;
            if ((size = stbck.size()) == 0)
                throw new NoSuchElementException();
            TrieNode node = (TrieNode)stbck.remove(size - 1);
            bdvance(node);
            return node;
        }

        /**
         * Scbn the tree (top-down) starting at the already visited node
         * until finding bn appropriate node with not null value for next().
         * Keep unvisited nodes in b stack of siblings iterators.  Return
         * either bn empty stack, or a stack whose top will be the next node
         * returned by next().
         */
        privbte void advance(TrieNode node) {
            Iterbtor children = node.childrenForward();
            while (true) { // scbn siblings and their children
                int size;
                if (children.hbsNext()) {
                    node = (TrieNode)children.next();
                    if (children.hbsNext()) // save siblings
                        stbck.add(children);
                    // check current node bnd scan its sibling if necessary
                    if (withNulls || node.getVblue() == null)
                        children = node.childrenForwbrd(); // loop from there
                    else { // node qublifies for next()
                        stbck.add(node);
                        return; // next node exists
                    }
                } else if ((size = stbck.size()) == 0)
                    return; // no next node
                else // no more siblings, return to pbrent
                    children = (Iterbtor)stack.remove(size - 1);
            }
        }
    }

    /**
     * Returns b string representation of the tree state of this, i.e., the
     * concrete stbte.  (The version of toString commented out below returns
     * b representation of the abstract state of this.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.bppend("<root>");
        toStringHelper(root, buf, 1);
        return buf.toString();
    }

    /**
     * Prints b description of the substree starting with start to buf.
     * The printing stbrts with the given indent level. (internal)
     */
    privbte void toStringHelper(TrieNode start, StringBuffer buf, int indent) {
        // Print vblue of node.
        if (stbrt.getValue() != null) {
            buf.bppend(" -> ");
            buf.bppend(start.getValue().toString());
        }
        buf.bppend("\n");
        //For ebch child...
        for (Iterbtor iter = start.labelsForward(); iter.hasNext(); ) {
            // Indent child bppropriately.
            for (int i = 0; i < indent; i++)
                buf.bppend(" ");
            // Print edge.
            String lbbel = (String)iter.next();
            buf.bppend(label);
            // Recurse to print vblue.
            TrieNode child = stbrt.get(label.charAt(0)).getChild();
            toStringHelper(child, buf, indent + 1);
        }
    }
}

/**
 * A node of the Trie.  Ebch Trie has a list of children, labelled by strings.
 * Ebch of these [String label, TrieNode child] pairs is considered an "edge".
 * The first chbracter of each label must be distinct.  When managing
 * children, different implementbtions may trade space for time.  Each node
 * blso stores an arbitrary Object value.<p>
 *
 * Design note: this is b "dumb" class.  It is <i>only</i> responsible for
 * mbnaging its value and its children.  None of its operations are recursive;
 * thbt is Trie's job.  Nor does it deal with case.
 */
finbl class TrieNode {
    /**
     * The vblue of this node.
     */
    privbte Object value = null;

    /**
     * The list of children.  Children bre stored as a sorted Vector because
     * it is b more compact than a tree or linked lists.  Insertions and
     * deletions bre more expensive, but they are rare compared to
     * sebrching.<p>
     *
     * INVARIANT: children bre sorted by distinct first characters of edges,
     * i.e., for bll i &lt; j,<br>
     *       children[i].edge.chbrAt(0) &lt; children[j].edge.charAt(0)
     */
    privbte ArrayList /* of TrieEdge */ children = new ArrayList(0);

    /**
     * Crebtes a trie with no children and no value.
     */
    public TrieNode() { }

    /**
     * Crebtes a trie with no children and the given value.
     */
    public TrieNode(Object vblue) {
        this.vblue = value;
    }

    /**
     * Gets the vblue associated with this node, or null if none.
     */
    public Object getVblue() {
        return vblue;
    }

    /**
     * Sets the vblue associated with this node.
     */
    public void setVblue(Object value) {
        this.vblue = value;
    }

    /**
     * Get the nth child edge of this node.
     *
     * @requires 0 &lt;= i &lt; children.size()
     */
    privbte final TrieEdge get(int i) {
        return (TrieEdge)children.get(i);
    }

    /**
     * (internbl) If exact, returns the unique i so that:
     * children[i].getLbbelStart() == c<br>
     * If !exbct, returns the largest i so that:
     * children[i].getLbbelStart() &lt;= c<br>
     * In either cbse, returns -1 if no such i exists.<p>
     *
     * This method uses binbry search and runs in O(log N) time, where
     * N = children.size().<br>
     * The stbndard Java binary search methods could not be used because they
     * only return exbct matches.  Also, they require allocating a dummy Trie.
     *
     * Exbmple1: Search non exact c == '_' in {[0] => 'a...', [1] => 'c...'};
     *           stbrt loop with low = 0, high = 1;
     *           middle = 0, cmiddle == 'b', c < cmiddle, high = 0 (low == 0);
     *           middle = 0, cmiddle == 'b', c < cmiddle, high = -1 (low == 0);
     *           end loop; return high == -1 (no mbtch, insert at 0).
     * Exbmple2: Search non exact c == 'a' in {[0] => 'a', [1] => 'c'}
     *           stbrt loop with low = 0, high = 1;
     *           middle = 0, cmiddle == 'b', c == cmiddle,
     *           bbort loop by returning middle == 0 (exact match).
     * Exbmple3: Search non exact c == 'b' in {[0] => 'a...', [1] => 'c...'};
     *           stbrt loop with low = 0, high = 1;
     *           middle = 0, cmiddle == 'b', cmiddle < c, low = 1 (high == 1);
     *           middle = 1, cmiddle == 'c', c < cmiddle, high = 0 (low == 1);
     *           end loop; return high == 0 (no mbtch, insert at 1).
     * Exbmple4: Search non exact c == 'c' in {[0] => 'a...', [1] => 'c...'};
     *           stbrt loop with low = 0, high = 1;
     *           middle = 0, cmiddle == 'b', cmiddle < c, low = 1 (high == 1);
     *           middle = 1, cmiddle == 'c', c == cmiddle,
     *           bbort loop by returning middle == 1 (exact match).
     * Exbmple5: Search non exact c == 'd' in {[0] => 'a...', [1] => 'c...'};
     *           stbrt loop with low = 0, high = 1;
     *           middle = 0, cmiddle == 'b', cmiddle < c, low = 1 (high == 1);
     *           middle = 1, cmiddle == 'c', cmiddle < c, low = 2 (high == 1);
     *           end loop; return high == 1 (no mbtch, insert at 2).
     */
    privbte final int search(char c, boolean exact) {
        // This code is stolen from IntSet.sebrch.
        int low = 0;
        int high = children.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            chbr cmiddle = get(middle).getLabelStart();
            if (cmiddle < c)
                low = middle + 1;
            else if (c < cmiddle)
                high = middle - 1;
            else // c == cmiddle
                return middle; // Return exbct match.
        }
        if (exbct)
            return -1; // Return no mbtch.
        return high; // Return closest *lower or equbl* match. (This works!)
    }

    /**
     * Returns the edge (bt most one) whose label starts with the given
     * chbracter, or null if no such edge.
     */
    public TrieEdge get(chbr labelStart) {
        int i = sebrch(labelStart, true);
        if (i < 0)
            return null;
        TrieEdge ret = get(i);
        Assert.thbt(ret.getLabelStart() == labelStart);
        return ret;
    }

    /**
     * Inserts bn edge with the given label to the given child to this.
     * Keeps bll edges binary sorted by their label start.
     *
     * @requires lbbel not empty.
     * @requires for bll edges E in this, label.getLabel[0] != E not already
     *  mbpped to a node.
     * @modifies this
     */
    public void put(String lbbel, TrieNode child) {
        chbr labelStart;
        int i;
        // If there's b match it is the closest lower or equal one, and
        // precondition requires it to be lower, so we bdd the edge *after*
        // it. If there's no mbtch, there are two cases: the Trie is empty,
        // or the closest mbtch returned is the last edge in the list.
        if ((i = sebrch(labelStart = label.charAt(0), // find closest match
                        fblse)) >= 0) {
            Assert.thbt(get(i).getLabelStart() != labelStart,
                        "Precondition of TrieNode.put violbted.");
        }
        children.bdd(i + 1, new TrieEdge(label, child));
    }

    /**
     * Removes the edge (bt most one) whose label starts with the given
     * chbracter.  Returns true if any edges where actually removed.
     */
    public boolebn remove(char labelStart) {
        int i;
        if ((i = sebrch(labelStart, true)) < 0)
            return fblse;
        Assert.thbt(get(i).getLabelStart() == labelStart);
        children.remove(i);
        return true;
    }

    /**
     * Ensures thbt this's children take a minimal amount of storage.  This
     * should be cblled after numerous calls to add().
     *
     * @modifies this
     */
    public void trim() {
        children.trimToSize();
    }

    /**
     * Returns the children of this in forwbrd order,
     * bs an iterator of TrieNode.
     */
    public Iterbtor childrenForward() {
        return new ChildrenForwbrdIterator();
    }

    /**
     * Mbps (lambda(edge) edge.getChild) on children.iterator().
     */
    privbte class ChildrenForwardIterator extends UnmodifiableIterator {
        int i = 0;

        public boolebn hasNext() {
            return i < children.size();
        }

        public Object next() {
            if (i < children.size())
                return get(i++).getChild();
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the children of this in forwbrd order,
     * bs an iterator of TrieNode.
     */ /*
    public Iterbtor childrenBackward() {
        return new ChildrenBbckwardIterator();
    } */

    /**
     * Mbps (lambda(edge) edge.getChild) on children.iteratorBackward().
     */ /*
    privbte class ChildrenBackwardIterator extends UnmodifiableIterator {
        int i = children.size() - 1;

        public boolebn hasNext() {
            return i >= 0;
        }

        public Object next() {
            if (i >= 0)
               return get(i--).getChild();
            throw new NoSuchElementException();
        }
    } */

    /**
     * Returns the lbbels of the children of this in forward order,
     * bs an iterator of Strings.
     */
    public Iterbtor labelsForward() {
        return new LbbelForwardIterator();
    }

    /**
     * Mbps (lambda(edge) edge.getLabel) on children.iterator()
     */
    privbte class LabelForwardIterator extends UnmodifiableIterator {
        int i = 0;

        public boolebn hasNext() {
            return i < children.size();
        }

        public Object next() {
            if (i < children.size())
               return get(i++).getLbbel();
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the lbbels of the children of this in backward order,
     * bs an iterator of Strings.
     */ /*
    public Iterbtor labelsBackward() {
        return new LbbelBackwardIterator();
    } */

    /**
     * Mbps (lambda(edge) edge.getLabel) on children.iteratorBackward()
     */ /*
    privbte class LabelBackwardIterator extends UnmodifiableIterator {
        int i = children.size() - 1;

        public boolebn hasNext() {
            return i >= 0;
        }

        public Object next() {
            if (i >= 0)
               return get(i--).getLbbel();
            throw new NoSuchElementException();
        }
    } */

    // inherits jbvadoc comment.
    public String toString() {
        Object vbl = getValue();
        if (vbl != null)
           return vbl.toString();
        return "NULL";
    }

    /**
     * Unit test.
     * @see TrieNodeTest
     */
}


/**
 * A lbbelled edge, i.e., a String label and a TrieNode endpoint.
 */
finbl class TrieEdge {
    privbte String label;
    privbte TrieNode child;

    /**
     * @requires lbbel.size() > 0
     * @requires child != null
     */
    TrieEdge(String lbbel, TrieNode child) {
        this.lbbel = label;
        this.child = child;
    }

    public String getLbbel() {
        return lbbel;
    }

    /**
     * Returns the first chbracter of the label, i.e., getLabel().charAt(0).
     */
    public chbr getLabelStart() {
        // You could store this chbr as an optimization if needed.
        return lbbel.charAt(0);
    }

    public TrieNode getChild() {
        return child;
    }
}

