package com.limegroup.gnutella.filters;

import junit.framework.*;

/**
 * Runs all the LimeWire filter tests
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("filter tests");
        suite.addTest(GUIDFilterTest.suite());
        return suite;
    }

}
