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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.BootstrapListener;
import com.limegroup.mojito.event.FindNodeListener;
import com.limegroup.mojito.event.FindValueListener;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.security.CryptoHelper;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.util.KeyValueCollection;

/**
 * The Mojito DHT. All you need to know is here!
 */
public class MojitoDHT {
    
    private static final Log LOG = LogFactory.getLog(MojitoDHT.class);
    
    private Context context;
    
    public MojitoDHT() {
        this(null, null, CryptoHelper.createKeyPair());
    }
    
    public MojitoDHT(String name) {
        this(name, null, CryptoHelper.createKeyPair());
    }
    
    private MojitoDHT(String name, Contact local, KeyPair keyPair) {
        if (name == null) {
            name = "DHT";
        }
        
        if (local == null) {
            int vendor = ContextSettings.VENDOR.getValue();
            int version = ContextSettings.VERSION.getValue();
            
            KUID nodeId = KUID.createRandomNodeID();
            SocketAddress addr = new InetSocketAddress(0);
            int flags = 0;
            int instanceId = 0;
            
            local = new ContactNode(vendor, version, nodeId, addr, instanceId, flags, State.UNKNOWN);
        }

        context = new Context(name, local, keyPair);
    }
    
    public String getName() {
        return context.getName();
    }
    
    public void setFirewalled(boolean firewalled) {
        // change from firewalled to non-firewalled? re-bootstrap
        if (context.isFirewalled() && !firewalled) {
            context.setFirewalled(false);
            try {
                bootstrap(new BootstrapListener() {
                    public void phaseOneComplete(long time) {}
                    public void phaseTwoComplete(boolean foundNodes, long time) {}
                    public void noBootstrapHost(List<? extends SocketAddress> failedHosts) {}
                    
                });
            } catch (IOException err) {
                LOG.error("Firewalled to non-firewalled re-bootstrap error: ", err);
            }
        } else {
            context.setFirewalled(firewalled);
        }
    }
    
    public boolean isFirewalled() {
        return context.isFirewalled();
    }
    
    public void bind(InetAddress addr, int port) throws IOException {
        bind(new InetSocketAddress(addr, port));
    }
    
    public void bind(SocketAddress address) throws IOException {
        context.bind(address);
    }
    
    public int size() {
        return context.size();
    }
    
    public KUID getLocalNodeID() {
        return context.getLocalNodeID();
    }
    
    public Contact getLocalNode() {
        return context.getLocalNode();
    }
    
    public SocketAddress getSocketAddress() {
        return context.getSocketAddress();
    }
    
