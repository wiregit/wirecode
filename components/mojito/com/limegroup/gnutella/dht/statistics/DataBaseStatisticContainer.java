package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;

public class DataBaseStatisticContainer extends StatisticContainer {

    public DataBaseStatisticContainer(Context context) {
        super(context);
    }

    public void writeStats(Writer writer) throws IOException {
        writer.write("DataBase Stats:\n");
        super.writeStats(writer);
    }
    
    /**
     * <tt>Statistic</tt> for the number of store forwards
     */
    public Statistic STORE_FORWARD_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for values that expire
     */
    public Statistic EXPIRED_VALUES =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for stored values
     */
    public Statistic STORED_VALUES =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for retrieved values
     */
    public Statistic RETRIEVED_VALUES =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for removed values
     */
    public Statistic REMOVED_VALUES =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for republished values
     */
    public Statistic REPUBLISHED_VALUES =
        new SimpleStatistic();
    
}
