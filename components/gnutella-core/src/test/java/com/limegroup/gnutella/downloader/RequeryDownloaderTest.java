package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import java.io.*;
import junit.framework.*;

public class RequeryDownloaderTest extends TestCase {

    public RequeryDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(RequeryDownloaderTest.class);
    }

	public void testLegacy() {
        RequeryDownloader.unitTest();
    }
}
