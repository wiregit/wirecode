package com.limegroup.gnutella.downloader;

import junit.framework.*;

public class IntervalTest extends TestCase {

    public IntervalTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(IntervalTest.class);
    }

	public void testLegacy() {
        Interval a=new Interval(0,3);
        Interval b=new Interval(3,5);
        Interval c=new Interval(1,4);
        Interval d=new Interval(6,10);
        Interval e=new Interval(0,10);

        assertTrue(a.overlaps(a));
        assertTrue(! a.adjacent(a));

        assertTrue(! a.overlaps(b));
        assertTrue(! b.overlaps(a));
        assertTrue(a.adjacent(b));
        assertTrue(b.adjacent(a));

        assertTrue(a.overlaps(c));
        assertTrue(c.overlaps(a));
        assertTrue(! a.adjacent(c));
        assertTrue(! c.adjacent(a));
        
        assertTrue(! a.overlaps(d));
        assertTrue(! d.overlaps(a));
        assertTrue(! a.adjacent(d));
        assertTrue(! d.adjacent(a));

        assertTrue(e.overlaps(c));
        assertTrue(c.overlaps(a));
    }
}
