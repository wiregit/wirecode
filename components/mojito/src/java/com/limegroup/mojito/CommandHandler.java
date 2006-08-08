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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.concurrent.Future;

import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.event.BootstrapEvent.Type;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.util.ArrayUtils;
import com.limegroup.mojito.util.CollectionUtils;

public class CommandHandler {
    
    private static final String[] COMMANDS = {
            "help",
            "info",
            "ping .+ \\d{1,5}",
            "reqstats .+ \\d{1,5} (stats|rt|db)",
            "bootstrap .+ \\d{1,5}",
            "put (key|kuid) (\\w|\\d)+ (value|file) .+",
            "remove (key|kuid) (\\w|\\d)+",
            "get (key|kuid) (\\w|\\d)+",
            "database",
            "routetable",
            "store .+",
            "load .+",
            "kill",
            "stats",
            "restart",
            "firewalled",
            "exhaustive",
            "id .+",
            "select .+",
            "nextid"
    };
    
    public static boolean handle(MojitoDHT dht, String command, PrintWriter out) throws IOException {
        try {
            for (String c : COMMANDS) {
                if (command.matches(c)) {
                    String[] args = command.split(" ");
                    Method m = CommandHandler.class.getDeclaredMethod(args[0], MojitoDHT.class, String[].class, PrintWriter.class);
                    m.invoke(null, dht, args, out);
                    return true;
                }
            }
        } catch (NoSuchMethodException err) {
            err.printStackTrace(out);
        } catch (InvocationTargetException err) {
            err.printStackTrace(out);
        } catch (IllegalAccessException err) {
            err.printStackTrace(out);
        } finally {
            out.flush();
        }
        
        return false;
    }
    
    public static void help(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        for (String c : COMMANDS) {
            out.println(c);
        }
    }
    
    public static void info(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        out.println("Local ContactNode: " + ((Context)dht).getLocalNode());
        out.println("Is running: " + dht.isRunning());
        out.println("Database Size (Keys): " + ((Context)dht).getDatabase().getKeyCount());
        out.println("Database Size (Values): " + ((Context)dht).getDatabase().getValueCount());
        out.println("RouteTable Size: " + ((Context)dht).getRouteTable().size());
        out.println("Estimated DHT Size: " + dht.size());
    }
    
