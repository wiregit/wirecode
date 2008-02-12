package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;

/**
 * Integration tests for magnet downloads.
 */
public class DownloadMagnetTest extends DownloadTestCase {

    private static final Log LOG = LogFactory.getLog(DownloadMagnetTest.class);
    
    private final int PPORT_1 = 10001;
    
    public DownloadMagnetTest(String name) {
        super(name);
    }
    
    public void testMagnetDownloadWithoutFilesize() throws Exception {
        LOG.info("-Testing non-swarmed push download");

        GUID guid = new GUID();
        URN guidUrn = URN.createGUIDUrn(guid);
        
        MagnetOptions magnet = MagnetOptions.parseMagnet("magnet:?xt=" + TestFile.hash()
                + "&xs=" + guidUrn  
                + "&dn=filename")[0];
        assertTrue(magnet.isDownloadable());
        
        TestUploader uploader = injector.getInstance(TestUploader.class);
        uploader.start("push uploader");
        
        
        tGeneric(magnet);
    }

}
