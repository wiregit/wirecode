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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.DHTEventListener;
import com.limegroup.mojito.handler.response.LookupResponseHandler;

/**
 * 
 */
abstract class AbstractLookupManager<L extends DHTEventListener, V> {
    
    protected final Context context;
    
    private List<L> globalListeners = new ArrayList<L>();
    
    private Map<KUID, LookupFuture> futureMap = new HashMap<KUID, LookupFuture>();
    
    public AbstractLookupManager(Context context) {
        this.context = context;
    }
    
    public void init() {
        synchronized(getLookupLock()) {
            futureMap.clear();
        }
    }
    
    private Object getLookupLock() {
        return futureMap;
    }
    
    public void addDHTEventListener(L l) {
        synchronized(globalListeners) {
            globalListeners.add(l);
        }
    }
    
    public void removeDHTListener(L l) {
        synchronized(globalListeners) {
            globalListeners.remove(l);
        }
    }
    
    public DHTFuture<V> lookup(KUID lookupId) {
        return lookup(lookupId, -1);
    }
    
    public DHTFuture<V> lookup(KUID lookupId, int count) {
        synchronized(getLookupLock()) {
            LookupFuture future = futureMap.get(lookupId);
            if (future == null) {
                LookupResponseHandler<V> handler = createLookupHandler(lookupId, count);
                
                future = new LookupFuture(lookupId, handler);
                futureMap.put(lookupId, future);
                
                context.execute(future);
            }
            
            return future;
        }
    }
    
    protected abstract LookupResponseHandler<V> createLookupHandler(KUID lookupId, int count);
    
    private class LookupFuture extends FutureTask<V> implements DHTFuture<V> {

        private KUID lookupId;
        
        private LookupResponseHandler<V> handler;
        
        private List<DHTEventListener<V>> listeners 
            = new ArrayList<DHTEventListener<V>>();
        
        public LookupFuture(KUID lookupId, LookupResponseHandler<V> handler) {
            super(handler);
            this.lookupId = lookupId;
            this.handler = handler;
        }

        public <L extends DHTEventListener<V>> void addDHTEventListener(L listener) {
            if (listener == null || isCancelled()) {
                return;
            }
            
            synchronized (listener) {
                synchronized (listeners) {
                    if (isDone()) {
                        try {
                            V result = get();
                            listener.handleResult(result);
                        } catch (CancellationException ignore) {
                        } catch (InterruptedException ignore) {
                        } catch (Exception ex) {
                            listener.handleException(ex);
                        }
                    } else {
                        listeners.add(listener);
                    }
                }
            }
        }

        @Override
        protected void done() {
            super.done();
            
            synchronized(getLookupLock()) {
                futureMap.remove(lookupId);
            }
            
            try {
                V result = get();
                fireResult(result);
            } catch (CancellationException ignore) {
            } catch (InterruptedException ignore) {
            } catch (Exception ex) {
                fireException(ex);
            }
        }
        
        @SuppressWarnings("unchecked")
        private void fireResult(final V result) {
            synchronized(globalListeners) {
                for (DHTEventListener<V> l : globalListeners) {
                    l.handleResult(result);
                }
            }
            
            synchronized (listeners) {
                for (DHTEventListener<V> l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        private void fireException(final Exception ex) {
            synchronized(globalListeners) {
                for (DHTEventListener<V> l : globalListeners) {
                    l.handleException(ex);
                }
            }
            
            synchronized (listeners) {
                for (DHTEventListener<V> l : listeners) {
                    l.handleException(ex);
                }
            }
        }
        
        public String toString() {
            return "Future: " + lookupId + ", " + handler;
        }
    }
}
