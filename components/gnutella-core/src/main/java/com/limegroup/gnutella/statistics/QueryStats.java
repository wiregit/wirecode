package com.limegroup.gnutella.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.NumericBuffer;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.statistic.StatsUtils;
import org.limewire.util.ByteOrder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.LifecycleManager;

/**
 * Keeps track and reports some statistics about local queries.
 */
@Singleton
public class QueryStats {
    
    private NumericBuffer<Long> times = new NumericBuffer<Long>(200);
    
    private final LifecycleManager lifecycleManager;
    
    @Inject
    public QueryStats(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }
    
    public synchronized void recordQuery() {
        times.add(System.currentTimeMillis());
    }
    
    public synchronized long getLastQueryTime() {
        if (times.isEmpty())
            return 0;
        return times.first();
    }

    @InspectionPoint("user query stats")
    @SuppressWarnings("unused")
    private final Inspectable queryStats = new Inspectable() {
        public Object inspect() {
            synchronized(QueryStats.this) {
                Map<String, Object> ret = new HashMap<String, Object>();
                /*
                 * version 1 used to send 10 search times in milliseconds
                 */
                ret.put("ver",2);

                // if there are less than 200 items (800 bytes) just send them
                if (times.getSize() <= 200) {
                    byte[] b = new byte[times.getSize() * 4];
                    for (int i = 0; i < times.getSize(); i++) {
                        int time = Float.floatToIntBits((times.get(i)-lifecycleManager.getStartFinishedTime()) / 1000f);
                        ByteOrder.int2beb(time, b, i*4);
                    }

                    ret.put("buf",b);
                    return ret;
                }

                // otherwise send the first 10 and some stats too
                List<Double> l = new ArrayList<Double>();
                for (long time : times)
                    l.add((double)(time - lifecycleManager.getStartFinishedTime()));
                Collections.sort(l);
                ret.put("times",StatsUtils.quickStatsDouble(l).getMap()); // ~100 bytes
                ret.put("timesh", StatsUtils.getHistogram(l, 20)); // ~80 bytes
                ret.put("first10",l.subList(0,10)); // ~80 bytes
                return ret;
            }
        }
    };
    
}
