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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.BootstrapEvent;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.routing.RouteTable;

/**
 * 
 */
public interface MojitoDHT {
    
    /**
     * Returns the name of the DHT instance
     */
    public String getName();
    
    /**
     * Returns the version of the DHT
     */
    public int getVersion();
    
    /**
     * Returns the vendor of the DHT
     */
    public int getVendor();
    
    /**
     * Returns the local Node ID
     */
    public KUID getLocalNodeID();
    
    /**
     * 
     */
    //public Contact getLocalNode();
    
    /**
     * Returns whether or not this DHT is firewalled
     */
    public boolean isFirewalled();
    
    /**
     * Binds the DHT to the specified Port number and the
     * any-address
     * 
     * @param port
     * @throws IOException
     */
    public void bind(int port) throws IOException;
    
    /**
     * Binds the DHT to the specified InetAddress and Port number
     * 
     * @param addr
     * @param port
     * @throws IOException
     */
    public void bind(InetAddress addr, int port) throws IOException;
    
    /**
     * Binds the DHT to the specified SocketAddress
     * 
     * @param address
     * @throws IOException
     */
    public void bind(SocketAddress address) throws IOException;
    
    /**
     * Returns whether or not this DHT is running
     */
    public boolean isRunning();
    
    /**
     * Returns whether or not this DHT is bootstrapped
     */
    public boolean isBootstrapped();
    
    /**
     * Starts the DHT
     */
    public void start();
    
    /**
     * Stops the DHT
     */
    public void stop();
    
    /**
     * Returns the approximate size of the DHT
     */
    public int size();
    
    /**
     * Sets the external (forced) Port number
     */
    public void setExternalPort(int port);
    
    /**
     * Returns the external (forced) Port number
     */
    public int getExternalPort();
    
    /**
     * Sets the external (forced) contact address
     */
    //public void setContactAddress(SocketAddress addr);
    
    /**
     * Returns the external (forced) contact address
     */
    public SocketAddress getContactAddress();
    
    /**
     * Returns the local address
     */
    public SocketAddress getLocalAddress();
    
    /**
     * Sets the ThreadFactory that will be used to create
     * all Thread. Passing null will reset it to the default
     * ThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory);
    
    /**
     * Sets the MessageFactory
     */
    public void setMessageFactory(MessageFactory messageFactory);
    
    /**
     * Sets and returns the MessageDispatcher
     */
    public MessageDispatcher setMessageDispatcher(Class<? extends MessageDispatcher> messageDispatcher);
    
    /**
     * Sets and returns the RouteTable
     */
    public RouteTable setRoutingTable(Class<? extends RouteTable> routeTable);
    
    /**
     * Tries to (re-)bootstrap from the RouteTable
     */
    //public DHTFuture<BootstrapEvent> bootstrap();
    
    /**
     * Tries to bootstrap from the given SocketAddress
     */
    public DHTFuture<BootstrapEvent> bootstrap(SocketAddress address);
    
    /**
     * Tries to bootstrap from one of the items in the hostList
     */
    public DHTFuture<BootstrapEvent> bootstrap(List<? extends SocketAddress> hostList);
    
    /**
     * Tries to ping the given address
     */
    public DHTFuture<Contact> ping(SocketAddress dst);
    
    /**
     * Returns a Set of all keys in the Database
     */
    public Set<KUID> getKeys();
    
    /**
     * Returns a Collection of all values in the Database
     */
    public Collection<KeyValue> getValues();
    
    /**
     * Tries to find the value for the given key
     */
    public DHTFuture<FindValueEvent> get(KUID key);
    
    /**
     * Stores the given key, value pair
     */
    public DHTFuture<StoreEvent> put(KUID key, byte[] value);
    
    /**
     * Stores the given key, value pair
     */
    //public DHTFuture<StoreEvent> put(KUID key, byte[] value, PrivateKey privateKey);
    
    /**
     * Removes the value for the given key
     */
    public DHTFuture<StoreEvent> remove(KUID key);
    
    /**
     * Removes the value for the given key
     */
    //public DHTFuture<StoreEvent> remove(KUID key, PrivateKey privateKey);
    
    /**
     * Writes the current state of Monjito DHT to the OutputStream
     */
    public void store(OutputStream out) throws IOException;
}
