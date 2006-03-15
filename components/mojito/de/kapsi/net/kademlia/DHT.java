/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Collection;

import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValue;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.routing.RouteTable;

public class DHT implements Runnable {
    
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
    
    public Node getLocalNode() {
        return context.getLocalNode();
    }
    
    public SocketAddress getSocketAddress() {
        return context.getLocalSocketAddress();
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
    
    public void put(KUID key, byte[] value/*, StoreListener l*/) 
            throws IOException {
        
        try {
            KeyValue keyValue = 
                KeyValue.createLocalKeyValue(key, value, getLocalNode());
            context.getDatabase().add(keyValue);
        } catch (SignatureException err) {
            throw new RuntimeException(err);
        } catch (InvalidKeyException err) {
            throw new RuntimeException(err);
        }
    }
    
    public Collection get(KUID key, FindValueListener l) throws IOException {
        Collection values = context.getDatabase().get(key);
        if (values != null) {
            if (l != null) {
                l.foundValue(key, values, 0L);
            }
            return values;
        } else {
            return getr(key, l);
        }
    }
    
    // TODO only for debugging purposes public
    Collection getr(KUID key, FindValueListener l) throws IOException {
        context.get(key, l);
        return null;
    }
    
    /*public boolean remove(Value value) {
        return context.getDatabase().remove(value);
    }*/
    
    // TODO for debugging purposes only
    void lookup(KUID lookup, FindNodeListener l) throws IOException {
        context.lookup(lookup, l);
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
    RouteTable getRoutingTable() {
        return context.getRouteTable();
    }
    
    public String toString() {
        return getLocalNode().toString();
    }
}

