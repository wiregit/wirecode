package com.limegroup.gnutella.dht;

import java.util.List;

import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.routing.PatriciaRouteTable;

public class LimeDHTRoutingTable extends PatriciaRouteTable {

    public LimeDHTRoutingTable(Context context) {
        super(context);
    }
    
    /**
     * Adds a DHT leaf node to the dht routing table, setting a very high timestamp
     * to make sure it always gets contacted first for the first hop of lookups
     * 
     * @param node The DHT leaf node to be added
     */
    public void addLeafDHTNode(ContactNode node) {
        node.setTimeStamp(Long.MAX_VALUE);
        super.add(node,true);
    }
    
    public void removeLeafDHTNode() {
    	//TODO implement
    }
}
