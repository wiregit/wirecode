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
    
    //constructor.
    public IntervalSet() {
        intervals = new ArrayList();
    }

    public void add(Interval addInterval) {
        int low = addInterval.low;
        int high = addInterval.high;
        Interval lower=null;
        Interval higher=null;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            Interval interval=(Interval)iter.next();
            if (low<=interval.low && interval.high<=high) {//  <low-------high>
                iter.remove();                             //      interval
                continue;
            }

            if (low<=interval.high && interval.low<low)    //     <low, high>
                lower=interval;                            //  interval........

            if (interval.high == low-1)             //             <low,high>
                lower=interval;                     // ....interval

            if (interval.low<=high && interval.high>high)  //     <low, high>
                higher=interval;                           //  .........interval

            if (high == interval.low-1)              //  <low, high>
                higher = interval;                   //             ...interval
        }

        //Add block.  Note that remove(..) is linear time.  That's not an issue
        //because there are typically few blocks.
        if (lower==null && higher==null) {
            //a) Doesn't overlap
            intervals.add(new Interval(low, high));
        } else if (lower!=null && higher!=null) {
            //b) Join two blocks
            intervals.remove(higher);
            intervals.remove(lower);
            intervals.add(new Interval(lower.low, higher.high));
        } else if (higher!=null) {
            //c) Join with higher
            intervals.remove(higher);
            intervals.add(new Interval(low, higher.high));
        } else if (lower!=null) {
            //d) Join with lower
            intervals.remove(lower);
            intervals.add(new Interval(lower.low, high));
        }   
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
        
        //TODO2:For now we iterate over each of the inervals we have, 
        //but there should be a faster way of finding which intrevals we 
        //can overlap, Actually there is a max of  two intervals we can overlap
        //one on the top end and one on the bottom end. We need to make this 
        //more efficient
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

    public Iterator getAllIntervals() {
        return intervals.iterator();
    }

    public List getAllIntervalsAsList() {
        return intervals;
    }

    public int getSize() {
        int sum=0;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            Interval block=(Interval)iter.next();
            sum+=block.high-block.low+1;
        }
        return sum;
    }
    
    public void clear() {
        intervals.clear();
    }

    /**
     * @return an iterator or intervals needed to fill in the holes in this
     * IntervalSet. Note that the IntervalSet does not know the maximum value of
     * all the intervals.
     */
    public Iterator getNeededIntervals(int maxSize) {
        if (intervals==null || intervals.size()==0) {//Nothing recorded?
            Interval block=new Interval(0, maxSize-1);
            List buf=new ArrayList(); 
            buf.add(block);
            return buf.iterator();
        }
            
        //Sort list by low point in ascending order.  This has a side effect but
        //it doesn't matter.
        Collections.sort(intervals, new IntervalComparator());

        //Now step through list one element at a time, putting gaps into buf.
        //We take advantage of the fact that intervals are disjoint.  Treat
        //beginning specially.  
        //LOOP INVARIANT: interval!=null ==> low==interval.high
        List buf=new ArrayList();
        int low=-1;
        Interval interval=null;
        for (Iterator iter=intervals.iterator(); iter.hasNext(); ) {
            interval=(Interval)iter.next();
            if (interval.low!=0 && low<interval.low)//needed for first interval
                buf.add(new Interval(low+1, interval.low-1));
            low=interval.high;
        }
        //Special case space between last block and end of file.
        Assert.that(interval!=null, "Null interval in getFreeBlocks");
        if (interval.high < maxSize-1)
            buf.add(new Interval(interval.high+1, maxSize-1));
        
        return buf.iterator();

    }

    private class IntervalComparator implements Comparator {
        public int compare(Object a, Object b) {
            Interval ia=(Interval)a;
            Interval ib=(Interval)b;
            return ia.low-ib.low;
        }
    }
}
