package com.limegroup.gnutella;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.limewire.http.HttpChannelTest;
import org.limewire.http.HttpCoreUtilsTest;
import org.limewire.http.HttpServiceHandlerTest;

import com.limegroup.gnutella.uploader.FilePieceReaderTest;
import com.limegroup.gnutella.uploader.HTTPUploaderTest;
import com.limegroup.gnutella.uploader.UploadSlotManagerTest;
import com.limegroup.gnutella.uploader.UploadTest;

public class AllUploadTests {

    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(HttpChannelTest.suite());
        suite.addTest(HttpCoreUtilsTest.suite());
        suite.addTest(HttpServiceHandlerTest.suite());
        
        suite.addTest(HTTPUploadManagerTest.suite());
        suite.addTest(HTTPUploaderTest.suite());
        suite.addTest(UploadSlotManagerTest.suite());
        suite.addTest(UrnHttpRequestTest.suite());
        suite.addTest(HTTPAcceptorTest.suite());
        suite.addTest(FilePieceReaderTest.suite());
        suite.addTest(UploaderTest.suite());
        suite.addTest(UploadTest.suite());

        return suite;
    }

}
