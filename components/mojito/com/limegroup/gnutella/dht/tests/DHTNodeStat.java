package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.routing.RouteTable;

public class DHTNodeStat implements DHTStats{

    public static final String FILE_DELIMITER = "\t";
    
    private final Context context;
    
    private final String nodeID;

    private HashMap valueStores;
    
    private HashMap valueLookups;
    
    public DHTNodeStat(Context context) {
        this.context = context;
        nodeID = context.getLocalNodeID().toHexString();
    }

    
    public void dumpDataBase(Writer writer) throws IOException{
        List KeyVals = context.getDatabase().getValues();
        for (Iterator iter = KeyVals.iterator(); iter.hasNext();) {
            KeyValue keyval = (KeyValue) iter.next();
            writeNodeStat(writer,keyval.toString());
        }
    }

    public void dumpRouteTable(Writer writer) throws IOException{
        RouteTable routeTable = context.getRouteTable();
        Collection nodes = routeTable.getAllNodes();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            Node node = (Node) iter.next();
            writeNodeStat(writer,node.getNodeID().toHexString());
        }
    }
    
    public void dumpGets(Writer writer) throws IOException {
        for (Iterator iter = valueLookups.values().iterator(); iter.hasNext();) {
            DHTLookupStat stat = (DHTLookupStat)iter.next();
            writeNodeStat(writer,stat.toString());
        }
    }

    public void dumpStores(Writer writer) throws IOException {
        for (Iterator iter = valueStores.values().iterator(); iter.hasNext();) {
            DHTLookupStat stat = (DHTLookupStat)iter.next();
            writeNodeStat(writer,stat.toString());
        }
    }


    public void recordLookup(KeyValue value, long latency, int hops, Node node, boolean success,boolean isStore) {
        DHTLookupStat stat = new DHTLookupStat(value,latency,hops,node,success);
        if(isStore) valueStores.put(value.getKey(),stat);
        else valueLookups.put(value.getKey(),stat);
    }
    
    public void writeNodeStat(Writer writer,String stat) throws IOException {
        writer.write(nodeID + FILE_DELIMITER + stat);
    }
}
