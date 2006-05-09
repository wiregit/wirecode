package com.limegroup.gnutella.dht.statistics;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;

public class FindValueLookupStatisticContainer extends SingleLookupStatisticContainer{
    
    /**
     * <tt>Statistic</tt> for all outgoing lookup messages for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_REQUESTS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for all incoming lookup messages for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_REPLIES =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for timeouts for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_TIMEOUTS=
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of hops for this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_HOPS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the time of this lookup.
     */
    public Statistic FIND_VALUE_LOOKUP_TIME =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the values found.
     */
    public Statistic FIND_VALUE_OK =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the values not found.
     */
    public Statistic FIND_VALUE_FAILURE =
        new SimpleStatistic();
    
    
    
    public FindValueLookupStatisticContainer(Context context, KUID lookupKey) {
        super(context, lookupKey);
    }


    public void setHops(int hops, boolean findValue) {
        super.setHops(hops, true);
        FIND_VALUE_LOOKUP_HOPS.addData(hops);
        FIND_VALUE_LOOKUP_HOPS.storeCurrentStat();
    }
    
    public void setTime(int time, boolean findValue) {
        super.setTime(time, true);
        FIND_VALUE_LOOKUP_TIME.addData(time);
        FIND_VALUE_LOOKUP_TIME.storeCurrentStat();
    }


    public void addReply() {
        super.addReply();
        FIND_VALUE_LOOKUP_REPLIES.incrementStat();
    }


    public void addRequest() {
        super.addRequest();
        FIND_VALUE_LOOKUP_REQUESTS.incrementStat();
    }


    public void addTimeout() {
        super.addTimeout();
        FIND_VALUE_LOOKUP_TIMEOUTS.incrementStat();
    }
}
