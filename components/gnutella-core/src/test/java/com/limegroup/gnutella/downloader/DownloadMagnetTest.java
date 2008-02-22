package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Integration tests for magnet downloads.
 */
public class DownloadMagnetTest extends DownloadTestCase {

    private static final Log LOG = LogFactory.getLog(DownloadMagnetTest.class);
    
    private final int PPORT_1 = 10001;
    
    public DownloadMagnetTest(String name) {
        super(name);
    }

    public void testMagnetWithAvailableAlternateLocations() {
        
    }
    
    public void testMagnetWithoutAvailableAlternateLocations() {
        
    }
    
    
    
}
