package org.limewire.collection;

import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class RandomSequenceTest extends BaseTestCase {
    
    public RandomSequenceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomSequenceTest.class);
    }
    
    public static void testUnique() throws Exception {
        TreeSet<Integer> s = new TreeSet<Integer>();
        RandomSequence r = new RandomSequence(1000);
        for (int i : r)
            assertTrue(s.add(i));
        assertEquals(1000, s.size());
        assertEquals(new Integer(0),s.first());
        assertEquals(new Integer(999), s.last());
    }
    
    public static void testDifferent() throws Exception {
        /*
         * this test will fail 1/2.5 million runs.
         */
        String out1 = ""; 
        String out2 = "";

        // iterations over the same sequence are identical
        RandomSequence r = new RandomSequence(10000);
        for (int i : r)
            out1 = out1 +" "+ i;
        for (int i : r)
            out2 = out2 +" "+ i;
        assertEquals(out1, out2);

        // but iterations over different sequences are not
        r = new RandomSequence(10000);
        out2 = "";
        for (int i : r)
            out2 = out2 +" "+ i;
        assertNotEquals(out1, out2);
    }
}
