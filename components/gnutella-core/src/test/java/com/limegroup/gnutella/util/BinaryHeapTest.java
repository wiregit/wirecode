package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.limewire.util.PrivilegedAccessor;


import junit.framework.Test;

/**
 * Unit tests for BinaryHeap
 */
public class BinaryHeapTest extends LimeTestCase {
    
    private List FINALIZED = new ArrayList();
            
	public BinaryHeapTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(BinaryHeapTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
	    BinaryHeap q=new BinaryHeap(4);
        Integer one=new Integer(1);
        Integer two=new Integer(2);
        Integer three=new Integer(3);
        Integer four=new Integer(4);
        Integer five=new Integer(5);

        assertTrue(q.isEmpty());
        assertEquals(4, q.capacity());
        assertEquals(0, q.size());

        q.insert(two);
        assertEquals(1, q.size());
        q.insert(three);
        q.insert(four);
        q.insert(one);

        assertTrue(q.isFull());
        assertEquals(4, q.size());

        assertEquals(four, q.getMax());
        assertEquals(four, q.extractMax());
        assertEquals(three, q.getMax());
        assertEquals(three, q.extractMax());
        q.insert(two);
        assertEquals(two, q.extractMax());
        assertEquals(two, q.extractMax());
        assertEquals(one, q.extractMax());
        
        try {
            q.extractMax();
            fail("no such element expected");
        } catch (NoSuchElementException e) { }

        //Iterator
        q=new BinaryHeap(2);
        assertTrue(! q.iterator().hasNext());
        q.insert(one);
        q.insert(two);
        Iterator iter=q.iterator();
        assertTrue(iter.hasNext());
        //first element is max
        assertEquals(two, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(one, iter.next());
        assertTrue(! iter.hasNext());
        try {
            iter.next();
            fail("no such element expected");
        } catch (NoSuchElementException e) {
        }        

        //try inserting when overfilled
        q=new BinaryHeap(4);
        assertNull(q.insert(one));
        assertNull(q.insert(four));
        assertNull(q.insert(three));
        assertTrue(! q.isFull());
        assertNull(q.insert(two));
        assertTrue(q.isFull());
//        System.out.println("The following tests are STRONGER than required"
//                           +" the specification of insert.");
//        System.out.println("(The spec does not say that the smallest"
//                           +" element is removed on overflow.)");
        assertNotNull(q.insert(five));
        assertEquals(new Integer(2), q.insert(five));
        assertEquals(five, q.extractMax());
        assertEquals(five, q.extractMax());
        assertEquals(four, q.extractMax());
        assertEquals(three, q.extractMax());
        assertTrue(q.isEmpty());
    }
    
    public void testResize() throws Exception {
        Integer one=new Integer(1);
        Integer two=new Integer(2);
        Integer three=new Integer(3);
        Integer four=new Integer(4);
        Integer five=new Integer(5);
        BinaryHeap q=new BinaryHeap(2, true);

        assertNull(q.insert(one));
        assertTrue(! q.isFull());
        assertNull(q.insert(four));
        assertEquals(2, q.capacity());
        assertTrue(q.isFull());

        assertNull(q.insert(three));
        assertEquals(4, q.capacity());
        assertTrue(! q.isFull());
        assertEquals(3, q.size());
        assertEquals(5, getArray(q).length);
        assertNull(getArray(q)[0]);

        assertNull(q.insert(two));
        assertEquals(4, q.capacity());
        assertTrue(q.isFull());
        assertEquals(4, q.size());

        assertNull(q.insert(five));
        assertEquals(8, q.capacity());
        assertTrue(! q.isFull());
        assertEquals(5, q.size());

        assertEquals(five, q.extractMax());
        assertEquals(four, q.extractMax());
        assertEquals(three, q.extractMax());
        assertEquals(two, q.extractMax());
        assertEquals(one, q.extractMax());
        assertTrue(q.isEmpty());
        assertEquals(8, q.capacity());
    }
    
    public void testErasesReferences() throws Exception {
        BinaryHeap heap = new BinaryHeap(50);
        heap.insert(new Finalizable(1));
        heap.extractMax();
        System.gc();
        Thread.sleep(2000);
        assertEquals(1, FINALIZED.size());
        assertEquals(new Integer(1), FINALIZED.get(0));
    }
    
    public void testSameValuesInserted() throws Exception {
        BinaryHeap heap = new BinaryHeap(5, true);
        Integer oneA = new Integer(1);
        Integer oneB = new Integer(1);
        Integer twoA = new Integer(2);
        Integer twoB = new Integer(2);
        Integer three = new Integer(3);
        heap.insert(oneA);
        heap.insert(twoA);
        assertEquals(2, heap.size());
        heap.insert(oneB);
        assertEquals(3, heap.size());
        heap.insert(twoB);
        assertEquals(4, heap.size());
        heap.insert(three);
        assertEquals(5, heap.size());
        assertSame(three, heap.extractMax());
        assertSame(twoB, heap.extractMax());
        assertSame(twoA, heap.extractMax());
        assertSame(oneB, heap.extractMax());
        assertSame(oneA, heap.extractMax());
        
    }
    
    private static Comparable[] getArray(BinaryHeap q) throws Exception {
        return (Comparable[])PrivilegedAccessor.getValue(
            q, "array");
    }
    
    private class Finalizable implements Comparable {
        private int id;
        Finalizable(int id) {
            this.id = id;
        }
        
        public int compareTo(Object other) {
            return new Integer(id).compareTo(new Integer(((Finalizable)other).id));
        }

        protected void finalize() throws Throwable {
            super.finalize();
            FINALIZED.add(new Integer(id));
        }
    }
}