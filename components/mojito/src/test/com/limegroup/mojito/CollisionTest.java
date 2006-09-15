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
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestSuite;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.settings.RouteTableSettings;


public class CollisionTest extends BaseTestCase {
    
    private static final int PORT = 4000;
    
    public CollisionTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(CollisionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSpoof() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        
        MojitoDHT bootstrap = null, original = null, spoofer = null;
        
        try {
            bootstrap = MojitoFactory.createDHT("Bootstrap Node");
            bootstrap.bind(new InetSocketAddress("localhost", PORT));
            bootstrap.start();
            
            original = MojitoFactory.createDHT("OriginalDHT");
            original.bind(new InetSocketAddress(PORT+1));
            original.start();
            original.bootstrap(bootstrap.getContactAddress()).get();
            bootstrap.bootstrap(original.getContactAddress()).get();
            
            // The spoofer Node
            spoofer = MojitoFactory.createDHT("Spoofer Node");
            
            assertNotEquals(original.getLocalNodeID(), spoofer.getLocalNodeID());
            Method m = spoofer.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
            m.setAccessible(true);
            m.invoke(spoofer, new Object[]{ original.getLocalNodeID() });
            assertEquals(original.getLocalNodeID(), spoofer.getLocalNodeID());
            
            spoofer.bind(new InetSocketAddress(PORT+2));
            spoofer.start();
            spoofer.bootstrap(bootstrap.getContactAddress()).get();
            Thread.sleep(500);
            
            assertNotEquals(original.getLocalNodeID(), spoofer.getLocalNodeID());
            assertNotEquals(original.getContactAddress(), spoofer.getContactAddress());
            
            Context context = (Context)bootstrap;
            List<Contact> nodes = context.getRouteTable().getContacts();
            
            // Precondition: LocalContact.equals(LiveContact) == false
            // Copy the fields we're interested in into a Map and perform
            // all tests on the Map rather than the actual List of Contacts!
            Map<KUID, SocketAddress> map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getNodeID(), node.getContactAddress()));
            }
            
            assertContains(map.keySet(), bootstrap.getLocalNodeID());
            assertEquals(map.get(bootstrap.getLocalNodeID()), bootstrap.getContactAddress());
            
            assertContains(map.keySet(), original.getLocalNodeID());
            assertEquals(map.get(original.getLocalNodeID()), original.getContactAddress());
            
            assertContains(map.keySet(), spoofer.getLocalNodeID());
            assertEquals(map.get(spoofer.getLocalNodeID()), spoofer.getContactAddress());
            
        } finally {
            if (bootstrap != null) {
                bootstrap.stop();
            }
            
            if (original != null) {
                original.stop();
            }
            
            if (spoofer != null) {
                spoofer.stop();
            }
        }
    }
    
    public void testReplace() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        NetworkSettings.TIMEOUT.setValue(100L);
        NetworkSettings.MAX_ERRORS.setValue(0);
        
        MojitoDHT bootstrap = null, original = null, replacement = null;
        
        try {
            bootstrap = MojitoFactory.createDHT("Bootstrap-DHT");
            bootstrap.bind(new InetSocketAddress(PORT));
            bootstrap.start();
            
            original = MojitoFactory.createDHT("OriginalDHT");
            original.bind(new InetSocketAddress(PORT+1));
            original.start();
            original.bootstrap(bootstrap.getContactAddress()).get();
            bootstrap.bootstrap(original.getContactAddress()).get();
            
            original.stop();
            
            List<Contact> nodes = ((Context)bootstrap).getRouteTable().getContacts();
            
            // Precondition: LocalContact.equals(LiveContact) == false
            // Copy the fields we're interested in into a Map and perform
            // all tests on the Map rather than the actual List of Contacts!
            Map<KUID, SocketAddress> map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getNodeID(), node.getContactAddress()));
            }
            
            assertContains("Bootstrap Node does not have the new Node in its RT!", map.keySet(), original.getLocalNodeID());
            assertEquals(map.get(original.getLocalNodeID()), original.getContactAddress());
            
            // The replacement Node
            replacement = MojitoFactory.createDHT("ReplacementDHT");
            assertNotEquals(original.getLocalNodeID(), replacement.getLocalNodeID());
            Method m = replacement.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
            m.setAccessible(true);
            m.invoke(replacement, new Object[]{ original.getLocalNodeID() });
            assertEquals(original.getLocalNodeID(), replacement.getLocalNodeID());
            
            replacement.bind(new InetSocketAddress(PORT+2));
            replacement.start();
            replacement.bootstrap(bootstrap.getContactAddress()).get();
            
            Thread.sleep(5L * NetworkSettings.TIMEOUT.getValue());
            
            nodes = ((Context)bootstrap).getRouteTable().getContacts();
            map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getNodeID(), node.getContactAddress()));
            }
            
            assertContains("Bootstrap Node does not have the new Node in its RT!", map.keySet(), replacement.getLocalNodeID());
            assertEquals(map.get(replacement.getLocalNodeID()), replacement.getContactAddress());
            
            // The original Contact shouldn't be no longer there
            assertNotEquals(map.get(original.getLocalNodeID()), original.getContactAddress());
            
        } finally {
            
            if (bootstrap != null) {
                bootstrap.stop();
            }
            
            if (original != null) {
                original.stop();
            }
            
            if (replacement != null) {
                replacement.stop();
            }
        }
    }
}
