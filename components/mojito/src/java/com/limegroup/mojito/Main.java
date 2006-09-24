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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.limegroup.gnutella.dht.impl.LimeDHTManager;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.LIFOSet;
import com.limegroup.gnutella.version.UpdateInformation;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.BootstrapEvent.EventType;
import com.limegroup.mojito.settings.RouteTableSettings;

public class Main {
    
    public static void main(String[] args) throws Exception {
        
        int count = 0;
        int port = 0;
        
        InetSocketAddress bootstrapHost = null;
        
        if (args.length != 2 && args.length != 4) {
            System.out.println("java Main count port");
            System.out.println("java Main count port host port");
            System.exit(-1);
        } else {
            count = Integer.parseInt(args[0]);
            port = Integer.parseInt(args[1]);
            
            if (args.length >= 4) {
                bootstrapHost = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            }/* else {
                bootstrapHost = new InetSocketAddress("localhost", port);
            }*/
        }
        
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.PORT.setValue(port);
        ConnectionSettings.FORCED_PORT.setValue(port);
        
        List<MojitoDHT> dhts = standalone(null, port, count);
        //List<MojitoDHT> dhts = limewire(null, port);
        
        if (dhts.isEmpty()) {
            System.out.println("No Nodes to run");
            System.exit(0);
        }
        
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            System.out.println("WARNING: LOCAL_IS_PRIVATE is set to false!");
        }
        
        run(port, dhts, bootstrapHost);
    }
    
    private static List<MojitoDHT> standalone(InetAddress addr, int port, int count) {
        if (count > 1) {
            RouteTableSettings.UNIFORM_BUCKET_REFRESH_DISTRIBUTION.setValue(true);
        }
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>(count);
        
        for(int i = 0; i < count; i++) {
            try {
                //MojitoDHT dht = new MojitoDHT("DHT" + i, false);
                MojitoDHT dht = MojitoFactory.createDHT("DHT" + i);
                
                if (addr != null) {
                    dht.bind(new InetSocketAddress(addr, port+i));
                } else {
                    dht.bind(new InetSocketAddress(port+i));
                }
                
                //dht.start();

                dhts.add(dht);
                System.out.println(i + ": " + ((Context)dhts.get(dhts.size()-1)).getLocalNode());
            } catch (IOException err) {
                System.err.println("Failed to start/connect DHT #" + i);
                err.printStackTrace();
            }
        }
        
        return dhts;
    }
    
    @SuppressWarnings("unused")
    private static List<MojitoDHT> limewire(InetAddress addr, int port) throws Exception {
        RouterService service = new RouterService(new DoNothing());
        RouterService.preGuiInit();
        service.start();
        
        LimeDHTManager manager = (LimeDHTManager)RouterService.getDHTManager();
        MojitoDHT dht = manager.getMojitoDHT();
        if (addr != null) {
            dht.bind(new InetSocketAddress(addr, port));
        } else {
            dht.bind(new InetSocketAddress(port));
        }
        //dht.start();
        
        return Arrays.asList(dht);
    }
    
    private static void run(int port, List<MojitoDHT> dhts, SocketAddress bootstrapHost) throws Exception {
        long time = 0L;
        DHTFuture<BootstrapEvent> future = null;
        
        Set<SocketAddress> bootstrapHostSet = new LIFOSet<SocketAddress>();
        
        /*SocketAddress[] hosts = { 
            new InetSocketAddress("www.apple.com", 80), 
            new InetSocketAddress("www.microsoft.com", 80), 
            new InetSocketAddress("www.google.com", 80),
            new InetSocketAddress("www.t-mobile.com", 80),
            new InetSocketAddress("www.verizon.com", 80),
            new InetSocketAddress("www.limewire.org", 80),
            new InetSocketAddress("www.cnn.com", 80), 
            new InetSocketAddress("www.scifi.com", 80), 
            new InetSocketAddress("192.168.1.5", 80),
            new InetSocketAddress("www.gnutella.com", 80),
            new InetSocketAddress("www.limewire.org", 80), 
            new InetSocketAddress("slashdot.org", 80), 
            new InetSocketAddress("www.altavista.com", 80), 
            new InetSocketAddress("www.ask.com", 80),
            new InetSocketAddress("www.vodafone.co.uk", 80),
            new InetSocketAddress("www.n-tv.de", 80),
            new InetSocketAddress("www.n24.de", 80), 
            host
        };
        
        future = dhts.get(i).bootstrap(new LinkedHashSet<SocketAddress>(Arrays.asList(hosts)));*/
        
        if(bootstrapHost != null) {
            bootstrapHostSet.add(bootstrapHost);
        } else {
            bootstrapHostSet.add(new InetSocketAddress("localhost", port));
        }
        
        int start = 0;
        if (bootstrapHost == null) {
            dhts.get(0).start();
            start = 1;
            
            // 1...n bootstraps from 0
        }
        
        for(int i = start; i < dhts.size(); i++) {
            try {
                MojitoDHT dht = dhts.get(i);
                dht.start();
                
                future = dht.bootstrap(bootstrapHostSet);
                
                BootstrapEvent evt = future.get();
                //bootstrapHostSet.add(dht.getContactAddress());
                
                time += evt.getTotalTime();
                
                if (evt.getEventType().equals(EventType.BOOTSTRAP_SUCCEEDED)) {    
                    System.out.println("Node #" + i + " finished bootstrapping from " 
                            + bootstrapHostSet + " in " + evt.getTotalTime() + "ms");
                } else {
                    System.out.println("Node #" + i + " failed to bootstrap from " 
                            + bootstrapHostSet + "\n" + evt);
                }
            } catch (Exception err) {
                System.out.println("Node #" + i + " failed to bootstrap from " + bootstrapHostSet);
                err.printStackTrace();
            }
        }
        System.out.println("All Nodes finished bootstrapping in " + time + "ms");
        
        /*future.addDHTEventListener(new BootstrapListener() {
            public void handleException(Exception ex) {
                System.out.println(ex);
            }

            public void handleResult(BootstrapEvent result) {
                System.out.println(result);
            }
        });*/
        
        int current = 0;
        MojitoDHT dht = dhts.get(current);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        
        while(true) {
            System.out.print("[" + current + "] $ ");
            String line = in.readLine().trim();
            if (line.equals("")) {
                continue;
            }
            
            try {
                if (line.indexOf("quit") >= 0) {
                    for(int i = 0; i < dhts.size(); i++) {
                        dhts.get(i).stop();
                    }
                    System.exit(0);
                } else if (line.indexOf("switch") >= 0) {
                    int index = Integer.parseInt(line.split(" ")[1]);
                    dht = dhts.get(index);
                    current = index;
                    CommandHandler.info(dht, null, out);
                } else if (line.indexOf("help") >= 0) {
                    out.println("quit");
                    out.println("switch \\d");
                    CommandHandler.handle(dht, line, out);
                } else if (line.indexOf("load") >= 0) {
                    MojitoDHT d = CommandHandler.load(dht, line.split(" "), out);
                    dht.stop();
                    dhts.set(current, d);
                    d.setThreadFactory(dht.getThreadFactory());
                    //System.out.println(dht.getLocalAddress());
                    //System.out.println(d.getLocalAddress());
                    d.bind(dht.getLocalAddress());
                    d.start();
                    dht = d;
                    CommandHandler.info(dht, null, out);
                } else {
                    CommandHandler.handle(dht, line, out);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                out.flush();
            }
        }
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