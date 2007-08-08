package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests the magnet downloader class.
 * 
 * TODO: lots more tests to add here!!!
 */
public class MagnetDownloaderTest extends LimeTestCase {

	//private static final Log LOG = LogFactory.getLog(MagnetDownloaderTest.class);
    
    final static int PORT=6666;
    private DownloadManagerStub manager;
    private FileManager fileman;
    private ActivityCallback callback;
    private MessageRouter router;
	
    /**
     * Creates a new test instance.
     *
     * @param name the test name
     */
    public MagnetDownloaderTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(MagnetDownloaderTest.class);
    }
    
    public static void globalSetUp() throws Exception{
        ConnectionManagerStub cmStub = new ConnectionManagerStub() {
            public boolean isConnected() {
                return true;
            }
        };
        PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
        assertTrue(ProviderHacks.getConnectionServices().isConnected());
    }
    
    public void setUp() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        manager = new DownloadManagerStub();
        fileman = new FileManagerStub();
        callback = new ActivityCallbackStub();
        router = new MessageRouterStub();
        manager.initialize(callback, router, fileman);
        PrivilegedAccessor.setValue(RouterService.class,"callback",callback);
        PrivilegedAccessor.setValue(RouterService.class,"messageRouter",router);
    }

    public void testInvalidMagnetDownloads() throws Exception {
    	// is invalid because we don't have a url
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			manager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
		
		// invalid: has empty kt
		opts = MagnetOptions.parseMagnet("magnet:?kt=&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			manager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
		
		// invalid: has only a display name
		opts = MagnetOptions.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			manager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
    }
    
    public void testValidMagnetDownloads() throws Exception {
    	// valid: has a url and a sha1
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		manager.download(opts[0], true, null, null);
				
		// valid: has a url and keyword topic
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		manager.download(opts[0], true, null, null);
		
		// valid: has everything
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:KRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		manager.download(opts[0], true, null, null);
		
		
		// downloadable: has kt and hash
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xt=urn:sha1:MRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		manager.download(opts[0], true, null, null);
		
		// downloadable: has dn and hash
		opts = MagnetOptions.parseMagnet("magnet:?dn=test&xt=urn:sha1:TRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		manager.download(opts[0], true, null, null);
		
		// downloadable hash only magnet
		opts = MagnetOptions.parseMagnet("magnet:?xt=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		manager.download(opts[0], true, null, null);
    }
}
