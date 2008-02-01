package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.LocalSocketAddressService;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests the magnet downloader class.
 * 
 * TODO: lots more tests to add here!!!
 */
public class MagnetDownloaderTest extends LimeTestCase {

	//private static final Log LOG = LogFactory.getLog(MagnetDownloaderTest.class);
    private DownloadManager downloadManager;
    private Mockery context;
    private AltLocFinder altLocFinder;
    private Injector injector;
	
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
    
    
    public void setUp() throws Exception {
        LocalSocketAddressProviderStub localSocketAddressProviderStub = new LocalSocketAddressProviderStub();
        localSocketAddressProviderStub.setLocalAddressPrivate(false);
        LocalSocketAddressService.setSocketAddressProvider(localSocketAddressProviderStub);
        
        context = new Mockery();
        altLocFinder = context.mock(AltLocFinder.class);
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
               bind(FileManager.class).to(FileManagerStub.class);
               bind(MessageRouter.class).to(MessageRouterStub.class);
               bind(ConnectionManager.class).to(ConnectionManagerStub.class);
               bind(AltLocFinder.class).toInstance(altLocFinder);
            } 
        });
        
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
        
        downloadManager = injector.getInstance(DownloadManager.class);
        downloadManager.initialize();
    }

    public void testInvalidMagnetDownloads() throws Exception {
    	// is invalid because we don't have a url
		MagnetOptions[] opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			downloadManager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
		
		// invalid: has empty kt
		opts = MagnetOptions.parseMagnet("magnet:?kt=&xt=urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			downloadManager.download(opts[0], false, null, null);
			fail("No illegal argument exception thrown");
		}
		catch (IllegalArgumentException iae) {
		} 
		
		// invalid: has only a display name
		opts = MagnetOptions.parseMagnet("magnet:?dn=me");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertFalse("Should not be downloadable", opts[0].isDownloadable());
		try {
			downloadManager.download(opts[0], false, null, null);
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
		downloadManager.download(opts[0], true, null, null);
				
		// valid: has a url and keyword topic
		opts = MagnetOptions.parseMagnet("magnet:?kt=test&xs=http://magnet2.limewire.com:6346");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
		
		// valid: has everything
		opts = MagnetOptions.parseMagnet("magnet:?xt=urn:sha1:KRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&dn=-weed-Soul%20Coughing-Rolling.wma&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:WRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be invalid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
	
		
		// downloadable: has kt and hash
		opts = MagnetOptions.parseMagnet("magnet:?kt=test2&xt=urn:sha1:MRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
		
		// downloadable: has dn and hash
		opts = MagnetOptions.parseMagnet("magnet:?dn=test3&xt=urn:sha1:TRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		downloadManager.download(opts[0], true, null, null);
		
		// downloadable hash only magnet
		opts = MagnetOptions.parseMagnet("magnet:?xt=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT");
		assertEquals("Wrong number of parsed magnets", 1, opts.length);
		assertTrue("Should be valid", opts[0].isDownloadable());
		assertTrue("Should be hash only", opts[0].isHashOnly());
		downloadManager.download(opts[0], true, null, null);
    }
    
    private Injector createTestUrlMagnetInjector() {
     // create new injector to bind TestUrlMagnetDownloader
        return LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MagnetDownloader.class).to(TestUrlMagnetDownloader.class);
            }
        });
    }
    
    public void testCachesAllMagnetUrls() throws Exception {
        Injector injector = createTestUrlMagnetInjector();
        CoreDownloaderFactory coreDownloaderFactory = injector.getInstance(CoreDownloaderFactory.class);
        
        MagnetOptions magnet = MagnetOptions.parseMagnet("magnet:?dn=filename&xt=urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&xs=http://magnet1.limewire.com:6346/uri-res/N2R?urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&xs=http://magnet2.limewire.com:6346/uri-res/N2R?urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT")[0];
        assertEquals(2, magnet.getDefaultURLs().length);
        
        MagnetDownloaderImpl downloader = (MagnetDownloaderImpl) coreDownloaderFactory.createMagnetDownloader(magnet, true, null, "test");
        downloader.initialize();
        downloader.initializeDownload();
        assertEquals(2, downloader.getCachedRFDs().size());
    }
    
    public void testAltLocsFromGUIDUrnsAreAdded() throws Exception {
        CoreDownloaderFactory coreDownloaderFactory = injector.getInstance(CoreDownloaderFactory.class);
        
        final GUID guid = new GUID();
        final MagnetOptions magnet = MagnetOptions.parseMagnet("magnet:?dn=filename&xt=urn:sha1:YRCIRZV5ZO56CWMNHFV4FRGNPWPPDVKT&xs=" + URN.createGUIDUrn(guid))[0];
        assertTrue(magnet.isDownloadable());
        
        final AlternateLocation alternateLocation = context.mock(AlternateLocation.class);
        // use a url remote file desc because it is easier to construct
        final RemoteFileDesc rfd = new URLRemoteFileDesc("host", 80, "filename", 1000, new UrnSet(magnet.getSHA1Urn()), new URL("http://host:80/hello"));
        
        context.checking(new Expectations() {{
            one(altLocFinder).getAlternateLocation(with(equal(guid)), with(equal(magnet.getSHA1Urn())));
            will(returnValue(alternateLocation));
            one(alternateLocation).createRemoteFileDesc(with(any(Long.class)));
            will(returnValue(rfd));
        }});
        
        MagnetDownloaderImpl downloader = (MagnetDownloaderImpl) coreDownloaderFactory.createMagnetDownloader(magnet, true, null, "test");
        downloader.initialize();
        downloader.initializeDownload();
        
        context.assertIsSatisfied();
        
        assertEquals(1, downloader.getCachedRFDs().size());
        assertSame(rfd, downloader.getCachedRFDs().iterator().next());
    }
    
    private static class TestUrlMagnetDownloader extends MagnetDownloaderImpl {

        @Inject
        TestUrlMagnetDownloader(SaveLocationManager saveLocationManager,
                DownloadManager downloadManager, FileManager fileManager,
                IncompleteFileManager incompleteFileManager, DownloadCallback downloadCallback,
                NetworkManager networkManager, AlternateLocationFactory alternateLocationFactory,
                RequeryManagerFactory requeryManagerFactory,
                QueryRequestFactory queryRequestFactory, OnDemandUnicaster onDemandUnicaster,
                DownloadWorkerFactory downloadWorkerFactory, AltLocManager altLocManager,
                ContentManager contentManager, SourceRankerFactory sourceRankerFactory,
                UrnCache urnCache, SavedFileManager savedFileManager,
                VerifyingFileFactory verifyingFileFactory, DiskController diskController,
                @Named("ipFilter") IPFilter ipFilter, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<MessageRouter> messageRouter, Provider<TigerTreeCache> tigerTreeCache,
                ApplicationServices applicationServices, RemoteFileDescFactory remoteFileDescFactory, AltLocFinder altLocFinder)
                throws SaveLocationException {
            super(saveLocationManager, downloadManager, fileManager, incompleteFileManager, downloadCallback,
                    networkManager, alternateLocationFactory, requeryManagerFactory, queryRequestFactory,
                    onDemandUnicaster, downloadWorkerFactory, altLocManager, contentManager,
                    sourceRankerFactory, urnCache, savedFileManager, verifyingFileFactory, diskController,
                    ipFilter, backgroundExecutor, messageRouter, tigerTreeCache, applicationServices,
                    remoteFileDescFactory, altLocFinder);
        }
        
        @Override
        protected RemoteFileDesc createRemoteFileDesc(String defaultURL, String filename, URN urn)
                throws IOException {
            URL url = new URL(defaultURL);
            return new URLRemoteFileDesc(url.getHost(), url.getPort(), filename, 1000, new UrnSet(urn), url);
        }
    }
}
