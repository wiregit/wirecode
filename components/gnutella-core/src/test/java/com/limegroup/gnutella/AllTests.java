package com.limegroup.gnutella;

import junit.framework.*;
import com.limegroup.gnutella.xml.*;

/**
 * Runs all the LimeWire tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("All LimeWire tests");
        suite.addTest(com.limegroup.gnutella.messages.AllTests.suite());
        suite.addTest(com.limegroup.gnutella.util.AllTests.suite());
        suite.addTest(ExtendedEndpointTest.suite());
        suite.addTest(HostCatcherTest.suite());
        suite.addTest(ManagedConnectionTest.suite());
        suite.addTest(GUIDTest.suite());
        suite.addTest(StatisticsTest.suite());
        suite.addTest(XMLDocSerializerTest.suite());
        suite.addTest(CollectionTester.suite());
        return suite;
    }

}
