package com.limegroup.gnutella.dht.statistics;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;

public class FindValueLookupStatisticContainer extends LookupStatisticContainer{
    
    /**
     * <tt>Statistic</tt> for all outgoing lookup messages for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_REQUESTS =
        new LookupStat();
    
    /**
     * <tt>Statistic</tt> for all incoming lookup messages for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_REPLIES =
        new LookupStat();
    
    /**
     * <tt>Statistic</tt> for timeouts for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_TIMEOUTS=
        new LookupStat();
    
    /**
     * <tt>Statistic</tt> for the number of hops for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_HOPS =
        new LookupStat();
    
    /**
     * <tt>Statistic</tt> for the time of this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_TIME =
        new LookupStat();
    
    
    public FindValueLookupStatisticContainer(Context context, KUID lookupKey) {
        super(context, lookupKey);
    }


    public void setHops(int hops) {
        FIND_VALUE_LOOKUP_HOPS.addData(hops);
        FIND_VALUE_LOOKUP_HOPS.storeCurrentStat();
    }
    
    public void setTime(int time) {
        FIND_VALUE_LOOKUP_TIME.addData(time);
        FIND_VALUE_LOOKUP_TIME.storeCurrentStat();
    }


    public void addReply() {
        FIND_VALUE_LOOKUP_REPLIES.incrementStat();
    }


    public void addRequest() {
        FIND_VALUE_LOOKUP_REQUESTS.incrementStat();
    }


    public void addTimeout() {
        FIND_VALUE_LOOKUP_TIMEOUTS.incrementStat();
    }
}
