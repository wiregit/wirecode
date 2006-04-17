package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;

public class PlanetLabTestsStatContainer extends StatisticContainer {

    public PlanetLabTestsStatContainer(Context context) {
        super(context);
    }

    public void writeStats(Writer writer) throws IOException {
        writer.write("PlanetLab Stats:\n");
        super.writeStats(writer);
    }
    
    /**
     * <tt>Statistic</tt> for churn disconnect
     */
    public Statistic CHURN_DISCONNECTS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for churn reconnects
     */
    public Statistic CHURN_RECONNECTS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of publishes
     */
    public Statistic PUBLISH_COUNT =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of publish locations
     */
    public Statistic PUBLISH_LOCATIONS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of successfull retrievals
     */
    public Statistic RETRIEVE_SUCCESS =
        new SimpleStatistic();
    
    /**
     * <tt>Statistic</tt> for the number of retrieval failures
     */
    public Statistic RETRIEVE_FAILURES =
        new SimpleStatistic();
}
