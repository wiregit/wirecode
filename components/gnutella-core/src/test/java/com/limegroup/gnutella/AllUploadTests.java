package com.limegroup.gnutella;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.uploader.FilePieceReaderTest;
import com.limegroup.gnutella.uploader.UploadSlotManagerTest;

public class AllUploadTests {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        //$JUnit-BEGIN$
        suite.addTest(HTTPUploadManagerTest.suite());
        suite.addTest(UploadSlotManagerTest.suite());
        suite.addTest(UrnHttpRequestTest.suite());
        suite.addTest(HTTPAcceptorTest.suite());
        suite.addTest(FilePieceReaderTest.suite());
        //suite.addTest(UploadTest.suite());
        suite.addTest(UploaderTest.suite());
        //$JUnit-END$
        return suite;
    }

}
