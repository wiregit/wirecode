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
 
package org.limewire.mojito.db;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.impl.DHTValueEntityBag;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.util.HostFilter;
import org.limewire.util.PrivilegedAccessor;


public class DatabaseTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public DatabaseTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DatabaseTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static DHTValueEntity createLocalDHTValue(byte[] value) {
        return createLocalDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createLocalDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        Contact node = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, nodeId, 0, false);
        return new DHTValueEntity(node, node, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value), true);
    }
    
    private static DHTValueEntity createDirectDHTValue(byte[] value) {
        return createDirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createDirectDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                nodeId, addr, 0, Contact.DEFAULT_FLAG);
        
        return new DHTValueEntity(node, node, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value), false);
    }
    
    private static DHTValueEntity createIndirectDHTValue(byte[] value) {
        return createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createIndirectDHTValue(KUID creatorId, KUID senderId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        
        Contact creator = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                creatorId, addr, 0, Contact.DEFAULT_FLAG);
        Contact sender = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                senderId, addr, 0, Contact.DEFAULT_FLAG);  
        
        return new DHTValueEntity(creator, sender, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value), false);
    }
    
    public void testLocalAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a local value
        DHTValueEntity value1 = createLocalDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Neither direct...
        DHTValueEntity value2 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getPrimaryKey(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // ...nor indirect values can replace a local value
        DHTValueEntity value3 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getPrimaryKey(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Only local values can replace local values
        DHTValueEntity value4 = createLocalDHTValue(value1.getSecondaryKey(), 
                value1.getPrimaryKey(), "Tonic".getBytes());
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
        
        // Add a new direct value
        DHTValueEntity value5 = createDirectDHTValue("Mojito".getBytes());
        database.store(value5);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value5.getPrimaryKey())
                    .get(value5.getSecondaryKey()).getValue().getValue()));
        
        // local values replace direct values
        DHTValueEntity value6 = createLocalDHTValue(value5.getSecondaryKey(), 
                value5.getPrimaryKey(), "Mary".getBytes());
        database.store(value6);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Mary".getBytes(), 
                database.get(value6.getPrimaryKey())
                    .get(value6.getSecondaryKey()).getValue().getValue()));
        
        // Add an indirect value
        DHTValueEntity value7 = createDirectDHTValue("Bloody".getBytes());
        database.store(value7);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Bloody".getBytes(), 
                database.get(value7.getPrimaryKey())
                    .get(value7.getSecondaryKey()).getValue().getValue()));
        
        // local values replace indirect values
        DHTValueEntity value8 = createLocalDHTValue(value7.getSecondaryKey(), 
                value7.getPrimaryKey(), "Lime".getBytes());
        database.store(value8);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Lime".getBytes(), 
                database.get(value8.getPrimaryKey())
                    .get(value8.getSecondaryKey()).getValue().getValue()));

    }
    
    public void testDirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a directly stored value
        DHTValueEntity value1 = createDirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Shouldn't change
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        // The originator is issuing a direct store request
        DHTValueEntity value2 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getPrimaryKey(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // A directly stored value cannot be replaced by
        // an indirect value
        DHTValueEntity value3 = createIndirectDHTValue(value2.getSecondaryKey(), 
                    KUID.createRandomID(), value2.getPrimaryKey(), "Tough".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testIndirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add an indiriectly stored value
        DHTValueEntity value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Indirect replaces indirect
        DHTValueEntity value2 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getPrimaryKey(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // Direct replaces indirect
        DHTValueEntity value3 = createDirectDHTValue(value2.getSecondaryKey(), 
                value2.getPrimaryKey(), "Tonic".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Indirect shouldn't replace direct
        DHTValueEntity value4 = createIndirectDHTValue(value3.getSecondaryKey(), 
                KUID.createRandomID(), value3.getPrimaryKey(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testMultipleValues() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValueEntity value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Same Key but different originator/sender
        DHTValueEntity value2 = createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value1.getPrimaryKey(), "Tonic".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // Same Key but a different originator
        DHTValueEntity value3 = createDirectDHTValue(KUID.createRandomID(),
                value1.getPrimaryKey(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Mary".getBytes(), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Different Key
        DHTValueEntity value4 = createDirectDHTValue("Olga".getBytes());
        database.store(value4);
        assertEquals(2, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertTrue(Arrays.equals("Olga".getBytes(), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testRemove() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValueEntity value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // It's not possible to remove a value indirectly
        DHTValueEntity value2 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getPrimaryKey(), new byte[0]);
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // But we can remove values directly
        DHTValueEntity value3 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getPrimaryKey(), new byte[0]);
        database.store(value3);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        
        // Add a new local value
        DHTValueEntity value4 = createLocalDHTValue("Mojito".getBytes());
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
        
        // A local value cannot be removed
        DHTValueEntity value5 = createDirectDHTValue(value4.getSecondaryKey(), 
                value4.getPrimaryKey(), new byte[0]);
        database.store(value5);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value5.getPrimaryKey())
                    .get(value5.getSecondaryKey()).getValue().getValue()));
        
        // But a local value can remove a local value
        DHTValueEntity value6 = createLocalDHTValue(value4.getSecondaryKey(), 
                value4.getPrimaryKey(), new byte[0]);
        database.store(value6);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
    }
    
    /**
     * A Database is a Set of DHTValueBags. Each Bag is a Set of
     * DHTValueEntities.
     * 
     * If you iterate over the Bag and remove items from the Bag
     * you leak Bag references because they're not removed from
     * the Database
     * 
     * If you iterate over the Bag and remove items from the Database
     * you don't leak references but run into a ConcurrentModificationException.
     * 
     * Solution: Get a copy of the Bag and remove the items from the Database
     * 
     * Better solution: It'd be nice if the Bag is holding a reference to its
     * Database and cleanup itself once it's empty. There are however all kinds
     * of side effects if you start adding items straight to the Bag instead
     * of using the Database interface and so on.
     * 
     * This test represents the current implemenetation. If you implement the
     * "better solution" this test will fail!
     */
    public void testMultiRemove() {
        MojitoDHT dht = MojitoFactory.createDHT();
        
        DatabaseImpl database = (DatabaseImpl)dht.getDatabase();
        
        KUID key = KUID.createRandomID();
        
        Contact c1 = ContactFactory.createUnknownContact(ContextSettings.getVendor(), 
                ContextSettings.getVersion(), KUID.createRandomID(), 
                new InetSocketAddress("localhost", 1000));
        
        Contact c2 = ContactFactory.createUnknownContact(ContextSettings.getVendor(), 
                ContextSettings.getVersion(), KUID.createRandomID(), 
                new InetSocketAddress("localhost", 2000));
        
        DHTValue value = new DHTValueImpl(DHTValueType.BINARY, Version.ZERO, new byte[1]);
        DHTValueEntity entity1 = new DHTValueEntity(
                dht.getLocalNode(), dht.getLocalNode(), key, value, true);
        
        DHTValueEntity entity2 = new DHTValueEntity(c1, c1, key, value, false);
        DHTValueEntity entity3 = new DHTValueEntity(c2, c2, key, value, false);
        
        database.store(entity1);
        database.store(entity2);
        database.store(entity3);
        
        assertEquals(3, database.getValueCount());
        
        Map<KUID, DHTValueEntity> bag = database.get(key);
        assertNotNull(bag);
        assertEquals(3, bag.size());
       
        // Cannot Iterate over Bag and remove items from it
        // TODO This operation is maybe OK in future (check
        // the Database implementation).
        try {
            for (Iterator<?> it = bag.values().iterator(); it.hasNext(); ) {
                it.remove();
            }
            fail("Should have failed");
        } catch (UnsupportedOperationException expected) {
        }
        
        assertEquals(3, bag.size());
        
        for (DHTValueEntity e : bag.values()) {
            database.remove(e.getPrimaryKey(), e.getSecondaryKey());
        }
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(3, bag.size());
        
        // The Bag should be gone
        assertNull(database.getBag(key));
        assertTrue(database.get(key).isEmpty());
    }
    
    public void testFloodDatabase() {
        DatabaseSettings.MAX_KEYS_PER_IP_BAN_LIMIT.setValue(10);
        Database db = new DatabaseImpl();
        HostFilterStub filter = new HostFilterStub();
        db.setHostFilter(filter);
        
        //this should accept
        Contact badHost = ContactFactory.createLiveContact(
                new InetSocketAddress("169.0.1.1", 1111), 
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("169.0.1.1", 1111), 1, 0);
        
        Contact goodHost = ContactFactory.createLiveContact(
                new InetSocketAddress("169.0.1.2", 1111), Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(), new InetSocketAddress("169.0.1.2", 1111), 1, 0);

        DHTValueEntity value = null;
        //should allow x direct values
        for(int i = 0; i < DatabaseSettings.MAX_KEYS_PER_IP.getValue(); i++) {
            value = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
                        new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "test".getBytes()), false);
            
            assertTrue(db.store(value));
        }
        //and reject after that
        DHTValueEntity newValue = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "test".getBytes()), false);
        assertFalse(db.store(newValue));
        
        //should make some space for a new one
        db.remove(value.getPrimaryKey(), value.getSecondaryKey());
        assertTrue(db.store(value));
        
        //should also reject an indirect one coming from the bad host
        newValue = new DHTValueEntity(badHost, goodHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "test".getBytes()), false);
        assertFalse(db.store(newValue));
        
        //should not allow more, even if it is coming indirectly        
        newValue = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "test".getBytes()), false);;
        assertFalse(db.store(newValue));
        
        //but should allow one created by a good host
        DHTValueEntity goodValue = new DHTValueEntity(goodHost, goodHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "test".getBytes()), false);
        
        assertTrue(db.store(goodValue));

        //test banning now
        badHost = ContactFactory.createLiveContact(
                new InetSocketAddress("169.0.1.3", 1111), Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(), 
                new InetSocketAddress("169.0.1.3", 1111), 1, 0);
        
        for(int i = 0; i <= DatabaseSettings.MAX_KEYS_PER_IP_BAN_LIMIT.getValue(); i++) {
            value = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
                    new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "test".getBytes()), false);
            db.store(value);
        }
        //should have banned the host
        assertContains(filter.getBannedHosts(), badHost.getContactAddress());
    }
    
    /*public void testRemoveAll() {
        Database database = new DatabaseImpl();
        
        KUID valueId = KUID.createRandomID();
        List<KUID> nodeIds = new ArrayList<KUID>();
        for (int i = 0; i < 10; i++) {
            KUID nodeId = KUID.createRandomID();
            DHTValue value = createDirectDHTValue(nodeId, 
                    valueId, ("Lime-" + i).getBytes());
            
            nodeIds.add(nodeId);
            database.store(value);
        }
        
        assertEquals(1, database.getKeyCount());
        assertEquals(10, database.getValueCount());
        
        Map<KUID, DHTValue> view = database.get(valueId);
        assertEquals(10, view.size());
        
        database.removeAll(view.values());
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
    }*/
    
    public void testIncrementRequestLoad() throws Exception{
        DatabaseImpl database = new DatabaseImpl();
        DHTValueEntity entity = createLocalDHTValue("Hello World".getBytes());
        database.store(entity);
        
        KUID primaryKey = entity.getPrimaryKey();
        
        assertEquals(0f, database.getRequestLoad(primaryKey, true));
        
        //should start with larger than smoothing factor
        Thread.sleep(500);
        float load = database.getRequestLoad(primaryKey, true);
        assertGreaterThan(
                DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.getValue(), load);
        Thread.sleep(500);
        assertGreaterThan(load, database.getRequestLoad(primaryKey, true));
        
        //test a 0 smoothing factor
        DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.setValue(0);
        
        database.clear();
        database.store(entity);
        
        Thread.sleep(500);
        assertEquals(0F, database.getRequestLoad(primaryKey, true));
        
        //try a delay larger than nulling time
        database.clear();
        database.store(entity);
        
        DHTValueEntityBag bag = database.getBag(entity.getPrimaryKey());
        long now = System.currentTimeMillis();
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 
                now - DatabaseSettings.VALUE_REQUEST_LOAD_NULLING_DELAY.getValue()*1000L);
        Thread.sleep(500);
        //load should be 0
        assertEquals(0, (int)bag.incrementRequestLoad());
        
        // try a very very large delay
        database.clear();
        database.store(entity);
        bag = database.getBag(entity.getPrimaryKey());
        
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 1L);
        load = bag.incrementRequestLoad();
        //load should never get < 0
        assertGreaterThanOrEquals(0f, load);
        //now try a very very small delay
        bag.incrementRequestLoad();
        Thread.sleep(10);
        bag.incrementRequestLoad();
        Thread.sleep(10);
        bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        //should now have increased (a lot!)
        assertGreaterThan(1f, load);
        //but never be larger than 1/0.01
        assertLessThan(1f/0.01f, load);
    }
    
    public void testReplaceFirewalledValue() {
        final int MAX_VALUES_PER_KEY = 5;
        
        DatabaseImpl database = new DatabaseImpl(-1, MAX_VALUES_PER_KEY);
        
        List<DHTValueEntity> values = new ArrayList<DHTValueEntity>();
        
        KUID primaryKey = KUID.createRandomID();
        
        // Create a bunch of firewalled values and add them to the Database
        for (int i = 0; i < 2*MAX_VALUES_PER_KEY; i++) {
            SocketAddress addr = new InetSocketAddress(6666);
            Contact node = ContactFactory.createLiveContact(
                    addr, 
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    addr, 0, 
                    Contact.FIREWALLED_FLAG);
            
            assertTrue(node.isFirewalled());
            
            DHTValueEntity entity = new DHTValueEntity(node, node, primaryKey, 
                    new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "Hello World".getBytes()), false);
            
            database.store(entity);
            values.add(entity);
        }
        
        // Check initial State
        assertEquals(1, database.getKeyCount());
        assertEquals(MAX_VALUES_PER_KEY, database.getValueCount());
        
        // The first five values should be in the Database
        for (DHTValueEntity entity : values.subList(0, MAX_VALUES_PER_KEY)) {
            assertTrue(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
        
        // And the last five shouldn't!
        for (DHTValueEntity entity : values.subList(MAX_VALUES_PER_KEY, values.size())) {
            assertFalse(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
        
        // Create a non-firewalled value
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactFactory.createLiveContact(
                addr, 
                Vendor.UNKNOWN, 
                Version.ZERO, 
                KUID.createRandomID(), 
                addr, 0, 
                Contact.DEFAULT_FLAG);
        
        assertFalse(node.isFirewalled());
        
        DHTValueEntity notFirewalled = new DHTValueEntity(node, node, primaryKey, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "Hello World".getBytes()), false);
        
        // Store it in the Database
        database.store(notFirewalled);
        
        // It should be there
        assertTrue(database.contains(notFirewalled.getPrimaryKey(), notFirewalled.getSecondaryKey()));
        
        // The oldest firewalled value should be done
        DHTValueEntity oldest = values.get(0);
        assertFalse(database.contains(oldest.getPrimaryKey(), oldest.getSecondaryKey()));
        
        // And the remaining four firewalled values should be still there
        for (DHTValueEntity firewalled : values.subList(1, MAX_VALUES_PER_KEY)) {
            assertTrue(database.contains(firewalled.getPrimaryKey(), firewalled.getSecondaryKey()));
        }
    }
    
    private class HostFilterStub implements HostFilter{
        
        private Set<SocketAddress> bannedHosts = new HashSet<SocketAddress>();
        
        public boolean allow(SocketAddress addr) {
            return true;
        }

        public void ban(SocketAddress addr) {
            bannedHosts.add(addr);
        }

        public Set<SocketAddress> getBannedHosts() {
            return bannedHosts;
        }
    }
}
