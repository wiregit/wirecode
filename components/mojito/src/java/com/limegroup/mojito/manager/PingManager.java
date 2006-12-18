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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.AbstractDHTFuture;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.handler.response.PingResponseHandler.PingIterator;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;
import com.limegroup.mojito.util.ContactUtils;

/**
 * The PingManager takes care of concurrent Pings and makes sure
 * a single Node cannot be pinged multiple times in parallel.
 */
public class PingManager extends AbstractManager<PingResult> {
    
    private final Map<SocketAddress, PingFuture> futureMap 
        = Collections.synchronizedMap(new HashMap<SocketAddress, PingFuture>());
    
    private NetworkStatisticContainer networkStats;
    
    public PingManager(Context context) {
        super(context);
        networkStats = context.getNetworkStats();
    }
    
    public void init() {
        futureMap.clear();
    }
    
    /**
     * Sends a ping to the remote Host
     */
    public DHTFuture<PingResult> ping(SocketAddress host) {
        PingIterator pinger = new PingIteratorFactory.SocketAddressPinger(host);
        return ping(null, host, pinger);
    }

    public DHTFuture<PingResult> pingAddresses(Set<SocketAddress> hosts) {
        PingIterator pinger = new PingIteratorFactory.SocketAddressPinger(hosts);
        return ping(null, null, pinger);
    }
    
    /**
     * Sends a ping to the remote Node
     */
    public DHTFuture<PingResult> ping(Contact node) {
        PingIterator pinger = new PingIteratorFactory.ContactPinger(node);
        return ping(null, node.getContactAddress(), pinger);
    }
    
    /**
     * Sends a ping to the remote Node
     */
    public DHTFuture<PingResult> ping(KUID nodeId, SocketAddress address) {
        PingIterator pinger = new PingIteratorFactory.EntryPinger(nodeId, address);
        return ping(null, address, pinger);
    }
    
    /**
     * Sends a ping to the remote Node
     */
    public DHTFuture<PingResult> ping(Set<? extends Contact> nodes) {
        PingIterator pinger = new PingIteratorFactory.ContactPinger(nodes);
        return ping(null, null, pinger);
    }
    
    /**
     * Sends a special ping to the given Node to test if there
     * is a Node ID collision
     */
    public DHTFuture<PingResult> collisionPing(Contact node) {
        return collisionPing(node.getContactAddress(), Collections.singleton(node));
    }
    
    /**
     * 
     */
    public DHTFuture<PingResult> collisionPing(Set<? extends Contact> nodes) {
        return collisionPing(null, nodes);
    }
    
    /**
     * Sends a special ping to the given Node to test if there
     * is a Node ID collision
     */
    private DHTFuture<PingResult> collisionPing(SocketAddress key, Set<? extends Contact> nodes) {
        Contact sender = ContactUtils.createCollisionPingSender(context.getLocalNode());
        PingIterator pinger = new PingIteratorFactory.CollisionPinger(context, sender, nodes);
        return ping(sender, key, pinger);
    }
    
    /**
     * Sends a ping to the remote Node
     * 
     * @param sender The local Node
     * @param nodeId The remote Node's KUID (can be null)
     * @param key The remote Node's address
     */
    private DHTFuture<PingResult> ping(Contact sender, SocketAddress key, PingIterator pinger) {
        PingFuture future;
        synchronized (futureMap) {
            future = (key != null ? futureMap.get(key) : null);

            if (future == null) {
                PingResponseHandler handler = new PingResponseHandler(context, sender, pinger);

                future = new PingFuture(key, handler);
                if (key != null) {
                    futureMap.put(key, future);
                }
            }
        }
        networkStats.PINGS_SENT.incrementStat();
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * A ping specific implementation of DHTFuture 
     */
    private class PingFuture extends AbstractDHTFuture<PingResult> {

        private final SocketAddress key;
        
        public PingFuture(SocketAddress key, Callable<PingResult> handler) {
            super(handler);
            this.key = key;
        }
        
        @Override
        protected void deregister() {
            if (key != null) {
                futureMap.remove(key);
            }
        }
        
        @Override
        protected void fireFutureSuccess(PingResult value) {
            networkStats.PINGS_OK.incrementStat();
            super.fireFutureSuccess(value);
        }
        
        @Override
        protected void fireFutureFailure(ExecutionException e) {
            networkStats.PINGS_FAILED.incrementStat();
            super.fireFutureFailure(e);
        }
    }
}
