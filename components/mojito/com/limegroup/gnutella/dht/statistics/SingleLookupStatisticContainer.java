package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;

public abstract class SingleLookupStatisticContainer extends StatisticContainer {
    
    protected final KUID lookupKey;
    private final GlobalLookupStatisticContainer globalLookupStats;
    
    
    protected SingleLookupStatisticContainer(Context context, KUID lookupKey) {
        super();
        this.lookupKey = lookupKey;
        this.globalLookupStats = context.getGlobalLookupStats();
        globalLookupStats.addSingleLookupStatistic(this);
    }
    
    public void setHops(int hops, boolean findValue) {
        if(findValue) {
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_HOPS.incrementStat();
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_HOPS.storeCurrentStat();
        }
        globalLookupStats.GLOBAL_LOOKUP_HOPS.addData(hops);
        globalLookupStats.GLOBAL_LOOKUP_HOPS.storeCurrentStat();
    }
    
    public void setTime(int time, boolean findValue) {
        if(findValue) {
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_TIME.addData(time);
            globalLookupStats.GLOBAL_FIND_VALUE_LOOKUP_TIME.storeCurrentStat();
        }
        globalLookupStats.GLOBAL_LOOKUP_TIME.addData(time);
        globalLookupStats.GLOBAL_LOOKUP_TIME.storeCurrentStat();
    }
    
    public void addRequest() {
        globalLookupStats.GLOBAL_LOOKUP_REQUESTS.incrementStat();
    }

    public void addReply() {
        globalLookupStats.GLOBAL_LOOKUP_REPLIES.incrementStat();
    }
    
    public void addTimeout() {
        globalLookupStats.GLOBAL_LOOKUP_TIMEOUTS.incrementStat();
    }
    
    public void writeStats(Writer writer) throws IOException {
        writer.write("Lookup: "+lookupKey+"\n");
        super.writeStats(writer);
    }
    
}
