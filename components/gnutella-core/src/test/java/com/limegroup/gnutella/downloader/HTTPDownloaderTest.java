package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.http.*;

import junit.framework.*;

public class HTTPDownloaderTest extends TestCase {

    public HTTPDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(HTTPDownloaderTest.class);
    }

	public void testLegacy() {
        HTTPDownloader.unitTest();
	}
}
