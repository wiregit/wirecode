package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.db.KeyValue;

public interface DHTStats {

    public void recordLookup(KeyValue value, long latency, int hops, Node node, boolean success,boolean isStore);
    
    public void dumpRouteTable(Writer writer) throws IOException;
    
    public void dumpDataBase(Writer writer) throws IOException;
    
    public void dumpStores(Writer writer) throws IOException;
    
    public void dumpGets(Writer writer) throws IOException;
}
