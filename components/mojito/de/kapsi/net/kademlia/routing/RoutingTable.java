package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;

public interface RoutingTable {
    
    public void clear();
    
    public boolean isEmpty();
    
    public int size();
    
    public void add(ContactNode node, boolean knownToBeAlive);
    
    public ContactNode get(KUID nodeId);
    
    public ContactNode get(KUID nodeId, boolean checkAndUpdateCache);
    
    public ContactNode selectNextClosest(KUID key);

    public ContactNode select(KUID key);
    
    public List select(KUID lookup, int k);
    
    public boolean containsNode(KUID nodeId);
    
    public boolean updateTimeStamp(ContactNode node);
    
    public void handleFailure(KUID nodeId);
    
    public Collection getAllNodes();
    
    public Collection getAllBuckets();
    
    public void refreshBuckets() throws IOException;
    
}
