package com.limegroup.gnutella.util;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.downloader.Interval;

/**
 * A "range" version of IntSet. This is a first cut of the class and does
 * not support all the operations IntSet does, just the ones we need for now.
 * <p>
 * Important Note: This class uses Interval from the download package. Ideally,
 * classes in the util package should be stand alone, but we need to have 
 * Interval stay in downloads for reasons of backward compatibility.
 */
public class IntervalSet {
    
    private List /*of Interval*/ intervals;
    
    /**
     * INVARIANT: size==sum over all i of (get(i).high-get(i).low+1)
     */
    private int size = 0;
    
    public IntervalSet() {
        intervals = new ArrayList();
    }


    /**
     * @return true if the interval overlaps with anything already added
     * but returns false if no overlap or if this interval "touches" another
     * interval
     public boolean add(int low, int high) throws IllegalArgumentException {
     if(low>high)
     throw new IllegalArgumentException();
     int numPoints = high-low+1;     
     }
    */
    public void add(Interval interval) {
        //TODO1: We should join adjacent Intervals here.
        intervals.add(interval);
    }

    /**
     *@return a List of intervals that overlap checkInterval. For example
     * if Intervals contains{[1-4],[6-10]} and checkInterval is [3-8],
     * this method should return a list of 2 intervals {[3-4],[6-8]}
     * If there are no overlaps, this method returns an empty List.
     */
    public List getOverlapIntervals(Interval checkInterval) {
        List overlapBlocks = new ArrayList(); //initialize for this write
        long high =checkInterval.high;
        long low = checkInterval.low;
        long numBytes = high-low+1;
        if (low >= high)
            return overlapBlocks;
        
        for(Iterator iter = intervals.iterator(); iter.hasNext(); ) {
            Interval interval = (Interval)iter.next();
            //case a:
            if(low <= interval.low && interval.high <= high) {
                //Need to check the whole iterval, starting point=interval.low
                overlapBlocks.add(interval);
                continue;
            }
            //case b:
            if(low<=interval.high && interval.low < low) {
                overlapBlocks.add(new Interval((int)low,
                                           Math.min((int)high,interval.high)));
            }
            //case c:
            if(interval.low <= high && interval.high > high) {
                overlapBlocks.add(new Interval(Math.max(interval.low,(int)low),
                                               (int)high));
            }
            //Note: There is one condition under which case b and c are both
            //true. In this case the same interval will be added twice. The
            //effect of this is that we will check the same overlap interval 
            //2 times. We are still doing it this way, beacuse this conditon
            //will not happen in practice, and the code looks better this way, 
            //and finally, it cannot do any harm - the worst that can happen is
            //that we check the exact same interval twice.
        }
        return overlapBlocks;
    }

}
