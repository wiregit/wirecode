package com.limegroup.gnutella.routing;

import junit.framework.*;

/**
 * Runs all the LimeWire filter tests
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("routing tests");
        suite.addTest(QueryRouteTableTest.suite());
        return suite;
    }

}
