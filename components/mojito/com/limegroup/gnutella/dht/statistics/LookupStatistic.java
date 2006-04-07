package com.limegroup.gnutella.dht.statistics;

public class LookupStatistic extends AbstractStatistic{

    /**
     * Make the constructor private so that only this class can construct
     * a <tt>LookupStatistic</tt> instances.
     */
    public LookupStatistic() {}
    
    /**
     * <tt>Statistic</tt> for all outgoing lookup messages for this lookup.
     */
    public final Statistic LOOKUP_REQUESTS =
        new LookupStatistic();
    
    /**
     * <tt>Statistic</tt> for all incoming lookup messages for this lookup.
     */
    public final Statistic LOOKUP_REPLIES =
        new LookupStatistic();
    
    /**
     * <tt>Statistic</tt> for hops for this lookup.
     */
    public final Statistic LOOKUP_HOPS =
        new LookupStatistic();
    
    /**
     * <tt>Statistic</tt> for timeouts for this lookup.
     */
    public final Statistic LOOKUP_TIMEOUTS=
        new LookupStatistic();
    
}
