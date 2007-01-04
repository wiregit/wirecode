package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.TreeSet;

import junit.framework.Test;

public class RandomSequenceTest extends BaseTestCase {
    
    public RandomSequenceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomSequenceTest.class);
    }
    
    public static void testUnique() throws Exception {
        TreeSet s = new TreeSet();
        RandomSequence r = new RandomSequence(1000);
        for (Iterator iter = r.iterator(); iter.hasNext();)
            assertTrue(s.add(iter.next()));
        assertEquals(1000, s.size());
        assertEquals(new Integer(0),s.first());
        assertEquals(new Integer(999), s.last());
    }
    
    public static void testDifferent() throws Exception {
        String out1 = "";
        String out2 = "";
        
        // iterations over the same sequence are identical
        RandomSequence r = new RandomSequence(10);
        for (Iterator iter = r.iterator(); iter.hasNext();)
            out1 = out1 + iter.next();
        for (Iterator iter = r.iterator(); iter.hasNext();)
            out2 = out2 + iter.next();
        assertEquals(out1, out2);
        
        // but iterations over different sequences are not
        r = new RandomSequence(10);
        out2 = "";
        for (Iterator iter = r.iterator(); iter.hasNext();)
            out2 = out2 + iter.next();
        assertNotEquals(out1, out2);
    }
}
