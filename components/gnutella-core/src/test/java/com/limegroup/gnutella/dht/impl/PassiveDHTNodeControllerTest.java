package com.limegroup.gnutella.dht.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
        DHTSettings.PERSIST_DHT.setValue(true);
        PassiveDHTNodeController controller = new PassiveDHTNodeController();
        controller.start();
        Thread.sleep(300);
//        assertTrue(controller.isWaiting());
        controller.addDHTNode(BOOTSTRAP_DHT.getContactAddress());
//        assertFalse(controller.isWaiting());
        Thread.sleep(300);
        controller.stop();
        //should have persisted the DHT
        controller = new PassiveDHTNodeController();
        controller.start();
//        assertFalse(controller.isWaiting());
        Thread.sleep(300);
        List<IpPort> nodes = controller.getActiveDHTNodes(1);
        assertEquals(BOOTSTRAP_DHT_PORT, 
                nodes.get(0).getPort());
        controller.stop();
        controller = new PassiveDHTNodeController();
        controller.start();
//        assertFalse(controller.isWaiting());
        controller.stop();
        //try a corrupt file
        File file = new File(CommonUtils.getUserSettingsDir(), "dhtnodes.dat");
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bos.write("mark".getBytes());
        bos.close();
        controller = new PassiveDHTNodeController();
        controller.start();
        Thread.sleep(500);
//        assertTrue(controller.isWaiting());
        //this should delete the corrupted file
        controller.stop();
        assertFalse(file.exists());
    }

}
