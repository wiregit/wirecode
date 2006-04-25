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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.routing.RoutingTable;

public class DHTNodeStat implements DHTStats{

    public static final String FILE_DELIMITER = "\t";
    
    private final Context context;
    
    private String nodeID;

    /**
     * <tt>List</tt> of all statistics classes.
     */
    private List DHT_STATS = new LinkedList();
    
    public DHTNodeStat(Context context) {
        this.context = context;
    }
    
    public void addStatisticContainer(StatisticContainer statsContainer) {
        synchronized (DHT_STATS) {
            DHT_STATS.add(statsContainer);
        }
    }
    
    public void dumpDataBase(Writer writer) throws IOException{
        List KeyVals = context.getDatabase().getAllValues();
        for (Iterator iter = KeyVals.iterator(); iter.hasNext();) {
            KeyValue keyval = (KeyValue) iter.next();
            writeNodeStat(writer,keyval.toString());
            writer.write("\n");
        }
    }
    
    public void dumpStats(Writer writer, boolean writeSingleLookups) throws IOException{
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID+"\n");
        synchronized (DHT_STATS) {
            for (Iterator iter = DHT_STATS.iterator(); iter.hasNext();) {
                StatisticContainer stat = (StatisticContainer) iter.next();
                if(!writeSingleLookups && (stat instanceof SingleLookupStatisticContainer)) {
                    continue;
                }
                stat.writeStats(writer);
            }
        }
        writer.write("--------------------------------------------\n");
    }

    public void dumpRouteTable(Writer writer) throws IOException{
        RoutingTable routeTable = context.getRouteTable();
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID);
        writer.write(FILE_DELIMITER+routeTable.toString());
        writer.write("\n");
    }
    
    public void writeNodeStat(Writer writer,String stat) throws IOException {
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID + FILE_DELIMITER + stat);
    }
}
