package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.NullDHTController;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocSearchListener;
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
    
    private MyDHTManager dhtManager;
    private MyManagedDownloader managedDownloader;
    private MyDownloadManager downloadManager;
    private MyAltLocFinder altLocFinder;
    
    public void setUp() throws Exception {
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        RequeryManager.NO_DELAY = true;
        dhtManager = new MyDHTManager();
        managedDownloader = new MyManagedDownloader();
        downloadManager = new MyDownloadManager();
        altLocFinder = new MyAltLocFinder();
        setPro(false);
    }
    
    private RequeryManager createRM () {
        return new RequeryManager(managedDownloader, downloadManager, altLocFinder, dhtManager, ProviderHacks.getConnectionServices());
    }
    
    private void setPro(boolean pro) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", pro);
        assertEquals(pro, LimeWireUtils.isPro());
    }
    
    public void testRegistersWithDHTManager() throws Exception {
        assertNull(dhtManager.listener);
        RequeryManager rm = createRM();
        assertSame(rm, dhtManager.listener);
        rm.cleanUp();
        assertNull(dhtManager.listener);
    }
    
    public void testCancelsQueries() throws Exception {
        dhtManager.on = true;
        RequeryManager rm = createRM();
        rm.activate();
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener);
        assertTrue(rm.isWaitingForResults());
        assertFalse(altLocFinder.cancelled);
        
        rm.cleanUp();
        assertTrue(altLocFinder.cancelled);
    }
    
    public void testNotInitedDoesNothingBasic() throws Exception {
        RequeryManager rm = createRM();
        
        // shouldn't trigger any queries
        dhtManager.on = true;
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        rm.sendQuery();
        assertNull(altLocFinder.listener);
        assertNull(downloadManager.requerier);
        assertNull(managedDownloader.getState());
    }
    
    public void testNotInitedAutoDHTPro() throws Exception {
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        RequeryManager rm = createRM();
        
        setPro(true);
        
        // if dht is off do nothing
        dhtManager.on = false;
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        rm.sendQuery();
        assertNull(altLocFinder.listener);
        assertNull(managedDownloader.getState());
        
        // if dht is on start querying
        dhtManager.on = true;
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener);
        assertSame(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        
        // But immediately after, requires an activate (for gnet query)
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // but if some time passes, a dht query will work again.
        rm.handleAltLocSearchDone(false);
        PrivilegedAccessor.setValue(rm, "lastQuerySent", 1);
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        // and we should start a lookup
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener);
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // make sure after that lookup finishes, can still do gnet 
        rm.handleAltLocSearchDone(false);
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // some time passes, but we've hit our dht queries limit
        // can still send gnet though
        rm.handleAltLocSearchDone(false);
        PrivilegedAccessor.setValue(rm, "lastQuerySent", 1);
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
    }
    
    /**
     * tests that only a single gnet query is sent if dht is off. 
     */
    public void testOnlyGnetIfNoDHT() throws Exception {
        RequeryManager rm = createRM();

        dhtManager.on = false;
        assertNull(altLocFinder.listener);
        assertNull(downloadManager.requerier);
        assertNull(managedDownloader.getState());
        
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        rm.activate();
        assertTrue(rm.canSendQueryNow());
        
        // first try a requery that will not work
        rm.sendQuery();
        assertNull(altLocFinder.listener); // should not try dht
        assertSame(managedDownloader, downloadManager.requerier); // should have tried gnet
        assertEquals(DownloadStatus.WAITING_FOR_GNET_RESULTS, managedDownloader.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, managedDownloader.getRemainingStateTime());
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // but if we try again, nothing happens.
        downloadManager.requerier = null;
        managedDownloader.setState(null);
        rm.sendQuery();
        assertNull(downloadManager.requerier);
        assertNull(managedDownloader.getState());
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
    }
    
    public void testWaitsForStableConns() throws Exception {
        // no DHT nor connections
        dhtManager.on = false;
        RequeryManager.NO_DELAY = false;
        RequeryManager rm = createRM();
        
        rm.activate();
        rm.sendQuery();
        assertNull(downloadManager.requerier);
        assertSame(DownloadStatus.WAITING_FOR_CONNECTIONS, managedDownloader.getState());
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        
        // now we get connected
        RequeryManager.NO_DELAY = true;
        rm.sendQuery();
        // should be sent.
        assertSame(managedDownloader, downloadManager.requerier);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,managedDownloader.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, managedDownloader.getRemainingStateTime());
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
    }
    
    public void testDHTTurnsOnStartsAutoIfInited() throws Exception {
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        // with dht off, send a query
        dhtManager.on = false;
        RequeryManager rm = createRM();
        
        rm.activate();
        rm.sendQuery();
        assertSame(managedDownloader, downloadManager.requerier);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,managedDownloader.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, managedDownloader.getRemainingStateTime());
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // query fails, dht still off
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        rm.sendQuery();
        assertNull(altLocFinder.listener);
        
        // turn the dht on should immediately query
        // even though the gnet query happened recently
        dhtManager.on = true;
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        // and we should start a lookup
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener);
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // make sure after that lookup finishes, we still can't do gnet 
        rm.handleAltLocSearchDone(false);
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // some time passes, can send one more dht query
        altLocFinder.listener = null;
        PrivilegedAccessor.setValue(rm, "lastQuerySent", 1);
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener);
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // more time passes, but we hit our dht query limit so we can't do anything.
        altLocFinder.listener = null;
        PrivilegedAccessor.setValue(rm, "lastQuerySent", 1);
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
    }
    
    public void testGnetFollowsDHT() throws Exception {
        RequeryManager rm = createRM();
        
        dhtManager.on = true;
        rm.activate();
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        
        // with dht on, start a requery
        rm.sendQuery();
        assertSame(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertNull(downloadManager.requerier);
        assertSame(rm, altLocFinder.listener);
        assertTrue(rm.isWaitingForResults());
        
        // pretend the dht lookup fails
        rm.handleAltLocSearchDone(false);
        assertFalse(rm.isWaitingForResults());
        assertSame(DownloadStatus.GAVE_UP, managedDownloader.getState());
        
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        
        // the next requery should be gnet
        altLocFinder.listener = null;
        rm.sendQuery();
        assertTrue(rm.isWaitingForResults());
        assertSame(managedDownloader, downloadManager.requerier);
        assertNull(altLocFinder.listener);
        
        // from now on we should give up & no more requeries
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
    }
    
    public void testGnetFollowsDHTPro() throws Exception {
        setPro(true);
        testGnetFollowsDHT();
    }
    
    public void testOnlyGnetPro() throws Exception {
        RequeryManager rm = createRM();
        
        dhtManager.on = true;
        setPro(true);
        
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener); // sent a DHT query
        assertEquals(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertTrue(rm.isWaitingForResults());
        rm.handleAltLocSearchDone(false); // finish it
        assertFalse(rm.isWaitingForResults());
        
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        rm.activate(); // now activate it.
        altLocFinder.listener = null;
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        rm.sendQuery();        
        assertTrue(rm.isWaitingForResults());
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, managedDownloader.getState());
        assertSame(managedDownloader, downloadManager.requerier);
        assertNull(altLocFinder.listener);
        
        assertFalse(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
    }
    
    public void testDHTTurnsOff() throws Exception {
        dhtManager.on = true;
        RequeryManager rm = createRM();
        setPro(true); // so we immediately launch a query
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener); // sent a DHT query
        assertEquals(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertTrue(rm.isWaitingForResults());
        
        // now turn the dht off
        dhtManager.on = false;
        rm.handleDHTEvent(new DHTEvent(new NullDHTController(), DHTEvent.Type.STOPPED));
        assertFalse(rm.isWaitingForResults());
        assertTrue(rm.canSendQueryAfterActivate());
        assertFalse(rm.canSendQueryNow());
        
        // turn the dht on again, and even though no time has passed
        // since the last query we can still do one
        dhtManager.on = true;
        
        altLocFinder.listener = null;
        assertTrue(rm.canSendQueryAfterActivate());
        assertTrue(rm.canSendQueryNow());
        rm.sendQuery();
        assertSame(rm, altLocFinder.listener); // sent a DHT query
        assertEquals(DownloadStatus.QUERYING_DHT, managedDownloader.getState());
        assertTrue(rm.isWaitingForResults());
    }
    private class MyDHTManager extends DHTManagerStub {

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
    
    private class MyAltLocFinder extends AltLocFinder {
        private volatile AltLocSearchListener listener;
        
        volatile boolean cancelled;
        public MyAltLocFinder() {
            super(null, ProviderHacks.getAlternateLocationFactory(), ProviderHacks.getAltLocManager(), ProviderHacks.getPushEndpointFactory());
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
    
    private class MyDownloadManager extends DownloadManagerStub {

        private volatile ManagedDownloader requerier;
        
        @Override
        public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) {
            this.requerier = requerier;
            return true;
        }
    }
    
    
    private class MyManagedDownloader extends ManagedDownloader {

        private volatile DownloadStatus status;
        private volatile long stateTime;
                
        public MyManagedDownloader() throws SaveLocationException {
            super(new RemoteFileDesc[0], new IncompleteFileManager(), new GUID(), ProviderHacks.getDownloadManager());
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
