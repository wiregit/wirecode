package com.limegroup.gnutella.guess;

import junit.framework.*;

/**
 * Runs all the LimeWire messaging tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("Some LimeWire GUESS tests");
        suite.addTest(QueryKeyTest.suite());
        suite.addTest(GUESSServerSideTest.suite());
        return suite;
    }

}
