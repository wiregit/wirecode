/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
 
package com.limegroup.mojito.statistics;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.routing.RouteTable;


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
        Collection KeyVals = context.getDatabase().getValues();
        writer.write(nodeID+"\n");
        for (Iterator iter = KeyVals.iterator(); iter.hasNext();) {
            KeyValue keyval = (KeyValue) iter.next();
            writer.write(keyval.toString());
            writer.write("\n");
        }
        writer.write("--------------------------------------------\n");
        writer.flush();
    }
    
    public void dumpStats(Writer writer, boolean writeSingleLookups) throws IOException{
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID+"\n");
        synchronized (DHT_STATS) {
            for (Iterator iter = DHT_STATS.iterator(); iter.hasNext();) {
                StatisticContainer stat = (StatisticContainer) iter.next();
                if(!writeSingleLookups && (stat instanceof GlobalLookupStatisticContainer)) {
                    GlobalLookupStatisticContainer lookupStat = (GlobalLookupStatisticContainer) stat;
                    lookupStat.writeGlobalStats(writer);
                } else {
                    stat.writeStats(writer);
                }
            }
        }
        writer.write("--------------------------------------------\n");
        writer.flush();
    }

    public void dumpRouteTable(Writer writer) throws IOException{
        RouteTable routeTable = context.getRouteTable();
        if(nodeID == null) {
            nodeID = context.getLocalNodeID().toHexString();
        }
        writer.write(nodeID);
        writer.write(FILE_DELIMITER+routeTable.toString());
        writer.write("\n");
        writer.flush();
    }
}
