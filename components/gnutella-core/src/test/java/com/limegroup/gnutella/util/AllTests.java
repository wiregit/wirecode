package com.limegroup.gnutella.util;

import junit.framework.*;

/**
 * Runs all the LimeWire util tests
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("util tests");
        suite.addTest(FixedsizePriorityQueueTest.suite());
        return suite;
    }

}
