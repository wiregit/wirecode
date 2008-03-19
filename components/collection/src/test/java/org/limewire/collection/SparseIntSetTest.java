package org.limewire.collection;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class SparseIntSetTest extends BaseTestCase {

    public SparseIntSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SparseIntSetTest.class);
    }
    
    public void testAdd() throws Exception {
        Set<Integer> sparseSet = new SparseIntSet();
        assertTrue(sparseSet.add(5));
        assertFalse(sparseSet.add(5));
        assertTrue(sparseSet.contains(5));
        assertEquals(1,sparseSet.size());
    }
    
    public void testSortedProperty() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        Set<Integer> sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 1000; i++) {
            int random = (int)(Math.random() * 1000);
            treeSet.add(random);
            sparseSet.add(random);
        }
        
        assertEquals(treeSet.size(),sparseSet.size());
        assertTrue(treeSet.containsAll(sparseSet));
        assertTrue(sparseSet.containsAll(treeSet));
        
        Iterator<Integer> treeIterator = treeSet.iterator();
        Iterator<Integer> sparseIterator = sparseSet.iterator();
        
        for (int i = 0; i < treeSet.size(); i++)
            assertEquals(treeIterator.next(),sparseIterator.next());
    }

    public void testRemoveAll() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        Set<Integer> sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        
        for (int i = 900; i < 1000; i++)
            treeSet.add(i);
        
        assertTrue(sparseSet.removeAll(treeSet));
        assertFalse(sparseSet.removeAll(treeSet));
        
        for (int i = 0; i < 900; i++)
            assertTrue(sparseSet.contains(i));
        for (int i = 900; i < 1000; i++)
            assertFalse(sparseSet.contains(i));
    }
    
    public void testRetainAll() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        Set<Integer> sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        
        for (int i = 800; i < 900; i++)
            treeSet.add(i);
        
        assertTrue(sparseSet.retainAll(treeSet));
        assertFalse(sparseSet.retainAll(treeSet));
        
        for (int i = 0; i < 800; i++)
            assertFalse(sparseSet.contains(i));
        for (int i = 800; i < 900; i++)
            assertTrue(sparseSet.contains(i));
        for (int i = 900; i < 1000; i++)
            assertFalse(sparseSet.contains(i));
    }
    
    public void testCompact() throws Exception {
        SparseIntSet sparseSet = new SparseIntSet();
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        
        assertGreaterThan(4000, sparseSet.getActualMemoryUsed());
        sparseSet.compact();
        assertEquals(4000, sparseSet.getActualMemoryUsed());
    }
}
