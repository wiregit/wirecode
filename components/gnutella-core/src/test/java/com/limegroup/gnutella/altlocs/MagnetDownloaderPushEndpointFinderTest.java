package com.limegroup.gnutella.altlocs;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.listener.DefaultEvent;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadManagerEvent;
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
        
        context.checking(new Expectations() {{
            one(downloader).getMagnet();
            will(returnValue(magnet));
        }});
        
        MagnetDownloaderPushEndpointFinder endpointFinder = new MagnetDownloaderPushEndpointFinder(null, null, null, null);
        
        // iterate through all events
        for (DownloadManagerEvent event : DownloadManagerEvent.values()) {
            endpointFinder.handleEvent(new DefaultEvent<CoreDownloader, DownloadManagerEvent>(downloader, event));
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
            one(downloadManager).addDownloadManagerListener(endpointFinder);
        }});
        
        endpointFinder.start();
        
        context.assertIsSatisfied();
    }

    public void testRemoveListener() {
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final MagnetDownloaderPushEndpointFinder endpointFinder = 
            new MagnetDownloaderPushEndpointFinder(downloadManager, null, null, null);
        
        context.checking(new Expectations() {{
            one(downloadManager).removeDownloadManagerListener(endpointFinder);
        }});
        
        endpointFinder.stop();
        
        context.assertIsSatisfied();
    }

}
