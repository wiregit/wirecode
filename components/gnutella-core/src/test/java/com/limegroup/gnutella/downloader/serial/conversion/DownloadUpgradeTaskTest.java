package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.LimeTestUtils;

public class DownloadUpgradeTaskTest extends BaseTestCase {
    
    public DownloadUpgradeTaskTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DownloadUpgradeTaskTest.class);
    }

    public void testConversion() throws Exception {
        File file = LimeTestUtils.getResourceInPackage("allKindsofDownloads.dat", DownloadUpgradeTask.class);
        
    }
    

}
