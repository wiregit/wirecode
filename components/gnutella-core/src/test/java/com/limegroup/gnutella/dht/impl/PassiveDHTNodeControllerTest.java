package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.impl.PassiveDHTNodeController;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;

public class PassiveDHTNodeControllerTest extends BaseTestCase {
    
    private static RouterService ROUTER_SERVICE;
    
    private static MojitoDHT BOOTSTRAP_DHT;

    public PassiveDHTNodeControllerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveDHTNodeControllerTest.class);
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
        
        //start router service
        ROUTER_SERVICE =
            new RouterService(new ActivityCallbackStub());
        ROUTER_SERVICE.start();
    }
    
    public static void globalTearDown() throws Exception {
        BOOTSTRAP_DHT.stop();
    }
    
    
    public void testNodesPersistence() throws Exception{
        PassiveDHTNodeController controller = new PassiveDHTNodeController();
        controller.start();
        Thread.sleep(300);
        assertTrue(controller.isWaiting());
        controller.addBootstrapHost(BOOTSTRAP_DHT.getSocketAddress());
        assertFalse(controller.isWaiting());
        Thread.sleep(300);
        controller.stop();
        //should have persisted the DHT
        controller = new PassiveDHTNodeController();
        controller.start();
        assertFalse(controller.isWaiting());
        Thread.sleep(300);
        List<IpPort> nodes = controller.getActiveDHTNodes(1);
        assertEquals(((InetSocketAddress) BOOTSTRAP_DHT.getSocketAddress()).getPort(), 
                nodes.get(0).getPort());
        controller.stop();
        controller = new PassiveDHTNodeController();
        controller.start();
        assertFalse(controller.isWaiting());
    }
    

}
