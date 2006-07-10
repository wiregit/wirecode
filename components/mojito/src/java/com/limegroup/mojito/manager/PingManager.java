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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The PingManager takes care of concurrent Pings and makes sure
 * a single Node cannot be pinged multiple times.
 */
public class PingManager extends AbstractManager {
    
    private Map<SocketAddress, PingFuture> futureMap 
        = new HashMap<SocketAddress, PingFuture>();
    
    private List<PingListener> globalListeners 
        = new ArrayList<PingListener>();
    
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
    
    public void addPingListener(PingListener listener) {
        synchronized (globalListeners) {
            globalListeners.add(listener);
        }
    }
    
    public void removePingListener(PingListener listener) {
        synchronized (globalListeners) {
            globalListeners.remove(listener);
        }
    }

    public PingListener[] getPingListeners() {
        synchronized (globalListeners) {
            return globalListeners.toArray(new PingListener[0]);
        }
    }
    
    public Future<Contact> ping(SocketAddress address) {
        return ping(null, address, null);
    }

    public Future<Contact> ping(SocketAddress address, PingListener listener) {
        return ping(null, address, listener);
    }

    public Future<Contact> ping(Contact node) {
        return ping(node.getNodeID(), node.getSocketAddress(), null);
    }
    
    public Future<Contact> ping(Contact node, PingListener listener) {
        return ping(node.getNodeID(), node.getSocketAddress(), listener);
    }

    public Future<Contact> ping(KUID nodeId, SocketAddress address, PingListener listener) {
        
        synchronized (getPingLock()) {
            PingFuture future = futureMap.get(address);
            
            if (future == null) {
                PingResponseHandler handler = new PingResponseHandler(context, nodeId, address);
                
                future = new PingFuture(address, handler);
                futureMap.put(address, future);
                networkStats.PINGS_SENT.incrementStat();
                
                context.execute(future);
            }
            
            if (listener != null) {
                future.addPingListener(listener);
            }
            
            return future;
        }
    }
    
    /**
     * 
     */
    private class PingFuture extends FutureTask<Contact> {

        private SocketAddress address;
        
        private PingResponseHandler handler;
        
        private List<PingListener> listeners = null;
        
        public PingFuture(SocketAddress address, PingResponseHandler handler) {
            super(handler);
            this.address = address;
        }
        
        public SocketAddress getSocketAddress() {
            return address;
        }
        
        public PingResponseHandler getPingResponseHandler() {
            return handler;
        }
        
        public void addPingListener(PingListener l) {
            if (listeners == null) {
                listeners = new ArrayList<PingListener>();
            }
            
            listeners.add(l);
        }
        
        @Override
        protected void done() {
            super.done();
            
            synchronized(getPingLock()) {
                futureMap.remove(address);
            }
            
            try {
                Contact node = get();
                fireResult(node);
            } catch (CancellationException ignore) {
            } catch (InterruptedException ignore) {
            } catch (Exception err) {
                fireException(err);
            }
        }
        
        private void fireResult(final Contact result) {
            networkStats.PINGS_OK.incrementStat();
            
            synchronized(globalListeners) {
                for (PingListener l : globalListeners) {
                    l.handleResult(result);
                }
            }
            
            if (listeners != null) {
                for (PingListener l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        private void fireException(final Exception ex) {
            networkStats.PINGS_FAILED.incrementStat();
            
            synchronized(globalListeners) {
                for (PingListener l : globalListeners) {
                    l.handleException(ex);
                }
            }
            
            if (listeners != null) {
                for (PingListener l : listeners) {
                    l.handleException(ex);
                }
            }
        }
    }
}
