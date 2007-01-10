package org.limewire.mojito.manager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.NetworkSettings;

public class BootstrapManagerTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
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
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setSettings();
        setLocalIsPrivate(false);
        
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
        BOOTSTRAP_DHT.close();
        TEST_DHT.close();
    }
    
    public void testBootstrapFailure() throws Exception {
        ((Context)BOOTSTRAP_DHT).setExternalAddress(
                new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT));
        
        // try failure first
        BOOTSTRAP_DHT.stop();
        
        try {
            ((Context)TEST_DHT).ping(BOOTSTRAP_DHT.getLocalNode()).get();
            fail("BOOTSTRAP_DHT should not respond");
        } catch (ExecutionException err) {
            assertTrue(err.getCause() instanceof DHTTimeoutException);
        }
        
        BootstrapResult result = TEST_DHT.bootstrap(BOOTSTRAP_DHT.getLocalNode()).get();
        assertEquals(BootstrapResult.ResultType.BOOTSTRAP_FAILED, result.getResultType());
        assertEquals(BOOTSTRAP_DHT.getLocalNodeID(), result.getContact().getNodeID());
        
        BOOTSTRAP_DHT.close();
    }

    public void testBootstrapFromList() throws Exception{
        //try pings to a bootstrap list
        //add some bad hosts
        Set<SocketAddress> bootstrapSet = new LinkedHashSet<SocketAddress>();
        bootstrapSet.clear();
        for(int i= 1; i < 5; i++) {
            bootstrapSet.add(new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT+i));
        }
        //add good host
        bootstrapSet.add(BOOTSTRAP_DHT.getContactAddress());
        
        PingResult pong = ((Context)TEST_DHT).ping(bootstrapSet).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
        assertEquals(BOOTSTRAP_DHT.getLocalNodeID(), result.getContact().getNodeID());
    }
    
    @SuppressWarnings("unchecked")
    public void testBootstrapFromRouteTable() throws Exception{
        //try ping from RT
        RouteTable rt = ((Context)TEST_DHT).getRouteTable();
        Contact node;
        for(int i= 1; i < 5; i++) {
            node = ContactFactory.createLiveContact(null,
                    0,0,KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i), 0, Contact.DEFAULT_FLAG);
            rt.add(node);
        }
        //add good node
        node = ContactFactory.createLiveContact(null,
                0,0,BOOTSTRAP_DHT.getLocalNodeID(),
                BOOTSTRAP_DHT.getContactAddress(), 0, Contact.DEFAULT_FLAG);
        rt.add(node);
        
        // Start pinging Nodes from the RouteTable
        PingResult pong = TEST_DHT.findActiveContact().get();
        // And bootstrap from the first Node that responds
        BootstrapResult evt = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(evt.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
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

        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
        //see if RT was purged
        assertNotContains(rt.getActiveContacts(), node);
    }
}
