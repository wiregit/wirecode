package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.StatsListener;
import de.kapsi.net.kademlia.handler.response.StatsResponseHandler;
import de.kapsi.net.kademlia.messages.request.StatsRequest;

public class StatsRPCTest {

    
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        int port = 4000;
        try {
            System.out.println("Starting stats server");
            DHT dht = new DHT();
            SocketAddress sac = new InetSocketAddress("localhost",port); 
            dht.bind(sac);
            Thread t = new Thread(dht, "DHT-StatsServer");
            t.start();
            System.out.println("Stats server is ready");
            System.out.println("Starting Node");
            SocketAddress sac2 = new InetSocketAddress("localhost",port+1);
            KUID nodeID = KUID.createRandomNodeID(sac2);
            final DHT dht2 = new DHT();
            dht2.bind(sac2,nodeID);
            Thread t2 = new Thread(dht2, "DHT-2");
            t2.start();
            dht2.bootstrap(sac,new BootstrapListener() {
                public void initialPhaseComplete(KUID nodeId, Collection nodes, long time) {}
                public void secondPhaseComplete(KUID nodeId, boolean foundNodes, long time) {}
            });
            try {
                Thread.sleep(3L*1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            StatsRequest req = dht.getContext().getMessageFactory().createStatsRequest(KUID.createRandomMessageID(),
                    new byte[0],StatsRequest.STATS);
            StatsListener listener = new StatsListener() {
                public void nodeStatsResponse(ContactNode node, String statistics, long time) {
                    System.out.println("Stats: "+statistics);
                }

                public void nodeStatsTimeout(KUID nodeId, SocketAddress address) {
                    System.out.println("Stats timeout");
                }
            };
            dht.getContext().getMessageDispatcher().send(sac2, req, new StatsResponseHandler(dht.getContext(),listener));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
