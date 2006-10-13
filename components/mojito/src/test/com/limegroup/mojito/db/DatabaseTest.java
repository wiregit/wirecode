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
 
package com.limegroup.mojito.db;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue.ValueType;
import com.limegroup.mojito.db.impl.DatabaseImpl;
import com.limegroup.mojito.routing.ContactFactory;

public class DatabaseTest extends BaseTestCase {
    
    public DatabaseTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(DatabaseTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static DHTValue createLocalDHTValue(byte[] value) {
        return createLocalDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValue createLocalDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        Contact node = ContactFactory.createLocalContact(0, 0, nodeId, 0, false);
        return DHTValueFactory.createLocalValue(node, valueId, ValueType.BINARY, value);
    }
    
    private static DHTValue createDirectDHTValue(byte[] value) {
        return createDirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValue createDirectDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactFactory.createLiveContact(addr, 0, 0, nodeId, addr, 0, false);
        return DHTValueFactory.createRemoteValue(node, node, valueId, ValueType.BINARY, value);
    }
    
    private static DHTValue createIndirectDHTValue(byte[] value) {
        return createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), KUID.createRandomID(), value);
    }
    
    private static DHTValue createIndirectDHTValue(KUID origId, KUID senderId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact orig = ContactFactory.createLiveContact(addr, 0, 0, origId, addr, 0, false);
        Contact sender = ContactFactory.createLiveContact(addr, 0, 0, senderId, addr, 0, false);   
        return DHTValueFactory.createRemoteValue(orig, sender, valueId, ValueType.BINARY, value);
    }
    
    public void testLocalAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a local value
        DHTValue value1 = createLocalDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getValueID())
                    .get(value1.getOriginatorID()).getData()));
        
        // Neither direct...
        DHTValue value2 = createDirectDHTValue(value1.getOriginatorID(), 
                value1.getValueID(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value2.getValueID())
                    .get(value2.getOriginatorID()).getData()));
        
        // ...nor indirect values can replace a local value
        DHTValue value3 = createIndirectDHTValue(value1.getOriginatorID(), 
                KUID.createRandomID(), value1.getValueID(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value3.getValueID())
                    .get(value3.getOriginatorID()).getData()));
        
        // Only local values can replace local values
        DHTValue value4 = createLocalDHTValue(value1.getOriginatorID(), 
                value1.getValueID(), "Tonic".getBytes());
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value4.getValueID())
                    .get(value4.getOriginatorID()).getData()));
        
        // Add a new direct value
        DHTValue value5 = createDirectDHTValue("Mojito".getBytes());
        database.store(value5);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value5.getValueID())
                    .get(value5.getOriginatorID()).getData()));
        
        // local values replace direct values
        DHTValue value6 = createLocalDHTValue(value5.getOriginatorID(), 
                value5.getValueID(), "Mary".getBytes());
        database.store(value6);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Mary".getBytes(), 
                database.get(value6.getValueID())
                    .get(value6.getOriginatorID()).getData()));
        
        // Add an indirect value
        DHTValue value7 = createDirectDHTValue("Bloody".getBytes());
        database.store(value7);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Bloody".getBytes(), 
                database.get(value7.getValueID())
                    .get(value7.getOriginatorID()).getData()));
        
        // local values replace indirect values
        DHTValue value8 = createLocalDHTValue(value7.getOriginatorID(), 
                value7.getValueID(), "Lime".getBytes());
        database.store(value8);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Lime".getBytes(), 
                database.get(value8.getValueID())
                    .get(value8.getOriginatorID()).getData()));

    }
    
    public void testDirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a directly stored value
        DHTValue value1 = createDirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getValueID())
                    .get(value1.getOriginatorID()).getData()));
        
        // Shouldn't change
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        // The originator is issuing a direct store request
        DHTValue value2 = createDirectDHTValue(value1.getOriginatorID(), 
                value1.getValueID(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value2.getValueID())
                    .get(value2.getOriginatorID()).getData()));
        
        // A directly stored value cannot be replaced by
        // an indirect value
        DHTValue value3 = createIndirectDHTValue(value2.getOriginatorID(), 
                    KUID.createRandomID(), value2.getValueID(), "Tough".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value3.getValueID())
                    .get(value3.getOriginatorID()).getData()));
    }
    
    public void testIndirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add an indiriectly stored value
        DHTValue value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getValueID())
                    .get(value1.getOriginatorID()).getData()));
        
        // Indirect replaces indirect
        DHTValue value2 = createIndirectDHTValue(value1.getOriginatorID(), 
                KUID.createRandomID(), value1.getValueID(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value2.getValueID())
                    .get(value2.getOriginatorID()).getData()));
        
        // Direct replaces indirect
        DHTValue value3 = createDirectDHTValue(value2.getOriginatorID(), 
                value2.getValueID(), "Tonic".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value3.getValueID())
                    .get(value3.getOriginatorID()).getData()));
        
        // Indirect shouldn't replace direct
        DHTValue value4 = createIndirectDHTValue(value3.getOriginatorID(), 
                KUID.createRandomID(), value3.getValueID(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value4.getValueID())
                    .get(value4.getOriginatorID()).getData()));
    }
    
    public void testMultipleValues() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValue value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getValueID())
                    .get(value1.getOriginatorID()).getData()));
        
        // Same Key but different originator/sender
        DHTValue value2 = createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value1.getValueID(), "Tonic".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value2.getValueID())
                    .get(value2.getOriginatorID()).getData()));
        
        // Same Key but a different originator
        DHTValue value3 = createDirectDHTValue(KUID.createRandomID(),
                value1.getValueID(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Mary".getBytes(), 
                database.get(value3.getValueID())
                    .get(value3.getOriginatorID()).getData()));
        
        // Different Key
        DHTValue value4 = createDirectDHTValue("Olga".getBytes());
        database.store(value4);
        assertEquals(2, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertTrue(Arrays.equals("Olga".getBytes(), 
                database.get(value4.getValueID())
                    .get(value4.getOriginatorID()).getData()));
    }
    
    public void testRemove() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValue value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getValueID())
                    .get(value1.getOriginatorID()).getData()));
        
        // It's not possible to remove a value indirectly
        DHTValue value2 = createIndirectDHTValue(value1.getOriginatorID(), 
                KUID.createRandomID(), value1.getValueID(), new byte[0]);
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value2.getValueID())
                    .get(value2.getOriginatorID()).getData()));
        
        // But we can remove values directly
        DHTValue value3 = createDirectDHTValue(value1.getOriginatorID(), 
                value1.getValueID(), new byte[0]);
        database.store(value3);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        
        // Add a new local value
        DHTValue value4 = createLocalDHTValue("Mojito".getBytes());
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value4.getValueID())
                    .get(value4.getOriginatorID()).getData()));
        
        // A local value cannot be removed
        DHTValue value5 = createDirectDHTValue(value4.getOriginatorID(), 
                value4.getValueID(), new byte[0]);
        database.store(value5);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value5.getValueID())
                    .get(value5.getOriginatorID()).getData()));
        
        // But a local value can remove a local value
        DHTValue value6 = createLocalDHTValue(value4.getOriginatorID(), 
                value4.getValueID(), new byte[0]);
        database.store(value6);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
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
}
