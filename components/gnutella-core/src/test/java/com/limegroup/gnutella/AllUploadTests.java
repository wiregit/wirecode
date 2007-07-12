package com.limegroup.gnutella;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.uploader.AltLocUploadTest;
import com.limegroup.gnutella.uploader.FilePieceReaderTest;
import com.limegroup.gnutella.uploader.FileRequestHandlerTest;
import com.limegroup.gnutella.uploader.HTTPUploaderTest;
import com.limegroup.gnutella.uploader.PushProxyRequestHandlerTest;
import com.limegroup.gnutella.uploader.PushUploadTest;
import com.limegroup.gnutella.uploader.UploadSlotManagerTest;
import com.limegroup.gnutella.uploader.UploadTest;
import com.limegroup.gnutella.uploader.UploadQueueingTest;

public class AllUploadTests {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        //$JUnit-BEGIN$
        suite.addTest(FileRequestHandlerTest.suite());
        suite.addTest(PushProxyRequestHandlerTest.suite());
        suite.addTest(HTTPUploadManagerTest.suite());
        suite.addTest(HTTPUploaderTest.suite());
        suite.addTest(UploadSlotManagerTest.suite());
        suite.addTest(UrnHttpRequestTest.suite());
        suite.addTest(HTTPAcceptorTest.suite());
        suite.addTest(FilePieceReaderTest.suite());
        suite.addTest(UploadQueueingTest.suite());
        suite.addTest(UploadTest.suite());
        suite.addTest(PushUploadTest.suite());
        suite.addTest(AltLocUploadTest.suite());
        //$JUnit-END$
        return suite;
    }

}
