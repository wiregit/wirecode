/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.security.KeyPair;

import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.statistics.DHTStats;
import org.limewire.mojito.util.HostFilter;


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
    public Vendor getVendor();
    
    /**
     * Returns the version of the DHT
     */
    public Version getVersion();
    
    /**
     * 
     */
    public void setKeyPair(KeyPair keyPair);
    
    /**
     * 
     */
    public KeyPair getKeyPair();
    
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
     * Returns whether or not the MojitoDHT is bootstrapping
     */
    public boolean isBootstrapping();
    
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
     * 
     */
    public void setDHTValueFactory(DHTValueFactory valueFactory);
    
    /**
     * 
     */
    public DHTValueFactory getDHTValueFactory();
    
    /**
     * 
     */
    public void setDHTValueEntityPublisher(DHTValueEntityPublisher x);
    
    /**
     * 
     */
    public DHTValueEntityPublisher getDHTValueEntityPublisher();
    
    /**
     * Bootstraps the MojitoDHT from the given Contact. Use
     * the ping() methods to find a Contact!
     */
    public DHTFuture<BootstrapResult> bootstrap(Contact node);
    
    /**
     * 
     */
    public DHTFuture<BootstrapResult> bootstrap(SocketAddress dst);
    
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
     * Tries to find the value for the given key
     */
    public DHTFuture<FindValueResult> get(KUID key);
    
    /**
     * Tries to get the value of the given EntityKey
     */
    public DHTFuture<FindValueResult> get(EntityKey entityKey);
    
    /**
     * Stores the given key, value pair
     */
    public DHTFuture<StoreResult> put(KUID key, DHTValue value);
    
    /**
     * Removes the value for the given key
     */
    public DHTFuture<StoreResult> remove(KUID key);
    
    /**
     * Sets the DHTExecutorService
     */
    public void setDHTExecutorService(DHTExecutorService executorService);
    
    /**
     * Returns the DHTExecutorService
     */
    public DHTExecutorService getDHTExecutorService();
}
