package com.limegroup.gnutella.messages.vendor;

import junit.framework.*;

/**
 * Runs all the LimeWire messaging tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("All LimeWire vendor messaging tests");
		suite.addTest(VendorMessageTest.suite());
		suite.addTest(ConnectBackVendorMessageTest.suite());
		suite.addTest(MessagesSupportedVendorMessageTest.suite());
        return suite;
    }
}
