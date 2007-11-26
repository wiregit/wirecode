package com.limegroup.gnutella.downloader;

import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.BrowseHostHandlerManager;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.NullDHTController;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocSearchListener;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.LimeWireUtils;

public class RequeryManagerTest extends LimeTestCase {

    public RequeryManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryManagerTest.class);
    }
    
    private RequeryManagerFactory requeryManagerFactory;
    private MyDHTManager dhtManager;
    private MyManagedDownloader managedDownloader;
    private MyDownloadManager downloadManager;
    private MyAltLocFinder altLocFinder;
    
    public void setUp() throws Exception {
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        RequeryManager.NO_DELAY = true;
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
                bind(DHTManager.class).to(MyDHTManager.class);
                bind(DownloadManager.class).to(MyDownloadManager.class);
                bind(AltLocFinder.class).to(MyAltLocFinder.class);
            } 
        });

        dhtManager = (MyDHTManager)injector.getInstance(DHTManager.class); 
        downloadManager = (MyDownloadManager)injector.getInstance(DownloadManager.class);    
        altLocFinder = (MyAltLocFinder)injector.getInstance(AltLocFinder.class);
        managedDownloader = new MyManagedDownloader(downloadManager);
        requeryManagerFactory = injector.getInstance(RequeryManagerFactory.class);
        
        setPro(false);
    }
    
    private void setPro(boolean pro) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", pro);
        assertEquals(pro, LimeWireUtils.isPro());
    }
    
    public void testRegistersWithDHTManager() throws Exception {
        assertNull(dhtManager.listener);
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        assertSame(requeryManager, dhtManager.listener);
        requeryManager.cleanUp();
        assertNull(dhtManager.listener);
    }
    
    public void testCancelsQueries() throws Exception {
        dhtManager.on = true;
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        requeryManager.activate();
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener);
        assertTrue(requeryManager.isWaitingForResults());
        assertFalse(altLocFinder.cancelled);
        
        requeryManager.cleanUp();
        assertTrue(altLocFinder.cancelled);
    }
    
    public void testNotInitedDoesNothingBasic() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        
        // shouldn't trigger any queries
        dhtManager.on = true;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener);
        assertNull(downloadManager.requerier);
        assertNull(managedDownloader.getState());
    }
    
    public void testNotInitedAutoDHTPro() throws Exception {
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        setPro(true);
        
        // if dht is off do nothing
        dhtManager.on = false;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener);
        assertNull(managedDownloader.getState());
        
        // if dht is on start querying
        dhtManager.on = true;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener);
        assertSame(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        
        // But immediately after, requires an activate (for gnet query)
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // but if some time passes, a dht query will work again.
        requeryManager.handleAltLocSearchDone(false);
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        // and we should start a lookup
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // make sure after that lookup finishes, can still do gnet 
        requeryManager.handleAltLocSearchDone(false);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // some time passes, but we've hit our dht queries limit
        // can still send gnet though
        requeryManager.handleAltLocSearchDone(false);
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    /**
     * tests that only a single gnet query is sent if dht is off. 
     */
    public void testOnlyGnetIfNoDHT() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        
        dhtManager.on = false;
        assertNull(altLocFinder.listener);
        assertNull(downloadManager.requerier);
        assertNull(managedDownloader.getState());
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.activate();
        assertTrue(requeryManager.canSendQueryNow());
        
        // first try a requery that will not work
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener); // should not try dht
        assertSame(managedDownloader, downloadManager.requerier); // should have tried gnet
        assertEquals(DownloadStatus.WAITING_FOR_GNET_RESULTS, managedDownloader.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, managedDownloader.getRemainingStateTime());
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // but if we try again, nothing happens.
        downloadManager.requerier = null;
        managedDownloader.setState(null);
        requeryManager.sendQuery();
        assertNull(downloadManager.requerier);
        assertNull(managedDownloader.getState());
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testWaitsForStableConns() throws Exception {
        // no DHT nor connections
        dhtManager.on = false;
        RequeryManager.NO_DELAY = false;
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        requeryManager.activate();
        requeryManager.sendQuery();
        assertNull(downloadManager.requerier);
        assertSame(DownloadStatus.WAITING_FOR_CONNECTIONS, managedDownloader.getState());
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        
        // now we get connected
        RequeryManager.NO_DELAY = true;
        requeryManager.sendQuery();
        // should be sent.
        assertSame(managedDownloader, downloadManager.requerier);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,managedDownloader.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, managedDownloader.getRemainingStateTime());
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testDHTTurnsOnStartsAutoIfInited() throws Exception {
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        // with dht off, send a query
        dhtManager.on = false;
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        
        requeryManager.activate();
        requeryManager.sendQuery();
        assertSame(managedDownloader, downloadManager.requerier);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,managedDownloader.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, managedDownloader.getRemainingStateTime());
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // query fails, dht still off
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertNull(altLocFinder.listener);
        
        // turn the dht on should immediately query
        // even though the gnet query happened recently
        dhtManager.on = true;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        // and we should start a lookup
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // make sure after that lookup finishes, we still can't do gnet 
        requeryManager.handleAltLocSearchDone(false);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // some time passes, can send one more dht query
        altLocFinder.listener = null;
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // more time passes, but we hit our dht query limit so we can't do anything.
        altLocFinder.listener = null;
        PrivilegedAccessor.setValue(requeryManager, "lastQuerySent", 1);
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testGnetFollowsDHT() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        dhtManager.on = true;
        requeryManager.activate();
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        
        // with dht on, start a requery
        requeryManager.sendQuery();
        assertSame(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertNull(downloadManager.requerier);
        assertSame(requeryManager, altLocFinder.listener);
        assertTrue(requeryManager.isWaitingForResults());
        
        // pretend the dht lookup fails
        requeryManager.handleAltLocSearchDone(false);
        assertFalse(requeryManager.isWaitingForResults());
        assertSame(DownloadStatus.GAVE_UP, managedDownloader.getState());
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        
        // the next requery should be gnet
        altLocFinder.listener = null;
        requeryManager.sendQuery();
        assertTrue(requeryManager.isWaitingForResults());
        assertSame(managedDownloader, downloadManager.requerier);
        assertNull(altLocFinder.listener);
        
        // from now on we should give up & no more requeries
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testGnetFollowsDHTPro() throws Exception {
        setPro(true);
        testGnetFollowsDHT();
    }
    
    public void testOnlyGnetPro() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        dhtManager.on = true;
        setPro(true);
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener); // sent a DHT query
        assertEquals(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertTrue(requeryManager.isWaitingForResults());
        requeryManager.handleAltLocSearchDone(false); // finish it
        assertFalse(requeryManager.isWaitingForResults());
        
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        requeryManager.activate(); // now activate it.
        altLocFinder.listener = null;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();        
        assertTrue(requeryManager.isWaitingForResults());
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, managedDownloader.getState());
        assertSame(managedDownloader, downloadManager.requerier);
        assertNull(altLocFinder.listener);
        
        assertFalse(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
    }
    
    public void testDHTTurnsOff() throws Exception {
        RequeryManager requeryManager = requeryManagerFactory.createRequeryManager(managedDownloader);
        dhtManager.on = true;
        setPro(true); // so we immediately launch a query
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener); // sent a DHT query
        assertEquals(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertTrue(requeryManager.isWaitingForResults());
        
        // now turn the dht off
        dhtManager.on = false;
        requeryManager.handleDHTEvent(new DHTEvent(new NullDHTController(), DHTEvent.Type.STOPPED));
        assertFalse(requeryManager.isWaitingForResults());
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertFalse(requeryManager.canSendQueryNow());
        
        // turn the dht on again, and even though no time has passed
        // since the last query we can still do one
        dhtManager.on = true;
        
        altLocFinder.listener = null;
        assertTrue(requeryManager.canSendQueryAfterActivate());
        assertTrue(requeryManager.canSendQueryNow());
        requeryManager.sendQuery();
        assertSame(requeryManager, altLocFinder.listener); // sent a DHT query
        assertEquals(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertTrue(requeryManager.isWaitingForResults());
    }
    
    @Singleton
    private static class MyDHTManager extends DHTManagerStub {

        private volatile DHTEventListener listener;
        private volatile boolean on;
        
        public void addEventListener(DHTEventListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isMemberOfDHT() {
            return on;
        }

        @Override
        public void removeEventListener(DHTEventListener listener) {
            if (this.listener == listener)
                this.listener = null;
        }
    }
    
    @Singleton    
    private static class MyAltLocFinder extends AltLocFinder {
        
        @Inject
        public MyAltLocFinder(DHTManager manager,
                AlternateLocationFactory alternateLocationFactory, AltLocManager altLocManager,
                PushEndpointFactory pushEndpointFactory) {
            super(manager, alternateLocationFactory, altLocManager, pushEndpointFactory);
            // TODO Auto-generated constructor stub
        }

        private volatile AltLocSearchListener listener;
        
        volatile boolean cancelled;
        
        @Override
        public Shutdownable findAltLocs(URN urn, AltLocSearchListener listener) {
            this.listener = listener;
            return new Shutdownable() {
                public void shutdown() {
                    cancelled = true;
                }
            };
        }

        @Override
        public boolean findPushAltLocs(GUID guid, URN urn) {
            return true;
        }
        
    }
    
    @Singleton
    private static class MyDownloadManager extends DownloadManagerStub {

        @Inject
        public MyDownloadManager(NetworkManager networkManager,
                DownloadReferencesFactory downloadReferencesFactory,
                DownloadCallback innetworkCallback, BTDownloaderFactory btDownloaderFactory,
                Provider<DownloadCallback> downloadCallback, Provider<MessageRouter> messageRouter,
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

        private volatile ManagedDownloader requerier;
        
        @Override
        public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) {
            this.requerier = requerier;
            return true;
        }
    }
    
    
    private static class MyManagedDownloader extends ManagedDownloader {

        private volatile DownloadStatus status;
        private volatile long stateTime;
                
        public MyManagedDownloader(DownloadManager downloadManager) throws SaveLocationException {
            super(new RemoteFileDesc[0], new IncompleteFileManager(), new GUID(), downloadManager);
        }
        
        @Override
        public DownloadStatus getState() {
            return status;
        }
        
        public int getRemainingStateTime() {
            return (int)stateTime;
        }
        
        @Override
        public void setState(DownloadStatus status) {
            this.status = status;
            stateTime = Integer.MAX_VALUE;
        }
        
        @Override
        public void setState(DownloadStatus status, long time) {
            this.status = status;
            stateTime = time;
        }
        
        @Override
        public QueryRequest newRequery(int numGnutellaQueries) {
            return null;
        }
    }
}
