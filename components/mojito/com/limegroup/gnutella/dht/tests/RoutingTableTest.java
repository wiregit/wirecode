/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;

import de.kapsi.net.kademlia.BucketNode;
import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;

public class RoutingTableTest {

    private static InetSocketAddress addr = new InetSocketAddress("localhost",3000);
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        KademliaSettings.setReplicationParameter(2);
        RouteTableSettings.setMaxLiveNodeFailures(2);
        DHT dht = new DHT();
        try {
            dht.bind(addr);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new Thread(dht,"DHT").start();
        RoutingTable routingTable = dht.getContext().getRouteTable();
        
        testBuckets(routingTable);
//        testReplaceNode(routingTable);
//        testReplaceCachedNode(routingTable);
//        testRemoveNode(routingTable);
//        testLiveNodesOnly(routingTable);
//        testreplaceBucketStaleNodes(routingTable);
        
        System.out.println("LOCAL NODE:"+dht.getLocalNode());
        try {
            dht.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }
    
    public static void testLiveNodesOnly(RoutingTable routingTable) {
        ContactNode node = new ContactNode(KUID.createRandomNodeID(addr),addr);
        routingTable.add(node,true);
        node = new ContactNode(KUID.createRandomNodeID(addr),addr);
        routingTable.add(node,false);
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        routingTable.handleFailure(node.getNodeID());
        try {
            routingTable.refreshBuckets(false);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void testBuckets(RoutingTable routingTable) {
        for (int i = 0; i < 22; i++) {
            ContactNode node = new ContactNode(KUID.createRandomNodeID(addr),addr);
            routingTable.add(node,true);
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
    
    public static void testReplaceCachedNode(RoutingTable routingTable) {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        for (int i = 0; i < 2; i++) {
            ContactNode node = new ContactNode(KUID.createPrefxNodeID(prefix,4),new InetSocketAddress("localhost",3000+i));
            routingTable.add(node,true);
        }
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix,4),new InetSocketAddress("localhost",30010));
        routingTable.add(node1,true);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        routingTable.add(node1,true);
    }
    
    public static void testreplaceBucketStaleNodes(RoutingTable routingTable) {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix,4),new InetSocketAddress("localhost",30010));
        node1.failure();
        node1.failure();
        node1.failure();
        routingTable.add(node1,false);
        for (int i = 0; i < 2; i++) {
            ContactNode node = new ContactNode(KUID.createPrefxNodeID(prefix,4),new InetSocketAddress("localhost",3000+i));
            routingTable.add(node,true);
        }
    }
    
    public static void testReplaceNode(RoutingTable routingTable) {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix,4),addr);
        routingTable.add(node1,true);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        routingTable.add(node1,true);
    }

    public static void testRemoveNode(RoutingTable routingTable) {
        byte[] prefix = new byte[1];
        prefix[0] = (byte)(0x01);
        ContactNode node1 = new ContactNode(KUID.createPrefxNodeID(prefix,4),addr);
        routingTable.add(node1,true);
        for (int i = 0; i < 20; i++) {
            ContactNode node = new ContactNode(KUID.createPrefxNodeID(prefix,4),addr);
            routingTable.add(node,true);
        }
        routingTable.handleFailure(node1.getNodeID());
        routingTable.handleFailure(node1.getNodeID());
        routingTable.handleFailure(node1.getNodeID());
        
    }
    
    
}
