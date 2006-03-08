package com.limegroup.gnutella.dht.tests;

import java.util.Collection;

import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;

public interface DHTStats {

    public void valueStored(KeyValue value, Node node);
    
    public void valueRetrieved(KeyValue value, long latency, int hops);
    
    //public void dumpRouteTable(RouteTable routeTable);
    public void dumpRouteTable(Collection nodes);
    
    public void dumpDataBase(Database db);
    
    public void persistLocalNodeInfo(Node localNode);
}
