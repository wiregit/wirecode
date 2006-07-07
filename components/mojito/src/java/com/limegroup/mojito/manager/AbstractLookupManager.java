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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.limegroup.mojito.Context;
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
    
    public Future<V> lookup(KUID lookupId) throws IOException {
        return lookup(lookupId, -1, null);
    }
    
    public Future<V> lookup(KUID lookupId, L l) throws IOException {
        return lookup(lookupId, -1, l);
    }
    
    public Future<V> lookup(KUID lookupId, int count, L l) throws IOException {
        synchronized(getLookupLock()) {
            LookupFuture future = futureMap.get(lookupId);
            if (future == null) {
                LookupResponseHandler<V> handler = createLookupHandler(lookupId, count);
                handler.start();
                
                future = new LookupFuture(lookupId, handler);
                futureMap.put(lookupId, future);
                
                context.execute(future);
            }
            
            if (l != null) {
                future.addDHTEventListener(l);
            }
            
            return future;
        }
    }
    
    protected abstract LookupResponseHandler<V> createLookupHandler(KUID lookupId, int count);
    
    private class LookupFuture extends FutureTask<V> {

        private KUID lookupId;
        
        private LookupResponseHandler<V> handler;
        
        private List<L> listeners = null;
        
        public LookupFuture(KUID lookupId, LookupResponseHandler<V> handler) {
            super(handler);
            this.lookupId = lookupId;
            this.handler = handler;
        }

        public void addDHTEventListener(L l) {
            if (listeners == null) {
                listeners = new ArrayList<L>();
            }
            listeners.add(l);
        }
        
        public LookupResponseHandler<V> getLookupResponseHandler() {
            return handler;
        }
        
        @Override
        protected void done() {
            super.done();
            
            synchronized(getLookupLock()) {
                futureMap.remove(lookupId);
            }
            
            try {
                V value = get();
                fireResult(value);
            } catch (CancellationException ignore) {
            } catch (InterruptedException ignore) {
            } catch (Exception err) {
                fireException(err);
            }
        }
        
        @SuppressWarnings("unchecked")
        private void fireResult(final V result) {
            synchronized(globalListeners) {
                for (L l : globalListeners) {
                    l.handleResult(result);
                }
            }
            
            if (listeners != null) {
                for (L l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        private void fireException(final Exception ex) {
            synchronized(globalListeners) {
                for (L l : globalListeners) {
                    l.handleException(ex);
                }
            }
            
            if (listeners != null) {
                for (L l : listeners) {
                    l.handleException(ex);
                }
            }
        }
    }
}
