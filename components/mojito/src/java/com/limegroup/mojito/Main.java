/*
 * Mojito Distributed Hash Tabe (DHT)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.version.UpdateInformation;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.util.ArrayUtils;
import com.limegroup.mojito.util.ContactUtils;


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
        
        List<MojitoDHT> dhts = standalone(addr, port, count);
        //List<MojitoDHT> dhts = limewire(addr, port);
        
        run(addr, port, dhts);
    }
    
    private static List<MojitoDHT> standalone(InetAddress addr, int port, int count) {
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>(count);
        
        for(int i = 0; i < count; i++) {
            try {
                MojitoDHT dht = new MojitoDHT("DHT" + i);
                
                if (addr != null) {
                    dht.bind(new InetSocketAddress(addr, port+i));
                } else {
                    dht.bind(new InetSocketAddress(port+i));
                }
                
                dht.start();

                dhts.add(dht);
                System.out.println(i + ": " + ((MojitoDHT)dhts.get(dhts.size()-1)).getLocalNode());
            } catch (IOException err) {
                System.err.println("Failed to start/connect DHT #" + i);
                err.printStackTrace();
            }
        }
        
        return dhts;
    }
    
    private static List<MojitoDHT> limewire(InetAddress addr, int port) throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.PORT.setValue(port);
        ConnectionSettings.FORCED_PORT.setValue(port);
        
        RouterService service = new RouterService(new DoNothing());
        RouterService.preGuiInit();
        service.start();
        
        MojitoDHT dht = RouterService.getLimeDHTManager().getMojitoDHT();
        if (addr != null) {
            dht.bind(new InetSocketAddress(addr, port));
        } else {
            dht.bind(new InetSocketAddress(port));
        }
        dht.start();
        
        return Arrays.asList(dht);
    }
    
    private static void run(InetAddress addr, int port, List<MojitoDHT> dhts) throws Exception {
        long time = 0L;
        for(int i = 1; i < dhts.size(); i++) {
            long t = dhts.get(i).bootstrap(dhts.get(i-1).getSocketAddress());
            
            if (t >= 0L) {
                time += t;
                System.out.println("Node #" + i + " finished bootstrapping in " + t + "ms");
            } else {
                System.out.println("Node #" + i + " failed to bootstrap");
            }
        }
        System.out.println("All Nodes finished bootstrapping in " + time + "ms");
        
        int current = 0;
        MojitoDHT dht = dhts.get(current);
        
        String help = "help";
        String info = "info";
        String svitch = "switch \\d{1,}";
        String ping = "ping .+ \\d{1,5}";
        String bootstrap = "bootstrap .+ \\d{1,5}";
        String put = "put (key|kuid) (\\w|\\d)+ (value|file) .+";
        String remove = "remove (key|kuid) (\\w|\\d)+";
        String get = "get (key|kuid) (\\w|\\d)+";
        String getr = "getr (key|kuid) (\\w|\\d)+";
        String getall = "get exhaustive (key|kuid) (\\w|\\d)+";
        String listDB = "list db";
        String listRT = "list rt";
        String store = "store .+";
        String load = "load .+";
        String kill = "kill";
        String stats = "stats";
        String restart = "restart";
        String quit = "quit";
        String reqstats = "reqstats .+ \\d{1,5} (stats|rt|db)";
        String firewalled = "firewalled";
        String exhaustive = "exhaustive";
        
        String[] commands = {
                help,
                info,
                svitch,
                ping,
                reqstats,
                bootstrap,
                put,
                remove,
                get,
                getr,
                listDB,
                listRT,
                store,
                load,
                kill,
                stats,
                restart,
                quit,
                reqstats,
                firewalled,
                exhaustive
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
                    dht = dhts.get(index);
                    current = index;
                    info(dht);
                } else if (line.matches(firewalled)) {
                    dht.setFirewalled(!dht.isFirewalled());
                    info(dht);
                } else if (line.equals(exhaustive)) {
                    KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.setValue(
                            !KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
                } else if (line.matches(ping)) {
                    ping(dht, line.split(" "));
                } else if (line.matches(reqstats)) {
                    reqstats(dht, line.split(" "));
                } else if (line.matches(bootstrap)) {
                    bootstrap(dht, line.split(" "));
                } else if (line.matches(put)) {
                    put(dht, line.split(" "));
                } else if (line.matches(remove)) {
                    remove(dht, line.split(" "));
                } else if (line.matches(get)) {
                    get(dht, line.split(" "));
                } else if (line.matches(getr)) {
                    get(dht, line.split(" "));
                } else if (line.matches(getall)) {
                    get(dht, line.split(" "));
                } else if (line.matches(listDB) || line.matches(listRT)) {
                    list(dht, line.split(" "));
                } else if (line.matches(store)) {
                    store(dht, line.split(" "));
                } else if (line.matches(load)) {
                    MojitoDHT nu = load(line.split(" "));
                    dht.stop();
                    
                    nu.bind(dht.getLocalAddress());
                    dhts.set(current, nu);
                    
                    nu.start();
                    dht = nu;
                    
                } else if (line.matches(kill)) {
                    if (dht.isRunning()) {
                        dht.stop();
                    }
                } else if (line.matches(stats)) {
                    DHTStats dhtStats = dht.getContext().getDHTStats();
                    OutputStreamWriter writer = new OutputStreamWriter(System.out);
                    dhtStats.dumpStats(writer, true);
                } else if (line.matches(restart)) {
                    if (!dht.isRunning()) {
                        dht = new MojitoDHT();
                        if (addr != null) {
                            dht.bind(new InetSocketAddress(addr, port+current));
                        } else {
                            dht.bind(new InetSocketAddress(port+current));
                        }
                        dht.start();
                        dhts.set(current, dht);
                    }
                } else if (line.matches(quit)) {
                    for(int i = 0; i < dhts.size(); i++) {
                        ((MojitoDHT)dhts.get(i)).stop();
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
    
    private static void info(MojitoDHT dht) throws Throwable {
        System.out.println("Local ContactNode: " + dht.getLocalNode());
        System.out.println("Is running: " + dht.isRunning());
        System.out.println("Database Size: " + dht.getDatabase().size());
        System.out.println("RouteTable Size: " + dht.getRoutingTable().getNodes().size());
        System.out.println("Size: " + dht.size());
    }
    
    private static void list(MojitoDHT dht, String[] line) throws Throwable {
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
            buffer.append(routingTable.toString());
        }
        
        System.out.println(buffer);
    }
    
    private static void ping(MojitoDHT dht, String[] line) throws Throwable {
        String host = line[1];
        int port = Integer.parseInt(line[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        System.out.println("Pinging... " + addr);
        dht.ping(addr, new PingListener() {
            public void response(ResponseMessage response, long time) {
                System.out.println("*** Ping to " + response.getContact() + " succeeded: " + time + "ms");
            }

            public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
                if (nodeId != null) {
                    System.out.println("*** Ping to " + ContactUtils.toString(nodeId, address) + " failed");
                } else {
                    System.out.println("*** Ping to " + address + " failed");
                }
            }
        });
    }
    
    private static void reqstats(MojitoDHT dht, String[] line) throws Exception {
    }
    
    private static void bootstrap(MojitoDHT dht, String[] line) throws Throwable {
        String host = line[1];
        int port = Integer.parseInt(line[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        System.out.println("Bootstraping... " + addr);
        dht.bootstrap(addr, new BootstrapListener() {
            public void phaseOneComplete(long time) {
                System.out.println("*** Bootstraping phase 1 finished in " + time + " ms");
            }

            public void phaseTwoComplete(boolean foundNodes, long time) {
                System.out.println("*** Bootstraping phase 2 " + (foundNodes ? "succeded" : "failed") + " in " + time + " ms");
            }

            public void noBootstrapHost() {
            }
        });
    }
    
    private static void put(MojitoDHT dht, final String[] line) throws Throwable {
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
            public void store(KeyValue keyValue, Collection nodes) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("STORED KEY_VALUES: ").append(keyValue).append("\n");
                int i = 0;
                for (Iterator iter = nodes.iterator(); iter.hasNext();) {
                    Contact node = (Contact) iter.next();
                    buffer.append(i).append(": ").append(node).append("\n");
                    i++;
                }
                System.out.println(buffer.toString());
            }
        });
    }
    
    private static void remove(MojitoDHT dht, final String[] line) throws Throwable {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        
        KUID key = null;

        if (line[1].equals("kuid")) {
            key = KUID.createValueID(ArrayUtils.parseHexString(line[2]));
        } else {
            key = KUID.createValueID(md.digest(line[2].getBytes("UTF-8")));
        }
        md.reset();
        
        System.out.println("Removing... " + key);
        dht.remove(key, new StoreListener() {
            public void store(KeyValue keyValue, Collection nodes) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("REMOVED KEY_VALUES: ").append(keyValue).append("\n");
                int i = 0;
                for (Iterator iter = nodes.iterator(); iter.hasNext();) {
                    Contact node = (Contact) iter.next();
                    buffer.append(i).append(": ").append(node).append("\n");
                    i++;
                }
                System.out.println(buffer.toString());
            }
        });
    }
    
    private static void get(MojitoDHT dht, String[] line) throws Throwable {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        
        KUID key = null;
        boolean exhaustive = line[1].equals("exhaustive");
        if(exhaustive) {
            if (line[2].equals("kuid")) {
                key = KUID.createValueID(ArrayUtils.parseHexString(line[3]));
            } else {
                key = KUID.createValueID(md.digest(line[3].getBytes("UTF-8")));
            }
        } else {
            if (line[1].equals("kuid")) {
                key = KUID.createValueID(ArrayUtils.parseHexString(line[2]));
            } else {
                key = KUID.createValueID(md.digest(line[2].getBytes("UTF-8")));
            }
        }
        md.reset();
        
        /*dht.get(key, new LookupAdapter() {
            public void found(KUID key, Collection values, long time) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(key).append(" in ").append(time).append("ms\n");
                buffer.append(values);
                buffer.append("\n");
                System.out.println(buffer.toString());
            }

            public void finish(KUID lookup, Collection c, long time) {
                if (c.isEmpty()) {
                    System.out.println(lookup + " was not found after " + time + "ms");
                } else {
                    System.out.println("Lookup for " + lookup + " finished after " 
                            + time + "ms and " + c.size() + " found locations");
                }
            }
        });*/
        
        long start = System.currentTimeMillis();
        Collection c = dht.get(key);
        long time = System.currentTimeMillis() - start;
        
        if (!c.isEmpty()) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(key).append(" in ").append(time).append("ms\n");
            buffer.append(c);
            buffer.append("\n");
            System.out.println(buffer.toString());
        } else {
            System.out.println(key + " was not found after " + time + "ms");
        }
        
        System.out.println();
    }
    
    private static MojitoDHT load(String[] line) throws Throwable {
        File file = new File(line[1]);
        System.out.println("Loading: " + file);
        FileInputStream fis = new FileInputStream(file);
        
        try {
            return MojitoDHT.load(fis);
        } finally {
            fis.close();
        }
    }
    
    private static void store(MojitoDHT dht, String[] line) throws Exception {
        File file = new File(line[1]);
        System.out.println("Storing: " + file);
        FileOutputStream fos = new FileOutputStream(file);
        dht.store(fos, true, true);
        fos.close();
    }
    
    private static class DoNothing implements ActivityCallback {

        public void acceptChat(Chatter ctr) {
        }

        public void acceptedIncomingChanged(boolean status) {
        }

        public void addressStateChanged() {
        }

        public void addUpload(Uploader u) {
        }

        public void browseHostFailed(GUID guid) {
        }

        public void chatErrorMessage(Chatter chatter, String str) {
        }

        public void chatUnavailable(Chatter chatter) {
        }

        public void componentLoading(String component) {
        }

        public void connectionClosed(Connection c) {
        }

        public void connectionInitialized(Connection c) {
        }

        public void connectionInitializing(Connection c) {
        }

        public void disconnected() {
        }

        public void fileManagerLoaded() {
        }

        public void fileManagerLoading() {
        }

        public void handleFileEvent(FileManagerEvent evt) {
        }

        public boolean handleMagnets(MagnetOptions[] magnets) {
            return false;
        }

        public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set locs) {
        }

        public void handleQueryString(String query) {
        }

        public void handleSharedFileUpdate(File file) {
        }

        public boolean isQueryAlive(GUID guid) {
            return false;
        }

        public void receiveMessage(Chatter chr) {
        }

        public void removeUpload(Uploader u) {
        }

        public void restoreApplication() {
        }

        public void setAnnotateEnabled(boolean enabled) {
        }

        public void updateAvailable(UpdateInformation info) {
        }

        public void uploadsComplete() {
        }

        public boolean warnAboutSharingSensitiveDirectory(File dir) {
            return false;
        }

        public void addDownload(Downloader d) {
        }

        public void downloadsComplete() {
        }

        public String getHostValue(String key) {
            return null;
        }

        public void promptAboutCorruptDownload(Downloader dloader) {
        }

        public void removeDownload(Downloader d) {
        }

        public void showDownloads() {
        }

        public void handleAddressStateChanged() {
        }

        public void handleLifecycleEvent(LifecycleEvent evt) {
        }
    }
}
