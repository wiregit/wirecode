/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.TestSuite;

import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.Trie;
import org.limewire.collection.TrieUtils;
import org.limewire.mojito.util.UnitTestUtils;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.MojitoFactory;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.SecurityTokenEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.settings.BucketRefresherSettings;
import org.limewire.mojito2.settings.DatabaseSettings;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.settings.RouteTableSettings;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.security.SecurityToken;
import org.limewire.util.StringUtils;

@SuppressWarnings("null")
public class CacheForwardTest extends MojitoTestCase {
    
    private static final int PORT = 3000;
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public CacheForwardTest(String name) {
        super(name);
    }
   
    public static TestSuite suite() {
        return buildTestSuite(CacheForwardTest.class);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
        RouteTableSettings.INCOMING_REQUESTS_UNKNOWN.setValue(true);
    }
    
    public void disabledtestCacheForward() throws Exception {
        // This test is testing a disabled feature and keeps failing. 
        // We disable this test for now. See LWC-2778 for detail
        
        final long waitForNodes = 1000; // ms
        final long BUCKET_REFRESH = 1 * 1000;
        // it takes my machine 8.5 seconds to bootstrap the 3*k nodes and the build machine
        // is faster than mine. So 8.5 seconds should be enough for the build machine to 
        // bootstrapp the 3*k nodes. To be safe, we set BOOTSTRAP_TIME to be 10 seconds. 
        // We use this value as BUCKET_REFRESHER_DELAY since we want all nodes finish 
        // bootstrapping (joining the network) before pinging nearest neighbors. 
        final long BOOTSTRAP_TIME = 10 * 1000; 
        
        //ContextSettings.SEND_SHUTDOWN_MESSAGE.setValue(false);
        BucketRefresherSettings.BUCKET_REFRESHER_PING_NEAREST.setValue(BUCKET_REFRESH);
        BucketRefresherSettings.BUCKET_REFRESHER_DELAY.setValue(BOOTSTRAP_TIME);        
        DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.setValue(false);
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        // KUID valueId = KUID.create("40229239B68FFA66575E59D0AB1F685AD3191960");
        KUID valueId = KUID.createRandomID();
        
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        MojitoDHT first = null;
        try {
            for (int i = 0; i < 3*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i, PORT+i);
                
                if (i > 0) {
                    Thread.sleep(100);
                    dht.bootstrap(new InetSocketAddress("localhost", PORT)).get();
                    // the bootstrapper needs to ping every joining node.
                    first.ping(new InetSocketAddress("localhost", PORT+i)).get();
                } else {
                    first = dht;
                }
                
                dhts.put(dht.getLocalNodeID(), dht);
            }
            
            first.bootstrap(new InetSocketAddress("localhost", PORT+1)).get();
            Thread.sleep(waitForNodes);
            
            // Sort all KUIDs by XOR distance and use the Node as 
            // creator that's furthest away from the value ID so
            // that it never cannot be member of the k-closest Nodes
            Trie<KUID, KUID> trie = new PatriciaTrie<KUID, KUID>(KUID.KEY_ANALYZER);
            for (KUID id : dhts.keySet()) {
                trie.put(id, id);
            }
            List<KUID> idsByXorDistance = TrieUtils.select(trie, valueId, trie.size());
            
            // Creator happens to be the furthest of the k-closest Nodes. 
            //MojitoDHT creator = dhts.get(idsByXorDistance.get(k-1)); 
            
            // Use the furthest Node as the creator.
            MojitoDHT creator = dhts.get(idsByXorDistance.get(idsByXorDistance.size()-1));
            // Store the value
            DHTValue value = new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toUTF8Bytes("Hello World"));
            StoreEntity evt = creator.put(valueId, value).get();            
            
            // see LWC-2778. In case the root is UNKNOWN in others route table, 
            // we wait BOOTSTRAP_TIME (the delay of ping the nearest bucket)
            // so that every node has a chance to ping the root, hence realize 
            // the root is ALIVE
            boolean waiting = true;
            KUID rootsID = TrieUtils.select(trie, valueId, 1).get(0);
            for (Contact c : evt.getContacts()){
                if (c.getNodeID().equals(rootsID)){
                    waiting = false;
                    break;
                }
            }
            if (waiting) {
                for (Contact c: evt.getContacts()) {
                    MojitoDHT dht = dhts.get(c.getNodeID());
                    dht.getDatabase().clear();
                }
                Thread.sleep(BOOTSTRAP_TIME);
                evt = creator.put(valueId, value).get();
            }
            
            assertEquals(k, evt.getContacts().length);
            
            // Give everybody time to process the store request
            Thread.sleep(waitForNodes);
            
