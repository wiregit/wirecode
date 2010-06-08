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

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.io.IOUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.util.ExceptionUtils;


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
        
        MojitoDHT bootstrap = null;
        MojitoDHT original = null;
        MojitoDHT spoofer = null;
        
        try {
            bootstrap = MojitoFactory.createDHT("Bootstrap Node", PORT);
            original = MojitoFactory.createDHT("OriginalDHT", PORT+1);
            spoofer = MojitoFactory.createDHT("Spoofer Node", PORT+2);
            
            original.bootstrap("localhost", PORT).get();
            bootstrap.bootstrap("localhost", PORT+1).get();
            
            assertNotEquals(original.getContactId(), spoofer.getContactId());
            
            spoofer.setContactId(original.getContactId());
            assertEquals(original.getContactId(), spoofer.getContactId());
            
            try {
                spoofer.bootstrap("localhost", PORT).get();
                fail("Shoud have failed!");
            } catch (ExecutionException err) {
                // Make sure it was a CollisionException
                CollisionException ex = ExceptionUtils.getCause(
                        err, CollisionException.class);
                assertNotNull("Expected a CollisionException", ex);
                
                Contact cause = ex.getContact();
                
                assertEquals(original.getContactId(), cause.getContactId());
                assertEquals(original.getContactAddress(), cause.getContactAddress());
            }
            
            Collection<Contact> nodes = bootstrap.getRouteTable().getContacts();
            
            // Precondition: LocalContact.equals(LiveContact) == false
            // Copy the fields we're interested in into a Map and perform
            // all tests on the Map rather than the actual List of Contacts!
            Map<KUID, SocketAddress> map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getContactId(), node.getContactAddress()));
            }
            
            assertContains(map.keySet(), bootstrap.getContactId());
            assertEquals(map.get(bootstrap.getContactId()), bootstrap.getContactAddress());
            
            assertContains(map.keySet(), original.getContactId());
            assertEquals(map.get(original.getContactId()), original.getContactAddress());
            
            assertContains(map.keySet(), spoofer.getContactId());
            assertNotEquals(map.get(spoofer.getContactId()), spoofer.getContactAddress());
            
        } finally {
            IOUtils.close(bootstrap, original, spoofer);
        }
    }
    
    public void testReplace() throws Exception {
        RouteTableSettings.MIN_RECONNECTION_TIME.setValue(0);
        NetworkSettings.DEFAULT_TIMEOUT.setValue(100L);
        NetworkSettings.MAX_ERRORS.setValue(0);
        
        MojitoDHT bootstrap = null;
        MojitoDHT original = null;
        MojitoDHT replacement = null;
        
        try {
            bootstrap = MojitoFactory.createDHT("Bootstrap-DHT", PORT);
            original = MojitoFactory.createDHT("OriginalDHT", PORT+1);
            
            original.bootstrap("localhost", PORT).get();
            bootstrap.bootstrap("localhost", PORT+1).get();
            replacement = MojitoFactory.createDHT("ReplacementDHT", PORT+2);
            
            Transport transport = original.unbind();
            if (transport instanceof Closeable) {
                IOUtils.close((Closeable)transport);
            }
            
            Collection<Contact> nodes = bootstrap.getRouteTable().getContacts();
            
            // Precondition: LocalContact.equals(RemoteContact) == false
            // Copy the fields we're interested in into a Map and perform
            // all tests on the Map rather than the actual List of Contacts!
            Map<KUID, SocketAddress> map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getContactId(), node.getContactAddress()));
            }
            
            assertContains("Bootstrap Node does not have the new Node in its RT!", map.keySet(), original.getContactId());
            assertEquals(map.get(original.getContactId()), original.getContactAddress());
            
            // The replacement Node
            assertNotEquals(original.getContactId(), replacement.getContactId());
            replacement.setContactId(original.getContactId());
            assertEquals(original.getContactId(), replacement.getContactId());
            
            // Bootstrap the replacement Contact
            replacement.bootstrap("localhost", PORT).get();
            
            Thread.sleep(5L * NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis());
            
            nodes = bootstrap.getRouteTable().getContacts();
            map = new HashMap<KUID, SocketAddress>();
            for (Contact node : nodes) {
                assertNull(map.put(node.getContactId(), node.getContactAddress()));
            }
            
            assertContains("Bootstrap Node does not have the new Node in its RT!", map.keySet(), replacement.getContactId());
            assertEquals(map.get(replacement.getContactId()), replacement.getContactAddress());
            
            // The original Contact shouldn't be no longer there
            assertNotEquals(map.get(original.getContactId()), original.getContactAddress());
            
        } finally {
            IOUtils.close(bootstrap, original, replacement);
        }
    }
}
