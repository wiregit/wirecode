package com.limegroup.gnutella.bootstrap;

import junit.framework.*;
import com.limegroup.gnutella.xml.*;

/**
 * Runs GWebCache tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("GWebCache tests");
        suite.addTest(BootstrapServerTest.suite());
        suite.addTest(BootstrapServerManagerTest.suite());
        suite.addTest(HostCatcherFetchTest.suite());
        return suite;
    }

}
