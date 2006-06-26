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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.handler.response.LookupResponseHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * The LookupManager takes care of multiple concurrent lookups
 */
public class LookupManager implements LookupListener {
    
    private static final Log LOG = LogFactory.getLog(LookupManager.class);
    
    private Context context;

    private Map<KUID, LookupResponseHandler> handlerMap 
        = new HashMap<KUID, LookupResponseHandler>();
    
    private List<LookupListener> listeners = new ArrayList<LookupListener>();
    
    public LookupManager(Context context) {
        this.context = context;
    }
    
    public void init() {
        synchronized (handlerMap) {
            handlerMap.clear();
        }
    }
    
    public void addLookupListener(LookupListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public void removeLookupListener(LookupListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public LookupListener[] getLookupListeners() {
        synchronized (listeners) {
            return listeners.toArray(new LookupListener[0]);
        }
    }
    
    public boolean lookup(KUID lookup) throws IOException {
        return lookup(lookup, null);
    }
    
    public boolean lookup(KUID lookup, LookupListener listener) throws IOException {
        if (!lookup.isNodeID() && !lookup.isValueID()) {
            throw new IllegalArgumentException("Lookup ID must be either a NodeID or ValueID");
        }
        
        synchronized (handlerMap) {
            LookupResponseHandler handler = handlerMap.get(lookup);
            if (handler == null) {
                handler = new LookupResponseHandler(lookup, context);
                handler.addLookupListener(this);
                
                if (listener != null) {
                    handler.addLookupListener(listener);
                }
                
                handler.start();
                handlerMap.put(lookup, handler);
                return true;
                
            } else if (listener != null) {
                handler.addLookupListener(listener);
            }
            
            return false;
        }
    }
    
    public boolean cancel(KUID lookup) {
        synchronized (handlerMap) {
            LookupResponseHandler handler = handlerMap.remove(lookup);
            if (handler != null) {
                handler.stop();
                return true;
            }
        }
        return false;
    }

    public void response(final ResponseMessage response, final long time) {
        synchronized (handlerMap) {
            context.fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(LookupListener listener : listeners) {
                            listener.response(response, time);
                        }
                    }
                }
            });
        }
    }

    public void timeout(final KUID nodeId, final SocketAddress address, 
            final RequestMessage request, final long time) {
        synchronized (handlerMap) {
            context.fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(LookupListener listener : listeners) {
                            listener.timeout(nodeId, address, request, time);
                        }
                    }
                }
            });
        }
    }
    
    public void found(final KUID lookup, final Collection c, final long time) {
        synchronized (handlerMap) {
            LookupResponseHandler handler = handlerMap.remove(lookup);
            
            context.fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(LookupListener listener : listeners) {
                            listener.found(lookup, c, time);
                        }
                    }
                }
            });
        }
    }
    
    public void finish(final KUID lookup, final Collection c, final long time) {
        synchronized (handlerMap) {
            LookupResponseHandler handler = handlerMap.remove(lookup);
            
            context.fireEvent(new Runnable() {
                public void run() {
                    synchronized (listeners) {
                        for(LookupListener listener : listeners) {
                            listener.finish(lookup, c, time);
                        }
                    }
                }
            });
        }
    }
}