/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.mojito;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.routing.impl.LocalContact;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;


public class CacheForwardTest extends BaseTestCase {
    
    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }
    
    public CacheForwardTest(String name) {
        super(name);
    }
   
    public static TestSuite suite() {
        return buildTestSuite(CacheForwardTest.class);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    public void testCaseForward() throws Exception {
        
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.setValue(true);
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        int port = 3000;
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        MojitoDHT originator = null;
        try {
            for (int i = 0; i < 3*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(new InetSocketAddress("localhost", port + i));
                dht.start();
                
                if (i > 0) {
                    dht.bootstrap(new InetSocketAddress("localhost", port)).get();
                } else {
                    originator = dht;
                }
                dhts.put(dht.getLocalNodeID(), dht);
            }
            originator.bootstrap(new InetSocketAddress("localhost", port+1)).get();
            
            // Store the value
            KUID valueId = KUID.create("40229239B68FFA66575E59D0AB1F685AD3191960");
            byte[] value = "Hello World".getBytes();
            StoreEvent evt = originator.put(valueId, value).get();
            assertEquals(k, evt.getNodes().size());
            
            // And check the initial state
            Context closest = null;
            Context furthest = null;
            for (Contact remote : evt.getNodes()) {
                Context dht = (Context)dhts.get(remote.getNodeID());
                assertEquals(1, dht.getDatabase().getKeyCount());
                assertEquals(1, dht.getDatabase().getValueCount());
                for (DHTValue dhtValue : dht.getValues()) {
                    assertEquals(valueId, dhtValue.getValueID());
                    assertEquals(value, dhtValue.getData());
                    assertEquals(originator.getLocalNodeID(), dhtValue.getOriginatorID());
                    assertEquals(originator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                }
                
                if (closest == null) {
                    closest = dht;
                }
                
                furthest = dht;
            }
            
            // Create a Node with the nearest possible Node ID
            // That means we set the Node ID to the Value ID
            Context nearest = (Context)MojitoFactory.createDHT("Nearest");
            Method m = nearest.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
            m.setAccessible(true);
            m.invoke(nearest, new Object[]{valueId});
            
            nearest.bind(new InetSocketAddress("localhost", port+1000));
            nearest.start();
            nearest.bootstrap(new InetSocketAddress("localhost", port + k)).get();
            
            // The 'furthest' Node should no longer have the value
            assertEquals(1, nearest.getValues().size());
            assertEquals(0, furthest.getValues().size());
            for (Contact remote : evt.getNodes()) {
                Context dht = (Context)dhts.get(remote.getNodeID());
                
                if (dht != furthest) {
                    assertEquals(1, dht.getDatabase().getKeyCount());
                    assertEquals(1, dht.getDatabase().getValueCount());
                    for (DHTValue dhtValue : dht.getValues()) {
                        assertEquals(valueId, dhtValue.getValueID());
                        assertEquals(value, dhtValue.getData());
                        assertEquals(originator.getLocalNodeID(), dhtValue.getOriginatorID());
                        assertEquals(originator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                    }
                }
            }
            
            // The 'nearest' Node received the value from the 
            // previous 'closest' Node
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValue dhtValue : nearest.getValues()) {
                assertEquals(valueId, dhtValue.getValueID());
                assertEquals(value, dhtValue.getData());
                assertEquals(originator.getLocalNodeID(), dhtValue.getOriginatorID());
                
                // The closest Node send us the value!
                assertEquals(closest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Clear the Database but don't change the instanceId!
            // The other Nodes don't know that we cleared our DB
            // and will this not store values!
            nearest.getDatabase().clear();
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            nearest.bootstrap(new InetSocketAddress("localhost", port + k + 5)).get();
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            
            // Change the instanceId and we'll asked to store the
            // value again!
            ((LocalContact)nearest.getLocalNode()).nextInstanceID();
            nearest.bootstrap(new InetSocketAddress("localhost", port + k + 10)).get();
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValue dhtValue : nearest.getValues()) {
                assertEquals(valueId, dhtValue.getValueID());
                assertEquals(value, dhtValue.getData());
                assertEquals(originator.getLocalNodeID(), dhtValue.getOriginatorID());
                
                // The closest Node send us the value!
                assertEquals(closest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Pick a Node from the middle of the k-closest Nodes,
            // clear its Database and do the same test as with the
            // nearest Node above
            Context middle = null;
            int index = 0;
            for (Contact node : evt.getNodes()) {
                if (index == k/2) {
                    middle = (Context)dhts.get(node.getNodeID());
                    break;
                }
                
                index++;
            }
            assertNotNull(middle);
            
            middle.getDatabase().clear();
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            middle.bootstrap(new InetSocketAddress("localhost", port + k + 5)).get();
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            
            ((LocalContact)middle.getLocalNode()).nextInstanceID();
            middle.bootstrap(new InetSocketAddress("localhost", port + k + 10)).get();
            assertEquals(1, middle.getDatabase().getKeyCount());
            assertEquals(1, middle.getDatabase().getValueCount());
            for (DHTValue dhtValue : middle.getValues()) {
                assertEquals(valueId, dhtValue.getValueID());
                assertEquals(value, dhtValue.getData());
                assertEquals(originator.getLocalNodeID(), dhtValue.getOriginatorID());
                
                // The nearest Node send us the value
                assertEquals(nearest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Change the instanceId the previous furthest Node
            // and it shouldn't get the value
            ((LocalContact)furthest.getLocalNode()).nextInstanceID();
            furthest.bootstrap(new InetSocketAddress("localhost", port + k + 2)).get();
            assertEquals(0, furthest.getDatabase().getKeyCount());
            assertEquals(0, furthest.getDatabase().getValueCount());
            
            // Same test with the current furthest Node but with
            // the exception that the current furthest Node will
            // receive the value from the nearest Node again.
            // First step: Get the current furthest Nodes
            index = 0;
            furthest = null;
            for (Contact node : evt.getNodes()) {
                if (index == evt.getNodes().size()-2) {
                    furthest = (Context)dhts.get(node.getNodeID());
                    break;
                }
                index++;
            }
            assertNotNull(furthest);
            
            // Check the intial state
            assertEquals(1, furthest.getDatabase().getKeyCount());
            assertEquals(1, furthest.getDatabase().getValueCount());
            
            // Clear the Database but don't change the instanceId yet
            furthest.getDatabase().clear();
            assertEquals(0, furthest.getDatabase().getKeyCount());
            assertEquals(0, furthest.getDatabase().getValueCount());
            furthest.bootstrap(new InetSocketAddress("localhost", port + k + 5)).get();
            assertEquals(0, furthest.getDatabase().getKeyCount());
            assertEquals(0, furthest.getDatabase().getValueCount());
            
            // Change the instanceId 
            ((LocalContact)furthest.getLocalNode()).nextInstanceID();
            furthest.bootstrap(new InetSocketAddress("localhost", port + k + 2)).get();
            
            // And we should have the value now
            assertEquals(1, furthest.getDatabase().getKeyCount());
            assertEquals(1, furthest.getDatabase().getValueCount());
            for (DHTValue dhtValue : furthest.getValues()) {
                assertEquals(valueId, dhtValue.getValueID());
                assertEquals(value, dhtValue.getData());
                assertEquals(originator.getLocalNodeID(), dhtValue.getOriginatorID());
                
                // The nearest Node send us the value
                assertEquals(nearest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            ((LocalContact)nearest.getLocalNode()).nextInstanceID();
            furthest.bootstrap(new InetSocketAddress("localhost", port + k + 2)).get();
            
            // Check the final state. k Nodes should have the value!
            int count = 0;
            dhts.put(nearest.getLocalNodeID(), nearest);
            for (MojitoDHT dht : dhts.values()) {
                count += ((Context)dht).getValues().size();
            }
            
            // Make sure we're not counting the originator if it's
            // not member of the k-closest Nodes to the given value!
            boolean contains = false;
            for (Contact node : evt.getNodes()) {
                if (node.getNodeID().equals(originator.getLocalNodeID())) {
                    contains = true;
                    break;
                }
            }
            
            if (!contains) {
                count--;
            }
            
            assertEquals(k, count);
            
        } finally {
            for (MojitoDHT dht : dhts.values()) {
                dht.stop();
            }
        }
    }
}