    public SocketAddress getLocalAddress() {
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
    
    public void setMessageFactory(MessageFactory messageFactory) {
        context.setMessageFactory(messageFactory);
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
    
    /*public void addLookupListener(LookupListener listener) {
        context.addLookupListener(listener);
    }
    
    public void removeLookupListener(LookupListener listener) {
        context.removeLookupListener(listener);
    }
    
    public LookupListener[] getLookupListeners() {
        return context.getLookupListeners();
    }*/
    
    public void setMessageDispatcher(Class<? extends MessageDispatcher> messageDispatcher) {
        context.setMessageDispatcher(messageDispatcher);
    }
    
    public RouteTable setRoutingTable(Class<? extends RouteTable> routeTable) {
        return context.setRoutingTable(routeTable);
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
            bootstrap(address, new BootstrapListener() {
                public void phaseOneComplete(long time) {
                }

                public void phaseTwoComplete(boolean foundNodes, long t) {
                    time[0] = t;
                    synchronized (time) {
                        time.notify();
                    }
                }

                public void noBootstrapHost(List<? extends SocketAddress> failedHosts) {
                    synchronized (time) {
                        time.notify();
                    }
                }
            });
            
            try {
                time.wait(timeout);
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
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
        bootstrap(Arrays.asList(address), listener);
    }
    
    /**
     * Tries to bootstrap from a List of Hosts.
     * 
     * @param hostList a List of <tt>SocketAddress</tt>
     * @param listener The listener for bootstrap events
     * @throws IOException
     */
    public void bootstrap(List<? extends SocketAddress> hostList, BootstrapListener listener) 
            throws IOException {
        context.bootstrap(hostList, listener);
    }
    
    /**
     * Synchronous ping. 
     * If the ping is successfull, the given host may be added to the routing table
     * 
     * @param dst
     * @return The responding <tt>ContactNode</tt> or null if there was a timeout
     * @throws IOException
     */
    public Future<Contact> ping(SocketAddress dst) throws IOException {
        //return ping(dst, ContextSettings.SYNC_PING_TIMEOUT.getValue());
        return context.ping(dst);
    }
    
    private Contact ping(SocketAddress dst, long timeout) throws IOException {
        final Contact[] node = new Contact[] {null};
        
        synchronized (node) {
            ping(dst, new PingListener() {
                public void handleResult(Contact result) {
                    node[0] = result;
                    synchronized (node) {
                        node.notify();
                    }
                }
                
                public void handleException(Exception ex) {
                    synchronized (node) {
                        node.notify();
                    }
                }
            });
            
            try {
                node.wait(timeout);
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
            }
        }
        return node[0];
    }
    
    /**
     * Asynchronous ping.
     * If the ping is successfull, the given host may be added to the routing table
     * 
     * @param dst
     * @param listener
     * @throws IOException
     */
    public void ping(SocketAddress dst, PingListener listener) throws IOException {
        if (listener == null) {
            throw new NullPointerException("PingListener is null");
        }
        context.ping(dst, listener);
    }
    
    // TODO remove - for test purposes only
    public Context getContext() {
        return context;
    }
    
    public int getVersion() {
        return context.getVersion();
    }
    
    public Set<KUID> getKeys() {
        return context.getDatabase().getKeys();
    }
    
    public Collection<KeyValue> getValues() {
        return context.getDatabase().getValues();
    }
    
    // TODO for debugging purposes only
    Collection<Contact> getNodes() {
        return context.getRouteTable().getContacts();
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
                if (DatabaseSettings.SIGN_KEY_VALUES.getValue()) {
                    keyValue.sign(context.getPrivateKey());
                    keyValue.setPublicKey(context.getPublicKey());
                }
            } else {
                keyValue.sign(privateKey);
                
                if (!keyValue.verify(context.getMasterKey())) {
                    throw new SignatureException("Cannot store " + keyValue 
                            + " because signature does not match with the master key");
                }
            }
            
            Database database = context.getDatabase();
            synchronized(database) {
                if (database.add(keyValue) 
                        || database.isTrustworthy(keyValue)) {
                    
                    // Create a new KeyPair every time we have removed
                    // all local KeyValues.
                    if (database.getLocalValueCount() == 0) {
                        context.createNewKeyPair();
                    }
                    
                    context.store(keyValue, listener);
                    return true;
                }
            }
        } catch (InvalidKeyException e) {
            LOG.error("InvalidKeyException", e);
        } catch (SignatureException e) {
            LOG.error("SignatureException", e);
        }
        
        return false;
    }
    
    public Future<List<KeyValueCollection>> get(KUID key) throws IOException {
        //return get(key, ContextSettings.SYNC_GET_VALUE_TIMEOUT.getValue());
        return context.get(key, null);
    }
    
    @SuppressWarnings("unchecked")
    public Collection<KeyValue> get(KUID key, long timeout) throws IOException {
        final Collection[] values = new Collection[] {
            Collections.emptyList()
        };
        
        synchronized (values) {
            get(key, new FindValueListener() {
                public void handleResult(List<KeyValueCollection> result) {
                    values[0] = result;
                    synchronized (values) {
                        values.notify();
                    }
                }
                
                public void handleException(Exception ex) {
                }
                
            });
            
            try {
                values.wait(timeout);
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
            }
        }
        
        return values[0];
    }
    
    public Future<List<KeyValueCollection>> get(KUID key, FindValueListener listener) throws IOException {
        if (!key.isValueID()) {
            throw new IllegalArgumentException("Key must be a Value ID");
        }
        
        if (listener == null) {
            throw new NullPointerException("FindValueListener is null");
        }
        
        return context.get(key, listener);
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
    void lookup(KUID lookup, FindNodeListener listener) throws IOException {
        context.lookup(lookup, listener);
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
    
    private static final long SERIAL_VERSION_UID = 0;
    
    /**
     * Writes the current state of Monjito DHT to the OutputStream.
     */
    public void store(OutputStream out) throws IOException {
        store(out, true, true);
    }
    
    /**
     * Writes the current state of Monjito DHT to the OutputStream.
     */
    public synchronized void store(OutputStream out, 
            boolean storeRouteTable, boolean storeDatabase) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        
        // SERIAL_VERSION_UID
        oos.writeLong(SERIAL_VERSION_UID);
        
        // Time
        oos.writeLong(System.currentTimeMillis());
        
        // Name
        oos.writeObject(context.getName());
        
        // ContactNode
        Contact local = context.getLocalNode();
        oos.writeInt(local.getVendor());
        oos.writeShort(local.getVersion());
        oos.writeObject(local.getNodeID());
        // TODO: store SocketAddress?
        oos.writeByte(local.getInstanceID());
        oos.writeByte(local.getFlags());
        
        // Store the RouteTable
        oos.writeBoolean(storeRouteTable);
        if (storeRouteTable) {
            List<Contact> nodes = context.getRouteTable().getLiveContacts();
            
            KUID nodeId = local.getNodeID();
            for(Contact node : nodes) {
                if (!node.getNodeID().equals(nodeId)) {
                    oos.writeObject(node);
                }
            }
            
            // Terminator
            oos.writeObject(null); 
        }

        // Store the Database
        oos.writeBoolean(storeDatabase);
        boolean anyLocalKeyValue = false;
        
        if (storeDatabase) {
            Collection<KeyValue> keyValues = context.getDatabase().getValues();
            
            for(KeyValue keyValue : keyValues) {
                if (!storeRouteTable 
                        && !keyValue.isLocalKeyValue()) {
                    continue;
                }
                
                if (!anyLocalKeyValue 
                        && keyValue.isLocalKeyValue()) {
                    anyLocalKeyValue = true;
                }
                
                oos.writeObject(keyValue);
            }
            
            // Terminator
            oos.writeObject(null);
        }
        
        // Store the KeyPair if there are any local KeyValues
        if (storeDatabase && anyLocalKeyValue) {
            oos.writeObject(context.getKeyPair());
        } else {
            oos.writeObject(null);
        }
    }

    /**
     * Loads a Mojito DHT instance from the InputStream.
     */
    public static MojitoDHT load(InputStream in) 
            throws ClassNotFoundException, IOException {
        
        ObjectInputStream ois = new ObjectInputStream(in);
        
        // SERIAL_VERSION_UID
        long serialVersionUID = ois.readLong();
        
        // Time
        long time = ois.readLong();
        
        // Name
        String name = (String)ois.readObject();
        
        // ContactNode
        int vendor = ois.readInt();
        int version = ois.readUnsignedShort();
        KUID nodeId = (KUID)ois.readObject();
        // TODO: load SocketAddress?
        int instanceId = ois.readUnsignedByte();
        int flags = ois.readUnsignedByte();
        
        //boolean timeout = (System.currentTimeMillis()-time) 
        //                    >= ContextSettings.NODE_ID_TIMEOUT.getValue();
        
        boolean timeout = false;
        if (timeout) {
            if (LOG.isInfoEnabled()) {
                LOG.info(nodeId + " has timed out. Cannot restore the RouteTable and Database");
            }
            
            nodeId = KUID.createRandomNodeID();
            instanceId = 0;
        }
        
        Contact local = new ContactNode(vendor, version, nodeId, 
                new InetSocketAddress(0), instanceId, flags, State.UNKNOWN);
        
        // Create an instance w/o a KeyPair for now (will set it later!)
        MojitoDHT dht = new MojitoDHT(name, local, null);
        
        // Load the RouteTable
        boolean storeRouteTable = ois.readBoolean();
        if (storeRouteTable) {
            RouteTable routeTable = dht.context.getRouteTable();
            
            Contact node = null;
            while((node = (Contact)ois.readObject()) != null) {
                if (!timeout) {
                    routeTable.add(node);
                }
            }
        }
        
        // Load the Database
        boolean storeDatabase = ois.readBoolean();
        if (storeDatabase) {
            Database database = dht.context.getDatabase();
            
            KeyValue keyValue = null;
            while((keyValue = (KeyValue)ois.readObject()) != null) {
                if (!timeout || keyValue.isLocalKeyValue()) {
                    database.add(keyValue);
                }
            }
        }
        
        // Load the KeyPair. If null then create a new KeyPair!
        KeyPair keyPair = (KeyPair)ois.readObject();
        if (!storeDatabase || keyPair == null) {
            keyPair = CryptoHelper.createKeyPair();
        }
        dht.context.setKeyPair(keyPair);

        return dht;
    }
}
