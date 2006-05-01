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
import java.net.SocketAddress;

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
            
            switch(testNum) {
                case 0:
                    testSpoof(port);
                    break;
                case 1:
                    testReplace(port);
                    break;
                default:
                    System.out.println("Unknown Test: " + testNum);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void testSpoof(int port) throws IOException{
        //original DHT
        System.out.println("here");
        SocketAddress sac2 = new InetSocketAddress(port+1);
        KUID nodeID = KUID.createRandomNodeID(sac2);
        final DHT dht2 = new DHT();
        dht2.bind(sac2,nodeID);
        Thread t2 = new Thread(dht2, "DHT-1");
        t2.start();
        dht2.bootstrap(new InetSocketAddress("localhost",port), null);
        System.out.println("2");
        
        //spoofer
        final DHT dht3 = new DHT();
        dht3.bind(new InetSocketAddress(port+2),nodeID);
        Thread t3 = new Thread(dht3, "DHT-2");
        t3.start();
        dht3.bootstrap(new InetSocketAddress("localhost",port), new BootstrapListener() {
            public void phaseOneComplete(long time) {
            }

            public void phaseTwoComplete(boolean foundNodes, long time) {
                System.out.println();
                System.out.println("1) Sent count: " + dht2.getSentMessagesCount());
                System.out.println("1) Recv count: " + dht2.getReceivedMessagesCount());
                System.out.println("1) Sent size: " + dht2.getSentMessagesSize());
                System.out.println("1) Recv size: " + dht2.getReceivedMessagesSize());
                
                System.out.println(); 
                System.out.println("2) Sent count: " + dht3.getSentMessagesCount());
                System.out.println("2) Recv count: " + dht3.getReceivedMessagesCount());
                System.out.println("2) Sent size: " + dht3.getSentMessagesSize());
                System.out.println("2) Recv size: " + dht3.getReceivedMessagesSize());
            }
        });
        System.out.println("3");
        
        
    }
    
    public static void testReplace(final int port) throws IOException{
        //original DHT
        SocketAddress sac2 = new InetSocketAddress(port+1);
        final KUID nodeID = KUID.createRandomNodeID(sac2);
        final DHT dht2 = new DHT();
        dht2.bind(sac2,nodeID);
        Thread t2 = new Thread(dht2, "DHT-1");
        t2.start();
        dht2.bootstrap(new InetSocketAddress("localhost",port), new BootstrapListener(){
            public void phaseOneComplete(long time) {}
            public void phaseTwoComplete(boolean foundNodes, long time) {
                System.out.println("2");
                //REPLACE!
                try {
                    dht2.close();
                    DHT dht3 = new DHT();
                    dht3.bind(new InetSocketAddress(port+2),nodeID);
                    Thread t3 = new Thread(dht3, "DHT-2");
                    t3.start();
                    dht3.bootstrap(new InetSocketAddress("localhost",port),null);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}
