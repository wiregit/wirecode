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
            if (low<interval.low)      //needed for first interval
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


    //////////////////////////Unit Test/////////////////////////////
    public static void main(String[] args) {
        IntervalSet iSet = new IntervalSet();
        Interval interval = new Interval(40,45);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==1,"add method broken");
        Assert.that(iSet.getSize()==6,"getSize() broken");
        Iterator iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==39,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==46,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
        ///testing case 1 (intervals is now {[40-45]}
        interval = new Interval(35,39);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==1,"lower boundry colating failed");
        interval = iSet.getIntervalAt(0);
        Assert.that(interval.high==45,"lower boundry colating failed");
        Assert.that(interval.low==35,"lower boundry colating failed");
        Assert.that(iSet.getSize()==11,"getSize() broken");
        //testing case 2 intervals is now {[35-45]}
        interval = new Interval(30,37);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==1,"lower overlap colating failed");
        interval = iSet.getIntervalAt(0);
        Assert.that(interval.high==45,"lower boundry colating failed");
        Assert.that(interval.low==30,"lower boundry colating failed");
        //testing case 3 intervals is now {[30-45]}
        interval = new Interval(20,25);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==2,"lower non-overlap add failed");
        Assert.that(iSet.getSize()==22,"getSize() broken");
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==19,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==26,"getNeededInterval broken");
        Assert.that(interval.high==29,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==46,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
        //testing case 4 intervals is now {[20-25],[30-45]}
        interval = new Interval(50,60);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==3, "upper non-overlap add failed");
        Assert.that(iSet.getSize()==33,"getSize() broken");
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==19,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==26,"getNeededInterval broken");
        Assert.that(interval.high==29,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==46,"getNeededInterval broken");
        Assert.that(interval.high==49,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==61,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
        //////////////////getOverlapIntervals tests//////////
        //Note: Test all the cases for getOverlapIntervals, before continuing 
        //add test while the intervals are [20-25],[30-45],[50-60]
        //Case a
        interval = new Interval(23,32);
        List list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==2,"getOverlapIntervals broken");
        //Note: we dont know the order of the intervals in the list
        interval = (Interval)list.get(0);//first interval
        Assert.that(interval.low==23,"getOverlapIntervals broken");
        Assert.that(interval.high==25,"getOverlapIntervals broken");
        interval = (Interval)list.get(1);
        Assert.that(interval.low==30,"getOverlapIntervals broken");
        Assert.that(interval.high==32,"getOverlapIntervals broken");
        //Case a.1
        interval = new Interval(25,30);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==2,"getOverlapIntervals broken");
        //Note: we dont know the order of the intervals in the list
        interval = (Interval)list.get(0);//first interval
        Assert.that(interval.low==25,"getOverlapIntervals broken");
        Assert.that(interval.high==25,"getOverlapIntervals broken");
        interval = (Interval)list.get(1);
        Assert.that(interval.low==30,"getOverlapIntervals broken");
        Assert.that(interval.high==30,"getOverlapIntervals broken");
        //Case b
        interval = new Interval(16,23);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==1,"getOverlapIntervals broken");
        interval = (Interval)list.get(0);
        Assert.that(interval.low==20,"getOverlapIntervals broken");
        Assert.that(interval.high==23,"getOverlapIntervals broken");
        //case b.1
        interval = new Interval(16,20);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==1,"getOverlapIntervals broken");
        interval = (Interval)list.get(0);
        Assert.that(interval.low==20,"getOverlapIntervals broken");
        Assert.that(interval.high==20,"getOverlapIntervals broken");
        //case c
        interval = new Interval(23,29);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==1,"getOverlapIntervals broken");
        interval = (Interval)list.get(0);
        Assert.that(interval.low==23,"getOverlapIntervals broken");
        Assert.that(interval.high==25,"getOverlapIntervals broken");
        //case c.1
        interval = new Interval(25,29);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==1,"getOverlapIntervals broken");
        interval = (Interval)list.get(0);
        Assert.that(interval.low==25,"getOverlapIntervals broken");
        Assert.that(interval.high==25,"getOverlapIntervals broken");
        //case d
        interval = new Interval(13,19);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==0,"reported false overlap");
        //case e
        interval = new Interval(26,29);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==0,"reported false overlap");
        //case f
        interval = new Interval(23,53);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==3,"getOverlapIntervals broken");
        interval = (Interval)list.get(0);
        Assert.that(interval.low==23);//order not known, but deterministic
        Assert.that(interval.high==25);
        interval = (Interval)list.get(1);
        Assert.that(interval.low==30);//order not known, but deterministic
        Assert.that(interval.high==45);
        interval = (Interval)list.get(2);
        Assert.that(interval.low==50);//order not known, but deterministic
        Assert.that(interval.high==53);
        //case g
        interval = new Interval(16,65);
        list = iSet.getOverlapIntervals(interval);
        Assert.that(list.size()==3,"getOverlapIntervals broken");
        interval = (Interval)list.get(0);
        Assert.that(interval.low==20);//order not known, but deterministic
        Assert.that(interval.high==25);
        interval = (Interval)list.get(1);
        Assert.that(interval.low==30);//order not known, but deterministic
        Assert.that(interval.high==45);
        interval = (Interval)list.get(2);
        Assert.that(interval.low==50);//order not known, but deterministic
        Assert.that(interval.high==60);
        ///OK, back to testing add.
        //testing case 5 intervals is [20-25],[30-45],[50-60]
        interval = new Interval(54,70);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==3);
        interval = iSet.getIntervalAt(2);
        Assert.that(interval.low==50);
        Assert.that(interval.high==70);
        Assert.that(iSet.getSize()==43,"getSize() broken");
        //testing case 6 intervals is [20-25],[30-45],[50-70]
        interval = new Interval(71,75);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==3);
        interval = iSet.getIntervalAt(2);
        Assert.that(interval.low==50);
        Assert.that(interval.high==75);
        Assert.that(iSet.getSize()==48,"getSize() broken");
        //test some boundry conditions [20-25],[30-45],[50-75]
        interval = new Interval(75,80);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==3);
        interval = iSet.getIntervalAt(2);
        Assert.that(interval.low==50);
        Assert.that(interval.high==80);

        interval = new Interval(15,20);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==3);
        interval = iSet.getIntervalAt(2);//it was removed an readded
        Assert.that(interval.low==15);
        Assert.that(interval.high==25);
        Assert.that(iSet.getSize()==58,"getSize() broken");
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==14,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==26,"getNeededInterval broken");
        Assert.that(interval.high==29,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==46,"getNeededInterval broken");
        Assert.that(interval.high==49,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==81,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
        //[15-25],[30-45],[50-80]
        interval = new Interval(49,81);
        iSet.add(interval);
        Assert.that(iSet.numIntervals() ==3);
        interval = iSet.getIntervalAt(2);
        Assert.that(interval.low==49);
        Assert.that(interval.high==81);
        Assert.that(iSet.getSize()==60,"getSize() broken");
        // {[15-25],[30-45],[49-81]}
        interval = new Interval(55,60);
        iSet.add(interval);
        Assert.that(iSet.numIntervals() ==3);
        interval = iSet.getIntervalAt(2);
        Assert.that(interval.low==49);
        Assert.that(interval.high==81);
        // {[15-25],[30-45],[49-81]}
        interval = new Interval(26,29);
        iSet.add(interval);
        Assert.that(iSet.numIntervals() ==2);
        interval = iSet.getIntervalAt(1);
        Assert.that(interval.low==15);
        Assert.that(interval.high==45);
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==14,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==46,"getNeededInterval broken");
        Assert.that(interval.high==48,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==82,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
        // {[15-45],[49-81]}
        interval = new Interval(3,5);
        iSet.add(interval);
        interval = new Interval(7,9);
        iSet.add(interval);
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==2,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==6,"getNeededInterval broken");
        Assert.that(interval.high==6,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==10,"getNeededInterval broken");
        Assert.that(interval.high==14,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==46,"getNeededInterval broken");
        Assert.that(interval.high==48,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==82,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
        // {[3-5],[7-9],[15-45],[49-81]}
        Assert.that(iSet.numIntervals() ==4);
        interval = new Interval(2,17);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==2);
        interval = iSet.getIntervalAt(1);
        Assert.that(interval.low==2);
        Assert.that(interval.high==45);
        Assert.that(iSet.getSize()==77,"getSize() broken");
        //{[2-45],[49-81]}
        interval = new Interval(40,50);
        iSet.add(interval);
        Assert.that(iSet.numIntervals()==1);
        //{[2-81]}
        interval = iSet.getIntervalAt(0);
        Assert.that(interval.low==2);
        Assert.that(interval.high==81);
        Assert.that(iSet.getSize()==80,"getSize() broken");
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        Assert.that(interval.low==0,"getNeededInterval broken");
        Assert.that(interval.high==1,"getNeededInterval broken");
        interval = (Interval)iter.next();
        Assert.that(interval.low==82,"getNeededInterval broken");
        Assert.that(interval.high==99,"getNeededInterval broken");
    }
    ////////////method used only for tesing purposes////////////////
    public int numIntervals() {
        return intervals.size();
    }

    public Interval getIntervalAt(int i) {
        return (Interval)intervals.get(i);
    }
    
}
