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
        suite.addTest(DownloadTest.suite());
        suite.addTest(ManagedDownloaderTest.suite());
        suite.addTest(TestOldDownloads.suite());
        suite.addTest(HTTPDownloaderTest.suite());
        suite.addTest(IntervalTest.suite());
        suite.addTest(MiniRemoteFileDescTest.suite());
        suite.addTest(RequeryDownloaderTest.suite());
        suite.addTest(IncompleteFileManagerTest.suite());
        suite.addTest(AutoDownloaderDetailsTest.suite());
        return suite;
    }

}
