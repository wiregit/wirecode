package com.limegroup.gnutella.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.NumericBuffer;
import org.limewire.inspection.Inspectable;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.StatsUtils;

/**
 * Keeps track and reports some statistics about local queries.
 */
public class QueryStats implements Inspectable {
    
    private NumericBuffer<Long> times = new NumericBuffer<Long>(200);
    
    public void recordQuery() {
        times.add(System.currentTimeMillis());
    }
    public long getLastQueryTime() {
        if (times.isEmpty())
            return 0;
        return times.first();
    }
    
    public Object inspect() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("ver",1);

        // if there are less than 10 items (80 bytes) just send them
        if (times.getSize() <= 10) {
            List<Long> l = new ArrayList<Long>(10);
            for(long time: times)
                l.add(time - RouterService.startTime);
            ret.put("list",l);
            return ret;
        }

        // otherwise send the first 10 and some stats too
        List<Double> l = new ArrayList<Double>();
        for (long time : times)
            l.add((double)(time - RouterService.startTime));
        Collections.sort(l);
        ret.put("times",StatsUtils.quickStatsDouble(l).getMap()); // ~100 bytes
        ret.put("timesh", StatsUtils.getHistogram(l, 20)); // ~80 bytes
        ret.put("first10",l.subList(0,10)); // ~80 bytes
        return ret;
    }
    
}
