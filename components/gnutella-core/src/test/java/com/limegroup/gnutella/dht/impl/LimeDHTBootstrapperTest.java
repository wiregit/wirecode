package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import junit.framework.Test;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.NetworkSettings;
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
    
    public static void globalSetUp() throws Exception {
        NetworkSettings.TIMEOUT.setValue(500);
        MojitoDHT dht = MojitoFactory.createDHT();
        dhtContext = (Context)dht;
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
    
    public void testAddBootstrapHost() throws Exception{
        fillRoutingTable(dhtContext.getRouteTable(), 2);
        //should be bootstrapping from routing table
        bootstrapper.bootstrap();
        DHTFuture future = (DHTFuture)PrivilegedAccessor.getValue(bootstrapper, "pingFuture");
        Thread.sleep(300);
        assertTrue((Boolean)PrivilegedAccessor.getValue(bootstrapper, "fromRouteTable"));
        //now emulate reception of a DHT node from the Gnutella network
        bootstrapper.addBootstrapHost(BOOTSTRAP_DHT.getContactAddress());
        assertTrue("ping future should be cancelled", future.isCancelled());
        Thread.sleep(300);
        future = (DHTFuture)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
        assertFalse("Should not be waiting",bootstrapper.isWaitingForNodes());
        //should be bootstrapping
        assertTrue(dhtContext.isBootstrapping());
        //now try adding more hosts -- should keep them but not bootstrap
        for(int i = 0; i < 30; i++) {
            bootstrapper.addBootstrapHost(new InetSocketAddress("1.2.3.4.5", i));
        }
        assertFalse(bootstrapper.isWaitingForNodes());
        assertFalse(future.isCancelled());
        //finish bootstrap
        future.get();
        assertTrue(dhtContext.isBootstrapped());
    }
    
    public void testNotCancelBootstrap() throws Exception {
        //now try bootstrapping of hostlist and see if it cancels (it should not)
        bootstrapper.bootstrap();
        Thread.sleep(100);
        assertTrue("Should be waiting", bootstrapper.isWaitingForNodes());
        bootstrapper.addBootstrapHost(new InetSocketAddress("localhost",5000));
        Future future = (Future)PrivilegedAccessor.getValue(bootstrapper, "pingFuture");
        assertNotNull("Should be pinging", future);
        assertFalse((Boolean)PrivilegedAccessor.getValue(bootstrapper, "fromRouteTable"));
        Thread.sleep(100);
        //now add other host: it should not cancel the previous attempt
        bootstrapper.addBootstrapHost(new InetSocketAddress("localhost",6000));
        assertFalse(future.isCancelled());
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
        InetSocketAddress addr = (InetSocketAddress) bootstrapper.getSimppHost();
        String address = addr.getHostName();
        int port = addr.getPort();
        assertEquals(address+":"+port, hosts[0]);
        
        //change first hex. Should point to last elem of the list
        id = KUID.createWithHexString("F3ED9650238A6C576C987793C01440A0EA91A1FB");
        localNode = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.UNKNOWN, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext.getRouteTable(), "localNode", localNode);
        addr = (InetSocketAddress) bootstrapper.getSimppHost();
        address = addr.getHostName();
        port = addr.getPort();
        assertEquals(address+":"+port, hosts[2]);
    }
}
