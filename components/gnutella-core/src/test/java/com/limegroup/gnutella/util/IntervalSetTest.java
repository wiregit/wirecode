package com.limegroup.gnutella.util;

import junit.framework.Test;

import com.limegroup.gnutella.downloader.Interval;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

/**
 * Unit tests for IntervalSet
 */
public class IntervalSetTest extends BaseTestCase {
    
    private IntervalSet iSet = null;
            
	public IntervalSetTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(IntervalSetTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
        iSet = new IntervalSet();
        Interval interval = new Interval(40,45);
        iSet.add(interval);
        assertEquals("add method broken", 1, numIntervals());
        assertEquals("getSize() broken", 6, iSet.getSize());
        Iterator iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 39, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 46, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        ///testing case 1 (intervals is now {[40-45]}
        interval = new Interval(35,39);
        iSet.add(interval);
        assertEquals("lower boundry colating failed", 1, numIntervals());
        interval = getIntervalAt(0);
        assertEquals("lower boundry colating failed", 45, interval.high);
        assertEquals("lower boundry colating failed", 35, interval.low);
        assertEquals("getSize() broken", 11, iSet.getSize());
        //testing case 2 intervals is now {[35-45]}
        interval = new Interval(30,37);
        iSet.add(interval);
        assertEquals("lower overlap colating failed", 1, numIntervals());
        interval = getIntervalAt(0);
        assertEquals("lower boundry colating failed", 45, interval.high);
        assertEquals("lower boundry colating failed", 30, interval.low);
        //testing case 3 intervals is now {[30-45]}
        interval = new Interval(20,25);
        iSet.add(interval);
        assertEquals("lower non-overlap add failed", 2, numIntervals());
        assertEquals("getSize() broken", 22, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 19, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 26, interval.low);
        assertEquals("getNeededInterval broken", 29, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 46, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        //testing case 4 intervals is now {[20-25],[30-45]}
        interval = new Interval(50,60);
        iSet.add(interval);
        assertEquals("upper non-overlap add failed", 3, numIntervals());
        assertEquals("getSize() broken", 33, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 19, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 26, interval.low);
        assertEquals("getNeededInterval broken", 29, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 46, interval.low);
        assertEquals("getNeededInterval broken", 49, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 61, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        //////////////////getOverlapIntervals tests//////////
        //Note: Test all the cases for getOverlapIntervals, before continuing 
        //add test while the intervals are [20-25],[30-45],[50-60]
        //Case a
        interval = new Interval(23,32);
        List list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 2, list.size());
        //Note: we dont know the order of the intervals in the list
        interval = (Interval)list.get(0);//first interval
        assertEquals("getOverlapIntervals broken", 23, interval.low);
        assertEquals("getOverlapIntervals broken", 25, interval.high);
        interval = (Interval)list.get(1);
        assertEquals("getOverlapIntervals broken", 30, interval.low);
        assertEquals("getOverlapIntervals broken", 32, interval.high);
        //Case a.1
        interval = new Interval(25,30);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 2, list.size());
        //Note: we dont know the order of the intervals in the list
        interval = (Interval)list.get(0);//first interval
        assertEquals("getOverlapIntervals broken", 25, interval.low);
        assertEquals("getOverlapIntervals broken", 25, interval.high);
        interval = (Interval)list.get(1);
        assertEquals("getOverlapIntervals broken", 30, interval.low);
        assertEquals("getOverlapIntervals broken", 30, interval.high);
        //Case b
        interval = new Interval(16,23);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval)list.get(0);
        assertEquals("getOverlapIntervals broken", 20, interval.low);
        assertEquals("getOverlapIntervals broken", 23, interval.high);
        //case b.1
        interval = new Interval(16,20);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval)list.get(0);
        assertEquals("getOverlapIntervals broken", 20, interval.low);
        assertEquals("getOverlapIntervals broken", 20, interval.high);
        //case c
        interval = new Interval(23,29);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval)list.get(0);
        assertEquals("getOverlapIntervals broken", 23, interval.low);
        assertEquals("getOverlapIntervals broken", 25, interval.high);
        //case c.1
        interval = new Interval(25,29);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 1, list.size());
        interval = (Interval)list.get(0);
        assertEquals("getOverlapIntervals broken", 25, interval.low);
        assertEquals("getOverlapIntervals broken", 25, interval.high);
        //case d
        interval = new Interval(13,19);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("reported false overlap", 0, list.size());
        //case e
        interval = new Interval(26,29);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("reported false overlap", 0, list.size());
        //case f
        interval = new Interval(23,53);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 3, list.size());
        interval = (Interval)list.get(0);
        assertEquals(23, interval.low);//order not known, but deterministic
        assertEquals(25, interval.high);
        interval = (Interval)list.get(1);
        assertEquals(30, interval.low);//order not known, but deterministic
        assertEquals(45, interval.high);
        interval = (Interval)list.get(2);
        assertEquals(50, interval.low);//order not known, but deterministic
        assertEquals(53, interval.high);
        //case g
        interval = new Interval(16,65);
        list = iSet.getOverlapIntervals(interval);
        assertEquals("getOverlapIntervals broken", 3, list.size());
        interval = (Interval)list.get(0);
        assertEquals(20, interval.low);//order not known, but deterministic
        assertEquals(25, interval.high);
        interval = (Interval)list.get(1);
        assertEquals(30, interval.low);//order not known, but deterministic
        assertEquals(45, interval.high);
        interval = (Interval)list.get(2);
        assertEquals(50, interval.low);//order not known, but deterministic
        assertEquals(60, interval.high);
        ///OK, back to testing add.
        //testing case 5 intervals is [20-25],[30-45],[50-60]
        interval = new Interval(54,70);
        iSet.add(interval);
        assertEquals(3, numIntervals());
        interval = getIntervalAt(2);
        assertEquals(50, interval.low);
        assertEquals(70, interval.high);
        assertEquals("getSize() broken", 43, iSet.getSize());
        //testing case 6 intervals is [20-25],[30-45],[50-70]
        interval = new Interval(71,75);
        iSet.add(interval);
        assertEquals(3, numIntervals());
        interval = getIntervalAt(2);
        assertEquals(50, interval.low);
        assertEquals(75, interval.high);
        assertEquals("getSize() broken", 48, iSet.getSize());
        //test some boundry conditions [20-25],[30-45],[50-75]
        interval = new Interval(75,80);
        iSet.add(interval);
        assertEquals(3, numIntervals());
        interval = getIntervalAt(2);
        assertEquals(50, interval.low);
        assertEquals(80, interval.high);

        interval = new Interval(15,20);
        iSet.add(interval);
        assertEquals(3, numIntervals());
        interval = getIntervalAt(2);//it was removed an readded
        assertEquals(15, interval.low);
        assertEquals(25, interval.high);
        assertEquals("getSize() broken", 58, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 14, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 26, interval.low);
        assertEquals("getNeededInterval broken", 29, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 46, interval.low);
        assertEquals("getNeededInterval broken", 49, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 81, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        //[15-25],[30-45],[50-80]
        interval = new Interval(49,81);
        iSet.add(interval);
        assertEquals(3, numIntervals());
        interval = getIntervalAt(2);
        assertEquals(49, interval.low);
        assertEquals(81, interval.high);
        assertEquals("getSize() broken", 60, iSet.getSize());
        // {[15-25],[30-45],[49-81]}
        interval = new Interval(55,60);
        iSet.add(interval);
        assertEquals(3, numIntervals());
        interval = getIntervalAt(2);
        assertEquals(49, interval.low);
        assertEquals(81, interval.high);
        // {[15-25],[30-45],[49-81]}
        interval = new Interval(26,29);
        iSet.add(interval);
        assertEquals(2, numIntervals());
        interval = getIntervalAt(1);
        assertEquals(15, interval.low);
        assertEquals(45, interval.high);
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 14, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 46, interval.low);
        assertEquals("getNeededInterval broken", 48, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 82, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        // {[15-45],[49-81]}
        interval = new Interval(3,5);
        iSet.add(interval);
        interval = new Interval(7,9);
        iSet.add(interval);
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 2, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 6, interval.low);
        assertEquals("getNeededInterval broken", 6, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 10, interval.low);
        assertEquals("getNeededInterval broken", 14, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 46, interval.low);
        assertEquals("getNeededInterval broken", 48, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 82, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        // {[3-5],[7-9],[15-45],[49-81]}
        assertEquals(4, numIntervals());
        interval = new Interval(2,17);
        iSet.add(interval);
        assertEquals(2, numIntervals());
        interval = getIntervalAt(1);
        assertEquals(2, interval.low);
        assertEquals(45, interval.high);
        assertEquals("getSize() broken", 77, iSet.getSize());
        //{[2-45],[49-81]}
        interval = new Interval(40,50);
        iSet.add(interval);
        assertEquals(1, numIntervals());
        //{[2-81]}
        interval = getIntervalAt(0);
        assertEquals(2, interval.low);
        assertEquals(81, interval.high);
        assertEquals("getSize() broken", 80, iSet.getSize());
        iter = iSet.getNeededIntervals(100);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 0, interval.low);
        assertEquals("getNeededInterval broken", 1, interval.high);
        interval = (Interval)iter.next();
        assertEquals("getNeededInterval broken", 82, interval.low);
        assertEquals("getNeededInterval broken", 99, interval.high);
        iSet.clear();
        iSet.add(new Interval(0,5));
        iter = iSet.getNeededIntervals(6);
        assertTrue(!iter.hasNext());
        iSet.add(new Interval(6,10));
        iter = iSet.getNeededIntervals(20);
        interval = (Interval)iter.next();
        assertEquals(11, interval.low);
        assertEquals(19, interval.high);
        assertTrue(!iter.hasNext());
        // test delete() method
        iSet.clear();
        iSet.add(new Interval(0,4));
        iSet.add(new Interval(8,12));
        iSet.add(new Interval(16,20));
        iSet.add(new Interval(24));
        iSet.add(new Interval(28,32));
        iSet.add(new Interval(36,40));
        iSet.delete(new Interval(5,7));
        iSet.delete(new Interval(12,16));
        iSet.delete(new Interval(24,25));
        iSet.delete(new Interval(29,30));
        iSet.delete(new Interval(35,41));
        iter = iSet.getAllIntervals();
        interval = getIntervalAt(0);
        assertEquals("delete broken", interval.low, 0);
        assertEquals("delete broken", interval.high, 4);
        interval = getIntervalAt(1);
        assertEquals("delete broken", interval.low, 8);
        assertEquals("delete broken", interval.high, 11);
        interval = getIntervalAt(2);
        assertEquals("delete broken", interval.low, 17);
        assertEquals("delete broken", interval.high, 20);
        interval = getIntervalAt(3);
        assertEquals("delete broken", interval.low, 28);
        assertEquals("delete broken", interval.high, 28);
        interval = getIntervalAt(4);
        assertEquals("delete broken", interval.low, 31);
        assertEquals("delete broken", interval.high, 32);
        assertEquals("delete broken", numIntervals(), 5);
    }


    private int numIntervals() {
        return iSet.getAllIntervalsAsList().size();
    }

    private Interval getIntervalAt(int i) {
        return (Interval)iSet.getAllIntervalsAsList().get(i);
    }
    
    private List getIntervals() throws Exception {
        return (List)PrivilegedAccessor.getValue(iSet, "intervals");
    }
    
    
}