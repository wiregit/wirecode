package com.limegroup.mojito.routing.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.routing.ContactFactory;

public class LocalContactTest  extends BaseTestCase {
    
    public LocalContactTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(LocalContactTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSetExternalAddress() {
        LocalContact local = (LocalContact)ContactFactory.createLocalContact(0, 0, false);
        
        // Initial State
        assertEquals(new InetSocketAddress("localhost", 0), local.getContactAddress());
        
        // Set External Port
        local.setExternalPort(1024);
        assertEquals(new InetSocketAddress("localhost", 1024), local.getContactAddress());
        
        // Accept everyting
        SocketAddress externalAddress1 = new InetSocketAddress("limewire.org", 2048);
        local.setExternalAddress(externalAddress1);
        assertEquals(new InetSocketAddress("limewire.org", 1024), local.getContactAddress());
        
        // Two Nodes report different external addresses
        SocketAddress externalAddress2 = new InetSocketAddress("microsoft.com", 2048);
        local.setExternalAddress(externalAddress2);
        assertEquals(new InetSocketAddress("limewire.org", 1024), local.getContactAddress());
        
        SocketAddress externalAddress3 = new InetSocketAddress("dell.com", 2048);
        local.setExternalAddress(externalAddress3);
        assertEquals(new InetSocketAddress("limewire.org", 1024), local.getContactAddress());
        
        // We accept an address if the same address is reported by two Nodes in a row
        local.setExternalAddress(externalAddress3);
        assertEquals(new InetSocketAddress("dell.com", 1024), local.getContactAddress());
    }
}
