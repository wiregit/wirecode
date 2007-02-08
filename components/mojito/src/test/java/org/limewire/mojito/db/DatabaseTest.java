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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.impl.DHTValueEntityImpl;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.util.HostFilter;


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
        Contact node = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.UNKNOWN, nodeId, 0, false);
        return new DHTValueEntityImpl(node, node, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, value), true);
    }
    
    private static DHTValueEntity createDirectDHTValue(byte[] value) {
        return createDirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createDirectDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.UNKNOWN, 
                nodeId, addr, 0, Contact.DEFAULT_FLAG);
        
        return new DHTValueEntityImpl(node, node, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, value), false);
    }
    
    private static DHTValueEntity createIndirectDHTValue(byte[] value) {
        return createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createIndirectDHTValue(KUID creatorId, KUID senderId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        
        Contact creator = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.UNKNOWN, 
                creatorId, addr, 0, Contact.DEFAULT_FLAG);
        Contact sender = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.UNKNOWN, 
                senderId, addr, 0, Contact.DEFAULT_FLAG);  
        
        return new DHTValueEntityImpl(creator, sender, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, value), false);
    }
    
    public void testLocalAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a local value
        DHTValueEntity value1 = createLocalDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getKey())
                    .getValuesMap().get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Neither direct...
        DHTValueEntity value2 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getKey(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value2.getKey())
                    .getValuesMap().get(value2.getSecondaryKey()).getValue().getValue()));
        
        // ...nor indirect values can replace a local value
        DHTValueEntity value3 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getKey(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value3.getKey())
                    .getValuesMap().get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Only local values can replace local values
        DHTValueEntity value4 = createLocalDHTValue(value1.getSecondaryKey(), 
                value1.getKey(), "Tonic".getBytes());
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value4.getKey())
                    .getValuesMap().get(value4.getSecondaryKey()).getValue().getValue()));
        
        // Add a new direct value
        DHTValueEntity value5 = createDirectDHTValue("Mojito".getBytes());
        database.store(value5);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value5.getKey())
                    .getValuesMap().get(value5.getSecondaryKey()).getValue().getValue()));
        
        // local values replace direct values
        DHTValueEntity value6 = createLocalDHTValue(value5.getSecondaryKey(), 
                value5.getKey(), "Mary".getBytes());
        database.store(value6);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Mary".getBytes(), 
                database.get(value6.getKey())
                    .getValuesMap().get(value6.getSecondaryKey()).getValue().getValue()));
        
        // Add an indirect value
        DHTValueEntity value7 = createDirectDHTValue("Bloody".getBytes());
        database.store(value7);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Bloody".getBytes(), 
                database.get(value7.getKey())
                    .getValuesMap().get(value7.getSecondaryKey()).getValue().getValue()));
        
        // local values replace indirect values
        DHTValueEntity value8 = createLocalDHTValue(value7.getSecondaryKey(), 
                value7.getKey(), "Lime".getBytes());
        database.store(value8);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Lime".getBytes(), 
                database.get(value8.getKey())
                    .getValuesMap().get(value8.getSecondaryKey()).getValue().getValue()));

    }
    
    public void testDirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a directly stored value
        DHTValueEntity value1 = createDirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getKey())
                    .getValuesMap().get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Shouldn't change
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        // The originator is issuing a direct store request
        DHTValueEntity value2 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getKey(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value2.getKey())
                    .getValuesMap().get(value2.getSecondaryKey()).getValue().getValue()));
        
        // A directly stored value cannot be replaced by
        // an indirect value
        DHTValueEntity value3 = createIndirectDHTValue(value2.getSecondaryKey(), 
                    KUID.createRandomID(), value2.getKey(), "Tough".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value3.getKey())
                    .getValuesMap().get(value3.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testIndirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add an indiriectly stored value
        DHTValueEntity value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getKey())
                    .getValuesMap().get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Indirect replaces indirect
        DHTValueEntity value2 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getKey(), "Mojito".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value2.getKey())
                    .getValuesMap().get(value2.getSecondaryKey()).getValue().getValue()));
        
        // Direct replaces indirect
        DHTValueEntity value3 = createDirectDHTValue(value2.getSecondaryKey(), 
                value2.getKey(), "Tonic".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value3.getKey())
                    .getValuesMap().get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Indirect shouldn't replace direct
        DHTValueEntity value4 = createIndirectDHTValue(value3.getSecondaryKey(), 
                KUID.createRandomID(), value3.getKey(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value4.getKey())
                    .getValuesMap().get(value4.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testMultipleValues() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValueEntity value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getKey())
                    .getValuesMap().get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Same Key but different originator/sender
        DHTValueEntity value2 = createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value1.getKey(), "Tonic".getBytes());
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals("Tonic".getBytes(), 
                database.get(value2.getKey())
                    .getValuesMap().get(value2.getSecondaryKey()).getValue().getValue()));
        
        // Same Key but a different originator
        DHTValueEntity value3 = createDirectDHTValue(KUID.createRandomID(),
                value1.getKey(), "Mary".getBytes());
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals("Mary".getBytes(), 
                database.get(value3.getKey())
                    .getValuesMap().get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Different Key
        DHTValueEntity value4 = createDirectDHTValue("Olga".getBytes());
        database.store(value4);
        assertEquals(2, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertTrue(Arrays.equals("Olga".getBytes(), 
                database.get(value4.getKey())
                    .getValuesMap().get(value4.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testRemove() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValueEntity value1 = createIndirectDHTValue("Hello World".getBytes());
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value1.getKey())
                    .getValuesMap().get(value1.getSecondaryKey()).getValue().getValue()));
        
        // It's not possible to remove a value indirectly
        DHTValueEntity value2 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getKey(), new byte[0]);
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Hello World".getBytes(), 
                database.get(value2.getKey())
                    .getValuesMap().get(value2.getSecondaryKey()).getValue().getValue()));
        
        // But we can remove values directly
        DHTValueEntity value3 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getKey(), new byte[0]);
        database.store(value3);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        
        // Add a new local value
        DHTValueEntity value4 = createLocalDHTValue("Mojito".getBytes());
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value4.getKey())
                    .getValuesMap().get(value4.getSecondaryKey()).getValue().getValue()));
        
        // A local value cannot be removed
        DHTValueEntity value5 = createDirectDHTValue(value4.getSecondaryKey(), 
                value4.getKey(), new byte[0]);
        database.store(value5);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals("Mojito".getBytes(), 
                database.get(value5.getKey())
                    .getValuesMap().get(value5.getSecondaryKey()).getValue().getValue()));
        
        // But a local value can remove a local value
        DHTValueEntity value6 = createLocalDHTValue(value4.getSecondaryKey(), 
                value4.getKey(), new byte[0]);
        database.store(value6);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
    }
    
    public void testFloodDatabase() {
        DatabaseSettings.MAX_KEY_PER_IP_BAN_LIMIT.setValue(10);
        Database db = new DatabaseImpl();
        HostFilterStub filter = new HostFilterStub();
        db.setHostFilter(filter);
        
        //this should accept
        Contact badHost = ContactFactory.createLiveContact(
                new InetSocketAddress("169.0.1.1", 1111), 
                Vendor.UNKNOWN, Version.UNKNOWN, KUID.createRandomID(), 
                new InetSocketAddress("169.0.1.1", 1111), 1, 0);
        
        Contact goodHost = ContactFactory.createLiveContact(
                new InetSocketAddress("169.0.1.2", 1111), Vendor.UNKNOWN, Version.UNKNOWN, 
                KUID.createRandomID(), new InetSocketAddress("169.0.1.2", 1111), 1, 0);

        DHTValueEntity value = null;
        //should allow x direct values
        for(int i = 0; i < DatabaseSettings.MAX_KEY_PER_IP.getValue(); i++) {
            value = new DHTValueEntityImpl(badHost, badHost, KUID.createRandomID(), 
                        new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false);
            
            assertTrue(db.store(value));
        }
        //and reject after that
        DHTValueEntity newValue = new DHTValueEntityImpl(badHost, badHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false);
        assertFalse(db.store(newValue));
        
        //should make some space for a new one
        db.remove(value);
        assertTrue(db.store(value));
        
        //should also reject an indirect one coming from the bad host
        newValue = new DHTValueEntityImpl(badHost, goodHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false);
        assertFalse(db.store(newValue));
        
        //should not allow more, even if it is coming indirectly        
        newValue = new DHTValueEntityImpl(badHost, badHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false);;
        assertFalse(db.store(newValue));
        
        //but should allow one created by a good host
        DHTValueEntity goodValue = new DHTValueEntityImpl(goodHost, goodHost, KUID.createRandomID(), 
                new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false);
        
        assertTrue(db.store(goodValue));

        //test banning now
        badHost = ContactFactory.createLiveContact(
                new InetSocketAddress("169.0.1.3", 1111), Vendor.UNKNOWN, Version.UNKNOWN, 
                KUID.createRandomID(), 
                new InetSocketAddress("169.0.1.3", 1111), 1, 0);
        
        for(int i = 0; i <= DatabaseSettings.MAX_KEY_PER_IP_BAN_LIMIT.getValue(); i++) {
            value = new DHTValueEntityImpl(badHost, badHost, KUID.createRandomID(), 
                    new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false);
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
    
    private class HostFilterStub implements HostFilter{
        
        private Set<SocketAddress> bannedHosts = new HashSet<SocketAddress>();
        
        public boolean allow(DHTMessage message) {
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
