package com.limegroup.gnutella.downloader;

import junit.framework.*;

public class ManagedDownloaderTest extends TestCase {

    public ManagedDownloaderTest(String name) {
        super(name);
    }

    
    public static Test suite() {
        return new TestSuite(ManagedDownloaderTest.class);
    }
    
    public void testLegacy() {
        ManagedDownloader.unitTest();
    }
}
