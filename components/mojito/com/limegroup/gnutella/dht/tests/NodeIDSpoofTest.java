package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;

public class NodeIDSpoofTest {

    public NodeIDSpoofTest() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        int port = 4000;
        try {
            System.out.println("Starting bootstrap server");
            DHT dht = new DHT();
            SocketAddress sac = new InetSocketAddress(port); 
            dht.bind(sac);
            Thread t = new Thread(dht, "DHT-bootstrap");
            t.start();
            System.out.println("bootstrap server is ready");
            //original DHT
            SocketAddress sac2 = new InetSocketAddress(port+1);
            KUID nodeID = KUID.createRandomNodeID(sac2);
            DHT dht2 = new DHT();
            dht2.bind(sac2,nodeID);
            Thread t2 = new Thread(dht2, "DHT-1");
            t2.start();
            dht2.bootstrap(sac,null);
            //spoofer
            DHT dht3 = new DHT();
            dht3.bind(new InetSocketAddress(port+2),nodeID);
            Thread t3 = new Thread(dht3, "DHT-2");
            t3.start();
            dht3.bootstrap(sac,null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
