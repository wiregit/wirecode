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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.RouteTableSettings;


public class RoutingTableTest extends BaseTestCase {
    
    private static InetSocketAddress addr = new InetSocketAddress("localhost",3000);

    private MojitoDHT dht = null;
    private RouteTable routingTable = null;
    
    public RoutingTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RoutingTableTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    protected void setUp() throws Exception {
        KademliaSettings.REPLICATION_PARAMETER.setValue(2);
        RouteTableSettings.MAX_LIVE_NODE_FAILURES.setValue(2);
       
        dht = new MojitoDHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            fail(e);
        }
        
        dht.start();
        routingTable = dht.getContext().getRouteTable();
    }

    protected void tearDown() throws Exception {
        dht.stop();
        
        dht = null;
        routingTable = null;
        
        Thread.sleep(3000);
    }

    public void testLiveNodesOnly() {
        ContactNode node = new ContactNode(KUID.createRandomNodeID(), addr);
        routingTable.addContactToBucket(node,true);
        node = new ContactNode(KUID.createRandomNodeID(), addr);
        routingTable.addContactToBucket(node,false);
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        
        try {
            routingTable.refreshBuckets(false);
        } catch (IOException e) {
            fail(e);
        }
    }
    
    public void testBuckets() {
        for (int i = 0; i < 22; i++) {
            ContactNode node = new ContactNode(KUID.createRandomNodeID(), addr);
            routingTable.addContactToBucket(node,true);
            System.out.println(routingTable.toString());
        }
        Collection bucketsList = routingTable.getAllBuckets();
        StringBuffer buffer = new StringBuffer("\n");
        buffer.append("-------------\nBuckets:\n");
        int totalNodesInBuckets = 0;
        for(Iterator it = bucketsList.iterator(); it.hasNext(); ) {
            BucketNode bucket = (BucketNode)it.next();
            buffer.append(bucket).append("\n");
            totalNodesInBuckets += bucket.getNodeCount();
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL BUCKETS: " + bucketsList.size()).append(" NUM. OF NODES: "+totalNodesInBuckets+"\n");
        buffer.append("-------------\n");
        
        Collection nodesList = routingTable.getAllNodes();
        buffer.append("-------------\nNodes:\n");
        for(Iterator it = nodesList.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            buffer.append(node).append("\n");
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL NODES: " + nodesList.size()).append("\n");
        buffer.append("-------------\n");
        System.out.println(buffer);
    }
    
    public void testReplaceCachedNode() {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        for (int i = 0; i < 2; i++) {
            ContactNode node = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 3000+i));
            routingTable.addContactToBucket(node,true);
        }
        
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 30010));
        routingTable.addContactToBucket(node1, true);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        routingTable.addContactToBucket(node1, true);
    }
    
    public void testreplaceBucketStaleNodes() {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 30010));
        node1.failure();
        node1.failure();
        node1.failure();
        routingTable.addContactToBucket(node1, false);
        
        for (int i = 0; i < 2; i++) {
            ContactNode node = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 3000+i));
            routingTable.addContactToBucket(node, true);
        }
    }
    
    public void testreplaceBucketUnknownNodes() {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 30010));
        routingTable.addContactToBucket(node1, true);
        System.out.println(routingTable.toString());
        ContactNode node2 = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 30011));
        routingTable.addContactToBucket(node2, true);
        System.out.println(routingTable.toString());
        ContactNode node3 = new ContactNode(KUID.createPrefxNodeID(prefix, 4), new InetSocketAddress("localhost", 30012));
        routingTable.addContactToBucket(node3, true);
        System.out.println(routingTable.toString());
    }
    
    public void testReplaceNode() {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix, 4), addr);
        routingTable.addContactToBucket(node1,true);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        routingTable.addContactToBucket(node1, true);
    }

    public void testRemoveNode() {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix,4),addr);
        routingTable.addContactToBucket(node1, true);
        for (int i = 0; i < 20; i++) {
            ContactNode node = new ContactNode(KUID.createPrefxNodeID(prefix,4),addr);
            routingTable.addContactToBucket(node, true);
        }
        routingTable.handleFailure(node1.getNodeID());
        routingTable.handleFailure(node1.getNodeID());
        routingTable.handleFailure(node1.getNodeID());
    }
    

    public void testStoreLoadRoutingTable() throws Exception {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node3;
        ContactNode node4;
        
        try {
            ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix,4), new InetSocketAddress("localhost", 30010));
            routingTable.addContactToBucket(node1, true);
            Thread.sleep(50);
            ContactNode node2 = new ContactNode(KUID.createPrefxNodeID(prefix,4), new InetSocketAddress("localhost", 30011));
            routingTable.addContactToBucket(node2, true);
            Thread.sleep(50);
            node3 = new ContactNode(KUID.createPrefxNodeID(prefix,4), new InetSocketAddress("localhost", 30012));
            routingTable.addContactToBucket(node3, true);
            Thread.sleep(50);
            node4 = new ContactNode(KUID.createPrefxNodeID(prefix,4), new InetSocketAddress("localhost", 30013));
            routingTable.addContactToBucket(node4, true);
            node4.failure();
            Thread.sleep(50);
            node1.failure();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        File file = new File("mojito.state");
        
        System.out.println(routingTable.toString());
        
        FileOutputStream out = new FileOutputStream(file);
        dht.store(out, true, true);
        out.close();
        
        routingTable.clear();
        System.out.println(routingTable.toString());
        
        try {
            dht.stop();
            
            FileInputStream in = new FileInputStream(file);
            dht = MojitoDHT.load(in);
            in.close();
            
            KUID newNodeId = KUID.createRandomNodeID();
            ContextSettings.setLocalNodeID(addr, newNodeId.getBytes());
            dht.bind(addr);
            routingTable = dht.getContext().getRouteTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        System.out.println(routingTable.toString());
    }
    
    // Bucket merging is not implemeneted!
    /*public void testMergeBuckets() {
        // byte[] prefix = new byte[1];
        // prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress("localhost", 30010));
        routingTable.add(node1,true);
        ContactNode node2 = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress("localhost", 30010));
        routingTable.add(node2,true);
        ContactNode node3 = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress("localhost", 30010));
        routingTable.add(node3,true);
        ContactNode node4 = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress("localhost", 30010));
        routingTable.add(node4,true);
        ContactNode node5 = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress("localhost", 30010));
        routingTable.add(node5,true);
        ContactNode node6 = new ContactNode(KUID.createRandomNodeID(),new InetSocketAddress("localhost", 30010));
        routingTable.add(node6,true);
        
        System.out.println(routingTable.toString());
        
        routingTable.handleFailure(node4.getNodeID());
        routingTable.handleFailure(node4.getNodeID());
        routingTable.handleFailure(node5.getNodeID());
        routingTable.handleFailure(node5.getNodeID());
        
        System.out.println(routingTable.toString());
    }*/
}
