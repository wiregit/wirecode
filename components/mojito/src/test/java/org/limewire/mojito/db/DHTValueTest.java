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
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.impl.DHTValueEntityImpl;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.DatabaseUtils;

public class DHTValueTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/

    public DHTValueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testLocationCount() throws Exception {
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        MojitoDHT first = null;
        try {
            for (int i = 0; i < 2*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(new InetSocketAddress(2000 + i));
                dht.start();
                
                if (i > 0) {
                    dht.bootstrap(new InetSocketAddress("localhost", 2000)).get();
                } else {
                    first = dht;
                }
                dhts.put(dht.getLocalNodeID(), dht);
            }
            first.bootstrap(new InetSocketAddress("localhost", 2000+1)).get();
            Thread.sleep(250);
            
            KUID key = KUID.createRandomID();
            DHTValueType type = DHTValueType.TEST;
            Version version = Version.ZERO;
            byte[] b = "Hello World".getBytes();
            
            long time = System.currentTimeMillis();
            DHTValueEntity value = new DHTValueEntityImpl(
                    first.getLocalNode(), first.getLocalNode(), key, 
                        new DHTValueImpl(type, version, b), true);
            
            // Pre-Condition
            assertEquals(0, value.getLocations().size());
            assertEquals(0L, value.getPublishTime());
            assertTrue(DatabaseUtils.isPublishingRequired(value));
            
            // Store...
            StoreResult result = ((Context)first).store(value).get();
            
            // Post-Condition
            assertSame(value, result.getValues().iterator().next());
            assertEquals(k, value.getLocations().size());
            assertGreaterThanOrEquals(time, value.getPublishTime());
            assertFalse(DatabaseUtils.isPublishingRequired(value));
            
        } finally {
            for (MojitoDHT dht : dhts.values()) {
                dht.close();
            }
        }
    }
}
