/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
 
package com.limegroup.mojito;

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

import com.limegroup.gnutella.util.KeyValue;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.statistics.PlanetLabTestsStatContainer;
import com.limegroup.mojito.statistics.StatsManager;


public class PlanetLab {
    
    private static long minOffline = 1L * 60L * 1000L;
    private static long maxOffline = 5L * 60L * 1000L;
    
    private static long minChurn = 5L * 60L * 1000L;
    private static long maxChurn = 10L * 60L * 1000L;
    
    private static long minRepublisher = (long)(((float)2/3) * 
        DatabaseSettings.VALUE_EXPIRATION_TIME.getValue());
    
    private static long minRetriever = 1L * 60L * 1000L;
    private static long maxRetriever = 5L * 60L * 1000L;
    
    private static long maxRunTime = Long.MAX_VALUE;
    
    private static String[] TEST_VALUES = {
        "hello",
        "mark",
        "roger",
        "greg",
        "sam",
        "zlatin",
        "justin",
        "tim",
        "karl",
        "kevin"
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
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                statsManager.addDHTNode(((Context)dht).getDHTStats());
                dht.bind(new InetSocketAddress(port+i));
                
                dht.start();
                
                dhts.add(dht);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        
        return dhts;
    }
    
    public static MojitoDHT createBootstrapDHT(int port) {
        return (MojitoDHT)createDHTs(port, 1).get(0);
    }
    
