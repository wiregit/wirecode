package com.limegroup.gnutella.util;

import junit.framework.Test;

import com.sun.java.util.collections.*;
import com.sun.java.util.collections.ArrayList;

/**
 * Unit tests for FixedSizeExpiringSetTest
 */
public class FixedSizeExpiringSetTest extends com.limegroup.gnutella.util.BaseTestCase {

	int MAX_SIZE, EXPIRE_TIME;
	FixedSizeExpiringSet set, set2, set3;
	
    public FixedSizeExpiringSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FixedSizeExpiringSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp(){
    	MAX_SIZE=8;
    	EXPIRE_TIME=10*1000;  // 10 seconds
    	set = new FixedSizeExpiringSet(MAX_SIZE, EXPIRE_TIME);
    	set2 = new FixedSizeExpiringSet(); //test the default constructor
    	set3 = new FixedSizeExpiringSet(MAX_SIZE); //test 3rd constructor
    }
    
    public void testSet() throws Exception {
            
            

        // initialize a couple of objects
        String[] obj = new String[10];
        for (int i = 0; i < 10; i++) {
            obj[i] = "obj" + i;
        }
        
        //also two null objects
        Object null1 = null;
        Object null2 = null;
        
        //and their timestamps
        long timeNull1, timeNull2;
        
        //in a collection (vector so that it will keep both
        Collection col1 = new Vector();
        col1.add(null1);
        col1.add(null2);
        
        //an empty collection
        Collection emptyCol = new Vector();
        
        

        //add first null object, timestamp it
        set.add(null1);
        timeNull1=System.currentTimeMillis();
        
        // remember the times I added the objects
        long[] timeExpiring = new long[10];

        // test add, contains, remove
        for (int i = 0; i < 10; i++) {
            timeExpiring[i] = System.currentTimeMillis() + EXPIRE_TIME;
            set.add(obj[i]);
            Thread.sleep(150L);
        }

        // test fixed size 
        //set contains 2-9
        assertEquals(set.size(), MAX_SIZE);
        assertFalse(set.contains(obj[0]));
        assertFalse(set.contains(obj[1]));

        assertTrue(set.contains(obj[2]));
        set.remove(obj[2]);
        assertFalse(set.contains(obj[1]));

        set.add(obj[0]);
        //set contains  3-9, 0
        assertTrue(set.contains(obj[8]));

        assertTrue(set.contains(obj[3]));
        set.remove(obj[3]);
        //set contains 4-9, 0 (7 elements)
        assertFalse(set.contains(obj[3]));

        set.add(obj[1]);
        //set contains  and 4-9, 0-1
        assertTrue(set.contains(obj[1]));

        // test expiring!
        while (set.size() > 0) {
            for (int i = 4; i < 10; i++) {
                if (set.contains(obj[i])) {
                    assertGreaterThanOrEquals(System.currentTimeMillis(), 
                        timeExpiring[i]);
                } else {
                    assertGreaterThan(timeExpiring[i],
                        System.currentTimeMillis());
                }
            }
            Thread.sleep(100L);
        }
        
        // test addAll()
        Collection col = new ArrayList();
        for (int i = 0; i < 10; i++)
            col.add(obj[i]);
        
        set.addAll(col);
        assertEquals(set.size(), MAX_SIZE);
        assertFalse(set.contains(obj[8]));
        assertFalse(set.contains(obj[9]));
        assertTrue(set.contains(obj[0]));
        assertTrue(set.contains(obj[1]));
        //set contains 0-7

        col.clear();
        
        // test removeAll()
        for (int i = 6; i < 10; i++)
            col.add(obj[i]);
        
        set.removeAll(col);
        
        for (int i = 0; i < 6; i++)
            assertTrue(set.contains(obj[i]));

        for (int i = 6; i < 10; i++)
            assertFalse(set.contains(obj[i]));
        
        set.clear();
        assertEquals(set.size(), 0);
        
        // test retainAll
        for (int i = 0; i < 10; i++)
            set.add(obj[i]);
        
        //set contains 2-9 col contains 6-9

        set.retainAll(col);

        System.out.println("Sumeet:"+set.size());

        for (int i = 0; i < 6; i++)
            assertFalse(set.contains(obj[i]));

        for (int i = 6; i < 9; i++)
            assertTrue(set.contains(obj[i]));

    }
}