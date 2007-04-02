package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import junit.framework.Test;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DefaultDHTValueFactory;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;

public class PassiveLeafTest extends DHTTestCase {
    
    public PassiveLeafTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveLeafTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testLookup() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        final int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        MojitoDHT passiveLeaf = null;
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        try {
            
            // Start some DHT Nodes
            for (int i = 0; i < 3*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(2000 + i);
                dht.start();
                
                if (i > 0) {
                    dht.bootstrap(dhts.get(i-1).getContactAddress()).get();
                }
                
                dhts.add(dht);
            }
            dhts.get(0).bootstrap(dhts.get(1).getContactAddress()).get();
            
            // Store a DHTValue
            KUID key = KUID.createRandomID();
            DHTValue value = DefaultDHTValueFactory.FACTORY.createDHTValue(
                    DHTValueType.BINARY, Version.ZERO, "Hello World".getBytes());
            StoreResult result = dhts.get(0).put(key, value).get();
            assertEquals(k, result.getNodes().size());
            
            // Create a passive leaf Node
            passiveLeaf = MojitoFactory.createDHT("PassiveLeaf");
            ((Context)passiveLeaf).setBootstrapped(true);
            ((Context)passiveLeaf).setBucketRefresherDisabled(true);
            RouteTable routeTable = new PassiveLeafRouteTable(Vendor.UNKNOWN, Version.ZERO);
            passiveLeaf.setRouteTable(routeTable);
            passiveLeaf.bind(4000);
            passiveLeaf.start();
            
            // Try to get the value which should fail
            try {
                FindValueResult r = passiveLeaf.get(key, DHTValueType.ANY).get();
                if (!r.getEntities().isEmpty()) {
                    fail("Should not have got DHTValue: " + r);
                }
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // Ping a Node which will add it to the passive leafs RouteTable
            assertEquals(1, routeTable.size());
            passiveLeaf.ping(dhts.get(0).getContactAddress()).get();
            assertEquals(2, routeTable.size());
            
            // Try again and it should work now
            try {
                FindValueResult r = passiveLeaf.get(key, DHTValueType.ANY).get();
                if (r.getEntities().isEmpty()) {
                    fail("Should have found DHTValue");
                }
            } catch (ExecutionException err) {
                fail(err);
            }
            
        } finally {
            close(dhts);
            
            if (passiveLeaf != null) {
                passiveLeaf.close();
            }
        }
    }
    
    public void testPassiveLeafController() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        PassiveLeafController controller = new PassiveLeafController(
                Vendor.UNKNOWN, Version.ZERO, new DHTEventDispatcherStub());
        try {
            controller.start();
            
            // Check initial state
            MojitoDHT dht = controller.getMojitoDHT();
            assertTrue(dht.isBootstrapped());
            assertTrue(dht.isFirewalled());
            
            RouteTable routeTable = dht.getRouteTable();
            assertEquals(1, routeTable.size());
            assertTrue(routeTable instanceof PassiveLeafRouteTable);
            
            // Add a Contact
            Contact c = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 3000));
            controller.addContact(c);
            
            // And check the final state of the RouteTable
            assertEquals(2, routeTable.size());
        } finally {
            controller.stop();
        }
    }
    
    public void testPassiveLeafManager() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        DHTManager manager = new DHTManagerImpl(Executors.newSingleThreadExecutor());
        try {
            // Check initial state
            assertEquals(DHTMode.INACTIVE, manager.getDHTMode());
            
            // Start in passive mode
            manager.start(DHTMode.PASSIVE_LEAF);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE_LEAF, manager.getDHTMode());
            
            // Stop and it should be in inital state again
            manager.stop();
            Thread.sleep(250);
            assertEquals(DHTMode.INACTIVE, manager.getDHTMode());
            
            // Start again
            manager.start(DHTMode.PASSIVE_LEAF);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE_LEAF, manager.getDHTMode());
            
            // Add a Contact through handleDHTContactsMessage(Contact)
            Contact c = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 3000));
            DHTContactsMessage msg = new DHTContactsMessage(c);
            manager.handleDHTContactsMessage(msg);
            Thread.sleep(250);
            
            // Check the RouteTable
            MojitoDHT dht = manager.getMojitoDHT();
            RouteTable routeTable = dht.getRouteTable();
            assertEquals(2, routeTable.size());
        } finally {
            manager.stop();
        }
    }
}
