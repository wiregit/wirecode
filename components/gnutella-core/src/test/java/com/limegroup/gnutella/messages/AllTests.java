package com.limegroup.gnutella.messages;

import junit.framework.*;

/**
 * Runs all the LimeWire messaging tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("All LimeWire messaging tests");
        suite.addTest(GGEPTest.suite());
        suite.addTest(PingReplyTest.suite());
        suite.addTest(PingRequestTest.suite());
        suite.addTest(PushRequestTest.suite());
		suite.addTest(MessageUtilsTest.suite());
        return suite;
    }

}
