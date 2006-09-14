package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import junit.framework.Test;

import com.limegroup.gnutella.dht.DHTControllerStub;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.util.ArrayUtils;

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
        bootstrapper = new LimeDHTBootstrapper(new DHTControllerStub(dht));
        dhtContext = (Context)dht;
        dhtContext.bind(new InetSocketAddress("localhost", 2000));
        dhtContext.start();
    }

    @Override
    protected void tearDown() throws Exception {
        bootstrapper.stop();
        dhtContext.getRouteTable().clear();
        dhtContext.stop();
    }
    
    public void testAddBootstrapHost() throws Exception{
        System.out.println("Setting: "+ ConnectionSettings.LOCAL_IS_PRIVATE.getValue());
        fillRoutingTable(dhtContext.getRouteTable(), 2);
        //should be bootstrapping from routing table
        bootstrapper.bootstrap();
        DHTFuture future = (DHTFuture)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
        Thread.sleep(300);
        assertFalse("Should not be waiting",bootstrapper.isWaitingForNodes());
        assertTrue("Should be bootstrapping from RT", bootstrapper.isBootstrappingFromRT());
        //now emulate reception of a DHT node from the Gnutella network
        bootstrapper.addBootstrapHost(BOOTSTRAP_DHT.getContactAddress());
        assertTrue(future.isCancelled());
        future = (DHTFuture)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
        assertFalse("Should not be waiting",bootstrapper.isWaitingForNodes());
        Thread.sleep(3000);
        //should be bootstrapping
        assertTrue(dhtContext.isBootstrapping());
        //now try adding more hosts -- should keep them but not bootstrap
        for(int i = 0; i < 30; i++) {
            bootstrapper.addBootstrapHost(new InetSocketAddress("1.2.3.4.5", i));
        }
        assertFalse(bootstrapper.isWaitingForNodes());
        assertFalse(future.isCancelled());
    }
    
    public void testNotCancelBootstrap() throws Exception {
        //now try bootstrapping of hostlist and see if it cancels (it should not)
        bootstrapper.bootstrap();
        Thread.sleep(1000);
        assertTrue("Should be waiting", bootstrapper.isWaitingForNodes());
        bootstrapper.addBootstrapHost(new InetSocketAddress("localhost",5000));
        Future future = (Future)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
        assertFalse(bootstrapper.isWaitingForNodes());
        assertFalse(bootstrapper.isBootstrappingFromRT());
        Thread.sleep(1000);
        //now add other host: it should not cancel the previous attempt
        bootstrapper.addBootstrapHost(new InetSocketAddress("localhost",6000));
        future = (Future)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
        assertFalse(future.isCancelled());
    }
    
    public void testGetSimppHosts() throws Exception{
        //set up SIMPP hosts
        String[] hosts = new String[] {
                "1.0.0.0.3:100","2.0.0.0.3:200","3.0.0.0.3:300"};
        
        DHTSettings.DHT_BOOTSTRAP_HOSTS.setValue(hosts);
        
        //only first hex counts
        KUID id = KUID.create(ArrayUtils.parseHexString("03ED9650238A6C576C987793C01440A0EA91A1FB"));
        Contact localNode = ContactFactory.createLocalContact(0, 0, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext, "localNode", localNode);
        
        bootstrapper.bootstrap();
        InetSocketAddress addr = (InetSocketAddress) bootstrapper.getSIMPPHost();
        String address = addr.getHostName();
        int port = addr.getPort();
        assertEquals(address+":"+port, hosts[0]);
        
        //change first hex. Should point to last elem of the list
        id = KUID.create(ArrayUtils.parseHexString("F3ED9650238A6C576C987793C01440A0EA91A1FB"));
        localNode = ContactFactory.createLocalContact(0, 0, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext, "localNode", localNode);
        addr = (InetSocketAddress) bootstrapper.getSIMPPHost();
        address = addr.getHostName();
        port = addr.getPort();
        assertEquals(address+":"+port, hosts[2]);
    }
}
