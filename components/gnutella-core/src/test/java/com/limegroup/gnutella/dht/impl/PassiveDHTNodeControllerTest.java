package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.routing.RouteTable;

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
        RouterService.clearHostCatcher();
        RouterService.connect();   
        
        assertEquals("unexpected port", PORT, 
                 ConnectionSettings.PORT.getValue());
    }
    
    public void tearDown() throws Exception {
        RouterService.disconnect();
        Thread.sleep(300);
    }
    
    public void testNodesPersistence() throws Exception{
        int numPersistedNodes = DHTSettings.NUM_PERSISTED_NODES.getValue();
        DHTSettings.PERSIST_DHT.setValue(true);
        //first delete any previous file
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
        dhtFile.delete();
        PassiveDHTNodeController controller = new PassiveDHTNodeController(0, 0);
        Context context = (Context) controller.getMojitoDHT();
        Contact localContact = context.getLocalNode();
        KUID nodeID = context.getLocalNodeID();
        RouteTable rt = context.getRouteTable();
        //fill the routing table a bit
        fillRoutingTable(rt, 2*numPersistedNodes);
        controller.stop();
        //now nodeID should have changed and we should have persisted SOME nodes
        controller = new PassiveDHTNodeController(0, 0);
        context = (Context) controller.getMojitoDHT();
        rt = context.getRouteTable();
        assertNotEquals(nodeID, context.getLocalNodeID());
        assertFalse(context.getRouteTable().select(nodeID).equals(localContact));
        assertEquals(numPersistedNodes, context.getRouteTable().getActiveContacts().size());
    }
    
    public void testAddRemoveLeafDHTNode() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        PassiveControllerTest controller = new PassiveControllerTest(0, 0);
        controller.start();
        Context context = (Context) controller.getMojitoDHT();
        PassiveDHTNodeRouteTable rt = (PassiveDHTNodeRouteTable) context.getRouteTable();
        MojitoDHT dht;
        InetSocketAddress addr;
        for(int i = 0; i < 20; i++) {
            dht = MojitoFactory.createDHT("Mojito"+i);
            addr = new InetSocketAddress(2000+i);
            dht.bind(addr);
            dht.start();
            DHT_LIST.add(dht);
            controller.addLeafDHTNode("localhost", addr.getPort());
            Thread.sleep(300);
        }
        assertTrue(rt.hasDHTLeaves());
        assertEquals(rt.getDHTLeaves().size(), rt.getActiveContacts().size() - 1); //minus local node
        //try removing nodes
        assertNotNull(controller.removeLeafDHTNode("localhost",2000));
        assertNotNull(controller.removeLeafDHTNode("localhost",2015));
        //see if removed from leaf set
        assertFalse(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2000)));
        assertFalse(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2015)));
        assertTrue(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2001)));
        //see if removed from the DHT RT
        assertEquals(19, rt.getActiveContacts().size());
        //getActive nodes should return leaf nodes
        List<IpPort> l = controller.getActiveDHTNodes(30);
        //and not local node
        Contact localNode = context.getLocalNode();
        addr = (InetSocketAddress) localNode.getContactAddress();
        boolean found = false;
        for(IpPort ipp: l) {
            assertFalse(ipp.getPort() == 2000);
            assertFalse(ipp.getPort() == 2015);
            assertFalse(ipp.getPort() == addr.getPort());
            found |= (ipp.getPort() == 2001);
        }
        assertTrue(found);
    }
    
    private class PassiveControllerTest extends PassiveDHTNodeController {
        
        public PassiveControllerTest(int vendor, int version) {
            super(vendor, version);
        }

        @Override
        protected synchronized void addLeafDHTNode(String host, int port) {
            super.addLeafDHTNode(host, port);
        }

        @Override
        protected synchronized SocketAddress removeLeafDHTNode(String host, int port) {
            return super.removeLeafDHTNode(host, port);
        }
    }
}
