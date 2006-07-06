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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;



public class StatsManager {
    
    public static final StatsManager INSTANCE = new StatsManager();
    
    private static final String LOCALDB_FILE = "storedData";
    
    private static final String STATS_FILE = "dhtStats";
    
    private static final String ROUTINGTABLE_FILE = "routingTable";
    
    private final ArrayList dhtNodeStats = new ArrayList();
    
    private String outputDir = "";
    
    private StatsManager() {
    }

    public void addDHTNode(DHTStats stat) {
        synchronized (dhtNodeStats) {
            dhtNodeStats.add(stat);
        }
    }

    public void writeStatsToFiles() {
        try {
            File dbFile = new File(outputDir+LOCALDB_FILE);
            BufferedWriter dbWriter = new BufferedWriter(new FileWriter(dbFile));
            File statsFile = new File(outputDir+STATS_FILE);
            BufferedWriter stats = new BufferedWriter(new FileWriter(statsFile));
            File routingTableFile = new File(outputDir+ROUTINGTABLE_FILE);
            BufferedWriter routingTableWriter = new BufferedWriter(new FileWriter(routingTableFile));
            
            synchronized (dhtNodeStats) {
                for (Iterator iter = dhtNodeStats.iterator(); iter.hasNext();) {
                    DHTStats stat = (DHTStats) iter.next();
                    //write node db
                    stat.dumpDataBase(dbWriter);
                    //write routing table
                    stat.dumpRouteTable(routingTableWriter);
                    //write other stats
                    stat.dumpStats(stats, true);
                }
            }
            
            dbWriter.close();
            routingTableWriter.close();
            stats.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void setOutputDir(String path) {
        outputDir = path;
    }
}
