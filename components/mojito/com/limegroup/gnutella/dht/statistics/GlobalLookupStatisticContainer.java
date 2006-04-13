package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;

import de.kapsi.net.kademlia.Context;

public class GlobalLookupStatisticContainer extends StatisticContainer {

    public GlobalLookupStatisticContainer(Context context) {
        super(context);
    }
    
    /**
    * <tt>Statistic</tt> for all outgoing lookup messages
    */
   public Statistic GLOBAL_LOOKUP_REQUESTS =
       new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for all incoming lookup messages
    */
   public Statistic GLOBAL_LOOKUP_REPLIES =
       new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for timeouts
    */
   public Statistic GLOBAL_LOOKUP_TIMEOUTS=
       new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the number of hops
    */
   public Statistic GLOBAL_LOOKUP_HOPS =
       new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the time
    */
   public Statistic GLOBAL_LOOKUP_TIME =
       new SimpleStatistic();
   
   public void writeStats(Writer writer) throws IOException {
       writer.write("Global lookups: \n");
       super.writeStats(writer);
   }

}
