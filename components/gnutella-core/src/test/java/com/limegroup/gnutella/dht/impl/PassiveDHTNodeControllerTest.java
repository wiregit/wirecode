package com.limegroup.gnutella.dht.impl;

import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.util.IpPort;

public class PassiveDHTNodeControllerTest extends DHTTestCase {
    
    public PassiveDHTNodeControllerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveDHTNodeControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        setSettings();
        ROUTER_SERVICE.start();
        ROUTER_SERVICE.clearHostCatcher();
        ROUTER_SERVICE.connect();   
        
        assertEquals("unexpected port", PORT, 
                 ConnectionSettings.PORT.getValue());
    }
    
    public void tearDown() throws Exception {
        ROUTER_SERVICE.disconnect();
        Thread.sleep(300);
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
        assertEquals(BOOTSTRAP_DHT_PORT, 
                nodes.get(0).getPort());
        controller.stop();
        controller = new PassiveDHTNodeController();
        controller.start();
        assertFalse(controller.isWaiting());
    }

}
