package com.limegroup.gnutella.dht.statistics;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;

import de.kapsi.net.kademlia.Context;

public class GlobalLookupStatisticContainer extends StatisticContainer {
    
    private static final int MAX_LOOKUPS = 20;
    private LinkedList singleLookups = new LinkedList();
    

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
   
   /**
    * <tt>Statistic</tt> for the FIND_VALUE times
    */
   public Statistic GLOBAL_FIND_VALUE_LOOKUP_TIME =
       new SimpleStatistic();
   
   /**
    * <tt>Statistic</tt> for the FIND_VALUE hops
    */
   public Statistic GLOBAL_FIND_VALUE_LOOKUP_HOPS =
       new SimpleStatistic();
   
   public void addSingleLookupStatistic(SingleLookupStatisticContainer lookupStat) {
       synchronized (singleLookups) {
           singleLookups.add(lookupStat);
           if(singleLookups.size() > MAX_LOOKUPS) {
               singleLookups.removeFirst();
           }
       }
   }
   
   public void writeStats(Writer writer) throws IOException {
       writer.write("Global lookups: \n");
       super.writeStats(writer);
       synchronized (singleLookups) {
           for (Iterator iter = singleLookups.iterator(); iter.hasNext();) {
               StatisticContainer stat = (StatisticContainer) iter.next();
               stat.writeStats(writer);
           }
       }
   }
   
   public void writeGlobalStats(Writer writer) throws IOException {
       writer.write("Global lookups: \n");
       super.writeStats(writer);
   }

}
