package com.limegroup.gnutella.dht.tests;

import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.db.KeyValue;

public class DHTLookupStat {

private final KeyValue value;

    private final long latency;
    
    private final int hops;
    
    private final Node node;
    
    private final boolean success;

    public DHTLookupStat(KeyValue value, long latency, int hops, Node node, boolean success) {
        this.hops = hops;
        this.latency = latency;
        this.node = node;
        this.value = value;
        this.success = success;
    }

    public String toString() {
        String delim = DHTNodeStat.FILE_DELIMITER;
        return value + delim + latency + delim + hops + delim + node + delim + success;
    }

    
    
}
