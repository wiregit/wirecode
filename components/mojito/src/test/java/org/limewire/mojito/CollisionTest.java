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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.settings.RouteTableSettings;
import org.limewire.mojito2.util.ExceptionUtils;
import org.limewire.mojito2.util.IoUtils;


public class CollisionTest extends MojitoTestCase {
    
    private static final int PORT = 3000;
    
    public CollisionTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(CollisionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
        /*
         * replacement of ALIVE nodes will work only if the previous node properly
         * sent a shut down message or incoming requests generate ALIVE contacts.
         */
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(1);
        // or set
        // RouteTableSettings.INCOMING_REQUESTS_UNKNOWN.setValue(false);
    }

    public void testSpoof() throws Exception {
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(1);
        
        MojitoDHT bootstrap = null, original = null, spoofer = null;
        
        try {
            bootstrap = MojitoFactory.createDHT("Bootstrap Node", PORT);
            original = MojitoFactory.createDHT("OriginalDHT", PORT+1);
            spoofer = MojitoFactory.createDHT("Spoofer Node", PORT+2);
            
            original.bootstrap("localhost", PORT).get();
            bootstrap.bootstrap("localhost", PORT+1).get();
            
            assertNotEquals(original.getLocalNodeID(), spoofer.getLocalNodeID());
            
            spoofer.setContactId(original.getLocalNodeID());
            assertEquals(original.getLocalNodeID(), spoofer.getLocalNodeID());
            
            try {
                spoofer.bootstrap("localhost", PORT).get();
                fail("Shoud have failed!");
            } catch (ExecutionException err) {
                // Make sure it was a CollisionException
                CollisionException ex = ExceptionUtils.getCause(
                        err, CollisionException.class);
                assertNotNull("Expected a CollisionException", ex);
                
                Contact cause = ex.getContact();
                
                assertEquals(original.getLocalNodeID(), cause.getNodeID());
                assertEquals(original.getContactAddress(), cause.getContactAddress());
            }
            
            Collection<Contact> nodes = bootstrap.getRouteTable().getContacts();
            
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
            assertNotEquals(map.get(spoofer.getLocalNodeID()), spoofer.getContactAddress());
            
        } finally {
            IoUtils.closeAll(bootstrap, original, spoofer);
        }
    }
    
    public void testReplace() throws Exception {
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        NetworkSettings.DEFAULT_TIMEOUT.setValue(100L);
        NetworkSettings.MAX_ERRORS.setValue(0);
        
        MojitoDHT bootstrap = null, original = null, replacement = null;
        
        try {
            bootstrap = MojitoFactory.createDHT("Bootstrap-DHT", PORT);
            original = MojitoFactory.createDHT("OriginalDHT", PORT+1);
            
            original.bootstrap("localhost", PORT).get();
            bootstrap.bootstrap("localhost", PORT+1).get();
            replacement = MojitoFactory.createDHT("ReplacementDHT", PORT+2);
            
            original.unbind();
            
            Collection<Contact> nodes = bootstrap.getRouteTable().getContacts();
            
            // Precondition: LocalContact.equals(RemoteContact) == false
            // Copy the fields we're interested in into a Map and perform
            // all tests on the Map rather than the actual List of Contacts!
            Map<KUID, SocketAddress> map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getNodeID(), node.getContactAddress()));
            }
            
            assertContains("Bootstrap Node does not have the new Node in its RT!", map.keySet(), original.getLocalNodeID());
            assertEquals(map.get(original.getLocalNodeID()), original.getContactAddress());
            
            // The replacement Node
            assertNotEquals(original.getLocalNodeID(), replacement.getLocalNodeID());
            replacement.setContactId(original.getLocalNodeID());
            assertEquals(original.getLocalNodeID(), replacement.getLocalNodeID());
            
            // Bootstrap the replacement Contact
            replacement.bootstrap("localhost", PORT).get();
            
            Thread.sleep(5L * NetworkSettings.DEFAULT_TIMEOUT.getValue());
            
            nodes = bootstrap.getRouteTable().getContacts();
            map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getNodeID(), node.getContactAddress()));
            }
            
            assertContains("Bootstrap Node does not have the new Node in its RT!", map.keySet(), replacement.getLocalNodeID());
            assertEquals(map.get(replacement.getLocalNodeID()), replacement.getContactAddress());
            
            // The original Contact shouldn't be no longer there
            assertNotEquals(map.get(original.getLocalNodeID()), original.getContactAddress());
            
        } finally {
            IoUtils.closeAll(bootstrap, original, replacement);
        }
    }
}
