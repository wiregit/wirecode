package com.limegroup.gnutella.downloader;


import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManagerStub;
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
    
    static MyDHTManager mdht;
    static MyManagedDownloader mmd;
    static MyDownloadManager mdm;
    static MyAltLocFinder malf;
    public void setUp() throws Exception {
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        RequeryManager.NO_DELAY = true;
        mdht = new MyDHTManager();
        mmd = new MyManagedDownloader();
        mdm = new MyDownloadManager();
        malf = new MyAltLocFinder();
        setPro(false);
    }
    
    static RequeryManager createRM () {
        return RequeryManager.getManager(mmd, mdm, malf, mdht);
    }
    
    static void setPro(boolean pro) throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", pro);
        assertEquals(pro, LimeWireUtils.isPro());
    }
    
    public void testSelection() throws Exception {
        setPro(false);
        assertInstanceof(BasicRequeryManager.class, createRM());
        setPro(true);
        assertInstanceof(ProRequeryManager.class, createRM());
    }
    
    public void testRegistersWithDHTManager() throws Exception {
        assertNull(mdht.listener);
        RequeryManager rm = createRM();
        assertSame(rm, mdht.listener);
        rm.cleanUp();
        assertNull(mdht.listener);
    }
    
    public void testNotInitedDoesNothingBasic() throws Exception {
        RequeryManager rm = createRM();
        // shouldn't trigger any changes in download.complete
        assertFalse(rm.shouldGiveUp());
        assertFalse(rm.shouldSendRequeryImmediately());
        
        // shouldn't trigger any queries
        mdht.on = true;
        rm.sendRequery();
        assertNull(malf.listener);
        assertNull(mdm.requirier);
        assertNull(mmd.getState());
        rm.handleGaveUpState();
        assertNull(malf.listener);
        assertNull(mmd.getState());
    }
    
    public void testNotInitedAutoDHTPro() throws Exception {
        setPro(true);
        RequeryManager rm = createRM();
        // shouldn't trigger any changes in download.complete
        assertFalse(rm.shouldGiveUp());
        assertFalse(rm.shouldSendRequeryImmediately());
        
        // if dht is off do nothing
        mdht.on = false;
        rm.handleGaveUpState();
        assertNull(malf.listener);
        assertNull(mmd.getState());
        
        // if dht is on start querying
        mdht.on = true;
        rm.handleGaveUpState();
        assertSame(rm, malf.listener);
        assertSame(DownloadStatus.QUERYING_DHT, mmd.getState());
    }
    
    /**
     * tests that only a single gnet query is sent if dht is off. 
     */
    public void testOnlyGnetIfNoDHT() throws Exception {
        mdht.on = false;
        assertNull(malf.listener);
        assertNull(mdm.requirier);
        assertNull(mmd.getState());
        RequeryManager rm = createRM();
        rm.init();
        assertFalse(rm.shouldGiveUp());
        assertTrue(rm.shouldSendRequeryImmediately());
        
        // first try a requery that will not work
        rm.sendRequery();
        assertNull(malf.listener); // should not try dht
        assertSame(mmd, mdm.requirier); // should have tried gnet
        assertNull(mmd.getState()); // but not succeeded
        assertFalse(rm.shouldGiveUp()); // can still try to send something
        assertTrue(rm.shouldSendRequeryImmediately());
        
        // now a query that will work
        mdm.queryWorked = true;
        mdm.requirier = null;
        rm.sendRequery();
        assertNull(malf.listener); // should not try dht
        assertSame(mmd, mdm.requirier); // should have tried gnet
        // and worked
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,mmd.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, mmd.getRemainingStateTime());
        assertTrue(rm.shouldGiveUp());
        assertFalse(rm.shouldSendRequeryImmediately());
        
        // but if we try again, nothing happens.
        mdm.requirier = null;
        mmd.setState(null);
        rm.sendRequery();
        assertNull(mdm.requirier);
        assertNull(mmd.getState());
        assertTrue(rm.shouldGiveUp());
        assertFalse(rm.shouldSendRequeryImmediately());
    }
    
    public void testWaitsForStableConns() throws Exception {
        // no DHT nor connections
        mdht.on = false;
        mdm.queryWorked = true;
        RequeryManager.NO_DELAY = false;
        RequeryManager rm = createRM();
        rm.init();
        rm.sendRequery();
        assertNull(mdm.requirier);
        assertSame(DownloadStatus.WAITING_FOR_CONNECTIONS, mmd.getState());
        assertFalse(rm.shouldGiveUp());
        assertTrue(rm.shouldSendRequeryImmediately());
        
        // now we get connected
        RequeryManager.NO_DELAY = true;
        rm.sendRequery();
        // should be sent.
        assertSame(mmd, mdm.requirier);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,mmd.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, mmd.getRemainingStateTime());
        assertTrue(rm.shouldGiveUp());
        assertFalse(rm.shouldSendRequeryImmediately());
    }
    
    public void testDHTTurnsOnStartsAutoIfInited() throws Exception {
        // with dht off, send a query
        mdht.on = false;
        mdm.queryWorked = true;
        RequeryManager rm = createRM();
        rm.init();
        rm.sendRequery();
        assertSame(mmd, mdm.requirier);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,mmd.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, mmd.getRemainingStateTime());
        assertTrue(rm.shouldGiveUp());
        assertFalse(rm.shouldSendRequeryImmediately());
        
        // query fails, dht still off
        rm.handleGaveUpState();
        assertNull(malf.listener);
        // turn the dht on
        mdht.on = true;
        // and we should start a lookup
        rm.handleGaveUpState();
        assertSame(rm, malf.listener);
    }
    
    private class MyDHTManager extends DHTManagerStub {

        volatile DHTEventListener listener;
        volatile boolean on;
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

        public MyAltLocFinder() {
            super(null);
        }
        
        volatile URN urn;
        volatile AltLocSearchListener listener;
        @Override
        public boolean findAltLocs(URN urn, AltLocSearchListener listener) {
            this.urn = urn;
            this.listener = listener;
            return true;
        }

        @Override
        public boolean findPushAltLocs(GUID guid, URN urn) {
            return true;
        }
        
    }
    
    private class MyDownloadManager extends DownloadManagerStub {

        volatile ManagedDownloader requirier;

        volatile boolean queryWorked;
        @Override
        public boolean sendQuery(ManagedDownloader requerier, QueryRequest query) {
            this.requirier = requerier;
            return queryWorked;
        }
    }
    
    
    private class MyManagedDownloader extends ManagedDownloader {

        volatile DownloadStatus status;
        volatile long stateTime;
        volatile int numGnet;
        public MyManagedDownloader() throws SaveLocationException {
            super(new RemoteFileDesc[0], new IncompleteFileManager(), new GUID());
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
            this.numGnet = numGnutellaQueries;
            return null;
        }
    }
}
