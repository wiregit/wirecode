package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.concurrent.FutureEvent;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.DownloadManagerEvent;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.downloader.MagnetDownloader;

public class DownloaderGuidAlternateLocationFinderTest extends BaseTestCase {

    private Mockery context;

    public DownloaderGuidAlternateLocationFinderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }

    public void testHandleEventHandlesAddEvent() {
        final MagnetDownloader downloader = context.mock(MagnetDownloader.class);
        // commented out since magnet search is not performed on add right now
        // use magnet without sha1 so we can just check if 
//        final MagnetOptions magnet = MagnetOptions.parseMagnet("magnet:?dn=file&kt=hello&xt=urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")[0];
        final DownloaderGuidAlternateLocationFinder endpointFinder 
            = new DownloaderGuidAlternateLocationFinder(null, null, null);
        
        context.checking(new Expectations() {{
            // commented out since magnet search is not performed on add right now
//            atLeast(1).of(downloader).getMagnet();
//            will(returnValue(magnet));
//            atLeast(1).of(downloader).getContentLength();
//            will(returnValue(1l));
            atLeast(1).of(downloader).addListener(endpointFinder.downloadStatusListener);
            atLeast(1).of(downloader).removeListener(endpointFinder.downloadStatusListener);
        }});
        
        // iterate through all events
        for (DownloadManagerEvent.Type type : DownloadManagerEvent.Type.values()) {
            endpointFinder.handleEvent(new DownloadManagerEvent(downloader, type));
        }
        
        context.assertIsSatisfied();
    }

    public void testSearchForPushEndpointsNonFirewalledResult() 
            throws IOException, InterruptedException {
        final AlternateLocationFactory alternateLocationFactory = context.mock(AlternateLocationFactory.class);
        final PushEndpointService pushEndpointManager = context.mock(PushEndpointService.class);
        final AltLocManager altLocManager = new AltLocManager();
        final PushEndpoint result = context.mock(PushEndpoint.class);
        final AlternateLocation alternateLocation = context.mock(AlternateLocation.class);
        final IpPort ipPort = new IpPortImpl("129.155.4.5:6666");
        
        final DownloaderGuidAlternateLocationFinder endpointFinder 
            = new DownloaderGuidAlternateLocationFinder(
                pushEndpointManager, alternateLocationFactory, altLocManager);
        
        final URN sha1Urn = URN.createSHA1Urn("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
        final GUID guid = new GUID();
        final URN guidUrn = URN.createGUIDUrn(guid);
        
        final DHTFuture<PushEndpoint> future 
            = new DHTValueFuture<PushEndpoint>(result);
        
        context.checking(new Expectations() {{
            one(pushEndpointManager).findPushEndpoint(with(equal(guid)));
            will(returnValue(future));
            
            allowing(result).getValidExternalAddress();
            will(returnValue(ipPort));
            allowing(result).getProxies();
            will(returnValue(new IpPortSet(ipPort)));
            
            one(alternateLocationFactory).createDirectAltLoc(ipPort, sha1Urn);
            will(returnValue(alternateLocation));
            allowing(alternateLocation).getSHA1Urn();
            will(returnValue(sha1Urn));
        }});
        
        endpointFinder.searchForPushEndpoints(
                sha1Urn, Collections.singleton(guidUrn));
        
        // The DownloaderGuidAlternateLocationFinder class is calling
        // everything after findPushEndpoint() on Mojito's internal
        // Event-Thread which is causing a race-condition with Mockery's
        // assertIsSatisfied() must wait for the event to finish before
        // it can check the final state.
        if (!awaitEventThread(future, 10L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
        
        context.assertIsSatisfied();
    }
    
    public void testSearchForPushEndpointsFirewalledResult() 
            throws IOException, InterruptedException {
        final AlternateLocationFactory alternateLocationFactory = context.mock(AlternateLocationFactory.class);
        final PushEndpointService pushEndpointManager = context.mock(PushEndpointService.class);
        final AltLocManager altLocManager = new AltLocManager();
        final PushEndpoint result = context.mock(PushEndpoint.class);
        final AlternateLocation alternateLocation = context.mock(AlternateLocation.class);
        final IpPort ipPort = new IpPortImpl("129.155.4.5:6666");
        final IpPort otherIpPort = new IpPortImpl("129.155.4.5:6667");
        
        final DownloaderGuidAlternateLocationFinder endpointFinder 
            = new DownloaderGuidAlternateLocationFinder(
                pushEndpointManager, alternateLocationFactory, altLocManager);
        
        final URN sha1Urn = URN.createSHA1Urn("urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C");
        final GUID guid = new GUID();
        final URN guidUrn = URN.createGUIDUrn(guid);
        
        final DHTFuture<PushEndpoint> future 
            = new DHTValueFuture<PushEndpoint>(result);
        
        context.checking(new Expectations() {{
            one(pushEndpointManager).findPushEndpoint(with(equal(guid)));
            will(returnValue(future));
            
            allowing(result).getValidExternalAddress();
            will(returnValue(ipPort));
            allowing(result).getProxies();
            will(returnValue(new IpPortSet(otherIpPort)));
            
            one(alternateLocationFactory).createPushAltLoc(result, sha1Urn);
            will(returnValue(alternateLocation));
            allowing(alternateLocation).getSHA1Urn();
            will(returnValue(sha1Urn));
        }});
        
        endpointFinder.searchForPushEndpoints(
                sha1Urn, Collections.singleton(guidUrn));
        
        // The DownloaderGuidAlternateLocationFinder class is calling
        // everything after findPushEndpoint() on Mojito's internal
        // Event-Thread which is causing a race-condition with Mockery's
        // assertIsSatisfied() must wait for the event to finish before
        // it can check the final state.
        if (!awaitEventThread(future, 10L, TimeUnit.MILLISECONDS)) {
            fail("Shouldn't have failed!");
        }
        
        context.assertIsSatisfied();
    }

    private static <T> boolean awaitEventThread(DHTFuture<T> future, 
            long timeout, TimeUnit unit) throws InterruptedException {
        
        final CountDownLatch latch = new CountDownLatch(1);
        future.addFutureListener(new EventListener<FutureEvent<T>>() {
            @Override
            public void handleEvent(FutureEvent<T> event) {
                latch.countDown();
            }
        });
        
        return latch.await(timeout, unit);
    }
}
