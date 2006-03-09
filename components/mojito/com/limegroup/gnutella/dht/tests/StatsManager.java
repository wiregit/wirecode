package com.limegroup.gnutella.dht.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.routing.RouteTable;


public class StatsManager {
    
    public static final StatsManager INSTANCE = new StatsManager();
    
    private static final String LOCALDB_FILE = "storedData";
    
    private static final String VALUE_STORES_FILE = "publishedData";
    
    private static final String VALUE_LOOKUPS_FILE = "lookedupData";
    
    private static final String ROUTINGTABLE_FILE = "routingTable";
    
    private final ArrayList dhtNodeStats = new ArrayList();
    
    private StatsManager() {
    }

    public void addDHTNode(DHTStats stat) {
        dhtNodeStats.add(stat);
    }

    public void writeStatsToFiles() {
        try {
            File dbFile = new File(LOCALDB_FILE);
            BufferedWriter dbWriter = new BufferedWriter(new FileWriter(dbFile));
            File storesFile = new File(VALUE_STORES_FILE);
            BufferedWriter storesWriter = new BufferedWriter(new FileWriter(storesFile));
            File lookupsFile = new File(VALUE_LOOKUPS_FILE);
            BufferedWriter lookupsWriter = new BufferedWriter(new FileWriter(lookupsFile));
            File routingTableFile = new File(ROUTINGTABLE_FILE);
            BufferedWriter routingTableWriter = new BufferedWriter(new FileWriter(routingTableFile));
            for (Iterator iter = dhtNodeStats.iterator(); iter.hasNext();) {
                DHTStats stat = (DHTStats) iter.next();
                //write node db
                stat.dumpDataBase(dbWriter);
                //write routing table
                stat.dumpRouteTable(routingTableWriter);
                //write gets
                stat.dumpGets(lookupsWriter);
                //write stores
                stat.dumpStores(storesWriter);
            }
            dbWriter.close();
            routingTableWriter.close();
            lookupsWriter.close();
            storesWriter.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
