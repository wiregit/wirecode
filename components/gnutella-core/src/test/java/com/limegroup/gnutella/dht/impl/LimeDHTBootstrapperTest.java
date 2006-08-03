package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import junit.framework.Test;

import com.limegroup.gnutella.dht.DHTControllerStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.ContextSettings;

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
        MojitoDHT dht = MojitoFactory.createDHT();
        dhtContext = (Context)dht;
    }

    @Override
    protected void setUp() throws Exception {
        bootstrapper = new LimeDHTBootstrapper(new DHTControllerStub());
        dhtContext.bind(new InetSocketAddress("localhost", 2000));
        dhtContext.start();
    }

    @Override
    protected void tearDown() throws Exception {
        bootstrapper.stop();
        dhtContext.getRouteTable().clear();
        dhtContext.stop();
    }
    
    public void testCancelBootstrap() throws Exception{
        fillRoutingTable(2);
        //should be bootstrapping from routing table
        bootstrapper.bootstrap(dhtContext);
        Future future = (Future)PrivilegedAccessor.getValue(bootstrapper, "bootstrapFuture");
        Thread.sleep(300);
        assertFalse("Should not be waiting",bootstrapper.isWaitingForNodes());
        assertTrue("Should be bootstrapping from RT", bootstrapper.isBootstrappingFromRT());
        //now emulate reception of a DHT node from the Gnutella network
        bootstrapper.addBootstrapHost(BOOTSTRAP_DHT.getContactAddress());
        assertTrue(future.isCancelled());
        assertFalse("Should not be waiting",bootstrapper.isWaitingForNodes());
    }
    
    public void testNotCancelBootstrap() throws Exception {
        //now try bootstrapping of hostlist and see if it cancels (it should not)
        bootstrapper.bootstrap(dhtContext);
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
    
    private void fillRoutingTable(int numNodes) {
        RouteTable rt = dhtContext.getRouteTable();
        for(int i = 0; i < numNodes; i++) {
            KUID kuid = KUID.createRandomNodeID();
            ContactNode node = new ContactNode(
                    new InetSocketAddress("localhost",4000+i),
                    ContextSettings.VENDOR.getValue(),
                    ContextSettings.VERSION.getValue(),
                    kuid,
                    new InetSocketAddress("localhost",4000+i),
                    0,
                    false,
                    State.UNKNOWN);
            rt.add(node);
        }
    }
    
    //TODO: fix this
//    public void testGetSimppHosts() throws Exception{
//        LimeDHTBootstrapper bootstrapper = new LimeDHTBootstrapper(new DHTControllerStub());
//        //set up SIMPP hosts
//        String[] hosts = new String[] {
//                "86.25.22.3:84","1.0.0.0.3:300"};
//        
//        DHTSettings.DHT_BOOTSTRAP_HOSTS.setValue(hosts);
//        
//        MojitoDHT dht = MojitoFactory.createDHT();
//        PrivilegedAccessor.setValue(bootstrapper, "dht", dht);
//        
//        SocketAddress addr = bootstrapper.getSIMPPHost();
//        
//    }

}
