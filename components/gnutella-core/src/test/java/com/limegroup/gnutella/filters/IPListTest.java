package com.limegroup.gnutella.filters;

import junit.framework.Test;
import junit.framework.TestSuite;

public class IPListTest extends com.limegroup.gnutella.util.BaseTestCase {
    public IPListTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IPListTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testContains() {
        IPList list = new IPList();
        
        list.add("192.168.1.1");
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertFalse(list.contains(new IP("192.168.1.2")));
        assertEquals(1, list.size());
        
        list.add("192.168.2.0/255.255.255.0");
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertTrue(list.contains(new IP("192.168.2.2")));
        assertFalse(list.contains(new IP("192.168.1.2")));
        assertEquals(2, list.size());
        
        list.add("192.168.2.1");
        assertEquals(2, list.size());
        list.add("192.168.2.3");
        assertEquals(2, list.size());
        list.add("192.168.1.255");
        assertEquals(3, list.size());
        list.add("192.168.1.254");
        assertEquals(4, list.size());
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertTrue(list.contains(new IP("192.168.2.2")));
        assertFalse(list.contains(new IP("192.168.1.2")));
        
        
        
        list.add("100.0.0.0/255.0.0.0");
        assertEquals(5, list.size());
        list.add("100.1.2.3");
        assertEquals(5, list.size());
        list.add("100.5.4.3");
        assertEquals(5, list.size());
        assertTrue(list.contains(new IP("100.5.3.2")));
        assertTrue(list.contains(new IP("100.2.3.4")));
        
        assertFalse(list.contains(new IP("99.2.1.3")));
        
        list.add("98.1.2.3");
        assertEquals(6, list.size());
        list.add("98.5.4.3");
        assertEquals(7, list.size());
        list.add("98.0.0.0/255.0.0.0");
        assertEquals(6, list.size());
        assertTrue(list.contains(new IP("98.5.3.2")));
        assertTrue(list.contains(new IP("98.2.3.4")));
        
        assertFalse(list.contains(new IP("99.2.1.3")));
        
        list.add("69.41.173.34");
        assertEquals(7, list.size());
        list.add("69.*.*.*");
        assertEquals(7, list.size());
        
        list.add("*.*.*.*");
        assertEquals(1, list.size());
        assertTrue(list.contains(new IP("98.5.3.2")));
        assertTrue(list.contains(new IP("98.2.3.4")));
        assertTrue(list.contains(new IP("99.2.1.3")));
        assertTrue(list.contains(new IP("100.5.3.2")));
        assertTrue(list.contains(new IP("100.2.3.4")));
        assertTrue(list.contains(new IP("99.2.1.3")));
        assertTrue(list.contains(new IP("192.168.1.1")));
        assertTrue(list.contains(new IP("192.168.2.2")));
        assertTrue(list.contains(new IP("192.168.1.2")));
    }
}