    public static void exhaustive(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.setValue(
                !KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
        
        out.println("Exhaustive: " + KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
    }
    
    public static void firewalled(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        //dht.setFirewalled(!dht.isFirewalled());
        //out.println("Firewalled: " + dht.isFirewalled());
        out.println("Switching between firewalled and not firewalled is not possible");
    }
    
    public static void database(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        StringBuilder buffer = new StringBuilder("\n");
        
        Database database = ((Context)dht).getDatabase();
        for(DHTValue value : database.values()) {    
            buffer.append("VALUE: ").append(value).append("\n\n");
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL: ").append(database.getKeyCount())
            .append("/").append(database.getValueCount()).append("\n");
        
        out.println(buffer);
    }
    
    public static void routetable(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        StringBuilder buffer = new StringBuilder("\n");
        
        RouteTable routingTable = ((Context)dht).getRouteTable();
        buffer.append(routingTable.toString());
        
        out.println(buffer);
    }
    
    public static Future<Contact> ping(MojitoDHT dht, String[] args, final PrintWriter out) throws IOException {
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        out.println("Pinging... " + addr);
        /*dht.ping(addr, new PingListener() {
            public void handleResult(Contact result) {
                out.println("Ping to " + result + " succeeded: " + result.getRoundTripTime() + "ms");
                out.flush();
            }
            
            public void handleException(Exception ex) {
                if (ex instanceof DHTException) {
                    DHTException dhtEx = (DHTException)ex;
                    KUID nodeId = dhtEx.getNodeID();
                    SocketAddress address = dhtEx.getSocketAddress();
                    out.println("Ping to " + ContactUtils.toString(nodeId, address) + " failed");
                } else {
                    ex.printStackTrace(out);
                }
                out.flush();
            }
        });*/
        
        Future<Contact> future = dht.ping(addr);
        try {
            Contact result = future.get();
            out.println("Ping to " + result + " succeeded: " + result.getRoundTripTime() + "ms");
        } catch (Exception err) {
            err.printStackTrace(out);
        }
        out.flush();
        
        return future;
    }
    
    public static void reqstats(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
    }
    
    public static void bootstrap(MojitoDHT dht, String[] args, final PrintWriter out) throws IOException {
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        out.println("Bootstrapping... " + addr);
        
        BootstrapListener listener = new BootstrapListener() {
            public void handleResult(BootstrapEvent result) {
                if (result.getType() == Type.SUCCEEDED) {
                    out.println("Bootstraping finished:\n" + result);
                    out.flush();
                }
            }
            
            public void handleThrowable(Throwable ex) {
                out.println("Bootstraping failed");
                ex.printStackTrace(out);
                out.flush();
            }
        };
        
        ((Context)dht).cancelBootstrapping();
        dht.bootstrap(addr).addDHTEventListener(listener);
    }
    
    public static void put(MojitoDHT dht, String[] args, final PrintWriter out) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            byte[] value = null;
            
            if (args[1].equals("kuid")) {
                key = KUID.createValueID(ArrayUtils.parseHexString(args[2]));
            } else {
                key = KUID.createValueID(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            if (args[3].equals("value")) {
                value = args[4].getBytes("UTF-8");
            } else if (args[3].equals("file")) {
                File file = new File(args[4]);
                byte[] data = new byte[(int)Math.min(1024, file.length())];
                FileInputStream in = new FileInputStream(file);
                in.read(data, 0, data.length);
                in.close();
                value = data;
            }
            md.reset();
            
            out.println("Storing... " + key);
            /*dht.put(key, value, new StoreListener() {
                public void store(KeyValue keyValue, Collection nodes) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("STORED KEY_VALUES: ").append(keyValue).append("\n");
                    int i = 0;
                    for (Iterator iter = nodes.iterator(); iter.hasNext();) {
                        Contact node = (Contact) iter.next();
                        buffer.append(i).append(": ").append(node).append("\n");
                        i++;
                    }
                    out.println(buffer.toString());
                    out.flush();
                }
            });*/
            
            StoreEvent evt = dht.put(key, value).get();
            StringBuilder buffer = new StringBuilder();
            buffer.append("STORE RESULT:\n");
            buffer.append(evt.toString());
            out.println(buffer.toString());
        } catch (Exception err) {
            err.printStackTrace(out);
        }
    }
    
    public static void remove(MojitoDHT dht, String[] args, final PrintWriter out) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            if (args[1].equals("kuid")) {
                key = KUID.createValueID(ArrayUtils.parseHexString(args[2]));
            } else {
                key = KUID.createValueID(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            out.println("Removing... " + key);
            /*dht.remove(key, new StoreListener() {
                public void handleResult(Entry<KeyValue, List<Contact>> result) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("REMOVED KEY_VALUES: ").append(result.getKey()).append("\n");
                    buffer.append(CollectionUtils.toString(result.getValue()));
                    out.println(buffer.toString());
                    out.flush();
                }
                
                public void handleException(Exception ex) {
                    ex.printStackTrace(out);
                    out.flush();
                }
            });*/
            
            StoreEvent evt = dht.remove(key).get();
            StringBuilder buffer = new StringBuilder();
            buffer.append("REMOVE RESULT:\n");
            buffer.append(evt.toString());
            out.println(buffer.toString());
            
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
    
    public static void get(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            if (args[1].equals("kuid")) {
                key = KUID.createValueID(ArrayUtils.parseHexString(args[2]));
            } else {
                key = KUID.createValueID(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            /*dht.get(key, new LookupAdapter() {
                public void found(KUID key, Collection values, long time) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(key).append(" in ").append(time).append("ms\n");
                    buffer.append(values);
                    buffer.append("\n");
                    out.println(buffer.toString());
                    out.flush();
                }

                public void finish(KUID lookup, Collection c, long time) {
                    if (c.isEmpty()) {
                        out.println(lookup + " was not found after " + time + "ms");
                    } else {
                        out.println("Lookup for " + lookup + " finished after " 
                                + time + "ms and " + c.size() + " found locations");
                    }
                    out.flush();
                }
            });*/
            
            long start = System.currentTimeMillis();
            FindValueEvent evt = dht.get(key).get();
            long time = System.currentTimeMillis() - start;
            
            /*if (!evt.getValues().isEmpty()) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(key).append(" in ").append(time).append("ms\n");
                buffer.append(evt);
                buffer.append("\n");
                out.println(buffer.toString());
            } else {
                out.println(key + " was not found after " + time + "ms");
            }*/
            
            StringBuilder buffer = new StringBuilder();
            for (DHTValue value : evt) {
                buffer.append(value).append("\n");
            }
            out.println(buffer.toString());
            
            out.println();
            
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
    
    public static MojitoDHT load(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        File file = new File(args[1]);
        out.println("Loading: " + file);
        FileInputStream fis = new FileInputStream(file);
        
        try {
            return MojitoFactory.load(fis);
        } catch (ClassNotFoundException err) {
            err.printStackTrace(out);
        } finally {
            fis.close();
        }
        
        return null;
    }
    
    public static void store(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        File file = new File(args[1]);
        out.println("Storing: " + file);
        FileOutputStream fos = new FileOutputStream(file);
        //dht.store(fos, true, true);
        dht.store(fos);
        fos.close();
    }
    
    public static void stats(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        DHTStats stats = ((Context)dht).getDHTStats();
        stats.dumpStats(out, true);
    }
    
    public static void id(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        KUID nodeId = KUID.createNodeID(ArrayUtils.parseHexString(args[1]));
        System.out.println("Setting NodeID to: " + nodeId);
        Method m = dht.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
        m.setAccessible(true);
        m.invoke(dht, new Object[]{nodeId});
    }
    
    public static void select(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        KUID nodeId = KUID.createNodeID(ArrayUtils.parseHexString(args[1]));
        System.out.println("Selecting: " + nodeId);
        
        RouteTable routeTable = ((Context)dht).getRouteTable();
        System.out.println(CollectionUtils.toString(routeTable.select(nodeId, 20, false)));
    }
    
    public static void nextid(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        ((ContactNode)((Context)dht).getLocalNode()).nextInstanceID();
    }
}