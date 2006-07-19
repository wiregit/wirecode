package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import junit.framework.Test;

import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.ContextSettings;

public class AbstractDHTControllerTest extends DHTTestCase {

    public AbstractDHTControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AbstractDHTControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        setSettings();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static void testCancelBootstrap() throws Exception{
        PassiveDHTNodeController controller = new PassiveDHTNodeController();
        //first try bootstrapping directly from RT and emulate reception of host
        //from network
        fillRoutingTable(controller, 2);
        controller.start();
        assertFalse("Should not be waiting",controller.isWaiting());

        assertTrue("Should be bootstrapping from RT", controller.isBootstrappingFromRT());
        
        Future future = (Future)PrivilegedAccessor.getValue(controller, "bootstrapFuture");
        
        controller.addBootstrapHost(BOOTSTRAP_DHT.getSocketAddress());
        assertTrue(future.isCancelled());
        assertFalse("Should not be waiting",controller.isWaiting());
        
        controller.stop();
        clearRoutingTable(controller);

        //now try bootstrapping of hostlist and see if it cancels
        controller.start();
        Thread.sleep(1000);
        assertTrue("Should be waiting", controller.isWaiting());
        controller.addBootstrapHost(new InetSocketAddress("localhost",5000));
        future = (Future)PrivilegedAccessor.getValue(controller, "bootstrapFuture");
        assertFalse(controller.isWaiting());
        assertFalse(controller.isBootstrappingFromRT());
        Thread.sleep(1000);
        //now add other host: it should not cancel the previous attempt
        controller.addBootstrapHost(new InetSocketAddress("localhost",6000));
        assertFalse(future.isCancelled());
    }
    
    private static void fillRoutingTable(PassiveDHTNodeController controller, int numNodes) 
            throws Exception{
        RouteTable rt = (RouteTable)PrivilegedAccessor.getValue(controller, "limeDHTRouteTable");
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
    
    private static void clearRoutingTable(PassiveDHTNodeController controller) throws Exception{
        RouteTable rt = (RouteTable)PrivilegedAccessor.getValue(controller, "limeDHTRouteTable");
        rt.clear();
    }
}
