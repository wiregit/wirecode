package com.limegroup.gnutella.xml;

import junit.framework.*;

/**
 * Runs all the LimeWire filter tests
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("xml tests");
        suite.addTest(CollectionTester.suite());
        suite.addTest(XMLDocSerializerTest.suite());
        return suite;
    }

}
