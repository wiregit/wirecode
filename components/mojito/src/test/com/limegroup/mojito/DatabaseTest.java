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

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.routing.impl.ContactNode;

public class DatabaseTest extends BaseTestCase {
    
    private static InetSocketAddress addr = new InetSocketAddress("localhost",3000);
    
    private MojitoDHT dht = null;
    
    public DatabaseTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(DatabaseTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    protected void setUp() throws Exception {
        dht = MojitoFactory.createDHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            fail(e);
        }
        dht.start();
    }

    protected void tearDown() throws Exception {
        dht.stop();
        dht = null;
        
        Thread.sleep(3000);
    }
    
    public void testRemoveValueDB() throws Exception {
        Database db = ((Context)dht).getDatabase();
        KUID key = KUID.createRandomNodeID();
        byte[] value = "test".getBytes("UTF-8");
        
        KUID nodeId = KUID.createRandomNodeID();
        Contact originator = ContactNode.createLocalContact(0, 0, nodeId, 0);
        DHTValue dhtValue1 = DHTValue.createLocalValue(originator, key, value);
        db.store(dhtValue1);
        assertEquals(1, db.getKeyCount());
        
        // Direct remove request -> remove
        DHTValue dhtValue2 = DHTValue.createRemoteValue(originator, originator, key, new byte[0]);
        db.store(dhtValue2);
        assertEquals(0, db.getKeyCount());
        
        db.store(dhtValue1);
        assertEquals(1, db.getKeyCount());
        
        // Indirect remove request -> don't remove
        Contact sender = ContactNode.createLocalContact(0, 0, KUID.createRandomNodeID(), 0);
        DHTValue dhtValue3 = DHTValue.createRemoteValue(originator, sender, key, new byte[0]);
        db.store(dhtValue3);
        assertEquals(1, db.getKeyCount());
    }
}
