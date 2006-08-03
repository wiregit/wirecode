package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.settings.NetworkSettings;

public class LimeDHTManagerTest extends BaseTestCase {
    
    private static final int PORT = 6346;
    
    private static RouterService ROUTER_SERVICE;
    
    private static DHTManager DHT_MANAGER;
    
    private static MojitoDHT BOOTSTRAP_DHT;
    
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
        //setup bootstrap node
        BOOTSTRAP_DHT = MojitoFactory.createDHT("bootstrapNode");
        InetSocketAddress addr = new InetSocketAddress("localhost", 3000);
        BOOTSTRAP_DHT.bind(addr);
        BOOTSTRAP_DHT.start();
    }
    
    public static void globalTearDown() throws Exception {
        BOOTSTRAP_DHT.stop();
    }
    
    protected void setUp() throws Exception {
        setSettings();
        ROUTER_SERVICE =
            new RouterService(new ActivityCallbackStub());
        ROUTER_SERVICE.start();
        
        DHT_MANAGER = RouterService.getDHTManager();
    }

    private static void setSettings() throws Exception {
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        DHTSettings.ACTIVE_DHT_CAPABLE.setValue(true);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.EXCLUDE_ULTRAPEERS.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        DHTSettings.PERSIST_DHT.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
            FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {"127.*.*.*", "18.239.0.*"});
    }
    
    public void testBootstrap() throws Exception {
        RouterService.startDHT(true);
        DHTController controller = getController();
        sleep(1000);
        assertTrue(controller.isRunning());
        assertTrue(controller.isActiveNode());
        assertTrue(DHT_MANAGER.isWaitingForNodes());
        
        //add to the manager
        DHT_MANAGER.addDHTNode(BOOTSTRAP_DHT.getLocalAddress());
        assertFalse(controller.isWaitingForNodes());
        sleep(100);
        assertGreaterThan(-1, CapabilitiesVM.instance().supportsDHT());
    }
    
    public void testAddBootstrapHost() throws Exception{
        NetworkSettings.MAX_ERRORS.setValue(1);
        NetworkSettings.MAX_TIMEOUT.setValue(300);
        RouterService.startDHT(true);
        DHTController controller = getController();
        sleep(300);
        //add invalid hosts
        DHT_MANAGER.addDHTNode(new InetSocketAddress("localhost", 2000));
        assertFalse(controller.isWaitingForNodes());
        for(int i = 1; i < 10; i++) {
            DHT_MANAGER.addDHTNode(new InetSocketAddress("0.0.0.0", i));
        }
        //now add valid: should be first in the list
        DHT_MANAGER.addDHTNode(BOOTSTRAP_DHT.getLocalAddress());
        DHTBootstrapper bootstrapper = (DHTBootstrapper)PrivilegedAccessor.getValue(controller, "dhtBootstrapper");
        List bootstrapHosts = (List) PrivilegedAccessor.getValue(bootstrapper, "bootstrapHosts");
        assertEquals(11, bootstrapHosts.size());
        assertEquals(BOOTSTRAP_DHT.getLocalAddress(), bootstrapHosts.get(0));
        assertFalse(bootstrapHosts.contains(new InetSocketAddress("localhost",2000)));
        sleep(1000);
        //by now, should have failed previous unsuccessfull bootstrap and bootstrapped correctly
        bootstrapHosts = (List) PrivilegedAccessor.getValue(controller, "bootstrapHosts");
        assertFalse(controller.isWaitingForNodes());
//        assertTrue()
    }
    
    public void testSwitchPassiveActive() throws Exception{
        DHTSettings.PERSIST_DHT.setValue(true);
        DHT_MANAGER.start(true);
        DHTController controller = getController();
        assertTrue(DHT_MANAGER.isRunning());
        assertTrue(DHT_MANAGER.isActiveNode());
        KUID nodeId = controller.getMojitoDHT().getLocalNodeID();
        //try to bootstrap at the same time
        DHT_MANAGER.addDHTNode(BOOTSTRAP_DHT.getLocalAddress());
        DHT_MANAGER.switchMode(false);
        controller = getController();
        //we should have switched IDs
        assertTrue(DHT_MANAGER.isRunning());
        assertFalse(DHT_MANAGER.isActiveNode());
        KUID passiveNodeId = controller.getMojitoDHT().getLocalNodeID();
        assertNotEquals(nodeId, passiveNodeId);
        DHT_MANAGER.switchMode(false);
        //this should not change anything
        DHT_MANAGER.switchMode(false);
        controller = getController();
        assertEquals(passiveNodeId, controller.getMojitoDHT().getLocalNodeID());
        
        DHT_MANAGER.switchMode(true);
        controller = getController();
        //we should have the same ID again!
        assertEquals(nodeId, controller.getMojitoDHT().getLocalNodeID());
        DHT_MANAGER.switchMode(false);
        controller = getController();
        //and now it should be different from last passive time
        assertNotEquals(passiveNodeId, controller.getMojitoDHT().getLocalNodeID());
    }
    
    public void tearDown() throws Exception{
        //Ensure no more threads.
        RouterService.shutdown();
        sleep();
    }
    
    private void sleep() throws Exception{
        sleep(300);
    }
    
    private DHTController getController() throws Exception{
        return (DHTController) PrivilegedAccessor.getValue(DHT_MANAGER, "dhtController");
    }

    private void sleep(int milliseconds) throws Exception{
        Thread.sleep(milliseconds);
    }
}