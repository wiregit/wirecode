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
 
package de.kapsi.net.kademlia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.limegroup.gnutella.dht.statistics.DHTStats;

import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.event.StoreListener;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.settings.NetworkSettings;
import de.kapsi.net.kademlia.util.ArrayUtils;

public class Main {
    
    public static void main(String[] args) throws Exception {
        
        int count = 10;
        String host = null;
        int port = NetworkSettings.PORT.getValue();
        
        if (args.length == 0) {
            System.out.println("java Main DHTs count [host] port");
            System.exit(-1);
        } else {
            count = Integer.parseInt(args[0]);
            
            if (args.length == 2) {
                port = Integer.parseInt(args[1]);
            } else {
                host = args[1];
                port = Integer.parseInt(args[2]);                
            }
        }
        
        InetAddress addr = host != null ? InetAddress.getByName(host) : null;
        
        ArrayList dhts = new ArrayList();
        
        for(int i = 0; i < count; i++) {
            try {
                DHT dht = new DHT();
                if (addr != null) {
                    dht.bind(new InetSocketAddress(addr, port+i));
                } else {
                    dht.bind(new InetSocketAddress(port+i));
                }
                
                new Thread(dht, "DHT-" + i).start();

                dhts.add(dht);
                System.out.println(i + ": " + ((DHT)dhts.get(dhts.size()-1)).getLocalNode());
            } catch (IOException err) {
                System.err.println("Failed to start/connect DHT #" + i);
                err.printStackTrace();
            }
        }
        
        new BootstrapUtil(dhts).bootstrap();
        
        int current = 0;
        DHT dht = (DHT)dhts.get(current);
        
        String help = "help";
        String info = "info";
        String svitch = "switch \\d{1,}";
        String ping = "ping .+ \\d{1,5}";
        String bootstrap = "bootstrap .+ \\d{1,5}";
        String put = "put (key|kuid) (\\w|\\d)+ (value|file) .+";
        String get = "get (key|kuid) (\\w|\\d)+";
        String getr = "getr (key|kuid) (\\w|\\d)+";
        String listDB = "list db";
        String listRT = "list rt";
        String storeRT = "store rt";
        String storeDB = "store db";
        String loadRT = "load rt";
        String loadDB = "load db";
        String kill = "kill";
        String stats = "stats";
        String restart = "restart";
        String quit = "quit";
        
        String[] commands = {
                help,
                info,
                svitch,
                ping,
                bootstrap,
                put,
                get,
                getr,
                listDB,
                listRT,
                storeRT,
                storeDB,
                loadRT,
                loadDB,
                kill,
                stats,
                restart,
                quit
        };
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            System.out.print("[" + current + "] $ ");
            String line = in.readLine().trim();
            if (line.equals("")) {
                continue;
            }
            
            try {
                if (line.matches(info)) {
                    info(dht);
                } else if (line.matches(svitch)) {
                    int index = Integer.parseInt(line.split(" ")[1]);
                    dht = (DHT)dhts.get(index);
                    current = index;
                    info(dht);
                } else if (line.matches(ping)) {
                    ping(dht, line.split(" "));
                } else if (line.matches(bootstrap)) {
                    bootstrap(dht, line.split(" "));
                } else if (line.matches(put)) {
                    put(dht, line.split(" "));
                } else if (line.matches(get)) {
                    get(dht, line.split(" "));
                } else if (line.matches(getr)) {
                    get(dht, line.split(" "));
                } else if (line.matches(listDB) || line.matches(listRT)) {
                    list(dht, line.split(" "));
                } else if (line.matches(storeRT) || line.matches(storeDB)) {
                    store(dht, line.split(" "));
                } else if (line.matches(loadRT) || line.matches(loadDB)) {
                    load(dht, line.split(" "));
                } else if (line.matches(kill)) {
                    if (dht.isRunning()) {
                        dht.close();
                    }
                } else if (line.matches(stats)) {
                    DHTStats dhtStats = dht.getContext().getDHTStats();
                    OutputStreamWriter writer = new OutputStreamWriter(System.out);
                    dhtStats.dumpStats(writer);
                } else if (line.matches(restart)) {
                    if (!dht.isRunning()) {
                        dht = new DHT();
                        if (addr != null) {
                            dht.bind(new InetSocketAddress(addr, port+current));
                        } else {
                            dht.bind(new InetSocketAddress(port+current));
                        }
                        new Thread(dht, "DHT-" + current).start();
                        dhts.set(current, dht);
                    }
                } else if (line.matches(quit)) {
                    for(int i = 0; i < dhts.size(); i++) {
                        ((DHT)dhts.get(i)).close();
                    }
                    System.exit(0);
                } else {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("--- List of commands ---\n");
                    for(int i = 0; i < commands.length; i++) {
                        buffer.append(commands[i]).append("\n");
                    }
                    System.out.println(buffer.toString());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
    
    private static void info(DHT dht) throws Throwable {
        System.out.println("Local ContactNode: " + dht.getLocalNode());
        System.out.println("Is running: " + dht.isRunning());
        System.out.println("Database Size: " + dht.getDatabase().size());
        System.out.println("RouteTable Size: " + dht.getRoutingTable().size());
        System.out.println("Size: " + dht.size());
    }
    
    private static void list(DHT dht, String[] line) throws Throwable {
        StringBuffer buffer = new StringBuffer("\n");
        
        if(line[1].equals("db")) {
            Database database = dht.getDatabase();
            Collection values = database.getAllValues();
            for(Iterator it = values.iterator(); it.hasNext(); ) {
                KeyValue value = (KeyValue)it.next();
                
                buffer.append("VALUE: ").append(value).append("\n\n");
            }
            buffer.append("-------------\n");
            buffer.append("TOTAL: " + values.size()).append("\n");
        } else {
            RoutingTable routingTable = dht.getRoutingTable();
            buffer.append(routingTable.toString());
        }
        
        System.out.println(buffer);
    }
    
    private static void ping(DHT dht, String[] line) throws Throwable {
        String host = line[1];
        int port = Integer.parseInt(line[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        System.out.println("Pinging... " + addr);
        dht.ping(addr, new PingListener() {
            public void pingSuccess(KUID nodeId, SocketAddress address, long time) {
                if (nodeId != null) {
                    System.out.println("*** Ping to " + ContactNode.toString(nodeId, address) + " succeeded: " + time + "ms");
                } else {
                    System.out.println("*** Ping to " + address + " succeeded: " + time + "ms");
                }
            }
            
            public void pingTimeout(KUID nodeId, SocketAddress address, long time) {
                if (nodeId != null) {
                    System.out.println("*** Ping to " + ContactNode.toString(nodeId, address) + " failed");
                } else {
                    System.out.println("*** Ping to " + address + " failed");
                }
            }
        });
    }
    
    private static void bootstrap(DHT dht, String[] line) throws Throwable {
        String host = line[1];
        int port = Integer.parseInt(line[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        System.out.println("Bootstraping... " + addr);
        dht.bootstrap(addr, new BootstrapListener() {
            public void initialPhaseComplete(KUID nodeId, Collection nodes, long time) {
                System.out.println("*** Bootstraping phase 1 " + (!nodes.isEmpty() ? "succeded" : "failed") + " in " + time + " ms");
            }

            public void secondPhaseComplete(KUID nodeId, boolean foundNodes, long time) {
                System.out.println("*** Bootstraping phase 2 " + (foundNodes ? "succeded" : "failed") + " in " + time + " ms");
            }
        });
    }
    
    private static void put(DHT dht, final String[] line) throws Throwable {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        
        KUID key = null;
        byte[] value = null;
        
        if (line[1].equals("kuid")) {
            key = KUID.createValueID(ArrayUtils.parseHexString(line[2]));
        } else {
            key = KUID.createValueID(md.digest(line[2].getBytes("UTF-8")));
        }
        md.reset();
        
        if (line[3].equals("value")) {
            value = line[4].getBytes("UTF-8");
        } else if (line[3].equals("file")) {
            File file = new File(line[4]);
            byte[] data = new byte[(int)Math.min(1024, file.length())];
            FileInputStream in = new FileInputStream(file);
            in.read(data, 0, data.length);
            in.close();
            value = data;
        }
        md.reset();
        
        System.out.println("Storing... " + key);
        dht.put(key, value, new StoreListener() {
            public void store(List keyValues, Collection nodes) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("STORED KEY_VALUES: ").append(keyValues).append("\n");
                int i = 0;
                for (Iterator iter = nodes.iterator(); iter.hasNext();) {
                    Node node = (Node) iter.next();
                    buffer.append(i).append(": ").append(node).append("\n");
                    i++;
                }
                System.out.println(buffer.toString());
            }
        });
    }
    
    private static void get(DHT dht, String[] line) throws Throwable {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        
        KUID key = null;
        
        if (line[1].equals("kuid")) {
            key = KUID.createValueID(ArrayUtils.parseHexString(line[2]));
        } else {
            key = KUID.createValueID(md.digest(line[2].getBytes("UTF-8")));
        }
        md.reset();
        
        if (line[0].equals("get")) {
            dht.get(key, new FindValueListener() {
                public void foundValue(KUID key, Collection values, long time) {
                    if (values != null) {
                        System.out.println("*** Found KeyValue " + key + " = " + values + " in " + time + " ms");
                    } else {
                        System.out.println("*** Lookup for KeyValue " + key + " failed after " + time + " ms");
                    }
                }
            });
        } else {
            dht.getr(key, new FindValueListener() {
                public void foundValue(KUID key, Collection values, long time) {
                    if (values != null) {
                        System.out.println("*** Found KeyValue " + key + " = " + values + " in " + time + " ms");
                    } else {
                        System.out.println("*** Lookup for KeyValue " + key + " failed after " + time + " ms");
                    }
                }
            });
        }
    }
    
    private static void load(DHT dht, String[] line) {
        if (line[1].equals("rt")) {
            Context context = dht.getContext();
            if (context.getRouteTable().load()) {
                System.out.println("RouteTable was loaded successfully");
            } else {
                System.out.println("Loading RouteTable failed");
            }
        } else {
            Context context = dht.getContext();
            if (context.getDatabase().load()) {
                System.out.println("Database was loaded successfully");
            } else {
                System.out.println("Loading Database failed");
            }
        }
    }
    
    private static void store(DHT dht, String[] line) {
        if (line[1].equals("rt")) {
            Context context = dht.getContext();
            context.getRouteTable().store();
        } else {
            Context context = dht.getContext();
            context.getDatabase().store();
        }
    }
    
    private static class BootstrapUtil implements BootstrapListener {
        
        private List dhts;
        private int index = 1;
        
        private long time = 0L;
        
        private boolean finished = false;
        
        public BootstrapUtil(List dhts) {
            this.dhts = dhts;
        }

        public void initialPhaseComplete(KUID nodeId, Collection nodes, long time) {}

        public synchronized void secondPhaseComplete(KUID nodeId, boolean foundNodes, long time) {
            if (time >= 0) {
                this.time += time;
            }
            
            bootstrap();
        }

        public synchronized void bootstrap() {
            if (index < dhts.size()) {
                try {
                    ((DHT)dhts.get(index)).bootstrap(((DHT)dhts.get(index-1)).getSocketAddress(), this);
                    index++;
                } catch (IOException err) {
                    err.printStackTrace();
                }
            } else if (!finished && !dhts.isEmpty()) {
                finished = true;
                System.out.println("*** Bootstraping finished in " + time + " ms ");
            }
        }
    }
}
