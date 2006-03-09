package com.limegroup.gnutella.dht.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;

public class PlanetLab {

    private static final StatsManager statsManager = StatsManager.INSTANCE;
    
    public static List createDHTs(int port, int number) {
        
        List dhts = new ArrayList(number);
        for(int i = 0; i < number; i++) {
            try {
                DHT dht = new DHT();
                statsManager.addDHTNode(dht.getContext().getDHTStats());
                dht.bind(new InetSocketAddress(port+i));
                
                Thread t = new Thread(dht, "DHT-" + i);
                t.start();
                
                dhts.add(dht);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        
        return dhts;
    }
    
    public static void bootstrap(List dhts, InetSocketAddress dst) {
        System.out.println("Bootstrapping from " + dst);
        long start = System.currentTimeMillis();
        
        final Object lock = new Object();
        
        for(int i = 0; i < dhts.size(); i++) {
            final int index = i;
            DHT dht = (DHT)dhts.get(i);
            
            try {
                synchronized(lock) {
                    dht.bootstrap(dst, new BootstrapListener() {
                        public void bootstrap(KUID nodeId, Collection nodes, long time) {
                            
                            if (nodes.isEmpty()) {
                                System.out.println(index + ": " + nodeId + " failed to bootstrap");
                            } else {
                                System.out.println(index + ": " + nodeId + " finished bootstraping in " + time + " ms");
                            }
                            
                            synchronized(lock) {
                                lock.notify();
                            }
                        }
                    });
                    
                    try { 
                        lock.wait(10L*1000L); 
                    } catch (InterruptedException err) {
                        err.printStackTrace();
                    }
                }
            } catch (IOException err) {
                err.printStackTrace();
            }
        }
        
        long end = System.currentTimeMillis();
        System.out.println("Bootstraping of " + dhts.size() + " Nodes finished in " + (end-start) + " ms");
        shutdown();
        //System.exit(0);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("PlanetLab <port> <number of instances> [<bootstrap host> <bootstrap port> <test>]");
            System.exit(-1);
        }
        
        int port = Integer.parseInt(args[0]);
        int number = Integer.parseInt(args[1]);
        
        InetSocketAddress dst = null;
        int test = 0;
        
        if (number > 1) {
            dst = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            test = Integer.parseInt(args[4]);
        }
        
        System.out.println("Port: " + port);
        System.out.println("Instances: " + number);
        System.out.println("Bootstrap: " + dst);
        System.out.println("Test: " + test);
                     
        List dhts = createDHTs(port, number);
        switch(test) {
            case 0:
                System.out.println(dhts.get(0) + " is ready");
                break;
            case 1:
                bootstrap(dhts, dst);
                break;
            default:
                System.err.print("Unknown test case: " + test);
                System.exit(-1);
                break;
        }
    }
    
    private static void shutdown() {
        statsManager.writeStatsToFiles();
    }
}
