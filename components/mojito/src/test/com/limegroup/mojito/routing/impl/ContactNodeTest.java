package com.limegroup.mojito.routing.impl;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.ContactFactory;

public class ContactNodeTest extends BaseTestCase {
    
    public ContactNodeTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(ContactNodeTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testFixSourceAndContactAddress() {
        // External Port == 0 -> force firewalled if it isn't!
        ContactNode node1 = (ContactNode)ContactFactory.createLiveContact(
                null, 0, 0, KUID.createRandomNodeID(), 
                new InetSocketAddress("localhost", 0), 0, false);
        
        assertNull(node1.getSourceAddress());
        assertFalse(node1.isFirewalled());
        
        node1.fixSourceAndContactAddress(new InetSocketAddress("localhost", 1024));
        
        assertNotNull(node1.getSourceAddress());
        assertTrue(node1.isFirewalled());
        
        assertEquals(new InetSocketAddress("localhost", 1024), node1.getSourceAddress());
        assertEquals(new InetSocketAddress("localhost", 1024), node1.getContactAddress());
        
        // External Port != 0 -> Contact Address = Source Address + external Port
        ContactNode node2 = (ContactNode)ContactFactory.createLiveContact(
                null, 0, 0, KUID.createRandomNodeID(), 
                new InetSocketAddress("dell.com", 2048), 0, false);
        
        assertNull(node2.getSourceAddress());
        assertFalse(node2.isFirewalled());
        
        node2.fixSourceAndContactAddress(new InetSocketAddress("localhost", 1024));
        
        assertNotNull(node2.getSourceAddress());
        assertFalse(node2.isFirewalled());
        
        assertEquals(new InetSocketAddress("localhost", 1024), node2.getSourceAddress());
        assertEquals(new InetSocketAddress("localhost", 2048), node2.getContactAddress());
        
        // Fix works only once
        node2.fixSourceAndContactAddress(new InetSocketAddress("dell.com", 8096));
        assertEquals(new InetSocketAddress("localhost", 1024), node2.getSourceAddress());
        assertEquals(new InetSocketAddress("localhost", 2048), node2.getContactAddress());
    }
    
    public void testUpdateWithExistingContact() {
        KUID nodeId = KUID.createRandomNodeID();
        
        ContactNode node1 = (ContactNode)ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                0, 0, nodeId, 
                new InetSocketAddress("localhost", 2048), 
                0, false);
        
        node1.setTimeStamp(100);
        node1.setRoundTripTime(100);
        node1.handleFailure();
        
        ContactNode node2 = (ContactNode)ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                0, 0, nodeId, 
                new InetSocketAddress("localhost", 2048), 
                0, false);
        
        node2.setTimeStamp(99); // Node #2 is elder than Node #1
        
        assertEquals(-1, node2.getRoundTripTime());
        assertEquals(0, node2.getFailures());
        assertEquals(0, node2.getLastFailedTime());
        assertTrue(node2.getTimeStamp() > 0 && node2.getTimeStamp() < Long.MAX_VALUE);
        
        node2.updateWithExistingContact(node1);
        
        assertEquals(100, node2.getRoundTripTime());
        assertEquals(1, node2.getFailures());
        assertEquals(node1.getLastFailedTime(), node2.getLastFailedTime());
        assertEquals(node1.getTimeStamp(), node2.getTimeStamp());
        
        // Make sure update works only with Contacts that have the same Node ID
        try {
            ContactNode node4 = (ContactNode)ContactFactory.createLiveContact(
                    new InetSocketAddress("localhost", 1024), 
                    0, 0, KUID.createRandomNodeID(), 
                    new InetSocketAddress("localhost", 2048), 
                    0, false);
            
            node4.updateWithExistingContact(node2);
            fail(node4 + " should have rejected the exsting Node " + node2);
        } catch (IllegalArgumentException ignore) {}
    }
    
    public void testVendorAndVersion() {
        Contact node1 = ContactFactory.createLocalContact(1, 2, false);
        
        assertEquals(1, node1.getVendor());
        assertEquals(2, node1.getVersion());
        
        Contact node2 = ContactFactory.createLiveContact(
                new InetSocketAddress("localhost", 1024), 
                3, 4, KUID.createRandomNodeID(), 
                new InetSocketAddress("localhost", 2048), 
                0, false);
        
        assertEquals(3, node2.getVendor());
        assertEquals(4, node2.getVersion());
        
        Contact node3 = ContactFactory.createUnknownContact(
                5, 6, KUID.createRandomNodeID(), 
                new InetSocketAddress("localhost", 2048));
        
        assertEquals(5, node3.getVendor());
        assertEquals(6, node3.getVersion());
    }
}
