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

import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.LookupAdapter;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.RoutingTable;
import com.limegroup.mojito.settings.ContextSettings;


public class MojitoDHT {
    
    private static final Log LOG = LogFactory.getLog(MojitoDHT.class);
    
    private Context context;
    
    public MojitoDHT() {
        context = new Context("DHT");
    }
    
    public MojitoDHT(String name) {
        context = new Context(name);
    }
    
    public void setName(String name) {
        context.setName(name);
    }
    
    public String getName() {
        return context.getName();
    }
    
    public void setFirewalled(boolean firewalled) {
        //change from firewalled to non-firewalled? re-bootstrap
        if(context.isFirewalled() && !firewalled) {
            context.setFirewalled(firewalled);
            try {
                bootstrap(new BootstrapListener() {
                    public void phaseOneComplete(long time) {}

                    public void phaseTwoComplete(boolean foundNodes, long time) {}

                    public void noBootstrapHost() {
                    }
                });
            } catch (IOException err) {
                if(LOG.isErrorEnabled()) {
                    LOG.error("Firewalled to non-firewalled re-bootstrap error: "+err);
                }
            }
        } else {
        	context.setFirewalled(firewalled);
        }
    }
    
    public boolean isFirewalled() {
        return context.isFirewalled();
    }
    
    public void bind(SocketAddress address) throws IOException {
        context.bind(address);
    }
    
    //  TODO testing purposes only - remove
    public void bind(SocketAddress address,KUID localNodeID) throws IOException {
        context.bind(address,localNodeID);
    }
    
    public int size() {
        return context.size();
    }
    
    public KUID getLocalNodeID() {
        return context.getLocalNodeID();
    }
    
    public ContactNode getLocalNode() {
        return context.getLocalNode();
    }
    
    public SocketAddress getSocketAddress() {
        return context.getSocketAddress();
    }
    
    public SocketAddress getLocalSocketAddrss() {
        return context.getLocalSocketAddress();
    }
    
    public int getReceivedMessagesCount() {
        return context.getReceivedMessagesCount();
    }
    
    public long getReceivedMessagesSize() {
        return context.getReceivedMessagesSize();
    }
    
    public int getSentMessagesCount() {
        return context.getSentMessagesCount();
    }
    
    public long getSentMessagesSize() {
        return context.getSentMessagesSize();
    }
    
    public boolean isRunning() {
        return context.isRunning();
    }
    
    public void start() {
        context.start();
    }
    
    public void stop() {
        context.stop();
    }
    
    public void setThreadFactory(ThreadFactory threadFactory) {
        context.setThreadFactory(threadFactory);
    }
    
    public void addPingListener(PingListener listener) {
        context.addPingListener(listener);
    }
    
    public void removePingListener(PingListener listener) {
        context.removePingListener(listener);
    }
    
    public PingListener[] getPingListeners() {
        return context.getPingListeners();
    }
    
    public void addLookupListener(LookupListener listener) {
        context.addLookupListener(listener);
    }
    
    public void removeLookupListener(LookupListener listener) {
        context.removeLookupListener(listener);
    }
    
    public LookupListener[] getLookupListeners() {
        return context.getLookupListeners();
    }
    
    public void setMessageDispatcher(Class messageDispatcher) {
        context.setMessageDispatcher(messageDispatcher);
    }
    
    /**
     * This method will try to bootstrap off the given address.
     * This is a synchronous process.
     * 
     * @param address The address of the bootstrap host
     * @return The bootstrap time
     * @throws IOException
     */
    public long bootstrap(SocketAddress address) throws IOException {
        return bootstrap(address, ContextSettings.SYNC_BOOTSTRAP_TIMEOUT.getValue());
    }
    
    public long bootstrap(SocketAddress address, long timeout) throws IOException {
        final long[] time = new long[]{ -1L };
        
        synchronized (time) {
            context.bootstrap(address, new BootstrapListener() {
                    public void phaseOneComplete(long time) {
                    }

                    public void phaseTwoComplete(boolean foundNodes, long t) {
                        time[0] = t;
                        synchronized (time) {
                            time.notify();
                        }
                    }

                    public void noBootstrapHost() {
                        synchronized (time) {
                            time.notify();
                        }
                    }
            });
            try {
                time.wait(timeout);
            } catch (InterruptedException err) {
                LOG.error(err);
            }
        }
        
        return time[0];
    }
    
    /**
     * This method will try to bootstrap off known nodes from the routing table.
     * 
     * @return The bootstrap time
     * @throws IOException
     */
    public void bootstrap(BootstrapListener listener) 
            throws IOException {
        context.bootstrap(listener);
    }

