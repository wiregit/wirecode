package org.limewire.collection;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class FixedSizeArrayHashMapTest extends BaseTestCase {

    public FixedSizeArrayHashMapTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FixedSizeArrayHashMapTest.class);
    }
    
    /**
     * tests that adding a new element ejects the oldest one.
     */
    public void testFixedSize() throws Exception {
        Map m = new FixedSizeArrayHashMap(2);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        assertEquals(2, m.size());
        String out = "";
        for (Object o : m.entrySet())
            out += o + ", ";
        assertEquals("3=33, 2=22, ",out);
        assertEquals("{3=33, 2=22}",m.toString());
        assertTrue(m.containsKey(2));
        assertTrue(m.containsKey(3));
        assertFalse(m.containsKey(1));
        assertTrue(m.containsValue(22));
        assertTrue(m.containsValue(33));
        assertFalse(m.containsValue(11));
        
        out = "";
        for(Object o : m.keySet())
            out += o + ", ";
        assertEquals("3, 2, ", out);
        
        out = "";
        for(Object o : m.values())
            out += o + ", ";
        assertEquals("33, 22, ", out);
    }

    /**
     * tests that re-adding an existing element will postpone
     * its ejection turn.
     */
    public void testRefresh() throws Exception {
        Map m = new FixedSizeArrayHashMap(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33); m.put(1, 11);
        assertEquals(3, m.size());
        String out = "";
        for (Object o : m.entrySet())
            out += o + ", ";
        assertEquals("1=11, 3=33, 2=22, ", out);
        
        m.put(4, 44);
        assertFalse(m.containsKey(2));
        assertTrue(m.containsKey(1));
        out = "";
        for (Object o : m.entrySet())
            out += o + ", ";
        assertEquals("4=44, 1=11, 3=33, ", out);
    }
    
    public void testRemove() throws Exception {
        Map m = new FixedSizeArrayHashMap(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        assertTrue(m.containsKey(2));
        m.remove(2);
        assertFalse(m.containsKey(2));
        assertEquals(2, m.size());
        m.remove(4);
        assertEquals(2, m.size());
    }

    /**
     * tests that creating from a collection that's larger than
     * the capacity will happen in fifo order.
     */
    public void testCreateFromCollection() throws Exception {
        Map m1 = new LinkedHashMap();
        m1.put(1, 11); m1.put(2, 22); m1.put(3, 33);
        Map m = new FixedSizeArrayHashMap(2, m1);
        assertEquals(2, m.size());
        assertTrue(m.containsKey(2));
        assertTrue(m.containsKey(3));
        assertFalse(m.containsKey(1));
    }
    
    public void testIndexing() throws Exception {
        FixedSizeArrayHashMap m = new FixedSizeArrayHashMap(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        assertEquals(3, m.getKeyAt(0));
        assertEquals(2, m.getKeyAt(1));
        assertEquals(1, m.getKeyAt(2));
        m.put(4, 44);
        assertEquals(4, m.getKeyAt(0));
        assertEquals(3, m.getKeyAt(1));
        assertEquals(2, m.getKeyAt(2));
    }
    
    public void testClone() throws Exception {
        FixedSizeArrayHashMap m = new FixedSizeArrayHashMap(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        Map m2 = (Map) m.clone();
        assertEquals(m.size(), m2.size());
        assertTrue(m2.keySet().containsAll(m.keySet()));
    }
}