            // And check the initial state
            MojitoDHT closest = null;
            for (Contact remote : evt.getContacts()) {
                MojitoDHT dht = dhts.get(remote.getNodeID());
                assertEquals(1, dht.getDatabase().getKeyCount());
                assertEquals(1, dht.getDatabase().getValueCount());
                for (DHTValueEntity dhtValue : dht.getDatabase().values()) {
                    assertEquals(valueId, dhtValue.getPrimaryKey());
                    assertEquals(value, dhtValue.getValue());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                }
                
                if (closest == null) {
                    closest = dht;
                }
            }
            
            // Create a Node with the nearest possible Node ID
            // That means we set the Node ID to the Value ID
            MojitoDHT nearest = MojitoFactory.createDHT("Nearest", PORT+500);
            nearest.setContactId(valueId);
            
            bootstrap(nearest, dhts.values());
            
            
            // Give everybody time to figure out whether to forward
            // a value or to remove it
            Thread.sleep(waitForNodes);

            // The Node with the nearest possible ID should have the value
            assertEquals(1, nearest.getDatabase().getValueCount());
            
            for (Contact remote : evt.getContacts()) {
                MojitoDHT dht = dhts.get(remote.getNodeID());

                assertEquals("I dont have it?? "+dht.getLocalNodeID(),1, dht.getDatabase().getKeyCount());
                assertEquals(1, dht.getDatabase().getValueCount());
                for (DHTValueEntity dhtValue : dht.getDatabase().values()) {
                    assertEquals(valueId, dhtValue.getPrimaryKey());
                    assertEquals(value, dhtValue.getValue());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                }
            }
            
            // The 'nearest' Node received the value from the 
            // previous 'closest' Node
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : nearest.getDatabase().values()) {
                assertEquals(valueId, dhtValue.getPrimaryKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The closest Node send us the value!
                assertEquals(closest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Clear the Database but don't change the instanceId!
            // The other Nodes don't know that we cleared our DB
            // and will this not store values!
            nearest.getDatabase().clear();
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            bootstrap(nearest, dhts.values());
            
            // Give everybody time to figure out whether to forward
            // a value or to remove it
            Thread.sleep(waitForNodes);
            
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            
            // Change the instanceId and we'll asked to store the
            // value again!
            nearest.getLocalNode().nextInstanceID();
            bootstrap(nearest, dhts.values());
            
            // Give everybody time to figure out whether to forward
            // a value or to remove it
            Thread.sleep(waitForNodes);
            
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : nearest.getDatabase().values()) {
                assertEquals(valueId, dhtValue.getPrimaryKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The closest Node send us the value!
                assertEquals(closest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Pick a Node from the middle of the k-closest Nodes,
            // clear its Database and do the same test as with the
            // nearest Node above
            MojitoDHT middle = null;
            int index = 0;
            for (Contact node : evt.getContacts()) {
                if (index == k/2) {
                    middle = dhts.get(node.getNodeID());
                    break;
                }
                
                index++;
            }
            assertNotNull(middle);
            
            middle.getDatabase().clear();
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            bootstrap(middle, dhts.values());
            Thread.sleep(waitForNodes);
            
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            
            middle.getLocalNode().nextInstanceID();
            bootstrap(middle, dhts.values());
            Thread.sleep(waitForNodes);
            
            assertEquals(1, middle.getDatabase().getKeyCount());
            assertEquals(1, middle.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : middle.getDatabase().values()) {
                assertEquals(valueId, dhtValue.getPrimaryKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The nearest Node send us the value
                assertEquals(nearest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Check the final state. k + 1 Nodes should have the value!
            int count = 0;
            dhts.put(nearest.getLocalNodeID(), nearest);
            for (MojitoDHT dht : dhts.values()) {
                count += dht.getDatabase().values().size();
            }
            
            // If the creator is a member of the k-closest Nodes then
            // make sure we're counting it as well
            for (Contact node : evt.getContacts()) {
                if (node.getNodeID().equals(creator.getLocalNodeID())) {
                    count++;
                    break;
                }
            }
            
            assertEquals(k + 1, count);
            
        } finally {
            IoUtils.closeAll(dhts.values());
        }
    }
    
    /**
     * This test will fail if the deletion step of store-forwarding
     * is enabled.  Its a reminder to test that functionality if
     * we ever decide to enable it.
     */
    public void testCacheForwardAndDelete() {
        DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.revertToDefault();
        assertFalse(DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.getValue());
    }
    
    /**
     * Bootstraps the given Node from one of the other Nodes and makes sure a
     * Node isn't trying to bootstrap from itself which would fail.
     */
    private static void bootstrap(MojitoDHT dht, Collection<MojitoDHT> dhts) 
            throws ExecutionException, InterruptedException {
        
        for (MojitoDHT other : dhts) {
            if (!dht.getLocalNodeID().equals(other.getLocalNodeID())) {
                InetSocketAddress addr = (InetSocketAddress)other.getContactAddress();
                dht.bootstrap(new InetSocketAddress("localhost", addr.getPort())).get();
                return;
            }
        }
        
        throw new IllegalStateException("Could not bootstrap: " + dht);
    }
}
