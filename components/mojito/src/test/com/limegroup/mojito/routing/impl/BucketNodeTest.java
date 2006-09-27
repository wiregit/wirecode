package com.limegroup.mojito.routing.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.event.PingEvent;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.routing.RouteTable.Callback;
import com.limegroup.mojito.settings.ContextSettings;

public class BucketNodeTest extends BaseTestCase {
    
    private Contact localNode;
    
    private RouteTableImpl routeTable;
    
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
        
        KUID nodeId = KUID.createRandomID();
        int instanceId = 0;
        
        localNode = ContactFactory.createLocalContact(vendor, version, nodeId, 
        		instanceId, false);
        
        routeTable = new RouteTableImpl();
        routeTable.setRouteTableCallback(new Callback() {
            public DHTFuture<PingEvent> ping(Contact node) {
                throw new UnsupportedOperationException();
            }
            
        });
        routeTable.add(localNode);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testPurge() {
        Bucket bucket = routeTable.getBucket(localNode.getNodeID());
        
        //try purging bucket with only local node
        bucket.purge();
        assertTrue("should contain local node", bucket.getActiveContacts().contains(localNode));
        
        SocketAddress address = new InetSocketAddress("localhost", 2000);
        
        //now add dead and unknown nodes
        Contact node = ContactFactory.createUnknownContact(
                vendor, version, KUID.createRandomID(), address);
        
        bucket.addActiveContact(node);
        Contact node2 = new RemoteContact(address, vendor, version, 
                KUID.createRandomID(), address, 0, false, State.DEAD);
        
        bucket.addActiveContact(node2);
        assertEquals(bucket.getActiveContacts().size(), 3);
        bucket.purge();
        assertTrue("should contain local node", bucket.getActiveContacts().contains(localNode));
        assertEquals(bucket.getActiveContacts().size(), 1);
        //now add cached node
        node = ContactFactory.createUnknownContact(
                vendor, version, KUID.createRandomID(), address);
        
        bucket.addActiveContact(node);
        node2 = new RemoteContact(address, vendor, version, 
                KUID.createRandomID(), address, 0, false, State.DEAD);
        bucket.addActiveContact(node2);
        
        Contact node3 = new RemoteContact(address, vendor, version, 
                KUID.createRandomID(), address, 0, false, State.ALIVE);
        bucket.addCachedContact(node3);
        
        assertEquals(bucket.getActiveContacts().size(), 3);
        bucket.purge();
        assertTrue(bucket.getActiveContacts().contains(node3));
        assertFalse(bucket.getCachedContacts().contains(node3));
        
    }
    
    public void testTouchBucket() throws Exception{
    	Bucket bucket = routeTable.getBucket(localNode.getNodeID());
    	
    	long now = System.currentTimeMillis();
    	assertEquals(0, bucket.getTimeStamp());
    	assertLessThan(now, bucket.getTimeStamp());
    	Thread.sleep(200);
    	bucket.touch();
    	Thread.sleep(200);
    	assertGreaterThan(now, bucket.getTimeStamp());
    	now = System.currentTimeMillis();
    	assertLessThan(now, bucket.getTimeStamp());
    }
}
