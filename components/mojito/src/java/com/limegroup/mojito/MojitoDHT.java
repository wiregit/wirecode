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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.DHTValueType;
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
     * Sets the ThreadFactory that will be used to create
     * all Thread. Passing null will reset it to the default
     * ThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory);
    
    /**
     * Returns the ThreadFactory that's used to create Threads
     */
    public ThreadFactory getThreadFactory();
    
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
     * Sets and returns the RouteTable. The class must be a sub-class of
     * <tt>RouteTable</tt>
     */
    public void setRouteTable(RouteTable routeTable);
    
    /**
     * Sets the Host Filter
     */
    public void setHostFilter(HostFilter hostFilter);
    
    /**
     * Sets the Database
     */
    public void setDatabase(Database database);
    
    /**
     * Tries to bootstrap from the given SocketAddress
     */
    public DHTFuture<BootstrapResult> bootstrap(SocketAddress address);
    
    /**
     * Tries to bootstrap from one of the items in the hostList
     */
    public DHTFuture<BootstrapResult> bootstrap(Set<? extends SocketAddress> hostList);
    
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
    public Collection<DHTValue> getValues();
    
    /**
     * Tries to find the value for the given key
     */
    public DHTFuture<FindValueResult> get(KUID key);
    
    /**
     * Stores the given key, value pair
     * @param version TODO
     */
    public DHTFuture<StoreResult> put(KUID key, DHTValueType type, int version, byte[] value);
    
    /**
     * Removes the value for the given key
     */
    public DHTFuture<StoreResult> remove(KUID key);
    
    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * period. The action is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, 
            long delay, long period, TimeUnit unit);
    
    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * delay. The action is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, 
            long initialDelay, long delay, TimeUnit unit);
            
    /**
     * Creates and executes a ScheduledFuture that becomes enabled after the
     * given delay. The task is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit);
    
    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task. The task is executed on
     * Mojito DHTs internal Executor (an unbound ThreadPoolExecutor).
     */
    public <V> Future<V> submit(Callable<V> task);
    
    /**
     * Executes the given command at some time in the future. The command 
     * is executed on Mojito DHTs internal Executor (an unbound ThreadPoolExecutor).
     */
    public void execute(Runnable command);
    
    /**
     * Writes the current state of Monjito DHT to the OutputStream
     */
    public void store(OutputStream out) throws IOException;
}
