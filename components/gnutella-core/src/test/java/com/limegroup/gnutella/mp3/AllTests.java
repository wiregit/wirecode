package com.limegroup.gnutella.mp3;

import junit.framework.*;

/**
 * Runs all the LimeWire filter tests
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("mp3 tests");
        suite.addTest(MP3Test.suite());
        return suite;
    }

}
