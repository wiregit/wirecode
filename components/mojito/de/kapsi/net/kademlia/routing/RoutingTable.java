package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;

public interface RoutingTable {
    
    public void clear();
    
    public boolean isEmpty();
    
    public int size();
    
    public void add(ContactNode node, boolean knownToBeAlive);
    
    public ContactNode get(KUID nodeId);
    
    public ContactNode get(KUID nodeId, boolean checkAndUpdateCache);
    
    public ContactNode selectNextClosest(KUID key);

    public ContactNode select(KUID key);
    
    public List select(KUID lookup, int k, boolean onlyLiveNodes, boolean isLocalLookup);
    
    public boolean containsNode(KUID nodeId);
    
    public void handleFailure(KUID nodeId);
    
    public Collection getAllNodes();
    
    public Collection getAllBuckets();
    
    public void refreshBuckets(boolean force) throws IOException;
    
    /**
     * Refreshes the routing table's buckets
     * 
     * @param force true to refresh all buckets, false otherwise
     * @param l the BootstrapListener callback
     * @throws IOException
     */
    public void refreshBuckets(boolean force, BootstrapListener l) throws IOException;
    
    public boolean load();
    public boolean store();
}
