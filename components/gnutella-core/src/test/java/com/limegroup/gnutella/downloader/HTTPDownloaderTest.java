package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.http.*;

import junit.framework.*;

public class HTTPDownloaderTest extends com.limegroup.gnutella.util.BaseTestCase {

    public HTTPDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(HTTPDownloaderTest.class);
    }

	public void testLegacy() {
        HTTPDownloader.unitTest();
	}
}
