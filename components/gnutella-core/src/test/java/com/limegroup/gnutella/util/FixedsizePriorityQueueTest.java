package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;

public class FixedsizePriorityQueueTest extends TestCase {
    private final Integer one=new Integer(1);
    private final Integer two=new Integer(2);
    private final Integer three=new Integer(3);
    private final Integer four=new Integer(4);
    private final Integer five=new Integer(5);
    private final Integer six=new Integer(6);

    public FixedsizePriorityQueueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FixedsizePriorityQueueTest.class);
    }    
    
    public void testAll() {
        FixedsizePriorityQueue q=new FixedsizePriorityQueue(
            4, ArrayListUtil.integerComparator());
        try {
            assertEquals(six, q.getMax());
            fail("No no such element exception");
        } catch (NoSuchElementException e) { }

        assertNull(q.insert(two));
        assertNull(q.insert(three));
        assertEquals(2, q.size());
        assertEquals(4, q.capacity());
        assertNull(q.insert(one));
        assertNull(q.insert(four));
                
        assertEquals(one, q.insert(five));
        assertEquals(two, q.insert(six));
        assertNull(q.insert(five));   //already there
        assertEquals(4, q.size());
        assertEquals(4, q.capacity());

        assertEquals(three, q.getMin());
        assertEquals(six, q.getMax());

        Iterator iter=q.iterator();
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertEquals(five, iter.next());
        assertEquals(six, iter.next());
        assertTrue(! iter.hasNext());
    }

    /*
    public void testPerformance() {
        int SIZE=500;
        MyInteger[] numbers=new MyInteger[100000];
        java.util.Random rand=new java.util.Random();
        for (int i=0; i<numbers.length; i++) 
            numbers[i]=new MyInteger(rand.nextInt());

        BinaryHeap q1=new BinaryHeap(SIZE);
        long start=System.currentTimeMillis();
        for (int i=0; i<q1.size(); i++) 
            q1.insert(numbers[i]);
        long stop=System.currentTimeMillis();
        System.out.println("Elapsed time for BinaryHeap: "
                           +(stop-start));
        q1=null;

        FixedsizePriorityQueue q2=new FixedsizePriorityQueue(SIZE);
        start=System.currentTimeMillis();
        for (int i=0; i<numbers.length; i++) 
            q2.insert(numbers[i]);
        stop=System.currentTimeMillis();
        System.out.println("Elapsed time for FixedSizePriorityQueue: "
                           +(stop-start));
    }

    class MyInteger implements com.sun.java.util.collections.Comparable {
        private int n;

        public MyInteger(int n) {
            this.n=n;
        }

        public int compareTo(Object o) {
            return n-((MyInteger)o).n;
        }
    }
    */
}
