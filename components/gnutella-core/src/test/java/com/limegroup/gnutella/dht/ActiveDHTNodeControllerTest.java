package com.limegroup.gnutella.dht;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.EventDispatcher;

public class ActiveDHTNodeControllerTest extends DHTTestCase {
    
    private static final EventDispatcher<DHTEvent, DHTEventListener> dispatcherStub = 
        new DHTEventDispatcherStub();
    
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
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
    }

    @Override
    protected void tearDown() throws Exception {
    }
    
    public void testPersistence() throws Exception{
        DHTSettings.PERSIST_DHT.setValue(true);
        //first delete any previous file
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
        dhtFile.delete();
        //start the node controller
        ActiveDHTNodeController controller = new ActiveDHTNodeController(Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
        
        try {
            Context context = (Context) controller.getMojitoDHT();
            KUID nodeID = context.getLocalNodeID();
            RouteTable rt = context.getRouteTable();
            //fill the routing table a bit
            fillRoutingTable(rt, 10);
            
            //add one more
            KUID kuid = KUID.createRandomID();
            RemoteContact node = new RemoteContact(
                    new InetSocketAddress("localhost",4010),
                    ContextSettings.getVendor(),
                    ContextSettings.getVersion(),
                    kuid,
                    new InetSocketAddress("localhost",4010),
                    0,
                    Contact.DEFAULT_FLAG,
                    State.UNKNOWN);
            rt.add(node);
            
            controller.start();
            controller.stop();
            
            controller = new ActiveDHTNodeController(Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
            context = (Context) controller.getMojitoDHT();
            rt = context.getRouteTable();
            //should have the same nodeID as before
            assertEquals(nodeID, context.getLocalNodeID());
            //should have persisted the routetable
            Collection<Contact> contacts = rt.getContacts();
            assertEquals(12, contacts.size()); //11 + localnode
            assertTrue(contacts.contains(node));
        } finally {
            controller.stop();
        }
    }
    
    public void testGetActiveDHTNodes() throws Exception{
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        ActiveDHTNodeController controller = new ActiveDHTNodeController(Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
        
        try {
            controller.start();
            //bootstrap active node
            controller.addActiveDHTNode(new InetSocketAddress("localhost",3000));
            Thread.sleep(2000);
            //ask for active nodes -- should return itself and the bootstrap node
            List<IpPort> l = controller.getActiveDHTNodes(10);
            assertEquals(2, l.size());
            assertEquals(RouterService.getPort(), l.get(0).getPort());
            assertEquals(3000, l.get(1).getPort());
        } finally {
            controller.stop();
        }
    }
}
