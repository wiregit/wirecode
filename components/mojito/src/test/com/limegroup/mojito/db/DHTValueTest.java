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
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.result.StoreResult;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.MojitoUtils;

public class DHTValueTest extends BaseTestCase {
    
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
    
    public void testLocationCount() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        MojitoDHT first = null;
        try {
            for (int i = 0; i < 2*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(new InetSocketAddress("localhost", 2000 + i));
                dht.start();
                
                if (i > 0) {
                    MojitoUtils.bootstrap(dht, new InetSocketAddress("localhost", 2000)).get();
                } else {
                    first = dht;
                }
                dhts.put(dht.getLocalNodeID(), dht);
            }
            MojitoUtils.bootstrap(first, new InetSocketAddress("localhost", 2000+1)).get();
            Thread.sleep(250);
            
            KUID key = KUID.createRandomID();
            DHTValueType type = DHTValueType.TEST;
            int version = 0;
            byte[] b = "Hello World".getBytes();
            
            long time = System.currentTimeMillis();
            DHTValue value = DHTValueFactory.createLocalValue(
                    first.getLocalNode(), key, type, version, b);
            
            // Pre-Condition
            assertEquals(0, value.getLocationCount());
            assertEquals(0L, value.getPublishTime());
            assertTrue(value.isRepublishingRequired());
            
            // Store...
            StoreResult result = ((Context)first).store(value).get();
            
            // Post-Condition
            assertEquals(0, result.getFailed().size());
            assertEquals(result.getValues().size(), result.getSucceeded().size());
            
            assertSame(value, result.getSucceeded().iterator().next());
            assertEquals(k, value.getLocationCount());
            assertGreaterThanOrEquals(time, value.getPublishTime());
            assertFalse(value.isRepublishingRequired());
            
        } finally {
            for (MojitoDHT dht : dhts.values()) {
                dht.stop();
            }
        }
    }
}
