package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;

public class NodeAssignerStat extends AdvancedStatistic {

    /**
     * Make the constructor private so that only this class can construct
     * an <tt>NodeAssignerStat</tt> instances.
     */
    private NodeAssignerStat() {}
    
    /**
     * Statistic for the number of ultrapeer attemps initiated by the NodeAssigner
     */
    public static final Statistic ULTRAPEER_ASSIGNMENTS = new NodeAssignerStat();
    
    /**
     * Statistic for passive DHT assignments
     */
    public static final Statistic PASSIVE_DHT_ASSIGNMENTS = new NodeAssignerStat();
    
    /**
     * Statistic for passive DHT disconnections
     */
    public static final Statistic PASSIVE_DHT_DISCONNECTIONS = new NodeAssignerStat();
    
    /**
     * Statistic for Active DHT assignments
     */
    public static final Statistic ACTIVE_DHT_ASSIGNMENTS = new NodeAssignerStat();
    
    /**
     * Statistic for Active DHT disconnections
     */
    public static final Statistic ACTIVE_DHT_DISCONNECTIONS = new NodeAssignerStat();

    /**
     * Statistic for the number of switches from Active DHT node to ultrapeer (Passive DHT)
     */
    public static final Statistic UP_TO_DHT_SWITCHES = new NodeAssignerStat();
    
}
