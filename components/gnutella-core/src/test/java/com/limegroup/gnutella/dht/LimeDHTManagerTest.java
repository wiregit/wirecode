package com.limegroup.gnutella.dht;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.impl.LimeDHTManager;
import com.limegroup.gnutella.settings.DHTSettings;
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
    
    public static void globalSetUp() throws Exception {
    }
    
    public static void globalTearDown() throws Exception {
    }
    
    protected void setUp() throws Exception {
    }

    public void tearDown() throws Exception{
        //Ensure no more threads.
        RouterService.shutdown();
    }
    
    public void testLimeDHTManager() throws Exception{
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        DHTManager manager = new LimeDHTManager();
        assertFalse(manager.isRunning());
        assertFalse(manager.isBootstrapped());
        assertFalse(manager.isWaitingForNodes());
        assertEquals(0, manager.getActiveDHTNodes(10).size());
        
        manager.start(true);
        assertTrue(manager.isRunning());
        assertTrue(manager.isActiveNode());
        KUID activeLocalNodeID = manager.getMojitoDHT().getLocalNodeID();
        //try starting again
        manager.start(true);
        assertEquals(activeLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        //try switching mode
        manager.start(false);
        assertFalse(manager.isActiveNode());
        assertTrue(manager.isRunning());
        KUID passiveLocalNodeID = manager.getMojitoDHT().getLocalNodeID();
        assertNotEquals(activeLocalNodeID, passiveLocalNodeID);
        manager.start(false);
        assertEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        manager.addressChanged();
        assertEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        //try switching multiple times
        manager.start(true);
        manager.start(false);
        manager.start(true);
        manager.start(false);
        manager.start(true);
        assertEquals(activeLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        assertTrue(manager.isActiveNode());
    }
    
}