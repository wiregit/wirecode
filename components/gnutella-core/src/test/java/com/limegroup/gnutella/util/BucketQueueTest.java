package com.limegroup.gnutella.util;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.Endpoint;

/**
 * Unit tests for BucketQueue
 */
public class BucketQueueTest extends BaseTestCase {
            
	public BucketQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(BucketQueueTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() {
        Endpoint e4=new Endpoint("garbage", 0); e4.setWeight(4);
        Endpoint e2a=new Endpoint("garbage", 0); e2a.setWeight(2);
        Endpoint e2b=new Endpoint("garbage", 0); e2b.setWeight(2);
        Endpoint e0=new Endpoint("garbage", 0); e0.setWeight(0);
        BucketQueue q=new BucketQueue(5, 10);
        assertTrue(q.isEmpty());

        assertNull(q.insert(e0, 0));
        assertNull(q.insert(e4, 4));
        assertNull(q.insert(e2b, 2));
        assertNull(q.insert(e2a, 2));
        assertEquals(4, q.size());
        assertEquals(1, q.size(4));
        assertEquals(2, q.size(2));
        assertEquals(0, q.size(3));
        assertTrue(! q.isEmpty());

        Iterator iter=q.iterator();
        assertSame(e4, iter.next());
        assertSame(e2a, iter.next());
        assertSame(e2b, iter.next());
        assertSame(e0, iter.next());
        try {
            iter.next();
            fail("no such element expected");
        } catch (NoSuchElementException e) { }

        //Make sure hasNext is idempotent
        iter=q.iterator(4, 100);
        assertTrue(iter.hasNext());
        assertSame(e4, iter.next());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertSame(e2a, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e2b, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e0, iter.next());
        assertTrue(! iter.hasNext());
        try {
            iter.next();
            fail("nse expected");
        } catch (NoSuchElementException e) { }

        iter=q.iterator(4, 2);
        assertSame(e4, iter.next());
        assertSame(e2a, iter.next());
        assertTrue(! iter.hasNext());        

        iter=q.iterator(2, 3);     //sorting by priority
        assertTrue(iter.hasNext());
        assertSame(e2a, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e2b, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e0, iter.next());
        assertTrue(! iter.hasNext());

        iter=q.iterator(1, 3);     //sorting by priority
        assertTrue(iter.hasNext());
        assertSame(e0, iter.next());
        assertTrue(! iter.hasNext());

        assertSame(e4, q.getMax());
        assertSame(e4, q.extractMax());
        assertSame(e2a, q.extractMax());
        assertSame(e2b, q.extractMax());
        assertSame(e0, q.getMax());
        assertSame(e0, q.extractMax());
        try {
            q.getMax();
            fail("nse expected");
        } catch (NoSuchElementException e) { }            

        Endpoint f1=new Endpoint("garbage2", 6346);
        assertTrue(!q.removeAll(f1));
        assertNull(q.insert(e0, 0));
        assertNull(q.insert(e4, 4));
        assertNull(q.insert(f1, 1));
        assertNull(q.insert(e2b, 2));
        assertNull(q.insert(e2a, 2));
        assertTrue(q.removeAll(e0));
        assertEquals(1, q.size());
        assertSame(f1, q.getMax());
        assertTrue(q.removeAll(f1));
        assertTrue(q.isEmpty());        

        //Test clone
        q=new BucketQueue(new int[] {10, 10, 10, 10, 10});
        q.insert(e4, 4);
        q.insert(e2a, 2);
        q.insert(e0, 0);

        BucketQueue q2=new BucketQueue(q);
        assertEquals(q.size(), q2.size());
        Iterator iter1=q.iterator();
        Iterator iter2=q.iterator();
        while (iter1.hasNext()) {
            assertEquals(iter1.next(), iter2.next());
        }
        assertTrue(! iter2.hasNext());

        q.insert(e2b, 2);
        assertEquals((q2.size()+1), q.size());

        //More rigorous test of insertion.  Also checks objects besides
        //Endpoint.        
        q=new BucketQueue(3, 2);
        assertNull(q.insert("oldest medium", 1));
        assertNull(q.insert("older medium", 1));
        assertEquals("oldest medium", q.insert("medium", 1));
        assertNull(q.insert("low", 0));
        assertNull(q.insert("high", 2));
        assertEquals("high", q.extractMax());
        assertEquals("medium", q.extractMax());
        assertEquals("older medium", q.extractMax());
        assertEquals("low", q.extractMax());

        //Test exceptional cases
        try {
            q=new BucketQueue(new int[0]);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q=new BucketQueue(new int[] {1, 2, 0});
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q=new BucketQueue(new int[] {1});
        } catch (IllegalArgumentException e) { 
            fail("illegal argument expected");
        }
        try {
            q=new BucketQueue(0, 1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { 
        }
        try {
            q=new BucketQueue(1, 0);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { 
        }

        q=new BucketQueue(new int[] {10, 10, 10});
        try {
            q.insert("oops", -1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q.insert("oops", 3);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }

        try {
            q.size(-1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q.size(3);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }


        try {
            iter=q.iterator(-1, 1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            iter=q.iterator(3, 1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
    }
}