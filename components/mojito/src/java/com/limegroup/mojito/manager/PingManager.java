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

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The PingManager takes care of concurrent Pings and makes sure
 * a single Node cannot be pinged multiple times.
 */
public class PingManager extends AbstractManager<Contact> {
    
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
    
    public Object getPingLock() {
        return futureMap;
    }
    
    public DHTFuture<Contact> ping(SocketAddress address) {
        return ping(null, address);
    }

    public DHTFuture<Contact> ping(Contact node) {
        return ping(node.getNodeID(), node.getContactAddress());
    }
    
    public DHTFuture<Contact> ping(KUID nodeId, SocketAddress address) {
        
        synchronized (getPingLock()) {
            PingFuture future = futureMap.get(address);
            
            if (future == null) {
                PingResponseHandler handler = new PingResponseHandler(context, nodeId, address);
                
                future = new PingFuture(address, handler);
                futureMap.put(address, future);
                networkStats.PINGS_SENT.incrementStat();
                
                context.execute(future);
            }
            
            return future;
        }
    }
    
    /**
     * 
     */
    private class PingFuture extends AbstractDHTFuture {

        private SocketAddress address;
        
        public PingFuture(SocketAddress address, Callable<Contact> handler) {
            super(handler);
            this.address = address;
        }
        
        public SocketAddress getSocketAddress() {
            return address;
        }
        
        @Override
        protected void deregister() {
            synchronized (getPingLock()) {
                futureMap.remove(address);
            }
        }

        @Override
        protected void fireResult(Contact result) {
            networkStats.PINGS_OK.incrementStat();
            super.fireResult(result);
        }
        
        @Override
        protected void fireThrowable(Throwable ex) {
            networkStats.PINGS_FAILED.incrementStat();
            super.fireThrowable(ex);
        }
    }
}
