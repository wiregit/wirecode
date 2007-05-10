package com.limegroup.gnutella;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllUploadTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for com.limegroup.gnutella");
        //$JUnit-BEGIN$
        suite.addTest(HTTPUploadManagerTest.suite());
        suite.addTest(UrnHttpRequestTest.suite());
        //suite.addTest(UploadTest.suite());
        suite.addTest(UploaderTest.suite());
        //$JUnit-END$
        return suite;
    }

}
