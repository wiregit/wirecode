package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;

import junit.framework.Test;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.dht.DHTControllerStub;
import com.limegroup.gnutella.dht.DHTEventDispatcherStub;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.settings.DHTSettings;

public class LimeDHTBootstrapperTest extends DHTTestCase {
    
    private static Context dhtContext;
    
    private LimeDHTBootstrapper bootstrapper;
    
    public LimeDHTBootstrapperTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeDHTBootstrapperTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        setSettings();
        MojitoDHT dht = MojitoFactory.createDHT();
        bootstrapper = new LimeDHTBootstrapper(new DHTControllerStub(dht), 
                new DHTEventDispatcherStub());
        dhtContext = (Context)dht;
        dhtContext.bind(new InetSocketAddress("localhost", 2000));
        dhtContext.start();
    }

    @Override
    protected void tearDown() throws Exception {
        bootstrapper.stop();
        dhtContext.getRouteTable().clear();
        dhtContext.close();
    }
    
    public void testCancelIfUsingRouteTable() throws Exception {
        fillRoutingTable(dhtContext.getRouteTable(), 2);
        
        // Should be bootstrapping from the RouteTable
        DHTFuture<PingResult> pingFuture = null;
        synchronized (bootstrapper) {
            bootstrapper.bootstrap();
            
            pingFuture = (DHTFuture<PingResult>)PrivilegedAccessor.getValue(bootstrapper, "pingFuture");
            assertNotNull(pingFuture);
            
            // Should use the RouteTable
            assertTrue(((Boolean)PrivilegedAccessor.getValue(bootstrapper, "fromRouteTable")).booleanValue());
            
            // Now emulate reception of a DHT node from the Gnutella network
            // It should cancel the existing pingFuture and create a new 
            // Future for the Contact we've just added
            bootstrapper.addBootstrapHost(BOOTSTRAP_DHT.getContactAddress());
            assertTrue("Ping future should be cancelled", pingFuture.isCancelled());
            
            // Get a handle of the new DHTFuture
            pingFuture = (DHTFuture<PingResult>)PrivilegedAccessor.getValue(bootstrapper, "pingFuture");
            assertNotNull(pingFuture);
            assertFalse("Ping future should NOT be cancelled", pingFuture.isCancelled());
        }
    }
    
    public void testDontCancelIfUsingHostList() throws Exception {
        // Try bootstrapping of hostlist and see if it cancels (it should not)
        DHTFuture<PingResult> pingFuture = null;
        synchronized (bootstrapper) {
            bootstrapper.bootstrap();
            assertTrue("Should be waiting", bootstrapper.isWaitingForNodes());
            
            bootstrapper.addBootstrapHost(new InetSocketAddress("localhost", 5000));
            
            pingFuture = (DHTFuture<PingResult>)PrivilegedAccessor.getValue(bootstrapper, "pingFuture");
            assertNotNull("Should be pinging", pingFuture);
            
            // Shouldn't use the RouteTable
            assertFalse(((Boolean)PrivilegedAccessor.getValue(bootstrapper, "fromRouteTable")).booleanValue());
            
            // Now emulate reception of a DHT node from the Gnutella network
            // It should NOT cancel the existing pingFuture 
            bootstrapper.addBootstrapHost(new InetSocketAddress("localhost", 6000));
            assertFalse("Ping future should NOT be cancelled", pingFuture.isCancelled());
        }
    }
    
    public void testDontCancelIfBootstrapping() throws Exception {
        Contact node = dhtContext.ping(
                BOOTSTRAP_DHT.getContactAddress()).get().getContact();
        
        DHTFuture<BootstrapResult> bootstrapFuture = null;
        synchronized (bootstrapper) {
            // Invoke bootstrap manually
            PrivilegedAccessor.invokeMethod(bootstrapper, "bootstrapFromContact", 
                    new Contact[]{ node }, new Class[]{Contact.class});
            
            // Get the handle of the bootstrapFuture
            bootstrapFuture = (DHTFuture<BootstrapResult>)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
            assertNotNull(bootstrapFuture);
            assertFalse("Should not be waiting", bootstrapper.isWaitingForNodes());
            
            // We should be bootstrapping
            assertTrue(dhtContext.isBootstrapping());
            
            // Now try adding more hosts
            for(int i = 0; i < 30; i++) {
                bootstrapper.addBootstrapHost(new InetSocketAddress("1.2.3.4.5", i));
            }
            
            // The bootstrapFuture should keep going
            assertFalse(bootstrapper.isWaitingForNodes());
            assertFalse(bootstrapFuture.isCancelled());
            
            // Finish bootstrap
            bootstrapFuture.get();
            
            // And we should be bootstrapped
            assertTrue(dhtContext.isBootstrapped());
        }
    }
    
    public void testGetSimppHosts() throws Exception{
        //set up SIMPP hosts
        String[] hosts = new String[] {
                "1.0.0.0.3:100","2.0.0.0.3:200","3.0.0.0.3:300"};
        
        DHTSettings.DHT_BOOTSTRAP_HOSTS.setValue(hosts);
        
        //only first hex counts
        KUID id = KUID.createWithHexString("03ED9650238A6C576C987793C01440A0EA91A1FB");
        Contact localNode = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.UNKNOWN, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext.getRouteTable(), "localNode", localNode);
        
        bootstrapper.bootstrap();
        InetSocketAddress addr = (InetSocketAddress)PrivilegedAccessor.invokeMethod(bootstrapper, "getSimppHost", new Object[0]);
        String address = addr.getHostName();
        int port = addr.getPort();
        assertEquals(address+":"+port, hosts[0]);
        
        //change first hex. Should point to last elem of the list
        id = KUID.createWithHexString("F3ED9650238A6C576C987793C01440A0EA91A1FB");
        localNode = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.UNKNOWN, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext.getRouteTable(), "localNode", localNode);
        addr = (InetSocketAddress)PrivilegedAccessor.invokeMethod(bootstrapper, "getSimppHost", new Object[0]);
        address = addr.getHostName();
        port = addr.getPort();
        assertEquals(address+":"+port, hosts[2]);
    }
    
    public void testListenToSimppHosts() throws Exception {
        bootstrapper.bootstrap();
        Thread.sleep(200);
        assertTrue(bootstrapper.isWaitingForNodes());
        //add bootstrap host post- bootstrap()
        DHTSettings.DHT_BOOTSTRAP_HOSTS.setValue(new String[] {"127.0.0.1:"+BOOTSTRAP_DHT_PORT});
        //this should trigger bootstrapping
        bootstrapper.simppUpdated(0);
        Thread.sleep(2000);
        assertFalse(bootstrapper.isWaitingForNodes());
        assertTrue(dhtContext.isBootstrapped());
    }
}
