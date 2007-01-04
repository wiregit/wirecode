package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.Set;

import junit.framework.Test;

public class RandomOrderHashSetTest extends BaseTestCase {
    public RandomOrderHashSetTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomOrderHashSetTest.class);
    }
    
    /**
     * tests that two subsequent iterations over the same set
     * will return the elements in different order.
     */
    public void testRandomOrder() throws Exception {
        Set s = new RandomOrderHashSet(50);
        for (int i = 0; i < 50; i++)
            s.add(new Integer(i));
        String out1 = "";
        String out2 = "";
        for (Iterator iter = s.iterator(); iter.hasNext();) 
            out1 = out1 + iter.next();
        for (Iterator iter = s.iterator(); iter.hasNext();) 
            out2 = out2 + iter.next();
        assertNotEquals(out1, out2);
    }
}
