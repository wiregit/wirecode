package org.limewire.mojito.routing.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.ClassfulNetworkCounter;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.RouteTableSettings;


public class BucketNodeTest extends MojitoTestCase {
    
    private Contact localNode;
    
    private RouteTableImpl routeTable;
    
    private Vendor vendor;
    private Version version;
    
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
        vendor = ContextSettings.getVendor();
        version = ContextSettings.getVersion();
        
        routeTable = new RouteTableImpl();
        localNode = routeTable.getLocalNode();
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
                KUID.createRandomID(), address, 0, Contact.DEFAULT_FLAG, State.DEAD);
        
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
                KUID.createRandomID(), address, 0, Contact.DEFAULT_FLAG, State.DEAD);
        bucket.addActiveContact(node2);
        
        Contact node3 = new RemoteContact(address, vendor, version, 
                KUID.createRandomID(), address, 0, Contact.DEFAULT_FLAG, State.ALIVE);
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
    
    public void testUpdateClassfulNetworkCounter() {
    	RouteTable routeTable = new RouteTableImpl();
    	Bucket bucket = routeTable.getBucket(KUID.MINIMUM);
    	ClassfulNetworkCounter counter = bucket.getClassfulNetworkCounter();
    	assertEquals(0, counter.size());
    	
    	KUID nodeId1 = KUID.createRandomID();
    	Contact node = ContactFactory.createUnknownContact(
    			Vendor.UNKNOWN, Version.ZERO, nodeId1, new InetSocketAddress("localhost", 5000));
    	bucket.addActiveContact(node);
    	assertEquals(1, counter.size());
    	
    	node = ContactFactory.createUnknownContact(
    			Vendor.UNKNOWN, Version.ZERO, nodeId1, new InetSocketAddress("www.apple.com", 5000));
    	bucket.updateContact(node);
    	assertEquals(1, counter.size());
    	
    	KUID nodeId2 = KUID.createRandomID();
    	node = ContactFactory.createUnknownContact(
    			Vendor.UNKNOWN, Version.ZERO, nodeId2, new InetSocketAddress("www.apple.com", 5000));
    	bucket.addActiveContact(node);
    	assertEquals(3, bucket.size()); // local Node, first Node and this Node
    	assertEquals(1, counter.size());
    	
    	KUID nodeId3 = KUID.createRandomID();
    	node = ContactFactory.createUnknownContact(
    			Vendor.UNKNOWN, Version.ZERO, nodeId3, new InetSocketAddress("www.google.com", 5000));
    	bucket.addActiveContact(node);
    	assertEquals(4, bucket.size());
    	assertEquals(2, counter.size());
    }
    
    public void testLeastAndMostRecentlySeenActiveContact() {
        RouteTableImpl routeTable = new RouteTableImpl();
        Bucket bucket = routeTable.getBucket(KUID.MINIMUM);
        bucket.clear();
        
        assertEquals(0, bucket.getActiveSize());
        
        Contact leastRecentlySeen = null;
        Contact mostRecentlySeen = null;
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        assertGreaterThan(0, k);
        for (int i = 0; i < k; i++) {
            Contact node = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 6000 + i));
            node.setTimeStamp(/* fake time */ i);
            
            if (leastRecentlySeen == null) {
                leastRecentlySeen = node;
            }
            
            mostRecentlySeen = node;
            bucket.addActiveContact(node);
        }
        assertEquals(k, bucket.getActiveSize());
        
        // Test initial State
        assertSame(leastRecentlySeen, bucket.getLeastRecentlySeenActiveContact());
        assertSame(mostRecentlySeen, bucket.getMostRecentlySeenActiveContact());
        
        // Touch the least recently seen Node which makes it the 
        // most recently seen Node
        assert leastRecentlySeen != null; // avoid NPE warning
        leastRecentlySeen.setTimeStamp(/* fake time */ Integer.MAX_VALUE);
        assertSame(leastRecentlySeen, bucket.getMostRecentlySeenActiveContact());
        assertNotSame(mostRecentlySeen, bucket.getMostRecentlySeenActiveContact());
    }
    
    public void testLeastAndMostRecentlySeenCachedContact() {
        RouteTableImpl routeTable = new RouteTableImpl();
        Bucket bucket = routeTable.getBucket(KUID.MINIMUM);
        
        Contact leastRecentlySeen = null;
        Contact mostRecentlySeen = null;
        int maxCacheSize = RouteTableSettings.MAX_CACHE_SIZE.getValue();
        for (int i = 0; i < maxCacheSize; i++) {
            Contact node = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 6000 + i));
            
            if (leastRecentlySeen == null) {
                leastRecentlySeen = node;
            }
            
            mostRecentlySeen = node;
            bucket.addCachedContact(node);
        }
        assertEquals(maxCacheSize, bucket.getCacheSize());
        
        // Test initial State
        assertSame(leastRecentlySeen, bucket.getLeastRecentlySeenCachedContact());
        assertSame(mostRecentlySeen, bucket.getMostRecentlySeenCachedContact());
        
        // Touch the least recently seen Node which makes it the 
        // most recently seen Node
        bucket.addCachedContact(leastRecentlySeen);
        assertSame(leastRecentlySeen, bucket.getMostRecentlySeenCachedContact());
        assertNotSame(mostRecentlySeen, bucket.getMostRecentlySeenCachedContact());
    }
}
