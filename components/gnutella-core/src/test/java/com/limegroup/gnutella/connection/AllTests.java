package com.limegroup.gnutella.connection;

import junit.framework.*;
import com.limegroup.gnutella.xml.*;

/**
 * Runs all the LimeWire tests.
 */
public class AllTests {
    public static Test suite() {
        TestSuite suite=new TestSuite("All connection tests");
        suite.addTest(ConnectionHandshakeTest.suite());
        suite.addTest(ConnectionTest.suite());
        return suite;
    }

}
