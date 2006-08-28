package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.ContextSettings;

public class ActiveDHTNodeControllerTest extends DHTTestCase {
    
    public ActiveDHTNodeControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActiveDHTNodeControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        setSettings();
    }

    @Override
    protected void tearDown() throws Exception {
    }
    
    public void testPersistence() throws Exception{
        DHTSettings.PERSIST_DHT.setValue(true);
        //first delete any previous file
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
        dhtFile.delete();
        //start the node controller
        ActiveDHTNodeController controller = new ActiveDHTNodeController(0, 0);
        Context context = (Context) controller.getMojitoDHT();
        KUID nodeID = context.getLocalNodeID();
        RouteTable rt = context.getRouteTable();
        //fill the routing table a bit
        fillRoutingTable(rt, 10);
        //add one more
        KUID kuid = KUID.createRandomNodeID();
        ContactNode node = new ContactNode(
                new InetSocketAddress("localhost",4010),
                ContextSettings.VENDOR.getValue(),
                ContextSettings.VERSION.getValue(),
                kuid,
                new InetSocketAddress("localhost",4010),
                0,
                false,
                State.UNKNOWN);
        rt.add(node);
        controller.stop();
        controller = new ActiveDHTNodeController(0, 0);
        context = (Context) controller.getMojitoDHT();
        rt = context.getRouteTable();
        //should have the same nodeID as before
        assertEquals(nodeID, context.getLocalNodeID());
        //should have persisted the routetable
        List<Contact> contacts = rt.getContacts();
        assertEquals(12, contacts.size()); //11 + localnode
        assertTrue(contacts.contains(node));
    }
    
    public void testGetActiveDHTNodes() throws Exception{
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        ActiveDHTNodeController controller = new ActiveDHTNodeController(0, 0);
        controller.start();
        //bootstrap active node
        controller.addActiveDHTNode(new InetSocketAddress("localhost",3000));
        Thread.sleep(1000);
        //ask for active nodes -- should return itself and the bootstrap node
        List<IpPort> l = controller.getActiveDHTNodes(10);
        assertEquals(2, l.size());
        assertEquals(RouterService.getPort(), l.get(0).getPort());
        assertEquals(3000, l.get(1).getPort());
    }

}
