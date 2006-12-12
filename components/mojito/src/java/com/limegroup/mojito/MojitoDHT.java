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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Set;

import com.limegroup.mojito.concurrent.DHTExecutorService;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueEntity;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.result.BootstrapResult;
import com.limegroup.mojito.result.FindValueResult;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.result.StoreResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.util.HostFilter;

/**
 * The public interface of the Mojito DHT
 */
public interface MojitoDHT {
    
    /**
     * Returns the name of the DHT instance
     */
    public String getName();
    
    /**
     * Returns the vendor of the DHT
     */
    public int getVendor();
    
    /**
     * Returns the version of the DHT
     */
    public int getVersion();
    
    /**
     * Returns the DHT stats
     */
    public DHTStats getDHTStats();
    
    /**
     * Returns the local Node ID
     */
    public KUID getLocalNodeID();
    
    /**
     * Returns the local Node's Contact
     */
    public Contact getLocalNode();
    
    /**
     * Returns whether or not this DHT is firewalled
     */
    public boolean isFirewalled();
    
    /**
     * Returns true if this Node is bound to a Network Interface
     */
    public boolean isBound();
    
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
     * Starts the DHT
     */
    public void start();
    
    /**
     * Stops the DHT
     */
    public void stop();
    
    /**
     * Closes the DHT instance and releases all resources
     */
    public void close();
    
    /**
     * Returns whether or not this DHT is bootstrapped
     */
    public boolean isBootstrapped();
    
    /**
     * Returns the approximate size of the DHT
     */
    public BigInteger size();
    
    /**
     * Sets the external (forced) Port number
     */
    public void setExternalPort(int port);
    
    /**
     * Returns the external (forced) Port number
     */
    public int getExternalPort();
    
    /**
     * Returns the external (forced) contact address
     */
    public SocketAddress getContactAddress();
    
    /**
     * Returns the local address
     */
    public SocketAddress getLocalAddress();
    
    /**
     * Sets the MessageFactory
     */
    public void setMessageFactory(MessageFactory messageFactory);
    
    /**
     * Sets and returns the MessageDispatcher. The class must be a sub-class of
     * <tt>MessageDispatcher</tt>
     */
    public MessageDispatcher setMessageDispatcher(Class<? extends MessageDispatcher> messageDispatcher);
    
    /**
     * Sets the RouteTable
     */
    public void setRouteTable(RouteTable routeTable);
    
    /**
     * Returns the RouteTable
     */
    public RouteTable getRouteTable();
    
    /**
     * Sets the Host Filter
     */
    public void setHostFilter(HostFilter hostFilter);
    
    /**
     * Sets the Database
     */
    public void setDatabase(Database database);
    
    /**
     * Returns the Database
     */
    public Database getDatabase();
    
    /**
     * Bootstraps the MojitoDHT from the given Contact. Use
     * the ping() methods to find a Contact!
     */
    public DHTFuture<BootstrapResult> bootstrap(Contact node);
    
    /**
     * Tries to ping Contacts in the RouteTable. You may use
     * this method to find a remote Node from where you can
     * bootstrap your MojitoDHT instance.
     */
    public DHTFuture<PingResult> findActiveContact();
    
    /**
     * Tries to ping the given address
     */
    public DHTFuture<PingResult> ping(SocketAddress dst);
    
    /**
     * Returns a Set of all keys in the Database
     */
    public Set<KUID> keySet();
    
    /**
     * Returns a Collection of all values in the Database
     */
    public Collection<DHTValueEntity> getValues();
    
    /**
     * Tries to find the value for the given key
     */
    public DHTFuture<FindValueResult> get(KUID key);
    
    /**
     * Stores the given key, value pair
     */
    public DHTFuture<StoreResult> put(KUID key, DHTValue value);
    
    /**
     * Removes the value for the given key
     */
    public DHTFuture<StoreResult> remove(KUID key);
    
    /**
     * 
     */
    public void setDHTExecutorService(DHTExecutorService executorService);
    
    /**
     * 
     */
    public DHTExecutorService getDHTExecutorService();
}
