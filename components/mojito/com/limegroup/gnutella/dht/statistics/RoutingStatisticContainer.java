package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;

public class RoutingStatisticContainer extends StatisticContainer {

    public RoutingStatisticContainer(Context context) {
        super(context);
    }
    
    public void writeStats(Writer writer) throws IOException {
        writer.write("Routing Stats:\n");
        super.writeStats(writer);
    }

    /**
     * <tt>Statistic</tt> for the number of contacts added to the main routing table
     */
    public Statistic NODE_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of contacts added to the main routing table
     */
    public Statistic BUCKET_COUNT =
        new SimpleStatistic();

    /**
     * <tt>Statistic</tt> for the number of bucket refreshes
     */
    public Statistic BUCKET_REFRESH_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of replacement contacts added
     */
    public Statistic REPLACEMENT_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of dead contacts
     */
    public Statistic DEAD_NODE_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for contacts trying to spoof node ids
     */
    public Statistic SPOOF_COUNT =
        new SimpleStatistic();

}
