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
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.limegroup.gnutella.dht.statistics.PlanetLabTestsStatContainer;
import com.limegroup.gnutella.dht.statistics.StatsManager;
import com.sun.tools.javac.v8.Retro;

import de.kapsi.net.kademlia.DHT;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.event.StoreListener;
import de.kapsi.net.kademlia.settings.DatabaseSettings;

public class PlanetLab {

    private static final String[] TEST_VALUES = {
        "hello",
        "world",
        "mark",
        "roger",
        "greg",
        "sam",
        "zlatin",
        "justin",
        "tim",
        "karl"
    };
    
    private static byte[] getBytes(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static KUID toKUID(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] bytes = getBytes(value);
            byte[] digest = md.digest(bytes);
            return KUID.createValueID(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
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
    
    public static void bootstrap(List dhts, final InetSocketAddress dst, final int testNumber) {
        System.out.println("Bootstrapping from " + dst);
        Random generator = new Random();
        
        long start = System.currentTimeMillis();
        
        final Object lock = new Object();
        
        for(int i = 0; i < dhts.size(); i++) {
            final int index = i;
            final DHT dht = (DHT)dhts.get(i);
            
            try {
                
                //System.out.println("Bootstraping " + dht);
                synchronized(lock) {
                    
                    try {
                        Thread.sleep(generator.nextInt(1000 * 60 * 5));
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
                            
                            if(testNumber == 2) {
                                if(index < TEST_VALUES.length){
                                    DHTController controller = new DHTController(dht, dst, TEST_VALUES[index]);
                                    new Thread(controller).start();
                                }
                            } else if (testNumber == 3) {
                                DHTController controller = new DHTController(dht, dst, null);
                                new Thread(controller).start();
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
        
        List dhts = null;
        switch(test) {
            case 0:
                DHT dht = createBootstrapDHT(port);
                System.out.println("Bootstrap DHT " + dht + " is ready!");
                break;
            case 1: //bootstrap test
            case 2: //churn - publisher test
            case 3: //churn - retriever test
                dhts = createDHTs(port, number);
                bootstrap(dhts, dst, test);
                break;
            default:
                System.err.print("Unknown test case: " + test);
                System.exit(-1);
                break;
        }
        
        new Thread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(15L * 60L * 1000L);
                    } catch (InterruptedException e) {
                    }
                    statsManager.writeStatsToFiles();
                }
            }
        }, "StatsWriterThread").start();
    }
    
    private static void shutdown() {
        //statsManager.writeStatsToFiles();
        
//        System.exit(0);
    }
    
    public static class DHTController implements Runnable {
        
        private static final Random GENERATOR = new Random();
        
        private static final long minOffline = 1L * 60L * 1000L;
        private static final long maxOffline = 5L * 60L * 1000L;
        
        private static final long minChurn = 5L * 60L * 1000L;
        private static final long maxChurn = 10L * 60L * 1000L;
        
        private static final long minRepublisher = (long)(((float)2/3) * 
            DatabaseSettings.EXPIRATION_TIME_CLOSEST_NODE);
        
        private static final long minRetriever = 1L * 60L * 1000L;
        private static final long maxRetriever = 5L * 60L * 1000L;
        
        private DHT dht;
        private SocketAddress bootstrapServer;
        
        private KUID localNodeId;
        private SocketAddress address;
        
        private PlanetLabTestsStatContainer planetlabStats;
        
        private boolean running = true;
        private String value;
        
        private long lastPublishTime;
        
        public DHTController(DHT dht, SocketAddress bootstrapServer, String value) {
            this.dht = dht;
            this.bootstrapServer = bootstrapServer;
            
            localNodeId = dht.getLocalNodeID();
            address = dht.getLocalSocketAddrss();
            
            this.value = value;
            
            planetlabStats = new PlanetLabTestsStatContainer(dht.getContext());
        }
        
        public void run() {
            
            final Object lock = new Object();
            
            while(true) {
                
                try {
                    if (value != null) {
                        long publishDelay = System.currentTimeMillis() - lastPublishTime;
                        
                        if(publishDelay >= minRepublisher){
                            
                            lastPublishTime = System.currentTimeMillis();
                            dht.put(toKUID(value), getBytes(value), new StoreListener() {
                                public void store(List keyValues, Collection nodes) {
                                    planetlabStats.PUBLISH_LOCATIONS.addData(nodes.size());
                                }
                            });
                            planetlabStats.PUBLISH_COUNT.incrementStat();
                        }
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                
                long sleep = minChurn + GENERATOR.nextInt((int)(maxChurn-minChurn));
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {}
                
                try {
                    running = false;
                    dht.close();
                    planetlabStats.CHURN_DISCONNECTS.incrementStat();
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                sleep = minOffline + GENERATOR.nextInt((int)(maxOffline-minOffline));
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {}
                
                
                synchronized (lock) {
                    try {
                        dht.bind(address, localNodeId);
                        new Thread(dht).start();
                        
                        dht.bootstrap(bootstrapServer, new BootstrapListener() {
                            public void initialPhaseComplete(KUID nodeId, Collection nodes, long time) {
                            }

                            public void secondPhaseComplete(KUID nodeId, boolean foundNodes, long time) {
                                planetlabStats.CHURN_RECONNECTS.incrementStat();
                                running = true;
                                
                                synchronized(lock) {
                                    lock.notify();
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                
                try {
                    if (value == null) { //Retriever DHTs
                        
                        sleep = minRetriever + GENERATOR.nextInt((int)(maxRetriever - minRetriever));
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException e) {
                        }
                        
                        String value = TEST_VALUES[GENERATOR.nextInt(TEST_VALUES.length)];
                        
                        KUID key = toKUID(value);
                        dht.get(key, new FindValueListener() {
                            public void foundValue(KUID key, Collection values, long time) {
                                if(values == null || values.isEmpty()){
                                    planetlabStats.RETRIEVE_FAILURES.incrementStat();
                                } else {
                                    planetlabStats.RETRIEVE_SUCCESS.incrementStat();
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
