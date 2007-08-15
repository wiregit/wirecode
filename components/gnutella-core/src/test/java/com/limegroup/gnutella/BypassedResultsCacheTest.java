package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class BypassedResultsCacheTest extends BaseTestCase {

    private QueryActiveActivityCallback callback = new QueryActiveActivityCallback();
    
    private GUIDActiveDownloadManager manager = new GUIDActiveDownloadManager();
    
    private GUESSEndpoint point1;
    
    private GUESSEndpoint point2;
    
    private GUESSEndpoint point3;
    
    public BypassedResultsCacheTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
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
    
    private static class GUIDActiveDownloadManager extends DownloadManagerStub {
        
        boolean isGUIDFor;
        
        @Override
        public synchronized boolean isGuidForQueryDownloading(GUID guid) {
            return isGUIDFor;
        }
    }
    
}
