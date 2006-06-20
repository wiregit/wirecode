/*
 * Mojito Distributed Hash Tabe (DHT)
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

package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The PingManager takes care of concurrent Pings and makes sure
 * a single Node cannot be pinged multiple times.
 */
public class PingManager implements PingListener {
    
    private Context context;

    private Map<SocketAddress, PingResponseHandler> handlerMap 
        = new HashMap<SocketAddress, PingResponseHandler>();
    
    private List<PingListener> listeners = new ArrayList<PingListener>();
    
    private NetworkStatisticContainer networkStats;
    
    public PingManager(Context context) {
        this.context = context;
        networkStats = context.getNetworkStats();
    }
    
    public void init() {
        synchronized (handlerMap) {
            handlerMap.clear();
        }
    }
    
    public void addPingListener(PingListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public void removePingListener(PingListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public PingListener[] getPingListeners() {
        synchronized (listeners) {
            return listeners.toArray(new PingListener[0]);
        }
    }
    
    public void ping(SocketAddress address) throws IOException {
        ping(null, address, null);
    }

    public void ping(SocketAddress address, PingListener listener) throws IOException {
        ping(null, address, listener);
    }

    public void ping(ContactNode node) throws IOException {
        ping(node.getNodeID(), node.getSocketAddress(), null);
    }
    
    public void ping(ContactNode node, PingListener listener) throws IOException {
        ping(node.getNodeID(), node.getSocketAddress(), listener);
    }

    public void ping(KUID nodeId, SocketAddress address, PingListener listener) throws IOException {
        synchronized (handlerMap) {
            PingResponseHandler responseHandler = handlerMap.get(address);
            if (responseHandler == null) {
                
                responseHandler = new PingResponseHandler(context);
                responseHandler.addPingListener(this);
                
                if (listener != null) {
                    responseHandler.addPingListener(listener);
                }
                
                PingRequest request = context.getMessageHelper().createPingRequest(address);
                context.getMessageDispatcher().send(nodeId, address, request, responseHandler);

                handlerMap.put(address, responseHandler);
                networkStats.PINGS_SENT.incrementStat();
                
            } else if (listener != null) {
                responseHandler.addPingListener(listener);
            }
        }
    }
    
    public void response(final ResponseMessage response, final long time) {
        networkStats.PINGS_OK.incrementStat();
        
        synchronized (handlerMap) {
            handlerMap.remove(response.getContactNode().getSocketAddress());
            context.fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(PingListener listener : listeners) {
                            listener.response(response, time);
                        }
                    }
                }
            });
        }
    }

    public void timeout(final KUID nodeId, final SocketAddress address, RequestMessage request, final long time) {            
        networkStats.PINGS_FAILED.incrementStat();
        
        synchronized (handlerMap) {
            handlerMap.remove(address);
            context.fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(PingListener listener : listeners) {
                            listener.timeout(nodeId, address, null, time);
                        }
                    }
                }
            });
        }
    }

    public boolean cancel(ContactNode node) {
        return cancel(node.getSocketAddress());
    }
    
    public boolean cancel(SocketAddress address) {
        synchronized (handlerMap) {
            PingResponseHandler handler = handlerMap.remove(address);
            if (handler != null) {
                handler.stop();
                return true;
            }
        }
        return false;
    }
}