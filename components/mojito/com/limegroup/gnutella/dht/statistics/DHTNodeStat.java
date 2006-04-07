/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.routing.RoutingTable;

public class DHTNodeStat implements DHTStats{

    public static final String FILE_DELIMITER = "\t";
    
    private final Context context;
    
    private String nodeID;

    private final HashMap valueStores = new HashMap();
    
    private final HashMap valueLookups = new HashMap();
    
    public DHTNodeStat(Context context) {
        this.context = context;
    }

    
    public void dumpDataBase(Writer writer) throws IOException{
        List KeyVals = context.getDatabase().getAllValues();
        for (Iterator iter = KeyVals.iterator(); iter.hasNext();) {
            KeyValue keyval = (KeyValue) iter.next();
            writeNodeStat(writer,keyval.toString());
            writer.write("\n");
        }
    }

    public void dumpRouteTable(Writer writer) throws IOException{
        RoutingTable routeTable = context.getRouteTable();
        Collection nodes = routeTable.getAllNodes();
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID);
        writer.write(FILE_DELIMITER+routeTable.toString());
        writer.write("\n");
    }
    
    public void dumpGets(Writer writer) throws IOException {
        for (Iterator iter = valueLookups.values().iterator(); iter.hasNext();) {
            DHTLookupStat stat = (DHTLookupStat)iter.next();
            writeNodeStat(writer,stat.toString());
            writer.write("\n");
        }
    }

    public void dumpStores(Writer writer) throws IOException {
        for (Iterator iter = valueStores.values().iterator(); iter.hasNext();) {
            DHTLookupStat stat = (DHTLookupStat)iter.next();
            writeNodeStat(writer,stat.toString());
            writer.write("\n");
        }
    }


    public void recordLookup(KeyValue value, long latency, int hops, ContactNode node, boolean success,boolean isStore) {
        DHTLookupStat stat = new DHTLookupStat(value,latency,hops,node,success);
        if(isStore) valueStores.put(value.getKey(),stat);
        else valueLookups.put(value.getKey(),stat);
    }
    
    public void writeNodeStat(Writer writer,String stat) throws IOException {
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID + FILE_DELIMITER + stat);
    }
}
