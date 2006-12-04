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
 
package com.limegroup.mojito.manager;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.AbstractDHTFuture;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The PingManager takes care of concurrent Pings and makes sure
 * a single Node cannot be pinged multiple times in parallel.
 */
public class PingManager extends AbstractManager<PingResult> {
    
    private Map<SocketAddress, PingFuture> futureMap 
        = new HashMap<SocketAddress, PingFuture>();
    
    private NetworkStatisticContainer networkStats;
    
    public PingManager(Context context) {
        super(context);
        networkStats = context.getNetworkStats();
    }
    
    public void init() {
        synchronized (getPingLock()) {
            futureMap.clear();
        }
    }
    
    /**
     * Returns the lock Object
     */
    private Object getPingLock() {
        return futureMap;
    }
    
    /**
     * Sends a ping to the remote Host
     */
    public DHTFuture<PingResult> ping(SocketAddress address) {
        return ping(null, null, address);
    }

    /**
     * Sends a ping to the remote Node
     */
    public DHTFuture<PingResult> ping(Contact node) {
        return ping(null, node.getNodeID(), node.getContactAddress());
    }
    
    /**
     * Sends a ping to the remote Node
     */
    public DHTFuture<PingResult> ping(KUID nodeId, SocketAddress address) {
        return ping(null, nodeId, address);
    }
    
    /**
     * Sends a special ping to the given Node to test if there
     * is a Node ID collision
     */
    public DHTFuture<PingResult> collisionPing(Contact node) {
        // The idea is to invert our local Node ID so that the
        // other Node doesn't get the impression we're trying
        // to spoof anything and we don't want that the other
        // guy adds this Contact to its RouteTable. To do so
        // we're creating a firewalled version of our local Node
        // (with the inverted Node ID of course).
        int vendor = context.getVendor();
        int version = context.getVersion();
        KUID nodeId = context.getLocalNodeID().invert();
        SocketAddress addr = context.getContactAddress();
        Contact sender = ContactFactory.createLiveContact(
                addr, vendor, version, nodeId, addr, 0, Contact.FIREWALLED_FLAG);
        
        return ping(sender, node.getNodeID(), node.getContactAddress());
    }
    
    /**
     * Sends a ping to the remote Node
     * 
     * @param sender The local Node
     * @param nodeId The remote Node's KUID (can be null)
     * @param address The remote Node's address
     */
    private DHTFuture<PingResult> ping(Contact sender, KUID nodeId, SocketAddress address) {
        synchronized (getPingLock()) {
            
            PingFuture future = futureMap.get(address);
            
            if (future == null) {
                PingResponseHandler handler = new PingResponseHandler(context, sender, nodeId, address);
                
                future = new PingFuture(address, handler);
                futureMap.put(address, future);
                networkStats.PINGS_SENT.incrementStat();
                
                context.getDHTExecutorService().execute(future);
            }
            
            return future;
        }
    }
    
    /**
     * A ping specific implementation of DHTFuture 
     */
    private class PingFuture extends AbstractDHTFuture<PingResult> {

        private SocketAddress address;
        
        public PingFuture(SocketAddress address, Callable<PingResult> handler) {
            super(handler);
            this.address = address;
        }
        
        @Override
        protected void deregister() {
            synchronized (getPingLock()) {
                futureMap.remove(address);
            }
        }
        
        @Override
        public void fireFutureSuccess(PingResult value) {
            networkStats.PINGS_OK.incrementStat();
            super.fireFutureSuccess(value);
        }
        
        @Override
        public void fireFutureFailure(ExecutionException e) {
            networkStats.PINGS_FAILED.incrementStat();
            super.fireFutureFailure(e);
        }
    }
}
