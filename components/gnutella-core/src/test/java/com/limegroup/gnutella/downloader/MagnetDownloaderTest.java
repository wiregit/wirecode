package com.limegroup.gnutella.downloader;

import java.lang.reflect.Method;

import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Tests the magnet downloader class.
 * 
 * TODO: lots more tests to add here!!!
 */
public class MagnetDownloaderTest extends BaseTestCase {

    /**
     * Creates a new test instance.
     *
     * @param name the test name
     */
    public MagnetDownloaderTest(String name) {
        super(name);
    }
    
    /**
     * Test to make sure that URL encoding is working properly.
     */ 
    public void testEncoding() throws Exception {
        
        // URL based on a real error reported in the field.  The space causes
        // the encoding problem, so this could just as easily be:
        // "http://somehost.com/somefile p", although the example we use throws
        // in more tricky characters.
        String testUrl = "http://ftp.crihan.fr/mirrors/ftp.redhat.com/" +
            "redhat/linux/9/en/iso/i386/shrike-i386-disc1.iso6-d p?";
    
        Method createRFD = 
            PrivilegedAccessor.getMethod(
                MagnetDownloader.class, "createRemoteFileDesc", 
                    new Class[] {String.class, String.class, URN.class});
        
        String fileName = "file.txt";
        URN urn = HugeTestUtils.SHA1;
        
        // Now, the following RFD creation would fail in the HTTP client code
        // if we were not properly encoding the URL.
        createRFD.invoke(MagnetDownloader.class, 
            new Object[]{testUrl, fileName, urn});
        
        // Try one without the space just as a sanity check.
        testUrl = "http://ftp.crihan.fr/mirrors/ftp.redhat.com/" +
            "redhat/linux/9/en/iso/i386/shrike-i386-disc1.iso6-d";
        createRFD.invoke(MagnetDownloader.class, 
            new Object[]{testUrl, fileName, urn});
    }

}
