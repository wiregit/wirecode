package com.limegroup.gnutella.uploader;

import junit.framework.*;

/**
 * Runs all the LimeWire tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("Uploader tests");
        suite.addTest(UploadTest.suite());
        return suite;
    }

}
