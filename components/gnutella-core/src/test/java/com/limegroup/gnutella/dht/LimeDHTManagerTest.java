package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.impl.LimeDHTManager;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.settings.NetworkSettings;

public class LimeDHTManagerTest extends BaseTestCase {
    
    private static RouterService ROUTER_SERVICE;
    
    private static LimeDHTManager DHT_MANAGER;
    
    private static MojitoDHT BOOTSTRAP_DHT;
    
    private static int PORT = 6346;

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
        BOOTSTRAP_DHT = new MojitoDHT("bootstrapNode");
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
        
        DHT_MANAGER = RouterService.getLimeDHTManager();
        
    }

    private static void setSettings() throws Exception {
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        DHTSettings.DHT_CAPABLE.setValue(true);
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
        sleep(500);
        assertTrue(controller.isRunning());
        assertTrue(controller.isActiveNode());
        assertTrue(controller.isWaiting());
        
        //add to the manager
        DHT_MANAGER.addBootstrapHost(BOOTSTRAP_DHT.getLocalAddress());
        assertFalse(controller.isWaiting());
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
        DHT_MANAGER.addBootstrapHost(new InetSocketAddress("localhost",2000));
        assertFalse(controller.isWaiting());
        for(int i = 1; i < 10; i++) {
            DHT_MANAGER.addBootstrapHost(new InetSocketAddress("0.0.0.0",i));
        }
        //now add valid: should be first in the list
        DHT_MANAGER.addBootstrapHost(BOOTSTRAP_DHT.getLocalAddress());
        List bootstrapHosts = (List) PrivilegedAccessor.getValue(controller, "bootstrapHosts");
        assertEquals(10, bootstrapHosts.size());
        assertEquals(BOOTSTRAP_DHT.getLocalAddress(), bootstrapHosts.get(0));
        assertFalse(bootstrapHosts.contains(new InetSocketAddress("localhost",2000)));
        sleep(1000);
        //by now, should have failed previous unsuccessfull bootstrap and bootstrapped correctly
        bootstrapHosts = (List) PrivilegedAccessor.getValue(controller, "bootstrapHosts");
        assertFalse(controller.isWaiting());
//        assertTrue()
    }
    
    public void testLeafDHTNode() throws Exception{
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);

        RouterService.startDHT(true);
        //get ready to accept connections
        RouterService.clearHostCatcher();
        RouterService.connect();  
        //create a leaf and connect
        Connection leaf = new Connection("localhost", PORT);
        leaf.initialize(new LeafHeaders("localhost"), new EmptyResponder());
        CapabilitiesVM.reconstructInstance();
        leaf.send(CapabilitiesVM.instance());
        //we should have received this leaf and bootstrapped off of it
        sleep(3000);
        //TODO: incomplete
        leaf.close();
    }
    
    public void testSwitchPassiveActive() throws Exception{
        DHTSettings.PERSIST_DHT.setValue(true);
        DHT_MANAGER.startDHT(true);
        DHTController controller = getController();
        assertTrue(DHT_MANAGER.isRunning());
        assertTrue(DHT_MANAGER.isActiveNode());
        KUID nodeId = controller.getMojitoDHT().getLocalNodeID();
        //try to bootstrap at the same time
        DHT_MANAGER.addBootstrapHost(BOOTSTRAP_DHT.getLocalAddress());
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
    
    public void tearDown() {
        //Ensure no more threads.
        RouterService.shutdown();
        sleep();
    }
    
    private void sleep() {
        sleep(300);
    }
    
    private DHTController getController() throws Exception{
        return (DHTController) PrivilegedAccessor.getValue(DHT_MANAGER, "dhtController");
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }
}
