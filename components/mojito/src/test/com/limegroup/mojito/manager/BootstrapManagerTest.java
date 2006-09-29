package com.limegroup.mojito.manager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.NetworkSettings;

public class BootstrapManagerTest extends BaseTestCase {
    
    protected static MojitoDHT BOOTSTRAP_DHT;
    protected static final int BOOTSTRAP_DHT_PORT = 3000;

    protected static MojitoDHT TEST_DHT;
    
    public BootstrapManagerTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(BootstrapManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() {
        NetworkSettings.TIMEOUT.setValue(200);
        NetworkSettings.MIN_TIMEOUT_RTT.setValue(200);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }
    
    @Override
    protected void setUp() throws Exception {
        setSettings();
        
        //setup bootstrap node
        BOOTSTRAP_DHT = MojitoFactory.createDHT("bootstrapNode");
        BOOTSTRAP_DHT.bind(BOOTSTRAP_DHT_PORT);
        BOOTSTRAP_DHT.start();
        //setup test node
        TEST_DHT = MojitoFactory.createDHT("dht-test");
        TEST_DHT.bind(2000);
        TEST_DHT.start();
    }

    @Override
    protected void tearDown() throws Exception {
        BOOTSTRAP_DHT.stop();
        TEST_DHT.stop();
    }
    
    public void testBootstrapFailure() throws Exception{
        //try failure first
        BOOTSTRAP_DHT.stop();
        HashSet<SocketAddress> bootstrapSet = new LinkedHashSet<SocketAddress>();
        bootstrapSet.add(BOOTSTRAP_DHT.getContactAddress());
        BootstrapEvent evt = TEST_DHT.bootstrap(bootstrapSet).get();
        assertEquals(evt.getEventType(), BootstrapEvent.EventType.BOOTSTRAP_FAILED);
        assertEquals(BOOTSTRAP_DHT.getContactAddress(), evt.getFailedHosts().get(0));
    }

    public void testBootstrapFromList() throws Exception{
        //try pings to a bootstrap list
        //add some bad hosts
        HashSet<SocketAddress> bootstrapSet = new LinkedHashSet<SocketAddress>();
        bootstrapSet.clear();
        for(int i= 1; i < 5; i++) {
            bootstrapSet.add(new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT+i));
        }
        //add good host
        bootstrapSet.add(BOOTSTRAP_DHT.getContactAddress());
        BootstrapEvent evt = TEST_DHT.bootstrap(bootstrapSet).get();
        assertEquals(evt.getEventType(), BootstrapEvent.EventType.BOOTSTRAP_SUCCEEDED);
        assertNotContains(evt.getFailedHosts(), BOOTSTRAP_DHT.getContactAddress());
    }
    
    @SuppressWarnings("unchecked")
    public void testBootstrapFromRouteTable() throws Exception{
        //try ping from RT
        RouteTable rt = ((Context)TEST_DHT).getRouteTable();
        Contact node;
        for(int i= 1; i < 5; i++) {
            node = ContactFactory.createLiveContact(null,
                    0,0,KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i), 0, false);
            rt.add(node);
        }
        //add good node
        node = ContactFactory.createLiveContact(null,
                0,0,BOOTSTRAP_DHT.getLocalNodeID(),
                BOOTSTRAP_DHT.getContactAddress(), 0, false);
        rt.add(node);
        //now try bootstrapping from RT
        BootstrapEvent evt = TEST_DHT.bootstrap(Collections.EMPTY_SET).get();
        assertEquals(evt.getEventType(), BootstrapEvent.EventType.BOOTSTRAP_SUCCEEDED);
    }
    
    public void testBootstrapPoorRatio() throws Exception{
        //fill RT with bad nodes
        RouteTable rt = ((Context)TEST_DHT).getRouteTable();
        Contact node;
        for(int i= 1; i < 100; i++) {
            node = ContactFactory.createUnknownContact(0,0,KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(0,0,KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);

        BootstrapEvent evt = TEST_DHT.bootstrap(BOOTSTRAP_DHT.getContactAddress()).get();
        assertEquals(evt.getEventType(), BootstrapEvent.EventType.BOOTSTRAP_SUCCEEDED);
        //see if RT was purged
        assertNotContains(rt.getActiveContacts(), node);
    }

    public void testBootstrapFromInvalidHostList() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
        Set<SocketAddress> hostList = new HashSet<SocketAddress>();
        hostList.add(new InetSocketAddress(1));
        hostList.add(new InetSocketAddress("localhost", 2));
        hostList.add(new InetSocketAddress("localhost", 0));
        hostList.add(new InetSocketAddress("localhost", 5000));
        hostList.add(new InetSocketAddress("www.google.com", 0));
        
        MojitoDHT dht = MojitoFactory.createDHT();
        
        try {
            dht.bind(5000);
            dht.start();
            
            dht.bootstrap(hostList);
            fail(dht + " should have rejected the invalid hostList " + hostList);
        } catch (IllegalArgumentException ignore) {
            //ignore.printStackTrace();
        } finally {
            dht.stop();
        }
    }
}
