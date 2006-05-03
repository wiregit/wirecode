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

import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.LookupListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.event.StoreListener;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.routing.RoutingTable;

public class DHT implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(DHT.class);
    
    private Context context;
    
    public DHT() {
        context = new Context();
    }
    
    public void bind(SocketAddress address) throws IOException {
        context.bind(address);
    }
    
    //  TODO testing purposes only - remove
    public void bind(SocketAddress address,KUID localNodeID) throws IOException {
        context.bind(address,localNodeID);
    }
    
    public void run() {
        context.run();
    }
    
    public int size() {
        return context.size();
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
    
    public SocketAddress getExternalSocketAddress() {
        return context.getExternalSocketAddress();
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
    
    public void close() throws IOException {
        context.close();
    }
    
    public void bootstrap(SocketAddress address, BootstrapListener l) 
            throws IOException {
        context.bootstrap(address, l);
    }
    
    // TODO for debugging purposes only
    void ping(SocketAddress dst, PingListener l) throws IOException {
        context.ping(dst, l);
    }
    
    // TODO remove - for test purposes only
    public Context getContext() {
        return context;
    }
    
    public void put(KUID key, byte[] value, StoreListener l) 
            throws IOException {
        
        try {
            KeyValue keyValue = 
                KeyValue.createLocalKeyValue(key, value, getLocalNode());
            Database database = context.getDatabase();
            synchronized(database) {
                if(database.add(keyValue)){
                    context.store(keyValue, l);
                    keyValue.setRepublishTime(System.currentTimeMillis());
                }
            }
        } catch (SignatureException err) {
            throw new RuntimeException(err);
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        }
    }
    
    public Collection get(KUID key) throws IOException {
        return get(key, 0L);
    }
    
    public Collection get(KUID key, long timeout) throws IOException {
        final Collection[] values = new Collection[] {
            Collections.EMPTY_LIST
        };
        
        synchronized (values) {
            context.get(key, new LookupListener() {
                public void response(ResponseMessage response, long time) {}

                public void timeout(KUID nodeId, SocketAddress address, 
                        RequestMessage request, long time) {}
                
                public void found(KUID lookup, Collection c, long time) {
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
        if (listener == null) {
            throw new NullPointerException("LookupListener is null");
        }
        
        context.get(key, listener);
    }
    
    /*public boolean remove(Value value) {
        return context.getDatabase().remove(value);
    }*/
    
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

