package org.limewire.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Structure that contains only the necessary nodes of a hash tree, automatically
 * verifies new nodes against the root and compacts the tree as nodes are used.
 * 
 * For the numbering of the nodes check
 * http://www.limewire.org/wiki/index.php?title=HashTreeRangeEncoding
 */
public class TreeStorage {
    
    /**
     * Node used to represent nodes that do not really exist in the tree.
     */
    private static final TreeNode PADDING = new TreeNode();
        
    private final Map<Integer, TreeNode> map = new TreeMap<Integer, TreeNode>();
    
    private final NodeGenerator generator;
    
    /** id of the largest node that maps to a real chunk of the file */
    private final int maxId;

    TreeStorage(byte [] rootHash, NodeGenerator generator, int numLeafs) {
        this.generator = generator;
        this.maxId = (0x1 << log2Ceil(numLeafs))+ numLeafs - 1;
        TreeNode root = new TreeNode(1, rootHash);
        // root is trusted
        root.verified = true;
        map.put(1, root);
    }
    
    /**
     * @param id the id of the tree node
     * @param data the hash in the node
     * @return true if the node was added and verified ok
     */
    public boolean add(int id, byte [] data) {
        assert id <= maxId;
        TreeNode tn = map.get(id);
        if (tn != null && tn.verified)
            return false;
        TreeNode newNode = new TreeNode(id, data);
        verify(newNode);
        map.put(id, newNode);
        return newNode.verified;
    }
    
    /**
     * @return the contents of the node with the given id
     */
    public byte [] get(int id) {
        TreeNode tn = map.get(id);
        if (tn != null && tn.verified)
            return tn.data;
        return null;
    }
    
    /**
     * notification that the node id has been used to verify
     * data from the file.  This will compact the tree. 
     */
    public void used(int id) {
        TreeNode tn = map.get(id);
        assert tn != null && tn.verified;
        tn.used = true;
        consolidate(tn);
    }
    
    /**
     * @return the reprsentation as defined on the wiki page
     */
    public Collection<Integer> getUsedNodes() {
        // reel easy
        List<Integer> l = new ArrayList<Integer>(map.keySet().size());
        for (int i : map.keySet())
            if (map.get(i).used)
                l.add(i);
        return l;
    }
    
    public Collection<Integer> getVerifiedNodes() {
        List<Integer> l = new ArrayList<Integer>(map.keySet().size());
        for (int i : map.keySet())
            if (map.get(i).verified)
                l.add(i);
        return l;
    }
    
    /**
     * Goes through the tree and consolidates any nodes
     * that have been used.
     */
    private void consolidate(TreeNode node){
        TreeNode [] siblings = getSiblings(node);
        
        // if no sibling, can't consolidate. (always true for root)
        if (siblings[0] == null || siblings[1] == null)
            return;
        
        // if both haven't been used, can't consolidate.
        if (!siblings[0].used || !siblings[1].used)
            return; 
        
        TreeNode parent;
        TreeNode existingParent = map.get(node.id / 2);
        if (existingParent != null) {
            assert existingParent.verified;
            assert !existingParent.used;
            parent = existingParent;
        } else 
            parent = generateParent(siblings);
        parent.verified = true;
        parent.used = true;
        map.remove(siblings[0].id);
        map.remove(siblings[1].id);
        map.put(parent.id, parent);
        consolidate(parent);
    }
    
    private TreeNode[] getSiblings(TreeNode oneOfThem) {
        TreeNode[] ret = new TreeNode[2];
        if (oneOfThem.id % 2 == 0) {
            ret[0] = oneOfThem;
            if (oneOfThem.id == maxId)
                ret[1] = PADDING;
            else
                ret[1] = map.get(oneOfThem.id + 1);
        } else {
            ret[0] = map.get(oneOfThem.id - 1);
            ret[1] = oneOfThem;
        }
        return ret;
    }
    
    /**
     * Goes through the tree and checks if any nodes
     * can be verified including the provided node.  
     */
    private void verify(TreeNode node) {
        TreeNode [] siblings = getSiblings(node);
        
        // if no sibling, can't verify.
        if (siblings[0] == null || siblings[1] == null)
            return;

        assert !siblings[0].verified && (!siblings[1].verified || siblings[1] == PADDING);
        
        TreeNode parent = generateParent(siblings);
        
        TreeNode existingParent = map.get(parent.id);
        boolean same = existingParent != null && Arrays.equals(existingParent.data, parent.data);
        if (same)
            parent = existingParent;
        if (!parent.verified) 
            verify(parent);

        if (parent.verified) {
            markVerified(siblings[0]);
            markVerified(siblings[1]);
            if (parent.id != 1) // don't remove the root
                map.remove(parent.id);
        } else if (parent.id != 1) // keep the parent around.
            map.put(parent.id, parent);
    }
    
    /**
     * marks the node as verified and removes it if its children
     * are verified too.
     */
    private void markVerified(TreeNode node) {
        if (node == null || node == PADDING || node.id == 1)
            return;
        map.remove(node.id);
        node.verified = true;
        map.put(node.id, node);
        
        // if both children are present, we can forget about this node.
        TreeNode left = map.get(node.id * 2);
        TreeNode right = map.get(node.id * 2 +1);
        
        if (left != null && right != null)
            map.remove(node.id);
        markVerified(left);
        markVerified(right);
    }
    
    private TreeNode generateParent(TreeNode[] children) {
        byte [] data = children[1] == PADDING ? children[0].data : 
            generator.generate(children[0].data, children[1].data);
        return new TreeNode(children[0].id / 2, data);
    }
    
    private static class TreeNode {
        private final int id;
        private final byte[] data;
        private boolean used;
        private boolean verified;
        
        /**
         * creates a marker node for a padding node.
         * Its always verified and used.
         */
        TreeNode() {
            this.id = Integer.MAX_VALUE;
            this.data = null;
            this.used = true;
            this.verified = true;
        }
        
        TreeNode(int id, byte [] data) {
            this.id = id;
            this.data = data;
        }
        
        public String toString() {
            return "id "+id+" verified " +verified + " used "+used;
        }
    }
    
    //////////////////////////////////////////////
   
    

    
    // calculates the next n with 2^n > number
    public static int log2Ceil(long number) {
        int n = 0;
        while (number > 1) {
            number++; // for rounding up.
            number >>>= 1;
            n++;
        }
        return n;
    }
}
