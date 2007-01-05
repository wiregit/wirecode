package com.limegroup.gnutella.dht;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.impl.LimeDHTManager;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.SimpleTimer;
import com.limegroup.mojito.KUID;

public class LimeDHTManagerTest extends DHTTestCase {
    
    
    public LimeDHTManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeDHTManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    protected void setUp() throws Exception {
        ROUTER_SERVICE = new RouterService(new ActivityCallbackStub());
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(false);
        ROUTER_SERVICE.start();
        //stop the nodeAssigner
        NodeAssigner na = 
            (NodeAssigner)PrivilegedAccessor.getValue(ROUTER_SERVICE, "nodeAssigner");
        na.stop();
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
    }

    public void tearDown() throws Exception{
        //Ensure no more threads.
        RouterService.shutdown();
    }
    
    public void testLimeDHTManager() throws Exception{
        TestThreadPool threadPool = new TestThreadPool();
        LimeDHTManager manager = new LimeDHTManager(threadPool);
        assertFalse(manager.isRunning());
        assertFalse(manager.isBootstrapped());
        assertFalse(manager.isWaitingForNodes());
        assertEquals(0, manager.getActiveDHTNodes(10).size());
        
        manager.start(true);
        assertEquals(1, threadPool.getRunners().size());
        Thread.sleep(200);
        assertTrue(manager.isRunning());
        assertTrue(manager.isActiveNode());
        KUID activeLocalNodeID = manager.getMojitoDHT().getLocalNodeID();
        //try starting again
        manager.start(true);
        Thread.sleep(200);
        assertEquals(activeLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        //try switching mode
        manager.start(false);
        Thread.sleep(200);
        assertFalse(manager.isActiveNode());
        assertTrue(manager.isRunning());
        KUID passiveLocalNodeID = manager.getMojitoDHT().getLocalNodeID();
        assertNotEquals(activeLocalNodeID, passiveLocalNodeID);
        manager.start(false);
        Thread.sleep(200);
        assertEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        manager.addressChanged();
        Thread.sleep(200);
        assertEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        //try switching multiple times
        manager.start(true);
        manager.start(false);
        manager.start(true);
        manager.start(false);
        manager.start(true);
        Thread.sleep(500);
        assertEquals(activeLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        assertTrue(manager.isActiveNode());
    }
    
    public void testStopStartLimeDHTManager() throws Exception{
        TestThreadPool threadPool = new TestThreadPool();
        LimeDHTManager manager = new LimeDHTManager(threadPool);
        manager.start(true);
        manager.stop();
        Thread.sleep(200);
        assertFalse(manager.isRunning());
        manager.start(true);
        Thread.sleep(200);
        assertTrue(manager.isRunning());
        assertTrue(manager.isActiveNode());
        manager.start(false);
        Thread.sleep(200);
        assertFalse(manager.isActiveNode());
        assertTrue(manager.isRunning());
        manager.start(true);
        manager.start(false);
        manager.start(true);
        manager.stop();
        assertFalse(manager.isRunning());
        Thread.sleep(500);
        assertFalse(manager.isRunning());
    }
    
    private class TestThreadPool extends SimpleTimer{
        
        private List<Runnable> runners = new ArrayList<Runnable>();
        
        public TestThreadPool() {
            super(true);
        };

        public void invokeLater(Runnable runner) {
            runners.add(runner);
            super.invokeLater(runner);
        }
        
        public List<Runnable> getRunners() {
            return runners;
        }
        
    }
    
}