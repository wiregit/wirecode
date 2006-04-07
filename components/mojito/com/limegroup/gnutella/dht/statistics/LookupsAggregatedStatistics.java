package com.limegroup.gnutella.dht.statistics;

import java.util.LinkedList;

public class LookupsAggregatedStatistics extends AbstractStatistic {
    
    private final LinkedList LOOKUPS_LIST = new LinkedList();
    
    protected LookupsAggregatedStatistics() {
        StatsManager.INSTANCE.addDHTStat(this);
    }
    
    public void addLookupStatistic(LookupStatistic lookupStat) {
        LOOKUPS_LIST.add(lookupStat);
    }
    
    public void storeCurrentStat() {
        //here we write the individual stats
        
    }

    public static final LookupsAggregatedStatistics FIND_NODE_LOOKUPS = 
        new LookupsAggregatedStatistics();
    
    
    public static final LookupsAggregatedStatistics FIND_VALUE_LOOKUPS = 
        new LookupsAggregatedStatistics();
    

}
