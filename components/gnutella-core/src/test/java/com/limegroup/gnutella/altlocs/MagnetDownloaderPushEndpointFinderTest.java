package com.limegroup.gnutella.altlocs;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerEvent;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.MagnetDownloader;

public class MagnetDownloaderPushEndpointFinderTest extends BaseTestCase {

    private Mockery context;

    public MagnetDownloaderPushEndpointFinderTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        context = new Mockery();
    }

    public void testHandleEventHandlesAddEvent() {
        final MagnetDownloader downloader = context.mock(MagnetDownloader.class);
        // use magnet without sha1 so we can just check if 
        final MagnetOptions magnet = MagnetOptions.parseMagnet("magnet:?dn=file&kt=hello")[0];
        final MagnetDownloaderPushEndpointFinder endpointFinder = new MagnetDownloaderPushEndpointFinder(null, null, null, null);
        
        context.checking(new Expectations() {{
            atLeast(1).of(downloader).getMagnet();
            will(returnValue(magnet));
            atLeast(1).of(downloader).getContentLength();
            will(returnValue(1l));
            atLeast(1).of(downloader).addListener(endpointFinder, endpointFinder.downloadStatusListener);
            atLeast(1).of(downloader).removeListener(endpointFinder, endpointFinder.downloadStatusListener);
        }});
        
        // iterate through all events
        for (DownloadManagerEvent.Type type : DownloadManagerEvent.Type.values()) {
            endpointFinder.handleEvent(new DownloadManagerEvent(downloader, type));
        }
        
        context.assertIsSatisfied();
    }

    public void testSearchForPushEndpoints() {
        fail("Not yet implemented");
    }

    public void testAddsListener() {
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final MagnetDownloaderPushEndpointFinder endpointFinder = 
            new MagnetDownloaderPushEndpointFinder(downloadManager, null, null, null);
        
        context.checking(new Expectations() {{
            one(downloadManager).addListener(endpointFinder, endpointFinder);
        }});
        
        endpointFinder.start();
        
        context.assertIsSatisfied();
    }

    public void testRemoveListener() {
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final MagnetDownloaderPushEndpointFinder endpointFinder = 
            new MagnetDownloaderPushEndpointFinder(downloadManager, null, null, null);
        
        context.checking(new Expectations() {{
            one(downloadManager).removeListener(endpointFinder, endpointFinder);
        }});
        
        endpointFinder.stop();
        
        context.assertIsSatisfied();
    }

}
