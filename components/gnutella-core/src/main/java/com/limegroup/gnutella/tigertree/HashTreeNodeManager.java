pbckage com.limegroup.gnutella.tigertree;

import jbva.util.ArrayList;
import jbva.util.Iterator;
import jbva.util.List;

import com.limegroup.gnutellb.util.FixedsizeForgetfulHashMap;

/**
 * Mbnages access to the list of full nodes for a HashTree.
 * This tries to keep b maximum amount of nodes in memory, purging
 * the lebst recently used items when the threshold is reached.
 */
clbss HashTreeNodeManager {
    
    privbte static final HashTreeNodeManager INSTANCE =
        new HbshTreeNodeManager();
    public stbtic HashTreeNodeManager instance()  { return INSTANCE; }
    privbte HashTreeNodeManager() {}
    
    /**
     * The mbximum amount of nodes to store in memory.
     *
     * This will use up MAX_NODES * 24 + overhebd bytes of memory.
     *
     * This number MUST be grebter than the maximum possible number
     * of nodes for the lbrgest depth this stores.  Currently
     * we store up to depth 7, which hbs a maximum node count of 127
     * nodes.
     */
    privbte static final int MAX_NODES = 500;    
    
    /**
     * Mbpping of Tree to all nodes in that tree.
     *
     * FixedsizeForgetfulHbshMap is used because it keeps track
     * of which elements bre most recently used, and provides a handy
     * "removeLRUEntry()" method.
     * The fixed-size portion is not used bnd is instead handled
     * by the mbximum node size externally calculated.
     */
    privbte FixedsizeForgetfulHashMap /* of HashTree -> List */ MAP = 
        new FixedsizeForgetfulHbshMap(MAX_NODES/2); // will never hit max.
        
    /**
     * The current bmount of nodes stored in memory.
     */
    privbte int _currentNodes = 0;
    
    /**
     * Returns bll intermediary nodes for the tree.
     */
    List /* of List of byte[] */ getAllNodes(HbshTree tree) {
        int depth = tree.getDepth();
        if(tree.getDepth() == 0) {
            // trees of depth 0 hbve only one row.
            List outer = new ArrbyList(1);
            outer.bdd(tree.getNodes());
            return outer;
        }else if (depth <2 || depth >= 7)
            // trees of depth 1 & 2 bre really easy to calculate, so
            // blways do those on the fly.
            // trees deeper thbn 7 take up too much memory to store,
            // so don't store them.
            return HbshTree.createAllParentNodes(tree.getNodes());
        else 
            // other trees need to bbttle it out for storage.
            return getAllNodesImpl(tree);
    }
    
    /**
     * Registers the given list of nodes for the tree.
     */
    void register(HbshTree tree, List nodes) {
        // don't register depths 0-2 bnd 7-11
        int depth = tree.getDepth();
        if(depth > 2 && depth < 7 && !MAP.contbinsKey(tree))
            insertEntry(tree, nodes);
    }

    /**
     * Returns bll intermediary nodes for the tree.
     *
     * If the item blready existed in the map, this refreshes that item
     * so thbt it is 'new' and then immediately returns it.
     * If the item did not blready exist, this may purge the oldest items
     * from the mbp until enough room is available for this list of nodes
     * to be bdded.
     */
    privbte synchronized List getAllNodesImpl(HashTree tree) {
        List nodes = (List)MAP.get(tree);
        if(nodes != null) {
            // Mbke sure the map remembers that we want this entry.
            MAP.put(tree, nodes);
            return nodes;
        }
            
        nodes = HbshTree.createAllParentNodes(tree.getNodes());
        insertEntry(tree, nodes);
        return nodes;
    }
    
    /**
     * Inserts the given entry into the Mbp, possibly purging older entries
     * in order to mbke room.
     */
    privbte synchronized void insertEntry(HashTree tree, List nodes) {
        int size = cblculateSize(nodes);
        while(_currentNodes + size > MAX_NODES) {
            if(MAP.isEmpty())
                throw new IllegblStateException(
                    "current: " + _currentNodes + ", size: " + size);
            purgeLRU();
        }
        _currentNodes += size;
        MAP.put(tree, nodes);
    }
    
    /**
     * Purges the lebst recently used items from the map, decreasing
     * the _currentNodes size.
     */
    privbte synchronized void purgeLRU() {
        List nodes = (List)MAP.removeLRUEntry();
        _currentNodes -= cblculateSize(nodes);
    }
    
    /**
     * Determines how mbny entries are within each list in this list.
     */
    privbte static int calculateSize(List /* of List */ nodes) {
        int size = 0;
        for(Iterbtor i = nodes.iterator(); i.hasNext(); )
            size += ((List)i.next()).size();
        return size;
    }
}
