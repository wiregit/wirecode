/*
 * Runs couple of quick tests on the Leaf comparator.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

import junit.framework.Test;

public class LeafComparatorTest extends BaseTestCase {
	
	
	static Connection leaf1, leaf2, leaf3, leaf4, leaf5, leafNull;
	
	static Collection all;
	
	static LeafComparator comparator = new LeafComparator();
	
	public LeafComparatorTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(LeafComparatorTest.class);
    }
	
	public void setUp() {
		
		//1 = best, 5 = worst. 
		leaf1 = new GoodLeafCandidate("1.2.3.1",1000,(short)600,0);
		leaf2 = new GoodLeafCandidate("1.2.3.2",1000,(short)600,100);
		leaf3 = new GoodLeafCandidate("1.2.3.3",1000,(short)700,1000);
		leaf4 = new GoodLeafCandidate("1.2.3.4",1000,(short)400,0);
		leaf5 = new GoodLeafCandidate("1.2.3.5",1000,(short)-2,-5);
		leafNull = null;
		
		all = new Vector();
		all.add(leaf1);
		all.add(leaf2);
		all.add(leaf3);
		all.add(leaf4);
		all.add(leaf5);
		all.add(leafNull);
	}
	
	/**
	 * tests general ordering.
	 */
	public void testGeneralTest() throws Exception {
		
		TreeSet sampleSet = new TreeSet(comparator);
		sampleSet.addAll(all);
		
		Object [] sorted = sampleSet.toArray();
		
		assertNull(sorted[0]);
		assertTrue(leaf5.isSame((IpPort)sorted[1]));
		assertTrue(leaf4.isSame((IpPort)sorted[2]));
		assertTrue(leaf3.isSame((IpPort)sorted[3]));
		assertTrue(leaf2.isSame((IpPort)sorted[4]));
		assertTrue(leaf1.isSame((IpPort)sorted[5]));
		
	}
}
