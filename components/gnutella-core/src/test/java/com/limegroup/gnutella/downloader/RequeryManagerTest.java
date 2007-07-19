package com.limegroup.gnutella.downloader;


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
import com.limegroup.gnutella.util.LimeTestCase;

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
        RequeryManager.NO_DELAY = true;
        mdht = new MyDHTManager();
        mmd = new MyManagedDownloader();
        mdm = new MyDownloadManager();
        malf = new MyAltLocFinder();
    }
    
    RequeryManager createRM () {
        return new RequeryManager(mmd, mdm, malf, mdht);
    }
    
    public void testRegistersWithDHTManager() throws Exception {
        assertNull(mdht.listener);
        RequeryManager rm = createRM();
        assertSame(rm, mdht.listener);
        rm.cleanUp();
        assertNull(mdht.listener);
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
        
        // first try a requery that will not work
        rm.sendRequery();
        assertNull(malf.listener); // should not try dht
        assertSame(mmd, mdm.requirier); // should have tried gnet
        assertNull(mmd.getState()); // but not succeeded
        
        // now a query that will work
        mdm.queryWorked = true;
        mdm.requirier = null;
        rm.sendRequery();
        assertNull(malf.listener); // should not try dht
        assertSame(mmd, mdm.requirier); // should have tried gnet
        // and worked
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS,mmd.getState());
        assertEquals(RequeryManager.TIME_BETWEEN_REQUERIES, mmd.getRemainingStateTime());
        
        // but if we try again, nothing happens.
        mdm.requirier = null;
        mmd.setState(null);
        rm.sendRequery();
        assertNull(mdm.requirier);
        assertNull(mmd.getState());
    }
    
    public void testWaitsForStableConns() throws Exception {
        // no DHT nor connections
        mdht.on = false;
        RequeryManager.NO_DELAY = false;
        RequeryManager rm = createRM();
        rm.sendRequery();
        assertNull(mdm.requirier);
        assertSame(DownloadStatus.WAITING_FOR_CONNECTIONS, mmd.getState());
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
