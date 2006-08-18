package com.limegroup.mojito.routing.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.RouteTable.Callback;
import com.limegroup.mojito.settings.ContextSettings;

public class BucketNodeTest extends BaseTestCase {
    
    private Contact localNode;
    
    private int vendor,version;
    
    public BucketNodeTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(BucketNodeTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        vendor = ContextSettings.VENDOR.getValue();
        version = ContextSettings.VERSION.getValue();
        
        KUID nodeId = KUID.createRandomNodeID();
        int instanceId = 0;
        
        localNode = ContactFactory.createLocalContact(vendor, version, nodeId, instanceId, false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testPurge() {
        RouteTable routeTable = new RouteTableImpl();
        routeTable.setRouteTableCallback(new Callback() {
            public Contact getLocalNode() {
                return localNode;
            }
            
            public DHTFuture<Contact> ping(Contact node) {
                throw new UnsupportedOperationException();
            }
            
        });
        
        SocketAddress address = new InetSocketAddress("localhost", 2000);
        Bucket bucket = new BucketNode(routeTable, KUID.MINIMUM, 0);
        bucket.addLiveContact(localNode);
        //try purging bucket with only local node
        bucket.purge();
        assertTrue("should contain local node", bucket.getLiveContacts().contains(localNode));
        
        //now add dead and unknown nodes
        Contact node = ContactFactory.createUnknownContact(
                vendor, version, KUID.createRandomNodeID(), address);
        
        bucket.addLiveContact(node);
        Contact node2 = new ContactNode(address, vendor, version, 
                KUID.createRandomNodeID(), address, 0, false, State.DEAD);
        
        bucket.addLiveContact(node2);
        assertEquals(bucket.getLiveContacts().size(), 3);
        bucket.purge();
        assertTrue("should contain local node", bucket.getLiveContacts().contains(localNode));
        assertEquals(bucket.getLiveContacts().size(), 1);
        //now add cached node
        node = ContactFactory.createUnknownContact(
                vendor, version, KUID.createRandomNodeID(), address);
        
        bucket.addLiveContact(node);
        node2 = new ContactNode(address, vendor, version, 
                KUID.createRandomNodeID(), address, 0, false, State.DEAD);
        bucket.addLiveContact(node2);
        
        Contact node3 = new ContactNode(address, vendor, version, 
                KUID.createRandomNodeID(), address, 0, false, State.ALIVE);
        bucket.addCachedContact(node3);
        
        assertEquals(bucket.getLiveContacts().size(), 3);
        bucket.purge();
        assertTrue(bucket.getLiveContacts().contains(node3));
        assertFalse(bucket.getCachedContacts().contains(node3));
        
    }

}
