package com.limegroup.gnutella.tigertree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.limegroup.gnutella.util.FixedsizeForgetfulHashMap;

/**
 * Manages access to the list of full nodes for a HashTree.
 * This tries to keep a maximum amount of nodes in memory, purging
 * the least recently used items when the threshold is reached.
 */
class HashTreeNodeManager {
    
    private static final HashTreeNodeManager INSTANCE =
        new HashTreeNodeManager();
    public static HashTreeNodeManager instance()  { return INSTANCE; }
    private HashTreeNodeManager() {}
    
    /**
     * The maximum amount of nodes to store in memory.
     *
     * This will use up MAX_NODES * 24 + overhead bytes of memory.
     *
     * This number MUST be greater than the maximum possible number
     * of nodes for the largest depth this stores.  Currently
     * we store up to depth 7, which has a maximum node count of 127
     * nodes.
     */
    private static final int MAX_NODES = 500;    
    
    /**
     * Mapping of Tree to all nodes in that tree.
     *
     * FixedsizeForgetfulHashMap is used because it keeps track
     * of which elements are most recently used, and provides a handy
     * "removeLRUEntry()" method.
     * The fixed-size portion is not used and is instead handled
     * by the maximum node size externally calculated.
     */
    private FixedsizeForgetfulHashMap /* of HashTree -> List */ MAP = 
        new FixedsizeForgetfulHashMap(MAX_NODES/2); // will never hit max.
        
    /**
     * The current amount of nodes stored in memory.
     */
    private int _currentNodes = 0;
    
    /**
     * Returns all intermediary nodes for the tree.
     */
    List /* of List of byte[] */ getAllNodes(HashTree tree) {
        int depth = tree.getDepth();
        if(tree.getDepth() == 0) {
            // trees of depth 0 have only one row.
            List outer = new ArrayList(1);
            outer.add(tree.getNodes());
            return outer;
        }else if (depth <2 || depth >= 7)
            // trees of depth 1 & 2 are really easy to calculate, so
            // always do those on the fly.
            // trees deeper than 7 take up too much memory to store,
            // so don't store them.
            return HashTree.createAllParentNodes(tree.getNodes());
        else 
            // other trees need to battle it out for storage.
            return getAllNodesImpl(tree);
    }
    
    /**
     * Registers the given list of nodes for the tree.
     */
    void register(HashTree tree, List nodes) {
        // don't register depths 0-2 and 7-11
        int depth = tree.getDepth();
        if(depth > 2 && depth < 7 && !MAP.containsKey(tree))
            insertEntry(tree, nodes);
    }

    /**
     * Returns all intermediary nodes for the tree.
     *
     * If the item already existed in the map, this refreshes that item
     * so that it is 'new' and then immediately returns it.
     * If the item did not already exist, this may purge the oldest items
     * from the map until enough room is available for this list of nodes
     * to be added.
     */
    private synchronized List getAllNodesImpl(HashTree tree) {
        List nodes = (List)MAP.get(tree);
        if(nodes != null) {
            // Make sure the map remembers that we want this entry.
            MAP.put(tree, nodes);
            return nodes;
        }
            
        nodes = HashTree.createAllParentNodes(tree.getNodes());
        insertEntry(tree, nodes);
        return nodes;
    }
    
    /**
     * Inserts the given entry into the Map, possibly purging older entries
     * in order to make room.
     */
    private synchronized void insertEntry(HashTree tree, List nodes) {
        int size = calculateSize(nodes);
        while(_currentNodes + size > MAX_NODES) {
            if(MAP.isEmpty())
                throw new IllegalStateException(
                    "current: " + _currentNodes + ", size: " + size);
            purgeLRU();
        }
        _currentNodes += size;
        MAP.put(tree, nodes);
    }
    
    /**
     * Purges the least recently used items from the map, decreasing
     * the _currentNodes size.
     */
    private synchronized void purgeLRU() {
        List nodes = (List)MAP.removeLRUEntry();
        _currentNodes -= calculateSize(nodes);
    }
    
    /**
     * Determines how many entries are within each list in this list.
     */
    private static int calculateSize(List /* of List */ nodes) {
        int size = 0;
        for(Iterator i = nodes.iterator(); i.hasNext(); )
            size += ((List)i.next()).size();
        return size;
    }
}
