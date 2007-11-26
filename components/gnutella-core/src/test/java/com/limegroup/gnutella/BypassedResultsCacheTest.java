package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactory;
import com.limegroup.gnutella.downloader.PurchasedStoreDownloaderFactory;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class BypassedResultsCacheTest extends BaseTestCase {

    private QueryActiveActivityCallback callback;
    
    private GUIDActiveDownloadManager manager;
    
    private GUESSEndpoint point1;
    
    private GUESSEndpoint point2;
    
    private GUESSEndpoint point3;
    
    public BypassedResultsCacheTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ActivityCallback.class).to(QueryActiveActivityCallback.class);
                bind(DownloadManager.class).to(GUIDActiveDownloadManager.class);
            };
        });
        callback = (QueryActiveActivityCallback)injector.getInstance(ActivityCallback.class);
        manager = (GUIDActiveDownloadManager)injector.getInstance(DownloadManager.class);
        
        point1 = new GUESSEndpoint(InetAddress.getLocalHost(), 5555);
        point2 = new GUESSEndpoint(InetAddress.getLocalHost(), 6666);
        point3 = new GUESSEndpoint(InetAddress.getLocalHost(), 7777);
    }
    
    public static Test suite() {
        return buildTestSuite(BypassedResultsCacheTest.class);
    }
    
    public void testAddBypassedSource() {
        
        BypassedResultsCache cache = new BypassedResultsCache(callback, manager);
        
        // success
        callback.isQueryAlive = true;
        GUID guid = new GUID();
        assertTrue(cache.addBypassedSource(guid, point1));
        assertFalse(cache.addBypassedSource(guid, point1));
        
        callback.isQueryAlive = false;
        manager.isGUIDFor = true;
        assertTrue(cache.addBypassedSource(guid, point2));
        assertFalse(cache.addBypassedSource(guid, point2));
        
        manager.isGUIDFor = false;
        assertFalse(cache.addBypassedSource(guid, point3));
        
        assertContains(cache.getQueryLocs(guid), point1);
        assertContains(cache.getQueryLocs(guid), point2);
    }
    
    public void testExpiration() {
        BypassedResultsCache cache = new BypassedResultsCache(callback, manager);
        callback.isQueryAlive = true;
        GUID guid = new GUID();
        assertTrue(cache.addBypassedSource(guid, point1));
        
        manager.isGUIDFor = false;
        cache.queryKilled(guid);
        
        assertTrue(cache.getQueryLocs(guid).isEmpty());
        
        assertTrue(cache.addBypassedSource(guid, point1));
        
        callback.isQueryAlive = false;
        cache.downloadFinished(guid);
        
        assertTrue(cache.getQueryLocs(guid).isEmpty());
    }
    
    public void testUpperThreshholdIsHonored() throws UnknownHostException {
        callback.isQueryAlive = true;
        GUID guid = new GUID();
        BypassedResultsCache cache = new BypassedResultsCache(callback, manager);
        
        for (int i = 0; i < BypassedResultsCache.MAX_BYPASSED_RESULTS; i++) {
            assertTrue(cache.addBypassedSource(guid, new GUESSEndpoint(InetAddress.getLocalHost(), 500 + i)));
        }
        
        assertFalse(cache.addBypassedSource(guid, point1));
    }

    private static class QueryActiveActivityCallback extends ActivityCallbackStub {
        
        boolean isQueryAlive;
        
        @Override
        public boolean isQueryAlive(GUID guid) {
            return isQueryAlive;
        }
        
    }
    
    @Singleton
    private static class GUIDActiveDownloadManager extends DownloadManagerStub {
        
        boolean isGUIDFor;
        
        @Inject
        public GUIDActiveDownloadManager(NetworkManager networkManager,
                DownloadReferencesFactory downloadReferencesFactory,
                @Named("inNetwork") DownloadCallback innetworkCallback,
                BTDownloaderFactory btDownloaderFactory,
                Provider<DownloadCallback> downloadCallback,
                Provider<MessageRouter> messageRouter,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<TorrentManager> torrentManager,
                Provider<PushDownloadManager> pushDownloadManager,
                BrowseHostHandlerManager browseHostHandlerManager,
                GnutellaDownloaderFactory gnutellaDownloaderFactory,
                PurchasedStoreDownloaderFactory purchasedDownloaderFactory) {
            super(networkManager, downloadReferencesFactory, innetworkCallback, btDownloaderFactory,
                    downloadCallback, messageRouter, backgroundExecutor, torrentManager, pushDownloadManager,
                    browseHostHandlerManager, gnutellaDownloaderFactory, purchasedDownloaderFactory);
        }



        @Override
        public synchronized boolean isGuidForQueryDownloading(GUID guid) {
            return isGUIDFor;
        }
    }
    
}
