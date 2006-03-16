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

public class BucketsTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        KademliaSettings.setReplicationParameter(20);
        DHT dht = new DHT();
        InetSocketAddress addr = new InetSocketAddress(3000);
        try {
            dht.bind(addr);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new Thread(dht,"DHT").start();
        RoutingTable routingTable = dht.getContext().getRouteTable();
        for (int i = 0; i < 800; i++) {
            ContactNode node = new ContactNode(KUID.createRandomNodeID(addr),addr);
            routingTable.add(node);
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
        buffer.append("LOCAL NODE:"+dht.getLocalNode());
        System.out.println(buffer);
        try {
            dht.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

}
