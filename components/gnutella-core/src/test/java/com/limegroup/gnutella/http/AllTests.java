package com.limegroup.gnutella.http;

import junit.framework.*;

/**
 * Runs all the LimeWire messaging tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("All LimeWire http tests");
		suite.addTest(HttpUtilsTest.suite());
        return suite;
    }
}
