package com.limegroup.gnutella.downloader;

import junit.framework.*;
import com.limegroup.gnutella.xml.*;

/**
 * Runs all the LimeWire tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("Downloader tests");
        suite.addTest(RemoteFileDescGrouperTest.suite());
        return suite;
    }

}
