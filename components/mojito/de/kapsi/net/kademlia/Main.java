/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.routing.RouteTable;
import de.kapsi.net.kademlia.settings.NetworkSettings;
import de.kapsi.net.kademlia.util.ArrayUtils;

public class Main {
    
    public static void main(String[] args) throws Exception {
        
        int count = 10;
        String host = null;
        int port = NetworkSettings.getPort();
        
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
                //Thread.sleep(100);
                
                dhts.add(dht);
                System.out.println(i + ": " + ((DHT)dhts.get(dhts.size()-1)).getLocalNode());
            } catch (IOException err) {
                System.err.println("Failed to start/connect DHT #" + i);
                err.printStackTrace();
            }
            
            //Thread.sleep(100);
        }
        
        new BootstrapUtil(dhts).bootstrap();
        
        int current = 0;
        DHT dht = (DHT)dhts.get(current);
        
        String help = "help";
        String info = "info";
        String svitch = "switch \\d{1,}";
        String ping = "ping (\\w|\\d|\\.)+ \\d{1,5}";
        String bootstrap = "bootstrap (\\w|\\d|\\.)+ \\d{1,5}";
        String put = "put (key|kuid) (\\w|\\d)+ (value|file) .+";
        String get = "get (key|kuid) (\\w|\\d)+";
        String getr = "getr (key|kuid) (\\w|\\d)+";
        String listDB = "list db";
        String listRT = "list rt";
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
                } else if (line.matches(listDB)) {
                    list(dht, line.split(" "));
                } else if (line.matches(listRT)) {
                    list(dht, line.split(" "));
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
        System.out.println("Local Node: " + dht.getLocalNode());
        System.out.println("Database Size: " + dht.getDatabase().size());
        System.out.println("RouteTable Size: " + dht.getRoutingTable().size());
    }
    
    private static void list(DHT dht, String[] line) throws Throwable {
        StringBuffer buffer = new StringBuffer("\n");
        
        if(line[1].equals("db")) {
            Database database = dht.getDatabase();
            Collection values = database.getValues();
            for(Iterator it = values.iterator(); it.hasNext(); ) {
                KeyValue value = (KeyValue)it.next();
                
                buffer.append("VALUE: ").append(value).append("\n\n");
            }
            buffer.append("-------------\n");
            buffer.append("TOTAL: " + values.size()).append("\n");
        } else {
            RouteTable routingTable = dht.getRoutingTable();
            Map map = routingTable.getRouteTableMap();
            for(Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                Entry entry = (Entry)it.next();
                Node node = (Node)entry.getValue();
                
                buffer.append(node).append("\n");
            }
            buffer.append("-------------\n");
            buffer.append("TOTAL: " + map.size()).append("\n");
        }
        
        System.out.println(buffer);
    }
    
    private static void ping(DHT dht, String[] line) throws Throwable {
        String host = line[1];
        int port = Integer.parseInt(line[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        System.out.println("Ping... " + addr);
        dht.ping(addr, new PingListener() {
            public void ping(KUID nodeId, SocketAddress address, long time) {
                if (time >= 0L) {
                    if (nodeId != null) {
                        System.out.println("*** Ping to " + Node.toString(nodeId, address) + " succeeded: " + time + "ms");
                    } else {
                        System.out.println("*** Ping to " + address + " succeeded: " + time + "ms");
                    }
                } else {
                    if (nodeId != null) {
                        System.out.println("*** Ping to " + Node.toString(nodeId, address) + " failed");
                    } else {
                        System.out.println("*** Ping to " + address + " failed");
                    }
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
            public void bootstrap(KUID nodeId, Collection nodes, long time) {
                System.out.println("*** Bootstraping " + (!nodes.isEmpty() ? "succeded" : "failed") + " in " + time + " ms");
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
        dht.put(key, value);/*, new StoreListener() {
            public void store(KeyValue value, Collection nodes) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("STORED KEY: ").append(value.getKey()).append("\n");
                for(int i = 0; i < nodes.size(); i++) {
                    buffer.append(i).append(": ").append(nodes.get(i)).append("\n");
                }
                System.out.println(buffer.toString());
            }
        });*/
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
    
    private static class BootstrapUtil implements BootstrapListener {
        
        private List dhts;
        private int index = 1;
        
        private long time = 0L;
        
        public BootstrapUtil(List dhts) {
            this.dhts = dhts;
        }

        public synchronized void bootstrap() {
            if (index < dhts.size()) {
                try {
                    ((DHT)dhts.get(index)).bootstrap(((DHT)dhts.get(index-1)).getSocketAddress(), this);
                    index++;
                } catch (IOException err) {
                    err.printStackTrace();
                }
            } else if (dhts.size() > 1) {
                System.out.println("*** Bootstraping finished in " + time + " ms");
            }
        }
        
        public void bootstrap(KUID nodeId, Collection nodes, long time) {
            if (time >= 0) {
                this.time += time;
            }
            bootstrap();
        }
    }
}
