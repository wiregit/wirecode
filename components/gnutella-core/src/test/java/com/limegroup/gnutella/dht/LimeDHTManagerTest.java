package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.routing.PatriciaRouteTable;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.NetworkSettings;

public class LimeDHTManagerTest extends BaseTestCase {
    
    private static RouterService ROUTER_SERVICE;
    
    private static LimeDHTManager DHT_MANAGER;
    
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
        BOOTSTRAP_DHT = new MojitoDHT("bootstrapNode");
        InetSocketAddress addr = new InetSocketAddress("localhost", 3000);
        BOOTSTRAP_DHT.bind(addr);
        BOOTSTRAP_DHT.start();
    }
    
    public static void globalTearDown() throws Exception {
        BOOTSTRAP_DHT.stop();
    }
    
    protected void setUp() throws Exception {
        ROUTER_SERVICE =
            new RouterService(new ActivityCallbackStub());
        ROUTER_SERVICE.start();
        
        DHT_MANAGER = RouterService.getLimeDHTManager();

        setSettings();
    }

    private static void setSettings() throws Exception {
        ConnectionSettings.PORT.setValue(6346);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        DHTSettings.DHT_CAPABLE.setValue(true);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.EXCLUDE_ULTRAPEERS.setValue(true);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        DHTSettings.NEED_STABLE_GNUTELLA.setValue(false);
        DHTSettings.PERSIST_DHT.setValue(false);
    }
    
    public void testBootstrap() throws Exception {
        RouterService.initializeDHT(false);
        sleep(500);
        assertTrue(DHT_MANAGER.isRunning());
        assertTrue(DHT_MANAGER.isActiveNode());
        assertTrue(DHT_MANAGER.isWaiting());
        
        //add to the manager
        DHT_MANAGER.addBootstrapHost(BOOTSTRAP_DHT.getLocalAddress());
        assertFalse(DHT_MANAGER.isWaiting());
        sleep(100);
        assertGreaterThan(-1, CapabilitiesVM.instance().supportsDHT());
        assertFalse(DHT_MANAGER.isWaiting());
    }
    
    public void testAddBootstrapHost() throws Exception{
        NetworkSettings.MAX_ERRORS.setValue(1);
        NetworkSettings.MAX_TIMEOUT.setValue(300);
        RouterService.initializeDHT(false);
        sleep(300);
        //add invalid hosts
        DHT_MANAGER.addBootstrapHost(new InetSocketAddress("localhost",2000));
        assertFalse(DHT_MANAGER.isWaiting());
        for(int i = 1; i < 10; i++) {
            DHT_MANAGER.addBootstrapHost(new InetSocketAddress("0.0.0.0",i));
        }
        //now add valid: should be first in the list
        DHT_MANAGER.addBootstrapHost(BOOTSTRAP_DHT.getLocalAddress());
        List bootstrapHosts = (List) PrivilegedAccessor.getValue(DHT_MANAGER, "bootstrapHosts");
        assertEquals(10, bootstrapHosts.size());
        assertEquals(BOOTSTRAP_DHT.getLocalAddress(), bootstrapHosts.get(0));
        assertFalse(bootstrapHosts.contains(new InetSocketAddress("localhost",2000)));
        sleep(1000);
        //by now, should have failed previous unsuccessfull bootstrap and bootstrapped correctly
        bootstrapHosts = (List) PrivilegedAccessor.getValue(DHT_MANAGER, "bootstrapHosts");
        assertFalse(DHT_MANAGER.isWaiting());
    }
    
    public void tearDown() {
        //Ensure no more threads.
        RouterService.shutdown();
        sleep();
    }
    
    private void sleep() {
        sleep(300);
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }
}
