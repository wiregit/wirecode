package com.limegroup.gnutella.util;

import junit.framework.*;

/**
 * Runs all the LimeWire util tests
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("util tests");
        suite.addTest(FixedsizePriorityQueueTest.suite());
        suite.addTest(StringUtilsTest.suite());
        suite.addTest(BandwidthThrottleTest.suite());
        suite.addTest(BufferTest.suite());
        suite.addTest(COBSUtilTest.suite());
		suite.addTest(CommonUtilsTest.suite());
        return suite;
    }

}
