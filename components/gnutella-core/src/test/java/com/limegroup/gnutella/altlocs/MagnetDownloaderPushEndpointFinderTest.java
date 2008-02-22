package com.limegroup.gnutella.altlocs;

import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.dht.db.SearchListener;
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

    @SuppressWarnings("unchecked")
    public void testSearchForPushEndpoints() throws Exception {
        final AlternateLocationFactory alternateLocationFactory = context.mock(AlternateLocationFactory.class);
        final PushEndpointService pushEndpointManager = context.mock(PushEndpointService.class);
        final AltLocManager altLocManager = new AltLocManager();
        final PushEndpoint result = context.mock(PushEndpoint.class);
        final AlternateLocation alternateLocation = context.mock(AlternateLocation.class);
        
        final MagnetDownloaderPushEndpointFinder endpointFinder = 
            new MagnetDownloaderPushEndpointFinder(null, pushEndpointManager, alternateLocationFactory, altLocManager);
        
        final URN sha1Urn = URN.createSHA1Urn("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
        final GUID guid = new GUID();
        final URN guidUrn = URN.createGUIDUrn(guid);
        
        context.checking(new Expectations() {{
            one(pushEndpointManager).findPushEndpoint(with(equal(guid)), with(any(SearchListener.class)));
            will(new CustomAction("call listener") {
                public Object invoke(Invocation invocation) throws Throwable {
                    ((SearchListener<PushEndpoint>)invocation.getParameter(1)).handleResult(result);
                    return null;
                }
            });
            one(alternateLocationFactory).createPushAltLoc(result, sha1Urn);
            will(returnValue(alternateLocation));
            allowing(alternateLocation).getSHA1Urn();
            will(returnValue(sha1Urn));
        }});
        
        try {
            endpointFinder.searchForPushEndpoints(sha1Urn, Collections.singleton(guidUrn));
            fail("AltLocManager was not called, which should have caused an IllegalState exception for an invalid AlternateLocation");
        } catch (IllegalStateException ise) {
        }
        context.assertIsSatisfied();
    }

    public void testAddsListener() {
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final MagnetDownloaderPushEndpointFinder endpointFinder = 
            new MagnetDownloaderPushEndpointFinder(Providers.of(downloadManager), null, null, null);
        
        context.checking(new Expectations() {{
            one(downloadManager).addListener(endpointFinder, endpointFinder);
        }});
        
        endpointFinder.start();
        
        context.assertIsSatisfied();
    }

    public void testRemoveListener() {
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final MagnetDownloaderPushEndpointFinder endpointFinder = 
            new MagnetDownloaderPushEndpointFinder(Providers.of(downloadManager), null, null, null);
        
        context.checking(new Expectations() {{
            one(downloadManager).removeListener(endpointFinder, endpointFinder);
        }});
        
        endpointFinder.stop();
        
        context.assertIsSatisfied();
    }

}
