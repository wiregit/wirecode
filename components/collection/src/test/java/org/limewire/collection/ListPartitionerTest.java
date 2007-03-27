package org.limewire.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class ListPartitionerTest extends BaseTestCase {

    public ListPartitionerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ListPartitionerTest.class);
    }
    
    public void testEmpty() throws Exception {
        ListPartitioner l = new ListPartitioner(new ArrayList(), 10);
        for (int i = 0; i < 10; i++)
            assertTrue(l.getPartition(i).isEmpty());
        try {
            l.getPartition(11);
            fail("should have thrown NSEX");
        } catch (NoSuchElementException expected){}
    }
    
    public void testPartitionsAppear() throws Exception {
        List list = new ArrayList();
        ListPartitioner partitioner = new ListPartitioner(list, 5);
        
        // add one element - the first partition should have 1 element, 
        // the rest empty
        list.add(new Object());
        assertEquals(1, partitioner.getPartition(0).size());
        for (int i = 1; i < 5; i++)
            assertTrue(partitioner.getPartition(i).isEmpty());
    }
    
    public void testPartitionsResize() throws Exception {
        List list = new ArrayList();
        ListPartitioner partitioner = new ListPartitioner(list, 5);
        
        // add 50 objects - each partition should have 10
        for (int i = 0; i < 50; i++)
            list.add(new Object());
        for (int i = 1; i < 5; i++)
            assertEquals(10,partitioner.getPartition(i).size());
        
        // remove all objects from one of the partitions
        List partition = partitioner.getPartition(1);
        partition.clear();
        
        // all partitions will hold 8 objects after re-creation
        for (int i = 1; i < 5; i++)
            assertEquals(8,partitioner.getPartition(i).size());
    }
    
    public void testLastPartitionExtends() throws Exception {
        List list = new ArrayList();
        ListPartitioner partitioner = new ListPartitioner(list, 2);
        
        // 2 partitions, 1 object in first, 2 in the second
        list.add(new Object());
        list.add(new Object());
        list.add(new Object());
        
        assertEquals(1, partitioner.getPartition(0).size());
        assertEquals(2, partitioner.getPartition(1).size());
        
        // with 5 objects there will be 2 in the first and 3 in the second
        list.add(new Object());
        list.add(new Object());
        assertEquals(2, partitioner.getPartition(0).size());
        assertEquals(3, partitioner.getPartition(1).size());
    }
}
