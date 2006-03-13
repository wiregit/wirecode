package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;

public class NodeIDSpoofTest {

    public NodeIDSpoofTest() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: NodeIDSpoofTest <testNum>. testNum: 0 for spoof, 1 for replace");
            System.exit(-1);
        }
        int testNum = Integer.parseInt(args[0]);
        int port = 4000;
        final Object lock = new Object();
        try {
            System.out.println("Starting bootstrap server");
            DHT dht = new DHT();
            SocketAddress sac = new InetSocketAddress(port); 
            dht.bind(sac);
            Thread t = new Thread(dht, "DHT-bootstrap");
            t.start();
            System.out.println("bootstrap server is ready");
            if(testNum == 1) testSpoof(port);
            else testReplace(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void testSpoof(int port) throws IOException{
        //original DHT
        System.out.println("here");
        SocketAddress sac2 = new InetSocketAddress(port+1);
        KUID nodeID = KUID.createRandomNodeID(sac2);
        DHT dht2 = new DHT();
        dht2.bind(sac2,nodeID);
        Thread t2 = new Thread(dht2, "DHT-1");
        t2.start();
        dht2.bootstrap(new InetSocketAddress("localhost",port),null);
        System.out.println("2");
        //spoofer
        DHT dht3 = new DHT();
        dht3.bind(new InetSocketAddress(port+2),nodeID);
        Thread t3 = new Thread(dht3, "DHT-2");
        t3.start();
        dht3.bootstrap(new InetSocketAddress("localhost",port),null);
        System.out.println("3");
    }
    
    public static void testReplace(int port) throws IOException{
        //original DHT
        SocketAddress sac2 = new InetSocketAddress(port+1);
        KUID nodeID = KUID.createRandomNodeID(sac2);
        DHT dht2 = new DHT();
        dht2.bind(sac2,nodeID);
        Thread t2 = new Thread(dht2, "DHT-1");
        t2.start();
        dht2.bootstrap(new InetSocketAddress("localhost",port),null);
        //REPLACE!
        dht2.close();
        DHT dht3 = new DHT();
        dht3.bind(new InetSocketAddress(port+2),nodeID);
        Thread t3 = new Thread(dht3, "DHT-2");
        t3.start();
        dht3.bootstrap(new InetSocketAddress("localhost",port),null);
    }
}
