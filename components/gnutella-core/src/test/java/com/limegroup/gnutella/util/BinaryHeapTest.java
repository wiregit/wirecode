package com.limegroup.gnutella.util;

import junit.framework.Test;


import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Unit tests for BinaryHeap
 */
public class BinaryHeapTest extends BaseTestCase {
            
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
        MyInteger one=new MyInteger(1);
        MyInteger two=new MyInteger(2);
        MyInteger three=new MyInteger(3);
        MyInteger four=new MyInteger(4);
        MyInteger five=new MyInteger(5);

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
        assertEquals(new MyInteger(2), q.insert(five));
        assertEquals(five, q.extractMax());
        assertEquals(five, q.extractMax());
        assertEquals(four, q.extractMax());
        assertEquals(three, q.extractMax());
        assertTrue(q.isEmpty());
    }
    
    public void testResize() throws Exception {
        MyInteger one=new MyInteger(1);
        MyInteger two=new MyInteger(2);
        MyInteger three=new MyInteger(3);
        MyInteger four=new MyInteger(4);
        MyInteger five=new MyInteger(5);
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
    
    private static Comparable[] getArray(BinaryHeap q) throws Exception {
        return (Comparable[])PrivilegedAccessor.getValue(
            q, "array");
    }

    //For testing with Java 1.1.8
    static class MyInteger implements Comparable
    {
        private int val;
        public MyInteger(int val)
        {
            this.val=val;
        }

        public int compareTo(Object other)
        {
            int val2=((MyInteger)other).val;
            if (val<val2)
                return -1;
            else if (val>val2)
                return 1;
            else
                return 0;
        }

        public String toString()
        {
            return String.valueOf(val);
        }
        
        public boolean equals(Object o) {
            return ((MyInteger)o).val == val;
        }
    }
}