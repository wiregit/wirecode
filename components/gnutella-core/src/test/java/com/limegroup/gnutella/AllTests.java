package com.limegroup.gnutella;

import junit.framework.*;
import com.limegroup.gnutella.xml.*;

/**
 * Runs all the LimeWire tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("All LimeWire tests");

        //Unit tests
        suite.addTest(com.limegroup.gnutella.messages.AllTests.suite());
        suite.addTest(com.limegroup.gnutella.util.AllTests.suite());
        suite.addTest(com.limegroup.gnutella.filters.AllTests.suite());
        suite.addTest(ExtendedEndpointTest.suite());
        suite.addTest(HostCatcherTest.suite());
        suite.addTest(FileManagerTest.suite());
        suite.addTest(ManagedConnectionTest.suite());
        suite.addTest(GUIDTest.suite());
        suite.addTest(MessageTest.suite());
        suite.addTest(StatisticsTest.suite());
        suite.addTest(XMLDocSerializerTest.suite());
        suite.addTest(CollectionTester.suite());

        //End-to-end integration tests (includes some unit tests too)
        suite.addTest(com.limegroup.gnutella.UltrapeerRoutingTest.suite());
        suite.addTest(com.limegroup.gnutella.LeafRoutingTest.suite());
        //suite.addTest(com.limegroup.gnutella.uploader.AllTests.suite());
        //suite.addTest(com.limegroup.gnutella.downloader.AllTests.suite());

        return suite;
    }

}