    public static void bootstrap(List dhts, final InetSocketAddress dst, final int testNumber) {
        System.out.println("Bootstrapping from " + dst);
        Random generator = new Random();
        
        if(testNumber == 4 || testNumber == 5) {
            //5 minutes expiration
            DatabaseSettings.VALUE_EXPIRATION_TIME.setValue(5L * 60L * 1000L);
        }
        
        long start = System.currentTimeMillis();
        
        final Object lock = new Object();
        
        for(int i = 0; i < dhts.size(); i++) {
            final int index = i;
            final MojitoDHT dht = (MojitoDHT)dhts.get(i);
            
            try {
                
                //System.out.println("Bootstraping " + dht);
                synchronized(lock) {
                    
                    try {
                        Thread.sleep(generator.nextInt(1000 /* 60 */* 2));
                    } catch (InterruptedException err) {
                        err.printStackTrace();
                    }
                    
                    dht.bootstrap(dst, new BootstrapListener() {
                        public void noBootstrapHost(List<? extends SocketAddress> failedHosts) {
                            System.out.println(index + ": no bootstrap host!");
                        }

                        public void phaseOneComplete(long time) {
                            System.out.println(index + ": bootstrap phase ONE finished");
                        }

                        public void phaseTwoComplete(boolean foundNodes, long time) {
                            StringBuffer buffer = new StringBuffer();
                            buffer.append(index).append(": finished bootstrapping phase TWO in ");
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
                            
                            if(testNumber == 2 || testNumber == 4) {
                                if(index < TEST_VALUES.length){
                                    DHTController controller = new DHTController(dht, dst, TEST_VALUES[index]);
                                    new Thread(controller).start();
                                }
                            } else if (testNumber == 3) {
                                DHTController controller = new DHTController(dht, dst, null);
                                new Thread(controller).start();
                            } else if (testNumber == 6) { //hotspot test
                                minChurn = Long.MAX_VALUE - 1L;
                                maxChurn = Long.MAX_VALUE;
                                minRetriever = 1L * 1000L; //retrieve values every 1 to 3 seconds
                                maxRetriever = 3L * 1000L;
                                TEST_VALUES = new String[]{"hello"};
                                DHTController controller = new DHTController(dht, dst, null);
                                new Thread(controller).start();
                            }
                        }
                    });
                    
                    try { 
                        lock.wait(5L*60L*1000L); 
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
        
        if (number >= 1) {
            dst = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            test = Integer.parseInt(args[4]);
        }
        
        System.out.println("Port: " + port);
        System.out.println("Instances: " + number);
        System.out.println("Bootstrap: " + dst);
        System.out.println("Test: " + test);
        
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
        
        List dhts = null;
        switch(test) {
            case 0:
                MojitoDHT dht = createBootstrapDHT(port);
                System.out.println("Bootstrap DHT " + dht + " is ready!");
                break;
            case 1: //bootstrap test
            case 2: //churn - publisher test
            case 3: //churn - retriever test
            case 4: //key,value distribution test - publishers
            case 5: //key,value distribution test - storers
            case 6: //Hotspot test - retrievers
                dhts = createDHTs(port, number);
                bootstrap(dhts, dst, test);
                break;
            default:
                System.err.print("Unknown test case: " + test);
                System.exit(-1);
                break;
        }
    }
    
    private static void shutdown() {
        //statsManager.writeStatsToFiles();
        
        // System.exit(0);
    }
    
    public static class DHTController implements Runnable {
        
        private static final Random GENERATOR = new Random();
        
        private MojitoDHT dht;
        private SocketAddress bootstrapServer;
        
        private KUID localNodeId;
        private SocketAddress address;
        
        private PlanetLabTestsStatContainer planetlabStats;
        
        private boolean running = true;
        private String value;
        
        private long lastPublishTime;
        
        public DHTController(MojitoDHT dht, SocketAddress bootstrapServer, String value) {
            this.dht = dht;
            this.bootstrapServer = bootstrapServer;
            
            localNodeId = dht.getLocalNodeID();
            address = dht.getLocalAddress();
            
            this.value = value;
            
            planetlabStats = new PlanetLabTestsStatContainer(dht.getContext());
        }
        
        public void run() {
            
            final Object lock = new Object();
            
            final Object lock2 = new Object();
            
            while(true) {
                
                //publisher node:
                //Publish values every minRepublisher minutes
                if (value != null) {
                    try {
                        /*long publishDelay = System.currentTimeMillis() - lastPublishTime;*/
                        /*if(publishDelay >= minRepublisher){*/
                        
                        lastPublishTime = System.currentTimeMillis();
                        dht.put(toKUID(value), getBytes(value), new StoreListener() {
                            public void store(KeyValue keyValue, Collection nodes) {
                                planetlabStats.PUBLISH_LOCATIONS.addData(nodes.size());
                            }
                        });
                        planetlabStats.PUBLISH_COUNT.incrementStat();
                        //}
                        try {
                            Thread.sleep(minRepublisher);
                        } catch (InterruptedException e) {}
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } 
                //retriever node:
                //1.Stay online for minChurn to maxChurn minutes
                //1.1 Try to retrieve values every minRetriever to maxRetriever minutes
                //2. Disconnect - Reconnect (bootstrap)
                //GOTO 1
                else {
                    long churn = minChurn + GENERATOR.nextInt((int)(maxChurn-minChurn));
                    long startTime = System.currentTimeMillis();
                    long livetime = 0L;
                    try {
                        while(livetime < churn) {
                            synchronized (lock2) {
                                long sleep = minRetriever + GENERATOR.nextInt((int)(maxRetriever - minRetriever));
                                try {
                                    Thread.sleep(sleep);
                                } catch (InterruptedException e) {}
                                String value = TEST_VALUES[GENERATOR.nextInt(TEST_VALUES.length)];
                                
                                KUID key = toKUID(value);
                                
                                dht.get(key, new LookupAdapter() {
                                    public void finish(KUID lookup, Collection c, long time) {
                                        if(c.isEmpty()){
                                            planetlabStats.RETRIEVE_FAILURES.incrementStat();
                                        } else {
                                            planetlabStats.RETRIEVE_SUCCESS.incrementStat();
                                        }
                                        synchronized(lock2) {
                                            lock2.notify();
                                        }
                                    }
                                });
                                try {
                                    lock2.wait();
                                } catch (InterruptedException e) {}
                            }
                            livetime = System.currentTimeMillis() - startTime;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    running = false;
                    dht.stop();
                    planetlabStats.CHURN_DISCONNECTS.incrementStat();
                    
                    long sleep = minOffline + GENERATOR.nextInt((int)(maxOffline-minOffline));
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {}
                    
                    synchronized (lock) {
                        try {
                            dht.bind(address);
                            dht.start();
                            
                            dht.bootstrap(bootstrapServer, new BootstrapListener() {
                                public void noBootstrapHost(List<? extends SocketAddress> failedHosts) {
                                    synchronized(lock) {
                                        lock.notify();
                                    }
                                }

                                public void phaseOneComplete(long time) {}

                                public void phaseTwoComplete(boolean foundNodes, long time) {
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
                            planetlabStats.CHURN_RECONNECTS.incrementStat();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }
    }
    
}