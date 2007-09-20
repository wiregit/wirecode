package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocSearchListener;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;

/// note -- mock ManagedDownloader
public class RequeryBehaviorTest extends LimeTestCase {

    private static final Log LOG = LogFactory.getLog(RequeryBehaviorTest.class);
    public RequeryBehaviorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryBehaviorTest.class);
    }
    
    
    public static void globalSetUp() throws Exception {
        // load up
        TestFile.length();
        RequeryManager.NO_DELAY = true;
        // TODO: fix this 
        PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",100);
        PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",100);
    }
    
    static DownloadManager downloadManager;
    static MyAltLocFinder myAltFinder;
    static MyDHTManager myDHTManager;
    static Runnable pump;
    public void setUp() throws Exception {
        Module m = new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).to(MyDHTManager.class);
                bind(AltLocFinder.class).to(MyAltLocFinder.class);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).to(MyExecutor.class);
            }
        };
        Injector injector = LimeTestUtils.createInjector(m);
        downloadManager = injector.getInstance(DownloadManager.class);
        downloadManager.initialize();
        myAltFinder = (MyAltLocFinder) injector.getInstance(AltLocFinder.class);
        myDHTManager = (MyDHTManager) injector.getInstance(DHTManager.class);
        MyExecutor myExecutor = (MyExecutor) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("backgroundExecutor")));
        Thread.sleep(1100);
        assertNotNull(myExecutor.r);
        pump = myExecutor.r;
    }
    
    /**
     * tests that if a result arrives and fails while we're
     * waiting for results from a gnet or dht query, we'll
     * fall back into the appropriate state.
     */
    public void testWaitingForResults() throws Exception {
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.setValue(31*1000);
        
        Downloader downloader = downloadManager.download(new RemoteFileDesc[] {fakeRFD()}, new ArrayList<RemoteFileDesc>(), new GUID() , true, new File("."), "asdf");
        LOG.debug("starting downloader");
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        // click resume, launch a dht query
        LOG.debug("clicking resume");
        myDHTManager.on = true;
        downloader.resume();
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
    }   
    
    
    private static RemoteFileDesc fakeRFD() {
        return new RemoteFileDesc("0.0.0.1", (int)(Math.random() * Short.MAX_VALUE +1000), 13l,
                "badger", 1024,
                new byte[16], 56, false, 4, true, null, new HashSet(),
                false, false,"",null, -1, false);
    }
    
    
    private int waitForStateToEnd(DownloadStatus status, Downloader downloader) throws Exception {
        LOG.debug("waiting for "+status+" to end");
        int pumps = 0;
        // causes busy-loop but this is a test so its ok.
        while(downloader.getState() == status) {
            pump.run();
            pumps++;
            LOG.debug("pump "+pumps);
        }
        LOG.debug("out of "+status+" now "+downloader.getState());
        return pumps;
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
        private volatile AltLocSearchListener listener;
        
        volatile boolean cancelled;
        @Inject
        public MyAltLocFinder(DHTManager manager, AlternateLocationFactory alternateLocationFactory, AltLocManager altLocManager, PushEndpointFactory pushEndpointFactory) {
            super(manager, alternateLocationFactory, altLocManager, pushEndpointFactory);
        }
        
        
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
    private static class MyExecutor extends ScheduledExecutorServiceStub {
        public Runnable r;
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            if (initialDelay == delay && delay == 1000)
                r = command;
            return null;
        }
        
    }
}
