package org.limewire.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class RandomOrderHashMapTest extends BaseTestCase {
    public RandomOrderHashMapTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomOrderHashMapTest.class);
    }
    
    /**
     * tests that two subsequent iterations over the same map
     * will return the elements in different order.
     */
    public void testRandomOrder() throws Exception {
        Map m = new RandomOrderHashMap(50);
        for (int i = 0; i < 50; i++)
            m.put(i, i + "x" + i);
        String out1 = "";
        String out2 = "";
        Set s = m.entrySet();
        for (Object o : s)
            out1 = out1 + o;
        for (Object o : s)
            out2 = out2 + o;
        assertNotEquals(out1, out2);
        
        out1 = "";
        out2 = "";
        s = m.keySet();
        for (Object o : s)
            out1 = out1 + o;
        for (Object o : s)
            out2 = out2 + o;
        assertNotEquals(out1, out2);
        
        out1 = "";
        out2 = "";
        Collection c = m.values();
        for (Object o : c)
            out1 = out1 + o;
        for (Object o : c)
            out2 = out2 + o;
        assertNotEquals(out1, out2);
    }
}
