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
 
package org.limewire.mojito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import junit.framework.TestSuite;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.Result;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.MojitoUtils;
import org.limewire.mojito.util.UnitTestUtils;
import org.limewire.security.QueryKey;


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
    
    @SuppressWarnings("unchecked")
    public void testGetQueryKey() throws Exception {
        
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            
            // Setup the first instance so that it thinks it's bootstrapping
            dht1 = MojitoFactory.createDHT();
            dht1.bind(2000);
            dht1.start();
            Context context1 = (Context)dht1;
            
            UnitTestUtils.setBootstrapping(dht1, KUID.createRandomID());            
            assertFalse(dht1.isBootstrapped());
            assertTrue(context1.isBootstrapping());
            
            // And setup the second instance so that it thinks it's bootstrapped 
            dht2 = MojitoFactory.createDHT();
            dht2.bind(3000);
            dht2.start();
            Context context2 = (Context)dht2;
            
            UnitTestUtils.setBootstrapped(dht2, true);
            assertTrue(dht2.isBootstrapped());
            assertFalse(context2.isBootstrapping());
            
            // Get the QueryKey...
            Class clazz = Class.forName("org.limewire.mojito.handler.response.StoreResponseHandler$GetQueryKeyHandler");
            Constructor<Callable<Result>> con 
                = clazz.getDeclaredConstructor(Context.class, Contact.class);
            con.setAccessible(true);
            
            Callable<Result> handler = con.newInstance(context2, context1.getLocalNode());
            
            try {
                Result result = handler.call();
                clazz = Class.forName("org.limewire.mojito.handler.response.StoreResponseHandler$GetQueryKeyResult");
                Method m = clazz.getDeclaredMethod("getQueryKey", new Class[0]);
                m.setAccessible(true);
                
                QueryKey queryKey = (QueryKey)m.invoke(result, new Object[0]);
                assertNotNull(queryKey);
            } catch (DHTException err) {
                fail("DHT-1 did not return a QueryKey", err);
            }
            
        } finally {
            if (dht1 != null) { dht1.close(); }
            if (dht2 != null) { dht2.close(); }
        }
    }
    
    public void testCacheForward() throws Exception {
        
        DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.setValue(true);
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        MojitoDHT creator = null;
        try {
            for (int i = 0; i < 3*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(new InetSocketAddress("localhost", PORT + i));
                dht.start();
                
                if (i > 0) {
                    PingResult result = dht.ping(new InetSocketAddress("localhost", PORT)).get();
                    dht.bootstrap(result.getContact());
                } else {
                    creator = dht;
                }
                dhts.put(dht.getLocalNodeID(), dht);
            }
            PingResult result = creator.ping(new InetSocketAddress("localhost", PORT+1)).get();
            creator.bootstrap(result.getContact()).get();
            
            // Store the value
            //KUID valueId = KUID.create("40229239B68FFA66575E59D0AB1F685AD3191960");
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValue(DHTValueType.TEST, 0, "Hello World".getBytes());
            StoreResult evt = creator.put(valueId, value).get();
            assertEquals(k, evt.getNodes().size());
            
            // And check the initial state
            Context closest = null;
            Context furthest = null;
            for (Contact remote : evt.getNodes()) {
                Context dht = (Context)dhts.get(remote.getNodeID());
                assertEquals(1, dht.getDatabase().getKeyCount());
                assertEquals(1, dht.getDatabase().getValueCount());
                for (DHTValueEntity dhtValue : dht.getValues()) {
                    assertEquals(valueId, dhtValue.getKey());
                    assertEquals(value, dhtValue.getValue());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSender().getNodeID());
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
            
            nearest.bind(new InetSocketAddress("localhost", PORT+1000));
            nearest.start();
            bootstrap(nearest, dhts.values());
            Thread.sleep(250);

            // The 'furthest' Node should no longer have the value
            assertEquals(1, nearest.getValues().size());
            assertEquals(0, furthest.getValues().size());
            for (Contact remote : evt.getNodes()) {
                Context dht = (Context)dhts.get(remote.getNodeID());
                
                if (dht != furthest) {
                    assertEquals(1, dht.getDatabase().getKeyCount());
                    assertEquals(1, dht.getDatabase().getValueCount());
                    for (DHTValueEntity dhtValue : dht.getValues()) {
                        assertEquals(valueId, dhtValue.getKey());
                        assertEquals(value, dhtValue.getValue());
                        assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                        assertEquals(creator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                    }
                }
            }
            
            // The 'nearest' Node received the value from the 
            // previous 'closest' Node
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : nearest.getValues()) {
                assertEquals(valueId, dhtValue.getKey());
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
            Thread.sleep(250);
            
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            
            // Change the instanceId and we'll asked to store the
            // value again!
            ((LocalContact)nearest.getLocalNode()).nextInstanceID();
            bootstrap(nearest, dhts.values());
            Thread.sleep(250);
            
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : nearest.getValues()) {
                assertEquals(valueId, dhtValue.getKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
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
            bootstrap(middle, dhts.values());
            Thread.sleep(250);
            
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            
            ((LocalContact)middle.getLocalNode()).nextInstanceID();
            bootstrap(middle, dhts.values());
            Thread.sleep(250);
            
            assertEquals(1, middle.getDatabase().getKeyCount());
            assertEquals(1, middle.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : middle.getValues()) {
                assertEquals(valueId, dhtValue.getKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The nearest Node send us the value
                assertEquals(nearest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Change the instanceId the previous furthest Node
            // and it shouldn't get the value
            ((LocalContact)furthest.getLocalNode()).nextInstanceID();
            bootstrap(furthest, dhts.values());
            Thread.sleep(250);
            
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
            bootstrap(furthest, dhts.values());
            Thread.sleep(250);
            
            assertEquals(0, furthest.getDatabase().getKeyCount());
            assertEquals(0, furthest.getDatabase().getValueCount());
            
            // Change the instanceId 
            ((LocalContact)furthest.getLocalNode()).nextInstanceID();
            bootstrap(furthest, dhts.values());
            Thread.sleep(250);
            
            // And we should have the value now
            assertEquals(1, furthest.getDatabase().getKeyCount());
            assertEquals(1, furthest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : furthest.getValues()) {
                assertEquals(valueId, dhtValue.getKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The nearest Node send us the value
                assertEquals(nearest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            ((LocalContact)nearest.getLocalNode()).nextInstanceID();
            bootstrap(furthest, dhts.values());
            Thread.sleep(250);
            
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
                if (node.getNodeID().equals(creator.getLocalNodeID())) {
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
                dht.close();
            }
        }
    }
    
    /**
     * Bootstraps the given Node from one of the other Nodes
     */
    private static void bootstrap(MojitoDHT dht, Collection<? extends MojitoDHT> dhts) throws Exception {
        MojitoUtils.bootstrap(dht, getRandomAddress(dht, dhts)).get();
    }
    
    /**
     * Returns an address that is different from the given Nodes contact address
     */
    private static SocketAddress getRandomAddress(MojitoDHT dht, Collection<? extends MojitoDHT> dhts) {
        InetSocketAddress addr1 = (InetSocketAddress)dht.getContactAddress();
        
        for (MojitoDHT other : dhts) {
            InetSocketAddress addr2 = (InetSocketAddress)other.getContactAddress();
            if (!addr1.equals(addr2)) {
                return addr2;
            }
        }
        
        throw new IllegalStateException();
    }
}
