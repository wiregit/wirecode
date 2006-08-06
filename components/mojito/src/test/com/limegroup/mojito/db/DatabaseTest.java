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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.impl.DatabaseImpl;
import com.limegroup.mojito.routing.impl.ContactNode;

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
        return createLocalDHTValue(KUID.createRandomNodeID(), 
                KUID.createRandomNodeID(), value);
    }
    
    private static DHTValue createLocalDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        Contact node = ContactNode.createLocalContact(0, 0, nodeId, 0);
        return DHTValue.createLocalValue(node, valueId, value);
    }
    
    private static DHTValue createDirectDHTValue(byte[] value) {
        return createDirectDHTValue(KUID.createRandomNodeID(), 
                KUID.createRandomNodeID(), value);
    }
    
    private static DHTValue createDirectDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactNode.createLiveContact(addr, 0, 0, nodeId, addr, 0, false);
        return DHTValue.createRemoteValue(node, node, valueId, value);
    }
    
    private static DHTValue createIndirectDHTValue(byte[] value) {
        return createIndirectDHTValue(KUID.createRandomNodeID(), 
                KUID.createRandomNodeID(), KUID.createRandomNodeID(), value);
    }
    
    private static DHTValue createIndirectDHTValue(KUID origId, KUID senderId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact orig = ContactNode.createLiveContact(addr, 0, 0, origId, addr, 0, false);
        Contact sender = ContactNode.createLiveContact(addr, 0, 0, senderId, addr, 0, false);   
        return DHTValue.createRemoteValue(orig, sender, valueId, value);
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
                KUID.createRandomNodeID(), value1.getValueID(), "Mary".getBytes());
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
                    KUID.createRandomNodeID(), value2.getValueID(), "Tough".getBytes());
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
                KUID.createRandomNodeID(), value1.getValueID(), "Mojito".getBytes());
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
                KUID.createRandomNodeID(), value3.getValueID(), "Mary".getBytes());
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
        DHTValue value2 = createIndirectDHTValue(KUID.createRandomNodeID(), 
                KUID.createRandomNodeID(), value1.getValueID(), "Tonic".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value2.getValueID())
                    .get(value2.getOriginatorID()).getData()));
        
        // Same Key but a different originator
        DHTValue value3 = createDirectDHTValue(KUID.createRandomNodeID(),
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
                KUID.createRandomNodeID(), value1.getValueID(), new byte[0]);
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
    
    public void testValuesView() {
        Database database = new DatabaseImpl();
        
        DHTValue value1 = createDirectDHTValue("Hello World".getBytes());
        database.store(value1);
        
        DHTValue value2 = createDirectDHTValue("Swiss Cheese Burger".getBytes());
        database.store(value2);
        
        DHTValue value3 = createDirectDHTValue("Mojito".getBytes());
        database.store(value3);
        
        DHTValue value4 = createDirectDHTValue(KUID.createRandomNodeID(), 
                value3.getValueID(), "Hello World".getBytes());
        database.store(value4);
        
        DHTValue value5 = createDirectDHTValue("Lime".getBytes());
        
        Collection<DHTValue> view = database.values();
        assertEquals(database.contains(value1), view.contains(value1));
        assertEquals(database.contains(value2), view.contains(value2));
        assertEquals(database.contains(value3), view.contains(value3));
        assertEquals(database.contains(value4), view.contains(value4));
        assertEquals(database.contains(value5), view.contains(value5)); // is not in the DB!
        
        assertEquals(3, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertEquals(4, view.size());
        
        // --- Test remove ---
        view.remove(value5); // Nothing changes as value5 is not in the DB
        assertEquals(3, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertEquals(4, view.size());
        
        view.remove(value4);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertEquals(3, view.size());
        
        view.remove(value3);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertEquals(2, view.size());
        
        // --- Test add ---
        view.add(value5);
        assertTrue(database.contains(value5));
        assertTrue(view.contains(value5));
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertEquals(3, view.size());
        
        // Store a new value under value5's key
        DHTValue value6 = createDirectDHTValue(KUID.createRandomNodeID(), 
                value5.getValueID(), "Juice".getBytes());
        view.add(value6);
        assertTrue(database.contains(value6));
        assertTrue(view.contains(value6));
        assertEquals(3, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertEquals(4, view.size());
    
        // Store an empty value -> remove
        DHTValue value7 = createDirectDHTValue(value6.getOriginatorID(), 
                value6.getValueID(), new byte[0]);
        view.add(value7);
        
        assertFalse(database.contains(value6));
        assertFalse(view.contains(value6));
        assertFalse(database.contains(value7));
        assertFalse(view.contains(value7));
        
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertEquals(3, view.size());
    }
    
    public void testMapViewRemove() {
        Database database = new DatabaseImpl();
        
        KUID valueId = KUID.createRandomNodeID();
        List<KUID> nodeIds = new ArrayList<KUID>();
        for (int i = 0; i < 10; i++) {
            KUID nodeId = KUID.createRandomNodeID();
            DHTValue value = createDirectDHTValue(nodeId, 
                    valueId, ("Lime-" + i).getBytes());
            
            nodeIds.add(nodeId);
            database.store(value);
        }
        
        assertEquals(1, database.getKeyCount());
        assertEquals(10, database.getValueCount());
        
        Map<KUID, DHTValue> view = database.get(valueId);
        assertEquals(10, view.size());
        
        view.remove(nodeIds.remove(nodeIds.size()/2));
        assertEquals(1, database.getKeyCount());
        assertEquals(9, database.getValueCount());
        assertEquals(9, view.size());
        
        view.keySet().remove(nodeIds.remove(nodeIds.size()/2));
        assertEquals(1, database.getKeyCount());
        assertEquals(8, database.getValueCount());
        assertEquals(8, view.size());
        
        view.values().remove(view.get(nodeIds.remove(nodeIds.size()/2)));
        assertEquals(1, database.getKeyCount());
        assertEquals(7, database.getValueCount());
        assertEquals(7, view.size());
        
        // Remove from EntrySet
        for (Iterator it = view.entrySet().iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
        }
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
        
        // Remove from KeySet
        for (int i = 0; i < 10; i++) {
            KUID nodeId = KUID.createRandomNodeID();
            DHTValue value = createDirectDHTValue(nodeId, 
                    valueId, ("Lime-" + i).getBytes());
            
            database.store(value);
        }
        
        assertEquals(1, database.getKeyCount());
        assertEquals(10, database.getValueCount());
        
        view = database.get(valueId);
        assertEquals(10, view.size());
        
        for (Iterator it = view.keySet().iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
        }
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
        
        // Remove from ValueCollection
        for (int i = 0; i < 10; i++) {
            KUID nodeId = KUID.createRandomNodeID();
            DHTValue value = createDirectDHTValue(nodeId, 
                    valueId, ("Lime-" + i).getBytes());
            
            database.store(value);
        }
        
        assertEquals(1, database.getKeyCount());
        assertEquals(10, database.getValueCount());
        
        view = database.get(valueId);
        assertEquals(10, view.size());
        
        for (Iterator it = view.values().iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
        }
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
        
        // Removing the last item from the keys view
        DHTValue value = createDirectDHTValue("Lime-".getBytes());
        database.store(value);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        view = database.get(value.getValueID());
        assertEquals(1, view.size());
        
        Set<KUID> keys = view.keySet();
        keys.remove(keys.iterator().next());
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
        assertEquals(0, keys.size());
        
        // Removing the last item from the entries view
        value = createDirectDHTValue("Lime-".getBytes());
        database.store(value);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        view = database.get(value.getValueID());
        assertEquals(1, view.size());
        
        Set<Entry<KUID,DHTValue>> entries = view.entrySet();
        entries.remove(entries.iterator().next());
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
        assertEquals(0, entries.size());
        
        // Removing the last item from the values view
        value = createDirectDHTValue("Lime-".getBytes());
        database.store(value);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        view = database.get(value.getValueID());
        assertEquals(1, view.size());
        
        Collection<DHTValue> values = view.values();
        values.remove(values.iterator().next());
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
        assertEquals(0, values.size());
    }
    
    public void testValuesIterator() {
        Database database = new DatabaseImpl();
        
        DHTValue value = null;
        for (int i = 0; i < 10; i++) {
            if (value != null && i % 3 == 0) {
                value = createDirectDHTValue(KUID.createRandomNodeID(), 
                        value.getValueID(), ("Lime-" + i).getBytes());
            } else {
                value = createDirectDHTValue(("Lime-" + i).getBytes());
            }
            
            database.store(value);
        }
        
        assertEquals(10, database.getValueCount());
        assertEquals(7, database.getKeyCount());
        
        Collection<DHTValue> view = database.values();
        
        int count = 0;
        for (DHTValue v : view) {
            count++;
        }
        assertEquals(10, count);
        
        for (Iterator<DHTValue> it = database.values().iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
        }
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(0, view.size());
    }
    
    public void testRemoveKeys() {
        Database database = new DatabaseImpl();
        
        DHTValue value = null;
        for (int i = 0; i < 10; i++) {
            if (value != null && i % 3 == 0) {
                value = createDirectDHTValue(KUID.createRandomNodeID(), 
                        value.getValueID(), ("Lime-" + i).getBytes());
            } else {
                value = createDirectDHTValue(("Lime-" + i).getBytes());
            }
            
            database.store(value);
        }
        
        assertEquals(10, database.getValueCount());
        assertEquals(7, database.getKeyCount());
        
        Set<KUID> keys = database.keySet();
        assertEquals(7, keys.size());
        
        KUID key = keys.iterator().next();
        Map<KUID,DHTValue> view = database.get(key);
        keys.remove(key);
        assertEquals(6, keys.size());
        assertEquals(6, database.getKeyCount());
        assertEquals(10-view.size(), database.getValueCount()); // 9 or 8
        assertTrue(database.getValueCount() == 8 || database.getValueCount() == 9);
        
        for (Iterator it = keys.iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
        }
        
        assertEquals(0, keys.size());
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
    }
}
