package com.limegroup.gnutella.dht;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;

import com.limegroup.gnutella.settings.ConnectionSettings;

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
                    DHTValueType.BINARY, Version.UNKNOWN, "Hello World".getBytes());
            StoreResult result = dhts.get(0).put(key, value).get();
            assertEquals(k, result.getNodes().size());
            
            // Create a passive leaf Node
            passiveLeaf = MojitoFactory.createDHT("PassiveLeaf");
            ((Context)passiveLeaf).setBootstrapped(true);
            ((Context)passiveLeaf).setBucketRefresherDisabled(true);
            RouteTable routeTable = new PassiveLeafRouteTable(Vendor.UNKNOWN, Version.UNKNOWN);
            passiveLeaf.setRouteTable(routeTable);
            passiveLeaf.bind(4000);
            passiveLeaf.start();
            
            // Try to get the value which should fail
            try {
                FindValueResult r = passiveLeaf.get(key).get();
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
                FindValueResult r = passiveLeaf.get(key).get();
                if (r.getEntities().isEmpty()) {
                    fail("Should have found DHTValue");
                }
            } catch (ExecutionException err) {
                fail(err);
            }
            
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
            
            if (passiveLeaf != null) {
                passiveLeaf.close();
            }
        }
    }
}
