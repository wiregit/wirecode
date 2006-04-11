package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;

public abstract class LookupStatisticContainer extends StatisticContainer {
    
    protected final KUID lookupKey;
    
    protected LookupStatisticContainer(Context context, KUID lookupKey) {
        super(context);
        this.lookupKey = lookupKey;
    }
    
    public abstract void setHops(int hops);
    
    public abstract void setTime(int time);
    
    public abstract void addRequest();

    public abstract void addReply();
    
    public abstract void addTimeout();
    
    //TODO: let's see if we need this
    protected class LookupStat extends AbstractStatistic{
        protected LookupStat() {}
    }

    public void writeStats(Writer writer) throws IOException {
        writer.append("Lookup: "+lookupKey+"\n");
        super.writeStats(writer);
    }
    
    
}
