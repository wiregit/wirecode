package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.StatsListener;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.request.StatsRequest;
import de.kapsi.net.kademlia.messages.response.StatsResponse;

public class StatsRPCTest {

    
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        int port = 4000;
        try {
            System.out.println("Starting stats server");
            DHT dht = new DHT("DHT-StatsServer");
            SocketAddress sac = new InetSocketAddress("localhost",port); 
            dht.bind(sac);
            dht.start();
            System.out.println("Stats server is ready");
            System.out.println("Starting Node");
            SocketAddress sac2 = new InetSocketAddress("localhost",port+1);
            KUID nodeID = KUID.createRandomNodeID(sac2);
            final DHT dht2 = new DHT("DHT-2");
            dht2.bind(sac2,nodeID);
            dht2.start();
            
            dht2.bootstrap(sac);
            
            StatsListener listener = new StatsListener() {
                public void response(ResponseMessage response, long time) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("*** Stats to ").append(response.getSource())
                        .append(" succeeded in ").append(time).append("ms\n");
                    buffer.append(((StatsResponse)response).getStatistics());
                    System.out.println(buffer.toString());
                }

                public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("*** Stats to ").append(ContactNode.toString(nodeId, address))
                        .append(" failed with timeout");
                    System.out.println(buffer.toString());
                }
            };
            
            dht.stats(sac2, StatsRequest.STATS, listener);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
