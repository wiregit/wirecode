package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.MojitoUtils;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventDispatcherStub;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.EventDispatcher;

public class PassiveDHTNodeControllerTest extends DHTTestCase {
    
    private static final EventDispatcher<DHTEvent, DHTEventListener> dispatcherStub = 
        new DHTEventDispatcherStub();
    
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
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        assertEquals("unexpected port", PORT, 
                 ConnectionSettings.PORT.getValue());
    }
    
    public void tearDown() throws Exception {
        Thread.sleep(300);
    }
    
    public void testNodesPersistence() throws Exception{
        int numPersistedNodes = DHTSettings.NUM_PERSISTED_NODES.getValue();
        DHTSettings.PERSIST_DHT.setValue(true);
        //first delete any previous file
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
        dhtFile.delete();
        PassiveDHTNodeController controller = new PassiveDHTNodeController(Vendor.UNKNOWN, Version.UNKNOWN, dispatcherStub);
        try {
            Context context = (Context) controller.getMojitoDHT();
            Contact localContact = context.getLocalNode();
            KUID nodeID = context.getLocalNodeID();
            RouteTable rt = context.getRouteTable();
            rt.setContactPinger(new RouteTable.ContactPinger() {
                public DHTFuture<PingResult> ping(Contact node) {
                    return null;
                }
            });
            //fill the routing table a bit
            fillRoutingTable(rt, 2*numPersistedNodes);
            controller.start();
            Thread.sleep(5000);
            controller.stop();
            //now nodeID should have changed and we should have persisted SOME nodes
            controller = new PassiveDHTNodeController(Vendor.UNKNOWN, Version.UNKNOWN, dispatcherStub);
            context = (Context) controller.getMojitoDHT();
            rt = context.getRouteTable();
            assertNotEquals(nodeID, context.getLocalNodeID());
            assertFalse(rt.select(nodeID).equals(localContact));
            assertEquals(numPersistedNodes, context.getRouteTable().getActiveContacts().size());
        } finally {
            controller.stop();
        }
    }
    
    public void testAddRemoveLeafDHTNode() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        PassiveDHTNodeController controller = new PassiveDHTNodeController(Vendor.UNKNOWN, Version.UNKNOWN, dispatcherStub);
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        try {
            controller.start();
            Context context = (Context) controller.getMojitoDHT();
            PassiveDHTNodeRouteTable rt = (PassiveDHTNodeRouteTable) context.getRouteTable();
            MojitoDHT dht;
            for(int i = 0; i < 20; i++) {
                dht = MojitoFactory.createDHT("Mojito-"+i, Vendor.UNKNOWN, Version.UNKNOWN);
                int port = 2000+i;
                dht.bind(port);
                dht.start();
                MojitoUtils.bootstrap(dht, new InetSocketAddress("localhost",BOOTSTRAP_DHT_PORT));
                DHT_LIST.add(dht);
                Thread.sleep(300);
                controller.addLeafDHTNode("localhost", port);
                dhts.add(dht);
            }
            assertTrue(rt.hasDHTLeaves());
            assertEquals(rt.getDHTLeaves().size(), rt.getActiveContacts().size() - 2); //minus local node and bootstrap host
            //try removing nodes
            assertNotNull(controller.removeLeafDHTNode("localhost",2000));
            assertNotNull(controller.removeLeafDHTNode("localhost",2015));
            //see if removed from leaf set
            assertFalse(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2000)));
            assertFalse(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2015)));
            assertTrue(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2001)));
            Thread.sleep(500);
            //see if removed from the DHT RT
            assertEquals(20, rt.getActiveContacts().size());
            
            //getActive nodes should return leaf nodes when node is bootstrapped
            List<IpPort> l = controller.getActiveDHTNodes(30);
            //and not local node
            Contact localNode = context.getLocalNode();
            InetSocketAddress addr = (InetSocketAddress) localNode.getContactAddress();
            boolean found = false;
            for(IpPort ipp: l) {
                assertFalse(ipp.getPort() == 2000);
                assertFalse(ipp.getPort() == 2015);
                assertFalse(ipp.getPort() == addr.getPort());
                found |= (ipp.getPort() == 2001);
            }
            assertTrue(found);
        } finally {
            controller.stop();
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
}
