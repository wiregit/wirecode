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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.limegroup.gnutella.dht.statistics.StatsManager;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;

public class PlanetLab {

    private static final StatsManager statsManager = StatsManager.INSTANCE;
    
    private static final String outputDir = "OUTBOUND/";
    
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
    
    public static DHT createBootstrapDHT(int port) {
        return (DHT)createDHTs(port, 1).get(0);
    }
    
    public static void bootstrap(List dhts, InetSocketAddress dst) {
        System.out.println("Bootstrapping from " + dst);
        Random generator = new Random();
        
        long start = System.currentTimeMillis();
        
        final Object lock = new Object();
        
        for(int i = 0; i < dhts.size(); i++) {
            final int index = i;
            DHT dht = (DHT)dhts.get(i);
            
            try {
                
                //System.out.println("Bootstraping " + dht);
                synchronized(lock) {
                    
                    try {
                        Thread.sleep(generator.nextInt(1000 * 60 * 10));
                    } catch (InterruptedException err) {
                        err.printStackTrace();
                    }
                    
                    dht.bootstrap(dst, new BootstrapListener() {
                        
                        public void initialPhaseComplete(KUID nodeId, Collection nodes, long time) {
                            if (nodes.isEmpty()) {
                                System.out.println(index + ": " + nodeId + " failed to bootstrap at PHASE 1");
                            } else {
//                                System.out.println(index + ": " + nodeId + " finished bootstraping PHASE 1 in " + time + " ms");
                            }
                        }

                        public void secondPhaseComplete(KUID nodeId, boolean foundNodes, long time) {
                            StringBuffer buffer = new StringBuffer();
                            buffer.append(index).append(": finished bootstrapping PHASE 2 in ");
                            buffer.append(time).append(" ms ");
                            
                            if (foundNodes) {
                                buffer.append("with new nodes");
                            } else {
                                buffer.append("with no new nodes");
                            }
                            
                            System.out.println(buffer.toString());
                            
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
        
        try {
            Thread.sleep(10000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        
        shutdown();
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("PlanetLab <port> <number of instances> [<bootstrap host> <bootstrap port> <test>]");
            System.exit(-1);
        }
        
        File file = new File(outputDir);
        if(!file.exists() || !file.isDirectory()) {
            file.mkdir();
        } else {
            file.delete();
            file.mkdir();
        }
        statsManager.setOutputDir(outputDir);
        
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
        
        switch(test) {
            case 0:
                DHT dht = createBootstrapDHT(port);
                System.out.println("Bootstrap DHT " + dht + " is ready!");
                break;
            case 1:
                List dhts = createDHTs(port, number);
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
        
//        System.exit(0);
    }
}