    public void bootstrap(SocketAddress address, BootstrapListener listener) 
            throws IOException {
        context.bootstrap(address, listener);
    }
    
    public void bootstrap(List bootstrapHostsList, BootstrapListener listener) 
            throws IOException {
        context.bootstrap(bootstrapHostsList, listener);
    }
    
    // TODO for debugging purposes only
    long ping(SocketAddress dst) throws IOException {
        return ping(dst, ContextSettings.SYNC_PING_TIMEOUT.getValue());
    }
    
    // TODO for debugging purposes only
    long ping(SocketAddress dst, long timeout) throws IOException {
        final long[] time = new long[]{ -1L };
        
        synchronized (time) {
            context.ping(dst, new PingListener() {
                public void response(ResponseMessage response, long t) {
                    time[0] = t;
                    synchronized (time) {
                        time.notify();
                    }
                }

                public void timeout(KUID nodeId, SocketAddress address, 
                        RequestMessage request, long t) {
                    synchronized (time) {
                        time.notify();
                    }
                }
            });
            
            try {
                time.wait(timeout);
            } catch (InterruptedException err) {
                LOG.error(err);
            }
        }
        
        return time[0];
    }
    
    // TODO for debugging purposes only
    void ping(SocketAddress dst, PingListener listener) throws IOException {
        if (listener == null) {
            throw new NullPointerException("PingListener is null");
        }
        context.ping(dst, listener);
    }
    
    // TODO remove - for test purposes only
    public Context getContext() {
        return context;
    }
    
    public Collection getKeys() {
        return context.getDatabase().getKeys();
    }
    
    public Collection getValues() {
        return context.getDatabase().getValues();
    }
    
    public boolean put(KUID key, byte[] value) 
            throws IOException {
        return put(key, value, null, null);
    }
    
    public boolean put(KUID key, byte[] value, StoreListener listener) 
            throws IOException {
        return put(key, value, listener, null);
    }
    
    public boolean put(KUID key, byte[] value, StoreListener listener, PrivateKey privateKey) 
            throws IOException {
        
        try {
            KeyValue keyValue = 
                KeyValue.createLocalKeyValue(key, value, getLocalNode());
            
            if (privateKey == null) {
                keyValue.sign(context.getKeyPair());
            } else {
                keyValue.sign(privateKey);
            }
            
            Database database = context.getDatabase();
            synchronized(database) {
                if (database.add(keyValue) 
                        || database.isTrustworthy(keyValue)) {
                    context.store(keyValue, listener);
                    return true;
                }
            }
        } catch (InvalidKeyException e) {
            LOG.error(e);
        } catch (SignatureException e) {
            LOG.error(e);
        }
        
        return false;
    }
    
    public Collection get(KUID key) throws IOException {
        return get(key, ContextSettings.SYNC_GET_VALUE_TIMEOUT.getValue());
    }
    
    public Collection get(KUID key, long timeout) throws IOException {
        final Collection[] values = new Collection[] {
            Collections.EMPTY_LIST
        };
        
        synchronized (values) {
            context.get(key, new LookupAdapter() {
                public void finish(KUID lookup, Collection c, long time) {
                    values[0] = c;
                    synchronized (values) {
                        values.notify();
                    }
                }
            });
            
            try {
                values.wait(timeout);
            } catch (InterruptedException err) {
                LOG.error(err);
            }
        }
        
        return values[0];
    }
    
    public void get(KUID key, LookupListener listener) throws IOException {
        if (!key.isValueID()) {
            throw new IllegalArgumentException("Key must be a Value ID");
        }
        
        if (listener == null) {
            throw new NullPointerException("LookupListener is null");
        }
        
        context.get(key, listener);
    }
    
    public boolean remove(KUID key) throws IOException {
        return remove(key, null, null);
    }
    
    public boolean remove(KUID key, StoreListener listener) throws IOException {
        return remove(key, listener, null);
    }

    public boolean remove(KUID key, StoreListener listener, PrivateKey privateKey) throws IOException {
        // To remove a KeyValue you just store an empty value!
        return put(key, new byte[0], listener, privateKey);
    }

    // TODO for debugging purposes only
    void lookup(KUID lookup, LookupListener listener) throws IOException {
        context.lookup(lookup, listener);
    }
    
    // TODO for debugging purposes only
    Collection getNodes() {
        return context.getRouteTable().getAllNodes();
    }
    
    // TODO for debugging purposes only
    Database getDatabase() {
        return context.getDatabase();
    }
    
    // TODO for debugging purposes only
    RoutingTable getRoutingTable() {
        return context.getRouteTable();
    }
    
    public String toString() {
        return getLocalNode().toString();
    }
}

